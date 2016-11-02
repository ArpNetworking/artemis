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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import controllers.ArtemisProxy;
import controllers.Stage;
import play.libs.ws.WSClient;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.util.concurrent.CompletionStage;

/**
 * Holds stage actions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class ProxyStage extends ArtemisProxy implements Stage {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     * @param client ws client to use
     */
    @Inject
    public ProxyStage(@Named("ArtemisProxyBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
    }

    @Override
    public CompletionStage<Result> detail(final String envName, final String stageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> addHostclass(final String envName, final String stageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> removeHostclass(final String envName, final String stageName, final String hostclassName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> prepareDeploy(final String envName, final String stageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> previewDeploy(final String envName, final String stageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> confirmDeploy(final String envName, final String stageName, final long version, final long manifestId) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> prepareDeployManifest(final String envName, final String stageName, final long manifestId) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> create(final String envName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> promote(final String sourceEnvName, final String sourceStageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> synchronize(final String sourceEnvName, final String sourceStageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> save(final String envName, final String stageName) {
        return proxy();
    }
}
