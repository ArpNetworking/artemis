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

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Represents an Oath authorization.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_name"}))
public class Authentication extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String userName;
    private String token;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String value) {
        token = value;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String value) {
        userName = value;
    }

    /**
     * Looks up an Authentication token by user name.
     *
     * @param userName the user name
     * @return an {@link Authentication} if one exists, otherwise null
     */
    @Nullable
    public static Authentication findByUserName(final String userName) {
        return FINDER.where().eq("user_name", userName).findUnique();
    }

    private static final Find<Long, Authentication> FINDER = new Find<Long, Authentication>(){};
}
