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
package com.groupon.deployment;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.PKCS5KeyFile;

import java.io.IOException;
import java.util.List;

/**
 * A factory for creating ssh sessions.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SshjSessionFactory implements SshSessionFactory {
    /**
     * Public constructor.
     *
     * @param config Artemis configuration
     */
    @Inject
    public SshjSessionFactory(final Config config) {
        _userName = config.getString("ssh.user");
        _keyPath = config.getString("ssh.keyFile");
    }

    @Override
    public SSHClient create(final String host) {
        try {
            final DefaultConfig config = new DefaultConfig();
            final List<Factory.Named<FileKeyProvider>> keyProviders = Lists.newArrayList();
            keyProviders.add(new PKCS5KeyFile.Factory());
            config.setFileKeyProviderFactories(keyProviders);

            final SSHClient client = new SSHClient(config);
            final KeyProvider keys = client.loadKeys(_keyPath);
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(host);
            client.authPublickey(_userName, keys);
            return client;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final String _userName;
    private final String _keyPath;
}
