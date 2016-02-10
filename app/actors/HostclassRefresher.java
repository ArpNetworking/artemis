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

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import client.HostProvider;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import models.Host;
import models.Hostclass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.libs.F;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

/**
 * Refreshes hosts on a periodic basis from a host provider.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class HostclassRefresher extends UntypedActor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostclassRefresher.class);
    private final HostProvider _hostProvider;

    /**
     * Creates a {@link Props} to create this actor.
     *
     * @param config The configuration to use
     * @return a new {@link Props}
     */
    public static Props props(final Configuration config) {
        return Props.create(HostclassRefresher.class, config);
    }

    /**
     * Public constructor.
     *
     * @param config The configuration to use
     * @param hostProvider The host provider to get host data from
     */
    @Inject
    public HostclassRefresher(final Configuration config, final HostProvider hostProvider) {
        context().system().scheduler().schedule(
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                FiniteDuration.apply(12, TimeUnit.HOURS),
                self(),
                new RefreshHostclassesMessage(),
                context().dispatcher(),
                self());
        _hostProvider = hostProvider;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof RefreshHostclassesMessage) {
            final F.Promise<HostclassListMessage> messagePromise = _hostProvider.getHosts()
                    .recover(this::recoverHostclassListLookupFailure)
                    .map(HostclassListMessage::new);
            Patterns.pipe(messagePromise.wrapped(), context().dispatcher()).to(self(), self());
        } else if (message instanceof HostclassListMessage) {
            final HostclassListMessage listMessage = (HostclassListMessage) message;
            final Set<String> hosts = listMessage.getHosts();
            for (final String name : hosts) {
                final String hostclassName = hostclassFromHost(name);
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
        } else {
            LOGGER.warn(String.format("Unhandled message; message=%s", message));
            unhandled(message);
        }
    }

    private Set<String> recoverHostclassListLookupFailure(final Throwable throwable) {
        LOGGER.warn("Error while trying to lookup hostclass list, skipping this run");
        return Collections.emptySet();
    }

    private String hostclassFromHost(final String name) {
        // First strip off the colo suffix
        final int index = name.lastIndexOf('.');
        if (index == -1) {
            return name;
        }

        final String colo = name.substring(index + 1);
        final String noColo = name.substring(0, index);
        final List<String> split = Lists.newArrayList(
                Splitter.on(new HostclassSplitterMatcher())
                        .omitEmptyStrings()
                        .trimResults()
                        .split(noColo));
        split.add(colo);
        return Joiner.on("_").skipNulls().join(split);
    }

    private static class HostclassSplitterMatcher extends CharMatcher {
        @Override
        public boolean matches(final char c) {
            return Character.isDigit(c) || c == '-' || c == '_';
        }
    }

    private static class RefreshHostclassesMessage {}
    private static class HostclassListMessage {
        private final Set<String> _hosts;

        public HostclassListMessage(final Set<String> hosts) {
            _hosts = hosts;
        }

        public Set<String> getHosts() {
            return _hosts;
        }
    }
}
