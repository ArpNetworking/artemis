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
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.Timeout;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
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
import models.Stage;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * JSON REST Apis.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class StandardApi extends Controller implements Api {
    /**
     * Public constructor.
     *
     * @param deploymentManager the deployment manager
     * @param actorSystem the actor system to create actors in
     */
    @Inject
    public StandardApi(@Named("DeployManager") final ActorRef deploymentManager, final ActorSystem actorSystem) {
        _deploymentManager = deploymentManager;
        _actorSystem = actorSystem;
    }

    @Override
    public CompletionStage<Result> hostclassSearch(final String query, final Http.Request request) {
        final ObjectNode node = Json.newObject();
        final List<models.Hostclass> hostclasses = models.Hostclass.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final models.Hostclass hostclass : hostclasses) {
            resultsArray.add(hostclass.getName());
        }
        return CompletableFuture.completedFuture(ok(node));
    }

    @Override
    public CompletionStage<Result> packageSearch(final String query, final Http.Request request) {
        final ObjectNode node = Json.newObject();
        final List<models.Package> packages = models.Package.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final models.Package pkg : packages) {
            resultsArray.add(pkg.getName());
        }
        return CompletableFuture.completedFuture(ok(node));
    }

    @Override
    public CompletionStage<Result> environmentSearch(final String query, final Http.Request request) {
        final ObjectNode node = Json.newObject();
        final List<Environment> environments = Environment.searchByPartialName(query, 10);
        final ArrayNode resultsArray = node.putArray("results");
        for (final Environment env : environments) {
            resultsArray.add(env.getName());
        }
        return CompletableFuture.completedFuture(ok(node));
    }

    @Override
    public CompletionStage<Result> getStages(final String envName, final Http.Request request) {
        final Environment environment = Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        final ObjectNode node = Json.newObject();
        final ArrayNode resultsArray = node.putArray("results");
        for (final Stage stage : environment.getStages()) {
            resultsArray.add(stage.getName());
        }
        return CompletableFuture.completedFuture(ok(node));
    }

    //TODO(barp): Authenticate this [Artemis-?]
    @Override
    public CompletionStage<Result> updateStagePackageVersions(final String envName, final String stageName,
            final Http.Request request) {
        final models.Stage stage = models.Stage.getByEnvironmentNameAndName(envName, stageName);
        if (stage == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        final List<models.PackageVersion> versions = Lists.newArrayList();
        final JsonNode requestJson = request.body().asJson();
        if (requestJson == null) {
            return CompletableFuture.completedFuture(badRequest());
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

        final CompletionStage<Object> ask = Patterns.ask(
                _deploymentManager,
                new FleetDeploymentCommands.DeployStage(stage, newManifest, "api"),
                Duration.ofSeconds(30));

        return ask.thenApply(
                o -> {
                    if (o instanceof Deployment) {
                        final Deployment deployment = (Deployment) o;
                        return ok(JsonNodeFactory.instance.objectNode().put("deployId", deployment.getId()));
                    }
                    LOGGER.error("Expected Deployment response from deployment manager, got " + o);
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

        PackageVersion pkgVersion = PackageVersion.getByPackageAndVersion(packageModel, version);
        if (pkgVersion == null) {
            pkgVersion = new PackageVersion();
            pkgVersion.setPkg(packageModel);
            pkgVersion.setVersion(version);
            pkgVersion.save();
        }
        return pkgVersion;
    }

    @Override
    public CompletionStage<Result> getReleasePreview(final String envName, final String version, final Http.Request request) {
        final Environment environment = Environment.getByName(envName);
        if (environment == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        final Manifest manifest = Manifest.getVersion(environment, version);
        if (manifest == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        final ObjectNode node = Json.newObject();
        for (final PackageVersion packageVersion : manifest.getPackages()) {
            final ArrayNode resultsArray = node.putArray("results");
            final ObjectNode entryNode = Json.newObject();
            entryNode.put("package", packageVersion.getPkg().getName());
            entryNode.put("version", packageVersion.getVersion());
            resultsArray.add(entryNode);
        }
        return CompletableFuture.completedFuture(ok(node));
    }

    @Override
    public CompletionStage<Result> deploymentLog(final long deploymentId, final Http.Request request) {
        final Deployment deployment = Deployment.getById(deploymentId);
        if (deployment == null) {
            return CompletableFuture.completedFuture(notFound());
        }

        final Source<ByteString, ?> source = Source.<String>actorRef((e) -> Optional.empty(), (e) -> Optional.empty(), 1024, OverflowStrategy.dropTail())
                .map(ByteString::fromString)
                .mapMaterializedValue(outRef -> {
                    final ActorRef relayActor = _actorSystem.actorOf(DeployLogRelay.props(outRef, deploymentId));
                    relayActor.tell(outRef, ActorRef.noSender());
                    return outRef;
                });
        return CompletableFuture.completedFuture(Results.ok().chunked(source).as("text/event-stream"));
    }

    private final ActorRef _deploymentManager;
    private final ActorSystem _actorSystem;
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardApi.class);
}
