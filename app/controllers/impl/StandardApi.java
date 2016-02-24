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

import actors.DeployLogRelay;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.groupon.deployment.FleetDeploymentCommands;
import controllers.Api;
import models.Deployment;
import models.Environment;
import models.Manifest;
import models.ManifestHistory;
import models.Package;
import models.PackageVersion;
import models.RollerPackageVersion;
import models.Stage;
import play.Logger;
import play.api.http.Writeable;
import play.api.libs.iteratee.Concurrent;
import play.api.libs.iteratee.Enumeratee;
import play.api.libs.iteratee.Enumeratee$;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.Codec;
import play.api.mvc.Results$;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Tuple2;
import scala.compat.java8.JFunction;
import scala.concurrent.Future;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * JSON REST Apis.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StandardApi extends Controller implements Api {
    /**
     * Public constructor.
     *
     * @param deploymentManager the deployment manager
     */
    @Inject
    public StandardApi(@Named("DeployManager") final ActorRef deploymentManager) {
        _deploymentManager = deploymentManager;
    }

    @Override
    public F.Promise<Result> hostclassSearch(final String query) {
        final ObjectNode node = Json.newObject();
        final List<models.Hostclass> hostclasses = models.Hostclass.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final models.Hostclass hostclass : hostclasses) {
            resultsArray.add(hostclass.getName());
        }
        return F.Promise.pure(ok(node));
    }

    @Override
    public F.Promise<Result> packageSearch(final String query) {
        final ObjectNode node = Json.newObject();
        final List<models.Package> packages = models.Package.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final models.Package pkg : packages) {
            resultsArray.add(pkg.getName());
        }
        return F.Promise.pure(ok(node));
    }

    @Override
    public F.Promise<Result> environmentSearch(final String query) {
        final ObjectNode node = Json.newObject();
        final List<Environment> environments = Environment.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final Environment env : environments) {
            resultsArray.add(env.getName());
        }
        return F.Promise.pure(ok(node));
    }

    @Override
    public F.Promise<Result> getStages(final String envName) {
        final Environment environment = Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        }

        final ObjectNode node = Json.newObject();
        final ArrayNode resultsArray = node.putArray("results");
        for (final Stage stage : environment.getStages()) {
            resultsArray.add(stage.getName());
        }
        return F.Promise.pure(ok(node));
    }

    //TODO(barp): Authenticate this [Artemis-?]
    @Override
    public F.Promise<Result> updateStagePackageVersions(final String envName, final String stageName) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return F.Promise.pure(notFound());
        }

        final List<models.PackageVersion> versions = Lists.newArrayList();
        final JsonNode requestJson = request().body().asJson();
        if (requestJson == null) {
            return F.Promise.pure(badRequest());
        }
        final ArrayNode packages = (ArrayNode) requestJson.get("packages");
        for (final JsonNode node : packages) {
            final ObjectNode packageNode = (ObjectNode) node;
            final String packageName = packageNode.get("name").asText();
            final String version = packageNode.get("version").asText();
            final PackageVersion pkgVersion = getPackageVersion(packageName, version);
            versions.add(pkgVersion);
        }

        final ManifestHistory currentHistory = ManifestHistory.getCurrentForStage(stage);
        final Manifest currentManifest = currentHistory.getManifest();

        final LinkedHashMap<String, PackageVersion> newPackages = Maps.newLinkedHashMap(currentManifest.asPackageMap());
        versions.forEach(pv -> newPackages.put(pv.getPkg().getName(), pv));
        final Manifest newManifest = new Manifest();
        newManifest.getPackages().addAll(newPackages.values());
        newManifest.save();

        final Future<Object> ask = Patterns.ask(
                _deploymentManager,
                new FleetDeploymentCommands.DeployStage(stage, newManifest, "api"),
                Timeout.apply(30L, TimeUnit.SECONDS));

        return F.Promise.wrap(ask).map(
                o -> {
                    if (o instanceof Deployment) {
                        final Deployment deployment = (Deployment) o;
                        return ok(JsonNodeFactory.instance.objectNode().put("deployId", deployment.getId()));
                    }
                    Logger.error("Expected Deployment response from deployment manager, got " + o);
                    return internalServerError();
                });
    }

    private PackageVersion getPackageVersion(final String packageName, final String version) {
        Package packageModel = Package.getByName(packageName);
        if (packageModel == null) {
            packageModel = new Package();
            packageModel.setName(packageName);
            packageModel.save();
        }

        RollerPackageVersion pkgVersion = RollerPackageVersion.getByPackageAndVersion(packageModel, version);
        if (pkgVersion == null) {
            pkgVersion = new RollerPackageVersion();
            pkgVersion.setPkg(packageModel);
            pkgVersion.setVersion(version);
            pkgVersion.save();
        }
        return pkgVersion;
    }

    @Override
    public F.Promise<Result> getReleasePreview(final String envName, final String version) {
        final Environment environment = Environment.getByName(envName);
        if (environment == null) {
            return F.Promise.pure(notFound());
        }

        final Manifest manifest = Manifest.getVersion(environment, version);
        if (manifest == null) {
            return F.Promise.pure(notFound());
        }

        final ObjectNode node = Json.newObject();
        for (final PackageVersion packageVersion : manifest.getPackages()) {
            final ArrayNode resultsArray = node.putArray("results");
            final ObjectNode entryNode = Json.newObject();
            entryNode.put("package", packageVersion.getPkg().getName());
            entryNode.put("version", packageVersion.getVersion());
            resultsArray.add(entryNode);
        }
        return F.Promise.pure(ok(node));
    }

    @Override
    public F.Promise<Result> deploymentLog(final long deploymentId) {
        final Deployment deployment = Deployment.getById(deploymentId);
        if (deployment == null) {
            return F.Promise.pure(notFound());
        }
        final Tuple2<Enumerator<String>, Concurrent.Channel<String>> channelTuple = Concurrent.broadcast();
        final Enumerator<String> enumerator = channelTuple._1();
        final Concurrent.Channel<String> channel = channelTuple._2();

        final ActorRef relayActor = Akka.system().actorOf(DeployLogRelay.props(channel, deploymentId));
        final Enumeratee<String, String> done = Enumeratee$.MODULE$.onIterateeDone(
                JFunction.proc(
                        () -> {
                            relayActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
                            Logger.debug("Log session disconnected");
                        }),
                Akka.system().dispatcher());

        response().setContentType("text/event-stream");
        final play.api.mvc.Result result = Results$.MODULE$.Ok().feed(enumerator.$amp$greater(done), Writeable.wString(Codec.utf_8()));
        return F.Promise.pure(() -> result);
    }

    private final ActorRef _deploymentManager;
}
