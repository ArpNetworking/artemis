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
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.groupon.deployment.FleetDeploymentCommands;
import controllers.Stage;
import forms.AddHostclassToStage;
import forms.ConfigForm;
import forms.CopyStage;
import forms.DeployManifest;
import forms.NewStage;
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
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import scala.concurrent.Future;
import utils.AuthN;
import utils.StageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.persistence.PersistenceException;

/**
 * Holds stage actions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class StandardStage extends Controller implements Stage {
    /**
     * Public constructor.
     *
     * @param deploymentManager a deployment manager
     */
    @Inject
    public StandardStage(@Named("DeployManager") final ActorRef deploymentManager) {
        _deploymentManager = deploymentManager;
    }

    @Override
    public F.Promise<Result> detail(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        } else {
            final Form<ConfigForm> configForm = ConfigForm.form(stage);
            final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
            final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
            final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
            return F.Promise.pure(
                    ok(
                            views.html.stageDetail.render(
                                    stage,
                                    stageDeployments,
                                    stageSnapshots,
                                    current.getManifest(),
                                    AddHostclassToStage.form(),
                                    configForm,
                                    false)));
        }
    }

    @Override
    public F.Promise<Result> addHostclass(final String envName, final String stageName) {
        final Form<AddHostclassToStage> bound = AddHostclassToStage.form().bindFromRequest();
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        if (bound.hasErrors()) {
            return F.Promise.pure(badRequest(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                    bound, ConfigForm.form(), false)));
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
            return F.Promise.pure(ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                    AddHostclassToStage.form(), ConfigForm.form(), false)));
        }
    }

    @Override
    public F.Promise<Result> removeHostclass(final String envName, final String stageName, final String hostclassName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        final Hostclass hostclass = Hostclass.getByName(hostclassName);
        if (hostclass == null) {
            return F.Promise.pure(notFound());
        }
        hostclass.getStages().remove(stage);
        hostclass.save();

        stage.getHostclasses().remove(hostclass);
        stage.save();

        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        return F.Promise.pure(ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                AddHostclassToStage.form(), ConfigForm.form(), false)));
    }

    @Override
    public F.Promise<Result> prepareDeployManifest(final String envName, final String stageName, final long manifestId) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return F.Promise.pure(unauthorized());
        }

        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);

        final Manifest manifest = Manifest.getById(manifestId);
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);

        final RollerDeploymentPrep deploymentPrep = new RollerDeploymentPrep();
        final DeploymentDescription description = deploymentPrep.getDeploymentDescription(stage, manifest);
        return F.Promise.pure(ok(views.html.stageDeployConfirm.render(stage, current, manifest, description, packageConflicts)));
    }

    @Override
    public F.Promise<Result> prepareDeploy(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return F.Promise.pure(unauthorized());
        }
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        final DeployManifest deployManifest = new DeployManifest();
        deployManifest.setVersion(current.getId());
        deployManifest.setManifest(current.getManifest().getId());
        final Form<DeployManifest> form = DeployManifest.form().fill(deployManifest);
        return F.Promise.pure(
                ok(views.html.stageDeployPrep.render(stage.getEnvironment(), stage, current, form, getDeployableManifests(stage))));
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
    public F.Promise<Result> previewDeploy(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(stage)) {
            return F.Promise.pure(unauthorized());
        }

        // Check conflicts in package versions
        final Form<DeployManifest> form = DeployManifest.form().bindFromRequest(request());
        if (form.hasErrors()) {
            return F.Promise.pure(badRequest(
                            views.html.stageDeployPrep.render(
                                    stage.getEnvironment(), stage,
                                    ManifestHistory.getCurrentForStage(stage), form, stage.getEnvironment()
                                            .getManifests())));
        }

        final DeployManifest deployManifest = form.get();
        final long snapshotVersion = deployManifest.getVersion();
        final ManifestHistory currentSnapshot = ManifestHistory.getCurrentForStage(stage);
        if (snapshotVersion != currentSnapshot.getId()) {
            return F.Promise.pure(status(
                            409, views.html.stageDeployPrep.render(
                                    stage.getEnvironment(), stage,
                                    ManifestHistory.getCurrentForStage(stage), form, stage.getEnvironment()
                                            .getManifests())));
        }

//        final Manifest manifest = Manifest.getVersion(stage.getEnvironment(), deployManifest.getManifest());
        final Manifest manifest = Manifest.getById(deployManifest.getManifest());
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);

        final RollerDeploymentPrep deploymentPrep = new RollerDeploymentPrep();
        final DeploymentDescription description = deploymentPrep.getDeploymentDescription(stage, manifest);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);
        return F.Promise.pure(ok(views.html.stageDeployConfirm.render(stage, current, manifest, description, packageConflicts)));
    }

    private boolean validateAuth(final models.Stage stage) {
        final Set<Owner> userGroups = Sets.newHashSet(UserMembership.getOrgsForUser(request().username()));
        if (!userGroups.contains(stage.getEnvironment().getOwner())) {
            Logger.warn(String.format(
                    "Attempt at unauthorized deployment; environment=%s, owner=%s, user=%s, users_orgs=%s",
                    stage.getEnvironment().getName(),
                    stage.getEnvironment().getOwner().getOrgName(),
                    request().username(),
                    userGroups));
            return false;
        }
        return true;
    }

    @Override
    public F.Promise<Result> confirmDeploy(final String envName, final String stageName, final long version, final long manifestId) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }
        final Manifest manifest = Manifest.getById(manifestId);
        if (manifest == null) {
            return F.Promise.pure(notFound());
        }
        if (!validateAuth(stage)) {
            return F.Promise.pure(unauthorized());
        }
        final ManifestHistory currentSnapshot = ManifestHistory.getCurrentForStage(stage);
        if (currentSnapshot.getId() != version) {
            return F.Promise.pure(status(CONFLICT));
        }
        final ConflictedPackages packageConflicts = StageUtil.getConflictedPackages(stage, manifest);
        if (packageConflicts.hasConflicts()) {
            //TODO(barp): show conflict page [Artemis-?]
            return F.Promise.pure(status(CONFLICT));
        }

        final Future<Object> ask = Patterns.ask(
                _deploymentManager,
                new FleetDeploymentCommands.DeployStage(stage, manifest, request().username()),
                Timeout.apply(30L, TimeUnit.SECONDS));

        return F.Promise.wrap(ask).map(
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
    public F.Promise<Result> create(final String envName) {
      final Form<NewStage> bound = NewStage.form().bindFromRequest();
        final Form<ConfigForm> configFormBound = ConfigForm.form().bindFromRequest();
      final models.Environment environment = models.Environment.getByName(envName);
      if (environment == null) {
          return F.Promise.pure(notFound());
      }
      if (bound.hasErrors()) {
          return F.Promise.pure(badRequest(views.html.environment.render(environment, bound, configFormBound, false)));
      } else {
          try (final Transaction transaction = Ebean.beginTransaction()) {
              final NewStage newStageForm = bound.get();
              final models.Stage newStage = new models.Stage();
              newStage.setName(newStageForm.getName());
              newStage.setEnvironment(environment);
              newStage.save();

              final Manifest manifest = new Manifest();
              manifest.setCreatedBy(request().username());
              manifest.save();

              models.Stage.applyManifestToStage(newStage, manifest);
              transaction.commit();
              return F.Promise.pure(redirect(controllers.routes.Environment.detail(envName)));
          } catch (final IOException e) {
              throw Throwables.propagate(e);
          }
      }
    }

    @Override
    public F.Promise<Result> save(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        final Form<ConfigForm> configForm = ConfigForm.form().bindFromRequest();

        final Map<String, String> configData = configForm.data();
        final String config = configData.get("config");
        final Long version = Long.parseLong(configData.get("version"));
        if (version != stage.getVersion()) {
            final ArrayList<ValidationError> errorList = new ArrayList<>();
            errorList.add(new ValidationError("VersionConflict", "There was a version conflict. Please try saving again."));
            configForm.errors().put("VersionConflict", errorList);
        }

        final List<Deployment> stageDeployments = models.Deployment.getByStage(stage, 10, 0);
        final List<ManifestHistory> stageSnapshots = models.ManifestHistory.getByStage(stage, 10, 0);
        final ManifestHistory current = ManifestHistory.getCurrentForStage(stage);

        if (configForm.hasErrors()) {
            return F.Promise.pure(badRequest(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                    AddHostclassToStage.form(), configForm, true)));
        }
        stage.setConfig(config);
        try {
            stage.save();
            return F.Promise.pure(ok(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                    AddHostclassToStage.form(), configForm, true)));
        } catch (final PersistenceException e) {
            return F.Promise.pure(badRequest(views.html.stageDetail.render(stage, stageDeployments, stageSnapshots, current.getManifest(),
                    AddHostclassToStage.form(), configForm, true)));
        }


    }

    @Override
    public F.Promise<Result> promote(final String sourceEnvName, final String sourceStageName) {
        final Form<CopyStage> bound = CopyStage.form().bindFromRequest();
        final models.Stage sourceStage = models.Stage.getByEnvironmentNameAndName(sourceEnvName, sourceStageName);
        final models.Stage destStage = models.Stage.getByEnvironmentNameAndName(bound.get().getEnvName(), bound.get().getStageName());
        return copy(sourceStage, destStage);
    }

    @Override
    public F.Promise<Result> synchronize(final String sourceEnvName, final String sourceStageName) {
        final Form<CopyStage> bound = CopyStage.form().bindFromRequest();
        final models.Stage sourceStage = models.Stage.getByEnvironmentNameAndName(bound.get().getEnvName(), bound.get().getStageName());
        final models.Stage destStage = models.Stage.getByEnvironmentNameAndName(sourceEnvName, sourceStageName);
        return copy(sourceStage, destStage);
    }

    private F.Promise<Result> copy(final models.Stage sourceStage, final models.Stage destStage) {
        if (sourceStage == null || destStage == null) {
            return F.Promise.pure(notFound());
        }
        try (final Transaction transaction = Ebean.beginTransaction()) {
            final Manifest manifestToCopy = ManifestHistory.getCurrentForStage(sourceStage).getManifest();
            if (manifestToCopy == null) {
                return F.Promise.pure(notFound());
            }
            final ManifestHistory currentOnDest = ManifestHistory.getCurrentForStage(destStage);
            transaction.commit();
            return F.Promise.pure(
                    ok(
                            views.html.copyStageDeployPrep.render(
                                    destStage.getEnvironment(),
                                    destStage,
                                    manifestToCopy,
                                    currentOnDest.getId())));
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private final ActorRef _deploymentManager;
}
