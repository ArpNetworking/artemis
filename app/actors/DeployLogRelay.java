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

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import models.Deployment;
import models.DeploymentLog;
import models.Host;
import play.api.libs.iteratee.Concurrent;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An actor to relay log messages for a deploy.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DeployLogRelay extends UntypedActor {
    /**
     * Creates a {@link Props} to create this actor.
     *
     * @param channel Play channel to send data to
     * @param deploymentId the deployment id
     * @return a new {@link Props}
     */
    public static Props props(final Concurrent.Channel<String> channel, final long deploymentId) {
        return Props.create(DeployLogRelay.class, channel, deploymentId);
    }

    /**
     * Public constructor.
     *
     * @param channel Play channel to send data to
     * @param deploymentId the deployment id
     */
    public DeployLogRelay(final Concurrent.Channel<String> channel, final long deploymentId) {
        _channel = channel;
        _deployment = Deployment.getById(deploymentId);
        context().system().scheduler().schedule(
                FiniteDuration.apply(1, TimeUnit.SECONDS),
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                self(),
                "check",
                context().dispatcher(),
                self());
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if ("check".equals(message)) {
            final List<DeploymentLog> logs = DeploymentLog.getLogsSince(_deployment, DeploymentLog.ref(_lastId));

            final ObjectNode node = JsonNodeFactory.instance.objectNode();
            final ArrayNode messages = node.putArray("messages");
            logs.forEach((logLine) -> messages.addObject()
                    .put("line", logLine.getMessage())
                    .put("timestamp", logLine.getLogTime().toString())
                    .put("host", Optional.fromNullable(logLine.getHost()).transform(Host::getName).or("Deployment")));
            pushLog(node);
            _lastId = Optional.fromNullable(Iterables.getLast(logs, null)).transform(DeploymentLog::getId).or(_lastId);
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
        } else if ("close".equals(message)) {
            _channel.eofAndEnd();
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    private void pushLog(final JsonNode node) {
        final StringBuilder builder = new StringBuilder();
        builder.append("event: log\n");
        builder.append("data: ");
        final Iterable<String> split = Splitter.on("\n").split(node.toString());
        Joiner.on("\ndata: ").appendTo(builder, split);
        builder.append("\n\n");

        _channel.push(builder.toString());
    }

    private void pushEnd() {
        _channel.push("event: end\ndata: \n\n");
    }

    private long _lastId = 0;
    private final Logger _logger = LoggerFactory.getLogger(DeployLogRelay.class);
    private final Concurrent.Channel<String> _channel;
    private final Deployment _deployment;
}
