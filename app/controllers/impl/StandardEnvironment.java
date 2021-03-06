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

import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import controllers.Environment;
import controllers.routes;
import forms.ConfigForm;
import forms.NewEnvironment;
import forms.NewStage;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.EnvironmentType;
import models.Manifest;
import models.ManifestHistory;
import models.Owner;
import models.PackageVersion;
import models.Stage;
import models.UserMembership;
import org.joda.time.DateTime;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

/**
 * Controller for Environments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class StandardEnvironment extends Controller implements Environment {
    /**
     * Public constructor.
     *
     * @param formFactory form factory to create forms
     */
    @Inject
    public StandardEnvironment(final FormFactory formFactory, final Config config, final MessagesApi messagesApi, final WebJarsUtil webJarsUtil) {
        _formFactory = formFactory;
        _config = config;
        _messagesApi = messagesApi;
        _webJarsUtil = webJarsUtil;
    }

    @Override
    public CompletionStage<Result> detail(final String envName, final Http.Request request) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        } else {
            final Form<NewStage> newStageForm = NewStage.form(_formFactory);
            final Form<ConfigForm> configForm = ConfigForm.form(environment, _formFactory);
            return CompletableFuture.completedFuture(ok(views.html.environment.render(environment, newStageForm, configForm, false, request, _messagesApi.preferred(request), _webJarsUtil)));
        }
    }

    @Override
    public CompletionStage<Result> newEnvironment(final String parentEnv, final Http.Request request) {
        Form<NewEnvironment> form = NewEnvironment.form(_formFactory);
        final models.Environment parent = models.Environment.getByName(parentEnv);
        if (parent != null) {
            final NewEnvironment env = new NewEnvironment();
            env.setParent(parent.getId());
            form = form.fill(env);
        }
        final List<Owner> owners = UserMembership.getOrgsForUser(request.attrs().get(Security.USERNAME));
        final Set<String> enabledEnvironmentTypes = new HashSet<>(_config.getStringList("enabledEnvironmentTypes"));
        final List<EnvironmentType> envTypes = Arrays.stream(EnvironmentType.values())
                .filter(environmentType -> enabledEnvironmentTypes.contains(environmentType.name().toLowerCase()))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(ok(views.html.newEnvironment.render(form, owners, envTypes, request, _messagesApi.preferred(request), _webJarsUtil)));
    }

    @Override
    public CompletionStage<Result> prepareRelease(final String envName, final Http.Request request) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        } else {
            final String currentVersion;
            final Manifest manifest = Manifest.getLatestManifest(environment);
            if (manifest != null) {
                currentVersion = manifest.getVersion();
            } else {
                currentVersion = "0";
            }

            final String nextVersion = incrementVersion(currentVersion);

            return CompletableFuture.completedFuture(ok(views.html.createReleasePrep.render(environment, manifest, nextVersion, request, _webJarsUtil)));
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
    public CompletionStage<Result> createRelease(final String envName, final Http.Request request) {
        final models.Environment environment = models.Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        // Make sure the user is an owner of the env
        if (!validateAuth(environment, request)) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        final Map<String, String[]> formUrlEncoded = request.body().asFormUrlEncoded();

        final String[] manifestVersion = formUrlEncoded.get("version");
        if (manifestVersion == null || manifestVersion.length > 1) {
            return CompletableFuture.completedFuture(badRequest());
        }

        final ArrayList<String> packages = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("packages")).orElse(new String[0]));
        final ArrayList<String> versions = Lists.newArrayList(Optional.ofNullable(formUrlEncoded.get("versions")).orElse(new String[0]));

        if (packages.size() != versions.size()) {
            return CompletableFuture.completedFuture(badRequest());
        }

        final Map<String, String> pkgs = Maps.newHashMap();
        for (int i = 0; i < packages.size(); i++) {
            final String aPackage = packages.get(i);
            final String version = versions.get(i);
            if (environment.getEnvironmentType() == EnvironmentType.ROLLER && (aPackage.contains("-") || version.contains("-"))) {
                return CompletableFuture.completedFuture(badRequest());
            } else if (Strings.isNullOrEmpty(version)) {
                return CompletableFuture.completedFuture(badRequest());
            }
            pkgs.put(aPackage, version);
        }

        // Check conflicts in package versions
        final Manifest manifest = createManifest(pkgs, request);
        manifest.setEnvironment(environment);
        manifest.setVersion(manifestVersion[0]);
        try {
            manifest.save();
        } catch (final PersistenceException e) {
            return CompletableFuture.completedFuture(badRequest());
        }
        return CompletableFuture.completedFuture(redirect(routes.Environment.detail(envName)));
    }

    @Override
    public CompletionStage<Result> create(final Http.Request request) {

        final Form<NewEnvironment> bound = NewEnvironment.form(_formFactory).bindFromRequest(request);
        if (bound.hasErrors()) {
            final List<Owner> owners = UserMembership.getOrgsForUser(request.attrs().get(Security.USERNAME));
            final List<EnvironmentType> envTypes = Arrays.asList(EnvironmentType.values());
            return CompletableFuture.completedFuture(badRequest(views.html.newEnvironment.render(bound, owners, envTypes, request, _messagesApi.preferred(request), _webJarsUtil)));
        } else {
            try (Transaction transaction = Ebean.beginTransaction()) {
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
                manifest.setCreatedBy(request.attrs().get(Security.USERNAME));
                manifest.setEnvironment(environment);
                manifest.setVersion("0");
                manifest.save();

                List<String> stages = _config.getStringList("defaultStages");
                stages.forEach(stage -> createStage(environment, manifest, stage));

                transaction.commit();
                return CompletableFuture.completedFuture(redirect(controllers.routes.Environment.detail(environment.getName())));
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
    public CompletionStage<Result> save(final String envName, final Http.Request request) {
        final models.Environment environment = models.Environment.getByName(envName);
        final Form<NewStage> newStageForm = NewStage.form(_formFactory);
        Form<ConfigForm> configForm = ConfigForm.form(_formFactory).bindFromRequest(request);
        final Map<String, String> configData = configForm.rawData();
        final String config = configData.get("config");
        final Long version = Long.parseLong(configData.get("version"));
        if (version != environment.getVersion()) {
            configForm = configForm.withError("VersionConflict", "There was a version conflict. Please try saving again.");
        }

        if (configForm.hasErrors()) {
            return CompletableFuture.completedFuture(
                    badRequest(views.html.environment.render(environment, newStageForm, configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        }
        environment.setConfig(config);
        try {
            environment.save();
            return CompletableFuture.completedFuture(
                    ok(views.html.environment.render(environment, newStageForm, configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        } catch (final PersistenceException e) {
            return CompletableFuture.completedFuture(
                    badRequest(views.html.environment.render(environment, newStageForm, configForm, true, request, _messagesApi.preferred(request), _webJarsUtil)));
        }
    }


    private boolean validateAuth(final models.Environment environment, final Http.Request request) {
        final Set<Owner> userGroups = Sets.newHashSet(UserMembership.getOrgsForUser(request.attrs().get(Security.USERNAME)));
        if (!userGroups.contains(environment.getOwner())) {
            LOGGER.warn()
                    .setMessage("Attempt at unauthorized deployment")
                    .addData("environment", environment.getName())
                    .addData("owner", environment.getOwner().getOrgName())
                    .addData("user", request.attrs().get(Security.USERNAME))
                    .addData("userGroups", userGroups)
                    .log();
            return false;
        }
        return true;
    }

    private Manifest createManifest(final Map<String, String> pkgs, final Http.Request request) {
        final List<PackageVersion> newPackages = Lists.newArrayList();
        for (final Map.Entry<String, String> entry : pkgs.entrySet()) {
            final String pkg = entry.getKey();
            final String version = entry.getValue();
            models.Package p = models.Package.getByName(pkg);
            if (p == null) {
                p = new models.Package();
                p.setName(pkg);
                p.save();
            }

            PackageVersion packageVersion = PackageVersion.getByPackageAndVersion(p, version);
            if (packageVersion == null) {
                packageVersion = new PackageVersion();
                packageVersion.setPkg(p);
                packageVersion.setVersion(version);
                packageVersion.save();
            }

            newPackages.add(packageVersion);
        }
        final Manifest manifest = new Manifest();
        manifest.setPackages(newPackages);
        manifest.setCreatedBy(request.attrs().get(Security.USERNAME));
        manifest.save();
        return manifest;
    }

    private final FormFactory _formFactory;
    private final Config _config;
    private final MessagesApi _messagesApi;
    private final WebJarsUtil _webJarsUtil;
    private static final com.arpnetworking.steno.Logger LOGGER = LoggerFactory.getLogger(StandardEnvironment.class);
}
