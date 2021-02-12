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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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
import play.mvc.StatusHeader;
import play.shaded.ahc.io.netty.handler.codec.http.HttpHeaders;
import play.shaded.ahc.org.asynchttpclient.AsyncCompletionHandler;
import play.shaded.ahc.org.asynchttpclient.HttpResponseBodyPart;
import play.shaded.ahc.org.asynchttpclient.HttpResponseStatus;
import play.shaded.ahc.org.asynchttpclient.Response;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public CompletionStage<Result> proxy(final String path, final Http.Request request) {
        LOGGER.info("proxying call from " + path + " to " + _baseURL);
        final CompletableFuture<Result> promise = new CompletableFuture<>();
        final boolean isHttp10 = request.version().equals("HTTP/1.0");
        LOGGER.info(String.format("Version=%s", request.version()));
        _client.proxy(
                path.startsWith("/") ? path : "/" + path,
                request,
                new ResponseHandler(promise, isHttp10));
        return promise;
    }

    private final ProxyClient _client;
    private final String _baseURL;

    private static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);

    private static class ResponseHandler extends AsyncCompletionHandler<Void> {
        ResponseHandler(
                final CompletableFuture<Result> promise,
                final boolean isHttp10) {
            try {
                _outputStream = new PipedOutputStream();
                _inputStream = new PipedInputStream(_outputStream);
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
        public State onHeadersReceived(final HttpHeaders headers) {
            try {
                final StatusHeader statusHeader = Results.status(_status);

                final List<Map.Entry<String, String>> entriesList = headers.entries();
                final Multimap<String, String> entries = entriesList.stream().collect(Multimaps.toMultimap(Map.Entry<String, String>::getKey, Map.Entry::getValue, ArrayListMultimap::create));
                Optional<Long> length = Optional.empty();
                if (entries.containsKey(CONTENT_LENGTH)) {
                    final List<String> clen = new ArrayList<>(entries.get(CONTENT_LENGTH));
                    length = clen.stream().findFirst().map(Long::parseLong);
                }
                final Optional<String> contentType;
                if (entries.get(CONTENT_TYPE) != null) {
                    contentType = entries.get(CONTENT_TYPE).stream().findFirst();
                } else if (length.isPresent() && length.get() == 0) {
                    contentType = Optional.of("text/html");
                } else {
                    contentType = Optional.empty();
                }

                Result response = statusHeader.sendEntity(
                        new HttpEntity.Streamed(
                                StreamConverters.fromInputStream(() -> _inputStream, DEFAULT_CHUNK_SIZE),
                                length,
                                contentType));

                final Set<String> filterHeaders;
                if (_isHttp10) {
                    filterHeaders = HTTP_10_FILTERED_HEADERS;
                } else {
                    filterHeaders = FILTERED_HEADERS;
                }

                entries.entries()
                        .stream()
                        .filter(entry -> !filterHeaders.contains(entry.getKey()))
                        .forEach(entry -> response.withHeader(entry.getKey(), entry.getValue()));

                _promise.complete(response);
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
        private final PipedInputStream _inputStream;
        private final CompletableFuture<Result> _promise;
        private final boolean _isHttp10;
        private static final int DEFAULT_CHUNK_SIZE = 8 * 1024;
        private static final Set<String> FILTERED_HEADERS = ImmutableSet.of(CONTENT_TYPE, CONTENT_LENGTH);
        private static final Set<String> HTTP_10_FILTERED_HEADERS;
        static {{
            Set<String> tmp = Sets.newHashSet(FILTERED_HEADERS);
            // Strip the transfer encoding header as chunked isn't supported in 1.0
            tmp.add(TRANSFER_ENCODING);
            // Strip the connection header since we don't support keep-alives in 1.0
            tmp.add(CONNECTION);
            HTTP_10_FILTERED_HEADERS = ImmutableSet.copyOf(tmp);
        }}
    }
}
