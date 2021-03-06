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
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.Terminated;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import models.Deployment;
import models.DeploymentLog;
import models.Host;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An actor to relay log messages for a deploy.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DeployLogRelay extends AbstractActor {
    /**
     * Creates a {@link Props} to create this actor.
     *
     * @param subscriber actor to send data to
     * @param deploymentId the deployment id
     * @return a new {@link Props}
     */
    public static Props props(final ActorRef subscriber, final long deploymentId) {
        return Props.create(DeployLogRelay.class, subscriber, deploymentId);
    }

    /**
     * Public constructor.
     *
     * @param subscriber actor to send data to
     * @param deploymentId the deployment id
     */
    public DeployLogRelay(final ActorRef subscriber, final long deploymentId) {
        _subscriber = subscriber;
        context().watch(_subscriber);
        _deployment = Deployment.getById(deploymentId);
        context().system().scheduler().scheduleWithFixedDelay(
                FiniteDuration.apply(1, TimeUnit.SECONDS),
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                self(),
                "check",
                context().dispatcher(),
                self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("check", check -> {
                    final List<DeploymentLog> logs = DeploymentLog.getLogsSince(_deployment, DeploymentLog.ref(_lastId));

                    final ObjectNode node = JsonNodeFactory.instance.objectNode();
                    final ArrayNode messages = node.putArray("messages");
                    logs.forEach(logLine -> messages.addObject()
                            .put("line", logLine.getMessage())
                            .put("timestamp", logLine.getLogTime().toString())
                            .put("host", Optional.ofNullable(logLine.getHost()).map(Host::getName).orElse("Deployment")));
                    pushLog(node);
                    _lastId = Optional.ofNullable(Iterables.getLast(logs, null)).map(DeploymentLog::getId).orElse(_lastId);
                    _deployment.refresh();
                    _logger.info("Refreshed the deploy, finished=" + _deployment.getFinished());
                    if (_deployment.getFinished() != null) {
                        pushEnd();
                        context().system().scheduler().scheduleOnce(FiniteDuration.apply(10, TimeUnit.SECONDS),
                                self(),
                                "close",
                                context().dispatcher(),
                                self());
                    }
                })
                .matchEquals("close", msg -> {
                    _subscriber.tell(PoisonPill.getInstance(), self());
                })
                .match(Terminated.class, terminated -> {
                    if (terminated.actor().equals(_subscriber)) {
                        self().tell(PoisonPill.getInstance(), self());
                    }
                })
                .build();
    }

    private void pushLog(final JsonNode node) {
        final StringBuilder builder = new StringBuilder();
        builder.append("event: log\n");
        builder.append("data: ");
        final Iterable<String> split = Splitter.on("\n").split(node.toString());
        Joiner.on("\ndata: ").appendTo(builder, split);
        builder.append("\n\n");

        _subscriber.tell(builder.toString(), self());
    }

    private void pushEnd() {
        _subscriber.tell("event: end\ndata: \n\n", self());
    }

    private long _lastId = 0;
    private final Logger _logger = LoggerFactory.getLogger(DeployLogRelay.class);
    private final ActorRef _subscriber;
    private final Deployment _deployment;
}
