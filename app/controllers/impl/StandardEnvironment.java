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

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import controllers.Environment;
import controllers.routes;
import forms.ConfigForm;
import forms.NewEnvironment;
import forms.NewStage;
import models.DockerImageVersion;
import models.DockerRepository;
import models.EnvironmentType;
import models.Manifest;
import models.ManifestHistory;
import models.Owner;
import models.RollerPackageVersion;
import models.Stage;
import models.UserMembership;
import org.joda.time.DateTime;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

/**
 * Controller for Environments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class StandardEnvironment extends Controller implements Environment {
    /**
     * Public constructor.
     */
    @Inject
    public StandardEnvironment() {
    }

    @Override
    public F.Promise<Result> detail(final String envName) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        } else {
            final Form<NewStage> newStageForm = NewStage.form();
            final Form<ConfigForm> configForm = ConfigForm.form(environment);
            return F.Promise.pure(ok(views.html.environment.render(environment, newStageForm, configForm, false)));
        }
    }

    @Override
    public F.Promise<Result> newEnvironment(final String parentEnv) {
        Form<NewEnvironment> form = NewEnvironment.form();
        final models.Environment parent = models.Environment.getByName(parentEnv);
        if (parent != null) {
            final NewEnvironment env = new NewEnvironment();
            env.setParent(parent.getId());
            form = form.fill(env);
        }
        final List<Owner> owners = UserMembership.getOrgsForUser(request().username());
        final List<EnvironmentType> envTypes = Arrays.asList(EnvironmentType.values());
        return F.Promise.pure(ok(views.html.newEnvironment.render(form, owners, envTypes)));
    }

    @Override
    public F.Promise<Result> prepareRelease(final String envName) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        } else {
            final String currentVersion;
            final Manifest manifest = Manifest.getLatestManifest(environment);
            if (manifest != null) {
                currentVersion = manifest.getVersion();
            } else {
                currentVersion = "0";
            }

            final String nextVersion = incrementVersion(currentVersion);

            return F.Promise.pure(ok(views.html.createReleasePrep.render(environment, manifest, nextVersion)));
        }
    }

    private String incrementVersion(final String currentVersion) {
        if (currentVersion == null) {
            return "1";
        }
        final StringBuilder lastNumber = new StringBuilder();
        Boolean newNum = true;
        int numIndex = 0;
        for (int x = 0; x < currentVersion.length(); x++) {
            if (Character.isDigit(currentVersion.charAt(x))) {
                if (newNum) {
                    lastNumber.setLength(0);
                    newNum = false;
                    numIndex = x;
                }
                lastNumber.append(currentVersion.charAt(x));
            } else {
                newNum = true;
            }
        }
        if (lastNumber.length() > 0) {
            final int parsed = Integer.parseInt(lastNumber.toString());
            final String nextVal = Integer.toString(parsed + 1);
            return currentVersion.substring(0, numIndex) + nextVal  + currentVersion.substring(numIndex + lastNumber.length());
        } else {
            return currentVersion + "-1";
        }
    }

    @Override
    public F.Promise<Result> createRelease(final String envName) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(environment)) {
            return F.Promise.pure(unauthorized());
        }

        final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();

        final String[] manifestVersion = formUrlEncoded.get("version");
        if (manifestVersion == null || manifestVersion.length > 1) {
            return F.Promise.pure(badRequest());
        }

        final ArrayList<String> packages = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("packages")).orElse(new String[0]));
        final ArrayList<String> versions = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("versions")).orElse(new String[0]));

        if (packages.size() != versions.size()) {
            return F.Promise.pure(badRequest());
        }

        final Map<String, String> pkgs = Maps.newHashMap();
        for (int i = 0; i < packages.size(); i++) {
            final String aPackage = packages.get(i);
            final String version = versions.get(i);
            if (environment.getEnvironmentType() == EnvironmentType.ROLLER && (aPackage.contains("-") || version.contains("-"))) {
                return F.Promise.pure(badRequest());
            }
            pkgs.put(aPackage, version);
        }

        // Check conflicts in package versions
        final Manifest manifest = createManifest(pkgs);
        manifest.setEnvironment(environment);
        manifest.setVersion(manifestVersion[0]);
        try {
            manifest.save();
        } catch (final PersistenceException e) {
            return F.Promise.pure(badRequest());
        }
        return F.Promise.pure(redirect(routes.Environment.detail(envName)));
    }

    public F.Promise<Result> createDockerRelease(final String envName) {

        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        }

        // Make sure the user is an owner of the env
        if (!validateAuth(environment)) {
            return F.Promise.pure(unauthorized());
        }

        final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();


        final String[] manifestVersion = formUrlEncoded.get("version");
        if (manifestVersion == null || manifestVersion.length > 1) {
            return F.Promise.pure(badRequest());
        }


        // Copied above


        final ArrayList<String> repositoryNames = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("repositoryNames")).orElse(new String[0]));
        final ArrayList<String> imageShas = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("imageShas")).orElse(new String[0]));

        if (repositoryNames.size() != imageShas.size()) {
            return F.Promise.pure(badRequest());
        }

        // Create list of docker images

        // each 'image':
            // Check the repo; create if doesn't exist
            // create an image model object for the image sha; link to the package


        for (int i = 0; i < repositoryNames.size(); i++) {
            String repositoryName = repositoryNames.get(i);
            String imageDigest = imageShas.get(i);

            DockerRepository repo = DockerRepository.getByName(repositoryName);
            if (repo == null) {
                repo = new DockerRepository();
                repo.setName(repositoryName);
                repo.save();
            }

            DockerImageVersion imageVersion = DockerImageVersion.getByPackageAndVersion(repo, imageDigest);
            if (imageVersion == null) {
                imageVersion = new DockerImageVersion();
                //imageVersion.
            }
        }
        return null;

    }



    @Override
    public F.Promise<Result> create() {
        final Form<NewEnvironment> bound = NewEnvironment.form().bindFromRequest();
        if (bound.hasErrors()) {
            final List<Owner> owners = UserMembership.getOrgsForUser(request().username());
            final List<EnvironmentType> envTypes = Arrays.asList(EnvironmentType.values());
            return F.Promise.pure(badRequest(views.html.newEnvironment.render(bound, owners, envTypes)));
        } else {
            try (final Transaction transaction = Ebean.beginTransaction()) {
                final models.Environment environment = new models.Environment();
                final NewEnvironment env = bound.get();

                // This valueOf should have been checked with the validation of the form
                environment.setEnvironmentType(env.getEnvironmentType());

                environment.setName(env.getName());
                environment.setOwner(Owner.getById(env.getOwner()));

                if (env.getParent() != null) {
                    final models.Environment parent = models.Environment.getById(env.getParent());
                    environment.setParent(parent);
                    environment.setName(String.format("%s/%s", parent.getName(), env.getName()));
                }
                environment.save();

                final Manifest manifest = new Manifest();
                manifest.setCreatedBy(request().username());
                manifest.setEnvironment(environment);
                manifest.setVersion("0");
                manifest.save();

                createStage(environment, manifest, "Production");
                createStage(environment, manifest, "Staging");
                createStage(environment, manifest, "UAT");

                transaction.commit();
                return F.Promise.pure(redirect(controllers.routes.Environment.detail(environment.getName())));
            } catch (final IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void createStage(final models.Environment environment, final Manifest manifest, final String name) {
        final Stage stagingStage = new Stage();
        stagingStage.setName(name);
        stagingStage.setEnvironment(environment);
        stagingStage.save();
        final ManifestHistory stagingManifestHistory = new ManifestHistory();
        stagingManifestHistory.setStart(DateTime.now());
        stagingManifestHistory.setStage(stagingStage);
        stagingManifestHistory.setManifest(manifest);
        stagingManifestHistory.setConfig("");
        stagingManifestHistory.save();
    }

    @Override
    public F.Promise<Result> save(final String envName) {
        final models.Environment environment = models.Environment.getByName(envName);
        final Form<NewStage> newStageForm = NewStage.form();
        final Form<ConfigForm> configForm = ConfigForm.form().bindFromRequest();
        final Map<String, String> configData = configForm.data();
        final String config = configData.get("config");
        final Long version = Long.parseLong(configData.get("version"));
        if (version != environment.getVersion()) {
            final ArrayList<ValidationError> errorList = new ArrayList<>();
            errorList.add(new ValidationError("VersionConflict", "There was a version conflict. Please try saving again."));
            configForm.errors().put("VersionConflict", errorList);
        }

        if (configForm.hasErrors()) {
            return F.Promise.pure(badRequest(views.html.environment.render(environment, newStageForm, configForm, true)));
        }
        environment.setConfig(config);
        try {
            environment.save();
            return F.Promise.pure(ok(views.html.environment.render(environment, newStageForm, configForm, true)));
        } catch (final PersistenceException e) {
            return F.Promise.pure(badRequest(views.html.environment.render(environment, newStageForm, configForm, true)));
        }
    }


    private boolean validateAuth(final models.Environment environment) {
        final Set<Owner> userGroups = Sets.newHashSet(UserMembership.getOrgsForUser(request().username()));
        if (!userGroups.contains(environment.getOwner())) {
            Logger.warn(
                    String.format(
                            "Attempt at unauthorized deployment; environment=%s, owner=%s, user=%s, users_orgs=%s",
                            environment.getName(),
                            environment.getOwner().getOrgName(),
                            request().username(),
                            userGroups));
            return false;
        }
        return true;
    }

    private Manifest createManifest(final Map<String, String> pkgs) {
        final List<RollerPackageVersion> newPackages = Lists.newArrayList();
        for (final Map.Entry<String, String> entry : pkgs.entrySet()) {
            final String pkg = entry.getKey();
            final String version = entry.getValue();
            models.Package p = models.Package.getByName(pkg);
            if (p == null) {
                p = new models.Package();
                p.setName(pkg);
                p.save();
            }

            RollerPackageVersion packageVersion = RollerPackageVersion.getByPackageAndVersion(p, version);
            if (packageVersion == null) {
                packageVersion = new RollerPackageVersion();
                packageVersion.setPkg(p);
                packageVersion.setVersion(version);
                packageVersion.save();
            }

            newPackages.add(packageVersion);
        }
        final Manifest manifest = new Manifest();
        manifest.setPackages(newPackages);
        manifest.setCreatedBy(request().username());
        manifest.save();
        return manifest;
    }

}
