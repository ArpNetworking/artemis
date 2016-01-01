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
import org.joda.time.DateTime;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Log line for a deployment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
public class DeploymentLog extends Model {
    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(final Deployment value) {
        deployment = value;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(final Host value) {
        host = value;
    }

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public DateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(final DateTime value) {
        logTime = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String value) {
        message = value;
    }

    /**
     * Get the logs for a deployment since a certain time.
     *
     * @param deployment the deployment
     * @param lastSeen the exclusive start
     * @return a list of log messages
     */
    public static List<DeploymentLog> getLogsSince(final Deployment deployment, final DeploymentLog lastSeen) {
        return FINDER.where().eq("deployment", deployment).gt("id", lastSeen.getId()).order().asc("id").findList();
    }

    /**
     * Get a deploymet log reference (deferred lookup) by id.
     * @param id the id
     * @return a DeploymentLog
     */
    public static DeploymentLog ref(final long id) {
        return FINDER.ref(id);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    private Host host;
    private String message;
    @ManyToOne
    private Deployment deployment;
    private DateTime logTime;
    private static final Find<Long, DeploymentLog> FINDER = new Find<Long, DeploymentLog>(){};
}
