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
import controllers.Deployment;
import play.libs.ws.WSClient;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Deployment controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class ProxyDeployment extends ArtemisProxy implements Deployment {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     * @param client ws client to use
     */
    @Inject
    public ProxyDeployment(@Named("ArtemisProxyBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
    }
    @Override
    public CompletionStage<Result> detail(final long deploymentId) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> log(final long deploymentId) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> diff(final long deploymentId) {
        return proxy();
    }
}
