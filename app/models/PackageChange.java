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
package models;

import java.util.Optional;

/**
 * Describes a package change in a deployment.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class PackageChange {
    /**
     * Public constructor.
     *
     * @param name package name
     * @param oldVersion old version
     * @param newVersion new version
     */
    public PackageChange(final String name, final Optional<String> oldVersion, final Optional<String> newVersion) {
        _name = name;
        _oldVersion = oldVersion;
        _newVersion = newVersion;
        _new = !oldVersion.isPresent();
        _deleted = !newVersion.isPresent();
        _updated = newVersion.isPresent() && oldVersion.isPresent() && !oldVersion.get().equals(newVersion.get());
        _same = newVersion.isPresent() && oldVersion.isPresent() && oldVersion.get().equals(newVersion.get());
    }

    public String getName() {
        return _name;
    }

    public Optional<String> getNewVersion() {
        return _newVersion;
    }

    public Optional<String> getOldVersion() {
        return _oldVersion;
    }

    public boolean isDeleted() {
        return _deleted;
    }

    public boolean isNew() {
        return _new;
    }

    public boolean isUpdated() {
        return _updated;
    }

    public boolean isSame() {
        return _same;
    }

    private final String _name;
    private final Optional<String> _oldVersion;
    private final Optional<String> _newVersion;
    private final boolean _new;
    private final boolean _deleted;
    private final boolean _updated;
    private final boolean _same;
}
