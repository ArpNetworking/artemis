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

import controllers.Deployment;
import models.DeploymentDiff;
import models.ManifestHistory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Singleton;

/**
 * Deployment controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class StandardDeployment extends Controller implements Deployment {
    @Override
    public CompletionStage<Result> detail(final long deploymentId) {
        final models.Deployment deployment = models.Deployment.getById(deploymentId);
        if (deployment == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        return CompletableFuture.completedFuture(
                ok(views.html.deploymentStatus.render(deployment, deployment.getManifestHistory().getStage())));
    }

    @Override
    public CompletionStage<Result> log(final long deploymentId) {
        final models.Deployment deployment = models.Deployment.getById(deploymentId);
        if (deployment == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        return CompletableFuture.completedFuture(ok(views.html.deployLog.render(deployment)));
    }

    @Override
    public CompletionStage<Result> diff(final long deploymentId) {
        final models.Deployment deployment = models.Deployment.getById(deploymentId);
        if (deployment == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        final ManifestHistory current = deployment.getManifestHistory();
        final ManifestHistory previous = current.getPrevious();
        final DeploymentDiff diff = new DeploymentDiff(previous.getManifest(), current.getManifest());
        return CompletableFuture.completedFuture(ok(views.html.deploymentDiff.render(deployment.getManifestHistory().getStage(), diff)));
    }
}
