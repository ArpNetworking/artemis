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

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import client.DeploymentClientFactory;
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
import com.groupon.deployment.host.Rpm;
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
public class Sequential extends AbstractActor {
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
            throw new RuntimeException(e);
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
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("start", start -> {
                    _current = _hostQueue.poll();
                    log("Deployment starting", null);
                    startCurrent();
                })
                .match(HostDeploymentNotifications.DeploymentSucceeded.class, succeeded -> {
                    // Only update if the host is the currently deploying host
                    _deployment.heartbeat();
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
                })
                .match(HostDeploymentNotifications.DeploymentStarted.class, started -> {
                    _deployment.heartbeat();
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
                })
                .match(HostDeploymentNotifications.DeploymentFailed.class, this::processHostDeploymentFailedMessage)
                .match(HostDeploymentNotifications.DeploymentLog.class, log -> log(log.getLog(), log.getHost()))
                .build();
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
                throw new RuntimeException(e);
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
            final String actorName;
            switch (environmentType) {
                case ROLLER:
                    actorName = "rollerDeploy-" + _current.getHost().getId();
                    context()
                            .actorOf(
                                    Props.create(
                                            Roller.class,
                                            () -> _hostDeploymentFactory.createRoller(
                                                    _current.getHost())),
                                    actorName);
                        break;
                case DOCKER:
                    actorName = "dockerDeploy-" + _current.getHost().getId();
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
                        break;
                case RPM:
                    actorName = "rpmDeploy-" + _current.getHost().getId();
                    context()
                            .actorOf(
                                    Props.create(
                                            Rpm.class,
                                            () -> _hostDeploymentFactory.createRpm(
                                                    _current.getHost(),
                                                    _deployment)),
                                    actorName);
                    break;
                default:
                    log(
                            String.format(
                                    "Unable to start deployment: Unknown environment type [%s]",
                                    environmentType.toString()),
                            _current.getHost());
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
