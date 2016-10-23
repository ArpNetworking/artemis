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

import models.Environment;
import models.EnvironmentType;
import models.Owner;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Form binding for a new environment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class NewEnvironment {
    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    public Long getOwner() {
        return _owner;
    }

    public void setOwner(final Long owner) {
        _owner = owner;
    }

    public Long getParent() {
        return _parent;
    }

    public void setParent(final Long parent) {
        _parent = parent;
    }

    public String getEnvType() {
        return _envType;
    }

    public void setEnvType(final String envType) {
        _envType = envType;
    }
    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<NewEnvironment> form(final FormFactory factory) {
        return factory.form(NewEnvironment.class);
    }

    /**
     * Validates the form binding.
     *
     * @return a list of binding errors
     */
    public List<ValidationError> validate() {
        final List<ValidationError> errors = new ArrayList<ValidationError>();
        if (Owner.getById(_owner) == null) {
            errors.add(new ValidationError("owner", "Unknown owner"));
        }

        if (_parent != null && Environment.getById(_parent) == null) {
            errors.add(new ValidationError("parent", "Unknown parent environment"));
        }

        try {
            getEnvironmentType();
        } catch (final IllegalArgumentException e) {
            errors.add(new ValidationError("type", "Type must be either 'roller' or 'docker'"));
        }

        return errors.isEmpty() ? null : errors;
    }

    public EnvironmentType getEnvironmentType() {
        return EnvironmentType.valueOf(_envType.toUpperCase(Locale.ENGLISH));
    }

    @Constraints.Required
    private String _name;
    private Long _owner;
    private Long _parent;
    private String _envType;

}
