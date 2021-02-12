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
package controllers;

import com.google.inject.name.Named;
import controllers.config.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.mvc.Http;
import play.mvc.Result;

import java.net.http.HttpRequest;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

/**
 * Acts as a proxy server for cross DC Artemis requests.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ArtemisProxy extends Proxy {
    /**
     * Public constructor.
     *
     * @param baseURL the base url to proxy to
     * @param client ws client to use
     */
    @Inject
    public ArtemisProxy(@Named("ArtemisProxyBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
    }

    /**
     * Proxy a reqeust.
     *
     * @return a {@link Result}
     */
    public CompletionStage<Result> proxy(final Http.Request request) {
        return super.proxy(request.uri(), request);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisProxy.class);
}
