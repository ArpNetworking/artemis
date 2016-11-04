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

        final Consumer<Set<String>> action = (results) -> {
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
        public void setProviders(final ImmutableList<HostProvider> value) {
            _providers = value;
        }

        @NotNull
        @NotEmpty
        private ImmutableList<HostProvider> _providers = null;
    }
}
