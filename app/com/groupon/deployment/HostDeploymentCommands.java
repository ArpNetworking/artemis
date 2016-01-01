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

import models.Host;
import models.Manifest;
import models.Stage;

/**
 * A strategy to deploy a manifest to a host.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HostDeploymentCommands {
    /**
     * Start a deployment to a host.
     */
    public static final class StartDeployment {
        /**
         * Public constructor.
         *
         * @param manifest manifest to deploy
         * @param host host to deploy to
         * @param stage stage to deploy
         */
        public StartDeployment(final Manifest manifest, final Host host, final Stage stage) {
            _manifest = manifest;
            _host = host;
            _stage = stage;
        }

        public Host getHost() {
            return _host;
        }

        public Manifest getManifest() {
            return _manifest;
        }

        public Stage getStage() {
            return _stage;
        }

        private final Manifest _manifest;
        private final Host _host;
        private final Stage _stage;
    }
}
