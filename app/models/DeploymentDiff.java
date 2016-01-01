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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Describes a diff from one deployment to the next.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DeploymentDiff {
    /**
     * Public constructor.
     *
     * @param oldManifest the old manifest
     * @param newManifest the new manifest
     */
    public DeploymentDiff(final Manifest oldManifest, final Manifest newManifest) {
        _changeList = getPackageChanges(oldManifest, newManifest);
    }

    public List<PackageChange> getChangeList() {
        return _changeList;
    }

    /**
     * Computes the changes from one manifest to another.
     *
     * @param oldManifest the old manifest
     * @param newManifest the new manifest
     * @return a list of package changes
     */
    List<PackageChange> getPackageChanges(final Manifest oldManifest, final Manifest newManifest) {
        final List<PackageVersion> oldPackages = oldManifest.getPackages();
        final ImmutableMap<String, PackageVersion> oldMap = Maps.uniqueIndex(oldPackages, (v) -> v.getPkg().getName());
        final ImmutableMap<String, PackageVersion> newMap = Maps.uniqueIndex(newManifest.getPackages(),
                                                                             (v) -> v.getPkg().getName());
        final MapDifference<String, PackageVersion> mapDifference = Maps.difference(oldMap, newMap);

        final List<PackageChange> changes = Lists.newArrayList();
        mapDifference.entriesOnlyOnLeft().forEach(
                (k, v) -> changes.add(new PackageChange(k, Optional.of(v.getVersion()), Optional.empty())));

        mapDifference.entriesOnlyOnRight().forEach(
                (k, v) -> changes.add(new PackageChange(k, Optional.empty(), Optional.of(v.getVersion()))));

        mapDifference.entriesDiffering().forEach(
                (k, v) -> changes.add(new PackageChange(
                        k, Optional.of(v.leftValue().getVersion()), Optional.of(v.rightValue().getVersion()))));

        mapDifference.entriesInCommon().forEach(
                (k, v) -> changes.add(new PackageChange(k, Optional.of(v.getVersion()), Optional.of(v.getVersion()))));
        changes.sort(Comparator.comparing(PackageChange::getName));
        return changes;
    }

    private final List<PackageChange> _changeList;
}
