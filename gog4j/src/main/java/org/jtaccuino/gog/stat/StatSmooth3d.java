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
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.geometry.SurfaceMethod;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.smooth3d()} — fits a 2-D smooth ({@code loess} or {@code lm})
 * surface over the points and emits polygon rows (tiles) covering the domain,
 * optionally clipped to the data's convex hull.
 * <p>
 * When {@code se = TRUE} the tiles are triplicated into {@code upper}/{@code
 * fitted}/{@code lower} confidence surfaces. Data points and residual \u201Cstems\u201D
 * may be appended as {@code prim = "point"} / {@code prim = "segment"} rows that
 * {@code Geoms.surface3d()} overlays on the finished surface.
 *
 * @param <DF> the DataFrame type
 */
public class StatSmooth3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a Stats.smooth3d() transformation.
     */
    public StatSmooth3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        if (aes.x() == null || aes.y() == null || aes.z() == null) {
            return StatData.builder().build();
        }
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var rawZ = ext.getColumn(df, aes.z());
        var rawG = aes.group() == null ? null : ext.getColumn(df, aes.group());

        var method = Stat3dSupport.enumParam(SmoothMethod.class, params.get("method", SmoothMethod.LOESS), SmoothMethod.LOESS, "method");
        var span = params.getDouble("span", 0.75);
        var se = params.getBoolean("se", false);
        var level = params.getDouble("level", 0.95);
        var domain = Stat3dSupport.enumParam(SmoothDomain.class, params.get("domain", SmoothDomain.CHULL), SmoothDomain.CHULL, "domain");
        var grid = Stat3dSupport.gridParam(params, GridGeometry.RECTANGLE);
        var gridType = grid;
        var direction = Stat3dSupport.directionParam(params, GridDirection.X);
        var trim = params.getBoolean("trim", true);
        var addPoints = params.getBoolean("points", false);
        var addResiduals = params.getBoolean("residuals", false);
        var dropMissing = params.getBoolean("na.rm", false);
        double zScore = normalQuantile(0.5 + level / 2);

        var builder = StatData.builder();
        // One output group per input group.
        var groups = numericGroups(rawX, rawY, rawZ, rawG, dropMissing);
        if (groups == null || groups.isEmpty()) {
            return StatData.builder().build();
        }

        for (var entry : groups.entrySet()) {
            var idx = entry.getValue();
            if (idx.size() < 4) {
                continue;
            }
            var xt = new double[idx.size()];
            var yt = new double[idx.size()];
            var zt = new double[idx.size()];
            for (int i = 0; i < idx.size(); i++) {
                xt[i] = ((Number) rawX.get(idx.get(i))).doubleValue();
                yt[i] = ((Number) rawY.get(idx.get(i))).doubleValue();
                zt[i] = ((Number) rawZ.get(idx.get(i))).doubleValue();
            }
            String grp = entry.getKey();

            var xlim = Stat3dSupport.rangeParam(params, "xlim", Stat3dSupport.minMax(xt));
            var ylim = Stat3dSupport.rangeParam(params, "ylim", Stat3dSupport.minMax(yt));
            if (xlim == null || ylim == null) {
                continue;
            }
            var pg = MeshUtil.makePointGrid(grid, Stat3dSupport.nParam(params.get("n", 40)), direction, xlim, ylim, trim);
            var px = pg.x();
            var py = pg.y();
            var tiles = MeshUtil.pointsToTiles(px, py, null, null, null, SurfaceMethod.GRID, gridType, "surfaceTile");

            // Each tile carries its own x/y vertices (clipped to the data hull
            // when requested); z is re-predicted afterwards.
            var groupSuffix = "::grp" + grp;
            var segments = new ArrayList<double[][]>();
            var tileGroups = new ArrayList<String>();
            double[] hullX = null;
            double[] hullY = null;
            if (domain == SmoothDomain.CHULL) {
                var hullIdx = PolygonMath.convexHull(xt, yt);
                hullX = new double[hullIdx.length];
                hullY = new double[hullIdx.length];
                for (int i = 0; i < hullIdx.length; i++) {
                    hullX[i] = xt[hullIdx[i]];
                    hullY[i] = yt[hullIdx[i]];
                }
            }
            for (var tile : tiles) {
                var vx = new double[tile.vertexIdx().length];
                var vy = new double[tile.vertexIdx().length];
                for (int i = 0; i < tile.vertexIdx().length; i++) {
                    vx[i] = px[tile.vertexIdx()[i]];
                    vy[i] = py[tile.vertexIdx()[i]];
                }
                double[][] poly;
                if (hullX != null) {
                    var clipped = PolygonMath.clipConvex(vx, vy, hullX, hullY);
                    if (clipped == null || clipped.length < 3) {
                        continue;
                    }
                    poly = clipped;
                } else {
                    poly = new double[vx.length][2];
                    for (int i = 0; i < vx.length; i++) {
                        poly[i][0] = vx[i];
                        poly[i][1] = vy[i];
                    }
                }
                segments.add(poly);
                tileGroups.add(tile.group() + groupSuffix);
            }
            if (segments.isEmpty()) {
                continue;
            }

            // Fit once over all tile vertices.
            int verts = 0;
            for (var seg : segments) {
                verts += seg.length;
            }
            var ex = new double[verts];
            var ey = new double[verts];
            int k = 0;
            for (var seg : segments) {
                for (var v : seg) {
                    ex[k] = v[0];
                    ey[k] = v[1];
                    k++;
                }
            }
            var fit = method == SmoothMethod.LM
                    ? SmoothModels3D.fitPlane(xt, yt, zt, ex, ey)
                    : SmoothModels3D.fitLoess(xt, yt, zt, ex, ey, span);

            // Panel surfaces: fitted (mandatory), lower/upper when se.
            String[] types;
            if (se) {
                types = new String[] {"upper", "fitted", "lower"};
            } else {
                types = new String[] {"fitted"};
            }
            for (var type : types) {
                String levelLabel = switch (type) {
                    case "upper" -> "upper " + Math.round(level * 100) + "% CI";
                    case "lower" -> "lower " + Math.round(level * 100) + "% CI";
                    default -> "fitted";
                };
                int vi = 0;
                for (int t = 0; t < segments.size(); t++) {
                    var seg = segments.get(t);
                    String tg = tileGroups.get(t) + "-" + type;
                    for (double[] v : seg) {
                        double z = switch (type) {
                            case "upper" -> fit.fitted()[vi] + zScore * fit.se()[vi];
                            case "lower" -> fit.fitted()[vi] - zScore * fit.se()[vi];
                            default -> fit.fitted()[vi];
                        };
                        builder.add("x", v[0]);
                        builder.add("y", v[1]);
                        builder.add("z", z);
                        builder.add("group", tg);
                        builder.add("fitted", fit.fitted()[vi]);
                        builder.add("se", fit.se()[vi]);
                        builder.add("level", levelLabel);
                        builder.add("prim", "polygon");
                        vi++;
                    }
                }
            }

            // Optional data-point overlay.
            if (addPoints) {
                for (int i = 0; i < idx.size(); i++) {
                    builder.add("x", xt[i]);
                    builder.add("y", yt[i]);
                    builder.add("z", zt[i]);
                    builder.add("group", "smoothDataGroup" + grp + "-" + i);
                    builder.add("fitted", zt[i]);
                    builder.add("se", 0.0);
                    builder.add("level", "fitted");
                    builder.add("prim", "point");
                }
            }

            // Optional residual stems from each observed point to the surface.
            if (addResiduals) {
                var atData = method == SmoothMethod.LM
                        ? SmoothModels3D.fitPlane(xt, yt, zt, xt, yt)
                        : SmoothModels3D.fitLoess(xt, yt, zt, xt, yt, span);
                for (int i = 0; i < idx.size(); i++) {
                    String group = "smoothResidGroup" + grp + "-" + i;
                    builder.add("x", xt[i]);
                    builder.add("y", yt[i]);
                    builder.add("z", atData.fitted()[i]);
                    builder.add("group", group);
                    builder.add("fitted", atData.fitted()[i]);
                    builder.add("se", 0.0);
                    builder.add("level", "fitted");
                    builder.add("prim", "segment");
                    builder.add("x", xt[i]);
                    builder.add("y", yt[i]);
                    builder.add("z", zt[i]);
                    builder.add("group", group);
                    builder.add("fitted", atData.fitted()[i]);
                    builder.add("se", 0.0);
                    builder.add("level", "fitted");
                    builder.add("prim", "segment");
                }
            }
        }
        return builder.build();
    }

    /** Acklam's inverse normal CDF approximation, giving the se z-score. */
    private static double normalQuantile(double p) {
        if (p <= 0 || p >= 1) {
            return Double.NEGATIVE_INFINITY;
        }
        double[] a = {-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
                1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
                6.680131188771972e+01, -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
                -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00};
        double[] d = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00,
                3.754408661907416e+00};
        double pLow = 0.02425;
        double pHigh = 1 - pLow;
        double q;
        double r;
        if (p < pLow) {
            q = Math.sqrt(-2 * Math.log(p));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
        }
        if (p <= pHigh) {
            q = p - 0.5;
            r = q * q;
            return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                    / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
        }
        q = Math.sqrt(-2 * Math.log(1 - p));
        return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "fitted", "se", "level", "prim");
    }
}
