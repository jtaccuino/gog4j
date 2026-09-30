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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;
import org.junit.jupiter.api.Test;

/**
 * Guards the pass-through semantics of {@code Stats.identity3d()}: numeric
 * positions for discrete columns, hierarchical group ids for the depth sorter,
 * and raw-value passthrough for labelling.
 */
class StatIdentity3dTest {

    @SuppressWarnings("unchecked")
    private static DataExtractor<DataFrame> extractor() {
        return (DataExtractor<DataFrame>) DataExtractorRegistry.extractorFor(DataFrame.class);
    }

    @Test
    void discreteAxesBecomeOrdinalPositions() {
        var df = DataFrame.byColumn("x", "y", "z", "g")
                .of(Series.of(1.0, 2.0, 3.0),
                        Series.of("low", "high", "low"),
                        Series.of("c", "a", "b"),
                        Series.of("red", "red", "blue"));
        var out = new StatIdentity3d<DataFrame>().computeLayer(
                df, extractor(), Aes.aes().x("x").y("y").z("z").group("g"), StatParams.empty());
        var ys = out.columnAsDoubles("y");
        var zs = out.columnAsDoubles("z");
        assertEquals(3, ys.size());
        assertEquals(1.0, ys.get(0), 1e-9, "factor levels follow first appearance");
        assertEquals(2.0, ys.get(1), 1e-9);
        assertEquals(1.0, ys.get(2), 1e-9);
        assertEquals(1.0, zs.get(0), 1e-9, "c is the first-appearing z level");
        assertEquals(2.0, zs.get(1), 1e-9);
        assertEquals(3.0, zs.get(2), 1e-9, "b occupies a distinct level");
        assertEquals(3.0, Values.toDouble(out.columnAsDoubles("x").get(2)), 1e-9);
    }

    @Test
    void hierarchicalGroupIdsFeedTheDepthSorter() {
        var df = DataFrame.byColumn("x", "y", "g")
                .of(Series.of(1.0, 2.0, 3.0),
                        Series.of(4.0, 5.0, 6.0),
                        Series.of("left", "left", "right"));
        var out = new StatIdentity3d<DataFrame>().computeLayer(
                df, extractor(), Aes.aes().x("x").y("y").group("g"), StatParams.empty());
        assertEquals(3, out.rowCount());
        assertEquals("left__group", out.column("group").get(0));
        assertEquals("left__group", out.column("group").get(1));
        assertEquals("right__group", out.column("group").get(2));
    }

    @Test
    void unnamedRowsShareTheIdentityGroup() {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1.0, 2.0),
                        Series.of(4.0, 5.0));
        var out = new StatIdentity3d<DataFrame>().computeLayer(
                df, extractor(), Aes.aes().x("x").y("y"), StatParams.empty());
        assertEquals(2, out.rowCount());
        assertEquals("-1__group", out.column("group").get(0));
        assertEquals("-1__group", out.column("group").get(1));
    }

    @Test
    void rowsMissingAxesAreDropped() {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.of(1.0, 2.0, 3.0),
                        Series.of(4.0, 5.0, 6.0),
                        Series.of("a", null, "c"));
        var out = new StatIdentity3d<DataFrame>().computeLayer(
                df, extractor(), Aes.aes().x("x").y("y").z("z"), StatParams.empty());
        assertEquals(2, out.rowCount());
        assertEquals("a", out.column("zRaw").get(0));
        assertEquals("c", out.column("zRaw").get(1));
    }

    @Test
    void rawValuesAreKeptForLabelling() {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.of(1.0, 2.0),
                        Series.of(7.0, 8.0),
                        Series.of("a", "b"));
        var out = new StatIdentity3d<DataFrame>().computeLayer(
                df, extractor(), Aes.aes().x("x").y("y").z("z"), StatParams.empty());
        assertTrue(out.column("xRaw").contains(1.0));
        assertTrue(out.column("yRaw").contains(8.0));
        assertTrue(out.column("zRaw").contains("b"));
    }
}
