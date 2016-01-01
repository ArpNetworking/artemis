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
package client.ssh;

import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;

/**
 * Defines a factory for creating ssh sessions.
 *
 * @author Kevin Jungmeisteris (kjungmeisteris at groupon dot com)
 *
 */
public interface SSHSessionFactory {

    /**
     * Create a new SSH session for the provided host and user name using the keys with keys from the common location
     * in the file system: {@code ~/.ssh/id_rsa}.
     * @param hostName the host to connect to
     * @param username the user to connect as
     * @return a new ssh session
     * @throws IOException on connection error
     */
    Session newSessionFromKeyFile(String hostName, String username) throws IOException;

    /**
     * Create a new SSH session for the provided host and user name using the keys with keys from the common location
     * in the file system: {@code ~/.ssh/id_rsa} with a password to access the key file.
     * @param hostName the host to connect to
     * @param username the user to connect as
     * @param privateKeyFilePassword the password to the key file
     * @return a new ssh session
     * @throws IOException on connection error
     */
    Session newSessionFromKeyFile(String hostName, String username, String privateKeyFilePassword) throws IOException;

    /**
     * Create a new SSH session for the provided host and user name using the private and public keys provided.
     * @param hostName the host to connect to
     * @param username the user to connect as
     * @param publicKey the public key
     * @param privateKey the private key
     * @return a new ssh session
     * @throws IOException on connection error
     */
    Session newSessionFromKeyPair(String hostName, String username, String publicKey, String privateKey) throws IOException;
}
