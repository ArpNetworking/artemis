/**
 * Copyright 2017 Brandon Arp
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
package client;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Retrieves hosts from multiple HostProviders.
 *
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
public final class MultiHostProvider implements HostProvider {
    @Override
    public CompletionStage<Set<String>> getHosts() {
        final CompletableFuture<Set<String>> future = new CompletableFuture<>();
        final AtomicInteger outstanding = new AtomicInteger(_providers.size());
        final Set<String> hosts = Sets.newConcurrentHashSet();

        final Consumer<Set<String>> action = results -> {
            final int remaining = outstanding.decrementAndGet();
            hosts.addAll(results);
            if (remaining == 0) {
                future.complete(hosts);
            }
        };

        for (final HostProvider provider : _providers) {
            provider.getHosts().thenAccept(action);
        }

        return future;
    }

    private MultiHostProvider(final Builder builder) {
        _providers = builder._providers;
    }

    private ImmutableList<HostProvider> _providers;

    /**
     * Implementation of the builder pattern for {@link MultiHostProvider}.
     *
     * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
     */
    public static class Builder extends OvalBuilder<MultiHostProvider> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(MultiHostProvider::new);
        }

        /**
         * Sets the list of providers. Required. Cannot be null.
         *
         * @param value List of providers
         * @return this {@link Builder}
         */
        public Builder setProviders(final ImmutableList<HostProvider> value) {
            _providers = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private ImmutableList<HostProvider> _providers = null;
    }
}
