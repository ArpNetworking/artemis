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

import com.google.common.base.Throwables;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base class for http client that supports a base url.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ClientBase {
    /**
     * Public constructor.
     *
     * @param baseUrl the base URL for all relative addresses
     * @param client ws client to use
     */
    @AssistedInject
    public ClientBase(@Assisted final String baseUrl, final WSClient client) {
        _baseUrl = baseUrl;
        _client = client;
    }

    /**
     * Computes an aboslute URI from a relative path.
     *
     * @param relativePath the relative path
     * @return the absolute URI
     */
    protected URI uri(final String relativePath) {
        try {
            return new URI(_baseUrl).resolve(relativePath);
        } catch (final URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Gets the WSClient.
     *
     * @return injected WSClient
     */
    protected WSClient client() {
        return _client;
    }

    /**
     * Creates a URIBuilder with the base path and a relative path resolved.
     *
     * @param relativePath the relative path
     * @return a new {@link URIBuilder}
     */
    protected URIBuilder uriBuilder(final String relativePath) {
        final URI tokenUri;
        try {
            tokenUri = new URI(_baseUrl).resolve(relativePath);
        } catch (final URISyntaxException e) {
            LOGGER.error(String.format("Unable to parse baseURL; baseURL=%s", _baseUrl), e);
            throw Throwables.propagate(e);
        }
        return new URIBuilder(tokenUri);
    }

    private final String _baseUrl;
    private final WSClient _client;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBase.class);
}
