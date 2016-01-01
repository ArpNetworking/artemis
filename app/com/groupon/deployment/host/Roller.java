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
package com.groupon.deployment.host;

import akka.actor.UntypedActor;
import com.groupon.deployment.HostDeploymentNotifications;
import com.groupon.deployment.SshSessionFactory;
import com.google.common.base.Charsets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import models.Host;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import play.Configuration;
import play.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Roller deployment actor.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Roller extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param host host to deploy to
     * @param sshFactory factory for ssh connections
     * @param config artemis configuration
     */
    @AssistedInject
    public Roller(
            @Assisted final Host host,
            final SshSessionFactory sshFactory,
            final Configuration config) {
        _host = host;
        _sshFactory = sshFactory;
        _config = config;
        Logger.info("Started roller deployment actor for host " + host.getName());
        context().parent().tell(new HostDeploymentNotifications.DeploymentStarted(host), self());
        self().tell("start", self());
    }

    @Override
    public void onReceive(final Object message) {
        if ("start".equals(message)) {
            final String dc = _host.getName().substring(_host.getName().lastIndexOf('.') + 1);
            String baseUrl = _config.getString("roller.artemisBaseUrl." + dc);
            if (baseUrl == null) {
                baseUrl = _config.getString("roller.artemisBaseUrl.default");
            }

            try (final SSHClient sshClient = _sshFactory.create(_host.getName())) {

                // Pre-roll scripts
                executeCommand(sshClient, "sudo /usr/local/bin/beforeRoll 2>&1");

                // Roll
                executeRequired(sshClient, "sudo /var/tmp/roll --baseurl " + baseUrl + " 2>&1", "roller");

                // Verify
                executeRequired(sshClient, "sudo /usr/local/bin/verifyRoll 2>&1", "verify roll script");

                // Post-roll scripts
                executeRequired(sshClient, "sudo /usr/local/bin/afterRoll 2>&1", "post-roll script");

                context().parent().tell(new HostDeploymentNotifications.DeploymentSucceeded(_host), self());
                // CHECKSTYLE.OFF: IllegalCatch - we need to catch everything, we'll record it and die
            } catch (final IOException | RuntimeException e) {
                // CHECKSTYLE.ON: IllegalCatch
                context().parent().tell(new HostDeploymentNotifications.DeploymentFailed(_host, e), self());
            }
        } else {
            unhandled(message);
        }
    }

    private void executeRequired(final SSHClient sshClient, final String commandString, final String description) throws IOException {
        final Integer exitStatus;
        exitStatus = executeCommand(sshClient, commandString);
        if (exitStatus == null || exitStatus != 0) {
            throw new IllegalStateException(description + " exit code was " + exitStatus);
        }
    }

    private Integer executeCommand(final SSHClient sshClient, final String commandString) throws IOException {
        context().parent().tell(new HostDeploymentNotifications.DeploymentLog(_host, "Executing '" + commandString + "'"), self());
        final Integer exitStatus;
        try (final Session session = sshClient.startSession();
                final Session.Command command = session.exec(commandString);
                final BufferedReader error = new BufferedReader(new InputStreamReader(command.getErrorStream(), Charsets.UTF_8));
                final BufferedReader reader = new BufferedReader(new InputStreamReader(command.getInputStream(), Charsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                Logger.info("***" + line);
                context().parent().tell(new HostDeploymentNotifications.DeploymentLog(_host, line), self());
                line = reader.readLine();
            }
            command.join(30, TimeUnit.SECONDS);
            session.join(30, TimeUnit.SECONDS);
            exitStatus = command.getExitStatus();
        }
        return exitStatus;
    }

    private final Host _host;
    private final SshSessionFactory _sshFactory;
    private final Configuration _config;
}
