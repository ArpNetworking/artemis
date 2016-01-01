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
import controllers.Admin;
import controllers.Api;
import controllers.Application;
import controllers.Authentication;
import controllers.Bundle;
import controllers.Deployment;
import controllers.Environment;
import controllers.Hostclass;
import controllers.Stage;
import controllers.impl.StandardAdmin;
import controllers.impl.StandardApi;
import controllers.impl.StandardApplication;
import controllers.impl.StandardAuthentication;
import controllers.impl.StandardBundle;
import controllers.impl.StandardDeployment;
import controllers.impl.StandardEnvironment;
import controllers.impl.StandardHostclass;
import controllers.impl.StandardStage;

/**
 * A module that provides standard APIs for controllers and falls back to ProdModule for non-controller classes.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StandardRoutesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Admin.class).to(StandardAdmin.class);
        bind(Api.class).to(StandardApi.class);
        bind(Application.class).to(StandardApplication.class);
        bind(Authentication.class).to(StandardAuthentication.class);
        bind(Bundle.class).to(StandardBundle.class);
        bind(Deployment.class).to(StandardDeployment.class);
        bind(Environment.class).to(StandardEnvironment.class);
        bind(Hostclass.class).to(StandardHostclass.class);
        bind(Stage.class).to(StandardStage.class);
    }
}
