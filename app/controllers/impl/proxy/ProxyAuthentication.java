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
package controllers.impl.proxy;

import controllers.ArtemisProxy;
import controllers.Authentication;
import play.libs.ws.WSClient;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Holds methods for authentication.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class ProxyAuthentication extends ArtemisProxy implements Authentication {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     * @param client ws client to use
     */
    @Inject
    public ProxyAuthentication(@Named("ArtemisProxyBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
    }

    @Override
    public CompletionStage<Result> auth(final String redirectUrl, final Http.Request request) {
        return proxy(request);
    }

    @Override
    public CompletionStage<Result> finishAuth(final String code, final String state, final Http.Request request) {
        return proxy(request);
    }

    @Override
    public CompletionStage<Result> logout(final Http.Request request) {
        return proxy(request);
    }
}
