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
package global;

import actors.DeployManager;
import actors.DockerPackageRefresher;
import actors.HostclassRefresher;
import actors.JvmMetricsCollector;
import actors.PackageRefresher;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import client.DeploymentClientFactory;
import client.DockerDeploymentClient;
import client.DockerSshClient;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdLogSink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.groupon.deployment.SshSessionFactory;
import com.groupon.deployment.SshjSessionFactory;
import com.groupon.deployment.fleet.FleetDeploymentFactory;
import com.groupon.deployment.fleet.Sequential;
import com.groupon.deployment.host.HostDeploymentFactory;
import com.groupon.deployment.host.Roller;
import com.groupon.guice.akka.GuiceActorCreator;
import com.groupon.guice.akka.RootActorProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import play.Configuration;

import java.io.File;
import java.util.Collections;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Contains the dependency injection bindings for the app.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ProdModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SshSessionFactory.class).to(SshjSessionFactory.class);
        bind(ActorRef.class)
                .annotatedWith(Names.named("HostclassRefresher"))
                .toProvider(HostclassRefresherProvider.class)
                .asEagerSingleton();
        bind(ActorRef.class).annotatedWith(Names.named("PackageRefresher")).toProvider(PackageRefresherProvider.class).asEagerSingleton();
        bind(ActorRef.class)
                .annotatedWith(Names.named("DockerPackageRefresher"))
                .toProvider(DockerPackageRefresherProvider.class)
                .asEagerSingleton();
        bind(ActorRef.class).annotatedWith(Names.named("DeployManager")).toProvider(DeployManagerProvider.class).asEagerSingleton();
        bind(ActorRef.class)
                .annotatedWith(Names.named("JvmMetricsCollector"))
                .toProvider(JvmMetricsCollectorProvider.class)
                .asEagerSingleton();
        install(
                new FactoryModuleBuilder()
                        .implement(DockerDeploymentClient.class, DockerSshClient.class)
                        .build(DeploymentClientFactory.class));
        install(
                new FactoryModuleBuilder()
                        .implement(Sequential.class, Sequential.class)
                        .build(FleetDeploymentFactory.class));
        install(new FactoryModuleBuilder()
                        .implement(Roller.class, Roller.class)
                                // TODO(barp): add the docker fun [Artemis-?]
                        .build(HostDeploymentFactory.class));
    }


    @Provides @Named("ConfigServerBaseUrl")
    String provideConfigServerBaseURL(final Configuration config) {
        return config.getString("artemis.roller.configServer");
    }

    @Provides @Named("DockerRegistryUrl")
    String provideDockerRegistryUrl(final Configuration config) {
        return config.getString("artemis.dockerRegistry");
    }

    @Provides
    @Singleton
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD") // Invoked reflectively by Guice
    private MetricsFactory getMetricsFactory(final Configuration configuration) {
        return new TsdMetricsFactory.Builder()
                .setClusterName(configuration.getString("metrics.cluster"))
                .setServiceName(configuration.getString("metrics.service"))
                .setSinks(Collections.singletonList(
                        new TsdLogSink.Builder()
                                .setName(configuration.getString("metrics.name"))
                                .setDirectory(new File(configuration.getString("metrics.path")))
                                .build()
                ))
                .build();
    }

    @Provides @Named("DockerRegistryName")
    String provideDockerRegistryName(final Configuration config) {
        return config.getString("artemis.dockerRegistryName");
    }

    @Provides @Named("DockerCmd")
    String provideDockerCmd(final Configuration config) {
        return config.getString("artemis.dockerCmd");
    }

    @Singleton
    private static final class HostclassRefresherProvider extends RootActorProvider {
        @Inject
        private HostclassRefresherProvider(final ActorSystem system, final Injector injector) {
            super(system, injector, HostclassRefresher.class, "HostclassRefresher");
        }
    }

    @Singleton
    private static final class PackageRefresherProvider extends RootActorProvider {
        @Inject
        private PackageRefresherProvider(final ActorSystem system, final Injector injector) {
            super(system, injector, PackageRefresher.class, "PackageRefresher");
        }
    }

    @Singleton
    private static final class DeployManagerProvider extends RootActorProvider {
        @Inject
        private DeployManagerProvider(final ActorSystem system, final Injector injector) {
            super(system, injector, DeployManager.class, "DeployManager");
        }
    }

    @Singleton
    private static final class DockerPackageRefresherProvider extends RootActorProvider {
        @Inject
        private DockerPackageRefresherProvider(final ActorSystem system, final Injector injector) {
            super(system, injector, DockerPackageRefresher.class, "DockerPackageRefresher");
        }
    }

    private static final class JvmMetricsCollectorProvider implements Provider<ActorRef> {
        @Inject
        private JvmMetricsCollectorProvider(final Injector injector, final ActorSystem system) {
            _injector = injector;
            _system = system;
        }

        @Override
        public ActorRef get() {
            return _system.actorOf(GuiceActorCreator.props(_injector, JvmMetricsCollector.class));
        }

        private final Injector _injector;
        private final ActorSystem _system;
    }
}
