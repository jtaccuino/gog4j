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

import dev.hardwood.InputFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A tabular frame backed by an Apache Parquet file, read with Hardwood.
 *
 * <p>Hardwood has no frame type of its own: it exposes a file schema plus
 * forward-only row/column cursors. This class is the handle the plotting engine
 * carries, in the spirit of the other backends' DataFrame: it names the Parquet
 * source, materializes columns lazily and caches them, and can also be built in
 * memory so a statistical transformation's output can be rendered without a
 * round-trip through disk.
 *
 * <p>A Parquet-backed frame comes from a file on disk ({@link #of}) or from a
 * classpath resource ({@link #ofResource}); an in-memory frame from
 * {@link #ofColumns}.
 */
public final class HardwoodDataFrame {

    /**
     * How a classpath Parquet resource is materialized before Hardwood opens it.
     * Only consulted by {@link #ofResource}: a frame created from a {@link Path}
     * is read straight from disk.
     */
    public enum Cache {
        /**
         * Read the resource into a heap buffer. No filesystem writes, but the
         * compressed bytes stay on the heap for as long as the frame lives.
         */
        IN_MEMORY,
        /**
         * Extract the resource to a temporary file, removed at JVM exit. Reads
         * are then memory-mapped and off-heap, which suits a large resource.
         */
        TEMP_FILE
    }

    // A Parquet-backed frame holds exactly one source: a file (a real file, or a
    // resource extracted for TEMP_FILE) or an in-memory buffer (IN_MEMORY).
    private final Path path;
    private final ByteBuffer buffer;
    private final Map<String, List<?>> memoryColumns;
    private final List<String> memoryColumnNames;

    // Parquet-backed state, resolved on first access. Hardwood's reader opens
    // and then closes the InputFiles it is given, so a fresh InputFile is built
    // per read; after the first read the schema and columns are cached.
    private volatile HardwoodSchema schema;
    private final Map<String, List<?>> columnCache = new LinkedHashMap<>();

    private HardwoodDataFrame(Path path, ByteBuffer buffer,
                              Map<String, List<?>> memoryColumns, List<String> memoryColumnNames) {
        this.path = path;
        this.buffer = buffer;
        this.memoryColumns = Map.copyOf(memoryColumns);
        this.memoryColumnNames = List.copyOf(memoryColumnNames);
    }

    /**
     * Opens a single Parquet file as a frame.
     *
     * @param path the Parquet file
     * @return a file-backed frame over that file
     */
    public static HardwoodDataFrame of(Path path) {
        Objects.requireNonNull(path, "path");
        return new HardwoodDataFrame(path, null, Map.of(), List.of());
    }

    /**
     * Opens a classpath Parquet resource as a frame, materializing it in memory.
     *
     * @param resource the absolute classpath resource path
     * @return a frame over that resource
     */
    public static HardwoodDataFrame ofResource(String resource) {
        return ofResource(resource, Cache.IN_MEMORY);
    }

    /**
     * Opens a classpath Parquet resource as a frame, materializing it according
     * to {@code cache}. An exploded (directory) build resolves the resource to a
     * real file and reads it straight from disk, ignoring {@code cache}.
     *
     * @param resource the absolute classpath resource path
     * @param cache    how to materialize a resource that lives inside a jar
     * @return a frame over that resource
     */
    public static HardwoodDataFrame ofResource(String resource, Cache cache) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(cache, "cache");
        URL url = HardwoodDataFrame.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("missing dataset resource " + resource);
        }
        if ("file".equals(url.getProtocol())) {
            try {
                return of(Path.of(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException("invalid dataset resource URL " + url, e);
            }
        }
        try {
            if (cache == Cache.TEMP_FILE) {
                Path temp = Files.createTempFile("gog4j-hardwood-", ".parquet");
                temp.toFile().deleteOnExit();
                try (InputStream in = url.openStream()) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }
                return of(temp);
            }
            try (InputStream in = url.openStream()) {
                return new HardwoodDataFrame(null, ByteBuffer.wrap(in.readAllBytes()), Map.of(), List.of());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read dataset resource " + resource, e);
        }
    }

    /**
     * Builds an in-memory frame from named columns. Used by the write path when
     * a statistical transformation produces a frame that must be rendered.
     *
     * @param columns the columns, keyed by name, all of equal length
     * @return a memory-backed frame
     */
    public static HardwoodDataFrame ofColumns(Map<String, ? extends List<?>> columns) {
        Objects.requireNonNull(columns, "columns");
        var names = new ArrayList<>(columns.keySet());
        return new HardwoodDataFrame(null, null, new LinkedHashMap<>(columns), names);
    }

    private boolean isInMemory() {
        return !memoryColumns.isEmpty();
    }

    /**
     * A fresh {@link InputFile} over this frame's Parquet source. Hardwood's
     * reader opens and then closes the files it is handed, so a new one is
     * created for every read rather than shared across reads.
     *
     * @return a single-element list holding a new input file
     */
    private List<InputFile> inputFiles() {
        return List.of(path != null ? InputFile.of(path) : InputFile.of(buffer));
    }

    /**
     * The names of this table's columns, in order.
     *
     * @return the column names
     */
    public List<String> columnNames() {
        return isInMemory() ? memoryColumnNames : schema().columnNames();
    }

    /**
     * Whether the given column is present.
     *
     * @param columnName the column name
     * @return {@code true} when present
     */
    public boolean hasColumn(String columnName) {
        return isInMemory() ? memoryColumns.containsKey(columnName) : schema().indexOf(columnName) >= 0;
    }

    /**
     * The number of rows in the table.
     *
     * @return the row count
     */
    public int rowCount() {
        if (isInMemory()) {
            return memoryColumns.values().iterator().next().size();
        }
        return Math.toIntExact(schema().rowCount());
    }

    /**
     * The values of one column, materialized once and cached for a file-backed
     * table. Prefer {@link HardwoodDataExtractor#getColumn} from plotting code.
     *
     * @param columnName the column name
     * @return the column values, or an empty list when the column is absent
     */
    public List<?> column(String columnName) {
        if (isInMemory()) {
            return memoryColumns.getOrDefault(columnName, List.of());
        }
        if (!hasColumn(columnName)) {
            return List.of();
        }
        synchronized (columnCache) {
            var cached = columnCache.get(columnName);
            if (cached != null) {
                return cached;
            }
            var loaded = ParquetColumns.read(inputFiles(), schema(), columnName);
            columnCache.put(columnName, loaded);
            return loaded;
        }
    }

    /**
     * The category of a column, from the Parquet schema for a file-backed table
     * or sampled from the values for an in-memory one.
     *
     * @param columnName the column name
     * @return the column's {@link HardwoodSchema.ColumnKind}
     */
    HardwoodSchema.ColumnKind kind(String columnName) {
        return isInMemory()
                ? HardwoodSchema.kindOfValues(column(columnName))
                : schema().kind(columnName);
    }

    /**
     * The values of a column as {@code int}s, parsing strings when needed.
     *
     * @param columnName the column name
     * @return the column as an {@code int} array
     */
    public int[] intColumn(String columnName) {
        var values = column(columnName);
        var out = new int[values.size()];
        for (var i = 0; i < out.length; i++) {
            out[i] = toInt(values.get(i));
        }
        return out;
    }

    /**
     * The values of a column as {@code double}s, parsing strings when needed.
     *
     * @param columnName the column name
     * @return the column as a {@code double} array
     */
    public double[] doubleColumn(String columnName) {
        var values = column(columnName);
        var out = new double[values.size()];
        for (var i = 0; i < out.length; i++) {
            out[i] = toDouble(values.get(i));
        }
        return out;
    }

    /**
     * The values of a column as strings, mapping {@code null} to empty.
     *
     * @param columnName the column name
     * @return the column as a {@code String} array
     */
    public String[] stringColumn(String columnName) {
        var values = column(columnName);
        var out = new String[values.size()];
        for (var i = 0; i < out.length; i++) {
            var value = values.get(i);
            out[i] = value == null ? "" : value.toString();
        }
        return out;
    }

    /**
     * Slices every column to the given row indices, producing a new in-memory
     * table.
     *
     * @param rows the row indices to keep, in order
     * @return the sliced table
     */
    public HardwoodDataFrame rows(int[] rows) {
        var columns = new LinkedHashMap<String, List<?>>();
        for (var name : columnNames()) {
            var source = column(name);
            var sliced = new ArrayList<Object>(rows.length);
            for (var row : rows) {
                sliced.add(source.get(row));
            }
            columns.put(name, sliced);
        }
        return ofColumns(columns);
    }

    private static int toInt(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        return Integer.parseInt(value.toString().trim());
    }

    private static double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return Double.parseDouble(value.toString().trim());
    }

    private HardwoodSchema schema() {
        var resolved = schema;
        if (resolved == null) {
            synchronized (this) {
                resolved = schema;
                if (resolved == null) {
                    resolved = HardwoodSchema.read(inputFiles());
                    schema = resolved;
                }
            }
        }
        return resolved;
    }

    @Override
    public String toString() {
        if (isInMemory()) {
            return "HardwoodDataFrame[in-memory, columns=" + memoryColumnNames + ", rows=" + rowCount() + "]";
        }
        return "HardwoodDataFrame[" + (path != null ? path : "<memory>") + "]";
    }
}
