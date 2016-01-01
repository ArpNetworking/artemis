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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import client.docker.inspectionbeans.ImageInspection;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Matthew Hayter (mhayter at groupon dot com)
 * @since 1.0.0
 */
public class DockerSshClientComponentTest {
    /*
    This 'live' test connects via ssh to the boot2docker VM running on localhost and runs docker commands, including
    starting and stopping of containers.
    It requires a default installation of boot2docker, with the usual location of the ssh keys and the default
    boot2docker ssh port.
     */
    @Test
    @Ignore
    public void testListStartStop() throws Exception {
        final DockerSshClient dockerSshClient = getConnectionToBoot2Docker();

        // List containers
        List<DockerDeploymentClient.ContainerDescription> containers = dockerSshClient.getRunningContainers();
        // Verify zero running
        assertTrue(containers.isEmpty());
        // Start container
        dockerSshClient.createRunCommandBuilder("dgageot/helloworld").doRun();

        // If you pause the test here, you should now be able to run
        // curl `boot2docker ip`:8080 to get the hello world page response.

        // List containers
        List<DockerDeploymentClient.ContainerDescription> containers2 = dockerSshClient.getRunningContainers();
        // Verify one running container
        assertEquals(1, containers2.size());
        // Stop container
        dockerSshClient.stopAndRemoveContainer(containers2.get(0).getId());
        // List
        List<DockerDeploymentClient.ContainerDescription> containers3 = dockerSshClient.getRunningContainers();
        // Verify zero running
        assertTrue(containers3.isEmpty());

    }

    @Test
    @Ignore
    public void testInspect() throws Exception {
        final DockerSshClient client = getConnectionToBoot2Docker();

        final List<ImageInspection> imageInspections = client.inspectImages(Collections.singletonList("dgageot/helloworld"));

        assertEquals(1, imageInspections.size());

        final List<String> portStrings = new ArrayList<>(imageInspections.get(0).getConfig().getExposedPorts().keySet());
        assertEquals(1, portStrings.size());

        assertEquals(8080, DockerDeploymentClient.getPortFromExposedPortString(portStrings.get(0)));
    }

    private DockerSshClient getConnectionToBoot2Docker() throws Exception {
        SSHClient sshClient = new SSHClient();
        // The PromiscuousVerifier apparently happily accepts any new hosts, so you don't need e.g. known_hosts file.
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        final KeyProvider keyProvider = sshClient.loadKeys(System.getProperty("user.home") + "/.ssh/id_boot2docker");

        sshClient.connect("localhost", 2022);
        sshClient.authPublickey("docker", keyProvider);

        return new DockerSshClient(sshClient, "/usr/local/bin/docker");
    }
}
