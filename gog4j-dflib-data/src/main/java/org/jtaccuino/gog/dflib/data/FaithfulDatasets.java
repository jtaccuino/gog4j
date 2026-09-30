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
 * Data loading for the Old Faithful dataset (faithfuld).
 * <p>
 * Provides a {@link #loadFaithfuld()} method that returns a DataFrame with
 * columns eruptions, waiting, and density.
 */
public class FaithfulDatasets {

    private static final SoftCache<DataFrame> FAITHFULD = new SoftCache<>();
    private static final SoftCache<DataFrame> FAITHFUL = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private FaithfulDatasets() {
    }

    /**
     * Loads the faithfuld CSV with proper double-column type inference.
     *
     * @return a {@link DataFrame} with columns eruptions, waiting, and density
     */
    public static DataFrame loadFaithfuld() {
        return FAITHFULD.get(FaithfulDatasets::readFaithfuld);
    }

    private static DataFrame readFaithfuld() {
        return Csv.loader()
                .doubleCol("eruptions")
                .doubleCol("waiting")
                .doubleCol("density")
                .load(CsvResources.reader("/examples/faithfuld/faithfuld.csv"));
    }

    /**
     * Loads the raw Old Faithful eruptions CSV with proper double-column type
     * inference — the 272 eruption/waiting observations that the reference
     * {@code Geoms.density2d()} example plots.
     *
     * @return a {@link DataFrame} with columns eruptions and waiting
     */
    public static DataFrame loadFaithful() {
        return FAITHFUL.get(FaithfulDatasets::readFaithful);
    }

    private static DataFrame readFaithful() {
        return Csv.loader()
                .doubleCol("eruptions")
                .doubleCol("waiting")
                .load(CsvResources.reader("/examples/faithful/faithful.csv"));
    }
}
