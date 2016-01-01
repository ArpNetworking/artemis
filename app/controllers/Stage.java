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
package controllers;

import play.libs.F;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

/**
 * Stage controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public interface Stage {
    /**
     * Shows a stage detail page.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return a {@link Result}
     */
    F.Promise<Result> detail(String envName, String stageName);

    /**
     * Add a hostclass to a stage.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return a {@link Result}
     */
    F.Promise<Result> addHostclass(String envName, String stageName);

    /**
     * Remove a hostclass from a stage.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @param hostclassName the name of the hostclass to add
     * @return a {@link Result}
     */
    F.Promise<Result> removeHostclass(String envName, String stageName, String hostclassName);

    /**
     * Prepare a deployment.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return a {@link Result}
     */
    F.Promise<Result> prepareDeploy(String envName, String stageName);

    /**
     * Prepare a deployment.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @param manifestId the manifest
     * @return a {@link Result}
     */
    F.Promise<Result> prepareDeployManifest(String envName, String stageName, long manifestId);

    /**
     * Preview a deployment.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return a {@link Result}
     */
    F.Promise<Result> previewDeploy(String envName, String stageName);

    /**
     * Confirm a deployment.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @param version a version string to prevent races
     * @param manifestId the manifest to deploy
     * @return a {@link Result}
     */
    F.Promise<Result> confirmDeploy(String envName, String stageName, long version, long manifestId);

    /**
     * Create a stage.
     *
     * @param envName the environment name
     * @return a {@link Result}
     */
    F.Promise<Result> create(String envName);

    /**
     * Update stage config.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return a {@link Result}
     */
    F.Promise<Result> save(String envName, String stageName);

    /**
     * Promote a stage to another.
     *
     * @param sourceEnvName the source environment name
     * @param sourceStageName the source stage name
     * @return a {@link Result}
     */
    F.Promise<Result> promote(String sourceEnvName, String sourceStageName);

    /**
     * Synchronize this stage from another.
     *
     * @param sourceEnvName the source environment name
     * @param sourceStageName the source stage name
     * @return a {@link Result}
     */
    F.Promise<Result> synchronize(String sourceEnvName, String sourceStageName);
}
