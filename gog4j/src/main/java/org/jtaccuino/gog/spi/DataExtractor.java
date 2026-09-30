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
package org.jtaccuino.gog.spi;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.facet.FacetGrid;
import org.jtaccuino.gog.stat.StatData;

/**
 * Service provider interface decoupling <b>gog4j</b> plotting engine from concrete DataFrame implementations.
 * <p>
 * Allows {@link org.jtaccuino.gog.Plot} to operate seamlessly with any DataFrame library (such as DFLib, Tablesaw, or custom structures)
 * by defining methods to extract column values, total row count, min/max numeric range bounds, and facet partitioning.
 * <p>
 * Implementations are discovered through {@link java.util.ServiceLoader}; each implementation must declare
 * which DataFrame type it supports via {@link #supports(Class)} and provide a public no-argument constructor.
 * The first registered implementation that supports a given DataFrame type is selected by
 * {@link DataExtractorRegistry#extractorFor(Class)}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public interface DataExtractor<DF> {

    /**
     * Whether this extractor can operate on the given DataFrame type.
     * <p>
     * The argument is the runtime class of the DataFrame instance; implementations should accept
     * their own type and its subclasses, e.g. {@code DataFrame.class.isAssignableFrom(type)}.
     *
     * @param dataFrameType the runtime DataFrame class
     * @return {@code true} if this extractor handles the type
     */
    boolean supports(Class<?> dataFrameType);

    /**
     * Extracts values for a specified column from the DataFrame.
     *
     * @param df the DataFrame object
     * @param columnName the name of the target column
     * @return a list of values contained in the column
     */
    List<?> getColumn(DF df, String columnName);

    /**
     * The category of values held by a column, centralizing the discrimination
     * between continuous numeric, temporal, and discrete/categorical data.
     * <p>
     * Each constant knows the schema type names it maps to, so the assignment
     * of a schema type to a {@link ColumnType} lives in exactly one place.
     */
    enum ColumnType {
        /** Numeric values ({@link Number} and the listed numeric primitives). */
        NUMBER("int", "long", "double", "float"),
        /** Temporal values, e.g. {@link LocalDate}. */
        DATE("LocalDate"),
        /** Anything else, e.g. categories, booleans, free text. */
        TEXT();

        @SuppressWarnings("ImmutableEnumChecker") // the Set.of(...) result is immutable, but Set isn't proven so
        private final Set<String> schemaNames;

        ColumnType(String... schemaNames) {
            this.schemaNames = Set.of(schemaNames);
        }

        /**
         * Maps a schema type to its {@link ColumnType}.
         *
         * @param type the inferred schema type, may be {@code null} (e.g. for an
         *             empty column); or a boxed {@link Number} subclass not listed
         *             by name
         * @return the matching {@link ColumnType}, or {@code null} if {@code type} is {@code null}
         */
        public static ColumnType of(Class<?> type) {
            if (type == null) return null;
            return Arrays.stream(values())
                    .filter(ct -> ct.schemaNames.contains(type.getSimpleName()))
                    .findFirst()
                    .orElse(Number.class.isAssignableFrom(type) ? NUMBER : TEXT);
        }
    }

    /**
     * Determines the {@link ColumnType} of the values in a column.
     * <p>
     * The default implementation inspects the first non-null value in the column:
     * {@link Number} yields {@link ColumnType#NUMBER}, {@link LocalDate} yields
     * {@link ColumnType#DATE}, and anything else yields {@link ColumnType#TEXT}.
     * Implementations backed by type-aware DataFrames may override this to use the
     * schema-provided type instead of sampling a value.
     *
     * @param df the DataFrame object
     * @param columnName the name of the target column
     * @return the {@link ColumnType} of the column's values
     */
    default ColumnType columnType(DF df, String columnName) {
        for (var v : getColumn(df, columnName)) {
            if (v == null) continue;
            if (v instanceof LocalDate) return ColumnType.DATE;
            if (v instanceof Number) return ColumnType.NUMBER;
            if (v instanceof String s) {
                try {
                    Double.parseDouble(s);
                    return ColumnType.NUMBER;
                } catch (NumberFormatException e) {
                    return ColumnType.TEXT;
                }
            }
            return ColumnType.TEXT;
        }
        return ColumnType.TEXT;
    }

    /**
     * Returns the total row count of the DataFrame.
     *
     * @param df the DataFrame object
     * @return number of rows
     */
    int getRowCount(DF df);

    /**
     * Computes or retrieves the minimum and maximum numeric bounds for a specified column.
     *
     * @param df the DataFrame object
     * @param columnName the target column name
     * @return the {@link MinMax} range of the column
     */
    default MinMax getMinMax(DF df, String columnName) {
        List<?> col = getColumn(df, columnName);
        if (col == null || col.isEmpty()) return MinMax.UNIT;
        double min = col.stream().mapToDouble(v -> Values.toDouble(v)).min().orElse(0.0);
        double max = col.stream().mapToDouble(v -> Values.toDouble(v)).max().orElse(1.0);
        return new MinMax(min, max);
    }

    /**
     * Partitions the DataFrame by unique values of a facet column.
     * <p>
     * If {@code facetColumn} is {@code null}, returns a single partition keyed by {@code "GLOBAL"}.
     *
     * @param df the full DataFrame
     * @param facetColumn the column name to partition by, or {@code null} for a single partition
     * @return a map from partition key to sub-DataFrame
     */
    default Map<Object, DF> partition(DF df, String facetColumn) {
        if (facetColumn == null) {
            return Map.of("GLOBAL", df);
        }
        var partitions = new LinkedHashMap<Object, DF>();
        partitions.put("GLOBAL", df);
        return partitions;
    }

    /**
     * Partitions the DataFrame into the cells of a two-dimensional facet grid
     * spanned by row and column variables. The returned {@link FacetGrid}
     * carries the ordered row and column keys plus, for every combination, the
     * matching sub-DataFrame or {@code null} for an empty panel.
     * <p>
     * Implementations that cannot honour the full grid contract may fall back to
     * {@link #partition(DF, String)} per variable and build a coarse grid.
     *
     * @param df the full DataFrame
     * @param rowVars the row faceting variables, empty for no row dimension
     * @param colVars the column faceting variables, empty for no column dimension
     * @return the two-dimensional partition, never {@code null}
     */
    default FacetGrid<DF> partitionGrid(DF df, List<String> rowVars, List<String> colVars) {
        return partitionGrid(df, rowVars, colVars, Set.of());
    }

    /**
     * Partitions the DataFrame into the cells of a two-dimensional facet grid
     * spanned by row and column variables, adding {@link FacetGrid#ALL margin}
     * levels to the dimensions that mention any of {@code marginVars}. The
     * returned {@link FacetGrid} carries the ordered row and column keys plus,
     * for every combination, the matching sub-DataFrame or {@code null} for an
     * empty panel.
     * <p>
     * Implementations that cannot honour the full grid contract may fall back to
     * {@link #partition(DF, String)} per variable and build a coarse grid.
     *
     * @param df the full DataFrame
     * @param rowVars the row faceting variables, empty for no row dimension
     * @param colVars the column faceting variables, empty for no column dimension
     * @param marginVars the variables that carry margin facets
     * @return the two-dimensional partition, never {@code null}
     */
    default FacetGrid<DF> partitionGrid(DF df, List<String> rowVars, List<String> colVars,
                                        Set<String> marginVars) {
        return partitionGrid(df, rowVars, colVars);
    }

    /**
     * Reconstructs a DataFrame of this library's type from the output columns of
     * a statistical transformation. This is the write-side counterpart to
     * {@link #getColumn(DF, String)}: it lets a stat's agnostic {@code StatData}
     * be turned back into a renderable frame without the caller knowing the
     * concrete DataFrame implementation.
     * <p>
     * The default implementation rejects the operation; concrete providers (such
     * as the DFLib one) override it to build a frame whose columns map to the
     * {@link StatData#columnNames() output columns}.
     *
     * @param statData the stat output to turn into a frame
     * @return a DataFrame of the provider's concrete type
     * @throws UnsupportedOperationException when the provider cannot reconstruct a frame
     */
    default DF fromStatData(StatData statData) {
        throw new UnsupportedOperationException(DataExtractor.class.getSimpleName()
                + " does not reconstruct frames from stat data: " + getClass().getName());
    }
}
