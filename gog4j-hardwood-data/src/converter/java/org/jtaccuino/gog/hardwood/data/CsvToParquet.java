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

        var schema = schemaFor(frame);
        var types = physicalTypes(frame);
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

    private static FileSchema schemaFor(DataFrame frame) {
        var builder = FileSchema.builder("gog4j");
        for (var name : columnNames(frame)) {
            var physical = physicalType(frame.getColumn(name));
            builder.addColumn(name, physical, RepetitionType.OPTIONAL,
                    physical == PhysicalType.BYTE_ARRAY ? new LogicalType.StringType() : null);
        }
        return builder.build();
    }

    /** The physical type decided for each column, aligned with the column order. */
    private static Map<String, PhysicalType> physicalTypes(DataFrame frame) {
        var types = new LinkedHashMap<String, PhysicalType>();
        for (var name : columnNames(frame)) {
            types.put(name, physicalType(frame.getColumn(name)));
        }
        return types;
    }

    /**
     * Classifies a column by scanning its values. DFLib's plain CSV loader
     * hands every value back as a {@link String}, so a column counts as numeric
     * when all its non-null values parse as numbers: integral ones become
     * {@code INT64}, any fractional value makes it {@code DOUBLE}, and
     * everything else is {@code BYTE_ARRAY} (UTF-8 string).
     */
    private static PhysicalType physicalType(Series<?> series) {
        var sawValue = false;
        var sawFraction = false;
        for (var i = 0; i < series.size(); i++) {
            Object value = series.get(i);
            if (isMissing(value)) {
                continue;
            }
            sawValue = true;
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
                return PhysicalType.BYTE_ARRAY;
            }
        }
        if (!sawValue) {
            return PhysicalType.BYTE_ARRAY;
        }
        return sawFraction ? PhysicalType.DOUBLE : PhysicalType.INT64;
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
                             Map<String, PhysicalType> types, DataFrame frame, int row) {
        for (var name : names) {
            Object value = frame.getColumn(name).get(row);
            if (isMissing(value)) {
                builder.setNull(name);
            } else if (types.get(name) == PhysicalType.INT64) {
                builder.setLong(name, (long) Double.parseDouble(value.toString().trim()));
            } else if (types.get(name) == PhysicalType.DOUBLE) {
                builder.setDouble(name, Double.parseDouble(value.toString().trim()));
            } else {
                builder.setString(name, String.valueOf(value));
            }
        }
    }
}
