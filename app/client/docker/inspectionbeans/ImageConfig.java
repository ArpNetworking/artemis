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
package client.docker.inspectionbeans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Representation of an image configuration from the docker inspect command.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageConfig {
    /**
     * Public constructor.
     */
    public ImageConfig() {
    }

    /**
     * Public constructor.
     *
     * @param exposedPorts the exposed ports
     */
    public ImageConfig(final Map<String, JsonNode> exposedPorts) {
        _exposedPorts = exposedPorts;
    }

    public Map<String, JsonNode> getExposedPorts() {
        return _exposedPorts;
    }

    @JsonProperty("exposedPorts")
    private Map<String, JsonNode> _exposedPorts;
}
