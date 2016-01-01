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
package global;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import controllers.Admin;
import controllers.Api;
import controllers.Application;
import controllers.Authentication;
import controllers.Bundle;
import controllers.Deployment;
import controllers.Environment;
import controllers.Hostclass;
import controllers.Stage;
import controllers.impl.proxy.ProxyAdmin;
import controllers.impl.proxy.ProxyApi;
import controllers.impl.proxy.ProxyApplication;
import controllers.impl.proxy.ProxyAuthentication;
import controllers.impl.proxy.ProxyBundle;
import controllers.impl.proxy.ProxyDeployment;
import controllers.impl.proxy.ProxyEnvironment;
import controllers.impl.proxy.ProxyHostclass;
import controllers.impl.proxy.ProxyStage;
import play.Configuration;

/**
 * A module that proxies API calls and falls back to ProdModule for non-controller classes.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ProxyRoutesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Admin.class).to(ProxyAdmin.class);
        bind(Api.class).to(ProxyApi.class);
        bind(Application.class).to(ProxyApplication.class);
        bind(Authentication.class).to(ProxyAuthentication.class);
        bind(Bundle.class).to(ProxyBundle.class);
        bind(Deployment.class).to(ProxyDeployment.class);
        bind(Environment.class).to(ProxyEnvironment.class);
        bind(Hostclass.class).to(ProxyHostclass.class);
        bind(Stage.class).to(ProxyStage.class);
    }

    @Provides
    @Named("ArtemisProxyBaseUrl")
    String provideArtemisUrl(final Configuration config) {
        return config.getString("proxy.artemis");
    }
}
