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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.DensityModels;

/**
 * Density plot geometry layer implementation.
 * <p>
 * Computes and draws a Gaussian kernel-density estimate — the smoothed cousin
 * of a histogram — over a continuous variable, following the
 * {@code Geoms.density()} / {@code densityTransform()}. The estimate is drawn as a
 * filled area from the density axis baseline up to the curve, with an optional
 * outline along the curve (the default {@code outline.type = "upper"}).
 * <p>
 * The density axis is computed, not read from the data: map only
 * {@code aes(x)} (or only {@code aes(y)}) and the estimate runs along the other
 * axis, exactly like the orientation rules. Options mirror the reference
 * implementation:
 * <ul>
 *   <li>{@code adjust} — multiplicative bandwidth adjustment (default {@code 1})</li>
 *   <li>{@code n} — number of grid points the density is sampled at (default {@code 512})</li>
 *   <li>{@code trim} — cut the tails of each estimate at the data range (default {@code false})</li>
 *   <li>{@code bounds} — finite lower/upper bounds the estimate is reflected around</li>
 *   <li>{@code stat} — the computed y statistic: {@code "density"} (default),
 *       {@code "count"} (density × n, for stacked density plots), or {@code "scaled"}</li>
 *   <li>{@code position} — {@link Position#IDENTITY} (default), {@link Position#STACK},
 *       or {@link Position#FILL} for a conditional density</li>
 * </ul>
 * A colour/fill mapped column groups the data into one estimate per category.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomDensity<DF> implements Layer<DF> {

    /** Default grid resolution of each density profile (the {@code n = 512}). */
    public static final int GRID_POINTS = 512;

    private double adjust = 1.0;
    private int n = GRID_POINTS;
    private boolean trim = false;
    private double[] bounds = null;
    private String stat = "density";
    private Position position = Position.IDENTITY;
    private String outlineType = "upper";

    private Color defaultFill = Color.web("#3182bd");
    private Color defaultStroke = Color.web("#1c5480");
    private double alpha = 0.0;

    // A diagonal matrix cell renders the same full-dataset frame on every
    // redraw, and the KDE is recomputed per group per phase: expandDomain needs
    // the y maximum to grow the density axis, render draws the same curve, and
    // locate hit-tests against it. Memoize the group partition + estimates keyed
    // on the input frame and value range so a redraw reuses the previous fit
    // instead of re-sorting and re-convolving 54k rows three times. The key
    // includes the frame identity and the range (the estimate is computed over
    // the in-range values), so a different panel, facet, or axis range still
    // recomputes exactly as before.
    private DF memoDf;
    private double memoLo;
    private double memoHi;
    private GroupedValues memoGroups;
    private Map<Object, DensityEstimate> memoEstimates;

    /**
     * Memoized {@link #groupValues} + per-group {@link #estimate} for the given
     * frame and value range: identical inputs reuse the previous fit.
     */
    private MemoizedEstimates estimatesFor(DF df, DataExtractor<DF> ext, Aes aes, double lo, double hi) {
        if (Objects.equals(df, memoDf) && lo == memoLo && hi == memoHi && memoGroups != null) {
            return new MemoizedEstimates(memoGroups, memoEstimates);
        }
        var groups = groupValues(df, ext, aes, lo, hi);
        Map<Object, DensityEstimate> estimates;
        if (groups == null) {
            estimates = Map.of();
        } else {
            estimates = new java.util.HashMap<>();
            for (var gv : groups.order) {
                estimates.put(gv, estimate(groups.values.get(gv)));
            }
        }
        memoDf = df;
        memoLo = lo;
        memoHi = hi;
        memoGroups = groups;
        memoEstimates = estimates;
        return new MemoizedEstimates(groups, estimates);
    }

    private record MemoizedEstimates(GroupedValues groups, Map<Object, DensityEstimate> estimates) {
    }

    /**
     * Creates a density geometry with default estimation parameters.
     */
    public GeomDensity() {
    }

    /**
     * Sets the multiplicative bandwidth adjustment (default {@code 1.0}). A value
     * below one fits the estimate more closely to the data, a value above one
     * smooths it more heavily.
     *
     * @param adjust the bandwidth multiplier
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> adjust(double adjust) {
        this.adjust = adjust;
        return this;
    }

    /**
     * Sets the number of equally spaced grid points the density is estimated at
     * (default {@code 512}).
     *
     * @param n the grid resolution
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> n(int n) {
        this.n = Math.max(16, n);
        return this;
    }

    /**
     * If {@code true}, trims the tails of each estimate to the range of the data.
     * If {@code false} (default), the kernel tails extend up to three bandwidths
     * past the observed range, as in the {@code densityTransform(trim = FALSE)}.
     *
     * @param trim {@code true} to trim the tails to the data range
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> trim(boolean trim) {
        this.trim = trim;
        return this;
    }

    /**
     * Sets known lower and upper bounds for the estimated variable, e.g.
     * {@code bounds(1, Double.POSITIVE_INFINITY)} for a strictly positive
     * quantity. Any finite bound corrects the boundary bias of the default
     * estimator by reflecting the tails around it; data points outside the
     * bounds are removed, as in the {@code bounds} argument.
     *
     * @param lower the lower bound (or {@code -Inf} for none)
     * @param upper the upper bound (or {@code +Inf} for none)
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> bounds(double lower, double upper) {
        this.bounds = new double[]{lower, upper};
        return this;
    }

    /**
     * Sets the statistic the density axis plots, following the
     * {@code afterStat(...)} variables: {@code "density"} (default), the raw
     * kernel-density estimate; {@code "count"}, density × number of points —
     * the right statistic for a stacked density plot; and {@code "scaled"},
     * density normalized to a maximum of one.
     *
     * @param stat one of {@code "density"}, {@code "count"}, {@code "scaled"}
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> stat(String stat) {
        this.stat = stat;
        return this;
    }

    /**
     * Sets the position adjustment mode: {@link Position#IDENTITY} (default)
     * draws every estimate at full height, {@link Position#STACK} accumulates
     * the estimates on top of each other, and {@link Position#FILL} stacks them
     * and normalizes each slice so the total is one — a conditional density.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> position(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Sets which part of the area silhouette is stroked: {@code "upper"}
     * (default) draws the curve itself, {@code "lower"} the baseline,
     * {@code "both"} the two, and {@code "full"} a closed polygon around the area.
     *
     * @param outlineType one of {@code "upper"}, {@code "lower"}, {@code "both"}, {@code "full"}
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> outlineType(String outlineType) {
        this.outlineType = outlineType;
        return this;
    }

    /**
     * Sets the constant fill color of the density areas.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the stroke color of the density curves.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> color(Color c) {
        this.defaultStroke = c;
        return this;
    }

    /**
     * Sets the fill opacity of the density areas (default {@code 0.35}). A
     * value between 0 and 1 lets overlapping estimates shine through; the
     * outline always stays opaque.
     *
     * @param alpha fill opacity between 0 and 1
     * @return this {@code GeomDensity} instance for fluid chaining
     */
    public GeomDensity<DF> alpha(double alpha) {
        this.alpha = alpha;
        return this;
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
        var densityOnX = (aes.x() == null) != coord.isFlipped();
        var valueScale = densityOnX ? sy : sx;
        var densityScale = densityOnX ? sx : sy;

        var groups = estimatesFor(df, ext, aes, valueScale.minData(), valueScale.maxData());
        if (groups.groups() == null) {
            return;
        }
        var groupCol = aes.fill() != null ? aes.fill() : aes.color();

        var estimates = new ArrayList<DensityEstimate>();
        for (var gv : groups.groups().order) {
            estimates.add(groups.estimates().get(gv));
        }
        var count = groups.groups().order.size();
        if (count == 0) {
            return;
        }

        // Plotted y statistic per group, then the stacking bases
        var upper = new double[count][n];
        for (var g = 0; g < count; g++) {
            System.arraycopy(estimates.get(g).y, 0, upper[g], 0, n);
        }
        var base = new double[count][n];
        if (position == Position.STACK || position == Position.FILL) {
            var totals = new double[n];
            for (var g = 0; g < count; g++) {
                for (var i = 0; i < n; i++) {
                    base[g][i] = totals[i];
                    totals[i] += upper[g][i];
                }
            }
            if (position == Position.FILL) {
                for (var g = 0; g < count; g++) {
                    for (var i = 0; i < n; i++) {
                        var t = totals[i];
                        if (t > 0) {
                            base[g][i] /= t;
                            upper[g][i] = base[g][i] + upper[g][i] / t;
                        }
                    }
                }
            }
        }

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var px = new double[n * 2];
        var py = new double[n * 2];
        var polar = coord instanceof CoordPolar;
        for (var g = 0; g < count; g++) {
            var gv = groups.groups().order.get(g);
            var values = estimates.get(g).values;
            var stroke = resolveStroke(scales, groupCol, gv);
            var fill = resolveFill(scales, groupCol, aes.fill(), gv);

            // Forward along the curve, back along the baseline. When the estimate
            // runs along x the density value goes to px and the value axis to py.
            // A polar coordinate system wraps the same data-space polygon around
            // the disc: the value axis becomes the angle, the density the radius.
            for (var i = 0; i < n; i++) {
                var idx = n * 2 - 1 - i;
                var up = upper[g][i];
                var lo = base[g][i];
                if (densityOnX) {
                    if (polar) {
                        px[i] = coord.xPixel(sx, sy, up, values[i]);
                        py[i] = coord.yPixel(sx, sy, up, values[i]);
                        px[idx] = coord.xPixel(sx, sy, lo, values[i]);
                        py[idx] = coord.yPixel(sx, sy, lo, values[i]);
                    } else {
                        px[i] = densityScale.toPixel(up);
                        py[i] = valueScale.toPixel(values[i]);
                        px[idx] = densityScale.toPixel(lo);
                        py[idx] = valueScale.toPixel(values[i]);
                    }
                } else {
                    if (polar) {
                        px[i] = coord.xPixel(sx, sy, values[i], up);
                        py[i] = coord.yPixel(sx, sy, values[i], up);
                        px[idx] = coord.xPixel(sx, sy, values[i], lo);
                        py[idx] = coord.yPixel(sx, sy, values[i], lo);
                    } else {
                        px[i] = valueScale.toPixel(values[i]);
                        py[i] = densityScale.toPixel(up);
                        px[idx] = valueScale.toPixel(values[i]);
                        py[idx] = densityScale.toPixel(lo);
                    }
                }
            }

            gc.setFill(fill);
            gc.fillPolygon(px, py, n * 2);

            gc.setStroke(stroke);
            gc.setLineWidth(1.0);
            strokeOutline(gc, px, py);
        }

        gc.restore();
    }

    /**
     * Strokes the configured part of the area silhouette. The upper edge is the
     * first {@code n} vertices, the lower edge the reversed baseline from
     * {@code n} to {@code 2n - 1}.
     */
    private void strokeOutline(DrawSurface gc, double[] px, double[] py) {
        switch (outlineType) {
            case "both" -> {
                strokePolyline(gc, px, py, 0, n);
                strokePolyline(gc, px, py, n, n);
            }
            case "lower" -> strokePolyline(gc, px, py, n, n);
            case "full" -> gc.strokePolygon(px, py, n * 2);
            default -> strokePolyline(gc, px, py, 0, n);
        }
    }

    private void strokePolyline(DrawSurface gc, double[] px, double[] py, int start, int count) {
        for (var i = 0; i < count - 1; i++) {
            gc.strokeLine(px[start + i], py[start + i], px[start + i + 1], py[start + i + 1]);
        }
    }

    /**
     * Resolves the outline color for a group: the mapped colour when a colour or
     * fill column is mapped, otherwise the configured default stroke. The scale
     * is shared with the legend, so the layer and the legend always agree.
     */
    private Color resolveStroke(ResolvedScales<DF> scales, String groupCol, Object gv) {
        if (groupCol != null && gv != null) {
            var c = scales.resolvedColorFor(groupCol, gv);
            return c != null ? c : defaultStroke;
        }
        return defaultStroke;
    }

    /**
     * Resolves the fill color for a group: a translucent tint of the mapped
     * colour when only the colour is mapped, the mapped fill when a fill column
     * is mapped, and the configured default fill otherwise.
     */
    private Color resolveFill(ResolvedScales<DF> scales, String groupCol, String fillCol, Object gv) {
        var opacity = alpha > 0 ? alpha : 0.35;
        if (fillCol != null && gv != null) {
            var c = scales.resolvedColorFor(fillCol, gv);
            if (c != null) {
                return withOpacity(c, opacity);
            }
        }
        if (groupCol != null && gv != null) {
            // Colour mapped without a fill: a faint wash of the same hue
            var s = scales.resolvedColorFor(groupCol, gv);
            if (s != null) {
                return withOpacity(s, 0.15);
            }
        }
        return withOpacity(defaultFill, opacity);
    }

    private Color withOpacity(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    /**
     * Groups the values of the value aesthetic (restricted to the visible
     * {@code [lo, hi]} range of its axis, so the estimate reflects the data the
     * panel actually shows) by the mapped group column, in the palette order the
     * legend uses. Groups with fewer than two observations cannot support a
     * bandwidth and are skipped.
     */
    private GroupedValues groupValues(DF df, DataExtractor<DF> ext, Aes aes, double lo, double hi) {
        var valueCol = aes.x() != null ? aes.x() : aes.y();
        if (valueCol == null) {
            return null;
        }
        var rawValue = ext.getColumn(df, valueCol);
        var rowCount = ext.getRowCount(df);
        if (rowCount == 0) {
            return null;
        }
        var groupCol = aes.fill() != null ? aes.fill() : aes.color();
        var rawGroup = groupCol != null ? ext.getColumn(df, groupCol) : null;

        var groups = new LinkedHashMap<Object, List<Double>>();
        var order = new ArrayList<Object>();
        for (var i = 0; i < rowCount; i++) {
            var rv = rawValue.get(i);
            if (rv == null) {
                continue;
            }
            var v = Values.toDouble(rv, Double.NaN);
            if (!Double.isFinite(v) || v < lo || v > hi) {
                continue;
            }
            var gv = (rawGroup != null && i < rawGroup.size()) ? rawGroup.get(i) : null;
            if (!groups.containsKey(gv)) {
                groups.put(gv, new ArrayList<>());
                order.add(gv);
            }
            groups.get(gv).add(v);
        }
        order.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
        var orderIt = order.listIterator();
        while (orderIt.hasNext()) {
            var gv = orderIt.next();
            if (groups.get(gv).size() < 2) {
                orderIt.remove();
            }
        }
        if (order.isEmpty()) {
            return null;
        }
        return new GroupedValues(order, groups);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        var densityOnX = (aes.x() == null) != coord.isFlipped();
        var valueScale = densityOnX ? sy : sx;
        var densityScale = densityOnX ? sx : sy;

        var groups = estimatesFor(df, ext, aes, valueScale.minData(), valueScale.maxData());
        if (groups.groups() == null) {
            return null;
        }

        var px = new double[n * 2];
        var py = new double[n * 2];
        for (var gv : groups.groups().order) {
            var est = groups.estimates().get(gv);
            for (var i = 0; i < n; i++) {
                var vpix = valueScale.toPixel(est.values[i]);
                var upix = densityScale.toPixel(est.y[i]);
                var bpix = densityScale.toPixel(0.0);
                if (densityOnX) {
                    px[i] = upix;
                    py[i] = vpix;
                    px[n * 2 - 1 - i] = bpix;
                    py[n * 2 - 1 - i] = vpix;
                } else {
                    px[i] = vpix;
                    py[i] = upix;
                    px[n * 2 - 1 - i] = vpix;
                    py[n * 2 - 1 - i] = bpix;
                }
            }
            if (pointInPolygon(mx, my, px, py)) {
                return formatTooltip(gv, est);
            }
        }
        return null;
    }

    private String formatTooltip(Object gv, DensityEstimate est) {
        var group = gv == null ? "" : String.valueOf(gv);
        return String.format("%s\nn = %d\nbandwidth = %,.2f\npeak = %,.3f",
                group, est.nObs(), est.bw(), est.maxY());
    }

    /**
     * Ray-casting point-in-polygon test for hover hit-testing.
     */
    private boolean pointInPolygon(double mx, double my, double[] px, double[] py) {
        var inside = false;
        int vertices = px.length;
        for (int i = 0, j = vertices - 1; i < vertices; j = i++) {
            if (((py[i] > my) != (py[j] > my))
                    && (mx < (px[j] - px[i]) * (my - py[i]) / (py[j] - py[i]) + px[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx, boolean xDiscrete, boolean yDiscrete) {
        var aes = ctx.aes();
        var coord = ctx.coord();
        var extractor = ctx.extractor();
        var df = ctx.globalDf();
        var densityOnX = (aes.x() == null) != coord.isFlipped();
        var valueLo = densityOnX ? bounds.yMin() : bounds.xMin();
        var valueHi = densityOnX ? bounds.yMax() : bounds.xMax();

        var groups = estimatesFor(df, extractor, aes, valueLo, valueHi);
        if (groups.groups() == null) {
            return bounds;
        }

        var maxY = 0.0;
        if (position == Position.FILL) {
            maxY = 1.0;
        } else if (position == Position.STACK) {
            var totals = new double[n];
            for (var gv : groups.groups().order) {
                var y = groups.estimates().get(gv).y;
                for (var i = 0; i < n; i++) {
                    totals[i] += y[i];
                }
            }
            for (var i = 0; i < n; i++) {
                maxY = Math.max(maxY, totals[i]);
            }
        } else {
            for (var gv : groups.groups().order) {
                var y = groups.estimates().get(gv).y;
                for (var i = 0; i < n; i++) {
                    maxY = Math.max(maxY, y[i]);
                }
            }
        }
        if (!(maxY > 0)) {
            return bounds;
        }
        // The density axis is computed, not read from a mapped column: when it
        // has no column of its own (a plain density plot, or the diagonal cell
        // of a pairs matrix) it *defines* the axis from zero to the tallest
        // estimate instead of being widended against the plot's neutral default
        // range. A mapped value axis simply keeps its data-derived bounds.
        var densityAxisMapped = densityOnX ? aes.x() != null : aes.y() != null;
        if (densityOnX) {
            if (densityAxisMapped) {
                return new Bounds(bounds.xMin(), Math.max(bounds.xMax(), maxY), bounds.yMin(), bounds.yMax());
            }
            return new Bounds(bounds.xMin(), maxY, bounds.yMin(), bounds.yMax());
        }
        if (densityAxisMapped) {
            return new Bounds(bounds.xMin(), bounds.xMax(), bounds.yMin(), Math.max(bounds.yMax(), maxY));
        }
        return new Bounds(bounds.xMin(), bounds.xMax(), 0.0, maxY);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        // One ~2n-vertex polygon (and its outline) per group, regardless of input size
        return n * 2;
    }

    /**
     * Estimates the Gaussian kernel density of a group, sampled on a fixed grid.
     * The bandwidth follows Silverman's rule of thumb ({@code Silverman's rule of thumb})
     * multiplied by {@code adjust}. Finite {@link #bounds} reflect the data
     * around the boundary to correct the tail bias of the plain estimator.
     */
    private DensityEstimate estimate(List<Double> data) {
        var sorted = new ArrayList<>(data);
        Collections.sort(sorted);
        var nObs = sorted.size();
        var min = sorted.get(0);
        var max = sorted.get(nObs - 1);
        var bw = DensityModels.bandwidthNrd0(sorted) * adjust;

        double lo, hi;
        if (bounds != null) {
            lo = Double.isFinite(bounds[0]) ? bounds[0] : (trim ? min : min - 3 * bw);
            hi = Double.isFinite(bounds[1]) ? bounds[1] : (trim ? max : max + 3 * bw);
        } else {
            lo = trim ? min : min - 3 * bw;
            hi = trim ? max : max + 3 * bw;
        }
        if (!(hi > lo)) {
            hi = lo + 1.0;
        }

        // Reflect points about finite bounds; points outside are removed.
        List<Double> kernelPoints = sorted;
        if (bounds != null && (Double.isFinite(bounds[0]) || Double.isFinite(bounds[1]))) {
            var reflected = new ArrayList<Double>();
            for (var v : sorted) {
                if (Double.isFinite(bounds[0]) && v < bounds[0]) continue;
                if (Double.isFinite(bounds[1]) && v > bounds[1]) continue;
                reflected.add(v);
                if (Double.isFinite(bounds[0])) reflected.add(2 * bounds[0] - v);
                if (Double.isFinite(bounds[1])) reflected.add(2 * bounds[1] - v);
            }
            kernelPoints = reflected;
        }
        if (kernelPoints.isEmpty()) {
            kernelPoints = List.of(0.0);
        }

        var grid = n;
        var values = new double[grid];
        var density = new double[grid];
        var step = (hi - lo) / (grid - 1);
        var invBw = 1.0 / bw;
        var norm = 1.0 / Math.sqrt(2.0 * Math.PI);
        var kernelNorm = norm * invBw / kernelPoints.size();

        // Linear binning, the same scheme a density estimator uses: every observation
        // spreads its weight onto the two neighbouring grid samples, and the
        // estimate is the convolution of those bin weights with the kernel.
        // This turns the KDE from O(points × grid) into O(points + grid²),
        // independent of the input size, so a full-sized dataset does not force
        // tens of millions of exp() calls per panel.
        var weights = new double[grid];
        for (var d : kernelPoints) {
            var pos = (d - lo) / step;
            var j = (int) pos;
            if (j < 0 || j > grid - 1) {
                continue;
            }
            if (j == grid - 1) {
                weights[grid - 1] += 1.0;
                continue;
            }
            var frac = pos - j;
            weights[j] += 1.0 - frac;
            weights[j + 1] += frac;
        }
        for (var i = 0; i < grid; i++) {
            var v = lo + i * step;
            values[i] = v;
            var sum = 0.0;
            for (var j = 0; j < grid; j++) {
                var w = weights[j];
                if (w == 0.0) {
                    continue;
                }
                var u = (i - j) * step * invBw;
                sum += w * Math.exp(-0.5 * u * u);
            }
            density[i] = sum * kernelNorm;
        }

        // The plotted statistic: density, density × n (count), or density scaled
        var y = new double[grid];
        var maxY = 0.0;
        for (var i = 0; i < grid; i++) {
            y[i] = "count".equals(stat) ? density[i] * nObs : density[i];
            maxY = Math.max(maxY, y[i]);
        }
        if ("scaled".equals(stat) && maxY > 0) {
            for (var i = 0; i < grid; i++) {
                y[i] /= maxY;
            }
            maxY = 1.0;
        }

        return new DensityEstimate(values, y, maxY, bw, nObs, Collections.unmodifiableList(sorted));
    }

    /**
     * The value groups of a density layer, in legend/palette order.
     *
     * @param order  the distinct group values in draw order
     * @param values the raw numeric values per group value
     */
    private record GroupedValues(List<Object> order, Map<Object, List<Double>> values) {
    }

    /**
     * Immutable kernel-density profile for one group.
     *
     * @param values the grid data values the density is sampled at
     * @param y      the plotted statistic per grid sample (density/count/scaled)
     * @param maxY   the maximum of {@code y}
     * @param bw     the effective bandwidth used (after {@code adjust})
     * @param nObs   the number of observations in the group
     * @param sorted the group's raw values in ascending order
     */
    @SuppressWarnings("ArrayRecordComponent") // transient per-group estimate, never mutated post-construction
    private record DensityEstimate(double[] values, double[] y, double maxY, double bw, int nObs, List<Double> sorted) {
    }
}
