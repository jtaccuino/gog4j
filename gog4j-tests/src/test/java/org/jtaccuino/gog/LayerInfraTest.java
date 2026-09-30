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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.layer.GeomBar;
import org.jtaccuino.gog.layer.GeomCol;
import org.jtaccuino.gog.layer.GeomFreqpoly;
import org.jtaccuino.gog.layer.GeomHistogram;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatBin;
import org.jtaccuino.gog.stat.StatCount;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.stat.StatParams;
import org.jtaccuino.gog.stat.Stats;
import org.junit.jupiter.api.Test;

/**
 * Guards the Sprint-6 layer-infrastructure primitives: {@code aesOf(Map)},
 * the {@code afterStat} column marker, the {@link Aes.ComputedVariable} enum,
 * {@code StatCount}, the {@code Stat.outputColumns()} contract, and
 * {@link LayerParams}.
 */
class LayerInfraTest {

    @Test
    void aesOfBuildsMappingFromShorthandMap() {
        var a = Aes.aesOf(Map.of(
                "x", "hwy",
                "fill", "::density"));
        assertEquals("hwy", a.x());
        assertEquals("::density", a.fill());
        assertTrue(a.fillValue() instanceof AesValue.AfterStat,
                "a wire value is parsed into its typed after-stat form");
        assertNull(a.y());
    }

    @Test
    void aesOfRejectsUnknownAestheticKey() {
        assertThrows(IllegalArgumentException.class,
                () -> Aes.aesOf(Map.of("colorfloor", "cut")),
                "a misspelled aesthetic must fail loudly, not be ignored");
    }

    @Test
    void afterStatMarksAndStripsComputedColumns() {
        assertTrue(Aes.isComputed("::count"));
        assertEquals("count", Aes.statColumn("::count"));
        assertEquals("::count", AesValue.afterStat("count").render());
        assertFalse(Aes.isComputed("count"));
        assertEquals("n", Aes.statColumn("n"), "a raw column name passes through untouched");
    }

    @Test
    void computedVariableExposesItsOutputColumn() {
        assertEquals("count", AesValue.ComputedVariable.COUNT.column());
        assertEquals("ncount", AesValue.ComputedVariable.NCOUNT.column());
        assertEquals("density", AesValue.ComputedVariable.DENSITY.column());
        assertEquals("ndensity", AesValue.ComputedVariable.NDENSITY.column());
        assertEquals("width", AesValue.ComputedVariable.WIDTH.column());
        assertEquals("prop", AesValue.ComputedVariable.PROP.column());
        assertEquals("n", AesValue.ComputedVariable.N.column());
    }

    @Test
    void statCountCountsPerCategorySortedByLabel() {
        var df = DataFrame.byColumn("x")
                .of(Series.of("c", "a", "b", "a", "c", "c"));
        var data = new StatCount<DataFrame>().computeLayer(
                df, new DflibDataExtractor(), Aes.aes().x("x"), StatParams.empty());

        assertEquals(List.of("a", "b", "c"), data.column("x"));
        assertEquals(List.of(2.0, 1.0, 3.0), data.columnAsDoubles("count"));
        assertEquals(2.0 / 6.0, data.columnAsDoubles("prop").get(0), 1e-9);
        assertEquals(3.0 / 6.0, data.columnAsDoubles("prop").get(2), 1e-9);
    }

    @Test
    void statCountEmitsTheDocumentedOutputColumns() {
        var data = new StatCount<DataFrame>().computeLayer(
                DataFrame.byColumn("x").of(Series.of("a", "b")),
                new DflibDataExtractor(), Aes.aes().x("x"), StatParams.empty());
        assertEquals(List.of("x", "count", "prop"), data.columnNames());
    }

    @Test
    void statDefaultOutputColumnsAreEmpty() {
        assertTrue(new StatNoOutput().outputColumns().isEmpty());
    }

    @Test
    void binSmoothAndSummaryDeclareTheirOutputColumns() {
        assertEquals(List.of("xmin", "xmax", "x", "count", "ncount", "density", "ndensity", "width"),
                Stats.bin().outputColumns());
        assertEquals(List.of("y", "ymin", "ymax"), Stats.smooth().outputColumns());
        assertEquals(List.of("y", "ymin", "ymax", "n"), Stats.summary().outputColumns());
    }

    @Test
    void layerParamsCarryStatConfigAndAestheticDefaults() {
        var params = LayerParams.builder()
                .stat(StatParams.of(Map.of("bins", 40)))
                .build();
        assertEquals(40, params.stat().getInt("bins", 0));
        assertNull(params.fill());
        assertNull(params.color());

        var styled = LayerParams.builder().fill(javafx.scene.paint.Color.RED).build();
        assertEquals(javafx.scene.paint.Color.RED, styled.fill());
    }

    @Test
    void layerParamsEmptyIsEmpty() {
        var empty = LayerParams.empty();
        assertEquals(0, empty.stat().size());
        assertNull(empty.fill());
    }

    @Test
    void statsFactoryBuildsTheBuiltinStats() {
        assertNotNull(Stats.count(), "count() must return a usable stat");
        assertNotNull(Stats.bin(), "bin() must return a usable stat");
        assertNotNull(Stats.smooth(), "smooth() must return a usable stat");
        assertNotNull(Stats.summary(), "summary() must return a usable stat");
        assertNull(Stats.identity(), "Stats.identity means 'run no statistic'");
    }

    @Test
    void geomsExposeTheirGgplot2DefaultStat() {
        assertTrue(new GeomBar<DataFrame>().defaultStat() instanceof StatCount);
        assertNull(new GeomCol<DataFrame>().defaultStat(),
                "Geoms.col() inherits Stats.identity, i.e. no stat");
        assertTrue(new GeomHistogram<DataFrame>().defaultStat() instanceof StatBin);
        assertTrue(new GeomFreqpoly<DataFrame>().defaultStat() instanceof StatBin);
    }

    @Test
    void bareGeomFactoriesHandAssembleLayers() {
        // The Geoms.barGeom() & co. factories return the bare geometry, so the
        // all-in-one layer(...) call can be written without any `new`.
        assertTrue(Geoms.<DataFrame>barGeom().getClass() == GeomBar.class);
        assertTrue(Geoms.<DataFrame>colGeom().getClass() == GeomCol.class);
        assertTrue(Geoms.<DataFrame>histogramGeom().getClass() == GeomHistogram.class);
        assertTrue(Geoms.<DataFrame>freqpolyGeom().getClass() == GeomFreqpoly.class);

        // Defaults still hold on the factory-built bare geoms: Geoms.bar runs
        // Stats.count, Geoms.col the identity stat.
        assertTrue(Geoms.<DataFrame>barGeom().defaultStat() instanceof StatCount);
        assertNull(Geoms.<DataFrame>colGeom().defaultStat());
    }

    @Test
    void barIdentityStatCountsAndColIdentityDrawsRawValues() {
        var ext = new DflibDataExtractor();
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of("a", "a", "b"), Series.ofDouble(1.0, 2.0, 3.0));

        // bar() runs Stats.count: height is the count, y is ignored.
        var count = Stats.<DataFrame>count().computeLayer(df, ext, Aes.aes().x("x"), StatParams.empty());
        assertEquals(List.of("a", "b"), count.column("x"));
        assertEquals(List.of(2.0, 1.0), count.columnAsDoubles("count"));

        // identity() runs no transformation: the caller keeps the raw frame.
        assertNull(Stats.identity());
    }

    /** A stat with the default (empty) output-columns contract. */
    private static final class StatNoOutput implements Stat<DataFrame> {
        @Override
        public StatData computeLayer(DataFrame df, org.jtaccuino.gog.spi.DataExtractor<DataFrame> ext,
                                     Aes aes, StatParams params) {
            return StatData.builder().build();
        }
    }
}
