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
package org.jtaccuino.gog.dflib.data;

import org.dflib.DataFrame;
import org.dflib.csv.Csv;

/**
 * Data loading for the fuel-economy (mpg) dataset.
 * <p>
 * Provides a {@link #loadMpg()} method that returns the raw DataFrame.
 */
public class MpgDatasets {

    private static final SoftCache<DataFrame> CACHE = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private MpgDatasets() {
    }

    /**
     * Loads the mpg CSV.
     *
     * @return a {@link DataFrame} with columns displ, hwy, drv, class, etc.
     */
    public static DataFrame loadMpg() {
        return CACHE.get(MpgDatasets::read);
    }

    private static DataFrame read() {
        return Csv.loader()
                .doubleCol("displ")
                .doubleCol("year")
                .doubleCol("cyl")
                .doubleCol("cty")
                .doubleCol("hwy")
                .load(CsvResources.reader("/examples/mpg/mpg.csv"));
    }
}
