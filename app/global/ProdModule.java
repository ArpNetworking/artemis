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
import client.HostProvider;
import client.PackageProvider;
import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdLogSink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector;
import com.fasterxml.jackson.module.guice.GuiceInjectableValues;
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
import com.groupon.deployment.host.HostDeploymentFactory;
import com.groupon.guice.akka.RootActorProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import play.Configuration;
import utils.JsonConfigBridge;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.inject.Singleton;

/**
 * Contains the dependency injection bindings for the app.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ProdModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SshSessionFactory.class).to(SshjSessionFactory.class).asEagerSingleton();
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
                        .build(FleetDeploymentFactory.class));
        install(new FactoryModuleBuilder()
                        .build(HostDeploymentFactory.class));
    }

    @Provides
    @Singleton
    HostProvider provideHostProvider(final Configuration configuration, final ObjectMapper objectMapper) throws IOException {
        final Configuration providerConfig = configuration.getConfig("hostProvider");
        return JsonConfigBridge.load(providerConfig, HostProvider.class, objectMapper);
    }

    @Provides
    @Singleton
    PackageProvider providePackageProvider(final Configuration configuration, final ObjectMapper objectMapper) throws IOException {
        final Configuration providerConfig = configuration.getConfig("packageProvider");
        return JsonConfigBridge.load(providerConfig, PackageProvider.class, objectMapper);
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper(final Injector injector) {
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();
        final GuiceAnnotationIntrospector guiceIntrospector = new GuiceAnnotationIntrospector();
        objectMapper.setInjectableValues(new GuiceInjectableValues(injector));
        objectMapper.setAnnotationIntrospectors(
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getSerializationConfig().getAnnotationIntrospector()),
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getDeserializationConfig().getAnnotationIntrospector()));

        return objectMapper;
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

    @Singleton
    private static final class JvmMetricsCollectorProvider extends RootActorProvider {
        @Inject
        private JvmMetricsCollectorProvider(final Injector injector, final ActorSystem system) {
            super(system, injector, JvmMetricsCollector.class, "JvmMetricsCollector");
        }
    }
}
