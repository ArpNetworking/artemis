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

import client.DockerDeploymentClient;
import models.Deployment;
import models.Host;

/**
 * Host deployment factory.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface HostDeploymentFactory {
    /**
     * Create a roller deployment.
     *
     * @param host the host to deploy to
     * @return a new host deployment strategy
     */
    Roller createRoller(Host host);

    /**
     * Create a docker deployment.
     *
     * @param deploymentClient the docker deployment client
     * @return a new host deployment strategy
     */
    Docker createDocker(DockerDeploymentClient deploymentClient);

    /**
     * Create a docker deployment.
     *
     * @param host the host to deploy to
     * @param deployment the deployment spec
     * @return a new host deployment strategy
     */
    Rpm createRpm(Host host, Deployment deployment);
}
