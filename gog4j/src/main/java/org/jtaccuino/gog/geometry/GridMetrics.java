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

import java.util.Arrays;

/**
 * Grid-spacing helpers for the volume statistics, following the
 * {@code resolution()} and {@code zero_range()} — the grid-expansion and
 * degenerate-range fallbacks that the 3-D column, bar and voxel
 * statistics ({@code Stats.col3d()}, {@code Stats.bar3d()},
 * {@code Stats.voxel3d()}) use to turn grid positions into column/voxel
 * footprints.
 */
public final class GridMetrics {

    private GridMetrics() {
    }

    /**
     * The finest grid spacing of a set of positions — the smallest positive
     * difference between consecutive sorted unique values. A degenerate (or
     * single-valued) set yields {@code 1}, following the
     * {@code resolution()}.
     *
     * @param x    the positions
     * @param zero when {@code true}, include {@code 0} as an extra candidate
     *             (mirrors the {@code zero} parameter)
     * @return the grid resolution
     */
    public static double resolution(double[] x, boolean zero) {
        if (zeroRange(x)) {
            return 1;
        }
        double[] u = uniqueSorted(x);
        if (u.length < 2) {
            return 1;
        }
        double min = Double.MAX_VALUE;
        for (int i = 1; i < u.length; i++) {
            double d = u[i] - u[i - 1];
            if (d > 0 && d < min) {
                min = d;
            }
        }
        if (min == Double.MAX_VALUE) {
            return 1;
        }
        if (zero && u[0] > 0) {
            min = Math.min(min, u[0]);
        }
        return min;
    }

    /** The sorted distinct finite values of {@code x}. */
    private static double[] uniqueSorted(double[] x) {
       return Arrays.stream(x)
                .distinct()
                .sorted()
                .toArray();
    }

    /**
     * Whether a set of positions is essentially constant — the
     * {@code zero_range()}: the span is zero relative to the magnitude.
     */
    private static boolean zeroRange(double[] x) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            if (Double.isFinite(v)) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        if (!Double.isFinite(min)) {
            return true;
        }
        double span = max - min;
        double mag = Math.max(Math.abs(min), Math.abs(max));
        return span < 1e-7 * mag;
    }
}
