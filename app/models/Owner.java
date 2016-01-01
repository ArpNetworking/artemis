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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Model that represents an owner organization.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "org_name"))
public class Owner extends Model {
    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(final String value) {
        orgName = value;
    }

    /**
     * Look up by name.
     *
     * @param name the name
     * @return an Owner or null if it doesn't exist
     */
    public static Owner getByName(final String name) {
        return FINDER.where().eq("orgName", name).findUnique();
    }

    /**
     * Look up by id.
     *
     * @param id the id
     * @return an Owner or null if it doesn't exist
     */
    public static Owner getById(final long id) {
        return FINDER.byId(id);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String orgName;

    private static final Find<Long, Owner> FINDER = new Find<Long, Owner>(){};
}
