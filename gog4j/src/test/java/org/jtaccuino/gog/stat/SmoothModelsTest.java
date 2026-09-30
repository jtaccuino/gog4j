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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the smooth-model edge cases, in particular the degenerate groups
 * (every X equal) that would otherwise regress to a NaN slope and draw
 * invisible trend lines.
 */
class SmoothModelsTest {

    @Test
    void lmOnConstantXDrawsNoTrendLine() {
        var res = new StatSmooth<>().fit(
                new double[] {2.5, 2.5, 2.5, 2.5},
                new double[] {29.0, 29.0, 28.0, 29.0},
                SmoothParams.defaults().method(SmoothMethod.LM).se(false));
        assertTrue(res.isEmpty(), "a constant-X group has no slope to regress on");
    }

    @Test
    void lmOnConstantXFullRangeFallsBackToFlatMeanLine() {
        var res = new StatSmooth<>().fit(
                new double[] {2.5, 2.5, 2.5, 2.5},
                new double[] {29.0, 29.0, 28.0, 29.0},
                SmoothParams.defaults()
                        .method(SmoothMethod.LM)
                        .se(true)
                        .fullrange(true)
                        .globalRange(1.0, 6.0)
                        .steps(5));
        assertNoNaN(res);
        for (var v : res.columnAsDoubles("y")) {
            assertEquals(28.75, v, 1e-9);
        }
        for (var v : res.columnAsDoubles("ymax")) {
            assertEquals(28.75, v, 1e-9);
        }
        assertEquals(5, res.columnAsDoubles("y").size());
    }

    @Test
    void lmOnVariableXFitsNormally() {
        var res = new StatSmooth<>().fit(
                new double[] {1.0, 2.0, 3.0, 4.0},
                new double[] {2.0, 3.0, 4.0, 5.0},
                SmoothParams.defaults().method(SmoothMethod.LM).se(false));
        assertNoNaN(res);
        var y = res.columnAsDoubles("y");
        assertEquals(2.0, y.get(0), 1e-9);
        assertEquals(5.0, y.get(y.size() - 1), 1e-9);
    }

    private static void assertNoNaN(StatData res) {
        assertFalse(res.columnAsDoubles("y").stream().anyMatch(v -> Double.isNaN(v)));
        assertFalse(res.columnAsDoubles("ymin").stream().anyMatch(v -> Double.isNaN(v)));
        assertFalse(res.columnAsDoubles("ymax").stream().anyMatch(v -> Double.isNaN(v)));
    }
}
