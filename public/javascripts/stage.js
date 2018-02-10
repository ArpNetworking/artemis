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
  window.Stage = window.Stage || {};
  (function() {
      var showConfigHandler = function() {
        $(".config-display-control .show-config").hide();
        $(".config-display-control .hide-config").show();
        $("#env-config-form").show();
        return false;
      };

      var hideConfigHandler = function() {
        $("#env-config-form").hide();
        $(".config-display-control .show-config").show();
        $(".config-display-control .hide-config").hide();
        return false;
      };

      function initConfigDisplay() {
        if($("#env-config-form").data("isPost")) {
            showConfigHandler();
        } else {
            hideConfigHandler();
        }
      }

      var previewConfigSuccessHandler = function(json) {
        var configHolder = $("#preview-config");
        configHolder.css({display: 'block'}).html(JSON.stringify(json));
        return false;
      };

      var previewConfigErrorHandler = function(json) {
        var configHolder = $("#preview-config");
        configHolder.css({display: 'block'}).html('Something went wrong');
        return false;
      };

      var previewMergedConfig = function() {
        var data = $(this).data();
        var type = data.type;
        var id = data.id;
        var version = data.version;
        var config = $(data.configSelector).val();
        $.ajax("/hocon/viewCombinedHocon", {
         data : JSON.stringify({ hocon: config , type: type, id:id, version: version}),
         beforeSend: function(request) {
             request.setRequestHeader("Csrf-Token", $('input[name="csrfToken"]')[0].value);
         },
         contentType : 'application/json',
         type : 'POST',
         success: previewConfigSuccessHandler,
         error: previewConfigErrorHandler
        });
        return false;
      };

      var hc_lookup = function(query, none, cb) {
          $.ajax(jsRoutes.controllers.Api.hostclassSearch(query))
              .done(
                  function(data) {
                      cb(data.results);
                  });
      };

      var env_lookup = function(query, none, cb) {
          $.ajax(jsRoutes.controllers.Api.environmentSearch(query))
              .done(
                  function(data) {
                      cb(data.results);
                  });
      };

      var stage_populate_promote = function(envName, none, cb) {
          envName = $("#promote_env_input" ).val();
          $.ajax(jsRoutes.controllers.Api.getStages(envName))
          .done(
              function(data) {
                  cb(data.results);
              });
      };

      var stage_populate_synchronize = function(envName, none, cb) {
          envName = $("#synchronize_env_input" ).val();
          $.ajax(jsRoutes.controllers.Api.getStages(envName))
              .done(
                  function(data) {
                      cb(data.results);
                  });
      };

      window.Stage.init = function() {
        $(".config-display-control .show-config").on("click", showConfigHandler);
        $(".config-display-control .hide-config").on("click", hideConfigHandler);
        $("#preview-merged-config").on("click", previewMergedConfig);
        $("#hc_input" ).typeahead(
            {
                minLength: 3,
                hint: true,
                highlight: true
            },
            {
                source: hc_lookup
            });
        $("#promote_env_input" ).typeahead(
            {
                minLength: 3,
                hint: true,
                highlight: true
            },
            {
                source: env_lookup
            });
        $("#promote_stage_input" ).typeahead(
            {
                minLength: 0,
                hint: true,
                highlight: true
            },
            {
                source: stage_populate_promote
            });

        $("#synchronize_env_input" ).typeahead(
            {
                minLength: 3,
                hint: true,
                highlight: true
            },
            {
                source: env_lookup
            });
        $("#synchronize_stage_input" ).typeahead(
            {
                minLength: 0,
                hint: true,
                highlight: true
            },
            {
                source: stage_populate_synchronize
          });
        initConfigDisplay();
      };
  }).call(this);
