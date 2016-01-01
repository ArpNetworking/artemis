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
import client.docker.inspectionbeans.ImageInspection;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This deployment interface intentionally has no method for stopContainer: currently, deployment should always remove
 * containers that are stopped; it is expected (though not necessarily assumed) that no stopped containers exist on the host.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
public interface DockerDeploymentClient {
    /**
     * Parse an exposed port string to extract the port.
     *
     * @param exposedPortString the string to parse
     * @return the exposed port
     */
    static int getPortFromExposedPortString(final String exposedPortString) {
        final Pattern pattern = Pattern.compile("(\\d+)");
        final Matcher matcher = pattern.matcher(exposedPortString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("String containing port does not match expected format.");
        }
        final String portAsString = matcher.group(1);
        return Integer.parseInt(portAsString);
    }

    /**
     * Gets a list of running containers.
     *
     * @return the running containers
     * @throws DockerDeploymentClientException on deployment errors
     */
    List<ContainerDescription> getRunningContainers() throws DockerDeploymentClientException;

    /**
     * Pulls (or downloads) an image onto a host.
     *
     * @param imageReference the image to pull
     * @throws DockerDeploymentClientException on deployment errors

     */
    void pull(String imageReference) throws DockerDeploymentClientException;

    /**
     * Creates a bound run command builder for an image.
     *
     * @param imageReference the image to bind
     * @return a new {@link RunCommandBuilder}
     */
    RunCommandBuilder createRunCommandBuilder(String imageReference);

    /**
     * Stops and removes a container.
     *
     * @param containerReference the container
     * @throws DockerDeploymentClientException on deployment errors
     */
    void stopAndRemoveContainer(String containerReference) throws DockerDeploymentClientException;

    /**
     * Shutdown the client.
     */
    void shutdown();

    /**
     * Inspects a list of images on the connected host.
     *
     * @param imageReference the image references
     * @return a list of {@link ImageInspection} with the image details from the host
     * @throws DockerDeploymentClientException on deployment errors
     */
    List<ImageInspection> inspectImages(List<String> imageReference) throws DockerDeploymentClientException;

    /**
     * An exception for all docker deployment problems.
     */
    class DockerDeploymentClientException extends Exception {
        static final long serialVersionUID = 1L;

        public DockerDeploymentClientException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public DockerDeploymentClientException(final String message) {
            super(message);
        }
    }

    /**
     * Used for errors in parsing json from docker commands.
     */
    class JsonFormatError extends DockerDeploymentClientException {
        static final long serialVersionUID = 1L;

        public JsonFormatError(final String message, final Throwable cause) {
            super(message, cause);
        }

        public JsonFormatError(final String s) {
            super(s);
        }
    }

    /**
     * Used when docker commands return an non-zero exit status.
     */
    class NonZeroExitStatusException extends DockerDeploymentClientException  {
        static final long serialVersionUID = 1L;

        public NonZeroExitStatusException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public NonZeroExitStatusException(final String message) {
            super(message);
        }
    }

    /**
     * Describes a container.
     */
    class ContainerDescription {
        public ContainerDescription(final String id, final String name, final String imageLabel, final String createdLabel) {
            _id = id;
            _name = name;
            _imageId = imageLabel;
            _createdAt = createdLabel;
        }

        private final String _name;
        private final String _id;
        private final String _imageId;
        private final String _createdAt;

        /**
         *
         * @return The name of the Container (randomly generated on container creation by default) - not the the repo
         * or tag or name of the image
         */
        public String getName() {
            return _name;
        }

        public String getId() {
            return _id;
        }

        public String getImageId() {
            return _imageId;
        }

        /**
         * Gets the createdAt time as a string.
         *
         * @return ISO8601 String
         */
        public String getCreatedAsString() {
            return _createdAt;
        }

    }

    //TODO(barp): break out a run command parameters object and move the "doRun" method to the DockerDeploymentClient [Artemis-?]
    /**
     * Executes a run command.
     */
    interface RunCommandRunner {
        void run(DefaultDockerRunCommandBuilder imageReference) throws DockerDeploymentClientException;
    }

    /**
     * Builder for a run command.
     */
    interface RunCommandBuilder {
        /**
         * Add a port mapping to the run command.
         *
         * @param portMapping a port mapping
         */
        void addPortMapping(PortMapping portMapping);

        /**
         * Execute the run command.
         *
         * @throws DockerDeploymentClientException
         */
        void doRun() throws DockerDeploymentClientException;

        /**
         * Sets the name of the container.
         *
         * @param containerName the container name
         */
        void setContainerName(String containerName);
    }

}
