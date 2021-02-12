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

import play.mvc.Http;
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
     * @param request
     * @return an http response
     */
    CompletionStage<Result> hostclassSearch(String query, final Http.Request request);

    /**
     * Performs a search for a package.
     *
     * @param query the query
     * @param request
     * @return an http response
     */
    CompletionStage<Result> packageSearch(String query, final Http.Request request);

    /**
     * Performs a search for an environment.
     *
     * @param query the query
     * @param request
     * @return an http response
     */
    CompletionStage<Result> environmentSearch(String query, final Http.Request request);

    /**
     * Gets a list of stages in an environment.
     *
     * @param envName the environment name
     * @param request
     * @return an http response
     */
    CompletionStage<Result> getStages(String envName, final Http.Request request);

    /**
     * Updates the packages and versions in a stage.
     *
     * @param envName the environment name
     * @param stageName the stage name
     * @param request
     * @return an http response
     */
    CompletionStage<Result> updateStagePackageVersions(String envName, String stageName, final Http.Request request);

    /**
     * Streams the log of a deployment.
     *
     * @param deploymentId the deployment id
     * @param request
     * @return an http response
     */
    CompletionStage<Result> deploymentLog(long deploymentId, final Http.Request request);

    /**
     * Gets a list of packages in a manifest version.
     *
     * @param envName the environment name
     * @param version the version of the manifest
     * @param request
     * @return an http response
     */
    CompletionStage<Result> getReleasePreview(String envName, String version, Http.Request request);
}
