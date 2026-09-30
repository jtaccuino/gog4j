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
import java.util.Arrays;
import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.density3d()} — a 2-D Gaussian kernel density estimate computed
 * over a point grid.
 * <p>
 * Emits the density grid as point rows carrying {@code row}/{@code col}
 * coordinates so a downstream {@code Geoms.surface3d()} tiles the surviving
 * (possibly sparse, {@code min_ndensity}-filtered) points without falling back
 * to Delaunay triangulation. Bandwidth selection follows {@code Silverman's rule of thumb}
 * (robust) rule, scaled by {@code adjust}.
 *
 * @param <DF> the DataFrame type
 */
public class StatDensity3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.density3d()} transformation.
     */
    public StatDensity3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var yCol = aes.y() == null ? null : ext.getColumn(df, aes.y());
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }
        var gCol = aes.group() == null ? null : ext.getColumn(df, aes.group());

        var grid = Stat3dSupport.gridParam(params, GridGeometry.RECTANGLE);
        var direction = Stat3dSupport.directionParam(params, GridDirection.Y);
        var trim = params.getBoolean("trim", true);
        var minNdensity = params.getDouble("minNdensity", 0.0);
        var adjust = params.getDouble("adjust", 1.0);
        var pad = params.getDouble("pad", 0.1);

        // Partition indices per group.
        var groups = numericGroups(xCol, yCol, null, gCol, true);
        if (groups == null) {
            return StatData.builder().build();
        }

        var builder = StatData.builder();
        var hParam = hParam(params);
        for (var entry : groups.entrySet()) {
            var idx = entry.getValue();
            if (idx.size() < 3) {
                continue;
            }
            var gx = new double[idx.size()];
            var gy = new double[idx.size()];
            for (int i = 0; i < idx.size(); i++) {
                gx[i] = ((Number) xCol.get(idx.get(i))).doubleValue();
                gy[i] = ((Number) yCol.get(idx.get(i))).doubleValue();
            }

            double[] h;
            if (hParam != null) {
                h = hParam.length == 1 ? new double[] {hParam[0], hParam[0]} : hParam;
            } else {
                h = new double[] {bandwidthNrd(gx), bandwidthNrd(gy)};
            }
            for (int i = 0; i < h.length; i++) {
                h[i] *= adjust;
            }

            var xlim = gridXlim(params, gx, pad);
            var ylim = gridYlim(params, gy, pad);
            var pg = MeshUtil.makePointGrid(grid, Stat3dSupport.nParam(params.get("n", 40)), direction, xlim, ylim, trim);
            var ex = pg.x();
            var ey = pg.y();
            var density = kde2d(gx, gy, ex, ey, h);

            double maxDensity = 0;
            for (double d : density) {
                maxDensity = Math.max(maxDensity, d);
            }

            int m = density.length;
            var row = new int[m];
            var col = new int[m];
            gridIndices(ex, ey, row, col);

            var grad = MeshUtil.gradients(ex, ey, density, row, col);
            String label = entry.getKey();
            for (int i = 0; i < m; i++) {
                if (minNdensity > 0 && (maxDensity <= 0 || density[i] / maxDensity < minNdensity)) {
                    continue;
                }
                builder.add("x", ex[i]);
                builder.add("y", ey[i]);
                builder.add("z", density[i]);
                builder.add("group", label);
                builder.add("row", row[i]);
                builder.add("col", col[i]);
                builder.add("density", density[i]);
                builder.add("ndensity", maxDensity > 0 ? density[i] / maxDensity : 0);
                builder.add("count", density[i] * idx.size());
                builder.add("n", idx.size());
                addGradient(builder, grad, i);
            }
        }
        return builder.build();
    }

    private static void gridIndices(double[] ex, double[] ey, int[] row, int[] col) {
        double[] xu = Arrays.stream(ex).sorted().distinct().toArray();
        double[] yu = Arrays.stream(ey).sorted().distinct().toArray();
        for (int i = 0; i < ex.length; i++) {
            col[i] = indexOf(xu, ex[i]);
            row[i] = indexOf(yu, ey[i]);
        }
    }

    private static int indexOf(double[] sorted, double v) {
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] == v) {
                return i + 1;
            }
        }
        return 1;
    }

    private static double[] gridXlim(StatParams params, double[] gx, double pad) {
        var lim = Stat3dSupport.rangeParam(params, "xlim", null);
        if (lim != null) {
            return lim;
        }
        return Stat3dSupport.paddedRange(gx, pad);
    }

    private static double[] gridYlim(StatParams params, double[] gy, double pad) {
        var lim = Stat3dSupport.rangeParam(params, "ylim", null);
        if (lim != null) {
            return lim;
        }
        return Stat3dSupport.paddedRange(gy, pad);
    }

    private static double[] hParam(StatParams params) {
        var v = params.get("h", null);
        if (v instanceof double[] d && d.length > 0 && d.length <= 2) {
            boolean positive = true;
            for (int i = 0; i < d.length; i++) {
                if (!(d[i] > 0)) {
                    positive = false;
                }
            }
            return positive ? d.clone() : null;
        }
        if (v instanceof Number num && num.doubleValue() > 0) {
            return new double[] {num.doubleValue()};
        }
        return null;
    }

    private static double bandwidthNrd(double[] data) {
        var list = new ArrayList<Double>(data.length);
        for (double v : data) {
            list.add(v);
        }
        return DensityModels.bandwidthNrd(list);
    }

    /**
     * A 2-D Gaussian kernel density estimate: a Gaussian product kernel summed
     * over observations, evaluated on the (possibly non-rectangular) point
     * grid.
     */
    private static double[] kde2d(double[] x, double[] y, double[] ex, double[] ey, double[] h) {
        h[0] /= 4;
        h[1] /= 4;
        var out = new double[ex.length];
        double normX = Math.sqrt(2 * Math.PI) * h[0];
        double normY = Math.sqrt(2 * Math.PI) * h[1];
        for (int i = 0; i < ex.length; i++) {
            double sum = 0;
            for (int j = 0; j < x.length; j++) {
                double dx = (ex[i] - x[j]) / h[0];
                double dy = (ey[i] - y[j]) / h[1];
                sum += Math.exp(-0.5 * (dx * dx + dy * dy));
            }
            out[i] = sum / (x.length * normX * normY);
        }
        return out;
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "row", "col", "density", "ndensity", "count", "n",
                "dzdx", "dzdy", "slope", "aspect");
    }
}
