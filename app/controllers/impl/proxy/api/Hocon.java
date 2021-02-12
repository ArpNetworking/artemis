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
package controllers.impl.proxy.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.NoIncluder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Singleton;

/**
 * JSON REST Apis for Hocon related services.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
@Singleton
public class Hocon extends Controller {
    /**
     *
         $.ajax("/hocon/validate", {
         data : JSON.stringify({ hocon: "John" }),
         contentType : 'application/json',
         type : 'POST'
         })
     */

    /**
    * expected postBody to be a json
    * {hocon: "all hocon config"}
    **/

    /**
     * Determines if the posted value is valid hocon.
     *
     * @return an http response
     */
    public CompletionStage<Result> isValid(Http.Request request) {
        final JsonNode postJson = request.body().asJson();
        final String hoconText = postJson.get("hocon").asText();
        boolean isValid = true;
        final ConfigParseOptions parseOptions = ConfigParseOptions.defaults().setIncluder(new NoIncluder()).setAllowMissing(false);
        try {
            final Config hoconConfig = ConfigFactory.parseString(hoconText, parseOptions);
        } catch (final ConfigException e) {
            isValid = false;
        }

        final ObjectNode resultJson = Json.newObject();
        resultJson.put("valid", isValid);
        return CompletableFuture.completedFuture(ok(resultJson));
    }

    /**
     * List of config for an environment with its parents.
     */
    private Config hoconConfigsForEnv(final models.Environment environment, final Config config) {
        Config returnConfig = config;
        models.Environment currentEnv = environment;
        while (currentEnv != null) {
            final String configString = currentEnv.getConfig();
            if (!Strings.isNullOrEmpty(configString)) {
                returnConfig = addBackupHoconString(configString, String.format("Environment %s", currentEnv.getName()), returnConfig);
            }
            currentEnv = currentEnv.getParent();
        }
        return returnConfig;
    }

    /**
     *
         $.ajax("/hocon/viewCombinedHocon", {
         data : JSON.stringify({ hocon: "to=John" , type: "environment", id:33}),
         contentType : 'application/json',
         type : 'POST'
         })
     */

    //TODO(barp): Authenticate this so there is no leaking of information [Artemis-?]
    /**
     * Combines the hocon from multiple levels into a single hocon block.
     *
     * @return an http response
     */
    public CompletionStage<Result> viewCombinedHocon(Http.Request request) {
        final JsonNode postJson = request.body().asJson();
        final String hoconText = postJson.get("hocon").asText();
        final String type = postJson.get("type").asText();
        final Long id = postJson.get("id").asLong();


        Config finalHocon = ConfigFactory.empty();

        final models.Environment startEnv;
        switch (type) {
            case "environment":
                startEnv = models.Environment.getById(id);
                if (startEnv == null) {
                    return CompletableFuture.completedFuture(badRequest());
                }
                break;
            case "stage":
                final models.Stage stage = models.Stage.getById(id);
                if (stage == null) {
                    return CompletableFuture.completedFuture(badRequest());
                }
                startEnv = stage.getEnvironment();
                finalHocon = addBackupHoconString(hoconText, String.format("Environment %s, Stage %s", startEnv.getName(), stage.getName()), finalHocon);
                break;
            default:
                throw new RuntimeException("unknown hocon level type");
        }
        finalHocon = hoconConfigsForEnv(startEnv, finalHocon);

        final String rendered = finalHocon.root().render(ConfigRenderOptions.defaults().setFormatted(true).setJson(false).setComments(true));
        return CompletableFuture.completedFuture(ok(rendered));
    }

    private Config addBackupHoconString(final String hoconText, final String location, final Config original) {
        final ConfigParseOptions parseOptions = ConfigParseOptions.defaults().setIncluder(new NoIncluder()).setAllowMissing(false);
        final Config parsed = ConfigFactory.parseString(hoconText, parseOptions.setOriginDescription(location));
        return original.withFallback(parsed);
    }
}
