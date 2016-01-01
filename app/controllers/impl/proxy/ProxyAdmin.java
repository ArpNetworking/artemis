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

import com.google.inject.Singleton;
import com.google.inject.name.Named;
import controllers.Admin;
import controllers.ArtemisProxy;
import play.libs.F;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * Administrative functions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class ProxyAdmin extends ArtemisProxy implements Admin {
    /**
     * Public constructor.
     *
     * @param baseURL the base proxy url
     */
    @Inject
    public ProxyAdmin(@Named("ArtemisProxyBaseUrl") final String baseURL) {
        super(baseURL);
    }

    @Override
    public F.Promise<Result> index() {
        return proxy();
    }

    @Override
    public F.Promise<Result> refreshPackages() {
        return proxy();
    }
}
