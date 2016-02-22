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
import play.libs.F;
import play.libs.ws.WS;

import java.net.URI;
import java.util.List;

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
     */
    public ConfigServerClient(final String baseUrl) {
        super(baseUrl);
    }

    /**
     * Gets all of the packages from the server.
     *
     * @return a list of all the packages
     */
    public F.Promise<List<Package>> getPackages() {
        return WS.client()
                .url(uri("/package").toString())
                .setHeader("Accept", "application/json")
                .get()
                .map(wsResponse -> null);
    }

    /**
     * Gets the data for a particular host.
     *
     * @param hostname the host to lookup
     * @return the data for the host
     */
    public F.Promise<HostOutput> getHostData(final String hostname) {
        final URI uri = uri(String.format("/host/%s", hostname));
        return WS.url(uri.toString())
                .get()
                .map(wsResponse -> YAML_MAPPER.readValue(wsResponse.getBody(), HostOutput.class));
    }

    /**
     * Gets the data for a particular hostclass.
     *
     * @param hostclass the hostclass to lookup
     * @return the data for the hostclass
     */
    public F.Promise<HostclassOutput> getHostclassData(final String hostclass) {
        final URI uri = uri(String.format("/hostclass/%s", hostclass));
        return WS.url(uri.toString())
                .get()
                .map(wsResponse -> YAML_MAPPER.readValue(wsResponse.getBody(), HostclassOutput.class));
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