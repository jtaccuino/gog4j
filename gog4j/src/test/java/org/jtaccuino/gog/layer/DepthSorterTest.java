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
package org.jtaccuino.gog.layer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/**
 * Guards the painter's-algorithm ordering of {@link DepthSorter}: farthest
 * first, stable for ties, and group-coherent for hierarchical labels.
 */
class DepthSorterTest {

    @Test
    void flatOrderDrawsFarthestFirst() {
        var nearness = new double[] {0.2, 0.9, 0.5};
        assertArrayEquals(new int[] {0, 2, 1}, DepthSorter.order(nearness));
    }

    @Test
    void flatOrderKeepsStableTies() {
        var nearness = new double[] {0.5, 0.5, 0.5};
        assertArrayEquals(new int[] {0, 1, 2}, DepthSorter.order(nearness));
    }

    @Test
    void flatOrderLeavesEmptyArraysAlone() {
        assertArrayEquals(new int[0], DepthSorter.order(new double[0]));
    }

    @Test
    void groupedOrderKeepsGroupsContiguous() {
        var level1 = new String[] {null, "B", "B", null, "A"};
        var nearness = new double[] {0.3, 0.8, 0.9, 0.1, 0.2};
        var order = DepthSorter.order(level1, nearness);
        assertArrayEquals(new int[] {3, 4, 0, 1, 2}, order);
    }

    @Test
    void groupedOrderIsStableWithinGroup() {
        var level1 = new String[] {"A", "A", "A"};
        var nearness = new double[] {0.4, 0.4, 0.4};
        var order = DepthSorter.order(level1, nearness);
        assertArrayEquals(new int[] {0, 1, 2}, order);
    }
}
