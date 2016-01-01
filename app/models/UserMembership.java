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
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Holds the organizations for a user.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_name", "org_id"}))
public class UserMembership extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String userName;
    @ManyToOne
    private Owner org;

    private static final Find<Long, UserMembership> FINDER = new Find<Long, UserMembership>(){};

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public Owner getOrg() {
        return org;
    }

    public void setOrg(final Owner value) {
        org = value;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String value) {
        userName = value;
    }

    /**
     * Looks up the organizations the user is a part of.
     *
     * @param userName the user name
     * @return a list of organizations
     */
    public static List<Owner> getOrgsForUser(final String userName) {
        final List<UserMembership> memberships = FINDER.fetch("org").where().eq("user_name", userName).findList();
        return FluentIterable.from(memberships)
                .transform(
                        new Function<UserMembership, Owner>() {
                            @Nullable
                            @Override
                            public Owner apply(final UserMembership userMembership) {
                                return userMembership.getOrg();
                            }
                        })
                .toList();
    }

    /**
     * Looks up a user by user name and organization.
     *
     * @param userName the user name
     * @param org the organization name
     * @return a {@link UserMembership} record, or null if none found
     */
    @Nullable
    public static UserMembership getByUserAndOrg(final String userName, final String org) {
        return FINDER.fetch("org").where().eq("userName", userName).eq("org.orgName", org).findUnique();
    }
}
