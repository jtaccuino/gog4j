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

import java.util.function.DoubleUnaryOperator;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Draws a mathematical function as a continuous curve over the x axis
 * ({@code Geoms.function()} in the reference implementation).
 * <p>
 * The underlying mechanism splits the visible x range into {@link #n(int)}
 * evenly spaced points, evaluates the function at each one, and connects the
 * results with a line — the same idea as {@link GeomSmooth} and
 * {@link GeomAbline}, and what makes the layer work on a transformed or polar
 * axis. It carries no data of its own, so it is ideal for superimposing a
 * known curve on top of an existing plot.
 * <p>
 * By convention, {@link #xlim(double, double)} only scopes the range the
 * function is evaluated and drawn over — it never changes the plot's axis. The
 * x axis itself comes from the mapped data, or, on a data-less plot, from the
 * plot-level coordinate limits (e.g. {@code Coord2D.cartesian().xlim(...)}).
 *
 * @param <DF> the DataFrame type
 */
public class GeomFunction<DF> implements Layer<DF> {

    /** Default number of evaluation points, mirroring the function layer's {@code n = 101}. */
    private static final int DEFAULT_POINTS = 101;

    private final DoubleUnaryOperator fun;
    private MinMax xlim;
    private int n = DEFAULT_POINTS;
    private Color color = Color.web("#333333");
    private Color assignedColor;
    private double width = 1.5;

    /**
     * Creates a function layer for the given (vectorised) function.
     *
     * @param fun the function to draw, mapping an x value to a y value
     */
    public GeomFunction(DoubleUnaryOperator fun) {
        this.fun = fun;
    }

    /**
     * Scopes the range the function is evaluated and drawn over, mirroring
     * {@code Geoms.function(fun, xlim = c(min, max))}. Unlike in a naive reading,
     * this does not size the plot's axis — the axis is governed by the mapped
     * data or, on a data-less plot, by the coordinate system's limits.
     * <p>
     * On a data-bearing plot a narrower range restricts the drawn curve within
     * the data window; a wider range draws the curve across the whole window
     * and fits the y axis to the function's values over the wider range.
     *
     * @param min the lower x bound
     * @param max the upper x bound
     * @return this instance for fluid chaining
     */
    public GeomFunction<DF> xlim(double min, double max) {
        this.xlim = new MinMax(min, max);
        return this;
    }

    /**
     * Sets the number of points the function is evaluated at along the x axis.
     *
     * @param n the number of interpolation points (default 101)
     * @return this instance for fluid chaining
     */
    public GeomFunction<DF> n(int n) {
        this.n = Math.max(2, n);
        return this;
    }

    /**
     * Sets the stroke colour of the curve.
     *
     * @param color the curve colour
     * @return this instance for fluid chaining
     */
    public GeomFunction<DF> color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Assigns the palette colour resolved for this curve's constant colour
     * label. When a colour is assigned it takes precedence over the default
     * stroke {@linkplain #color(Color)}
     *
     * @param color the palette colour for the curve's label, or {@code null} to
     *              fall back to the default stroke
     * @return this instance for fluid chaining
     */
    public GeomFunction<DF> setAssignedColor(Color color) {
        this.assignedColor = color;
        return this;
    }

    /**
     * Sets the stroke width of the curve in pixels.
     *
     * @param width the stroke width
     * @return this instance for fluid chaining
     */
    public GeomFunction<DF> width(double width) {
        this.width = width;
        return this;
    }

    /**
     * The function's explicitly requested x range, or {@code null} when it
     * follows the panel's x axis.
     *
     * @return the {@code [min, max]} range, or {@code null}
     */
    public MinMax xlim() {
        return xlim;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        if (sx.isDiscrete()) {
            return;
        }
        var lo = sx.minData();
        var hi = sx.maxData();
        if (xlim != null) {
            lo = Math.max(lo, xlim.min());
            hi = Math.min(hi, xlim.max());
        }
        if (hi <= lo) {
            return;
        }

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var stroke = assignedColor != null ? assignedColor : color;
        gc.setStroke(stroke);
        gc.setLineWidth(width);
        gc.beginPath();
        var first = true;
        for (var i = 0; i < n; i++) {
            var x = lo + (hi - lo) * i / (n - 1);
            var y = evaluate(x);
            if (!Double.isFinite(y)) {
                continue;
            }
            var px = coord.xPixel(sx, sy, x, y);
            var py = coord.yPixel(sx, sy, x, y);
            if (first) {
                gc.moveTo(px, py);
                first = false;
            } else {
                gc.lineTo(px, py);
            }
        }
        gc.stroke();

        gc.restore();
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        if (xDiscrete) {
            return bounds;
        }
        // The function only ever contributes to the y domain: it widens the y
        // axis to fit its values. The x axis is left entirely to the mapped
        // data, or to the plot's coordinate limits on a data-less plot. When a
        // geom xlim is set, the y axis is fitted over that whole range (not
        // clipped to the panel) so a wider range lets the y axis accommodate
        // the function's tails, as the reference examples do.
        var lo = xlim != null ? xlim.min() : bounds.xMin();
        var hi = xlim != null ? xlim.max() : bounds.xMax();
        if (hi <= lo) {
            return bounds;
        }
        var fitted = evaluateRange(lo, hi);
        if (!fitted.finite) {
            return bounds;
        }
        // On a data-less plot (no y mapped) the function is the sole y source,
        // so its fitted range *defines* the y axis rather than being unioned
        // with a neutral seed. On a data-bearing plot it merely widens the axis
        // to also fit the curve.
        if (ctx.aes().y() == null) {
            return new Bounds(bounds.xMin(), bounds.xMax(), fitted.min, fitted.max);
        }
        return new Bounds(bounds.xMin(), bounds.xMax(),
                Math.min(bounds.yMin(), fitted.min), Math.max(bounds.yMax(), fitted.max));
    }

    private record FittedRange(double min, double max, boolean finite) {
    }

    private FittedRange evaluateRange(double lo, double hi) {
        var yMin = Double.POSITIVE_INFINITY;
        var yMax = Double.NEGATIVE_INFINITY;
        for (var i = 0; i < n; i++) {
            var y = evaluate(lo + (hi - lo) * i / (n - 1));
            if (Double.isFinite(y)) {
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        return new FittedRange(yMin, yMax,
                Double.isFinite(yMin) && Double.isFinite(yMax));
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        if (sx.isDiscrete()) {
            return null;
        }
        var lo = sx.minData();
        var hi = sx.maxData();
        if (xlim != null) {
            lo = Math.max(lo, xlim.min());
            hi = Math.min(hi, xlim.max());
        }
        if (hi <= lo) {
            return null;
        }
        var tolerance = 12.0;
        var bestDistance = Double.MAX_VALUE;
        double bestX = lo;
        double bestY = Double.NaN;
        for (var i = 0; i < n; i++) {
            var x = lo + (hi - lo) * i / (n - 1);
            var y = evaluate(x);
            if (!Double.isFinite(y)) {
                continue;
            }
            var cx = coord.xPixel(sx, sy, x, y);
            var cy = coord.yPixel(sx, sy, x, y);
            var distance = Math.hypot(mx - cx, my - cy);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestX = x;
                bestY = y;
            }
        }
        if (bestDistance > tolerance || !Double.isFinite(bestY)) {
            return null;
        }
        return "x = " + Scale.formatTick(bestX)
                + "\nf(x) = " + Scale.formatTick(bestY);
    }

    private double evaluate(double x) {
        try {
            return fun.applyAsDouble(x);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return n;
    }
}
