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
package client.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A mapping from a port internal to a docker container to a host port (external).
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 * @since 1.0.0
 */
public class PortMapping {
    /**
     * Public constructor.
     *
     * @param externalPort the external port
     * @param internalPort the internal port
     */
    public PortMapping(final int externalPort, final int internalPort) {
        _externalPort = externalPort;
        _internalPort = internalPort;
    }

    public int getExternalPort() {
        return _externalPort;
    }

    public int getInternalPort() {
        return _internalPort;
    }

    @JsonProperty("externalPort")
    private final int _externalPort;
    @JsonProperty("internalPort")
    private final int _internalPort;

}
