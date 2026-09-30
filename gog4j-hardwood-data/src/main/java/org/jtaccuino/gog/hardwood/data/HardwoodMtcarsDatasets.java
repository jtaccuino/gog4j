/*
 * Copyright 2026 JTaccuino project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.gog.hardwood.data;

import org.jtaccuino.gog.hardwood.HardwoodDataFrame;

/**
 * The Motor Trend Cars dataset (32 rows) as a Hardwood Parquet table.
 */
public final class HardwoodMtcarsDatasets {

    private static final SoftCache<HardwoodDataFrame> CACHE = new SoftCache<>();

    private HardwoodMtcarsDatasets() {
    }

    /**
     * Loads the Motor Trend Cars dataset (32 rows) as a Hardwood Parquet table.
     *
     * @return the table
     */
    public static HardwoodDataFrame loadMtcars() {
        return CACHE.get(() -> HardwoodDataFrame.ofResource("/examples/mtcars/mtcars.parquet"));
    }
}
