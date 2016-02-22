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

import com.google.inject.Singleton;
import controllers.Admin;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * Administrative functions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class StandardAdmin extends Controller implements Admin {
    /**
     * Public constructor.
     */
    @Inject
    public StandardAdmin() {
    }

    @Override
    public F.Promise<Result> index() {
        return F.Promise.pure(ok(views.html.admin.render()));
    }

    @Override
    public F.Promise<Result> refreshPackages() {
        return F.Promise.pure(ok(views.html.admin.render()));
    }
}