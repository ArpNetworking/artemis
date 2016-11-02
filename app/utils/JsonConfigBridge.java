/**
 * Copyright 2016 Brandon Arp
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
package utils;

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
     * @param mapper ObjectMapper to use for deserialization
     * @param <T> the class to deserialize into
     * @return deserialized object from config
     * @throws IOException if serialization fails
     */
    public static <T> T load(final Configuration configuration, final Class<T> clazz, final ObjectMapper mapper) throws IOException {
        final String json = configuration.underlying().root().render(ConfigRenderOptions.concise());
        return mapper.readValue(json, clazz);
    }
}
