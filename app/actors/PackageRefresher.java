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
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import client.PackageProvider;
import com.google.inject.Inject;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.Package;
import models.PackageVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

/**
 * Refreshes the packages from the package server.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class PackageRefresher extends AbstractActor {
    /**
     * Creates a {@link Props} to create this actor.
     *
     * @param packageClient The package client to use
     * @return a new {@link Props}
     */
    public static Props props(final PackageProvider packageClient) {
        return Props.create(PackageRefresher.class, packageClient);
    }

    /**
     * Public constructor.
     *
     * @param packageClient the package client
     */
    @Inject
    public PackageRefresher(final PackageProvider packageClient) {
        context().system().scheduler().scheduleWithFixedDelay(
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                FiniteDuration.apply(2, TimeUnit.HOURS),
                self(),
                new RefreshPackagesMessage(),
                context().dispatcher(),
                self());
        _packageClient = packageClient;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RefreshPackagesMessage.class, refresh -> {
                    final CompletionStage<PackageListMessage> messagePromise = _packageClient.getAllPackages().thenApply(
                            PackageListMessage::new
                    );
                    Patterns.pipe(messagePromise, context().dispatcher()).to(self(), self());
                })
                .match(PackageListMessage.class, packageListMessage -> {
                    final Map<String, List<PackageProvider.PackageVersionMetadata>> packages = packageListMessage
                            .getList()
                            .getPackages();
                    for (final Map.Entry<String, List<PackageProvider.PackageVersionMetadata>> entry : packages.entrySet()) {
                        final String packageName = entry.getKey();
                        try (Transaction transaction = Ebean.beginTransaction()) {
                            Package aPackage = Package.getByName(packageName);
                            if (aPackage == null) {
                                aPackage = new Package();
                                aPackage.setName(packageName);
                                aPackage.save();
                            }

                            for (final PackageProvider.PackageVersionMetadata versionMetadata : entry.getValue()) {
                                PackageVersion version = PackageVersion.getByPackageAndVersion(aPackage,
                                        versionMetadata.getVersion());
                                if (version == null) {
                                    version = new PackageVersion();
                                    version.setVersion(versionMetadata.getVersion());
                                    version.setPkg(aPackage);
                                    version.save();
                                }
                            }
                            transaction.commit();
                        } catch (final PersistenceException e) {
                            LOGGER.warn(String.format("Package %s not updated", packageName), e);
                        }
                    }
                })
                .build();
    }

    private final PackageProvider _packageClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageRefresher.class);

    private static class RefreshPackagesMessage { }

    private static class PackageListMessage {
        PackageListMessage(final PackageProvider.PackageListResponse list) {
            _list = list;
        }

        public PackageProvider.PackageListResponse getList() {
            return _list;
        }

        private final PackageProvider.PackageListResponse _list;
    }
}
