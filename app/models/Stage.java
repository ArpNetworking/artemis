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
package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import org.joda.time.DateTime;
import utils.NoIncluder;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

/**
 * Represents a stage in the database.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"environment_id", "name"}))
public class Stage extends Model {
    public String getConfig() {
        return config;
    }

    public void setConfig(final String value) {
        config = value;
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public void setId(final long value) {
        id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        name = value;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(final Environment value) {
        environment = value;
    }

    public Set<Hostclass> getHostclasses() {
        return hostclasses;
    }

    public void setHostclasses(final Set<Hostclass> value) {
        hostclasses = value;
    }

    /**
     * Look up a stage by name and environment name.
     *
     * @param environmentName the environment name
     * @param name the stage name
     * @return an environment, or null if it doesn't exist
     */
    @Nullable
    public static Stage getByEnvironmentNameAndName(final String environmentName, final String name) {
        return FINDER.where().eq("environment.name", environmentName).eq("name", name).findUnique();
    }

    /**
     * Look up all the stages for a hostclass.
     *
     * @param hostclass the hostclass
     * @return a list of stages
     */
    public static List<Stage> getStagesForHostclass(final Hostclass hostclass) {
        return FINDER.where().eq("hostclasses", hostclass).findList();
    }

    /**
     * Look up stage by id.
     *
     * @param id the id
     * @return the stage, or null if it doesn't exist
     */
    public static Stage getById(final Long id) {
        return FINDER.where().eq("id", id).findUnique();
    }

    /**
     * Apply a manifest to a stage.
     *
     * @param stage the stage
     * @param manifest the manifest
     * @return a new {@link ManifestHistory} record representing the application
     */
    public static ManifestHistory applyManifestToStage(final Stage stage, final Manifest manifest) {
        if (Ebean.currentTransaction() == null) {
            throw new IllegalStateException("Must be in a transaction to call this function");
        }

        final DateTime switchTime = DateTime.now();
        final ManifestHistory currentManifest = ManifestHistory.getCurrentForStage(stage);
        final ManifestHistory newManifest = new ManifestHistory();
        newManifest.setStage(stage);
        newManifest.setFinish(null);
        newManifest.setStart(switchTime);
        newManifest.setManifest(manifest);
        newManifest.setConfig(computeConfig(stage));
        if (currentManifest != null) {
            currentManifest.setFinish(switchTime);
            currentManifest.save();
        }

        newManifest.save();
        return newManifest;
    }

    private static String computeConfig(final Stage stage) {
        final ConfigParseOptions parseOptions = ConfigParseOptions.defaults().setIncluder(new NoIncluder()).setAllowMissing(false);
        Config config = ConfigFactory.empty();
        if (stage.getConfig() != null) {
            config  = ConfigFactory.parseString(stage.getConfig(), parseOptions);
        }
        Environment env = stage.getEnvironment();
        while (env != null) {
            if (env.getConfig() != null) {
                config = config.withFallback(ConfigFactory.parseString(env.getConfig(), parseOptions));
            }
            env = env.getParent();
        }
        return config.root().render(ConfigRenderOptions.concise());
    }

    @Version
    private Long version;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @ManyToOne
    private Environment environment;
    @ManyToMany
    private Set<Hostclass> hostclasses;
    private String config;

    private static final Find<Long, Stage> FINDER = new Find<Long, Stage>(){};

}
