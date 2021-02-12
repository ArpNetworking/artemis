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

import com.google.common.collect.Lists;
import models.Bundle;
import models.Environment;
import models.Hostclass;
import models.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjars.play.WebJarsUtil;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * Generic application actions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class StandardApplication extends Controller implements controllers.Application {

    /**
     * Public constructor.
     */
    @Inject
    public StandardApplication(final WebJarsUtil webJarsUtil) {
        _webJarsUtil = webJarsUtil;
    }

    @Override
    public CompletionStage<Result> main(final Http.Request request) {
        final List<Owner> orgs = AuthN.getOrganizations(request.attrs().get(Security.USERNAME));
        final List<Environment> environments = Environment.getEnvironmentsForOrgs(orgs, 10);
        final List<Bundle> bundles = Lists.newArrayList();
        final List<Hostclass> hostclasses = Hostclass.getHostclassesForEnvironments(environments, 10);
        return CompletableFuture.completedFuture(ok(views.html.index.render(environments, bundles, hostclasses, request, _webJarsUtil)));
    }

    private WebJarsUtil _webJarsUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardApplication.class);
}
