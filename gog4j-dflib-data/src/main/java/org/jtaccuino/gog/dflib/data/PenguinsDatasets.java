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
import org.dflib.ValueMapper;
import org.dflib.csv.Csv;

/**
 * Data loading for the Palmer Penguins dataset (CC0).
 * <p>
 * Provides a {@link #loadPenguins()} method that returns the raw DataFrame
 * with the columns species, island, bill_length_mm, bill_depth_mm,
 * flipper_length_mm, body_mass_g, sex and year.
 */
public class PenguinsDatasets {

    private static final SoftCache<DataFrame> CACHE = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private PenguinsDatasets() {
    }

    /**
     * Loads the penguins CSV.
     *
     * @return a {@link DataFrame} with 344 rows and 8 columns
     */
    public static DataFrame loadPenguins() {
        return CACHE.get(PenguinsDatasets::read);
    }

    private static DataFrame read() {
        return Csv.loader()
                .nullString("NA")
                .col("bill_length_mm", ValueMapper.stringToDouble())
                .col("bill_depth_mm", ValueMapper.stringToDouble())
                .col("flipper_length_mm", ValueMapper.stringToDouble())
                .col("body_mass_g", ValueMapper.stringToDouble())
                .load(CsvResources.reader("/examples/penguins/penguins.csv"));
    }
}
