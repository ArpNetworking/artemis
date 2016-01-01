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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.inject.Named;

/**
 * Ssh client for deploying docker images to a host.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
public class DockerSshClient implements DockerDeploymentClient, DockerDeploymentClient.RunCommandRunner {

    /**
     * Public constructor.
     *
     * @param client an ssh client
     * @param dockerCmd the command for "docker"
     */
    @AssistedInject
    public DockerSshClient(@Assisted final SSHClient client, @Named("DockerCmd") final String dockerCmd) {
        _sshClient = client;
        _dockerCmd = dockerCmd;
    }

    @Override
    public List<DockerDeploymentClient.ContainerDescription> getRunningContainers() throws DockerDeploymentClientException {
        final String inspectionJson;
        /*
        'docker ps -q | docker inspect' : -q tells docker to only show container ids (quiet mode), and then
        'docker inspect' takes a list of ids and produces a json array.
        */
        inspectionJson = sshExecAndGetOutput(_dockerCmd + " ps -q | xargs " + _dockerCmd + " inspect");

        final List<ContainerDescription> containers = new LinkedList<>();
        if (inspectionJson.isEmpty()) {
            return containers;
        }

        final ObjectMapper mapper = new ObjectMapper();
        try {
            final ArrayNode rootArray = mapper.readValue(inspectionJson, ArrayNode.class);

            for (final JsonNode containerJson : rootArray) {
                final ContainerDescription c = new ContainerDescription(
                        containerJson.get("Id").asText(),
                        containerJson.get("Name").asText(),
                        containerJson.get("Image").asText(),
                        containerJson.get("Created").asText()
                );
                containers.add(c);
            }
        } catch (final IOException e) {
            throw new JsonFormatError("Error parsing JSON from docker command", e);
        }

        return containers;
    }

    @Override
    public void shutdown() {
        try {
            _sshClient.disconnect();
            _sshClient.close();
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void pull(final String imageReference) throws DockerDeploymentClientException {
        sshExecExpectSuccess(_dockerCmd + " pull " + imageReference);
        // All is well!
    }

    /**
     * Runs the image using -d for detached-mode. This allows the run command to finish while the docker container
     * continues to run.
     */
    @Override
    public void run(final DefaultDockerRunCommandBuilder runParameters) throws DockerDeploymentClientException {
        final StringBuilder sb = new StringBuilder();
        sb
                .append(_dockerCmd)
                .append(" run -d ")
                .append(String.format("--name=%s ", runParameters.getContainerName()));
        for (final PortMapping mapping : runParameters.getPortMappings()) {
            sb.append(String.format("-p %d:%d ", mapping.getExternalPort(), mapping.getInternalPort()));
        }
        sb.append(runParameters.getImageReference());

        sshExecExpectSuccess(sb.toString());
    }

    @Override
    public DockerDeploymentClient.RunCommandBuilder createRunCommandBuilder(final String imageReference) {
        return new DefaultDockerRunCommandBuilder(imageReference, this);
    }

    @Override
    public void stopAndRemoveContainer(final String containerReference) throws DockerDeploymentClientException {
        this.stop(containerReference);
        this.rm(containerReference);
    }

    /**
     * If the image reference does not exist, this function will throw a JsonFormatError as the
     * output will not contain the expected objects.
     *
     * @param imageReferences the image references
     */
    @Override
    public List<ImageInspection> inspectImages(final List<String> imageReferences) throws DockerDeploymentClientException {
        if (imageReferences.size() == 0) {
            return Collections.emptyList();
        }

        final ObjectMapper inspectionMapper = new ObjectMapper();

        inspectionMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.PropertyNamingStrategyBase() {
            static final long serialVersionUID = 1L;

            @Override
            public String translate(final String s) {
                // Json produced by docker uses capitalized words for property names.
                return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);
            }
        });

        final StringBuilder sb = new StringBuilder();
        sb.append(_dockerCmd).append(" inspect ");
        for (final String imageReference : imageReferences) {
            sb.append(imageReference).append(" ");
        }

        final String inspectionJson = sshExecAndGetOutput(sb.toString());

        final List<ImageInspection> inspections;
        try {
            inspections = inspectionMapper.readValue(
                    inspectionJson,
                    inspectionMapper.getTypeFactory().constructCollectionType(List.class, ImageInspection.class));
        } catch (final IOException e) {
            throw new DockerDeploymentClient.JsonFormatError("docker inspect output in unexpected format", e);
        }

        return inspections;
    }

    /**
     * Stops a container.
     *
     * @param containerReference Either a container ID or a container name.
     * @throws DockerDeploymentClientException on deployment error
     */
    public void stop(final String containerReference) throws DockerDeploymentClientException {
        sshExecExpectSuccess(_dockerCmd + " stop " + containerReference);
    }

    /**
     * Removes a container.
     *
     * @param containerReference Either a container ID or a container name.
     * @throws DockerDeploymentClientException on deployment error
     */
    public void rm(final String containerReference) throws DockerDeploymentClientException {
        sshExecExpectSuccess(_dockerCmd + " rm " + containerReference);
    }

    /**
     * Handy for commands that should never fail, and you just want the output.
     */
    private String sshExecAndGetOutput(final String cmd) throws DockerDeploymentClientException {

        return withSession(session -> {
            final Session.Command commandResult;
            commandResult = session.exec(cmd);
            //commandResult.join();
            return IOUtils.readFully(commandResult.getInputStream()).toString(Charsets.UTF_8.name());
        });

    }

    private void sshExecExpectSuccess(final String cmd) throws DockerDeploymentClientException {
        final int exitStatus;
        final Session.Command cmdResult;
        cmdResult = withSession(session -> {
            final Session.Command exec = session.exec(cmd);
            exec.join();
            return exec;
        });
        final Integer boxedExitStatus = cmdResult.getExitStatus();
        if (boxedExitStatus == null) {
            throw new DockerDeploymentClientException("Received null but expected exit status of SSH command");
        }
        exitStatus = boxedExitStatus;

        if (exitStatus != 0) {
            final String stdErr;
            try {
                stdErr = IOUtils.readFully(cmdResult.getErrorStream()).toString(Charsets.UTF_8.name());
            } catch (final IOException e) {
                throw new NonZeroExitStatusException(
                        String.format("Docker command exited with status %d but was unable to get stderr", exitStatus)
                );
            }
            throw new NonZeroExitStatusException(
                    String.format("Docker command exited with status %d and stderr: %s", exitStatus, stdErr)
            );
        }
    }

    private <R> R withSession(final FunctionWithException<Session, R> f) throws DockerDeploymentClientException {
        Session session = null;
        try {
            session = _sshClient.startSession();
            // CHECKSTYLE.OFF: IllegalCatch - we need to catch everything, we'll rethrow it later
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new DockerDeploymentClientException("SSH Error", e);
        }

        final R ret;
        try {
            ret = f.accept(session);
            // CHECKSTYLE.OFF: IllegalCatch - we need to catch everything, we'll rethrow it later
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            try {
                session.close();
            } catch (final TransportException | ConnectionException ignored) {
                // Don't care
                e.addSuppressed(ignored);
            }
            throw new DockerDeploymentClientException("SSH Error", e);
        }
        return ret;
    }

    private final SSHClient _sshClient;
    private final String _dockerCmd;

    @FunctionalInterface
    private interface FunctionWithException<T, R> {
        R accept(T param) throws Exception;
    }
}
