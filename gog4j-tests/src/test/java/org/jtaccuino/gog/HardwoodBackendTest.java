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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.jtaccuino.gog.examples.hardwood.HardwoodGwasPlots;
import org.jtaccuino.gog.hardwood.HardwoodDataExtractor;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodGwasDatasets;
import org.jtaccuino.gog.hardwood.data.HardwoodSeattleWeatherDatasets;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end checks for the Hardwood backend: the extractor is discovered
 * through the {@link java.util.ServiceLoader}, facet/stat partitioning works on
 * a {@link HardwoodDataFrame}, and the Parquet GWAS example renders through the SVG
 * backend.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class HardwoodBackendTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    @Test
    void extractorIsDiscoveredForHardwoodTables() {
        var extractor = DataExtractorRegistry.extractorFor(HardwoodDataFrame.class);
        assertTrue(extractor instanceof HardwoodDataExtractor,
                "the ServiceLoader must resolve the Hardwood backend");
    }

    @Test
    void columnTypeAndMinMaxAreSchemaDriven() {
        var extractor = new HardwoodDataExtractor();
        var table = HardwoodGwasDatasets.loadManhattan();
        assertEquals(DataExtractor.ColumnType.NUMBER, extractor.columnType(table, "NEGLOG10P"));
        assertEquals(DataExtractor.ColumnType.TEXT, extractor.columnType(table, "CHRBAND"));
        var range = extractor.getMinMax(table, "NEGLOG10P");
        assertTrue(range.max() > range.min(), "the Manhattan height must have a range");
    }

    @Test
    void timestampColumnHasAFiniteEpochMilliRange() {
        var extractor = new HardwoodDataExtractor();
        var table = HardwoodSeattleWeatherDatasets.loadHourly();
        assertEquals(DataExtractor.ColumnType.TIMESTAMP, extractor.columnType(table, "date"));
        var range = extractor.getMinMax(table, "date");
        assertTrue(Double.isFinite(range.min()) && Double.isFinite(range.max()),
                "a timestamp range must be finite, not the categorical placeholder");
        assertTrue(range.max() > range.min(), "a year of hourly readings spans a range");
        assertTrue(range.min() > 1.0e12,
                "the range must be epoch milliseconds, not days or raw ticks");
    }

    @Test
    void partitionGridSplitsAParquetBackedTable() {
        var extractor = new HardwoodDataExtractor();
        var table = HardwoodGwasDatasets.chromosome(HardwoodGwasDatasets.loadManhattan(), 3);
        var grid = extractor.partitionGrid(table,
                List.of("BAND"), List.of("BAND"), Set.of());
        assertTrue(grid.numRows() >= 1 && grid.numCols() >= 1,
                "the grid must have at least one cell");
        var lead = grid.panel(List.of("lead"), List.of("lead"));
        assertTrue(lead != null && extractor.getRowCount(lead) > 0,
                "the lead band must hold the highlighted SNPs");
    }

    @Test
    void manhattanExampleRendersFromParquet() throws Exception {
        var svg = onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT)
                .toSvg(HardwoodGwasPlots.createManhattan()));
        assertTrue(svg.contains("Hardwood GWAS"), "the SVG must carry the Manhattan figure");
        assertTrue(svg.contains("<circle") || svg.contains("<polygon") || svg.contains("<path"),
                "the SNP points must project to SVG shapes");
    }

    @Test
    void gwasDerivedTablesAreStable() {
        assertSame(HardwoodGwasDatasets.loadManhattan(), HardwoodGwasDatasets.loadManhattan(),
                "repeated loads must return the cached table");
    }
}
