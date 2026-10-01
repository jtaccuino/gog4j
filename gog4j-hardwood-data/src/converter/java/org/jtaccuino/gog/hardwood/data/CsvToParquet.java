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
package org.jtaccuino.gog.hardwood.data;

import dev.hardwood.OutputFile;
import dev.hardwood.metadata.CompressionCodec;
import dev.hardwood.metadata.LogicalType;
import dev.hardwood.metadata.PhysicalType;
import dev.hardwood.metadata.RepetitionType;
import dev.hardwood.schema.FileSchema;
import dev.hardwood.writer.ParquetFileWriter;
import dev.hardwood.writer.RowWriter;
import dev.hardwood.writer.StructBuilder;
import dev.hardwood.writer.WriterConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dflib.ByteSource;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.codec.Codec;
import org.dflib.csv.Csv;

/**
 * Build-time tool that converts the bundled demo CSV datasets into ZSTD
 * Parquet files with Hardwood's writer.
 *
 * <p>Invoked by the {@code :gog4j-hardwood-data:convertParquet} Gradle task; not
 * part of the shipped runtime. Each CSV is read with DFLib (handling the
 * gzipped GWAS file), its columns typed from the first non-null value, and
 * written row by row to {@code <outputDir>/examples/<name>/<name>.parquet}.
 *
 * <p>Usage: {@code CsvToParquet <outputDir> (<name> <sourceFile>)+}. The source
 * files are read from the classpath, where {@code :gog4j-data} provides them
 * under {@code /examples/...}.
 */
public final class CsvToParquet {

    /** Strict ISO-8601 calendar date, so a numeric column is never read as one. */
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private CsvToParquet() {
    }

    /**
     * Runs the conversion.
     *
     * @param args the output directory followed by name/source-file pairs
     * @throws IOException when a file cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length % 2 == 0) {
            throw new IllegalArgumentException(
                    "usage: CsvToParquet <outputDir> (<name> <sourceFile>)+");
        }
        var outputDir = Path.of(args[0]);
        var datasets = new LinkedHashMap<String, String>();
        for (int i = 1; i < args.length; i += 2) {
            datasets.put(args[i], args[i + 1]);
        }
        for (var entry : datasets.entrySet()) {
            convert(outputDir, entry.getKey(), entry.getValue());
        }
    }

    private static void convert(Path outputDir, String name, String sourceFile) throws IOException {
        var categoryDir = name.startsWith("gwas") ? "gwas" : name;
        var source = "/examples/" + categoryDir + "/" + sourceFile;
        var frame = readCsv(source);

        var target = outputDir.resolve("examples").resolve(categoryDir).resolve(name + ".parquet");
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);

        var types = columnTypes(frame);
        var schema = schemaFor(frame, types);
        var config = WriterConfig.builder().codec(CompressionCodec.ZSTD).build();
        try (var writer = ParquetFileWriter.create(OutputFile.of(target), schema, config)) {
            writer.createdBy("gog4j-hardwood-data CsvToParquet");
            RowWriter rows = writer.rowWriter();
            var names = columnNames(frame);
            for (int r = 0; r < frame.height(); r++) {
                final int row = r;
                rows.writeRow(builder -> fill(builder, names, types, frame, row));
            }
        }
        System.out.println("converted " + source + " -> " + target
                + " (" + frame.height() + " rows, " + frame.width() + " cols)");
    }

    private static DataFrame readCsv(String resource) {
        try (InputStream in = CsvToParquet.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing dataset resource " + resource);
            }
            var loader = Csv.loader();
            if (resource.endsWith(".gz")) {
                // Hand DFLib the raw compressed bytes and let its GZIP codec
                // decompress; decoding to text here would bypass the codec.
                loader = loader.compression(Codec.GZIP);
                return loader.load(ByteSource.of(in.readAllBytes()));
            }
            try (Reader reader = new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8))) {
                return loader.load(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot read " + resource, e);
        }
    }

    private static List<String> columnNames(DataFrame frame) {
        var index = frame.getColumnsIndex();
        var names = new ArrayList<String>(index.size());
        for (int i = 0; i < index.size(); i++) {
            names.add(index.get(i));
        }
        return names;
    }

    private static FileSchema schemaFor(DataFrame frame, Map<String, ColumnType> types) {
        var builder = FileSchema.builder("gog4j");
        for (var name : columnNames(frame)) {
            var type = types.get(name);
            builder.addColumn(name, type.physical(), RepetitionType.OPTIONAL, type.logical());
        }
        return builder.build();
    }

    /**
     * The column type decided for each column, aligned with the column order.
     * <p>
     * Classified once and then reused for both the schema and the row writing:
     * a per-row decision could disagree with the schema it was written against,
     * which Parquet only catches at read time.
     *
     * @param frame the loaded frame
     * @return the type of each column
     */
    private static Map<String, ColumnType> columnTypes(DataFrame frame) {
        var types = new LinkedHashMap<String, ColumnType>();
        for (var name : columnNames(frame)) {
            types.put(name, columnType(frame.getColumn(name)));
        }
        return types;
    }

    /**
     * The engine-facing shape of a converted column: the physical type to write
     * and the logical annotation that gives it meaning on the way back in.
     */
    private record ColumnType(PhysicalType physical, LogicalType logical) {

        /** {@code INT64} numbers, written as {@code long}. */
        static final ColumnType INTEGER = new ColumnType(PhysicalType.INT64, null);

        /** {@code DOUBLE} numbers, written as {@code double}. */
        static final ColumnType DOUBLE = new ColumnType(PhysicalType.DOUBLE, null);

        /** UTF-8 text, written as {@link String}. */
        static final ColumnType TEXT = new ColumnType(PhysicalType.BYTE_ARRAY, new LogicalType.StringType());

        /**
         * {@code INT32} epoch days with Parquet's {@code DATE} annotation,
         * written as a {@link LocalDate}. Parquet defines a date as days since
         * the epoch with no time of day, which is exactly a calendar date.
         */
        static final ColumnType DATE = new ColumnType(PhysicalType.INT32, new LogicalType.DateType());

        /**
         * {@code INT64} epoch milliseconds with a UTC-adjusted {@code TIMESTAMP}
         * annotation, written as an {@link Instant}.
         * <p>
         * The zone-less local date-times of the source files are read as UTC, the
         * same reading the rendering path applies, so the round trip is
         * lossless for them.
         */
        static final ColumnType TIMESTAMP = new ColumnType(PhysicalType.INT64,
                new LogicalType.TimestampType(true, LogicalType.TimeUnit.MILLIS));
    }

    /**
     * Classifies a column by scanning its values, in the order that keeps the
     * more specific readings first: a date or date-time column is also "not a
     * number", so asking about numbers before about calendars would misread it.
     *
     * <p>DFLib's plain CSV loader hands every value back as a {@link String}, so
     * each shape is confirmed against the whole column: a single value that does
     * not fit downgrades the column to text, since a partially-typed column is
     * not something the reader can recover.
     */
    private static ColumnType columnType(Series<?> series) {
        var sawValue = false;
        var sawFraction = false;
        for (var i = 0; i < series.size(); i++) {
            Object value = series.get(i);
            if (isMissing(value)) {
                continue;
            }
            sawValue = true;
            if (value instanceof LocalDate || value instanceof LocalDateTime
                    || value instanceof Instant || value instanceof OffsetDateTime
                    || value instanceof ZonedDateTime) {
                continue;
            }
            if (value instanceof Double || value instanceof Float) {
                sawFraction = true;
                continue;
            }
            if (value instanceof Number) {
                continue;
            }
            var text = value.toString().trim();
            try {
                var parsed = Double.parseDouble(text);
                if (parsed != Math.rint(parsed) || text.indexOf('.') >= 0) {
                    sawFraction = true;
                }
            } catch (NumberFormatException e) {
                // Not a number: a calendar reading still fits, otherwise text.
                if (!parsesAsDate(text) && !parsesAsDateTime(text)) {
                    return ColumnType.TEXT;
                }
            }
        }
        if (!sawValue) {
            return ColumnType.TEXT;
        }
        var first = firstNonMissing(series);
        var text = String.valueOf(first).trim();
        if (parsesAsDate(text)) {
            return ColumnType.DATE;
        }
        if (parsesAsDateTime(text)) {
            return ColumnType.TIMESTAMP;
        }
        return sawFraction ? ColumnType.DOUBLE : ColumnType.INTEGER;
    }

    /** {@return the first non-missing value of the column} */
    private static Object firstNonMissing(Series<?> series) {
        for (var i = 0; i < series.size(); i++) {
            Object value = series.get(i);
            if (!isMissing(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Whether the text is an ISO-8601 calendar date with no time of day, e.g.
     * {@code 2010-01-01}.
     *
     * @param text the cell text
     * @return {@code true} when the text parses as a date
     */
    static boolean parsesAsDate(String text) {
        try {
            LocalDate.parse(text, ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Whether the text is an ISO-8601 date-time, with or without a zone, e.g.
     * {@code 2010-01-01T01:00:00} or {@code 2010-01-01T01:00:00Z}.
     *
     * @param text the cell text
     * @return {@code true} when the text parses as a date-time
     */
    static boolean parsesAsDateTime(String text) {
        try {
            LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * The epoch-millisecond position of a date-time cell.
     * <p>
     * A cell that carries a zone or offset names an instant, so it is read as
     * one. A zone-less cell is a wall-clock reading with no instant of its own;
     * it is taken as UTC, the same reading the rendering path applies, so the
     * round trip through the file is lossless.
     *
     * @param text the cell text
     * @return the position in epoch milliseconds
     */
    static long toEpochMillis(String text) {
        try {
            return ZonedDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneOffset.UTC).toEpochMilli();
        }
    }

    /**
     * Whether a CSV cell is missing. Besides empty cells, the demo files use
     * R's {@code NA} marker for absent values (e.g. some penguin measurements),
     * which is carried through as the literal string.
     */
    private static boolean isMissing(Object value) {
        if (value == null) {
            return true;
        }
        var text = value.toString().trim();
        return text.isEmpty() || "NA".equalsIgnoreCase(text);
    }

    private static void fill(StructBuilder builder, List<String> names,
                             Map<String, ColumnType> types, DataFrame frame, int row) {
        for (var name : names) {
            Object value = frame.getColumn(name).get(row);
            if (isMissing(value)) {
                builder.setNull(name);
                continue;
            }
            var type = types.get(name);
            var text = value.toString().trim();
            if (type.physical() == PhysicalType.INT32) {
                builder.setDate(name, LocalDate.parse(text, ISO_LOCAL_DATE));
            } else if (type.logical() instanceof LogicalType.TimestampType) {
                builder.setTimestamp(name, Instant.ofEpochMilli(toEpochMillis(text)));
            } else if (type.physical() == PhysicalType.INT64) {
                builder.setLong(name, (long) Double.parseDouble(text));
            } else if (type.physical() == PhysicalType.DOUBLE) {
                builder.setDouble(name, Double.parseDouble(text));
            } else {
                builder.setString(name, String.valueOf(value));
            }
        }
    }
}
