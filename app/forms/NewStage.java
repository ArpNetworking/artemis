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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import play.data.Form;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.List;
import java.util.Map;

/**
 * Form binding for a new environment.
 *
 * @author Brandon Arp (barp at groupon dot com)
*/
public class NewStage {
    /**
     * Factory method for a form.
     *
     * @return a new {@link Form} bound to this class
     */
    public static Form<NewStage> form() {
        return Form.form(NewStage.class);
    }

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    /**
     * Validates the form binding.
     *
     * @return a list of binding errors
     */
    public Map<String, List<ValidationError>> validate() {
        final Map<String, List<ValidationError>> errors = Maps.newHashMap();
        if (_name.trim().equals("")) {
            final List<ValidationError> errorList = Lists.newArrayList();
            final ValidationError nameValidationError = new ValidationError("name", EMPTY_ERROR);
            errorList.add(nameValidationError);
            errors.put("name", errorList);
        }
        return errors.isEmpty() ? null : errors;
    }

  @Constraints.Required
  private String _name;
  private static final String EMPTY_ERROR = "Empty value is not allowed";
}
