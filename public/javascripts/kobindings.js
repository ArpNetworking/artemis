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
ko.bindingHandlers["typeahead"] = {
    init: function (element, valueAccessor, allValuesAccessor) {
        var value = valueAccessor();
        var valueUnwrapped = ko.utils.unwrapObservable(value);

        var ta = $(element).typeahead(valueUnwrapped.options.opt, valueUnwrapped.options.source);

        if (valueUnwrapped.value !== undefined) {
            ta.data().ttTypeahead.input.onSync(
                "queryChanged",
                function() {
                    valueUnwrapped.value(ta.typeahead('val'));
                });

            ta.on(
                'typeahead:autocompleted',
                function() {
                    valueUnwrapped.value(ta.typeahead('val'));
                });

            ta.on('typeahead:selected', function() {
                valueUnwrapped.value(ta.typeahead('val'));
            });

            //Hack to handle the clearing of the query
            valueUnwrapped.value.subscribe(
                function(newValue) {
                    if (newValue == "")
                    {
                        ta.typeahead('val', '');
                    }
                });
        }
    }
};
