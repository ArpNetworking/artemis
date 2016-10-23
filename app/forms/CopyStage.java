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
 * Form for applying a stage to another.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class CopyStage {

    public String getEnvName() {
        return _envName;
    }

    public void setEnvName(final String envName) {
        _envName = envName;
    }

    public String getStageName() {
        return _stageName;
    }

    public void setStageName(final String stageName) {
        _stageName = stageName;
    }

    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<CopyStage> form(final FormFactory factory) {
        return factory.form(CopyStage.class);
    }

    @Constraints.Required
    private String _envName;
    private String _stageName;
}
