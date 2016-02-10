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
package com.groupon.deployment.fleet;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import client.DeploymentClientFactory;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.groupon.deployment.HostDeploymentCommands;
import com.groupon.deployment.HostDeploymentNotifications;
import com.groupon.deployment.SshSessionFactory;
import com.groupon.deployment.host.Docker;
import com.groupon.deployment.host.HostDeploymentFactory;
import com.groupon.deployment.host.Roller;
import models.Deployment;
import models.DeploymentLog;
import models.DeploymentState;
import models.EnvironmentType;
import models.Host;
import models.HostDeployment;
import models.ManifestHistory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import play.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.List;

/**
 * Deploys one host, then moves on to the next.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Sequential extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param hostDeploymentFactory a factory to create a host deployment
     * @param dcf deployment client factory
     * @param sshFactory ssh session factory
     * @param deployment deployment to run
     */
    @AssistedInject
    public Sequential(
            final HostDeploymentFactory hostDeploymentFactory,
            final DeploymentClientFactory dcf,
            final SshSessionFactory sshFactory,
            @Assisted final Deployment deployment) {
        _hostDeploymentFactory = hostDeploymentFactory;
        _dcf = dcf;
        _sshFactory = sshFactory;
        _deployment = Deployment.getById(deployment.getId());  // Refresh the deployment

        final String myName;
        try {
            myName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (final UnknownHostException e) {
            throw Throwables.propagate(e);
        }
        // If this host no longer owns the deployment, die
        if (!myName.equals(_deployment.getDeploymentOwner())) {
            Logger.warn(String.format(
                    "Current server does not own the deployment, aborting deploy on this server; owner=%s",
                    _deployment.getDeploymentOwner()));
            self().tell(PoisonPill.getInstance(), self());
        }
        Logger.info("Sequential fleet deployment actor started up");

        final List<HostDeployment> hosts = Lists.newArrayList();
        deployment.getHostStates().forEach(
                host -> {
                    final DeploymentState hostState = host.getState();
                    if (host.getFinished() == null
                            || hostState == null
                            || (hostState != DeploymentState.FAILED && hostState != DeploymentState.SUCCEEDED)) {
                        hosts.add(host);
                    }
                });

        // Sort the hosts with the following rules:
        // TODO(barp): 1) hosts that are "down" should be deployed first [Artemis-?]
        // 2) if the current machine is in the list, it should be last
        hosts.sort((a, b) -> {
            if (a.getHost().getName().equals(myName)) {
                return 1;
            }
            if (b.getHost().getName().equals(myName)) {
                return -1;
            }
            return a.getHost().getName().compareTo(b.getHost().getName());
        });
        _hostQueue = Queues.newArrayDeque(hosts);
        self().tell("start", self());
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if ("start".equals(message)) {
            _current = _hostQueue.poll();
            log("Deployment starting", null);
            startCurrent();
        } else if (message instanceof HostDeploymentNotifications.DeploymentSucceeded) {
            _deployment.heartbeat();
            // Only update if the host is the currently deploying host
            final HostDeploymentNotifications.DeploymentSucceeded succeeded = (HostDeploymentNotifications.DeploymentSucceeded) message;
            if (!_current.getHost().getName().equals(succeeded.getHost().getName())) {
                Logger.warn(String.format(
                        "Received a host deployment succeeded message from unexpected host; expected=%s, actual=%s",
                        _current.getHost().getName(),
                        succeeded.getHost().getName()));
            } else {
                _current.setState(DeploymentState.SUCCEEDED);
                _current.setFinished(DateTime.now());
                _current.save();
                _current = _hostQueue.poll();
                startCurrent();
            }
        } else if (message instanceof HostDeploymentNotifications.DeploymentStarted) {
            _deployment.heartbeat();
            final HostDeploymentNotifications.DeploymentStarted started = (HostDeploymentNotifications.DeploymentStarted) message;
            if (!_current.getHost().getName().equals(started.getHost().getName())) {
                Logger.warn(String.format(
                        "Received a host deployment started message from unexpected host; expected=%s, actual=%s",
                        _current.getHost().getName(),
                        started.getHost().getName()));
            } else {
                log("Deployment started for host; host=" + started.getHost().getName(), started.getHost());
                _current.setState(DeploymentState.RUNNING);
                _current.save();
            }
        } else if (message instanceof HostDeploymentNotifications.DeploymentFailed) {
            final HostDeploymentNotifications.DeploymentFailed failed = (HostDeploymentNotifications.DeploymentFailed) message;
            processHostDeploymentFailedMessage(failed);
        } else if (message instanceof HostDeploymentNotifications.DeploymentLog) {
            final HostDeploymentNotifications.DeploymentLog log = (HostDeploymentNotifications.DeploymentLog) message;
            log(log.getLog(), log.getHost());
        } else {
            unhandled(message);
        }
    }

    private void processHostDeploymentFailedMessage(final HostDeploymentNotifications.DeploymentFailed failed) {
        _deployment.heartbeat();
        if (!_current.getHost().getName().equals(failed.getHost().getName())) {
            Logger.warn(String.format(
                    "Received a host deployment failed message from unexpected host; expected=%s, actual=%s",
                    _current.getHost().getName(),
                    failed.getHost().getName()));
        } else {
            _current.setState(DeploymentState.FAILED);
            _current.setFinished(DateTime.now());
            _current.save();
        }
        _deployment.setState(DeploymentState.FAILED);
        _deployment.setFinished(DateTime.now());
        _deployment.save();
        log("Deployment has failed; cause=" + failed.getFailure(), failed.getFailure(), failed.getHost());
        self().tell(PoisonPill.getInstance(), self());
    }

    private void startCurrent() {
        if (_current == null) {
            _deployment.setFinished(DateTime.now());
            _deployment.setState(DeploymentState.SUCCEEDED);
            _deployment.save();
            final String message = "Deployment completed successfully";
            log(message, null);
            self().tell(PoisonPill.getInstance(), self());
        } else {
            final String myName;
            try {
                myName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (final UnknownHostException e) {
                throw Throwables.propagate(e);
            }
            // If the host is ourselves, then set the owner to null and wait for someone else to take over
            if (myName.equals(_current.getHost().getName())) {
                Logger.info("Found myself as the deploy target. Turning over control.");
                _deployment.refresh();
                _deployment.setDeploymentOwner(null);
                _deployment.save();

                self().tell(PoisonPill.getInstance(), self());
                return;
            }

            final ManifestHistory manifestHistory = _deployment.getManifestHistory();
            final EnvironmentType environmentType = manifestHistory
                    .getStage()
                    .getEnvironment()
                    .getEnvironmentType();
            if (environmentType.equals(EnvironmentType.ROLLER)) {
                final String actorName = "rollerDeploy-" + _current.getHost().getId();
                context()
                        .actorOf(
                                Props.create(
                                        Roller.class,
                                        () -> _hostDeploymentFactory.createRoller(
                                                _current.getHost())),
                                actorName);
            } else if (environmentType.equals(EnvironmentType.DOCKER)) {
                final String actorName = "dockerDeploy-" + _current.getHost().getId();
                final ActorRef dockerDeployActor = context()
                        .actorOf(
                                Props.create(
                                        Docker.class,
                                        () -> _hostDeploymentFactory.createDocker(
                                                _dcf.createDockerClient(_sshFactory.create(_current.getHost().getName())))),
                                actorName);
                dockerDeployActor.tell(
                        new HostDeploymentCommands.StartDeployment(
                                manifestHistory.getManifest(),
                                _current.getHost(),
                                manifestHistory.getStage()),
                        self());
            }
        }
    }

    private void log(final String message, final Host host) {
        final DeploymentLog logRecord = new DeploymentLog();
        logRecord.setDeployment(_deployment);
        logRecord.setHost(host);
        logRecord.setLogTime(DateTime.now());
        logRecord.setMessage(message);
        logRecord.save();
    }

    private void log(final String message, final Throwable exception, final Host host) {
        log(String.format("%s%n%s", message, ExceptionUtils.getStackTrace(exception)), host);
    }

    private HostDeployment _current;

    private final Deque<HostDeployment> _hostQueue;
    private final HostDeploymentFactory _hostDeploymentFactory;
    private final DeploymentClientFactory _dcf;
    private final SshSessionFactory _sshFactory;
    private final Deployment _deployment;
}
