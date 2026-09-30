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

import java.util.ArrayList;
import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.distributions3d()} — one 1-D kernel density per position slice,
 * laid out as 3-D ridgelines. Along the ridge (march) axis the emitted x/y
 * coordinate is the constant slice position; across it the density estimate.
 * A downstream {@code Geoms.ridgeline3d()} slices these point rows into closed
 * base-to-ridge polygons.
 *
 * @param <DF> the DataFrame type
 */
public class StatDistributions3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a Stats.distributions3d() transformation.
     */
    public StatDistributions3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var yCol = aes.y() == null ? null : ext.getColumn(df, aes.y());
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }
        var gCol = aes.group() == null ? null : ext.getColumn(df, aes.group());

        var direction = String.valueOf(params.get("direction", "x"));
        var nGrid = params.getInt("n", 512);
        var adjust = params.getDouble("adjust", 1.0);
        var bwRule = params.get("bw", "nrd0");
        var kernel = String.valueOf(params.get("kernel", "gaussian"));
        var trim = params.getBoolean("trim", false);
        var relMinHeight = params.getDouble("relMinHeight", 0.0);
        var boundsLo = params.getDouble("boundsLo", Double.NEGATIVE_INFINITY);
        var boundsHi = params.getDouble("boundsHi", Double.POSITIVE_INFINITY);

        var builder = StatData.builder();
        var groups = numericGroups(xCol, yCol, null, gCol, true);
        if (groups == null) {
            return StatData.builder().build();
        }

        for (var entry : groups.entrySet()) {
            var idx = entry.getValue();
            var denseVals = new ArrayList<Double>();
            double position = 0;
            boolean positionSet = false;
            for (int i : idx) {
                double xv = ((Number) xCol.get(i)).doubleValue();
                double yv = ((Number) yCol.get(i)).doubleValue();
                double dv = "x".equals(direction) ? yv : xv;
                if (Double.isFinite(dv)) {
                    if (boundsLo != Double.NEGATIVE_INFINITY && dv < boundsLo) {
                        continue;
                    }
                    if (boundsHi != Double.POSITIVE_INFINITY && dv > boundsHi) {
                        continue;
                    }
                    denseVals.add(dv);
                }
                if (!positionSet) {
                    position = "x".equals(direction) ? xv : yv;
                    positionSet = true;
                }
            }
            if (denseVals.size() < 2) {
                continue;
            }

            double bw = resolveBw(bwRule, denseVals) * adjust;
            int m = Math.max(2, nGrid);
            var mm = Stat3dSupport.minMax(denseVals.stream().mapToDouble(Double::doubleValue).toArray());
            var dataRange = mm != null ? mm : new double[] {denseVals.get(0), denseVals.get(0)};
            double lo = trim ? dataRange[0] : dataRange[0] - 4 * bw;
            double hi = trim ? dataRange[1] : dataRange[1] + 4 * bw;
            if (!(hi > lo)) {
                hi = lo + 1;
            }
            var eval = new double[m];
            for (int i = 0; i < m; i++) {
                eval[i] = lo + (hi - lo) * i / (m - 1);
            }
            var density = DensityModels.densityAt(denseVals, eval, bw, kernel);

            // Zero out density outside finite bounds.
            if (boundsLo != Double.NEGATIVE_INFINITY || boundsHi != Double.POSITIVE_INFINITY) {
                for (int i = 0; i < m; i++) {
                    if (boundsLo != Double.NEGATIVE_INFINITY && eval[i] < boundsLo) {
                        density[i] = 0;
                    }
                    if (boundsHi != Double.POSITIVE_INFINITY && eval[i] > boundsHi) {
                        density[i] = 0;
                    }
                }
            }

            double maxDensity = 0;
            for (double d : density) {
                maxDensity = Math.max(maxDensity, d);
            }
            double threshold = relMinHeight * maxDensity;
            int nObs = denseVals.size();
            String label = entry.getKey();
            for (int i = 0; i < m; i++) {
                if (relMinHeight > 0 && density[i] < threshold) {
                    continue;
                }
                builder.add("x", "x".equals(direction) ? position : eval[i]);
                builder.add("y", "x".equals(direction) ? eval[i] : position);
                builder.add("z", density[i]);
                builder.add("group", label);
                builder.add("density", density[i]);
                builder.add("ndensity", maxDensity > 0 ? density[i] / maxDensity : 0);
                builder.add("count", density[i] * nObs);
                builder.add("n", nObs);
                builder.add("bw", bw);
            }
        }
        return builder.build();
    }

    private static double resolveBw(Object rule, List<Double> data) {
        if (rule instanceof Number num) {
            return num.doubleValue();
        }
        if (rule instanceof double[] d && d.length > 0) {
            return d[0];
        }
        return "nrd".equals(rule) ? DensityModels.bandwidthNrd(data) : DensityModels.bandwidthNrd0(data);
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "density", "ndensity", "count", "n", "bw");
    }
}
