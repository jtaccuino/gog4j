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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.junit.jupiter.api.Test;

class SizeScaleTest {

    @Test
    void rangeSpansTheColumnValues() {
        var df = DataFrame.byColumn("s").of(Series.of(2.0, 4.0, 8.0));
        var range = SizeScale.range(df, new DflibDataExtractor(), "s");
        assertEquals(2.0, range.min());
        assertEquals(8.0, range.max());
    }

    @Test
    void rangeIsNullWithoutNumericValues() {
        var df = DataFrame.byColumn("s").of(Series.of(null, null));
        assertNull(SizeScale.range(df, new DflibDataExtractor(), "s"));
    }

    @Test
    void radiusIsProportionalToTheSquareRootOfTheValue() {
        var range = new MinMax(0.0, 100.0);
        assertEquals(SizeScale.RADIUS_MIN, SizeScale.radiusFor(0.0, range));
        // area ∝ value ⇒ radius ∝ sqrt(value): half the max value gives
        // sqrt(0.5) ≈ 0.7071 of the radius span.
        assertEquals(SizeScale.RADIUS_MIN + (SizeScale.RADIUS_MAX - SizeScale.RADIUS_MIN) * Math.sqrt(0.5),
                SizeScale.radiusFor(50.0, range), 1e-9);
        assertEquals(SizeScale.RADIUS_MAX, SizeScale.radiusFor(100.0, range));
        // Values outside the domain clamp to the endpoints.
        assertEquals(SizeScale.RADIUS_MAX, SizeScale.radiusFor(150.0, range));
        assertEquals(SizeScale.RADIUS_MIN, SizeScale.radiusFor(-10.0, range));
    }

    @Test
    void degenerateRangeMapsToTheMiddleRadius() {
        var range = new MinMax(5.0, 5.0);
        assertEquals((SizeScale.RADIUS_MIN + SizeScale.RADIUS_MAX) / 2.0,
                SizeScale.radiusFor(5.0, range));
    }
}
