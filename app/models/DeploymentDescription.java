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

import com.google.common.collect.Lists;
import com.groupon.utility.OvalBuilder;
import net.sf.oval.constraint.NotNull;

import java.util.List;

/**
 * Describes a potential deployment, including any errors or warnings.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class DeploymentDescription {

    public DeploymentDiff getDeploymentDiff() {
        return _deploymentDiff;
    }

    public List<Host> getHosts() {
        return _hosts;
    }

    private DeploymentDescription(final Builder builder) {
        _deploymentDiff = builder._deploymentDiff;
        _hosts = builder._hosts;
    }

    private final DeploymentDiff _deploymentDiff;
    private final List<Host> _hosts;

    /**
     * Implementation of the Builder pattern for DeploymentDescription.
     */
    public static class Builder extends OvalBuilder<DeploymentDescription> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(DeploymentDescription.class);
        }

        /**
         * Sets the deployment diff. Required. Not null.
         *
         * @param value the deployment diff
         * @return This builder
         */
        public Builder setDeploymentDiff(final DeploymentDiff value) {
            _deploymentDiff = value;
            return this;
        }

        /**
         * Sets the hosts. Required. Not null.
         *
         * @param value the hosts
         * @return This builder
         */
        public Builder setHosts(final List<Host> value) {
            _hosts = Lists.newArrayList(value);
            return this;
        }

        @NotNull
        private DeploymentDiff _deploymentDiff;
        @NotNull
        private List<Host> _hosts;
    }
}
