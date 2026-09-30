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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jtaccuino.gog.hardwood.HardwoodDataExtractor;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.junit.jupiter.api.Test;

/**
 * Verifies the generated Parquet datasets read back through the Hardwood
 * extractor with the expected shape, column names, and types.
 */
class ParquetDatasetsTest {

    private static final HardwoodDataExtractor EXTRACTOR = new HardwoodDataExtractor();

    @Test
    void penguinsHasExpectedShapeAndTypes() {
        HardwoodDataFrame penguins = HardwoodPenguinsDatasets.loadPenguins();
        assertEquals(344, EXTRACTOR.getRowCount(penguins));
        assertEquals(8, penguins.columnNames().size());
        assertTrue(penguins.columnNames().contains("bill_length_mm"));
        assertEquals("Adelie", EXTRACTOR.getColumn(penguins, "species").get(0));
        assertEquals(HardwoodDataExtractor.ColumnType.TEXT,
                EXTRACTOR.columnType(penguins, "species"));
        assertEquals(HardwoodDataExtractor.ColumnType.NUMBER,
                EXTRACTOR.columnType(penguins, "bill_length_mm"));
    }

    @Test
    void tipsHasExpectedShape() {
        HardwoodDataFrame tips = HardwoodTipsDatasets.loadTips();
        assertEquals(244, EXTRACTOR.getRowCount(tips));
        assertEquals(7, tips.columnNames().size());
        assertEquals(HardwoodDataExtractor.ColumnType.NUMBER,
                EXTRACTOR.columnType(tips, "total_bill"));
    }

    @Test
    void mtcarsHasExpectedShape() {
        HardwoodDataFrame mtcars = HardwoodMtcarsDatasets.loadMtcars();
        assertEquals(32, EXTRACTOR.getRowCount(mtcars));
        assertEquals(12, mtcars.columnNames().size());
    }

    @Test
    void diamondsHasExpectedShape() {
        HardwoodDataFrame diamonds = HardwoodDiamondsDatasets.loadDiamonds();
        assertEquals(53940, EXTRACTOR.getRowCount(diamonds));
        assertEquals(10, diamonds.columnNames().size());
    }

    @Test
    void gwasManhattanDerivesThePlotReadyColumns() {
        HardwoodDataFrame manhattan = HardwoodGwasDatasets.loadManhattan();
        assertEquals(HardwoodGwasDatasets.THINNED_SNP_COUNT, EXTRACTOR.getRowCount(manhattan));
        assertTrue(manhattan.columnNames().containsAll(
                java.util.List.of("CHR", "BP", "P", "BPCUM", "NEGLOG10P", "CHRBAND", "BAND", "GENE")));
        assertTrue(HardwoodGwasDatasets.highlightedLeadSnps(manhattan) > 0,
                "at least one lead SNP must be highlighted");
        assertEquals(22, HardwoodGwasDatasets.chromosomeBreaks().size());
    }

    @Test
    void gwasQqPlotIsSampledOnly() {
        HardwoodDataFrame qq = HardwoodGwasDatasets.qqPlot(HardwoodGwasDatasets.loadManhattan());
        assertEquals(HardwoodGwasDatasets.UNIFORM_SAMPLE_COUNT, EXTRACTOR.getRowCount(qq));
        assertTrue(qq.columnNames().containsAll(java.util.List.of("EXPECTED", "OBSERVED", "BAND")));
    }

    @Test
    void gwasChromosomeCloseUpKeepsOneChromosome() {
        HardwoodDataFrame close = HardwoodGwasDatasets.chromosome(
                HardwoodGwasDatasets.loadManhattan(), 3);
        assertTrue(EXTRACTOR.getRowCount(close) > 0);
        assertNotNull(close.columnNames().contains("MB") ? close : null);
    }
}
