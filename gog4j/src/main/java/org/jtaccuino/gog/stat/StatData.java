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
package org.jtaccuino.gog.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a {@link Stat}: an immutable map of named column vectors, one
 * column per output aesthetic or computed value (e.g. {@code x}, {@code y},
 * {@code ymin}, {@code ymax}, {@code count}, {@code density}).
 * <p>
 * All columns share the same length — the number of output rows. A geometry
 * reads the vectors it needs by name and iterates them in lockstep.
 */
public final class StatData {

    private final Map<String, List<Object>> columns;

    private StatData(Map<String, List<Object>> columns) {
        this.columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
    }

    /**
     * Starts building a new {@link StatData}.
     *
     * @return a fresh {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return the output row count — the shared length of every column}
     */
    public int rowCount() {
        if (columns.isEmpty()) {
            return 0;
        }
        return columns.values().iterator().next().size();
    }

    /**
     * Whether this stat produced no output rows.
     *
     * @return {@code true} when the transformation yielded nothing
     */
    public boolean isEmpty() {
        return rowCount() == 0;
    }

    /**
     * Reads an output column by name, or {@code null} when absent.
     *
     * @param name the column name
     * @return the column vector, or {@code null}
     */
    public List<Object> column(String name) {
        return columns.get(name);
    }

    /**
     * Reads an output column as doubles, mapping missing entries to
     * {@link Double#NaN}. Returns an empty list when the column is absent.
     *
     * @param name the column name
     * @return the numeric column vector
     */
    public List<Double> columnAsDoubles(String name) {
        var col = columns.get(name);
        if (col == null) {
            return List.of();
        }
        var out = new ArrayList<Double>(col.size());
        for (var v : col) {
            out.add(v instanceof Number n ? n.doubleValue() : Double.NaN);
        }
        return out;
    }

    /**
     * {@return the names of all output columns}
     */
    public List<String> columnNames() {
        return new ArrayList<>(columns.keySet());
    }

    /**
     * Accumulates named column vectors for a {@link StatData}.
     */
    public static final class Builder {
        private final Map<String, List<Object>> columns = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Appends a value to the named column, creating it on first use.
         * All columns are expected to end up with the same length.
         *
         * @param name  the column name
         * @param value the value to append
         * @return this builder
         */
        public Builder add(String name, Object value) {
            columns.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Builds the immutable {@link StatData}.
         *
         * @return the finished result
         */
        public StatData build() {
            return new StatData(columns);
        }
    }
}
