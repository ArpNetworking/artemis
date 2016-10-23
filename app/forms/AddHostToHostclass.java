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
package forms;

import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;

/**
 * Form for adding a host to a hostclass.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class AddHostToHostclass {
    public String getHost() {
        return _host;
    }

    public void setHost(final String host) {
        _host = host;
    }

    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<AddHostToHostclass> form(final FormFactory factory) {
        return factory.form(AddHostToHostclass.class);
    }

    @Constraints.Required
    private String _host;
}
