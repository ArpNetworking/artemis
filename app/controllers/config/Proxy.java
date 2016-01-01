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

import akka.dispatch.Futures;
import client.ProxyClient;
import com.google.common.base.Throwables;
import com.google.inject.Singleton;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.http.Writeable;
import play.api.libs.iteratee.Enumerator$;
import play.api.mvc.Results$;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import scala.concurrent.Promise;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

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
     */
    public Proxy(final String baseURL) {
        _baseURL = baseURL;
        _client = new ProxyClient(baseURL);
    }

    /**
     * Proxy a request.
     *
     * @param path the path
     * @return the proxied {@link Result}
     */
    public F.Promise<Result> proxy(final String path) {
        LOGGER.info("proxying call from " + path + " to " + _baseURL);
        final Promise<Result> promise = Futures.promise();
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
        return F.Promise.wrap(promise.future());
    }

    private final ProxyClient _client;
    private final String _baseURL;

    private static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);

    private static class ResponseHandler extends AsyncCompletionHandler<Void> {
        public ResponseHandler(
                final PipedOutputStream outputStream,
                final Http.Response configResponse,
                final PipedInputStream inputStream,
                final Promise<Result> promise,
                final boolean isHttp10) {
            _outputStream = outputStream;
            _configResponse = configResponse;
            _inputStream = inputStream;
            _promise = promise;
            _isHttp10 = isHttp10;
        }

        @Override
        public STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
            _status = status.getStatusCode();
            return STATE.CONTINUE;
        }

        @Override
        public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            _outputStream.write(content.getBodyPartBytes());

            if (content.isLast()) {
                _outputStream.flush();
                _outputStream.close();
            }
            return STATE.CONTINUE;
        }

        @Override
        public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
            try {
                final FluentCaseInsensitiveStringsMap entries = headers.getHeaders();
                if (entries.getFirstValue(CONTENT_TYPE) != null) {
                    _configResponse.setHeader(CONTENT_TYPE, entries.getFirstValue(CONTENT_TYPE));
                } else if (entries.getFirstValue(CONTENT_LENGTH) != null) {
                    final String clen = entries.getFirstValue(CONTENT_LENGTH);
                    final int length = Integer.parseInt(clen);
                    if (length == 0) {
                        _configResponse.setHeader(CONTENT_TYPE, "text/html");
                    }
                }
                for (final Map.Entry<String, List<String>> entry : entries.entrySet()) {
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

                final play.api.mvc.Result result;
                if (entries.containsKey(TRANSFER_ENCODING)
                        && entries.getFirstValue(TRANSFER_ENCODING).equalsIgnoreCase("chunked")
                        && !_isHttp10) {
                    result = Results$.MODULE$.Status(_status)
                            .chunked(
                                    Enumerator$.MODULE$.fromStream(_inputStream, 4096,
                                            play.api.libs.concurrent.Execution
                                                    .defaultContext()),
                                    Writeable.wBytes());
                } else {
                    result = Results$.MODULE$.Status(_status)
                            .feed(Enumerator$.MODULE$.fromStream(_inputStream, 4096,
                                    play.api.libs.concurrent.Execution
                                            .defaultContext()),
                                    Writeable.wBytes());
                }

                final Result r = () -> result;
                _promise.success(r);
                return STATE.CONTINUE;
                // CHECKSTYLE.OFF: IllegalCatch - We need to return a response no matter what
            } catch (final Throwable e) {
                // CHECKSTYLE.ON: IllegalCatch
                _promise.failure(e);
                throw e;
            }
        }

        @Override
        public void onThrowable(final Throwable t) {
            try {
                _outputStream.close();
                _promise.failure(t);
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
        private final Promise<Result> _promise;
        private final boolean _isHttp10;

    }
}
