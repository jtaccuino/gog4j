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
package org.jtaccuino.gog;

import org.jtaccuino.gog.spi.DataExtractor;

/**
 * The family of a {@code matrixPlot(...)} cell, decided from the
 * {@link DataExtractor.ColumnType}s of its row (y) and column (x) variables —
 * the type dispatch.
 */
enum CellKind {
    /** Continuous × continuous: densities, scatters, correlations. */
    TT,
    /** Continuous × categorical (or categorical × continuous): box plots, faceted histograms. */
    COMBO,
    /** Categorical × categorical: count bars. */
    DD;

    /**
     * Maps a row and column value type to its cell family:
     * continuous×continuous → {@link #TT}, categorical×categorical →
     * {@link #DD}, mixed → {@link #COMBO}.
     *
     * @param rowType the type of the y variable ('row' variable)
     * @param colType the type of the x variable ('column' variable)
     * @return the cell family
     */
    static CellKind of(DataExtractor.ColumnType rowType, DataExtractor.ColumnType colType) {
        boolean rowCont = isContinuous(rowType);
        boolean colCont = isContinuous(colType);
        if (rowCont && colCont) {
            return TT;
        }
        if (rowCont || colCont) {
            return COMBO;
        }
        return DD;
    }

    /**
     * Whether the value type plots along a continuous axis. Delegates to
     * {@link DataExtractor.ColumnType#isContinuous} so the notion of
     * "continuous" stays defined in one place — a timestamp column is as
     * continuous as a date column, just with a finer break ladder.
     */
    static boolean isContinuous(DataExtractor.ColumnType type) {
        return DataExtractor.ColumnType.isContinuous(type);
    }
}
