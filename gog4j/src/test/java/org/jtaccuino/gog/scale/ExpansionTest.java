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

import org.junit.jupiter.api.Test;

class ExpansionTest {

    @Test
    void noneLeavesTheRangeUntouched() {
        var expanded = Expansion.none().expand(10.0, 20.0);
        assertEquals(10.0, expanded.min(), 1e-9);
        assertEquals(20.0, expanded.max(), 1e-9);
    }

    @Test
    void multWidensBothSidesByTheDataSpanFraction() {
        var expanded = Expansion.mult(0.5).expand(10.0, 20.0);
        assertEquals(5.0, expanded.min(), 1e-9);
        assertEquals(25.0, expanded.max(), 1e-9);
    }

    @Test
    void addWidensBothSidesByAConstantAmount() {
        var expanded = Expansion.add(2.0).expand(10.0, 20.0);
        assertEquals(8.0, expanded.min(), 1e-9);
        assertEquals(22.0, expanded.max(), 1e-9);
    }

    @Test
    void multAsymmetricWidensEachSideIndependently() {
        var expanded = Expansion.mult(0.1, 0.5).expand(10.0, 20.0);
        assertEquals(9.0, expanded.min(), 1e-9);
        assertEquals(25.0, expanded.max(), 1e-9);
    }

    @Test
    void addAsymmetricWidensEachSideIndependently() {
        var expanded = Expansion.add(1.0, 3.0).expand(10.0, 20.0);
        assertEquals(9.0, expanded.min(), 1e-9);
        assertEquals(23.0, expanded.max(), 1e-9);
    }

    @Test
    void expansionAsymmetricCombinesMultAndAddPerSide() {
        var expanded = Expansion.expansion(0.1, 0.5, 1.0, 3.0).expand(10.0, 20.0);
        assertEquals(8.0, expanded.min(), 1e-9);
        assertEquals(28.0, expanded.max(), 1e-9);
    }

    @Test
    void expansionCombinesMultAndAdd() {
        var expanded = Expansion.expansion(0.1, 1.0).expand(10.0, 20.0);
        assertEquals(8.0, expanded.min(), 1e-9);
        assertEquals(22.0, expanded.max(), 1e-9);
    }

    @Test
    void addOnAZeroSpanWidensByTheConstant() {
        var expanded = Expansion.add(5.0).expand(10.0, 10.0);
        assertEquals(5.0, expanded.min(), 1e-9);
        assertEquals(15.0, expanded.max(), 1e-9);
    }
}
