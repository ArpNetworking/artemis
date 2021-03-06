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

/**
 * Form to hold the data about the previous manifest version and the new packages.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DeployManifest {
    public long getManifest() {
        return _manifest;
    }

    public void setManifest(final long manifest) {
        _manifest = manifest;
    }

    public long getVersion() {
        return _version;
    }

    public void setVersion(final long version) {
        _version = version;
    }

    /**
     * Factory method for a form.
     *
     * @param factory form factory to create forms
     * @return a new {@link Form} bound to this class
     */
    public static Form<DeployManifest> form(final FormFactory factory) {
        return factory.form(DeployManifest.class);
    }

    private long _manifest;
    private long _version;
}
