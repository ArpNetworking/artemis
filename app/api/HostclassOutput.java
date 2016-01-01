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
package api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the output needed for roller to run a hostclass file.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HostclassOutput extends YamlUnknownBase {
    public Packages getPackages() {
        return _packages;
    }

    public void setPackages(final Packages packages) {
        _packages = packages;
    }

    public Map<String, ?> getParams() {
        return _params;
    }

    public void setParams(final Map<String, ?> params) {
        _params = params;
    }

    @JsonProperty("packages")
    private Packages _packages;
    @JsonProperty("params")
    private Map<String, ?> _params;

    /**
     * Represents the packages block in Roller.
     */
    public static class Packages {
        public Set<Package> getFailsafe() {
            return _failsafe;
        }

        /**
         * Setter for failsafe packages.
         *
         * @param failsafe the failsafe packages
         */
        public void setFailsafe(final Set<Package> failsafe) {
            _failsafe = Sets.newTreeSet(new NameVersionComparitor());
            _failsafe.addAll(failsafe);
        }

        public Set<Package> getProduction() {
            return _production;
        }

        /**
         * Setter for production packages.
         *
         * @param production the production packages
         */
        public void setProduction(final Set<Package> production) {
            _production = Sets.newTreeSet(new NameVersionComparitor());
            _production.addAll(production);
        }

        @JsonProperty("failsafe")
        private Set<Package> _failsafe;
        @JsonProperty("production")
        private Set<Package> _production;
    }

    /**
     * Represents a package in Roller.
     */
    @JsonSerialize(using = PackageSerializer.class)
    public static class Package {
        /**
         * Public constructor.
         *
         * @param rawVal the raw string value of the package (in the form of name-version)
         */
        public Package(final String rawVal) {
            final List<String> split = Splitter.on("-").omitEmptyStrings().splitToList(rawVal);
            if (split.size() != 2) {
                throw new IllegalArgumentException(String.format("Unable to parse package string; string=%s", rawVal));
            }
            _name = split.get(0);
            _version = split.get(1);
        }

        public String getName() {
            return _name;
        }

        public void setName(final String name) {
            _name = name;
        }

        public String getVersion() {
            return _version;
        }

        public void setVersion(final String version) {
            _version = version;
        }

        private String _name;
        private String _version;
    }

    /**
     * Compares packages by name and version.
     */
    private static final class NameVersionComparitor implements Comparator<Package>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final Package o1, final Package o2) {
            final int nameComp = o1.getName().compareTo(o2.getName());
            if (nameComp != 0) {
                return nameComp;
            }

            return o1.getVersion().compareTo(o2.getVersion());
        }
    }

    /**
     * Serializer for a package to conform to Roller format.
     */
    private static final class PackageSerializer extends JsonSerializer<Package> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(
                final Package value,
                final JsonGenerator jgen,
                final SerializerProvider provider) throws IOException {
            jgen.writeString(String.format("%s-%s", value.getName(), value.getVersion()));
        }
    }
}
