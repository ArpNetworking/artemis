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

var source = new EventSource('/api/deployLog/' + deploymentId);
source.addEventListener("log", function(e) {
    var messages = JSON.parse(e.data).messages;
    console.log("messages", messages, e);
    messages.forEach(function(msg) {
        $("#entries").append(msg.timestamp + " [" + msg.host + "] - " + msg.line + "\n");
    });
});

source.addEventListener("end", function(e) {
    source.close();
});
