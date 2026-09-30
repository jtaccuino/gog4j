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

import java.util.List;
import java.util.Map;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.junit.jupiter.api.Test;

/**
 * Guards the discrete placement of {@code Stats.bar3d()} bars: 0-based
 * ordinal positions that mirror {@code Scale.createDiscrete()}'s
 * {@code [-0.5, N - 0.5]} domain, and the 0.9 default width that leaves a gap
 * between neighbouring bars. This keeps {@code Geoms.bar3d()} bars inside the
 * coord3d cube — the earlier 1-based ordinals pushed the last category's bars
 * beyond the {@code N - 0.5} bound.
 */
class StatBar3dDiscreteTest {

    private static DataFrame twoByTwo() {
        return DataFrame.byColumn("x", "y")
                .of(Series.of("a", "a", "b", "b"), Series.of("A", "B", "A", "B"));
    }

    @Test
    void discreteBarsSitOnZeroBasedOrdinalsAndFitDomain() {
        var data = Stats.<DataFrame>bar3d().computeLayer(twoByTwo(), new DflibDataExtractor(),
                Aes.aes().x("x").y("y"), StatParams.empty());
        var xs = data.column("x");
        var ys = data.column("y");
        var minX = extent(xs, true);
        var maxX = extent(xs, false);
        var minY = extent(ys, true);
        var maxY = extent(ys, false);

        // 0-based ordinals: the first bar is centred at 0 (its left edge is under 0),
        // not shifted a full slot to the right as the 1-based code produced.
        assertTrue(minX < 0.0, "first bar must span the 0-band, left edge centred at 0");
        // Default width 0.9 on a spacing of 1 leaves a 0.1 gap on each side.
        assertEquals(0.45, -minX, 1e-9, "discrete bars default to width 0.9 (half 0.45)");
        assertEquals(0.45, maxX - 1.0, 1e-9, "second bar's right edge is 0.45 past its centre 1");
        assertEquals(0.45, -minY, 1e-9, "y bars also default to width 0.9");
        // Everything stays within the discrete domain [-0.5, N - 0.5] = [-0.5, 1.5].
        assertTrue(minX >= -0.5 - 1e-9 && maxX <= 1.5 + 1e-9, "x bars must fit the discrete domain");
        assertTrue(minY >= -0.5 - 1e-9 && maxY <= 1.5 + 1e-9, "y bars must fit the discrete domain");
    }

    @Test
    void explicitFullWidthFillsTheBandWithoutExceedingIt() {
        var data = Stats.<DataFrame>bar3d().computeLayer(twoByTwo(), new DflibDataExtractor(),
                Aes.aes().x("x").y("y"), StatParams.of(Map.of("width", 1.0)));
        var xs = data.column("x");
        var ys = data.column("y");
        // An explicit width of 1 fills the band edge to edge ([i - 0.5, i + 0.5]).
        assertEquals(-0.5, extent(xs, true), 1e-9);
        assertEquals(1.5, extent(xs, false), 1e-9);
        assertTrue(extent(ys, true) >= -0.5 - 1e-9 && extent(ys, false) <= 1.5 + 1e-9,
                "y bars must still respect the discrete domain");
    }

    /** The outermost finite coordinate in a series, min or max. */
    private static double extent(List<?> values, boolean min) {
        double best = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        for (var v : values) {
            if (v instanceof Number num) {
                double d = num.doubleValue();
                if (Double.isFinite(d)) {
                    best = min ? Math.min(best, d) : Math.max(best, d);
                }
            }
        }
        return best;
    }
}
