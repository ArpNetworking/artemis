package models;

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
public class DockerImageVersion implements PackageVersion {
    @Override
    public long getId() {
        return id;
    }

    @Override
    public Package getPkg() {
        return pkg;
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

    @Override
    public String getDescription() {
        return description;
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
}
