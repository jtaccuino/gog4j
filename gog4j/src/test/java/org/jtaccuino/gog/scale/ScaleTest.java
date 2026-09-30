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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScaleTest {

    @Test
    void toPixelInterpolatesLinearly() {
        var scale = new Scale(0, 10, 0, 100);
        assertEquals(0, scale.toPixel(0));
        assertEquals(50, scale.toPixel(5));
        assertEquals(100, scale.toPixel(10));
    }

    @Test
    void toPixelHandlesInvertedPixelRange() {
        // Y-Achsen laufen auf dem Canvas von unten (großer Pixelwert) nach oben
        var scale = new Scale(0, 10, 200, 0);
        assertEquals(200, scale.toPixel(0));
        assertEquals(100, scale.toPixel(5));
        assertEquals(0, scale.toPixel(10));
    }

    @Test
    void toPixelWithZeroDataRangeReturnsMinPixel() {
        var scale = new Scale(5, 5, 0, 100);
        assertEquals(0, scale.toPixel(5));
    }

    @Test
    void continuousScaleIsNotDiscrete() {
        var scale = new Scale(0, 1, 0, 1);
        assertFalse(scale.isDiscrete());
    }

    @Test
    void discreteScaleMapsCategoriesToIndices() {
        var scale = Scale.createDiscrete(List.of("a", "b", "c"), 0, 300);
        assertTrue(scale.isDiscrete());
        assertEquals(0, scale.toData("a"));
        assertEquals(1, scale.toData("b"));
        assertEquals(2, scale.toData("c"));
        assertEquals(-0.5, scale.minData());
        assertEquals(2.5, scale.maxData());
        assertEquals(List.of("a", "b", "c"), scale.getTickLabels());
    }

    @Test
    void toDataExtractsNumbersOnContinuousScale() {
        var scale = new Scale(0, 10, 0, 100);
        assertEquals(3.5, scale.toData((Object) 3.5));
        assertEquals(7, scale.toData((Object) 7));
        assertEquals(0, scale.toData("kein Zahlenwert"));
    }

    @Test
    void toDataIsInverseOfToPixel() {
        var scale = new Scale(0, 10, 200, 0);
        assertEquals(5, scale.toData(scale.toPixel(5)));
        assertEquals(2.5, scale.toData(scale.toPixel(2.5)));
    }

    @Test
    void calculateTicksUsesUserBreaksIfProvided() {
        var scale = new Scale(0, 10, 0, 100);
        assertEquals(List.of(2.0, 4.0, 6.0), scale.calculateTicks(5, List.of(2.0, 4.0, 6.0)));
    }

    @Test
    void calculateTicksGeneratesPrettyBreaks() {
        var scale = new Scale(0, 10, 0, 100);
        var ticks = scale.calculateTicks(5, null);
        assertFalse(ticks.isEmpty());
        assertEquals(0.0, ticks.getFirst());
        assertEquals(10.0, ticks.getLast());
    }

    @Test
    void calculateTicksReturnsOneIndexPerCategory() {
        var scale = Scale.createDiscrete(List.of("a", "b", "c"), 0, 300);
        assertEquals(List.of(0.0, 1.0, 2.0), scale.calculateTicks(5, null));
    }
}
