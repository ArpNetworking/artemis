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
 * Holds methods for authentication.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface Authentication {
    /**
     * Authenticate a client.  Redirects to github to do OAuth.
     *
     * @param redirectUrl URL to redirect back to when authentication is complete
     * @return a {@link Result}
     */
    CompletionStage<Result> auth(String redirectUrl);

    /**
     * Finishes the authentication of a client.
     *
     * @param code code to use to get OAuth token
     * @param state the state
     * @return a {@link Result}
     */
    CompletionStage<Result> finishAuth(String code, String state);

    /**
     * Logs a user out.
     *
     * @return a {@link Result}
     */
    CompletionStage<Result> logout();
}
