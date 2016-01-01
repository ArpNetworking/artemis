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
package api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents the output needed for roller to run a host file.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HostOutput extends YamlUnknownBase {
    public String getColo() {
        return _colo;
    }

    public String getHostclass() {
        return _hostclass;
    }

    public String getHostname() {
        return _hostname;
    }

    public void setColo(final String value) {
        _colo = value;
    }

    public void setHostclass(final String value) {
        _hostclass = value;
    }

    public void setHostname(final String value) {
        _hostname = value;
    }

    public Map<String, Object> getParams() {
        return _params;
    }

    public void setParams(final Map<String, Object> value) {
        _params = value;
    }

    @JsonProperty("colo")
    private String _colo;
    @JsonProperty("hostname")
    private String _hostname;
    @JsonProperty("hostclass")
    private String _hostclass;
    @JsonProperty("params")
    private Map<String, Object> _params;
}
