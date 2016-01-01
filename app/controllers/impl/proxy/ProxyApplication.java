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
import play.libs.F;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Generic application actions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class ProxyApplication extends ArtemisProxy implements controllers.Application {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     */
    @Inject
    public ProxyApplication(@Named("ArtemisProxyBaseUrl") final String baseURL) {
        super(baseURL);
    }

    @Override
    public F.Promise<Result> main() {
        return proxy();
    }
}
