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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Describes conflicted packages.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
public class ConflictedPackages {
    /**
     * Public constructor.
     *
     * @param stage the stage
     * @param manifest the proposed manifest
     */
    public ConflictedPackages(@Nonnull final Stage stage, @Nonnull final Manifest manifest) {
        if (!isStageConflictFree(stage)) {
            final Map<Package, PackageVersion> knownPackageVersionMap = Maps.newHashMap();
            for (final PackageVersion eachPackageVersion: manifest.getPackages()) {
                final String packageName = eachPackageVersion.getPkg().getName();
                if (packageName.isEmpty()) {
                    continue;
                }
                knownPackageVersionMap.put(eachPackageVersion.getPkg(), eachPackageVersion);

            }

            for (final Hostclass eachHostclass : stage.getHostclasses()) {
                for (final Stage eachStage : eachHostclass.getStages()) {
                    if (eachStage == stage || isStageConflictFree(eachStage)) {
                        continue;
                    }
                    final Manifest latestManifest = ManifestHistory.getCurrentForStage(eachStage).getManifest();
                    for (final PackageVersion eachPackageVersion : latestManifest.getPackages()) {
                        final Package eachPackage = eachPackageVersion.getPkg();
                        final String packageName = eachPackage.getName();
                        if (packageName.isEmpty()) {
                            continue;
                        }
                        if (knownPackageVersionMap.containsKey(eachPackage)
                                && !knownPackageVersionMap.get(eachPackage).equals(eachPackageVersion)) {
                            _conflictingPackageNames.add(packageName);
                            _conflictingPackageVersions.add(new ConflictedPackage(packageName, eachHostclass, eachPackageVersion, stage));
                        }
                    }
                }
            }
        }

    }

    /**
     * Determines if this object has package conflicts.
     *
     * @return true if there are conflicts, otherwise false
     */
    public boolean hasConflicts() {
        return _conflictingPackageVersions.size()  > 0;
    }

    public List<ConflictedPackage> getConflictingPackageVersions() {
        return _conflictingPackageVersions;
    }

    /**
     * Determines if a sepcific package has conflicts.
     *
     * @param pkgName the package to check
     * @return true if this package is conflicting, otherwise false
     */
    public boolean packageHasConflicts(final String pkgName) {
        return _conflictingPackageNames.contains(pkgName);
    }

    private boolean isStageConflictFree(final Stage stage) {
        return CONFLICT_FREE_ENV_TYPE.contains(stage.getEnvironment().getEnvironmentType());
    }

    private final Set<String> _conflictingPackageNames = Sets.newHashSet();
    private final List<ConflictedPackage> _conflictingPackageVersions = Lists.newArrayList();
    private static final HashSet<EnvironmentType> CONFLICT_FREE_ENV_TYPE = Sets.newHashSet();
    static {
        CONFLICT_FREE_ENV_TYPE.add(EnvironmentType.DOCKER);
    }
}
