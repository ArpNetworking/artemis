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

import models.Hostclass;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Form binding for a new hostclass.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class NewHostclass {
    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    public Long getParent() {
        return _parent;
    }

    public void setParent(final Long parent) {
        _parent = parent;
    }

    public List<Long> getHosts() {
        return _hosts;
    }

    public void setHosts(final List<Long> hosts) {
        _hosts = hosts;
    }

    /**
     * Factory method for a form.
     *
     * @return a new {@link Form} bound to this class
     * @param factory form factory to create forms
     */
    public static Form<NewHostclass> form(final FormFactory factory) {
        return factory.form(NewHostclass.class);
    }

    /**
     * Validates the form binding.
     *
     * @return a list of binding errors
     */
    public List<ValidationError> validate() {
        final List<ValidationError> errors = new ArrayList<>();

        if (_parent != null && Hostclass.getById(_parent) == null) {
            errors.add(new ValidationError("hostclass", "Unknown parent hostclass"));
        }

        return errors.isEmpty() ? null : errors;
    }

    @Constraints.Required
    private String _name;
    private Long _parent;
    private List<Long> _hosts;
}
