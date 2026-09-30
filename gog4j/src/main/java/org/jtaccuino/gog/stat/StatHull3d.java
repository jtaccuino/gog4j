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

import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.Hull3d;
import org.jtaccuino.gog.geometry.HullMethod;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.hull3d()} — turns a 3-D point cloud into a surface hull of
 * triangular polygons.
 * <p>
 * {@code method = "convex"} returns the convex hull (star-shaped about its
 * centroid); {@code method = "alpha"} returns the alpha shape filtered from
 * the 3-D Delaunay tetrahedralization by circumradius, which can represent
 * non-convex topologies such as the torus. Each input group produces its own
 * hull; triangles carry the hierarchical group
 * {@code "hull3dGroup{t}_tri{i}"} and the per-vertex preserved aesthetics.
 *
 * @param <DF> the DataFrame type
 */
public class StatHull3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.hull3d()} transformation.
     */
    public StatHull3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var rawX = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var rawY = aes.y() == null ? null : ext.getColumn(df, aes.y());
        var rawZ = aes.z() == null ? null : ext.getColumn(df, aes.z());
        if (rawX == null || rawY == null || rawZ == null) {
            return StatData.builder().build();
        }
        HullMethod method = Stat3dSupport.enumParam(HullMethod.class, params.get("method", HullMethod.CONVEX), HullMethod.CONVEX, "method");
        Double radius = params.get("radius", null) instanceof Number num ? num.doubleValue() : null;
        var dropMissing = params.getBoolean("na.rm", false);

        // Partition rows by the group aesthetic (a single bucket when ungrouped).
        var rawG = aes.group() == null ? null : ext.getColumn(df, aes.group());
        var buckets = numericGroups(rawX, rawY, rawZ, rawG, dropMissing);
        if (buckets == null) {
            return StatData.builder().build();
        }

        var builder = StatData.builder();
        int groupNo = 0;
        for (var entry : buckets.entrySet()) {
            var idx = entry.getValue();
            if (idx.size() < 4) {
                continue;
            }
            double[][] coords = new double[idx.size()][3];
            for (int i = 0; i < idx.size(); i++) {
                int r = idx.get(i);
                coords[i][0] = Values.toDouble(rawX.get(r));
                coords[i][1] = Values.toDouble(rawY.get(r));
                coords[i][2] = Values.toDouble(rawZ.get(r));
            }
            int[][] tri;
            if (method == HullMethod.ALPHA) {
                tri = Hull3d.alphaHull(coords, radius != null ? radius : defaultRadius(coords));
            } else {
                tri = Hull3d.convexHull(coords);
            }
            String prefix = "hull3dGroup" + ++groupNo;
            for (int t = 0; t < tri.length; t++) {
                String group = prefix + "_tri" + (t + 1);
                for (int v = 0; v < 3; v++) {
                    int p = idx.get(tri[t][v]);
                    builder.add("x", Values.toDouble(rawX.get(p)));
                    builder.add("y", Values.toDouble(rawY.get(p)));
                    builder.add("z", Values.toDouble(rawZ.get(p)));
                    builder.add("group", group);
                    addPreserved(ext, df, aes, p, builder);
                }
            }
        }
        return builder.build();
    }

    /** The default alpha radius: half the mean per-axis span of the data. */
    private static double defaultRadius(double[][] coords) {
        double spanX = span(coords, 0);
        double spanY = span(coords, 1);
        double spanZ = span(coords, 2);
        return significantDigits(0.5 * (spanX + spanY + spanZ) / 3, 3);
    }

    private static double span(double[][] coords, int axis) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double[] c : coords) {
            min = Math.min(min, c[axis]);
            max = Math.max(max, c[axis]);
        }
        return max - min;
    }

    /** Rounds to the given number of significant digits ({@code significant digits}). */
    private static double significantDigits(double value, int digits) {
        if (value == 0 || !Double.isFinite(value)) {
            return value;
        }
        double mag = Math.pow(10, digits - 1 - (int) Math.floor(Math.log10(Math.abs(value))));
        return Math.round(value * mag) / mag;
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group");
    }
}
