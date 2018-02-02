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
package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.groupon.deployment.FleetDeploymentCommands;
import com.groupon.deployment.fleet.FleetDeploymentFactory;
import com.groupon.deployment.fleet.Sequential;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.Deployment;
import models.DeploymentState;
import models.HostDeployment;
import models.Manifest;
import models.ManifestHistory;
import models.Stage;
import org.joda.time.DateTime;
import play.Logger;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

/**
 * Manages deployments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class DeployManager extends AbstractActor {
    /**
     * Public constructor.
     *
     * @param fleetDeploymentFactory a factory to create a fleet deployment.
     */
    @Inject
    public DeployManager(final FleetDeploymentFactory fleetDeploymentFactory) {
        _fleetDeploymentFactory = fleetDeploymentFactory;
        context().system().scheduler().schedule(
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                FiniteDuration.apply(30, TimeUnit.SECONDS),
                self(),
                new DeploymentSweep(), context().dispatcher(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FleetDeploymentCommands.DeployStage.class, this::deployStage)
                .match(DeploymentSweep.class, sweep -> sweepForStuckDeployments())
                .build();
    }

    private void sweepForStuckDeployments() {
        final Deployment stuckDeployment = Deployment.getAndLockStuckDeployment();
        if (stuckDeployment != null) {
            Logger.info(String.format("Found stuck deployment, resuming; id=%d", stuckDeployment.getId()));
            startDeployment(stuckDeployment);
        } else {
            Logger.info("Found no stuck deployments");
        }
    }

    private void deployStage(final FleetDeploymentCommands.DeployStage deployStageMessage) {
        Logger.info("DeployManager starting a fleet deployment");
        // TODO(barp): make sure the stage is locked [Artemis-?]
        // TODO(barp): try grabbing the deployment lock [Artemis-?]
        // TODO(barp): lookup any existing child actor deploying that stage [Artemis-?]
        final Manifest manifest = deployStageMessage.getDeployment();
        final Stage stage = deployStageMessage.getStage();

        try (Transaction transaction = Ebean.beginTransaction()) {
            final ManifestHistory history = Stage.applyManifestToStage(stage, manifest);
            final Deployment deployment = new Deployment();
            deployment.setStart(DateTime.now());
            deployment.setInitiator(deployStageMessage.getInitiator());
            deployment.setState(DeploymentState.NOT_STARTED);
            deployment.setDeploymentOwner(InetAddress.getLocalHost().getCanonicalHostName());
            deployment.setHeartbeat(DateTime.now());
            deployment.save();
            // Render the hosts into the deployment
            final List<HostDeployment> hostDeployments = Lists.newArrayList();
            stage.getHostclasses().forEach(hc -> {
                hc.getHosts().forEach(host -> {
                    final HostDeployment hd = new HostDeployment();
                    hd.setDeployment(deployment);
                    hd.setHeartbeat(DateTime.now());
                    hd.setHost(host);
                    hd.setState(DeploymentState.NOT_STARTED);
                    hd.save();
                    hostDeployments.add(hd);
                });
            });
            deployment.setHostStates(hostDeployments);
            deployment.setManifestHistory(history);
            deployment.save();
            transaction.commit();
            startDeployment(deployment);
            sender().tell(deployment, self());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startDeployment(final Deployment deployment) {
        context()
                .system()
                .actorOf(Props.create(Sequential.class, () -> _fleetDeploymentFactory.create(deployment)),
                         "deploy-" + deployment.getId());
    }

    private final FleetDeploymentFactory _fleetDeploymentFactory;

    private static final class DeploymentSweep {}
}
