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
package org.jtaccuino.gog.hardwood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.facet.FacetGrid;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.StatData;

/**
 * The {@link DataExtractor} provider for {@link HardwoodDataFrame}s — frames backed
 * by Apache Parquet files read with Hardwood.
 *
 * <p>It mirrors {@code DflibDataExtractor}'s behaviour so plots render
 * identically regardless of the backing frame: schema-driven column typing,
 * numeric-aware facet ordering, nulls-last sorting, and full two-dimensional
 * grid partitioning with {@link FacetGrid#ALL margin} levels. It is registered
 * as a {@link java.util.ServiceLoader} provider for
 * {@code org.jtaccuino.gog.spi.DataExtractor} and is the single Hardwood
 * implementation.
 */
public class HardwoodDataExtractor implements DataExtractor<HardwoodDataFrame> {

    /**
     * Creates a stateless extractor, normally loaded through the
     * {@linkplain java.util.ServiceLoader ServiceLoader} provider registration.
     */
    public HardwoodDataExtractor() {
    }

    @Override
    public boolean supports(Class<?> dataFrameType) {
        return HardwoodDataFrame.class.isAssignableFrom(dataFrameType);
    }

    @Override
    public List<?> getColumn(HardwoodDataFrame df, String columnName) {
        if (!df.hasColumn(columnName)) {
            return List.of();
        }
        return df.column(columnName);
    }

    @Override
    public int getRowCount(HardwoodDataFrame df) {
        return df.rowCount();
    }

    @Override
    public ColumnType columnType(HardwoodDataFrame df, String columnName) {
        return df.kind(columnName).columnType();
    }

    @Override
    public MinMax getMinMax(HardwoodDataFrame df, String columnName) {
        return defaultMinMax(df, columnName);
    }

    @Override
    public Map<Object, HardwoodDataFrame> partition(HardwoodDataFrame df, String facetColumn) {
        if (facetColumn == null) {
            return Map.of("GLOBAL", df);
        }
        var facetValues = df.column(facetColumn);
        var partitions = new LinkedHashMap<Object, HardwoodDataFrame>();
        for (var val : new LinkedHashSet<>(facetValues)) {
            partitions.put(val, select(df, List.of(facetColumn), List.of(val),
                    List.of(), List.of()));
        }
        return partitions;
    }

    /**
     * Partitions the frame into the cells of a two-dimensional facet grid. The
     * row and column keys are the distinct value-combinations of each dimension
     * in natural (numeric-aware) order; when a variable is listed in
     * {@code marginVars}, a {@link FacetGrid#ALL margin} level is added to its
     * dimension.
     *
     * @param df         the full frame
     * @param rowVars    the row faceting variables
     * @param colVars    the column faceting variables
     * @param marginVars the variables that carry margin facets (empty for none)
     * @return the two-dimensional partition
     */
    @Override
    public FacetGrid<HardwoodDataFrame> partitionGrid(HardwoodDataFrame df, List<String> rowVars,
                                                  List<String> colVars, Set<String> marginVars) {
        FacetGrid.Builder<HardwoodDataFrame> builder = FacetGrid.builder(rowVars, colVars);
        var rowKeys = combinations(df, rowVars, marginVars);
        var colKeys = combinations(df, colVars, marginVars);
        for (var rowKey : rowKeys) {
            for (var colKey : colKeys) {
                var cell = select(df, rowVars, rowKey, colVars, colKey);
                builder.put(builder.row(rowKey), builder.col(colKey), cell);
            }
        }
        return builder.build();
    }

    @Override
    public HardwoodDataFrame fromStatData(StatData statData) {
        var names = statData.columnNames();
        var columns = new LinkedHashMap<String, List<?>>();
        for (var name : names) {
            columns.put(name, statData.column(name));
        }
        return HardwoodDataFrame.ofColumns(columns);
    }

    /**
     * Enumerates the distinct component combinations of the given variables —
     * one key per panel row or column, ordered by the natural (numeric-aware)
     * ordering of each component. When any variable is listed in
     * {@code marginVars}, an all-margin key is appended.
     */
    private static List<List<Object>> combinations(HardwoodDataFrame df, List<String> vars,
                                                   Set<String> marginVars) {
        if (vars.isEmpty()) {
            return List.of(List.of());
        }
        var columns = vars.stream().map(df::column).toList();
        var seen = IntStream.range(0, df.rowCount())
                .mapToObj(i -> {
                    List<Object> key = new ArrayList<>(columns.size());
                    for (var column : columns) {
                        key.add(column.get(i));
                    }
                    return key;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var keys = seen.stream().sorted(HardwoodDataExtractor::compareKeys)
                .collect(Collectors.toCollection(ArrayList::new));
        if (vars.stream().anyMatch(marginVars::contains)) {
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

    /**
     * Selects the rows of one grid cell into a new in-memory frame: every
     * variable is matched to its key component unless that component is the
     * {@link FacetGrid#ALL} margin marker, which matches every value.
     *
     * @param df      the full frame
     * @param rowVars the row variables
     * @param rowKey  the row key, one component per row variable
     * @param colVars the column variables
     * @param colKey  the column key, one component per column variable
     * @return the matching sub-frame; the whole frame for a grand-margin cell
     */
    private static HardwoodDataFrame select(HardwoodDataFrame df, List<String> rowVars, List<Object> rowKey,
                                        List<String> colVars, List<Object> colKey) {
        var vars = new ArrayList<String>(rowVars.size() + colVars.size());
        var keys = new ArrayList<Object>(rowKey.size() + colKey.size());
        collect(vars, keys, rowVars, rowKey);
        collect(vars, keys, colVars, colKey);
        if (vars.isEmpty()) {
            return df;
        }

        // Materialize the columns once and compute the matching row indices,
        // then slice every column so the cell stays a real frame. Facet panels
        // are usually small, but a frame of a handful of columns is cheap to
        // copy and keeps the extractor free of DFLib-style filter expressions.
        var filterColumns = vars.stream().map(df::column).toList();
        var rows = new ArrayList<Integer>();
        for (int i = 0, n = df.rowCount(); i < n; i++) {
            boolean matches = true;
            for (int v = 0; v < vars.size(); v++) {
                var key = keys.get(v);
                if (FacetGrid.ALL.equals(key)) {
                    continue;
                }
                var value = filterColumns.get(v).get(i);
                if (!componentsEqual(key, value)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                rows.add(i);
            }
        }
        return slice(df, rows);
    }

    private static void collect(List<String> vars, List<Object> keys,
                                List<String> sourceVars, List<Object> sourceKey) {
        for (int i = 0; i < sourceVars.size(); i++) {
            vars.add(sourceVars.get(i));
            keys.add(sourceKey.get(i));
        }
    }

    private static boolean componentsEqual(Object key, Object value) {
        if (key == null || value == null) {
            return Objects.equals(key, value);
        }
        if (key instanceof Number k && value instanceof Number v) {
            return Double.compare(k.doubleValue(), v.doubleValue()) == 0;
        }
        return key.equals(value);
    }

    /** Slices every column of the frame to the given row indices. */
    private static HardwoodDataFrame slice(HardwoodDataFrame df, List<Integer> rows) {
        var columns = new LinkedHashMap<String, List<?>>();
        for (var name : df.columnNames()) {
            var source = df.column(name);
            var sliced = new ArrayList<>(rows.size());
            for (var row : rows) {
                sliced.add(source.get(row));
            }
            columns.put(name, sliced);
        }
        return HardwoodDataFrame.ofColumns(columns);
    }

    /**
     * Default min/max computation mirroring the DFLib backend:
     * <ul>
     *   <li>Missing columns → {@code [0, 100]}</li>
     *   <li>Empty columns → {@code [0, 100]}</li>
     *   <li>All-null or fully categorical columns → {@code [0, 10]}</li>
     *   <li>Numeric or numeric-string columns → actual min/max of the raw values</li>
     * </ul>
     */
    static MinMax defaultMinMax(HardwoodDataFrame df, String columnName) {
        if (!df.hasColumn(columnName)) {
            return new MinMax(0.0, 100.0);
        }
        var column = df.column(columnName);
        if (column.isEmpty()) {
            return new MinMax(0.0, 100.0);
        }
        var min = Double.MAX_VALUE;
        var max = -Double.MAX_VALUE;
        var hasData = false;
        for (var v : column) {
            if (v == null) {
                continue;
            }
            double d = Values.toDouble(v, Double.NaN);
            if (Double.isNaN(d)) {
                continue; // skip categorical strings
            }
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
            hasData = true;
        }
        return hasData ? new MinMax(min, max) : new MinMax(0.0, 10.0);
    }
}
