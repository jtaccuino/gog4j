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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.DiscreteColorScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * 2D density contour geometry layer: the {@code Geoms.density2d()} counterpart
 * of {@link GeomDensity}.
 * <p>
 * Runs a two-dimensional Gaussian kernel-density estimate (a two-dimensional
 * density transform) over the points mapped
 * to {@code aes(x)} and {@code aes(y)}, then extracts the contours of the
 * resulting surface. {@code Geoms.density2d()} strokes the contour lines —
 * the remedy for over-plotted scatter where the eye cannot count — while
 * {@code Geoms.density2dFilled()} fills the bands between consecutive
 * contours on a continuous colour ramp.
 * <p>
 * Options mirror the reference implementation: the grid resolution
 * ({@code n = 100}), the multiplicative bandwidth adjustment ({@code adjust}),
 * the number of contour levels ({@code bins = 10}), and the contoured
 * statistic ({@code contour_var} of "density", "ndensity", or "count"). A
 * colour mapped column draws one contour set per category, each with its own
 * estimate and bandwidth, exactly as in the reference example.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomDensity2d<DF> implements Layer<DF> {

    /** Default grid resolution in each direction (the {@code n = 100}). */
    public static final int GRID_DIM = 100;

    /** Beyond four bandwidths a Gaussian kernel contributes nothing visible. */
    private static final double KERNEL_CUTOFF = 4.5;

    private int n = GRID_DIM;
    private double adjustX = 1.0;
    private double adjustY = 1.0;
    private int bins = 10;
    private String contourVar = "density";

    private boolean filled = false;
    private double alpha = 0.5;
    private double lineWidth = 0.5;
    private String cmapName = "viridis";

    private Color defaultStroke = Color.web("#3182bd");

    /** The layer's position adjustment, e.g. {@code positionOnFace()} for cube-face placement. */
    private PositionAdjust position = Position.IDENTITY;

    /**
     * Creates a 2D density contour geometry with default estimation parameters.
     */
    public GeomDensity2d() {
    }

    /**
     * Sets the number of grid points the estimate is sampled at in each
     * direction (default {@code 100}).
     *
     * @param n the grid resolution
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> n(int n) {
        this.n = Math.max(16, n);
        return this;
    }

    /**
     * Sets the multiplicative bandwidth adjustment applied to both axes
     * (default {@code 1.0}). A value below one fits the contours to the data
     * more closely, above one smooths them more heavily.
     *
     * @param adjust the bandwidth multiplier for both axes
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> adjust(double adjust) {
        return adjust(adjust, adjust);
    }

    /**
     * Sets independent bandwidth adjustments for the x and y axes.
     *
     * @param xAdjust the bandwidth multiplier for the x axis
     * @param yAdjust the bandwidth multiplier for the y axis
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> adjust(double xAdjust, double yAdjust) {
        this.adjustX = xAdjust;
        this.adjustY = yAdjust;
        return this;
    }

    /**
     * Sets the number of contour levels (default {@code 10}). More bins give
     * finer-grained contours across the density surface.
     *
     * @param bins the number of evenly spaced contour levels
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> bins(int bins) {
        this.bins = Math.max(2, bins);
        return this;
    }

    /**
     * Sets the statistic the contours trace across the estimate:
     * {@code "density"} (default), {@code "ndensity"} (normalized to a maximum
     * of one per group), or {@code "count"} (density × number of observations).
     *
     * @param contourVar one of {@code "density"}, {@code "ndensity"}, {@code "count"}
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> contourVar(String contourVar) {
        this.contourVar = contourVar;
        return this;
    }

    /**
     * Turns on filled contour bands (the {@code Geoms.density2dFilled()}
     * rendering) instead of contour lines. Bands are coloured through the
     * configured colour ramp, nested inside-out so the highest-density core
     * sits on top.
     *
     * @param filled {@code true} to fill the bands between contour levels
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> filled(boolean filled) {
        this.filled = filled;
        return this;
    }

    /**
     * Sets the fill opacity of the contour bands (default {@code 0.5}).
     *
     * @param alpha fill opacity between 0 and 1
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> alpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Sets the stroke width of the contour lines (default {@code 0.5}).
     *
     * @param lineWidth the stroke width in pixels
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> lineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    /**
     * Sets the colour ramp used by the filled bands — one of {@code viridis},
     * {@code plasma}, {@code blues}, {@code greens}, {@code reds}, or
     * {@code greys} (default {@code viridis}).
     *
     * @param cmapName the colour ramp name
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> cmap(String cmapName) {
        this.cmapName = cmapName;
        return this;
    }

    /**
     * Sets the constant stroke color of the contour lines when no colour
     * column is mapped.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> color(Color c) {
        this.defaultStroke = c;
        return this;
    }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} so the contours lie flat on a face of a
     * 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code GeomDensity2d} instance for fluid chaining
     */
    public GeomDensity2d<DF> position(PositionAdjust position) {
        this.position = position;
        return this;
    }

    @Override
    public PositionAdjust positionAdjust() {
        return position;
    }

    /** {@return whether this layer draws filled contour bands instead of lines} */
    public boolean isFilled() {
        return filled;
    }

    /** {@return the colour ramp used by the filled bands, for the plot's guide} */
    public String getCmapName() {
        return cmapName;
    }

    /**
     * The maximum of the contoured statistic across all groups, the top of the
     * filled band's continuous colourbar.
     *
     * @param globalDf the full data frame across all panels
     * @param ext      the data extractor used to read the frame
     * @param aes      the aesthetic mapping
     * @return the maximum contoured statistic across all groups
     */
    public double maxStatistic(DF globalDf, DataExtractor<DF> ext, Aes aes) {
        if (aes.x() == null || aes.y() == null) {
            return 0.0;
        }
        var groups = groupPoints(globalDf, ext, aes);
        if (groups == null) {
            return 0.0;
        }
        var max = 0.0;
        for (var gv : groups.order) {
            max = Math.max(max, kde2d(groups.values.get(gv)).max());
        }
        return max;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var scales = ctx.plot().scales();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        if (aes.x() == null || aes.y() == null) {
            return;
        }
        var groups = groupPoints(df, ext, aes);
        if (groups == null) {
            return;
        }
        var groupCol = aes.color() != null ? aes.color() : aes.fill();
        var strokeScale = scales.colorScale(groupCol);

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var ramp = new ContinuousColorScale(cmapName);
        double levelMin = 0.0;
        double levelMax = 0.0;
        if (filled) {
            for (var gv : groups.order) {
                var g = kde2d(groups.values.get(gv));
                levelMax = Math.max(levelMax, g.max());
            }
        }

        for (var gv : groups.order) {
            var g = kde2d(groups.values.get(gv));
            var levels = contourLevels(g);
            var stroke = resolveStroke(strokeScale, gv);

            if (filled) {
                paintBands(gc, g, levels, ramp, levelMin, levelMax, sx, sy, coord);
            } else {
                paintLines(gc, g, levels, stroke, sx, sy, coord);
            }
        }

        gc.restore();
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        if (aes.x() == null || aes.y() == null) {
            return null;
        }
        double dataX = coord.isFlipped() ? sy.toData(mx) : sx.toData(mx);
        double dataY = coord.isFlipped() ? sx.toData(my) : sy.toData(my);
        if (Double.isNaN(dataX) || Double.isNaN(dataY)) {
            return null;
        }
        var groups = groupPoints(df, ext, aes);
        if (groups == null) {
            return null;
        }
        var groupCol = aes.color() != null ? aes.color() : aes.fill();
        for (var gv : groups.order) {
            var pts = groups.values.get(gv);
            var g = kde2d(pts);
            if (dataX < g.minX() || dataX > g.maxX() || dataY < g.minY() || dataY > g.maxY()) {
                continue;
            }
            var group = groupCol != null && gv != null ? String.valueOf(gv) : "";
            return String.format("%s\n2D density\nn = %d\nbandwidth = %,.2f, %,.2f",
                    group, pts.xs().size(), g.hX(), g.hY());
        }
        return null;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        // Per group roughly a few hundred contour segments per level
        return bins * 200;
    }

    // --- 2D kernel density ---------------------------------------------------

    /**
     * Groups the (x, y) point pairs by the mapped group column, in palette
     * order, skipping non-finite pairs and groups that cannot support a
     * bandwidth (fewer than two points).
     */
    private GroupedPoints groupPoints(DF df, DataExtractor<DF> ext, Aes aes) {
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var rowCount = ext.getRowCount(df);
        if (rowCount == 0) {
            return null;
        }
        var groupCol = aes.color() != null ? aes.color() : aes.fill();
        var rawGroup = groupCol != null ? ext.getColumn(df, groupCol) : null;

        var xs = new LinkedHashMap<Object, List<Double>>();
        var ys = new LinkedHashMap<Object, List<Double>>();
        var order = new ArrayList<Object>();
        for (var i = 0; i < rowCount; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx == null || ry == null) {
                continue;
            }
            var x = Values.toDouble(rx, Double.NaN);
            var y = Values.toDouble(ry, Double.NaN);
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                continue;
            }
            var gv = (rawGroup != null && i < rawGroup.size()) ? rawGroup.get(i) : null;
            if (!xs.containsKey(gv)) {
                xs.put(gv, new ArrayList<>());
                ys.put(gv, new ArrayList<>());
                order.add(gv);
            }
            xs.get(gv).add(x);
            ys.get(gv).add(y);
        }
        order.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
        var orderIt = order.listIterator();
        while (orderIt.hasNext()) {
            var gv = orderIt.next();
            if (xs.get(gv).size() < 2) {
                orderIt.remove();
            }
        }
        if (order.isEmpty()) {
            return null;
        }
        var groups = new LinkedHashMap<Object, PointGroup>();
        for (var gv : order) {
            groups.put(gv, new PointGroup(xs.get(gv), ys.get(gv)));
        }
        return new GroupedPoints(order, groups);
    }

    /**
     * a conventional two-dimensional Gaussian kernel estimate: the product kernel
     * summed on an {@code n × n} grid that spans the data range with three
     * bandwidths of margin, exactly like {@code kde2d()}.
     */
    private DensityGrid kde2d(PointGroup pts) {
        var xs = pts.xs();
        var ys = pts.ys();
        var m = xs.size();

        var hx = bandwidthNrd(xs) * adjustX;
        var hy = bandwidthNrd(ys) * adjustY;

        var minX = Collections.min(xs);
        var maxX = Collections.max(xs);
        var minY = Collections.min(ys);
        var maxY = Collections.max(ys);

        // A zero-variance column (all identical points) yields a zero
        // bandwidth, which would produce NaNs; floor it to a visible minimum.
        var spanX = maxX - minX;
        var spanY = maxY - minY;
        if (!(hx > 0)) {
            hx = Math.max(spanX, 1e-9) * 0.1;
        }
        if (!(hy > 0)) {
            hy = Math.max(spanY, 1e-9) * 0.1;
        }

        var loX = minX - 3 * hx;
        var hiX = maxX + 3 * hx;
        var loY = minY - 3 * hy;
        var hiY = maxY + 3 * hy;

        var nx = n;
        var ny = n;
        var gx = new double[nx];
        var gy = new double[ny];
        for (var i = 0; i < nx; i++) {
            gx[i] = loX + (hiX - loX) * i / (nx - 1);
        }
        for (var j = 0; j < ny; j++) {
            gy[j] = loY + (hiY - loY) * j / (ny - 1);
        }

        var z = new double[ny][nx];
        var norm = (1.0 / (2.0 * Math.PI)) * (1.0 / (hx * hy)) / m;
        var invHx = 1.0 / hx;
        var invHy = 1.0 / hy;

        for (var p = 0; p < m; p++) {
            var px = xs.get(p);
            var py = ys.get(p);
            // Weight per grid column and per grid row; the kernel separates.
            var wx = new double[nx];
            for (var i = 0; i < nx; i++) {
                var u = (gx[i] - px) * invHx;
                wx[i] = Math.abs(u) < KERNEL_CUTOFF ? Math.exp(-0.5 * u * u) : 0.0;
            }
            var wy = new double[ny];
            for (var j = 0; j < ny; j++) {
                var u = (gy[j] - py) * invHy;
                wy[j] = Math.abs(u) < KERNEL_CUTOFF ? Math.exp(-0.5 * u * u) : 0.0;
            }
            for (var j = 0; j < ny; j++) {
                var rowNormWeight = wy[j];
                if (rowNormWeight == 0.0) {
                    continue;
                }
                var row = z[j];
                for (var i = 0; i < nx; i++) {
                    row[i] += wx[i] * rowNormWeight;
                }
            }
        }
        for (var j = 0; j < ny; j++) {
            for (var i = 0; i < nx; i++) {
                z[j][i] *= norm;
            }
        }

        // The contoured statistic: density, density scaled to a maximum of one,
        // or density × number of observations.
        var maxZ = 0.0;
        for (var j = 0; j < ny; j++) {
            for (var i = 0; i < nx; i++) {
                maxZ = Math.max(maxZ, z[j][i]);
            }
        }
        switch (contourVar) {
            case "ndensity" -> {
                if (maxZ > 0) {
                    for (var j = 0; j < ny; j++) {
                        for (var i = 0; i < nx; i++) {
                            z[j][i] /= maxZ;
                        }
                    }
                }
            }
            case "count" -> {
                for (var j = 0; j < ny; j++) {
                    for (var i = 0; i < nx; i++) {
                        z[j][i] *= m;
                    }
                }
            }
            default -> {
            }
        }
        if ("ndensity".equals(contourVar)) {
            maxZ = 1.0;
        } else if ("count".equals(contourVar)) {
            maxZ *= m;
        }

        return new DensityGrid(z, gx, gy, hx, hy, maxZ, m);
    }

    /**
     * Silverman's rule-of-thumb bandwidth ("nrd0"), the convention the
     * reference estimator uses.
     */
    private double bandwidthNrd(List<Double> sorted) {
        var k = sorted.size();
        var sortedList = new ArrayList<>(sorted);
        Collections.sort(sortedList);
        var mean = 0.0;
        for (var v : sortedList) {
            mean += v;
        }
        mean /= k;
        var varSum = 0.0;
        for (var v : sortedList) {
            varSum += (v - mean) * (v - mean);
        }
        var sd = k > 1 ? Math.sqrt(varSum / (k - 1)) : 0.0;
        var iqr = quantile(sortedList, 0.75) - quantile(sortedList, 0.25);
        var bw = 1.06 * Math.min(sd, Math.abs(iqr) / 1.34) * Math.pow(k, -0.2);
        if (!(bw > 0) || Double.isNaN(bw)) {
            bw = Math.max(1.0, sd);
        }
        return bw;
    }

    private double quantile(List<Double> sortedList, double p) {
        var k = sortedList.size();
        if (k == 1) {
            return sortedList.get(0);
        }
        var h = (k - 1) * p;
        var lo = (int) Math.floor(h);
        var hi = (int) Math.ceil(h);
        var frac = h - lo;
        return sortedList.get(lo) + frac * (sortedList.get(hi) - sortedList.get(lo));
    }

    /**
     * Evenly spaced contour levels across the positive range of the estimate,
     * analogous to the default splitting the range with pretty breaks.
     */
    private double[] contourLevels(DensityGrid g) {
        var max = g.max();
        if (max <= 0) {
            return new double[0];
        }
        var levels = new double[bins];
        for (var l = 0; l < bins; l++) {
            levels[l] = max * (l + 1) / (double) bins;
        }
        return levels;
    }

    // --- Contour tracing -----------------------------------------------------

    private void paintLines(DrawSurface gc, DensityGrid g, double[] levels, Color stroke, Scale sx, Scale sy, Coord coord) {
        gc.setStroke(stroke);
        gc.setLineWidth(lineWidth);
        for (var level : levels) {
            for (var polyline : traceContours(g, level)) {
                gc.beginPath();
                for (var p = 0; p < polyline.size(); p++) {
                    var px = px(polyline.get(p).x(), polyline.get(p).y(), sx, sy, coord);
                    var py = py(polyline.get(p).x(), polyline.get(p).y(), sx, sy, coord);
                    if (p == 0) {
                        gc.moveTo(px, py);
                    } else {
                        gc.lineTo(px, py);
                    }
                }
                if (polyline.size() >= 2 && polyline.get(0).equals(polyline.get(polyline.size() - 1))) {
                    gc.closePath();
                }
                gc.stroke();
            }
        }
    }

    private void paintBands(DrawSurface gc, DensityGrid g, double[] levels, ContinuousColorScale ramp,
                            double levelMin, double levelMax, Scale sx, Scale sy, Coord coord) {
        if (levels.length == 0 || levelMax <= levelMin) {
            return;
        }
        // Outer first, inner last, so each narrower band paints over the one
        // around it and the highest-density core ends up on top.
        for (var l = 0; l < levels.length; l++) {
            var bandColor = ramp.colorFor(levels[l], levelMin, levelMax);
            gc.setFill(new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), alpha));
            for (var loop : traceContours(g, levels[l])) {
                if (loop.size() < 4 || !loop.get(0).equals(loop.get(loop.size() - 1))) {
                    continue; // open contours do not enclose a visible band
                }
                var px = new double[loop.size() - 1];
                var py = new double[loop.size() - 1];
                for (var p = 0; p < loop.size() - 1; p++) {
                    var pt = loop.get(p);
                    px[p] = px(pt.x(), pt.y(), sx, sy, coord);
                    py[p] = py(pt.x(), pt.y(), sx, sy, coord);
                }
                gc.fillPolygon(px, py, px.length);
            }
        }
    }

    /**
     * Traces the closed polylines where the density surface crosses
     * {@code level}, using marching squares with linear interpolation along
     * grid edges and a centre-based resolution of the ambiguous saddle cells.
     * Each polyline is a chain of data-space points; closed ones repeat their
     * first point at the end.
     */
    private List<List<Pt>> traceContours(DensityGrid g, double level) {
        var z = g.z();
        var nx = g.gx().length;
        var ny = g.gy().length;

        var segs = new ArrayList<Seg>();
        var edgeCache = new LinkedHashMap<Edge, Pt>();
        for (var j = 0; j < ny - 1; j++) {
            for (var i = 0; i < nx - 1; i++) {
                var v00 = z[j][i];
                var v10 = z[j][i + 1];
                var v11 = z[j + 1][i + 1];
                var v01 = z[j + 1][i];
                var above = (v00 > level ? 1 : 0) | (v10 > level ? 2 : 0)
                        | (v11 > level ? 4 : 0) | (v01 > level ? 8 : 0);

                // Edge endpoints: bottom (i,j)-(i+1,j), right (i+1,j)-(i+1,j+1),
                // top (i+1,j+1)-(i,j+1), left (i,j+1)-(i,j).
                var bottom = crossEdge(edgeCache, i, j, i + 1, j, v00, v10, level, g);
                var right = crossEdge(edgeCache, i + 1, j, i + 1, j + 1, v10, v11, level, g);
                var top = crossEdge(edgeCache, i + 1, j + 1, i, j + 1, v11, v01, level, g);
                var left = crossEdge(edgeCache, i, j + 1, i, j, v01, v00, level, g);

                switch (above) {
                    case 1, 14 -> segs.add(new Seg(bottom, left));
                    case 2, 13 -> segs.add(new Seg(bottom, right));
                    case 3, 12 -> segs.add(new Seg(left, right));
                    case 4, 11 -> segs.add(new Seg(right, top));
                    case 6, 9 -> segs.add(new Seg(bottom, top));
                    case 7, 8 -> segs.add(new Seg(top, left));
                    case 5 -> {
                        var center = (v00 + v10 + v11 + v01) / 4.0;
                        if (center > level) {
                            segs.add(new Seg(bottom, right));
                            segs.add(new Seg(left, top));
                        } else {
                            segs.add(new Seg(bottom, left));
                            segs.add(new Seg(right, top));
                        }
                    }
                    case 10 -> {
                        var center = (v00 + v10 + v11 + v01) / 4.0;
                        if (center > level) {
                            segs.add(new Seg(bottom, left));
                            segs.add(new Seg(right, top));
                        } else {
                            segs.add(new Seg(bottom, right));
                            segs.add(new Seg(left, top));
                        }
                    }
                    default -> {
                        // 0 and 15: no crossing
                    }
                }
            }
        }
        return chainSegments(segs);
    }

    private Pt crossEdge(Map<Edge, Pt> cache, int i0, int j0, int i1, int j1,
                         double v0, double v1, double level, DensityGrid g) {
        var edge = new Edge(Math.min(i0, i1), Math.min(j0, j1), Math.max(i0, i1), Math.max(j0, j1));
        var cached = cache.get(edge);
        if (cached != null) {
            return cached;
        }
        var t = (level - v0) / (v1 - v0);
        var x = g.gx()[i0] + t * (g.gx()[i1] - g.gx()[i0]);
        var y = g.gy()[j0] + t * (g.gy()[j1] - g.gy()[j0]);
        var pt = new Pt(x, y);
        cache.put(edge, pt);
        return pt;
    }

    /**
     * Joins contour segments into maximal polylines by following shared
     * endpoints. Closed loops appear with their first point repeated at the end.
     */
    private List<List<Pt>> chainSegments(List<Seg> segs) {
        var adjacency = new LinkedHashMap<Pt, List<Seg>>();
        for (var seg : segs) {
            adjacency.computeIfAbsent(seg.a(), k -> new ArrayList<>()).add(seg);
            adjacency.computeIfAbsent(seg.b(), k -> new ArrayList<>()).add(seg);
        }
        var used = new HashSet<Seg>();
        var polylines = new ArrayList<List<Pt>>();
        for (var seed : segs) {
            if (used.contains(seed)) {
                continue;
            }
            var line = new ArrayDeque<Pt>();
            line.addLast(seed.a());
            line.addLast(seed.b());
            used.add(seed);

            // Extend forward from the tail.
            var closed = false;
            var first = seed.a();
            var cur = seed.b();
            var guard = segs.size();
            while (cur != null && guard-- > 0) {
                var next = nextUnused(adjacency, used, cur, null);
                if (next == null) {
                    break;
                }
                if (next.equals(first) && !line.isEmpty()) {
                    closed = true;
                    break;
                }
                line.addLast(next);
                cur = next;
            }

            // Extend backward from the head (only if not already closed).
            if (!closed) {
                var head = line.getFirst();
                while (head != null) {
                    var prev = nextUnused(adjacency, used, head, null);
                    if (prev == null) {
                        break;
                    }
                    if (prev.equals(line.getLast())) {
                        closed = true;
                        break;
                    }
                    line.addFirst(prev);
                    head = prev;
                }
            }

            if (!closed) {
                // The seed itself may be the only segment still joining the
                // ends once every extension move is exhausted; any segment
                // directly linking the tail back to the head closes the loop.
                closed = isAdjacent(adjacency, line.getLast(), line.getFirst());
            }

            if (closed && !line.getFirst().equals(line.getLast())) {
                line.addLast(line.getFirst());
            }
            polylines.add(new ArrayList<>(line));
        }
        return polylines;
    }

    private Pt nextUnused(Map<Pt, List<Seg>> adjacency, Set<Seg> used, Pt at, Pt except) {
        var attached = adjacency.get(at);
        if (attached == null) {
            return null;
        }
        for (var seg : attached) {
            if (used.contains(seg)) {
                continue;
            }
            var other = seg.a().equals(at) ? seg.b() : seg.a();
            if (except != null && other.equals(except)) {
                continue;
            }
            used.add(seg);
            return other;
        }
        return null;
    }

    /** Whether any segment directly joins the two points. */
    private boolean isAdjacent(Map<Pt, List<Seg>> adjacency, Pt p, Pt q) {
        var attached = adjacency.get(p);
        if (attached == null) {
            return false;
        }
        for (var seg : attached) {
            var other = seg.a().equals(p) ? seg.b() : seg.a();
            if (other.equals(q)) {
                return true;
            }
        }
        return false;
    }

    private Color resolveStroke(DiscreteColorScale strokeScale, Object gv) {
        if (strokeScale != null && gv != null) {
            return strokeScale.colorFor(gv);
        }
        return defaultStroke;
    }

    private double px(double xd, double yd, Scale sx, Scale sy, Coord coord) {
        return coord.xPixel(sx, sy, xd, yd);
    }

    private double py(double xd, double yd, Scale sx, Scale sy, Coord coord) {
        return coord.yPixel(sx, sy, xd, yd);
    }

    /** A point's location in data space. */
    private record Pt(double x, double y) {
    }

    /** A contour segment between two grid-crossing points. */
    private record Seg(Pt a, Pt b) {
    }

    /** A grid edge, keyed by its two grid-point indices. */
    private record Edge(int i0, int j0, int i1, int j1) {
    }

    /** The raw points of one group. */
    private record PointGroup(List<Double> xs, List<Double> ys) {
    }

    /**
     * The point groups of a density2d layer, in legend/palette order.
     *
     * @param order  the distinct group values in draw order
     * @param values the point groups per group value
     */
    private record GroupedPoints(List<Object> order, Map<Object, PointGroup> values) {
    }

    /**
     * A 2D kernel-density estimate on a grid.
     *
     * @param z    the density at each grid cell, indexed {@code [y][x]}
     * @param gx   the grid x coordinates
     * @param gy   the grid y coordinates
     * @param hX   the effective x bandwidth (after {@code adjust})
     * @param hY   the effective y bandwidth (after {@code adjust})
     * @param max  the maximum of the contoured statistic
     * @param nObs the number of observations in the group
     */
    @SuppressWarnings("ArrayRecordComponent") // transient per-group grid, the arrays are read-only
    private record DensityGrid(double[][] z, double[] gx, double[] gy, double hX, double hY, double max, int nObs) {
        /** Smallest sampled grid coordinate (x). */
        double minX() {
            return gx[0];
        }

        /** Largest sampled grid coordinate (x). */
        double maxX() {
            return gx[gx.length - 1];
        }

        /** Smallest sampled grid coordinate (y). */
        double minY() {
            return gy[0];
        }

        /** Largest sampled grid coordinate (y). */
        double maxY() {
            return gy[gy.length - 1];
        }
    }
}
