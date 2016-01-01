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

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Model that represents a package.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Package extends Model {
    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        this.name = value;
    }

    /**
     * Look up package by name.
     *
     * @param name the name
     * @return a package, or null if it does not exist
     */
    @Nullable
    public static Package getByName(final String name) {
        return FINDER.where().eq("name", name).findUnique();
    }

    /**
     * Search for package by name.
     *
     * @param query the query
     * @param limit maximum number of results
     * @return a list of packages
     */
    public static List<Package> searchByPartialName(final String query, final int limit) {
        return FINDER.where().ilike("name", String.format("%%%s%%", query)).setMaxRows(limit).findList();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;

    private static final Find<Long, Package> FINDER = new Find<Long, Package>(){};
}
