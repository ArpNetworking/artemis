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

import io.ebean.Finder;
import io.ebean.Model;

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

/**
 * Represents an environment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Environment extends Model {
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

    public Owner getOwner() {
        return owner;
    }

    public Environment getParent() {
        return parent;
    }

    public void setParent(final Environment value) {
        parent = value;
    }

    public void setOwner(final Owner value) {
        owner = value;
    }

    public List<Environment> getChildren() {
        return children;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String value) {
        config = value;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(final EnvironmentType value) {
        environmentType = value;
    }

    @OrderBy("createdAt desc")
    public List<Manifest> getManifests() {
        return manifests;
    }

    /**
     * Look up environments owned by a set of organizations.
     *
     * @param orgs organizations
     * @param maxRows maximum number of environments to return
     * @return a list of environments
     */
    public static List<Environment> getEnvironmentsForOrgs(final List<Owner> orgs, final int maxRows) {
        return FINDER.query().where().in("owner", orgs).orderBy("name").setMaxRows(maxRows).findList();
    }

    /**
     * Look up an environment by name.
     *
     * @param name the name to look up
     * @return an environment, or null if it does not exist
     */
    @Nullable
    public static Environment getByName(final String name) {
        return FINDER.query().where().eq("name", name).findOne();
    }

    /**
     * Look up environment by id.
     *
     * @param id the id
     * @return an environment, or null if it does not exist
     */
    public static Environment getById(final Long id) {
        return FINDER.byId(id);
    }

    /**
     * Search for an environment.
     *
     * @param query the query string
     * @param limit max number of results
     * @return a list of environment results
     */
    public static List<Environment> searchByPartialName(final String query, final int limit) {
        return FINDER.query().where().ilike("name", String.format("%%%s%%", query)).setMaxRows(limit).findList();
    }

    @Version
    private Long version;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @ManyToOne
    private Owner owner;
    @ManyToOne
    private Environment parent;
    @OneToMany(mappedBy = "parent")
    private List<Environment> children;
    @OneToMany(mappedBy = "environment")
    private List<Stage> stages;
    @OneToMany(mappedBy = "environment")
    private List<Manifest> manifests;
    private EnvironmentType environmentType;
    private String config;

    private static final Finder<Long, Environment> FINDER = new Finder<>(Environment.class);
}
