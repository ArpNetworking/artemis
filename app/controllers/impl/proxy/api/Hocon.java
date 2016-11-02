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
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.NoIncluder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * JSON REST Apis for Hocon related services.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
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
    public CompletionStage<Result> isValid() {
        final JsonNode postJson = request().body().asJson();
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
     * Most specific config at the front.
     */
    private Config combineHocons(final List<String> configs) {
        Config finalHocon = ConfigFactory.empty();
        final ConfigParseOptions parseOptions = ConfigParseOptions.defaults().setIncluder(new NoIncluder()).setAllowMissing(false);
        for (final String config: Lists.reverse(configs)) {
            finalHocon = ConfigFactory.parseString(config, parseOptions).withFallback(finalHocon);
        }
        return finalHocon;
    }

    private ObjectNode hoconToJson(final Config  hocon) {
        final ObjectNode resultJson =  JsonNodeFactory.instance.objectNode();
        for (final Map.Entry<String, ConfigValue> entry : hocon.entrySet()) {
            resultJson.put(entry.getKey(), entry.getValue().unwrapped().toString());
        }
        return  resultJson;
    }

    /**
     * List of config for an environment with its parents.
     */
    private List<String> hoconConfigsForEnv(final models.Environment environment) {
        final List<String> finalConfigs = new ArrayList<>();
        models.Environment currentEnv = environment;
        while (currentEnv != null) {
            final String config = currentEnv.getConfig();
            finalConfigs.add(config);
            currentEnv = currentEnv.getParent();
        }
        return finalConfigs;
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
    public CompletionStage<Result> viewCombinedHocon() {
        final JsonNode postJson = request().body().asJson();
        final String hoconText = postJson.get("hocon").asText();
        final String type = postJson.get("type").asText();
        final Long id = postJson.get("id").asLong();
        List<String> hoconsToCombine = new ArrayList<>();
        if (type.equals("environment")) {
            final models.Environment startEnv = models.Environment.getById(id);
            if (startEnv == null) {
                return CompletableFuture.completedFuture(badRequest());
            }
            hoconsToCombine = hoconConfigsForEnv(startEnv.getParent());
        } else if (type.equals("stage")) {
            final models.Stage stage = models.Stage.getById(id);
            if (stage == null) {
                return CompletableFuture.completedFuture(badRequest());
            }
            hoconsToCombine = hoconConfigsForEnv(stage.getEnvironment());
        }
        hoconsToCombine.add(0, hoconText);
        final Config finalHocon = combineHocons(hoconsToCombine);
        final ObjectNode resultJson = hoconToJson(finalHocon);
        return CompletableFuture.completedFuture(ok(resultJson));
    }

}
