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
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import models.Environment;
import models.Stage;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import utils.NoIncluder;

import java.util.List;

/**
 * Form for adding config.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
public class ConfigForm {
    public String getConfig() {
        return _config;
    }

    public void setConfig(final String config) {
        _config = config;
    }

    public Long getId() {
        return _id;
    }

    public void setId(final Long id) {
        _id = id;
    }

    public Long getVersion() {
        return _version;
    }

    public void setVersion(final Long version) {
        _version = version;
    }

    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<ConfigForm> form(final FormFactory factory) {
        return factory.form(ConfigForm.class);
    }

    /**
     * Factory method for a form.
     *
     * @param env Environment to bind from
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<ConfigForm> form(final Environment env, final FormFactory factory) {
        final ConfigForm configForm = new ConfigForm();
        configForm._config = env.getConfig();
        configForm._id = env.getId();
        configForm._version = env.getVersion();
        final Form<ConfigForm> form = factory.form(ConfigForm.class);
        return form.fill(configForm);
    }

    /**
     * Factory method for a form.
     *
     * @param stage Stage to bind from
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<ConfigForm> form(final Stage stage, final FormFactory factory) {
        final ConfigForm configForm = new ConfigForm();
        configForm._config = stage.getConfig();
        configForm._id = stage.getId();
        configForm._version = stage.getVersion();
        final Form<ConfigForm> form = factory.form(ConfigForm.class);
        return form.fill(configForm);
    }


    /**
     * Validates the form binding.
     *
     * @return a list of binding errors
     */
    public List<ValidationError> validate() {
        final List<ValidationError> errors = Lists.newArrayList();

        try {
            final ConfigParseOptions parseOptions = ConfigParseOptions.defaults().setIncluder(new NoIncluder()).setAllowMissing(false);
            ConfigFactory.parseString(_config, parseOptions);
        } catch (final NoIncluder.IncludesNotAllowedException e) {
            errors.add(new ValidationError("config", e.getMessage()));
        } catch (final ConfigException e) {
            errors.add(new ValidationError("config", "Invalid HOCON"));
        }

        return errors.isEmpty() ? null : errors;
    }

    @Constraints.Required
    private String _config;
    @Constraints.Required
    private Long _id;
    @Constraints.Required
    private Long _version;

}
