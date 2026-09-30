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
 * Data loading for the restaurant-tips dataset, the canonical mixed-type
 * showcase of a generalized pairs plot.
 * <p>
 * Provides a {@link #loadTips()} method that returns a DataFrame with the
 * numerical columns total_bill, tip and size next to the categorical columns
 * sex, smoker, day and time.
 */
public class TipsDatasets {

    private static final SoftCache<DataFrame> CACHE = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private TipsDatasets() {
    }

    /**
     * Loads the tips CSV.
     *
     * @return a {@link DataFrame} with 244 rows and 7 columns
     */
    public static DataFrame loadTips() {
        return CACHE.get(TipsDatasets::read);
    }

    private static DataFrame read() {
        return Csv.loader()
                .col("total_bill", ValueMapper.stringToDouble())
                .col("tip", ValueMapper.stringToDouble())
                .col("size", ValueMapper.stringToDouble())
                .load(CsvResources.reader("/examples/tips/tips.csv"));
    }
}
