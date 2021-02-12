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
package actors;

import akka.actor.AbstractActor;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import client.DockerPackageClient;
import client.DockerPackageClient.PackageListResponse;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.Package;
import models.PackageVersion;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

/**
 * Refreshes docker packages from a docker registry.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
@Singleton
public class DockerPackageRefresher extends AbstractActor {

    /**
     * Public constructor.
     *
     * @param dockerClient The client to use to interact with the docker registry
     * @param registryName The string to be put in the Package model's 'repository' field
     */
    @Inject
    public DockerPackageRefresher(final DockerPackageClient dockerClient, @Named("DockerRegistryName") final String registryName) {
        context().system().scheduler().scheduleWithFixedDelay(
                Duration.ofSeconds(3),
                Duration.ofHours(3),
                self(),
                new RefreshPackagesMessage(),
                context().dispatcher(),
                self());
        _dockerPackageClient = dockerClient;
        _registryName = registryName;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RefreshPackagesMessage.class, message -> {
                    if (_refreshState == RefreshStates.READY) {
                        fetchPackagesAndSendMessage();
                    } else {
                        _refreshState = RefreshStates.QUEUED;
                    }
                })
                .match(PackageListMessage.class, message -> {
                    final DockerPackageClient.PackageListResponse list = message.getList();
                    final Map<String, List<DockerPackageClient.ImageMetadata>> packages = list.getPackages();
                    persistImagesToPackageVersions(packages);

                    if (_refreshState == RefreshStates.QUEUED) {
                        self().tell(new RefreshPackagesMessage(), self());
                    }
                    _refreshState = RefreshStates.READY;

                })
                .build();
    }

    private void fetchPackagesAndSendMessage() {
        _refreshState = RefreshStates.RUNNING;
        final CompletionStage<PackageListMessage> imagesListPromise =
                _dockerPackageClient
                        .getAllPackages()
                        .thenApply(PackageListMessage::new);
        Patterns.pipe(imagesListPromise, context().dispatcher()).to(self(), self());
    }

    private void persistImagesToPackageVersions(final Map<String, List<DockerPackageClient.ImageMetadata>> repoToImageMap) {

        repoToImageMap.forEach((repoName, imageMetadataList) -> {
            try (Transaction transaction = Ebean.beginTransaction()) {
                Package aPackage = Package.getByName(repoName);
                if (aPackage == null) {
                    aPackage = new Package();
                    aPackage.setName(repoName);
                    aPackage.save();
                }

                for (final DockerPackageClient.ImageMetadata imageMetadata : imageMetadataList) {
                    PackageVersion packageVersion = PackageVersion.getByRepositoryPackageAndVersion(_registryName,
                                                                                                    aPackage,
                                                                                                    imageMetadata.getId());

                    if (packageVersion == null) {
                        packageVersion = new PackageVersion();
                        packageVersion.setType(TYPE_FOR_PACKAGES);
                        packageVersion.setRepository(_registryName);
                        packageVersion.setVersion(imageMetadata.getId());
                        packageVersion.setPkg(aPackage);
                    }
                    // TODO(mhayter): format the description nicely [Artemis-?]

                    // Take the first 254 characters of description
                    final String description = imageMetadata.getTags().toString();
                    packageVersion.setDescription(description.substring(0, Math.min(description.length(), 254)));

                    packageVersion.save();
                }
                transaction.commit();
            }
        });
    }

    private RefreshStates _refreshState = RefreshStates.READY;
    private final DockerPackageClient _dockerPackageClient;
    private final String _registryName;

    private static final String TYPE_FOR_PACKAGES = "docker";
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerPackageRefresher.class);

    private enum RefreshStates {
        READY,
        QUEUED,
        RUNNING
    }

    private static class PackageListMessage {
        private final PackageListResponse _list;

        PackageListMessage(final PackageListResponse list) {
            _list = list;
        }

        public PackageListResponse getList() {
            return _list;
        }
    }

    private static class RefreshPackagesMessage {}
}
