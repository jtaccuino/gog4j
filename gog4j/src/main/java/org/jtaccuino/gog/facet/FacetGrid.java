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
package org.jtaccuino.gog.facet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two-dimensional partition of a grid facet: the ordered row and column
 * keys plus, for every row/column combination, the matching sub-DataFrame or
 * {@code null} for an empty panel.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public final class FacetGrid<DF> {

    /**
     * The margin-level sentinel of a grid cell dimension, following the
     * {@code (all)} strip label. Reuses {@link FacetValues#ALL} so the data
     * driven layer restriction treats a margin grid cell exactly like any other
     * margin value.
     */
    public static final Object ALL = FacetValues.ALL;

    private final List<String> rowVars;
    private final List<String> colVars;
    private final List<List<Object>> rows;
    private final List<List<Object>> cols;
    private final Map<Cell, DF> panels;

    FacetGrid(List<String> rowVars, List<String> colVars, List<List<Object>> rows,
              List<List<Object>> cols, Map<Cell, DF> panels) {
        this.rowVars = List.copyOf(rowVars);
        this.colVars = List.copyOf(colVars);
        this.rows = List.copyOf(rows);
        this.cols = List.copyOf(cols);
        this.panels = panels;
    }

    /**
     * Creates a builder for a grid over the given row and column variables.
     *
     * @param rowVars the row faceting variables, empty for no row dimension
     * @param colVars the column faceting variables, empty for no column dimension
     * @param <DF>    the DataFrame type
     * @return a new {@link Builder}
     */
    public static <DF> Builder<DF> builder(List<String> rowVars, List<String> colVars) {
        return new Builder<>(rowVars, colVars);
    }

    /** {@return the row faceting variables, empty for no row dimension} */
    public List<String> rowVars() {
        return rowVars;
    }

    /** {@return the column faceting variables, empty for no column dimension} */
    public List<String> colVars() {
        return colVars;
    }

    /** {@return the ordered row keys} */
    public List<List<Object>> rows() {
        return rows;
    }

    /** {@return the ordered column keys} */
    public List<List<Object>> cols() {
        return cols;
    }

    /** {@return the number of rows, at least 1} */
    public int numRows() {
        return Math.max(rows.size(), 1);
    }

    /** {@return the number of columns, at least 1} */
    public int numCols() {
        return Math.max(cols.size(), 1);
    }

    /**
     * The sub-frame of one grid cell.
     *
     * @param row the row key
     * @param col the column key
     * @return the matching sub-DataFrame, or {@code null} for an empty panel
     */
    public DF panel(List<Object> row, List<Object> col) {
        return panels.get(new Cell(row, col));
    }

    /**
     * {@return whether the row key contains a margin level}
     *
     * @param key the row key
     */
    public static boolean isMarginRow(List<Object> key) {
        return key.contains(ALL);
    }

    /**
     * {@return whether the column key contains a margin level}
     *
     * @param key the column key
     */
    public static boolean isMarginCol(List<Object> key) {
        return key.contains(ALL);
    }

    /**
     * Accumulates row/column keys and their {@link Cell} sub-frames.
     *
     * @param <DF> the dataframe type
     */
    public static final class Builder<DF> {
        private final List<String> rowVars;
        private final List<String> colVars;
        private final List<List<Object>> rows = new ArrayList<>();
        private final List<List<Object>> cols = new ArrayList<>();
        private final Map<Cell, DF> panels = new LinkedHashMap<>();

        private Builder(List<String> rowVars, List<String> colVars) {
            this.rowVars = rowVars;
            this.colVars = colVars;
        }

        /**
         * Registers a row key, returning its index (adding it on first use).
         *
         * @param key the row key
         * @return the index of the row in the final grid
         */
        public int row(List<Object> key) {
            int idx = rows.indexOf(key);
            if (idx >= 0) {
                return idx;
            }
            rows.add(key);
            return rows.size() - 1;
        }

        /**
         * Registers a column key, returning its index (adding it on first use).
         *
         * @param key the column key
         * @return the index of the column in the final grid
         */
        public int col(List<Object> key) {
            int idx = cols.indexOf(key);
            if (idx >= 0) {
                return idx;
            }
            cols.add(key);
            return cols.size() - 1;
        }

        /**
         * Stores the sub-frame of one cell.
         *
         * @param row the row index from {@link #row}
         * @param col the column index from {@link #col}
         * @param df  the matching sub-frame, or {@code null} for an empty panel
         */
        public void put(int row, int col, DF df) {
            panels.put(new Cell(rows.get(row), cols.get(col)), df);
        }

        /** {@return the finished, immutable {@link FacetGrid}} */
        public FacetGrid<DF> build() {
            return new FacetGrid<>(rowVars, colVars, rows, cols, panels);
        }
    }

    /**
     * A grid cell identified by its row and column keys.
     *
     * @param row the row key
     * @param col the column key
     */
    public record Cell(List<Object> row, List<Object> col) {
        @Override
        public String toString() {
            return row + " x " + col;
        }
    }
}
