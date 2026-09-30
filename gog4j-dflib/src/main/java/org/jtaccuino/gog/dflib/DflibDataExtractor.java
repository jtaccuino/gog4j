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
package org.jtaccuino.gog.dflib;

import static org.dflib.Exp.$str;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.dflib.Condition;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.facet.FacetGrid;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.StatData;

/**
 * The {@link DataExtractor} provider for DFLib {@link DataFrame}s.
 * <p>
 * Handles numeric, date, and string columns. It is registered as a
 * {@link java.util.ServiceLoader} provider for
 * {@code org.jtaccuino.gog.spi.DataExtractor} and is the single DFLib implementation.
 */
public class DflibDataExtractor implements DataExtractor<DataFrame> {

    /**
     * Creates a stateless extractor, normally loaded through the
     * {@linkplain java.util.ServiceLoader ServiceLoader} provider registration.
     */
    public DflibDataExtractor() {
    }

    @Override
    public boolean supports(Class<?> dataFrameType) {
        return DataFrame.class.isAssignableFrom(dataFrameType);
    }

    @Override
    public List<?> getColumn(DataFrame df, String columnName) {
        if (!df.getColumnsIndex().contains(columnName)) return List.of();
        return df.getColumn(columnName).toList();
    }

    @Override
    public int getRowCount(DataFrame df) {
        return df.height();
    }

    @Override
    public MinMax getMinMax(DataFrame df, String columnName) {
        return defaultMinMax(df, columnName);
    }

    @Override
    public ColumnType columnType(DataFrame df, String columnName) {
        return inferType(df, columnName);
    }

    @Override
    public Map<Object, DataFrame> partition(DataFrame df, String facetColumn) {
        if (facetColumn == null) {
            return Map.of("GLOBAL", df);
        }
        var uniqueValues = df.getColumn(facetColumn).unique();
        var partitions = new LinkedHashMap<Object, DataFrame>();
        for (var val : uniqueValues) {
            partitions.put(val, df.rows($str(facetColumn).eq(val)).select());
        }
        return partitions;
    }

    /**
     * Partitions the frame into the cells of a two-dimensional facet grid.
     * The row and column keys are the distinct value-combinations of each
     * dimension in natural (numeric-aware) order; when a variable is listed in
     * {@code marginVars}, a {@link FacetGrid#ALL margin} level is added to its
     * dimension.
     *
     * @param df         the full DataFrame
     * @param rowVars    the row faceting variables
     * @param colVars    the column faceting variables
     * @param marginVars the variables that carry margin facets (empty for none)
     * @return the two-dimensional partition
     */
    @Override
    public FacetGrid<DataFrame> partitionGrid(DataFrame df, List<String> rowVars, List<String> colVars,
                                              Set<String> marginVars) {
        FacetGrid.Builder<DataFrame> builder = FacetGrid.builder(rowVars, colVars);
        var rowKeys = combinations(df, rowVars, marginVars);
        var colKeys = combinations(df, colVars, marginVars);
        for (var rowKey : rowKeys) {
            for (var colKey : colKeys) {
                var cell = selectCell(df, rowVars, rowKey, colVars, colKey);
                builder.put(builder.row(rowKey), builder.col(colKey), cell);
            }
        }
        return builder.build();
    }

    /**
     * Enumerates the distinct component combinations of the given variables —
     * one key per panel row or column, ordered by the natural (numeric-aware)
     * ordering of each component just as conventional discrete scales do. When
     * any variable is listed in {@code marginVars}, an all-margin key is
     * appended.
     *
     * @param df         the full DataFrame
     * @param vars       the variables of one dimension
     * @param marginVars the variables that carry margin facets
     * @return the ordered keys, {@code [List.of()]} when there are no variables
     */
    private static List<List<Object>> combinations(DataFrame df, List<String> vars, Set<String> marginVars) {
        if (vars.isEmpty()) {
            return List.of(List.of());
        }
        var columns = vars.stream().map(df::getColumn).toList();
        var seen = IntStream.range(0, df.height())
                .mapToObj(i -> columns.stream().map(column -> column.get(i)).toList())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var keys = seen.stream().sorted(DflibDataExtractor::compareKeys)
                .collect(Collectors.toCollection(ArrayList::new));
        if (isMargined(vars, marginVars)) {
            // A margin dimension also gains one key per original combination
            // with each margined variable flipped to the ALL sentinel (e.g.
            // vs+am rows gain (vs, ALL) and (ALL, am)), plus the grand key.
            var marginKeys = new LinkedHashSet<List<Object>>();
            IntStream.range(0, vars.size())
                    .filter(varIdx -> marginVars.contains(vars.get(varIdx)))
                    .forEach(varIdx -> seen.forEach(key -> {
                        var variant = new ArrayList<Object>(key);
                        variant.set(varIdx, FacetGrid.ALL);
                        marginKeys.add(variant);
                    }));
            marginKeys.add(Collections.nCopies(vars.size(), FacetGrid.ALL));
            keys.addAll(marginKeys);
        }
        return keys;
    }

    /**
     * Lexicographically compares two keys component by component. Numbers
     * compare numerically; other values with the same runtime type use their
     * natural order; mixed types fall back to their string forms. Nulls sort
     * last.
     */
    private static int compareKeys(List<Object> a, List<Object> b) {
        int n = Math.min(a.size(), b.size());
        for (var i = 0; i < n; i++) {
            int c = compareComponents(a.get(i), b.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a.size(), b.size());
    }

    @SuppressWarnings("unchecked")
    private static int compareComponents(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable<?> && a.getClass() == b.getClass()) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /** {@return whether any of the given dimension's variables carries a margin} */
    private static boolean isMargined(List<String> vars, Set<String> marginVars) {
        return vars.stream().anyMatch(marginVars::contains);
    }

    /**
     * Filters the frame to the rows of one grid cell: every row variable is
     * matched to its key component unless that component is the {@link
     * FacetGrid#ALL} margin marker, which matches every value.
     *
     * @param df      the full DataFrame
     * @param rowVars the row variables
     * @param rowKey  the row key, one component per row variable
     * @param colVars the column variables
     * @param colKey  the column key, one component per column variable
     * @return the matching sub-frame; the whole frame for a grand-margin cell
     */
    private static DataFrame selectCell(DataFrame df, List<String> rowVars, List<Object> rowKey,
                                        List<String> colVars, List<Object> colKey) {
        Condition cond = null;
        for (var i = 0; i < rowVars.size(); i++) {
            if (FacetGrid.ALL.equals(rowKey.get(i))) {
                continue;
            }
            cond = and(cond, $str(rowVars.get(i)).eq(rowKey.get(i)));
        }
        for (var i = 0; i < colVars.size(); i++) {
            if (FacetGrid.ALL.equals(colKey.get(i))) {
                continue;
            }
            cond = and(cond, $str(colVars.get(i)).eq(colKey.get(i)));
        }
        if (cond == null) {
            return df;
        }
        return df.rows(cond).select();
    }

    private static Condition and(Condition c1, Condition c2) {
        if (c1 == null) {
            return c2;
        }
        return c1.and(c2);
    }

    /** Shared schema-driven type inference for this extractor and its default min/max. */
    private static ColumnType inferType(DataFrame df, String columnName) {
        var t = ColumnType.of(df.getColumn(columnName).getInferredType());
        return t == null ? ColumnType.TEXT : t;
    }

    /**
     * Default min/max computation that handles:
     * <ul>
     *   <li>Missing columns → {@code [0, 100]}</li>
     *   <li>Empty columns → {@code [0, 100]}</li>
     *   <li>All-null or fully categorical columns → {@code [0, 10]}</li>
     *   <li>{@link java.time.LocalDate} columns → epoch-day range</li>
     *   <li>Numeric or numeric-string columns → actual min/max of the raw values</li>
     * </ul>
     */
    static MinMax defaultMinMax(DataFrame df, String columnName) {
        if (!df.getColumnsIndex().contains(columnName)) return new MinMax(0.0, 100.0);
        var series = df.getColumn(columnName);
        if (series.size() == 0) return new MinMax(0.0, 100.0);

        var min = Double.MAX_VALUE;
        var max = -Double.MAX_VALUE;
        var hasData = false;
        for (var i = 0; i < series.size(); i++) {
            var v = series.get(i);
            if (v == null) continue;
            double d = Values.toDouble(v, Double.NaN);
            if (Double.isNaN(d)) continue; // skip categorical strings
            if (d < min) min = d;
            if (d > max) max = d;
            hasData = true;
        }
        if (!hasData) return new MinMax(0.0, 10.0);
        return new MinMax(min, max);
    }

    @Override
    public DataFrame fromStatData(StatData statData) {
        var names = statData.columnNames();
        var series = names.stream()
                .map(name -> Series.of(statData.column(name).toArray()))
                .toArray(Series[]::new);
        return DataFrame.byColumn(names.toArray(String[]::new)).of(series);
    }
}
