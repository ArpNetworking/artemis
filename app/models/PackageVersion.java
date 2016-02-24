package models;

/**
 * @author Matthew Hayter (mhayter at groupon dot com)
 * @since 1.0.0
 */
public interface PackageVersion {
    @Override
    boolean equals(Object obj);

    long getId();

    Package getPkg();

    String getVersion();

    String getType();

    String getRepository();

    String getDescription();
}
