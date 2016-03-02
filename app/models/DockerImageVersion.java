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
 * Could also be called 'DockerImage' as an 'Image' refers to a single immutable docker image with a specific digest.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"registry", "repository", "digest"}))
public class DockerImageVersion extends Model implements PackageVersion {
    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Package getPkg() {
        return pkg;
    }

    public void setPkg(Package pkg) {
        this.pkg = pkg;
    }

    @Override
    public String getVersion() {
        return digest;
    }

    @Override
    public String getType() {
        return TYPE_FOR_PACKAGES;
    }

    /**
     * The place that stores many docker images of many projects is called a 'registry' e.g. index.docker.io (Docker Hub). This
     * is more often called a repository for other deployment mechanisms including Roller. A docker 'repository' is
     * the name of the project, e.g. debian, postgresql etc.
     */
    @Override
    public String getRepository() {
        return registry;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    /**
     * Look up by package and version.
     *
     * @param repository the docker repository e.g. 'nginx' or 'training/sinatra'
     * @param digest the sha256 digest, e.g. cbbf2f9a99b47fc460d423812b6a5adff7dfee951d8fa2e4a98caa0382cfbdbf
     * @return a packageversion or null if it doesn't exist
     */
    @Nullable
    public static DockerImageVersion getByPackageAndVersion(final DockerRepository repository, final String digest) {
        return FINDER.where().eq("repository", repository).eq("digest", digest).findUnique();
    }

    /**
     * Look up by repository, package and digest.
     *
     * @param registry the docker registry e.g. index.docker.io
     * @param repository the docker repository e.g. 'nginx' or 'training/sinatra'
     * @param digest the sha256 digest, e.g. cbbf2f9a99b47fc460d423812b6a5adff7dfee951d8fa2e4a98caa0382cfbdbf
     * @return a packageversion or null if it doesn't exist
     */
    @Nullable
    public static DockerImageVersion getByRepositoryPackageAndVersion(final String registry, final DockerRepository repository, final String digest) {
        return FINDER.where().eq("registry", registry).eq("repository", repository).eq("digest", digest).findUnique();
    }

    public static final String TYPE_FOR_PACKAGES = "docker";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String registry;
    private String repository;
    private String digest;
    private Package pkg;
    private String description;

    private static final Find<Long, DockerImageVersion> FINDER = new Find<Long, DockerImageVersion>(){};

}
