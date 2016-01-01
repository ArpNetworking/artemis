/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.groupon.deployment.host;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import client.DockerDeploymentClient;
import client.DockerDeploymentClient.ContainerDescription;
import client.docker.PortMapping;
import client.docker.inspectionbeans.ImageInspection;
import com.groupon.deployment.HostDeploymentCommands;
import com.groupon.deployment.HostDeploymentNotifications;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import models.Host;
import models.Manifest;
import models.PackageVersion;
import models.Stage;
import scala.compat.java8.JFunction;
import scala.compat.java8.JFunction1;
import scala.concurrent.ExecutionContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implements the DeploymentManager actor interface by accepting HostDeploymentCommands.StartDeployment messages.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
public class Docker extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param deploymentClient a deployment client
     * @param futuresContext a context to run futures in
     */
    @AssistedInject
    public Docker(@Assisted final DockerDeploymentClient deploymentClient, final ExecutionContext futuresContext) {
        _deploymentClient = deploymentClient;
        _futuresContext = futuresContext;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof HostDeploymentCommands.StartDeployment) {
            final HostDeploymentCommands.StartDeployment deployStage = (HostDeploymentCommands.StartDeployment) message;
            deploy(deployStage.getManifest(), deployStage.getHost(), deployStage.getStage());

        } else if (message instanceof RunningContainersMsg) {
            final List<ContainerDescription> containerDescriptions = ((RunningContainersMsg) message)._containerDescriptions;
            containerDescriptions.forEach(d -> LOGGER.info().setMessage("Running container").addData("name", d.getName()).log());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        _deploymentClient.shutdown();
    }

    private void deploy(final Manifest manifest, final Host host, final Stage stage) {
        final ActorRef parent = context().parent();

        final LoggerToParent logger = new LoggerToParent(context().parent(), self(), host);
        context().parent().tell(new HostDeploymentNotifications.DeploymentStarted(host), self());

        // TODO(mhayter): andThen always runs in the provided _futuresContext, right? .map sometimes runs in the calling thread. [Artemis-?]
        final String environmentId = String.valueOf(stage.getEnvironment().getId());
        Futures.future(_deploymentClient::getRunningContainers, _futuresContext)
                .map(JFunction.func(containers -> {
                    context().parent().tell(
                            new HostDeploymentNotifications.DeploymentLog(
                                    host,
                                    String.format("Found %d running containers", containers.size())),
                            self());
                    context().parent().tell(
                            new HostDeploymentNotifications.DeploymentLog(
                                    host,
                                    "Looking for extraneous containers"),
                            self());
                    return findContainersForRemoval(manifest, containers, environmentId);
                }), _futuresContext)
                .map(JFunction.func(new RmContainersCb(logger)), _futuresContext)
                .map(JFunction.func(new PullImagesCb(manifest, _deploymentClient, logger)), _futuresContext)
                .map(JFunction.func(new GetPortsCb(manifest, _deploymentClient, logger)), _futuresContext)
                .map(JFunction.func(new StartImagesCb(manifest, _deploymentClient, logger, environmentId)), _futuresContext)
                .andThen(new OnComplete<Void>() {
                    @Override
                    public void onComplete(final Throwable failure, final Void success) throws Throwable {
                        if (failure == null) {
                            // Deploy success!
                            parent.tell(new HostDeploymentNotifications.DeploymentSucceeded(host), self());
                        } else {
                            // Deploy failure!
                            parent.tell(new HostDeploymentNotifications.DeploymentFailed(host, failure), self());
                        }

                    }
                }, _futuresContext);
    }

    /**
     *
     * @return Containers that are running as part of this Environment, which have images that are not specified in
     * the manifest that is being deployed.
     */
    private List<ContainerDescription> findContainersForRemoval(
            final Manifest manifest,
            final List<ContainerDescription> runningContainers,
            final String envId)
            throws DockerDeployFailureException {
        return runningContainers.stream()
                .filter(container -> isInEnv(envId, container))
                .collect(Collectors.toList());
    }

    private boolean isInEnv(final String envName, final ContainerDescription container) {
        final Matcher matcher = CONTAINER_NAME_PATTERN.matcher(container.getName());
        if (!matcher.find()) {
            // If the name of the image does not match artemis' format, it mustn't be in our artemis environment.
            return false;
        }
        return envName.equals(matcher.group(CONTAINER_NAME_PATTERN_ENVIRONMENT_GROUP));
    }

    private final DockerDeploymentClient _deploymentClient;
    private final ExecutionContext _futuresContext;
    private Manifest _manifest;

    /**
     * This string should be prepended to the containers created by artemis.
     */
    // TODO(mhayter): Use 'labels' on containers instead of name prefixes. [Artemis-?]
    private static final String CONTAINER_NAME_PREFIX = "artemis-";
    private static final Pattern CONTAINER_NAME_PATTERN = Pattern.compile(CONTAINER_NAME_PREFIX + "(\\d+)");
    private static final int CONTAINER_NAME_PATTERN_ENVIRONMENT_GROUP = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(Docker.class);

    // TODO(mhayter): make this a checked exception [Artemis-?]
    private static final class DockerDeployFailureException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DockerDeployFailureException(final String message) {
            super(message);
        }

        public DockerDeployFailureException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private static final class RunningContainersMsg {
        private final List<ContainerDescription> _containerDescriptions;

        private RunningContainersMsg(final List<ContainerDescription> containerDescriptions) {
            _containerDescriptions = containerDescriptions;
        }
    }

    private static final class StartImagesCb implements JFunction1<Map<PackageVersion, List<PortMapping>>, Void> {
        private StartImagesCb(
                final Manifest manifest,
                final DockerDeploymentClient client,
                final LoggerToParent logger,
                final String envId) {
            _manifest = manifest;
            _client = client;
            _logger = logger;
            _envId = envId;
        }

        @Override
        public Void apply(final Map<PackageVersion, List<PortMapping>> packageToPortMap) {
            // Start all the images from the manifest
            for (final PackageVersion pkgVersion : _manifest.getPackages()) {
                _logger.log("Starting container with image " + pkgVersion.getVersion());
                // The docker ImageId is the 'version' in the manifest package list
                final String imageId = pkgVersion.getVersion();
                final DockerDeploymentClient.RunCommandBuilder runCommandBuilder = _client.createRunCommandBuilder(imageId);
                // The container name MUST match CONTAINER_NAME_PATTERN
                runCommandBuilder.setContainerName(CONTAINER_NAME_PREFIX + _envId);
                for (final PortMapping portMapping : packageToPortMap.get(pkgVersion)) {
                    runCommandBuilder.addPortMapping(portMapping);
                }

                try {
                    runCommandBuilder.doRun();
                } catch (final DockerDeploymentClient.DockerDeploymentClientException e) {
                    throw Throwables.propagate(e);
                }
            }
            return null;
        }



        private final Manifest _manifest;
        private final DockerDeploymentClient _client;
        private final LoggerToParent _logger;
        private final String _envId;
        private static final long serialVersionUID = -8489088643825495311L;
    }

    private class RmContainersCb implements JFunction1<List<ContainerDescription>, Void> {
        private final LoggerToParent _logger;

        public RmContainersCb(final LoggerToParent logger) {
            _logger = logger;
        }


        @Override
        public Void apply(final List<ContainerDescription> containers) {
            try {
                for (final ContainerDescription c : containers) {
                    _logger.log("Removing container " + c.getName());
                    _deploymentClient.stopAndRemoveContainer(c.getId());
                }
            } catch (final DockerDeploymentClient.DockerDeploymentClientException e) {
                throw new DockerDeployFailureException("Failed to remove extraneous running containers", e);
            }
            return null;
        }

        private static final long serialVersionUID = 3840905603195409063L;
    }

    private static final class GetPortsCb implements JFunction1<Void, Map<PackageVersion, List<PortMapping>>> {
        private final Manifest _manifest;
        private final DockerDeploymentClient _deploymentClient;
        private final LoggerToParent _logger;

        private GetPortsCb(final Manifest manifest, final DockerDeploymentClient deploymentClient, final LoggerToParent logger) {
            _manifest = manifest;
            _deploymentClient = deploymentClient;
            _logger = logger;
        }

        @Override
        public Map<PackageVersion, List<PortMapping>> apply(final Void v1) {
            final List<String> imageReferences = new LinkedList<>();
            final List<PackageVersion> packageVersions = _manifest.getPackages();
            for (final PackageVersion packageVersion : packageVersions) {
                imageReferences.add(packageVersion.getVersion());
            }

            final List<ImageInspection> inspections;
            try {
                inspections = _deploymentClient.inspectImages(imageReferences);
            } catch (final DockerDeploymentClient.DockerDeploymentClientException e) {
                throw Throwables.propagate(e);
            }

            final Map<PackageVersion, List<PortMapping>> packageToPortsMap = Maps.newHashMapWithExpectedSize(packageVersions.size());

            final Iterator<PackageVersion> packages = packageVersions.iterator();
            for (final ImageInspection inspection : inspections) {
                if (packages.hasNext()) {
                    final PackageVersion packageVersion = packages.next();
                    final Map<String, JsonNode> exposedPorts = inspection.getConfig().getExposedPorts();
                    final List<PortMapping> portMappings = new ArrayList<>();
                    for (final String portMapString : exposedPorts.keySet()) {
                        final int port = DockerDeploymentClient.getPortFromExposedPortString(portMapString);
                        /*
                        Map the internal port to the external host port. Any port conflicts amongst the images will cause a failure
                        during 'docker run'
                        */
                        portMappings.add(new PortMapping(port, port));
                        _logger.log("Opening port " + port + " to container with image id " + packageVersion.getVersion());
                    }

                    packageToPortsMap.put(packageVersion, portMappings);
                }
            }

            return packageToPortsMap;
        }

        private static final long serialVersionUID = -1078768538853480646L;
    }

    private static final class PullImagesCb implements JFunction1<Void, Void> {
        private final Manifest _manifest;
        private final DockerDeploymentClient _deploymentClient;
        private final LoggerToParent _logger;

        private PullImagesCb(final Manifest manifest, final DockerDeploymentClient deploymentClient, final LoggerToParent logger) {
            _manifest = manifest;
            _deploymentClient = deploymentClient;
            _logger = logger;
        }

        @Override
        public Void apply(final Void v1) {
            return null;
        }

        private static final long serialVersionUID = -5620630969091464926L;
    }

    private static final class LoggerToParent {

        private final ActorRef _parent;
        private final ActorRef _self;
        private final Host _host;

        private LoggerToParent(final ActorRef parent, final ActorRef self, final Host host) {
            _parent = parent;
            _self = self;
            _host = host;
        }

        public void log(final String s) {
            _parent.tell(new HostDeploymentNotifications.DeploymentLog(_host, s), _self);
        }
    }
}
