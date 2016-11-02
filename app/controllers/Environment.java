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
import play.mvc.Security;
import utils.AuthN;

import java.util.concurrent.CompletionStage;

/**
 * Environment controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public interface Environment {
    /**
     * Shows an environment detail page.
     *
     * @param name the environment name
     * @return a {@link Result}
     */
    CompletionStage<Result> detail(String name);

    /**
     * Creates a new environment.
     *
     * @param parentEnv the parent environment
     * @return a {@link Result}
     */
    CompletionStage<Result> newEnvironment(String parentEnv);

    /**
     * Create an environment page.
     *
     * @return a {@link Result}
     */
    CompletionStage<Result> create();

    /**
     * Save changes to an environment.
     *
     * @param envName the environment name
     * @return a {@link Result}
     */
    CompletionStage<Result> save(String envName);

    /**
     * Create a new release.
     *
     * @param envName the environment name
     * @return a {@link Result}
     */
    CompletionStage<Result> createRelease(final String envName);

    /**
     * Prepare a release.
     *
     * @param envName the environment name
     * @return a {@link Result}
     */
    CompletionStage<Result> prepareRelease(final String envName);
}
