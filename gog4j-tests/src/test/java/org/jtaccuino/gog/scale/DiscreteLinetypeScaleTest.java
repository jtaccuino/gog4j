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
package org.jtaccuino.gog.scale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link DiscreteLinetypeScale} maps a column's ordered
 * categories to the conventional dash patterns in a repeating cycle, and that
 * {@link LineDash} yields the expected per-name arrays.
 */
class DiscreteLinetypeScaleTest {

    private static DataFrame letters(int n) {
        var x = new String[n];
        var y = new double[n];
        for (var i = 0; i < n; i++) {
            x[i] = Character.toString((char) ('a' + i));
            y[i] = i;
        }
        return DataFrame.byColumn("g", "y").of(Series.of(x), Series.ofDouble(y));
    }

    @Test
    void assignsPatternsInAlphabeticalCategoryOrder() {
        var df = letters(10);
        var scale = DiscreteLinetypeScale.forColumn(df, new DflibDataExtractor(), "g");
        assertEquals(10, scale.categories().size());
        assertEquals("g", scale.columnName());
        for (var cat : scale.categories()) {
            assertArrayEquals(
                    scale.patterns().get(scale.categories().indexOf(cat)),
                    scale.patternFor(cat),
                    "pattern for category " + cat + " must match the cycle assignment");
        }
    }

    @Test
    void patternCycleRepeatsAfterTheNonSolidLineTypes() {
        var df = letters(LineDash.cycle(0).size() * 2 + 1);
        var scale = DiscreteLinetypeScale.forColumn(df, new DflibDataExtractor(), "g");
        var cats = scale.categories();
        var loop = LineDash.cycle(0).size();
        for (int i = 0; i < loop; i++) {
            assertArrayEquals(scale.patternFor(cats.get(i)),
                    scale.patternFor(cats.get(i + loop)),
                    "pattern assignment must repeat every " + loop + " categories");
        }
    }

    @Test
    void unknownAndNullValuesFallBackToSolid() {
        var df = letters(3);
        var scale = DiscreteLinetypeScale.forColumn(df, new DflibDataExtractor(), "g");
        assertNull(scale.patternFor(null));
        assertNull(scale.patternFor("zzz"));
    }

    @Test
    void lineDashOfReturnsTheConventionalArrays() {
        assertNull(LineDash.of("solid"));
        assertNull(LineDash.of("blank"));
        assertNull(LineDash.of("unknown"));
        assertNull(LineDash.of(null));
        assertArrayEquals(new double[] {6.0, 4.0}, LineDash.of("dashed"));
        assertArrayEquals(new double[] {1.0, 3.0}, LineDash.of("dotted"));
        assertArrayEquals(new double[] {1.0, 3.0, 6.0, 3.0}, LineDash.of("dotdash"));
        assertArrayEquals(new double[] {6.0, 3.0}, LineDash.of("longdash"));
        assertArrayEquals(new double[] {2.0, 2.0}, LineDash.of("twodash"));
    }
}
