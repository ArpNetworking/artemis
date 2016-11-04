/**
 * Copyright 2016 Brandon Arp
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
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.groupon.deployment.HostDeploymentNotifications;
import com.groupon.deployment.SshSessionFactory;
import models.Deployment;
import models.Host;
import models.PackageVersion;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import play.Configuration;
import play.Logger;
import utils.RpmVersionComparator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RPM deployment actor.
 *
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
public class Rpm extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param host host to deploy to
     * @param deployment the deployment spec
     * @param sshFactory factory for ssh connections
     * @param config artemis configuration
     */
    @AssistedInject
    public Rpm(
            @Assisted final Host host,
            @Assisted final Deployment deployment,
            final SshSessionFactory sshFactory,
            final Configuration config) {
        _host = host;
        _deployment = deployment;
        _sshFactory = sshFactory;
        Logger.info("Started rpm deployment actor for host " + host.getName());
        context().parent().tell(new HostDeploymentNotifications.DeploymentStarted(host), self());
        self().tell("start", self());
    }

    @Override
    public void onReceive(final Object message) {
        if ("start".equals(message)) {
            deploy();
        } else {
            unhandled(message);
        }
    }

    private void deploy() {
        try (final SSHClient sshClient = _sshFactory.create(_host.getName())) {
            final Map<String, PackageVersion> deploymentMap = _deployment.getManifestHistory().getManifest().asPackageMap();
            // Yum install
            final Map<String, String> installedPackages = getInstalledPackages(sshClient, deploymentMap);

            final List<String> installTargets = Lists.newArrayList();
            final List<String> updateTargets = Lists.newArrayList();
            final List<String> downgradeTargets = Lists.newArrayList();
            for (final Map.Entry<String, PackageVersion> entry : deploymentMap.entrySet()) {
                final String pkg = entry.getKey();
                final String newVersion = entry.getValue().getVersion();
                final String oldVersion = installedPackages.get(pkg);
                final String line;
                if (oldVersion != null) {
                    final int comparison = RPM_VERSION_COMPARATOR.compare(oldVersion, newVersion);
                    if (comparison < 0) {
                        line = String.format("upgrade package %s from %s to %s", pkg, oldVersion, newVersion);
                        updateTargets.add(pkg);
                    } else if (comparison > 0) {
                        line = String.format("downgrade package %s from %s to %s", pkg, oldVersion, newVersion);
                        downgradeTargets.add(pkg);
                    } else {
                        line = String.format("package %s version not changing", pkg);
                    }
                } else {
                    line = String.format("installing package %s version %s", pkg, newVersion);
                    installTargets.add(pkg);
                }
                context().parent().tell(new HostDeploymentNotifications.DeploymentLog(_host, line), self());
            }

            if (installTargets.size() > 0 || downgradeTargets.size() > 0 || updateTargets.size() > 0) {
                executeRequired(sshClient, "sudo yum clean expire-cache", "yum");
            }
            executeYum(sshClient, deploymentMap, "install", installTargets);
            executeYum(sshClient, deploymentMap, "downgrade", downgradeTargets);
            executeYum(sshClient, deploymentMap, "update", updateTargets);

            context().parent().tell(new HostDeploymentNotifications.DeploymentSucceeded(_host), self());
            // CHECKSTYLE.OFF: IllegalCatch - we need to catch everything, we'll record it and die
        } catch (final IOException | RuntimeException e) {
            // CHECKSTYLE.ON: IllegalCatch
            context().parent().tell(new HostDeploymentNotifications.DeploymentFailed(_host, e), self());
        }
    }

    private void executeYum(
            final SSHClient sshClient,
            final Map<String, PackageVersion> deploymentMap,
            final String operation,
            final List<String> targetList)
            throws IOException {
        if (targetList.size() > 0) {
            final String installString = targetList.stream()
                    .map(pkg -> String.format("%s-%s", pkg, deploymentMap.get(pkg).getVersion()))
                    .reduce("", (a, b) -> String.format("%s %s", a, b));
            executeRequired(sshClient, String.format("sudo yum %s -y %s 2>&1", operation, installString), "yum");
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
        try (final Session session = sshClient.startSession()) {
            session.allocateDefaultPTY();
            try (
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
        }
        return exitStatus;
    }

    private Map<String, String> getInstalledPackages(
            final SSHClient sshClient,
            final Map<String, PackageVersion> deployVersions)
            throws IOException {
        final String commandString = "rpm -qa --queryformat \"%{NAME} %{VERSION} %{RELEASE}\\n\"";
        final Map<String, String> versions = Maps.newHashMap();
        context().parent().tell(new HostDeploymentNotifications.DeploymentLog(_host, "Executing '" + commandString + "'"), self());
        final Integer exitStatus;
        try (final Session session = sshClient.startSession();
             final Session.Command command = session.exec(commandString);
             final BufferedReader error = new BufferedReader(new InputStreamReader(command.getErrorStream(), Charsets.UTF_8));
             final BufferedReader reader = new BufferedReader(new InputStreamReader(command.getInputStream(), Charsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                final String[] split = line.split(" ", 3);
                if (split.length == 3) {
                    final String name = split[0];
                    final String version = split[1] + "-" + split[2];
                    if (deployVersions.containsKey(name)) {
                        versions.put(name, version);
                    }
                }
                Logger.info("***" + line);
                context().parent().tell(new HostDeploymentNotifications.DeploymentLog(_host, line), self());
                line = reader.readLine();
            }
            command.join(30, TimeUnit.SECONDS);
            session.join(30, TimeUnit.SECONDS);
            exitStatus = command.getExitStatus();
            if (exitStatus == null || exitStatus != 0) {
                throw new IllegalStateException("package lookup exit code was " + exitStatus);
            }
        }
        return versions;
    }

    private final Host _host;
    private final Deployment _deployment;
    private final SshSessionFactory _sshFactory;
    private static final RpmVersionComparator RPM_VERSION_COMPARATOR = new RpmVersionComparator();
}
