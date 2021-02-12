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

import io.ebean.Ebean;
import io.ebean.Expr;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * Represents a deployment of a stage.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
public class Deployment extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @OneToMany(mappedBy = "deployment")
    private List<HostDeployment> hostStates;
    private DateTime heartbeat;
    private String deploymentOwner;
    private String initiator;
    @Column(nullable = false)
    @ManyToOne
    private ManifestHistory manifestHistory;
    private DeploymentState state;
    private DateTime start;
    private DateTime finished;

    private static final Finder<Long, Deployment> FINDER = new Finder<>(Deployment.class);

    public String getDeploymentOwner() {
        return deploymentOwner;
    }

    public void setDeploymentOwner(final String value) {
        deploymentOwner = value;
    }

    public DateTime getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(final DateTime value) {
        heartbeat = value;
    }

    public List<HostDeployment> getHostStates() {
        return hostStates;
    }

    public void setHostStates(final List<HostDeployment> value) {
        hostStates = value;
    }

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public ManifestHistory getManifestHistory() {
        return manifestHistory;
    }

    public void setManifestHistory(final ManifestHistory value) {
        manifestHistory = value;
    }

    public DateTime getFinished() {
        return finished;
    }

    public void setFinished(final DateTime value) {
        finished = value;
    }

    public DateTime getStart() {
        return start;
    }

    public void setStart(final DateTime value) {
        start = value;
    }

    public DeploymentState getState() {
        return state;
    }

    public void setState(final DeploymentState value) {
        state = value;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(final String value) {
        initiator = value;
    }

    /**
     * Looks up a deployment by it's primary key id.
     *
     * @param value the id of the deployment
     * @return a deployment, or null if none is found
     */
    @Nullable
    public static Deployment getById(final long value) {
        return FINDER.byId(value);
    }

    /**
     * Get all deployments to a stage, paginated.
     *
     * @param stage the stage
     * @param limit maximum number of deployments to return
     * @param offset offset in the database table
     * @return a list of deployments
     */
    public static List<Deployment> getByStage(final Stage stage, final int limit, final int offset) {
        return FINDER.query()
                .where()
                .eq("manifestHistory.stage", stage)
                .orderBy()
                .desc("start")
                .setMaxRows(limit)
                .setFirstRow(offset)
                .findList();
    }

    /**
     * Gets and locks a deployment that is considered stuck.
     *
     * @return a stuck deployment, or null if there are none
     */
    @Nullable
    public static Deployment getAndLockStuckDeployment() {
        if (Ebean.currentTransaction() != null) {
            throw new IllegalStateException("Must not be in a transaction.  getAndLockStuckDeployment requires creating a new transaction");
        }
        try (Transaction transaction = Ebean.beginTransaction(TxIsolation.SERIALIZABLE)) {
            final Deployment deployment = Ebean.createQuery(Deployment.class)
                    .forUpdate()
                    .where()
                    .isNull("finished")
                    .ne("state", DeploymentState.SUCCEEDED)
                    .ne("state", DeploymentState.FAILED)
                            // if the heartbeat is older than 10 minutes
                            // OR if the owner is null (it's handing off)
                    .or(
                            Expr.lt("heartbeat", DateTime.now().minusMinutes(10)),
                            Expr.isNull("deploymentOwner")
                    )
                    .setMaxRows(1)
                    .findOne();
            if (deployment == null) {
                return null;
            }

            deployment.setDeploymentOwner(InetAddress.getLocalHost().getCanonicalHostName());
            deployment.setHeartbeat(DateTime.now());
            deployment.setState(DeploymentState.RUNNING);
            deployment.save();
            transaction.commit();
            return deployment;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Heartbeats a deployment so it will not be seen as stuck by other hosts.
     */
    public void heartbeat() {
        if (Ebean.currentTransaction() != null) {
            throw new IllegalStateException("Must not be in a transaction.  getAndLockStuckDeployment requires creating a new transaction");
        }
        try (Transaction transaction = Ebean.beginTransaction(TxIsolation.SERIALIZABLE)) {
            final String myName = InetAddress.getLocalHost().getCanonicalHostName();
            final Deployment toHeartbeat = Ebean.createQuery(Deployment.class)
                    .forUpdate()
                    .where()
                    .eq("id", getId())
                    .findOne();
            // Verify that we still own the deployment
            if (!myName.equals(toHeartbeat.getDeploymentOwner())) {
                throw new IllegalStateException("cannot heartbeat, we no longer own the deployment");
            }
            toHeartbeat.setHeartbeat(DateTime.now());
            toHeartbeat.save();
            transaction.commit();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
