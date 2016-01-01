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
 * Deployment controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public interface Deployment {
    /**
     * Shows a deployment detail page.
     *
     * @param deploymentId the deployment id
     * @return a {@link Result}
     */
    F.Promise<Result> detail(long deploymentId);

    /**
     * Shows a deployment log page.
     *
     * @param deploymentId the deployment id
     * @return a {@link Result}
     */
    F.Promise<Result> log(long deploymentId);

    /**
     * Shows a deployment diff page.
     *
     * @param deploymentId the deployment id
     * @return a {@link Result}
     */
    F.Promise<Result> diff(long deploymentId);
}
