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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Represents the state of a host in a deployment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
public class HostDeployment extends Model {
    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(final Deployment value) {
        deployment = value;
    }

    public DateTime getFinished() {
        return finished;
    }

    public void setFinished(final DateTime value) {
        finished = value;
    }

    public DateTime getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(final DateTime value) {
        heartbeat = value;
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

    public DateTime getStarted() {
        return started;
    }

    public void setStarted(final DateTime value) {
        started = value;
    }

    public DeploymentState getState() {
        return state;
    }

    public void setState(final DeploymentState value) {
        state = value;
    }

    public String getStateDetail() {
        return stateDetail;
    }

    public void setStateDetail(final String value) {
        stateDetail = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    private Host host;
    @ManyToOne
    private Deployment deployment;
    private DeploymentState state;
    private String stateDetail;
    private DateTime heartbeat;
    private DateTime started;
    private DateTime finished;
}
