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
package client;

import api.HostOutput;
import api.HostclassOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import play.libs.ws.WSClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Serves as a client for accessing the config server.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConfigServerClient extends ClientBase {

    /**
     * Public constructor.
     *
     * @param baseUrl the base URL
     * @param client ws client to use
     */
    @AssistedInject
    public ConfigServerClient(@Assisted final String baseUrl, final WSClient client) {
        super(baseUrl, client);
    }

    /**
     * Gets all of the packages from the server.
     *
     * @return a list of all the packages
     */
    public CompletionStage<List<Package>> getPackages() {
        return client()
                .url(uri("/package").toString())
                .setHeader("Accept", "application/json")
                .get()
                .thenApply(wsResponse -> null);
    }

    /**
     * Gets the data for a particular host.
     *
     * @param hostname the host to lookup
     * @return the data for the host
     */
    public CompletionStage<HostOutput> getHostData(final String hostname) {
        final URI uri = uri(String.format("/host/%s", hostname));
        return client().url(uri.toString())
                .get()
                .thenApply(wsResponse -> {
                    try {
                        return YAML_MAPPER.readValue(wsResponse.getBody(), HostOutput.class);
                    } catch (final IOException e) {
                        throw Throwables.propagate(e);
                    }
                });
    }

    /**
     * Gets the data for a particular hostclass.
     *
     * @param hostclass the hostclass to lookup
     * @return the data for the hostclass
     */
    public CompletionStage<HostclassOutput> getHostclassData(final String hostclass) {
        final URI uri = uri(String.format("/hostclass/%s", hostclass));
        return client().url(uri.toString())
                .get()
                .thenApply(wsResponse -> {
                    try {
                        return YAML_MAPPER.readValue(wsResponse.getBody(), HostclassOutput.class);
                    } catch (final IOException e) {
                        throw Throwables.propagate(e);
                    }
                });
    }

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Representation of a package.
     */
    public static class Package {

        /**
         * Public constructor.
         *
         * @param name the package name
         * @param versions the list of versions found
         */
        public Package(final String name, final List<String> versions) {
            _name = name;
            _versions = versions;
        }

        public String getName() {
            return _name;
        }

        public List<String> getVersions() {
            return _versions;
        }

        private final String _name;
        private final List<String> _versions;
    }
}
