package utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface HostClassifier {
    /**
     * Maps a host name to a hostclass name.
     *
     * @param hostname the host name
     * @return the hostclass for this host
     */
    String hostclassFor(String hostname);
}
