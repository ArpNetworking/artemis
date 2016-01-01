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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of a deployment prep stage for Roller.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class RollerDeploymentPrep {
    /**
     * Create a {@link DeploymentDescription} from a {@link Stage} and a {@link Manifest}.
     * @param stage the stage
     * @param proposedManifest the manifest
     * @return a deployment description
     */
    public DeploymentDescription getDeploymentDescription(final Stage stage, final Manifest proposedManifest) {
        final Manifest oldManifest = ManifestHistory.getCurrentForStage(stage).getManifest();

        final DeploymentDiff diff = new DeploymentDiff(oldManifest, proposedManifest);
        final List<Host> hosts = stage.getHostclasses()
                .stream()
                .flatMap(hc -> hc.getHosts().stream())
                .distinct()
                .sorted(Comparator.comparing(Host::getName))
                .collect(Collectors.toList());
        return new DeploymentDescription.Builder()
                .setDeploymentDiff(diff)
                .setHosts(hosts)
                .build();
    }
}
