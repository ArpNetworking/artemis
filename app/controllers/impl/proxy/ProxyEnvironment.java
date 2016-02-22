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
import controllers.Environment;
import play.libs.F;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for Environments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class ProxyEnvironment extends ArtemisProxy implements Environment {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     */
    @Inject
    public ProxyEnvironment(@Named("ArtemisProxyBaseUrl") final String baseURL) {
        super(baseURL);
    }

    @Override
    public F.Promise<Result> detail(final String name) {
        return proxy();
    }

    @Override
    public F.Promise<Result> newEnvironment(final String parentEnv) {
        return proxy();
    }


    @Override
    public F.Promise<Result> create() {
        return proxy();
    }

    @Override
    public F.Promise<Result> save(final String envName) {
        return proxy();
    }

    @Override
    public F.Promise<Result> createRelease(final String envName) {
        return proxy();
    }

    @Override
    public F.Promise<Result> prepareRelease(final String envName) {
        return proxy();
    }
}