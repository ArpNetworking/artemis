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
package controllers.impl;

import controllers.Bundle;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import javax.inject.Inject;

/**
 * Controller for bundles.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class StandardBundle extends Controller implements Bundle {
    /**
     * Public constructor.
     */
    @Inject
    public StandardBundle() {
    }

    @Override
    public F.Promise<Result> newBundle() {
        /*
        Form<NewEnvironment> form = NewEnvironment.form();
        final models.Environment parent = models.Environment.getByName(parentEnv);
        if (parent != null) {
            final NewEnvironment env = new NewEnvironment();
            env.parent = parent.getId();
            form = form.fill(env);
        }
        final List<Owner> owners = UserMembership.getOrgsForUser(request().username());
        return ok(views.html.newEnvironment.render(form, owners));
        */
        return F.Promise.pure(ok());
    }
}