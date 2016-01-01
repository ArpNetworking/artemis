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

import models.Manifest;
import models.Stage;

/**
 * Commands for fleet deployment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class FleetDeploymentCommands {
    /**
     * Deploy to a stage.
     */
    public static final class DeployStage {
        /**
         * Public constructor.
         *
         * @param stage the stage to deploy to
         * @param manifest the manifest to deploy
         * @param initiator the initiator of the deployment
         */
        public DeployStage(final Stage stage, final Manifest manifest, final String initiator) {
            _stage = stage;
            _deployment = manifest;
            _initiator = initiator;
        }

        public Manifest getDeployment() {
            return _deployment;
        }

        public Stage getStage() {
            return _stage;
        }

        public String getInitiator() {
            return _initiator;
        }

        private final Stage _stage;
        private final Manifest _deployment;
        private final String _initiator;
    }
}
