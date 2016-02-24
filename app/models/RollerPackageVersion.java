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
public class RollerPackageVersion extends Model implements PackageVersion {
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
        final RollerPackageVersion other = (RollerPackageVersion) obj;
        return Objects.equal(pkg, other.pkg) && Objects.equal(this.version, other.version);
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long value) {
        id = value;
    }

    @Override
    public Package getPkg() {
        return pkg;
    }

    public void setPkg(final Package value) {
        pkg = value;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String value) {
        version = value;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(final String value) {
        type = value;
    }

    @Override
    public String getRepository() {
        return repository;
    }

    public void setRepository(final String value) {
        repository = value;
    }

    @Override
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
    public static RollerPackageVersion getByPackageAndVersion(final Package pkg, final String version) {
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
    public static RollerPackageVersion getByRepositoryPackageAndVersion(final String repository, final Package pkg, final String version) {
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

    private static final Find<Long, RollerPackageVersion> FINDER = new Find<Long, RollerPackageVersion>(){};
}
