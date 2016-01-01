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
import com.google.common.base.Objects;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Represents a version of a package in the database.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"repository", "pkg_id", "version"}))
public class PackageVersion extends Model {
    @Override
    public int hashCode() {
        return Objects.hashCode(pkg, version);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PackageVersion other = (PackageVersion) obj;
        return Objects.equal(pkg, other.pkg) && Objects.equal(this.version, other.version);
    }

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    public Package getPkg() {
        return pkg;
    }

    public void setPkg(final Package value) {
        pkg = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String value) {
        version = value;
    }

    public String getType() {
        return type;
    }

    public void setType(final String value) {
        type = value;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(final String value) {
        repository = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String value) {
        description = value;
    }

    /**
     * Look up by package and version.
     *
     * @param pkg the package
     * @param version the version
     * @return a packageversion or null if it doesn't exist
     */
    @Nullable
    public static PackageVersion getByPackageAndVersion(final Package pkg, final String version) {
        return FINDER.where().eq("pkg", pkg).eq("version", version).findUnique();
    }

    /**
     * Look up by repository, package and version.
     *
     * @param repository the repository
     * @param pkg the package
     * @param version the version
     * @return a packageversion or null if it doesn't exist
     */
    @Nullable
    public static PackageVersion getByRepositoryPackageAndVersion(final String repository, final Package pkg, final String version) {
        return FINDER.where().eq("repository", repository).eq("pkg", pkg).eq("version", version).findUnique();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    private Package pkg;
    private String type;
    private String repository;
    private String version;
    private String description;

    private static final Find<Long, PackageVersion> FINDER = new Find<Long, PackageVersion>(){};
}
