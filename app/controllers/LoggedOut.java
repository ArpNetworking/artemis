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

import org.webjars.play.WebJarsUtil;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

/**
 * Post logout controller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LoggedOut extends Controller {
    /**
     * Public constructor.
     */
    @Inject
    public LoggedOut(final WebJarsUtil webJarsUtil) {
        _webJarsUtil = webJarsUtil;
    }

    /**
     * Shows logout screen.
     *
     * @return an html response
     */
    public CompletionStage<Result> index(final Http.Request request) {
        return CompletableFuture.completedFuture(ok(views.html.loggedout.render(request, _webJarsUtil)));
    }

    private WebJarsUtil _webJarsUtil;
}
