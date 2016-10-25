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
package controllers.config;

import api.HostclassOutput;
import client.ConfigServerClient;
import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.name.Named;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueFactory;
import models.Host;
import models.Hostclass;
import models.Manifest;
import models.ManifestHistory;
import models.PackageVersion;
import models.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.libs.ws.WSClient;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

/**
 * Acts as a config server.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Config extends Proxy {

    /**
     * Public constructor.
     *
     * @param baseURL base url of the config server
     * @param client ws client to use
     */
    @Inject
    public Config(@Named("ConfigServerBaseUrl") final String baseURL, final WSClient client) {
        super(baseURL, client);
        _configClient = new ConfigServerClient(baseURL, client);
    }

    /**
     * Overlays the host configuration.
     *
     * @param hostname the hostname
     * @return a host configuration yaml response
     */
    public CompletionStage<Result> host(final String hostname) {
        LOGGER.info(String.format("Request for host; host=%s", hostname));
        return _configClient.getHostData(hostname).thenApply(
                hostOutput -> {
                    // Find the host
                    final Host host = Host.getByName(hostname);
                    if (host == null) {
                        try {
                            return ok(YAML_MAPPER.writeValueAsString(hostOutput));
                        } catch (final JsonProcessingException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                    final String rawHostclass = hostOutput.getHostclass();
                    hostOutput.setHostclass(rawHostclass + "::" + host.getHostclass().getName());
                    final Map<String, Object> params = hostOutput.getParams();
                    final List<HostRedirect> lockMappings = Lists.newArrayList();
                    if (params.containsKey("hosts_redirect")) {
                        @SuppressWarnings("unchecked")
                        final List<Map<String, Object>> hostsRedirect = (List<Map<String, Object>>) params.get("hosts_redirect");
                        for (final Map<String, Object> map : hostsRedirect) {
                            final HostRedirect redirect = new HostRedirect(
                                    (String) map.get("src_host"),
                                    Optional.ofNullable((Integer) map.getOrDefault("src_port", null)),
                                    (String) map.get("dst_host"),
                                    Optional.ofNullable((Integer) map.getOrDefault("dst_port", null)));
                            lockMappings.add(redirect);
                        }
                    }
                    params.put("hosts_redirect", lockMappings);

                    final List<Configuration> configList = Configuration.root().getConfigList("package.lock.replacements");

                    for (final Configuration configuration : configList) {
                        final Map<String, Object> map = configuration.asMap();
                        final HostRedirect redirect = new HostRedirect(
                                (String) map.get("src_host"),
                                Optional.ofNullable((Integer) map.getOrDefault("src_port", null)),
                                (String) map.get("dst_host"),
                                Optional.ofNullable((Integer) map.getOrDefault("dst_port", null)));

                        if (lockMappings.stream().filter(
                                (k) ->
                                        redirect._sourceHost.equals(k._sourceHost) && redirect._sourcePort.equals(k._sourcePort))
                                .count() == 0) {
                            lockMappings.add(redirect);
                        }
                    }

                    try {
                        return ok(YAML_MAPPER.writeValueAsString(hostOutput));
                    } catch (final JsonProcessingException e) {
                        throw Throwables.propagate(e);
                    }
                }
        );
    }

    /**
     * Overlays the hostclass configuration.
     *
     * @param rawHostclass the hostclass
     * @return a hostclass configuration yaml response
     */
    public CompletionStage<Result> hostclass(final String rawHostclass) {
        final String rollerHostclass = Iterables.get(
                Splitter.on("-")
                        .trimResults()
                        .limit(2)
                        .omitEmptyStrings()
                        .split(rawHostclass), 0);
        final List<String> splitHostclass = Splitter.on("::")
                .trimResults()
                .limit(2)
                .omitEmptyStrings()
                .splitToList(rawHostclass);
        final Optional<String> artemisHostclass = Optional.ofNullable(Iterables.get(splitHostclass, 1, null));
        final Optional<String> rollerVersionedHostclass = Optional.ofNullable(Iterables.get(splitHostclass, 0, null));
        LOGGER.info(
                String.format(
                        "Request for hostclass; rollerHostclass=%s, artemisHostclass=%s",
                        rollerHostclass,
                        artemisHostclass.orElse("[null]")));
        return _configClient.getHostclassData(rollerVersionedHostclass.get()).thenApply(
                hostclassOutput -> {
                    if (artemisHostclass.isPresent()) {
                        final Hostclass hostclass = Hostclass.getByName(artemisHostclass.get());
                        if (hostclass == null) {
                            return notFound();
                        }

                        overlayHostclass(hostclassOutput, hostclass);
                    }
                    try {
                        return ok(YAML_MAPPER.writeValueAsString(hostclassOutput));
                    } catch (final JsonProcessingException e) {
                        throw Throwables.propagate(e);
                    }
                }
        );
    }

    private void overlayHostclass(final HostclassOutput hostclassOutput, final Hostclass hostclass) {
        final ImmutableMap<String, HostclassOutput.Package> oldPackageMap = Maps.uniqueIndex(
                hostclassOutput.getPackages().getProduction(), HostclassOutput.Package::getName);
        final Map<String, HostclassOutput.Package> newPackages = Maps.newHashMap(oldPackageMap);

        final ImmutableMap<String, HostclassOutput.Package> oldFailsafeMap = Maps.uniqueIndex(
                hostclassOutput.getPackages().getFailsafe(), HostclassOutput.Package::getName);
        final Map<String, HostclassOutput.Package> newFailsafe = Maps.newHashMap(oldFailsafeMap);

        final List<Stage> stages = Stage.getStagesForHostclass(hostclass);
        for (final Stage stage : stages) {
            final ManifestHistory snapshot = ManifestHistory.getCurrentForStage(stage);
            if (snapshot == null) {
                LOGGER.warn("snapshot for stage " + stage.getName() + "[" + stage.getId() + "] was null");
                continue;
            }
            final Manifest manifest = snapshot.getManifest();
            for (final PackageVersion aPackage : manifest.getPackages()) {
                newPackages.put(
                        aPackage.getPkg().getName(),
                        new HostclassOutput.Package(
                                String.format(
                                        "%s-%s",
                                        aPackage.getPkg().getName(),
                                        aPackage.getVersion())));
            }
            if (!Strings.isNullOrEmpty(snapshot.getConfig())) {
                injectConfig(snapshot, hostclassOutput);
            }
        }
        if (stages.size() > 0) {
            Configuration.root()
                    .getStringList("package.overlay")
                    .stream()
                    .map(HostclassOutput.Package::new)
                    .forEach(
                            p -> {
                                newPackages.put(p.getName(), p);
                                newFailsafe.put(p.getName(), p);
                            });
        }
        final ArrayList<HostclassOutput.Package> packageList = Lists.newArrayList(newPackages.values());
        Collections.sort(packageList, Comparator.comparing(HostclassOutput.Package::getName));
        hostclassOutput.getPackages().setProduction(Sets.newLinkedHashSet(packageList));

        final ArrayList<HostclassOutput.Package> failsafeList = Lists.newArrayList(newFailsafe.values());
        Collections.sort(failsafeList, Comparator.comparing(HostclassOutput.Package::getName));
        hostclassOutput.getPackages().setFailsafe(Sets.newLinkedHashSet(failsafeList));
    }

    private void injectConfig(final ManifestHistory snapshot, final HostclassOutput hostclassOutput) {
        try {
            final com.typesafe.config.Config config = ConfigFactory.parseString(snapshot.getConfig());
            final ConfigObject origParams = ConfigValueFactory.fromMap(hostclassOutput.getParams());
            final ConfigObject resolvedConfig = config.withFallback(origParams).root();
            hostclassOutput.setParams(resolvedConfig.unwrapped());
        } catch (final ConfigException e) {
            LOGGER.warn("Error parsing config for snapshot id=" + snapshot.getId(), e);
        }
    }

    private final ConfigServerClient _configClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final ObjectMapper YAML_MAPPER;

    static {
        final YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
        YAML_MAPPER = ObjectMapperFactory.createInstance(factory);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static final class HostRedirect {
        private HostRedirect(
                final String sourceHost,
                final Optional<Integer> sourcePort,
                final String destinationHost,
                final Optional<Integer> destinationPort) {
            _sourceHost = sourceHost;
            _sourcePort = sourcePort;
            _destinationHost = destinationHost;
            _destinationPort = destinationPort;
        }

        @JsonProperty("dst_host")
        public String getDestinationHost() {
            return _destinationHost;
        }

        @JsonProperty("dst_port")
        public Optional<Integer> getDestinationPort() {
            return _destinationPort;
        }

        @JsonProperty("src_host")
        public String getSourceHost() {
            return _sourceHost;
        }

        @JsonProperty("src_port")
        public Optional<Integer> getSourcePort() {
            return _sourcePort;
        }

        private final String _sourceHost;
        private final Optional<Integer> _sourcePort;
        private final String _destinationHost;
        private final Optional<Integer> _destinationPort;
    }
}


