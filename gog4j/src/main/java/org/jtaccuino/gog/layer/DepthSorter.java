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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Painter's-algorithm back-to-front ordering for 3D layers.
 * <p>
 * Elements are emitted so that the farthest one is drawn first and the nearest
 * one last, which gives correct depth layering without a z-buffer. Depth is
 * expressed as <em>nearness</em> {@code 0.0..1.0} (larger = closer to the
 * camera, as produced by {@link org.jtaccuino.gog.coord.Coord3D}'s
 * {@code depthScale}).
 * <ul>
 * <li>{@link #order(double[]) order(nearness)} sorts an ungrouped element list,
 * stable by original index.</li>
 * <li>{@link #order(String[], double[]) order(level1, nearness)} treats
 * {@code level1} (a hierarchical group label) as the grouping key: whole groups
 * are kept contiguous and sorted by the nearness of their farthest member, so
 * a multi-primitive group never self-intersects in depth — the top level of a
 * two-level group hierarchy, where a group label is suffixed with
 * {@code "__group"}.</li>
 * </ul>
 */
public final class DepthSorter {

    /** Utility class; not meant to be instantiated. */
    private DepthSorter() {
    }

    /**
     * Back-to-front order for elements without grouping.
     *
     * @param nearness the per-element depth scaling, near to {@code 1.0} and far
     *                 to {@code 0.0}
     * @return the element indices in draw order (farthest first), possibly empty
     */
    public static int[] order(double[] nearness) {
        var n = nearness.length;
        var order = new Integer[n];
        for (var i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(nearness[a], nearness[b]));
        var result = new int[n];
        for (var i = 0; i < n; i++) {
            result[i] = order[i];
        }
        return result;
    }

    /**
     * Back-to-front order with hierarchical (top-level) group contiguity. Each
     * element's {@code level1} label selects its group; {@code null} labels make
     * a singleton group, i.e. that element sorts flat against its neighbours.
     * Groups are drawn in order of ascending nearness of their farthest member,
     * and the members of a group keep their original relative order.
     *
     * @param level1   the per-element group label ({@code null} for ungrouped)
     * @param nearness the per-element depth scaling, near to {@code 1.0} and far
     *                 to {@code 0.0}
     * @return the element indices in draw order (farthest first), possibly empty
     */
    public static int[] order(String[] level1, double[] nearness) {
        var n = nearness.length;
        var groups = new LinkedHashMap<Object, Group>();
        for (var i = 0; i < n; i++) {
            Object key = level1[i];
            if (key == null) {
                key = Integer.valueOf(i);
            }
            var group = groups.computeIfAbsent(key, k -> new Group());
            group.indices.add(i);
            group.repNearness = Math.min(group.repNearness, nearness[i]);
        }
        var keys = groups.keySet().toArray();
        var reps = new double[keys.length];
        for (var i = 0; i < keys.length; i++) {
            reps[i] = groups.get(keys[i]).repNearness;
        }
        var groupOrder = new Integer[keys.length];
        for (var i = 0; i < keys.length; i++) {
            groupOrder[i] = i;
        }
        Arrays.sort(groupOrder, (a, b) -> Double.compare(reps[a], reps[b]));
        var result = new int[n];
        var oi = 0;
        for (var g : groupOrder) {
            for (var idx : groups.get(keys[g]).indices) {
                result[oi++] = idx;
            }
        }
        return result;
    }

    private static final class Group {
        private final List<Integer> indices = new ArrayList<>();
        private double repNearness = Double.POSITIVE_INFINITY;
    }
}
