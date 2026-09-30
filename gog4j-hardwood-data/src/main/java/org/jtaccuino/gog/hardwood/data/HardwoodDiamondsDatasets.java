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

import java.util.List;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;

/**
 * The diamonds dataset (53,940 rows) as a Hardwood Parquet table. The columns
 * mirror the classic grammar demo: {@code carat}, {@code cut}, {@code color},
 * {@code clarity}, and the numeric dimensions and price.
 */
public final class HardwoodDiamondsDatasets {

    private static final SoftCache<HardwoodDataFrame> CACHE = new SoftCache<>();

    private HardwoodDiamondsDatasets() {
    }

    /**
     * Loads the diamonds table.
     *
     * @return the diamonds frame
     */
    public static HardwoodDataFrame loadDiamonds() {
        return CACHE.get(() -> HardwoodDataFrame.ofResource("/examples/diamonds/diamonds.parquet"));
    }

    /** {@return the column names of the diamonds dataset} */
    public static List<String> columnNames() {
        return loadDiamonds().columnNames();
    }
}
