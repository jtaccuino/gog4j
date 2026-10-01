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
import dev.hardwood.reader.RowReader;
import dev.hardwood.schema.ColumnSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Materializes one column of a Parquet file into a {@link List}, decoding each
 * value to the Java type the engine understands: {@link Long}/{@link Double}
 * for numerics, {@link LocalDate}/{@link Instant} for temporals, {@link String}
 * for text, and {@code null} for missing values.
 *
 * <p>Hardwood's {@link RowReader} is a forward-only cursor driven by one
 * thread, so a reader is opened per column. A single row pass could fill every
 * column at once, but the engine asks for columns independently (and often only
 * a few), so per-column reads keep the lazy contract and stay simple.
 */
final class ParquetColumns {

    private ParquetColumns() {
    }

    /**
     * Reads one column's values from the given Parquet input file(s).
     *
     * @param files      the Parquet input files
     * @param schema     the resolved schema, used to size the result
     * @param columnName the column to read
     * @return the column values
     */
    static List<?> read(List<InputFile> files, HardwoodSchema schema, String columnName) {
        try (var reader = ParquetFileReader.openAll(files)) {
            ColumnSchema column = reader.getFileSchema().getColumn(columnName);
            var values = new ArrayList<Object>((int) schema.rowCount());
            try (RowReader rows = reader.rowReader()) {
                while (rows.hasNext()) {
                    rows.next();
                    values.add(decode(rows, column));
                }
            }
            return values;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read Parquet column '" + columnName + "' from "
                    + files.stream().map(InputFile::name).toList(), e);
        }
    }

    /**
     * Decodes the current row's value for one column, honouring the column's
     * physical and logical type.
     */
    private static Object decode(RowReader rows, ColumnSchema column) {
        var name = column.name();
        if (rows.isNull(name)) {
            return null;
        }
        var logical = column.logicalType();
        if (logical instanceof LogicalType.DateType) {
            return rows.getDate(name);
        }
        if (logical instanceof LogicalType.TimestampType timestamp) {
            return decodeTimestamp(rows, name, timestamp);
        }
        if (logical instanceof LogicalType.DecimalType) {
            BigDecimal decimal = rows.getDecimal(name);
            return decimal == null ? null : decimal.doubleValue();
        }
        if (logical instanceof LogicalType.UuidType) {
            return String.valueOf(rows.getUuid(name));
        }
        if (logical instanceof LogicalType.StringType
                || logical instanceof LogicalType.EnumType
                || logical instanceof LogicalType.JsonType) {
            return rows.getString(name);
        }

        PhysicalType type = column.type();
        return switch (type) {
            case BOOLEAN -> rows.getBoolean(name);
            case INT32 -> (long) rows.getInt(name);
            case INT64 -> rows.getLong(name);
            case FLOAT -> (double) rows.getFloat(name);
            case DOUBLE -> rows.getDouble(name);
            // INT96 has no zone semantics of its own, so it decodes as an
            // instant on Parquet's own convention of UTC nanoseconds.
            case INT96 -> rows.getTimestamp(name);
            case BYTE_ARRAY, FIXED_LEN_BYTE_ARRAY -> decodeBinary(rows.getBinary(name));
        };
    }

    /**
     * Decodes a timestamp according to its zone convention, which the Parquet
     * logical type states explicitly rather than leaving to be guessed.
     * <p>
     * {@code isAdjustedToUTC} means the stored count is an offset from
     * 1970-01-01T00:00:00Z, so the value is an {@link Instant}. When it is
     * false the count is a wall-clock reading with no zone, so the value is a
     * {@link LocalDateTime} — which {@code Temporals} then reads as UTC, the
     * same reading the writing side used.
     *
     * @param rows      the row reader positioned on the current row
     * @param name      the column name
     * @param timestamp the column's timestamp logical type
     * @return an {@link Instant} for a UTC-adjusted column, else a {@link LocalDateTime}
     */
    private static Object decodeTimestamp(RowReader rows, String name, LogicalType.TimestampType timestamp) {
        return timestamp.isAdjustedToUTC() ? rows.getTimestamp(name) : rows.getLocalTimestamp(name);
    }

    /**
     * Decodes a {@code BYTE_ARRAY} value with no logical type annotation as
     * UTF-8 text; the demo datasets are all string-typed at the logical level,
     * so this is only a defensive fallback.
     */
    private static String decodeBinary(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
