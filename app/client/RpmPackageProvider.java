/**
 * Copyright 2016 Brandon Arp
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
import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.net.URI;
import java.util.concurrent.CompletionStage;

/**
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
public final class RpmPackageProvider implements PackageProvider {
    @Override
    public CompletionStage<PackageListResponse> getAllPackages() {
        return null;
//        return _client
//                .url(_baseUrl + "/repodata/repomd.xml")
//                .get()
//                .thenCompose(this::parseRepoMd)
//                .then;
    }

    private CompletionStage<RepoMetadata> parseRepoMd(final WSResponse response) {
        return null;
    }

    private RpmPackageProvider(final Builder builder) {
        _client = builder._client;
        _baseUrl = builder._baseUrl;
    }

    private final WSClient _client;
    private final URI _baseUrl;

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    /**
     * Implementation of the Builder pattern for RpmPackageProvider.
     *
     * @author Brandon Arp (brandon dot arp at smartsheet dot com)
     */
    public static final class Builder extends OvalBuilder<RpmPackageProvider> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(RpmPackageProvider::new);
        }

        /**
         * Sets the base URL of the Foreman API. Required. Cannot be null. Cannot be empty.
         *
         * @param value The url.
         * @return This {@link Builder}.
         */
        public Builder setBaseUrl(final URI value) {
            _baseUrl = value;
            return this;
        }

        /**
         * Sets the {@link play.libs.ws.WSClient} to use to make the calls. Required. Cannot be null.
         *
         * @param value The WSClient.
         * @return this {@link Builder}
         */
        public Builder setClient(final WSClient value) {
            _client = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private URI _baseUrl;
        @JacksonInject
        @NotNull
        private WSClient _client;
    }

    private static final class RepoMetadata {

    }
}
