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
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import client.HostProvider;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import models.Host;
import models.Hostclass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;
import utils.HostClassifier;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

/**
 * Refreshes hosts on a periodic basis from a host provider.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class HostclassRefresher extends AbstractActor {

    /**
     * Creates a {@link Props} to create this actor.
     *
     * @param config The configuration to use
     * @param hostProvider Provider to lookup hosts
     * @return a new {@link Props}
     */
    public static Props props(final Config config, final HostProvider hostProvider, final HostClassifier hostClassifier) {
        return Props.create(HostclassRefresher.class, () -> new HostclassRefresher(config, hostProvider, hostClassifier));
    }

    /**
     * Public constructor.
     *
     * @param config The configuration to use
     * @param hostProvider The host provider to get host data from
     */
    @Inject
    public HostclassRefresher(final Config config, final HostProvider hostProvider, final HostClassifier hostClassifier) {
        context().system().scheduler().scheduleWithFixedDelay(
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                FiniteDuration.apply(12, TimeUnit.HOURS),
                self(),
                new RefreshHostclassesMessage(),
                context().dispatcher(),
                self());
        _hostProvider = hostProvider;
        _hostClassifier = hostClassifier;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RefreshHostclassesMessage.class, refresh -> {
                    final CompletionStage<HostclassListMessage> messagePromise = _hostProvider.getHosts()
                            .exceptionally(this::recoverHostclassListLookupFailure)
                            .thenApply(HostclassListMessage::new);
                    Patterns.pipe(messagePromise, context().dispatcher()).to(self(), self());
                })
                .match(HostclassListMessage.class, listMessage -> {
                    final Set<String> hosts = listMessage.getHosts();
                    for (final String name : hosts) {
                        final String hostclassName = _hostClassifier.hostclassFor(name);
                        Hostclass hostclass = Hostclass.getByName(hostclassName);
                        if (hostclass == null) {
                            try {
                                hostclass = new Hostclass();
                                hostclass.setName(hostclassName);
                                hostclass.setParent(null);
                                hostclass.save();
                            } catch (final PersistenceException e) {
                                LOGGER.warn(String.format("Problem creating hostclass; name=%s", name), e);
                            }
                        }

                        Host host = Host.getByName(name);
                        if (host == null) {
                            host = new Host();
                            host.setHostclass(hostclass);
                            host.setName(name);
                            host.save();
                        }
                    }
                })
                .build();
    }

    private Set<String> recoverHostclassListLookupFailure(final Throwable throwable) {
        LOGGER.warn("Error while trying to lookup hostclass list, skipping this run", throwable);
        return Collections.emptySet();
    }

    private final HostClassifier _hostClassifier;
    private final HostProvider _hostProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(HostclassRefresher.class);

    private static class RefreshHostclassesMessage {}
    private static class HostclassListMessage {
        private final Set<String> _hosts;

        HostclassListMessage(final Set<String> hosts) {
            _hosts = hosts;
        }

        public Set<String> getHosts() {
            return _hosts;
        }
    }
}
