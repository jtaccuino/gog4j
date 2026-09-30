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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.layer.PointShape;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the shared {@link DiscreteShapeScale} assigns shapes exactly
 * the way the historical per-call {@link Scale#resolveGlobalShape} resolver
 * did, so the shape legend and the rendered symbols stay in lockstep.
 */
class DiscreteShapeScaleTest {

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
    void assignsShapesInAlphabeticalCategoryOrder() {
        var df = letters(10);
        var scale = DiscreteShapeScale.forColumn(df, new DflibDataExtractor(), "g");
        assertEquals(10, scale.categories().size());
        for (var cat : scale.categories()) {
            var viaScale = scale.shapeFor(cat);
            var viaResolver = Scale.resolveGlobalShape(df, new DflibDataExtractor(), "g", cat);
            assertEquals(viaResolver, viaScale,
                    "shape for category " + cat + " must match the global resolver");
        }
    }

    @Test
    void assignmentCyclesThroughThePointShapeEnum() {
        var df = letters(PointShape.values().length * 2 + 1);
        var scale = DiscreteShapeScale.forColumn(df, new DflibDataExtractor(), "g");
        var cats = scale.categories();
        var loop = PointShape.values().length;
        for (var i = 0; i < loop; i++) {
            var shape = scale.shapeFor(cats.get(i));
            assertEquals(shape, scale.shapeFor(cats.get(i + loop)),
                    "shape assignment must repeat every " + loop + " categories");
        }
    }

    @Test
    void unknownAndNullValuesFallBackToCircle() {
        var df = letters(3);
        var scale = DiscreteShapeScale.forColumn(df, new DflibDataExtractor(), "g");
        assertEquals(PointShape.CIRCLE, scale.shapeFor(null));
        assertEquals(PointShape.CIRCLE, scale.shapeFor("zzz"));
    }
}
