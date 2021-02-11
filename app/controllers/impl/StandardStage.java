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

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.groupon.deployment.FleetDeploymentCommands;
import controllers.Stage;
import forms.AddHostclassToStage;
import forms.ConfigForm;
import forms.CopyStage;
import forms.DeployManifest;
import forms.NewStage;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.ConflictedPackages;
import models.Deployment;
import models.DeploymentDescription;
import models.Environment;
import models.Hostclass;
import models.Manifest;
import models.ManifestHistory;
import models.Owner;
import models.RollerDeploymentPrep;
import models.UserMembership;
import org.webjars.play.WebJarsUtil;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;
import utils.PageUtils;
import utils.StageUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

/**
 * Holds stage actions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class StandardStage extends Controller implements Stage {
    /**
     * Public constructor.
     *
     * @param deploymentManager a deployment manager
     * @param formFactory form factory to create forms
     */
    @Inject
    public StandardStage(@Named("DeployManager") final ActorRef deploymentManager, final FormFactory formFactory, final MessagesApi messagesApi, final
            WebJarsUtil webJarsUtil) {
        _deploymentManager = deploymentManager;
        _formFactory = formFactory;
        _messagesApi = messagesApi;
        _webJarsUtil = webJarsUtil;
    }

    @Override
    public CompletionStage<Result> detail(final String envName, final String stageName, final Http.Request request) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        } else {
            final Form<ConfigForm> configForm = ConfigForm.form(stage, _formFactory);
            final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
            final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
            final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
            return CompletableFuture.completedFuture(
                    ok(
                            views.html.stageDetail.render(
                                    stage,
                                    stageDeployments,
                                    stageSnapshots,
                                    current.getManifest(),
                                    AddHostclassToStage.form(_formFactory),
                                    configForm,
                                    false,
                                    request, _messagesApi.preferred(request), _webJarsUtil)));
        }
    }

    @Override
    public CompletionStage<Result> addHostclass(final String envName, final String stageName) {
        final Form<AddHostclassToStage> bound = AddHostclassToStage.form(_formFactory).bindFromRequest();
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        final String nonce = PageUtils.createNonce();
        response().setHeader("Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        if (bound.hasErrors()) {
            return CompletableFuture.completedFuture(
                    badRequest(views.html.stageDetail.render(
                            stage, stageDeployments, stageSnapshots, current.getManifest(), bound, ConfigForm.form(_formFactory), false, nonce)));
        } else {
            final AddHostclassToStage addObject = bound.get();
            Hostclass hostclass = Hostclass.getByName(addObject.getHostclass());
            if (hostclass == null) {
                hostclass = new Hostclass();
                hostclass.setName(addObject.getHostclass());
                hostclass.setStages(Sets.newHashSet(stage));
                hostclass.save();
            } else {
                hostclass.getStages().add(stage);
                hostclass.save();
            }
            stage.getHostclasses().add(hostclass);
            stage.save();
            return CompletableFuture.completedFuture(
                    ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                            AddHostclassToStage.form(_formFactory), ConfigForm.form(_formFactory), false, nonce)));
        }
    }

    @Override
    public CompletionStage<Result> removeHostclass(final String envName, final String stageName, final String hostclassName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        final Hostclass hostclass = Hostclass.getByName(hostclassName);
        if (hostclass == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        hostclass.getStages().remove(stage);
        hostclass.save();

        stage.getHostclasses().remove(hostclass);
        stage.save();

        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        final String nonce = PageUtils.createNonce();
        response().setHeader("Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        return CompletableFuture.completedFuture(
                ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                AddHostclassToStage.form(_formFactory), ConfigForm.form(_formFactory), false, nonce)));
    }

    @Override
    public CompletionStage<Result> prepareDeployManifest(final String envName, final String stageName, final long manifestId) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);

        final Manifest manifest = Manifest.getById(manifestId);
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);

        final RollerDeploymentPrep deploymentPrep = new RollerDeploymentPrep();
        final DeploymentDescription description = deploymentPrep.getDeploymentDescription(stage, manifest);
        return CompletableFuture.completedFuture(
                ok(views.html.stageDeployConfirm.render(stage, current, manifest, description, packageConflicts)));
    }

    @Override
    public CompletionStage<Result> prepareDeploy(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return CompletableFuture.completedFuture(unauthorized());
        }
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        final DeployManifest deployManifest = new DeployManifest();
        deployManifest.setVersion(current.getId());
        deployManifest.setManifest(current.getManifest().getId());
        final Form<DeployManifest> form = DeployManifest.form(_formFactory).fill(deployManifest);
        final String nonce = PageUtils.createNonce();
        response().setHeader("Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        return CompletableFuture.completedFuture(
                ok(views.html.stageDeployPrep.render(stage.getEnvironment(), stage, current, form, getDeployableManifests(stage), nonce)));
    }

    private List<Manifest> getDeployableManifests(final models.Stage stage) {
        final ArrayList<Manifest> manifests = Lists.newArrayList();
        Environment environment = stage.getEnvironment();
        while (environment != null) {
            manifests.addAll(environment.getManifests());
            environment = environment.getParent();
        }
        manifests.sort(Comparator.comparing(Manifest::getCreatedAt).reversed());
        return manifests;
    }

    @Override
    public CompletionStage<Result> previewDeploy(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        final String nonce = PageUtils.createNonce();
        response().setHeader("Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        // Check conflicts in package versions
        final Form<DeployManifest> form = DeployManifest.form(_formFactory).bindFromRequest(request());
        if (form.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest(
                            views.html.stageDeployPrep.render(
                                    stage.getEnvironment(), stage,
                                    ManifestHistory.getCurrentForStage(stage), form, stage.getEnvironment()
                                            .getManifests(), nonce)));
        }

        final DeployManifest deployManifest = form.get();
        final long snapshotVersion = deployManifest.getVersion();
        final ManifestHistory currentSnapshot = ManifestHistory.getCurrentForStage(stage);
        if (snapshotVersion != currentSnapshot.getId()) {
            return CompletableFuture.completedFuture(status(
                            409, views.html.stageDeployPrep.render(
                                    stage.getEnvironment(), stage,
                                    ManifestHistory.getCurrentForStage(stage), form, stage.getEnvironment()
                                            .getManifests(), nonce)));
        }

//        final Manifest manifest = Manifest.getVersion(stage.getEnvironment(), deployManifest.getManifest());
        final Manifest manifest = Manifest.getById(deployManifest.getManifest());
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);

        final RollerDeploymentPrep deploymentPrep = new RollerDeploymentPrep();
        final DeploymentDescription description = deploymentPrep.getDeploymentDescription(stage, manifest);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        return CompletableFuture.completedFuture(
                ok(views.html.stageDeployConfirm.render(stage, current, manifest, description, packageConflicts)));
    }

    private boolean validateAuth(final models.Stage stage) {
        final Set<Owner> userGroups = Sets.newHashSet(UserMembership.getOrgsForUser(request().attrs().get(Security.USERNAME)));
        if (!userGroups.contains(stage.getEnvironment().getOwner())) {
            Logger.warn(String.format(
                    "Attempt at unauthorized deployment; environment=%s, owner=%s, user=%s, users_orgs=%s",
                    stage.getEnvironment().getName(),
                    stage.getEnvironment().getOwner().getOrgName(),
                    request().attrs().get(Security.USERNAME),
                    userGroups));
            return false;
        }
        return true;
    }

    @Override
    public CompletionStage<Result> confirmDeploy(final String envName, final String stageName, final long version, final long manifestId) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        final Manifest manifest = Manifest.getById(manifestId);
        if (manifest == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        if (!validateAuth(stage)) {
            return CompletableFuture.completedFuture(unauthorized());
        }
        final ManifestHistory currentSnapshot = ManifestHistory.getCurrentForStage(stage);
        if (currentSnapshot.getId() != version) {
            return CompletableFuture.completedFuture(status(CONFLICT));
        }
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);
        if (packageConflicts.hasConflicts()) {
            //TODO(barp): show conflict page [Artemis-?]
            return CompletableFuture.completedFuture(status(CONFLICT));
        }

        final CompletionStage<Object> ask = PatternsCS.ask(
                _deploymentManager,
                new FleetDeploymentCommands.DeployStage(stage, manifest, request().attrs().get(Security.USERNAME)),
                Timeout.apply(30L, TimeUnit.SECONDS));

        return ask.thenApply(
                o -> {
                    if (o instanceof Deployment) {
                        final Deployment deployment = (Deployment) o;
                        return redirect(controllers.routes.Deployment.detail(deployment.getId()));
                    }
                    Logger.error("Expected Deployment response from deployment manager, got " + o);
                    return internalServerError();
                });
    }

    @Override
    public CompletionStage<Result> create(final String envName, final Http.Request request) {
        final Form<NewStage> bound = NewStage.form(_formFactory).bindFromRequest(request);
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        if (bound.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest(views.html.environment.render(environment, bound, _formFactory.form(ConfigForm.class), false, request, _messagesApi.preferred(request), _webJarsUtil)));
        } else {
            try (Transaction transaction = Ebean.beginTransaction()) {
                final NewStage newStageForm = bound.get();
                final models.Stage newStage = new models.Stage();
                newStage.setName(newStageForm.getName());
                newStage.setEnvironment(environment);
                newStage.save();

                final Manifest manifest = new Manifest();
                manifest.setCreatedBy(request.attrs().get(Security.USERNAME));
                manifest.setEnvironment(environment);
                manifest.save();

                models.Stage.applyManifestToStage(newStage, manifest);
                transaction.commit();
                return CompletableFuture.completedFuture(redirect(controllers.routes.Environment.detail(envName)));
          }
      }
    }

    @Override
    public CompletionStage<Result> save(final String envName, final String stageName, final Http.Request request) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        Form<ConfigForm> configForm = ConfigForm.form(_formFactory).bindFromRequest(request);

        final Map<String, String> configData = configForm.rawData();
        final String config = configData.get("config");
        final Long version = Long.parseLong(configData.get("version"));
        if (version != stage.getVersion()) {
            configForm = configForm.withError("VersionConflict", "There was a version conflict. Please try saving again.");
        }

        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);

        if (configForm.hasErrors()) {
            return CompletableFuture.completedFuture(
                    badRequest(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                            AddHostclassToStage.form(_formFactory), configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        }
        stage.setConfig(config);
        try {
            stage.save();
            return CompletableFuture.completedFuture(
                    ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                            AddHostclassToStage.form(_formFactory), configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        } catch (final PersistenceException e) {
            return CompletableFuture.completedFuture(
                    badRequest(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                            AddHostclassToStage.form(_formFactory), configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        }


    }

    @Override
    public CompletionStage<Result> promote(final String sourceEnvName, final String sourceStageName, final Http.Request request) {
        final Form<CopyStage> bound = CopyStage.form(_formFactory).bindFromRequest(request);
        final models.Stage sourceStage = models.Stage.getByEnvironmentNameAndName(sourceEnvName, sourceStageName);
        final models.Stage destStage = models.Stage.getByEnvironmentNameAndName(bound.get().getEnvName(), bound.get().getStageName());
        return copy(sourceStage, destStage, request);
    }

    @Override
    public CompletionStage<Result> synchronize(final String sourceEnvName, final String sourceStageName, final Http.Request request) {
        final Form<CopyStage> bound = CopyStage.form(_formFactory).bindFromRequest(request);
        final models.Stage sourceStage = models.Stage.getByEnvironmentNameAndName(bound.get().getEnvName(), bound.get().getStageName());
        final models.Stage destStage = models.Stage.getByEnvironmentNameAndName(sourceEnvName, sourceStageName);
        return copy(sourceStage, destStage, request);
    }

    private CompletionStage<Result> copy(final models.Stage sourceStage, final models.Stage destStage, final Http.Request request) {
        final String nonce = PageUtils.createNonce();
        Result t = new Result(0);
        if (sourceStage == null || destStage == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        try (Transaction transaction = Ebean.beginTransaction()) {
            final Manifest manifestToCopy = ManifestHistory.getCurrentForStage(sourceStage).getManifest();
            if (manifestToCopy == null) {
                return CompletableFuture.completedFuture(notFound());
            }
            final ManifestHistory currentOnDest = ManifestHistory.getCurrentForStage(destStage);
            transaction.commit();



            final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(destStage, manifestToCopy);

            final RollerDeploymentPrep deploymentPrep = new RollerDeploymentPrep();
            final DeploymentDescription description = deploymentPrep.getDeploymentDescription(destStage, manifestToCopy);
            return CompletableFuture.completedFuture(
                    ok(views.html.stageDeployConfirm.render(destStage, currentOnDest, manifestToCopy, description, packageConflicts, request, _webJarsUtil)));
        }
    }

    private final ActorRef _deploymentManager;
    private final FormFactory _formFactory;
    private MessagesApi _messagesApi;
    private WebJarsUtil _webJarsUtil;
}
