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
package org.jtaccuino.gog.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the pairwise depth sorter: larger depth = farther, output is
 * back-to-front (farthest first), and {@code PrimitivePolygon.fromNearness}
 * correctly converts nearness values to internal depth.
 */
class Pairwise3dSorterTest {

    @Test
    void fromNearnessNegatesValues() {
        var prim = Pairwise3dSorter.PrimitivePolygon.fromNearness(
                new double[]{1, 2, 3},
                new double[]{4, 5, 6},
                new double[]{0.2, 0.5, 0.9});
        assertEquals(3, prim.depth().length);
        assertEquals(-0.2, prim.depth()[0], 1e-12);
        assertEquals(-0.5, prim.depth()[1], 1e-12);
        assertEquals(-0.9, prim.depth()[2], 1e-12);
    }

    @Test
    void overlappingPolygonsDrawFarFirst() {
        // Two identical quads overlapping, the far one has larger depth values.
        var back = makePoly(2.5,
                100, 100, 100, 400, 400, 400, 400, 100);
        var front = makePoly(2.0,
                100, 100, 100, 400, 400, 400, 400, 100);

        var order = Pairwise3dSorter.sort(List.of(back, front));
        assertNotNull(order);
        assertEquals(2, order.length);
        assertArrayEquals(new int[]{0, 1}, order,
                "back polygon (index 0) must be painted before front (index 1)");
    }

    @Test
    void noOverlapFallsBackToDepthOrder() {
        // Two non-overlapping polygons: no constraints; topological sort
        // picks the farthest first by mean depth.
        var far = makePoly(3.0,
                10, 10, 10, 40, 40, 40, 40, 10);
        var near = makePoly(1.0,
                200, 200, 200, 230, 230, 230, 230, 200);

        var order = Pairwise3dSorter.sort(List.of(far, near));
        assertNotNull(order);
        assertEquals(2, order.length);
        assertArrayEquals(new int[]{0, 1}, order,
                "far polygon must be painted first even without overlap constraints");
    }

    @Test
    void nearnessFactoryProducesBackToFrontOrder() {
        // The typical use case: nearness values (larger = nearer) converted
        // via fromNearness. The far slice (nearness=0.5) should be painted
        // before the near slice (nearness=0.9).
        var back = Pairwise3dSorter.PrimitivePolygon.fromNearness(
                new double[]{100, 100, 400, 400},
                new double[]{100, 400, 400, 100},
                new double[]{0.5, 0.5, 0.5, 0.5});
        var front = Pairwise3dSorter.PrimitivePolygon.fromNearness(
                new double[]{100, 100, 400, 400},
                new double[]{100, 400, 400, 100},
                new double[]{0.9, 0.9, 0.9, 0.9});

        var order = Pairwise3dSorter.sort(List.of(back, front));
        assertArrayEquals(new int[]{0, 1}, order,
                "fromNearness: far (0.5) must be painted before near (0.9)");
    }

    @Test
    void segmentAndPointSortByMeanDepth() {
        // A far segment behind a near point sharing the same screen region:
        // mean depth drives the painter's order when no overlap constraint
        // applies, so the farther segment paints first.
        var far = Pairwise3dSorter.PrimitiveSegment.fromNearness(
                50, 50, 0.4, 350, 350, 0.4);
        var near = Pairwise3dSorter.PrimitivePoint.fromNearness(400, 60, 0.9);

        var order = Pairwise3dSorter.sort(List.of(far, near));
        assertArrayEquals(new int[]{0, 1}, order,
                "far segment (mean depth) must be painted before the near point");
    }

    @Test
    void scalarFactoriesNegateNearness() {
        var segment = Pairwise3dSorter.PrimitiveSegment.fromNearness(
                1, 2, 0.3, 3, 4, 0.7);
        assertEquals(-0.3, segment.depth0(), 1e-12);
        assertEquals(-0.7, segment.depth1(), 1e-12);

        var point = Pairwise3dSorter.PrimitivePoint.fromNearness(1, 2, 0.5);
        assertEquals(-0.5, point.depth(), 1e-12);
    }

    private static Pairwise3dSorter.PrimitivePolygon makePoly(double depth,
                                                  double x0, double y0,
                                                  double x1, double y1,
                                                  double x2, double y2,
                                                  double x3, double y3) {
        return new Pairwise3dSorter.PrimitivePolygon(
                new double[]{x0, x1, x2, x3},
                new double[]{y0, y1, y2, y3},
                new double[]{depth, depth, depth, depth});
    }
}
