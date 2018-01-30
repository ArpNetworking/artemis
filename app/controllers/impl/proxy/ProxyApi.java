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

import controllers.Api;
import controllers.ArtemisProxy;
import play.libs.ws.WSClient;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * JSON REST Apis.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class ProxyApi extends ArtemisProxy implements Api {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     * @param client ws client to use
     */
    @Inject
    public ProxyApi(@Named("ArtemisProxyBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
    }

    @Override
    public CompletionStage<Result> hostclassSearch(final String query) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> packageSearch(final String query) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> environmentSearch(final String query) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> getStages(final String envName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> updateStagePackageVersions(final String envName, final String stageName) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> deploymentLog(final long deploymentId) {
        return proxy();
    }

    @Override
    public CompletionStage<Result> getReleasePreview(final String envName, final String version) {
        return proxy();
    }
}
