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

import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * JSON REST Apis.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface Api {
    /**
     * Performs a search for a hostclass.
     *
     * @param query the query
     * @return an http response
     */
    CompletionStage<Result> hostclassSearch(String query);

    /**
     * Performs a search for a package.
     *
     * @param query the query
     * @return an http response
     */
    CompletionStage<Result> packageSearch(String query);

    /**
     * Performs a search for an environment.
     *
     * @param query the query
     * @return an http response
     */
    CompletionStage<Result> environmentSearch(String query);

    /**
     * Gets a list of stages in an environment.
     *
     * @param envName the environment name
     * @return an http response
     */
    CompletionStage<Result> getStages(String envName);

    /**
     * Updates the packages and versions in a stage.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @return an http response
     */
    CompletionStage<Result> updateStagePackageVersions(String envName, String stageName);

    /**
     * Streams the log of a deployment.
     *
     * @param deploymentId the deployment id
     * @return an http response
     */
    CompletionStage<Result> deploymentLog(long deploymentId);

    /**
     * Gets a list of packages in a manifest version.
     *
     * @param envName the environment name
     * @param version the version of the manifest
     * @return an http response
     */
    CompletionStage<Result> getReleasePreview(String envName, String version);
}
