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

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;

/**
 * Represents a set of versioned packages for deployment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
public class Manifest extends Model {
    public List<PackageVersion> getPackages() {
        return new ArrayList<>(packages);
    }

    public void setPackages(final List<RollerPackageVersion> value) {
        packages = value;
    }

    public List<DockerImageVersion> getDockerImages() {
        return dockerImages;
    }

    public void setDockerImages(List<DockerImageVersion> dockerImages) {
        this.dockerImages = dockerImages;
    }

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String value) {
        createdBy = value;
    }

    public void setEnvironment(final Environment value) {
        environment = value;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String value) {
        version = value;
    }

    /**
     * Converts the manifest to a {@link Map}.
     *
     * @return a map of the manifest
     */
    public Map<String, PackageVersion> asPackageMap() {
        return Maps.uniqueIndex(
                getPackages(), new Function<PackageVersion, String>() {
                    @Nullable
                    @Override
                    public String apply(final PackageVersion input) {
                        return input.getPkg().getName();
                    }
                });
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Get the latest manifest in an environment.
     *
     * @param environment the environment
     * @return the latest in an environment or null if it doesn't exist
     */
    @Nullable
    public static Manifest getLatestManifest(final Environment environment) {
        return FINDER.where().eq("environment", environment).orderBy().desc("id").setMaxRows(1).findUnique();
    }

    /**
     * Get a manifest by environment and version.
     *
     * @param environment the environment
     * @param version the version
     * @return the manifest or null if it doesn't exist
     */
    @Nullable
    public static Manifest getVersion(final Environment environment, final String version) {
        return FINDER.where().eq("environment", environment).eq("version", version).findUnique();
    }

    /**
     * Look up a {@link Manifest} by id.
     *
     * @param id the id
     * @return the manifest, or null if it doesn't exist
     */
    @Nullable
    public static Manifest getById(final long id) {
        return FINDER.byId(id);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * One of package and dockerImages will be empty, according to environment.environmentType.
     */
    @ManyToMany
    @OrderBy("pkg.name asc")
    private List<RollerPackageVersion> packages;

    @ManyToMany
    @OrderBy("pkg.name asc")
    private List<DockerImageVersion> dockerImages;

    //NOTE: createdBy serves as a field to put into the database to prevent
    // an ebean bug that doesn't allow the creation of "empty" table records in postgres
    private String createdBy;
    @CreatedTimestamp
    private DateTime createdAt;
    @ManyToOne
    private Environment environment;
    private String version;

    private static final Find<Long, Manifest> FINDER = new Find<Long, Manifest>(){};
}
