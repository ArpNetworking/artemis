package utils;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;

public class DefaultHostClassifier implements HostClassifier {
    @Override
    public String hostclassFor(final String fqdn) {
        // First strip off the domain suffix
        final int index = fqdn.indexOf('.');
        final String host;
        if (index == -1) {
            host = fqdn;
        } else {
            host = fqdn.substring(0, index);
        }

        final String domain = fqdn.substring(index + 1);
        final List<String> hostSplit = Lists.newArrayList(
                Splitter.on(new HostclassSplitterMatcher())
                        .omitEmptyStrings()
                        .trimResults()
                        .splitToList(host));
        final List<String> domainSplit = Lists.newArrayList(
                Splitter.on('.')
                        .omitEmptyStrings()
                        .trimResults()
                        .split(domain));

        hostSplit.addAll(domainSplit.subList(0, domainSplit.size() - _stripSuffixDomains));
        return Joiner.on("_").skipNulls().join(hostSplit);
    }

    private DefaultHostClassifier(final Builder builder) {
        _stripSuffixDomains = builder._stripSuffixDomains;
    }

    private final int _stripSuffixDomains;

    private static class HostclassSplitterMatcher extends CharMatcher {
        @Override
        public boolean matches(final char c) {
            return Character.isDigit(c) || c == '-' || c == '_' || c == '.';
        }
    }


    /**
     * Implementation of the builder pattern for {@link DefaultHostClassifier}.
     */
    public static class Builder extends OvalBuilder<DefaultHostClassifier> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(DefaultHostClassifier::new);
        }

        public Builder setStripSuffixDomains(final int value) {
            _stripSuffixDomains = value;
            return this;
        }

        private int _stripSuffixDomains = 0;
    }
}
