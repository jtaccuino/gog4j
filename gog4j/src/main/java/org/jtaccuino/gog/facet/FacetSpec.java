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

import java.util.List;

/**
 * Specification for multi-panel faceted layouts.
 * <p>
 * Two modes are supported:
 * <ul>
 *   <li>{@link Mode#WRAP} — a single variable wrapped into a grid of small
 *       multiples ({@code Facets.wrap(~col)}), the classic gog4j behavior.</li>
 *   <li>{@link Mode#GRID} — a matrix of panels defined by row and column
 *       faceting variables ({@code Facets.grid(rows, cols)}), with per-dimension
 *       strips and the {@link GridOptions} knobs for scales, space, margins,
 *       strip placement, and axis labels.</li>
 * </ul>
 */
public class FacetSpec {

    /** The two facet layout families. */
    public enum Mode {
        /** One variable wrapped into a grid (facet wrap). */
        WRAP,
        /** A matrix of panels spanned by row and column variables (facet grid). */
        GRID
    }

    private final Mode mode;
    private final String columnName;
    private final int cols;
    private final List<String> rowVars;
    private final List<String> colVars;
    private final GridOptions options;

    /**
     * Constructs a wrap facet specification.
     *
     * @param columnName the categorical column used to partition the data
     * @param cols       the number of grid columns (-1 for automatic square layout)
     */
    public FacetSpec(String columnName, int cols) {
        this.mode = Mode.WRAP;
        this.columnName = columnName;
        this.cols = cols;
        this.rowVars = List.of(columnName);
        this.colVars = List.of();
        this.options = GridOptions.defaults();
    }

    private FacetSpec(List<String> rowVars, List<String> colVars, GridOptions options) {
        this.mode = Mode.GRID;
        this.columnName = null;
        this.cols = -1;
        this.rowVars = List.copyOf(rowVars);
        this.colVars = List.copyOf(colVars);
        this.options = options;
    }

    /** {@return the facet layout mode} */
    public Mode getMode() {
        return mode;
    }

    /** {@return whether this is a grid facet specification} */
    public boolean isGrid() {
        return mode == Mode.GRID;
    }

    /**
     * Returns the single faceting column name.
     *
     * @return the column name for a wrap facet; for a grid facet, the single
     *         variable when only one dimension is used (for layer matching),
     *         otherwise {@code null}
     */
    public String getColumnName() {
        if (mode == Mode.WRAP) {
            return columnName;
        }
        if (rowVars.size() == 1 && colVars.isEmpty()) {
            return rowVars.get(0);
        }
        if (colVars.size() == 1 && rowVars.isEmpty()) {
            return colVars.get(0);
        }
        return null;
    }

    /**
     * Returns the maximum number of columns in a wrap layout.
     *
     * @return column count for a wrap facet, or -1 for a grid facet
     */
    public int getCols() {
        return cols;
    }

    /** {@return the row faceting variables (grid only, empty for wrap)} */
    public List<String> getRowVars() {
        return rowVars;
    }

    /** {@return the column faceting variables (grid only, empty for wrap)} */
    public List<String> getColVars() {
        return colVars;
    }

    /** {@return the grid options (grid only)} */
    public GridOptions getOptions() {
        return options;
    }

    /**
     * Builds a grid facet specification over the given row and column
     * variables.
     *
     * @param rowVars the row faceting variables, empty for no row dimension
     * @param colVars the column faceting variables, empty for no column dimension
     * @param options the grid behaviour options
     * @return a new grid {@link FacetSpec}
     */
    public static FacetSpec grid(List<String> rowVars, List<String> colVars, GridOptions options) {
        return new FacetSpec(rowVars, colVars, options);
    }

    @Override
    public String toString() {
        if (mode == Mode.WRAP) {
            return "FacetSpec[wrap " + columnName + ", cols=" + cols + "]";
        }
        return "FacetSpec[grid rows=" + rowVars + ", cols=" + colVars + ", " + options + "]";
    }
}
