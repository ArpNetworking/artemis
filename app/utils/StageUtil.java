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
package utils;

import models.ConflictedPackages;
import models.Manifest;
import models.Stage;

import javax.annotation.Nonnull;

/**
 * Set of utility functions related to Stages.
 *
 * @author Nabin Timsina (ntimsina at groupon dot com)
 */
public final class StageUtil {
    private StageUtil() {}

    /**
     * Gets the conflicted packages descrption for a stage and a proposed manifest.
     *
     * @param stage the stage
     * @param manifest the proposed manifest
     * @return a conflicted package description
       */
    public static ConflictedPackages getConflictedPackages(@Nonnull final Stage stage, @Nonnull final Manifest manifest) {
        return new ConflictedPackages(stage, manifest);
    }
}
