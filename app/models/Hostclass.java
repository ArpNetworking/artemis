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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.ebean.Finder;
import io.ebean.Model;

import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Database model representing a hostclass by name.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Hostclass extends Model {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        name = value;
    }

    public Hostclass getParent() {
        return parent;
    }

    public List<Hostclass> getChildren() {
        return children;
    }

    public void setParent(final Hostclass value) {
        parent = value;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(final List<Host> value) {
        hosts = value;
    }

    public Set<Stage> getStages() {
        return stages;
    }

    public void setStages(final Set<Stage> value) {
        stages = value;
    }

    /**
     * Look up by name.
     *
     * @param name the name
     * @return the hostclass, or null if it doesn't exist
     */
    public static Hostclass getByName(final String name) {
        return FINDER.query().where().eq("name", name).findOne();
    }

    /**
     * Look up by id.
     *
     * @param id the id
     * @return the hostclass, or null if it doesn't exist
     */
    public static Hostclass getById(final Long id) {
        return FINDER.byId(id);
    }

    /**
     * Search for a hostclass by name.
     *
     * @param query the query
     * @param limit the maximum number of results to return
     * @return a list of hostclasses
     */
    public static List<Hostclass> searchByPartialName(final String query, final int limit) {
        return FINDER.query().where().ilike("name", String.format("%%%s%%", query)).setMaxRows(limit).findList();
    }

    /**
     * Gets hostclasses from a list of environments.
     *
     * @param environments the environments
     * @param limit the maximum number of hostclasses to return
     * @return a list of hostclasses
     */
    public static List<Hostclass> getHostclassesForEnvironments(final List<Environment> environments, final int limit) {
        // TODO(vkoskela): Convert to a query based on user to ensure we get limit hostclasses. [MAI-?]
        final Set<Hostclass> result = Sets.newHashSet();
        environments:
        for (final Environment environment : environments) {
            for (final Stage stage : environment.getStages()) {
                for (final Hostclass hostclass : stage.getHostclasses()) {
                    result.add(hostclass);
                    if (result.size() >= limit) {
                        break environments;
                    }
                }
            }
        }
        return Lists.newArrayList(result);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @ManyToOne
    private Hostclass parent;
    @OneToMany(mappedBy = "parent")
    private List<Hostclass> children;
    @OneToMany
    private List<Host> hosts;
    @ManyToMany
    private Set<Stage> stages;

    private static final Finder<Long, Hostclass> FINDER = new Finder<>(Hostclass.class);
}
