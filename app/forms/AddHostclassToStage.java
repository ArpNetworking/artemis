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
 * Form for adding a hostclass to a stage.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AddHostclassToStage {
    public String getHostclass() {
        return _hostclass;
    }

    public void setHostclass(final String value) {
        _hostclass = value;
    }

    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<AddHostclassToStage> form(final FormFactory factory) {
        return factory.form(AddHostclassToStage.class);
    }

    @Constraints.Required
    private String _hostclass;
}
