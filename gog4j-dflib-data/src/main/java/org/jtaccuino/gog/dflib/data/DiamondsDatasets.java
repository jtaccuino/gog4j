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
 * Data loading for the diamonds dataset.
 * <p>
 * Provides a {@link #loadDiamonds()} method that returns a pre-configured DataFrame.
 */
public class DiamondsDatasets {

    private static final SoftCache<DataFrame> CACHE = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private DiamondsDatasets() {
    }

    /**
     * Loads the diamonds CSV with proper double-column type inference.
     *
     * @return a {@link DataFrame} with columns carat, depth, table, price, x, y, z, cut, color, clarity
     */
    public static DataFrame loadDiamonds() {
        return CACHE.get(DiamondsDatasets::read);
    }

    private static DataFrame read() {
        return Csv.loader()
                .doubleCol("carat")
                .doubleCol("depth")
                .doubleCol("table")
                .doubleCol("price")
                .doubleCol("x")
                .doubleCol("y")
                .doubleCol("z")
                .load(CsvResources.reader("/examples/diamonds/diamonds.csv"));
    }
}
