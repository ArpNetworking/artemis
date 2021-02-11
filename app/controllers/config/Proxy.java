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

import akka.stream.javadsl.StreamConverters;
import client.ProxyClient;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.http.HttpEntity;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.shaded.ahc.io.netty.handler.codec.http.HttpHeaders;
import play.shaded.ahc.org.asynchttpclient.AsyncCompletionHandler;
import play.shaded.ahc.org.asynchttpclient.HttpResponseBodyPart;
import play.shaded.ahc.org.asynchttpclient.HttpResponseHeaders;
import play.shaded.ahc.org.asynchttpclient.HttpResponseStatus;
import play.shaded.ahc.org.asynchttpclient.Response;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Singleton;

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
        final Http.Response configResponse = response();
                _client.proxy(
                        path.startsWith("/") ? path : "/" + path,
                        request,
                        new ResponseHandler(configResponse, promise, isHttp10));
        return promise;
    }

    private final ProxyClient _client;
    private final String _baseURL;

    private static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);

    private static class ResponseHandler extends AsyncCompletionHandler<Void> {
        ResponseHandler(
                final Http.Response response,
                final CompletableFuture<Result> promise,
                final boolean isHttp10) {
            try {
                _outputStream = new PipedOutputStream();
                _inputStream = new PipedInputStream(_outputStream);
                _response = response;
                _promise = promise;
                _isHttp10 = isHttp10;
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public State onStatusReceived(final HttpResponseStatus status) {
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
        public State onHeadersReceived(final HttpResponseHeaders headers) {
            try {
                final HttpHeaders entries = headers.getHeaders();
                Optional<Long> length = Optional.empty();
                if (entries.contains(CONTENT_LENGTH)) {
                    final String clen = entries.get(CONTENT_LENGTH);
                    length = Optional.of(Long.parseLong(clen));
                }
                final String contentType;
                if (entries.get(CONTENT_TYPE) != null) {
                    contentType = entries.get(CONTENT_TYPE);
                } else if (length.isPresent() && length.get() == 0) {
                    contentType = "text/html";
                } else {
                    contentType = null;
                }

                entries.entries()
                        .stream()
                        .filter(entry -> !FILTERED_HEADERS.contains(entry.getKey()))
                        .forEach(entry -> _response.setHeader(entry.getKey(), entry.getValue()));

                if (_isHttp10) {
                    // Strip the transfer encoding header as chunked isn't supported in 1.0
                    _response.getHeaders().remove(TRANSFER_ENCODING);
                    // Strip the connection header since we don't support keep-alives in 1.0
                    _response.getHeaders().remove(CONNECTION);
                }

                final play.mvc.Result result = Results.status(_status).sendEntity(
                        new HttpEntity.Streamed(
                                StreamConverters.fromInputStream(() -> _inputStream, DEFAULT_CHUNK_SIZE),
                                length,
                                Optional.ofNullable(contentType)));

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
                throw new RuntimeException(e);
            }
            super.onThrowable(t);
        }

        @Override
        public Void onCompleted(final Response response) throws Exception {
            try {
                _outputStream.flush();
                _outputStream.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        private int _status;
        private final PipedOutputStream _outputStream;
        private final Http.Response _response;
        private final PipedInputStream _inputStream;
        private final CompletableFuture<Result> _promise;
        private final boolean _isHttp10;
        private static final int DEFAULT_CHUNK_SIZE = 8 * 1024;
        private static final Set<String> FILTERED_HEADERS = Sets.newHashSet(CONTENT_TYPE, CONTENT_LENGTH);
    }
}
