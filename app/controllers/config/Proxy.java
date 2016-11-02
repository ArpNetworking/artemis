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
package controllers.config;

import client.ProxyClient;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * A generic proxy controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class Proxy extends Controller {
    /**
     * Public constructor.
     *
     * @param baseURL the base url to proxy
     * @param client ws client to use
     */
    @AssistedInject
    public Proxy(final String baseURL, final WSClient client) {
        _baseURL = baseURL;
        _client = new ProxyClient(baseURL, client);
    }

    /**
     * Proxy a request.
     *
     * @param path the path
     * @return the proxied {@link Result}
     */
    public CompletionStage<Result> proxy(final String path) {
        LOGGER.info("proxying call from " + path + " to " + _baseURL);
        final CompletableFuture<Result> promise = new CompletableFuture<>();
        final Http.Request request = request();
        final boolean isHttp10 = request.version().equals("HTTP/1.0");
        LOGGER.info(String.format("Version=%s", request.version()));
        try {
            final PipedOutputStream outputStream = new PipedOutputStream();
            final PipedInputStream inputStream = new PipedInputStream(outputStream);
            final Http.Response configResponse = response();
                    _client.proxy(
                            path.startsWith("/") ? path : "/" + path,
                            request,
                            new ResponseHandler(outputStream, configResponse, inputStream, promise, isHttp10));
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
        return promise;
    }

    private final ProxyClient _client;
    private final String _baseURL;

    private static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);

    private static class ResponseHandler extends AsyncCompletionHandler<Void> {
        public ResponseHandler(
                final PipedOutputStream outputStream,
                final Http.Response configResponse,
                final PipedInputStream inputStream,
                final CompletableFuture<Result> promise,
                final boolean isHttp10) {
            _outputStream = outputStream;
            _configResponse = configResponse;
            _inputStream = inputStream;
            _promise = promise;
            _isHttp10 = isHttp10;
        }

        @Override
        public State onStatusReceived(final HttpResponseStatus status) throws Exception {
            _status = status.getStatusCode();
            return State.CONTINUE;
        }

        @Override
        public State onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            _outputStream.write(content.getBodyPartBytes());

            if (content.isLast()) {
                _outputStream.flush();
                _outputStream.close();
            }
            return State.CONTINUE;
        }

        @Override
        public State onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
            try {
                final HttpHeaders entries = headers.getHeaders();
                long length = -1;
                if (entries.contains(CONTENT_LENGTH)) {
                    final String clen = entries.get(CONTENT_LENGTH);
                    length = Long.parseLong(clen);
                }
                if (entries.get(CONTENT_TYPE) != null) {
                    _configResponse.setHeader(CONTENT_TYPE, entries.get(CONTENT_TYPE));
                } else if (length == 0) {
                    _configResponse.setHeader(CONTENT_TYPE, "text/html");
                }

                final Map<String, ArrayList<String>> mapped = entries.entries().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Lists.newArrayList(entry.getValue()),
                                (a, b) -> {
                                    a.addAll(b);
                                    return a;
                                }));

                for (final Map.Entry<String, ArrayList<String>> entry : mapped.entrySet()) {
                    for (final String val : entry.getValue()) {
                        _configResponse.setHeader(entry.getKey(), val);
                    }
                }
                if (_isHttp10) {
                    // Strip the transfer encoding header as chunked isn't supported in 1.0
                    _configResponse.getHeaders().remove(TRANSFER_ENCODING);
                    // Strip the connection header since we don't support keep-alives in 1.0
                    _configResponse.getHeaders().remove(CONNECTION);
                }

                final play.mvc.Result result;
                if (length >= 0) {
                    result = Results.status(_status, _inputStream, length);
                } else {
                    result = Results.status(_status, _inputStream);
                }

                _promise.complete(result);
                return State.CONTINUE;
                // CHECKSTYLE.OFF: IllegalCatch - We need to return a response no matter what
            } catch (final Throwable e) {
                // CHECKSTYLE.ON: IllegalCatch
                _promise.completeExceptionally(e);
                throw e;
            }
        }

        @Override
        public void onThrowable(final Throwable t) {
            try {
                _outputStream.close();
                _promise.completeExceptionally(t);
            } catch (final IOException e) {
                throw Throwables.propagate(e);
            }
            super.onThrowable(t);
        }

        @Override
        public Void onCompleted(final Response response) throws Exception {
            try {
                _outputStream.flush();
                _outputStream.close();
            } catch (final IOException e) {
                throw Throwables.propagate(e);
            }
            return null;
        }

        private int _status;
        private final PipedOutputStream _outputStream;
        private final Http.Response _configResponse;
        private final PipedInputStream _inputStream;
        private final CompletableFuture<Result> _promise;
        private final boolean _isHttp10;

    }
}
