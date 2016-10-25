package utils;

import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigRenderOptions;
import play.Configuration;

import java.io.IOException;

/**
 * Emits JSON from a HOCON config object.
 *
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
public final class JsonConfigBridge {

    private JsonConfigBridge() {}

    /**
     * Creates an object via JSON deserialization of the provided HOCON configuration.
     *
     * @param configuration the config
     * @param clazz class
     * @param <T> the class to deserialize into
     * @return deserialized object from config
     * @throws IOException if serialization fails
     */
    public static <T> T load(final Configuration configuration, final Class<T> clazz) throws IOException {
        final String json = configuration.underlying().root().render(ConfigRenderOptions.concise());
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
}
