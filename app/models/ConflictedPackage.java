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

/**
 * Describes a conflicted package.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
public class ConflictedPackage {

    /**
     * Public constructor.
     *
     * @param packageName the package name
     * @param hostclass the hostclass
     * @param packageVersion the package version
     * @param stage the stage
     */
    public ConflictedPackage(final String packageName, final Hostclass hostclass, final PackageVersion packageVersion, final Stage stage) {
        this._packageName = packageName;
        this._hostclass = hostclass;
        this._packageVersion = packageVersion;
        this._stage = stage;
    }

    public String getPackageName() {
        return _packageName;
    }

    public Hostclass getHostclass() {
        return _hostclass;
    }

    public PackageVersion getPackageVersion() {
        return _packageVersion;
    }

    public Stage getStage() {
        return _stage;
    }

    private final String _packageName;
    private final Hostclass _hostclass;
    private final PackageVersion _packageVersion;
    private final Stage _stage;
}
