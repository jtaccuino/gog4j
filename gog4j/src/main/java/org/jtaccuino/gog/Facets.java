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

import java.util.List;
import org.jtaccuino.gog.facet.FacetSpec;
import org.jtaccuino.gog.facet.GridOptions;

/**
 * Factory for creating facet specifications used to split a plot into
 * sub-panels: {@link #wrap(String, int)} for small multiples of one variable
 * and {@link #grid} for a matrix of panels spanned by row and column variables.
 */
public class Facets {

    /** Utility class; not meant to be instantiated. */
    private Facets() {}

    /**
     * Creates a wrap facet specification for a grid-based layout, the analogue
     * of the {@code Facets.wrap(~col)}.
     *
     * @param columnName the categorical column to split by
     * @param cols the maximum number of columns in the grid (-1 for auto-square)
     * @return a new {@link FacetSpec} for the given column and grid configuration
     */
    public static FacetSpec wrap(String columnName, int cols) {
        return new FacetSpec(columnName, cols);
    }

    /**
     * Creates a grid facet specification — the analogue of the
     * {@code Facets.grid(rows, cols)}.
     *
     * @param rowVar the categorical variable defining the rows, or {@code null}
     * @param colVar the categorical variable defining the columns, or {@code null}
     * @return a new grid {@link FacetSpec} with default {@link GridOptions}
     */
    public static FacetSpec grid(String rowVar, String colVar) {
        return grid(rowVar, colVar, GridOptions.defaults());
    }

    /**
     * Creates a grid facet specification with explicit options.
     *
     * @param rowVar  the row variable, or {@code null}
     * @param colVar  the column variable, or {@code null}
     * @param options the grid behaviour options
     * @return a new grid {@link FacetSpec}
     */
    public static FacetSpec grid(String rowVar, String colVar, GridOptions options) {
        return FacetSpec.grid(listOf(rowVar), listOf(colVar), options);
    }

    /**
     * Creates a grid facet specification over multiple variables per dimension,
     * the analogue of {@code Facets.grid(vs + am ~ gear)}.
     *
     * @param rowVars the row variables (may be empty)
     * @param colVars the column variables (may be empty)
     * @param options the grid behaviour options
     * @return a new grid {@link FacetSpec}
     */
    public static FacetSpec grid(List<String> rowVars, List<String> colVars, GridOptions options) {
        return FacetSpec.grid(rowVars, colVars, options);
    }

    private static List<String> listOf(String value) {
        return value == null ? List.of() : List.of(value);
    }
}
