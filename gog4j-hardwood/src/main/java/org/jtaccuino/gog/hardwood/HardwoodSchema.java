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
import dev.hardwood.metadata.LogicalType;
import dev.hardwood.metadata.PhysicalType;
import dev.hardwood.reader.ParquetFileReader;
import dev.hardwood.schema.ColumnSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The resolved schema of a Parquet file (or set of files): the column names, in
 * order, and their {@link ColumnKind}.
 *
 * <p>Read once per {@link HardwoodDataFrame} through {@link ParquetFileReader}'s
 * file metadata and schema, which do not require scanning any rows.
 */
final class HardwoodSchema {

    /** The engine-facing category of a column, mirroring {@code DataExtractor.ColumnType}. */
    enum ColumnKind {
        /** Numeric values. */
        NUMBER,
        /** Temporal values such as {@link LocalDate}. */
        DATE,
        /** Everything else: categories, booleans, free text. */
        TEXT
    }

    private final List<String> columnNames;
    private final List<ColumnKind> kinds;
    private final long rowCount;

    private HardwoodSchema(List<String> columnNames, List<ColumnKind> kinds, long rowCount) {
        this.columnNames = List.copyOf(columnNames);
        this.kinds = List.copyOf(kinds);
        this.rowCount = rowCount;
    }

    /**
     * Reads the schema of the given Parquet input file(s).
     *
     * @param files the Parquet input files
     * @return the resolved schema
     */
    static HardwoodSchema read(List<InputFile> files) {
        try (var reader = ParquetFileReader.openAll(files)) {
            var schema = reader.getFileSchema();
            var names = new ArrayList<String>();
            var kinds = new ArrayList<ColumnKind>();
            for (var i = 0; i < schema.getColumnCount(); i++) {
                ColumnSchema column = schema.getColumn(i);
                names.add(column.name());
                kinds.add(kindOf(column));
            }
            return new HardwoodSchema(names, kinds, reader.getFileMetaData().numRows());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read Parquet schema for "
                    + files.stream().map(InputFile::name).toList(), e);
        }
    }

    /**
     * Classifies a Parquet column from its physical and logical type.
     *
     * @param column the column schema
     * @return the column kind
     */
    static ColumnKind kindOf(ColumnSchema column) {
        var logical = column.logicalType();
        if (logical instanceof LogicalType.DateType || logical instanceof LogicalType.TimestampType) {
            return ColumnKind.DATE;
        }
        if (logical instanceof LogicalType.StringType || logical instanceof LogicalType.EnumType
                || logical instanceof LogicalType.UuidType || logical instanceof LogicalType.JsonType) {
            return ColumnKind.TEXT;
        }
        var physical = column.type();
        return switch (physical) {
            case INT32, INT64, FLOAT, DOUBLE -> ColumnKind.NUMBER;
            case BOOLEAN -> ColumnKind.TEXT;
            case INT96, BYTE_ARRAY, FIXED_LEN_BYTE_ARRAY -> ColumnKind.TEXT;
        };
    }

    /**
     * Classifies an in-memory column by sampling its first non-null value,
     * mirroring {@code DataExtractor.columnType}'s default.
     *
     * @param values the column values
     * @return the column kind
     */
    static ColumnKind kindOfValues(List<?> values) {
        for (var v : values) {
            if (v == null) {
                continue;
            }
            if (v instanceof LocalDate) {
                return ColumnKind.DATE;
            }
            if (v instanceof Number) {
                return ColumnKind.NUMBER;
            }
            if (v instanceof String s) {
                try {
                    Double.parseDouble(s);
                    return ColumnKind.NUMBER;
                } catch (NumberFormatException e) {
                    return ColumnKind.TEXT;
                }
            }
            return ColumnKind.TEXT;
        }
        return ColumnKind.TEXT;
    }

    /** {@return the column names, in order} */
    List<String> columnNames() {
        return columnNames;
    }

    /** {@return the row count, across all files} */
    long rowCount() {
        return rowCount;
    }

    /**
     * The index of a column, or {@code -1} when absent.
     *
     * @param columnName the column name
     * @return the zero-based column index
     */
    int indexOf(String columnName) {
        return columnNames.indexOf(columnName);
    }

    /**
     * The kind of a column, defaulting to {@link ColumnKind#TEXT} when absent.
     *
     * @param columnName the column name
     * @return the column kind
     */
    ColumnKind kind(String columnName) {
        int idx = indexOf(columnName);
        return idx < 0 ? ColumnKind.TEXT : kinds.get(idx);
    }

    /** {@return the physical type used only for diagnostics} */
    @Override
    public String toString() {
        return "HardwoodSchema" + columnNames;
    }
}
