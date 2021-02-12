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
import io.ebean.Finder;
import io.ebean.Model;
import org.joda.time.DateTime;

import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Represents a deployment of a manifest to a stage.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
public class ManifestHistory extends Model {
    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public DateTime getFinish() {
        return finish;
    }

    public void setFinish(final DateTime value) {
        finish = value;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(final Manifest value) {
        manifest = value;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(final Stage value) {
        stage = value;
    }

    public DateTime getStart() {
        return start;
    }

    public void setStart(final DateTime value) {
        start = value;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String value) {
        config = value;
    }

    /**
     * Get the previous {@link ManifestHistory}.
     *
     * @return the previous {@link ManifestHistory} or null if it doesn't exist
     */
    @Nullable
    public ManifestHistory getPrevious() {
        return FINDER.query().where().eq("stage", stage).lt("start", start).order().desc("start").setMaxRows(1).findOne();
    }

    /**
     * Get the current {@link ManifestHistory} for a stage.
     *
     * @param stage the stage
     * @return the current {@link ManifestHistory} or null if it doesn't exist
     */
    @Nullable
    public static ManifestHistory getCurrentForStage(final Stage stage) {
        //TODO(barp): why is this locking? [Artemis-?]
        return Ebean.createQuery(ManifestHistory.class).forUpdate().where().eq("stage", stage).isNull("finish").findOne();
    }

    /**
     * Get a list of {@link ManifestHistory} for a stage.
     *
     * @param stage the stage
     * @param limit maximum entries to return
     * @param offset offset of the first entry to return
     * @return a list of {@link ManifestHistory}
     */
    public static List<ManifestHistory> getByStage(final Stage stage, final int limit, final int offset) {
        return FINDER.query().where().eq("stage", stage).orderBy().desc("start").setMaxRows(limit).setFirstRow(offset).findList();
    }

    /**
     * Look up by id.
     *
     * @param id the id
     * @return the {@link ManifestHistory} or null if it doesn't exist
     */
    public static ManifestHistory getById(final long id) {
        return FINDER.byId(id);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    private Manifest manifest;
    @ManyToOne
    private Stage stage;
    // TODO(barp): set the default in the database [Artemis-?]
    // TODO(barp): index this [Artemis-?]
    private DateTime start;
    private DateTime finish;
    private String config;

    private static final Finder<Long, ManifestHistory> FINDER = new Finder<>(ManifestHistory.class);
}
