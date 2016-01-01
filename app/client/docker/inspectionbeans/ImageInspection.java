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

/**
 * Represents the docker output for an inspect command.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInspection {
    public String getId() {
        return _id;
    }

    public String getParent() {
        return _parent;
    }

    public String getComment() {
        return _comment;
    }

    public String getCreated() {
        return _created;
    }

    public String getContainer() {
        return _container;
    }

    public ImageConfig getConfig() {
        return _config;
    }

    @JsonProperty("container")
    private String _container;
    @JsonProperty("config")
    private ImageConfig _config;
    @JsonProperty("id")
    private String _id;
    @JsonProperty("parent")
    private String _parent;
    @JsonProperty("comment")
    private String _comment;
    @JsonProperty("created")
    private String _created;
}
