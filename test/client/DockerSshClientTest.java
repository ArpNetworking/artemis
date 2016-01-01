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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.util.List;

public class DockerSshClientTest {

    @Test
    public void testGetRunningContainers() throws Exception {

        SSHClient clientMock = Mockito.mock(SSHClient.class);
        Session sessionMock = Mockito.mock(Session.class);
        Session.Command cmdMock = Mockito.mock(Session.Command.class);
        String dockerInspectJson = "[{\n" +
                "    \"Created\": \"2015-01-22T00:54:43.498562145Z\",\n" +
                "    \"Id\": \"82daa08ecbc52aa0ffee413382f22ba9b90271a0e9cf0e38349825696ec6834f\",\n" +
                "    \"Image\": \"258105bea10d7cebbfbd877543f0e19f2e8b4428100f9f6e8be04c7dbe3f6cf7\",\n" +
                "    \"Name\": \"/focused_archimedes\"\n" +
                "}\n" +
                "]\n";

        Mockito.when(clientMock.startSession()).thenReturn(sessionMock);
        Mockito.when(sessionMock.exec(Mockito.anyString())).thenReturn(cmdMock);
        Mockito.when(cmdMock.getInputStream()).thenReturn(new ByteArrayInputStream(dockerInspectJson.getBytes()));

        DockerSshClient client = new DockerSshClient(clientMock, "some_docker_command");

        final List<DockerDeploymentClient.ContainerDescription> runningContainers = client.getRunningContainers();

        final DockerDeploymentClient.ContainerDescription containerDescription = runningContainers.get(0);
        Assert.assertEquals("82daa08ecbc52aa0ffee413382f22ba9b90271a0e9cf0e38349825696ec6834f", containerDescription.getId());
        Assert.assertEquals("258105bea10d7cebbfbd877543f0e19f2e8b4428100f9f6e8be04c7dbe3f6cf7", containerDescription.getImageId());
        Assert.assertEquals("/focused_archimedes", containerDescription.getName());
        Assert.assertEquals("2015-01-22T00:54:43.498562145Z", containerDescription.getCreatedAsString());
    }
}
