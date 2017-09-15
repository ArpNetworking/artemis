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

import akka.util.ByteString;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.mvc.Http;

import java.net.URI;
import java.util.Map;

/**
 * A simple proxy client.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ProxyClient extends ClientBase {
    /**
     * Public constructor.
     *
     * @param baseUrl the base url for the proxy
     * @param client ws client to use
     */
    @AssistedInject
    public ProxyClient(@Assisted final String baseUrl, final WSClient client) {
        super(baseUrl, client);
    }

    /**
     * Proxy a request.
     *
     * @param path the path to proxy
     * @param request the request
     * @param handler a handler to execute as a callback
     * @param <T> the type of the handler
     */
    public <T> void proxy(
            final String path,
            final play.mvc.Http.Request request,
            final AsyncHandler<T> handler) {

        final ByteString body = request.body().asBytes();
        final URI uri = uri(path);
        LOGGER.info(String.format("Proxy url: %s", uri));

        final RequestBuilder builder = new RequestBuilder();
        for (final Map.Entry<String, String[]> entry : request.queryString().entrySet()) {
            for (final String val : entry.getValue()) {
                builder.addQueryParam(entry.getKey(), val);
            }
        }

        builder.setUrl(uri.toString());
        builder.setMethod(request.method());
        for (final Map.Entry<String, String[]> entry : request.headers().entrySet()) {
            for (final String val : entry.getValue()) {
                builder.setHeader(entry.getKey(), val);
            }
        }
        if (body != null) {
            builder.setBody(body.asByteBuffer());
        }

        final Object underlying = client().getUnderlying();
        if (underlying instanceof AsyncHttpClient) {
            final AsyncHttpClient client = (AsyncHttpClient) underlying;
            client.executeRequest(builder.build(), handler);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyClient.class);
}
