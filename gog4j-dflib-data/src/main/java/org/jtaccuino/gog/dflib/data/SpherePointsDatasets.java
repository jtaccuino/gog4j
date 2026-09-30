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
 * The demo point cloud behind the 3-D point/hull figures: points on a
 * gently lumpy sphere, sampled on a latitude/longitude grid with a
 * {@code band} column (north/tropics/south) so a scatter can colour the
 * surface bands and a convex or alpha hull can enclose the cloud.
 */
public final class SpherePointsDatasets {

    /** Longitude columns per hemisphere (u direction). */
    private static final int U = 16;

    /** Latitude rows (v direction) from pole to pole. */
    private static final int V = 14;

    /** Utility class; not meant to be instantiated. */
    private SpherePointsDatasets() {
    }

    /**
     * Points on a radius-varying sphere, z vertical: x follows the sine of the
     * polar angle, y the longitude, z the cosine, matching the cube's z-up
     * convention. The radius wobbles with the longitude so the cloud is not a
     * perfect sphere — a hull then has edges to show.
     *
     * @return a {@link DataFrame} with columns {@code x}, {@code y}, {@code z},
     *         and {@code band}
     */
    public static DataFrame loadSpherePoints() {
        var xs = new double[U * V];
        var ys = new double[U * V];
        var zs = new double[U * V];
        var bands = new String[U * V];
        int k = 0;
        for (int u = 0; u < U; u++) {
            double theta = Math.PI * u / (U - 1);
            double sinT = Math.sin(theta);
            double cosT = Math.cos(theta);
            for (int v = 0; v < V; v++) {
                double phi = 2 * Math.PI * v / V;
                double r = 1.0 + 0.18 * Math.sin(3 * phi) * Math.cos(2 * theta);
                xs[k] = r * sinT * Math.cos(phi);
                ys[k] = r * sinT * Math.sin(phi);
                zs[k] = r * cosT;
                bands[k] = theta < Math.PI / 3 ? "north"
                        : theta > 2 * Math.PI / 3 ? "south" : "tropics";
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z", "band").of(
                Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs),
                Series.of(bands));
    }
}
