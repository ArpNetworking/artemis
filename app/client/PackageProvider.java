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
package client;

import com.google.common.collect.Maps;
import play.libs.F;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A provider that can be used to get a list of packages.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface PackageProvider {
    F.Promise<PackageProvider.PackageListResponse> getAllPackages();

    /**
     * A response for a list of packages.
     */
    public static class PackageListResponse {
        /**
         * Public constructor.
         *
         * @param packages the list of packages
         */
        public PackageListResponse(final Map<String, List<PackageVersionMetadata>> packages) {
            _packages = Maps.newLinkedHashMap(packages);
        }

        public Map<String, List<PackageVersionMetadata>> getPackages() {
            return Collections.unmodifiableMap(_packages);
        }

        private final Map<String, List<PackageVersionMetadata>> _packages;
    }

    /**
     * Metadata for a package version.
     */
    public static class PackageVersionMetadata {
        /**
         * Public constructor.
         *
         * @param name package name
         * @param version package version
         * @param bad whether the package is bad
         */
        public PackageVersionMetadata(final String name, final String version, final boolean bad) {
            _name = name;
            _version = version;
            _bad = bad;
        }

        public boolean isBad() {
            return _bad;
        }

        public String getName() {
            return _name;
        }

        public String getVersion() {
            return _version;
        }

        private final String _name;
        private final String _version;
        private final boolean _bad;
    }
}
