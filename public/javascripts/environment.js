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
  window.Environment = window.Environment || {};
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
        configHolder.css({display: 'block'}).html('Something went wrong!');
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
         contentType : 'application/json',
         type : 'POST',
         success: previewConfigSuccessHandler,
         error: previewConfigErrorHandler
        });
        return false;
      };

      window.Environment.init = function() {
        $(".config-display-control .show-config").on("click", showConfigHandler);
        $(".config-display-control .hide-config").on("click", hideConfigHandler);
        $("#preview-merged-config").on("click", previewMergedConfig);
        initConfigDisplay();
      };
  }).call(this);
