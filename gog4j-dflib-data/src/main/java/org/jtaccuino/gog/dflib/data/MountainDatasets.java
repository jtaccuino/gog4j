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
package org.jtaccuino.gog.dflib.data;

import org.dflib.DataFrame;
import org.dflib.Series;

/**
 * Demo terrain for the 3-D surface figures: a regular
 * {@link #loadMountain()} grid of the classic two-peak "mountain" height
 * function sampled over {@code [-3, 3]}². The 45×45 sampling keeps every tile
 * crisp at the gallery's cube canvas while remaining light enough to re-render
 * live under an {@code Orbit3d} drag.
 */
public final class MountainDatasets {

    private static final int N = 45;

    /** Utility class; not meant to be instantiated. */
    private MountainDatasets() {
    }

    /**
     * A regular 45×45 grid over {@code [-3, 3]}² of the classic two-peak
     * mountain surface
     * {@code z = 3(1-x)²e^{-x²-(y+1)²} - 10(x/5 - x³ - y⁵)e^{-x²-y²} - ⅓e^{-(x+1)²-y²}}.
     *
     * @return a {@link DataFrame} with columns {@code x}, {@code y}, and {@code z}
     */
    public static DataFrame loadMountain() {
        var xs = new double[N * N];
        var ys = new double[N * N];
        var zs = new double[N * N];
        int k = 0;
        for (int i = 0; i < N; i++) {
            double x = -3 + 6.0 * i / (N - 1);
            for (int j = 0; j < N; j++) {
                double y = -3 + 6.0 * j / (N - 1);
                double z = 3 * (1 - x) * (1 - x) * Math.exp(-(x * x) - (y + 1) * (y + 1))
                        - 10 * (x / 5 - Math.pow(x, 3) - Math.pow(y, 5)) * Math.exp(-x * x - y * y)
                        - 1.0 / 3 * Math.exp(-(x + 1) * (x + 1) - y * y);
                xs[k] = x;
                ys[k] = y;
                zs[k] = z;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }
}
