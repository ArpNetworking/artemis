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
package client;

import client.docker.PortMapping;

import java.util.LinkedList;
import java.util.List;

/**
 * Default implementation of a RunCommandBuilder.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
*/
public class DefaultDockerRunCommandBuilder implements DockerDeploymentClient.RunCommandBuilder {

    /**
     * Public constructor.
     *
     * @param imageReference the image to run
     * @param runner the runner to execute on
     */
    public DefaultDockerRunCommandBuilder(final String imageReference, final DockerDeploymentClient.RunCommandRunner runner) {
        _imageReference = imageReference;
        _runner = runner;
    }

    public String getImageReference() {
        return _imageReference;
    }

    public List<PortMapping> getPortMappings() {
        return _portMappings;
    }

    public String getContainerName() {
        return _containerName;
    }

    @Override
    public void addPortMapping(final PortMapping portMapping) {
        _portMappings.add(portMapping);
    }

    @Override
    public void doRun() throws DockerDeploymentClient.DockerDeploymentClientException {
        _runner.run(this);
    }

    @Override
    public void setContainerName(final String containerName) {
        _containerName = containerName;
    }

    private final String _imageReference;
    private String _containerName;
    private final List<PortMapping> _portMappings = new LinkedList<>();
    private final DockerDeploymentClient.RunCommandRunner _runner;

}
