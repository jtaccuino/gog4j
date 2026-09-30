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

import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Reference line geometry defined by a slope and an intercept in data space
 * ({@code Geoms.abline}). The identity line {@code slope = 1, intercept = 0} is
 * the null expectation of a quantile-quantile plot.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomAbline<DF> implements Layer<DF> {

    /** Segments used to approximate the line; enough to stay smooth on a transformed axis. */
    private static final int SAMPLES = 120;

    private final Double slope;
    private final Double intercept;
    private DF data;
    private String slopeColumn;
    private String interceptColumn;
    private String colorColumn = null;
    private Color color = Color.web("#666666");
    private double width = 1.0;
    private double[] dashes = null;

    /** Whether hovering the line surfaces a tooltip; off by default. */
    private boolean tooltipEnabled = false;

    /**
     * Creates a reference line from constant slope and intercept values.
     *
     * @param slope     the slope in data units
     * @param intercept the value of Y where X is zero
     */
    public GeomAbline(double slope, double intercept) {
        this.slope = slope;
        this.intercept = intercept;
        this.data = null;
        this.slopeColumn = null;
        this.interceptColumn = null;
    }

    /**
     * Creates a bare builder instance; configure it via
     * {@link #data(Object)}, {@link #slope(String)} and {@link #intercept(String)}.
     */
    public GeomAbline() {
        this.slope = null;
        this.intercept = null;
        this.data = null;
        this.slopeColumn = null;
        this.interceptColumn = null;
    }

    /**
     * Creates a data-driven reference line that reads its slope and intercept
     * values from columns of the given frame, drawing one line per row. Under a
     * facet the frame's own rows are matched to the current panel by the facet
     * column, so each panel can draw its own lines.
     *
     * @param data            the frame holding the line parameters
     * @param slopeColumn     the column holding the slope values
     * @param interceptColumn the column holding the intercept values
     */
    public GeomAbline(DF data, String slopeColumn, String interceptColumn) {
        this.slope = null;
        this.intercept = null;
        this.data = data;
        this.slopeColumn = slopeColumn;
        this.interceptColumn = interceptColumn;
    }

    /**
     * Maps the line colour to a column of the data-driven frame, mirroring
     * {@code colourMapping} in the reference implementation. Numeric columns use the continuous
     * ramp; categorical columns use the palette.
     *
     * @param column the column to map colour from
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> color(String column) {
        this.colorColumn = column;
        return this;
    }

    /**
     * Returns the column mapped to line colour, or {@code null} when the line
     * uses a constant colour. Lets the plot render a legend for a data-driven
     * reference line.
     *
     * @return the colour column name, or {@code null}
     */
    public String colorColumn() {
        return colorColumn;
    }

    /**
     * Returns the data frame the line parameters are read from, or {@code null}
     * for a constant line. Lets the plot derive the legend range.
     *
     * @return the data frame, or {@code null}
     */
    public DF data() {
        return data;
    }

    /**
     * Associates the data frame the line parameters are read from.
     *
     * @param data frame holding the slope and intercept values
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> data(DF data) {
        this.data = data;
        return this;
    }

    /**
     * Names the column of {@link #data} holding the slope values.
     *
     * @param column the column name
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> slope(String column) {
        this.slopeColumn = column;
        return this;
    }

    /**
     * Names the column of {@link #data} holding the intercept values.
     *
     * @param column the column name
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> intercept(String column) {
        this.interceptColumn = column;
        return this;
    }

    /**
     * Sets the line colour.
     *
     * @param color the stroke colour
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the stroke width in pixels.
     *
     * @param width the stroke width
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Renders the line dashed rather than solid.
     *
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> dashed() {
        this.dashes = new double[]{6.0, 4.0};
        return this;
    }

    /**
     * Enables or disables a hover tooltip on this reference line. Disabled by
     * by default.
     *
     * @param enabled {@code true} to surface the line's equation on hover
     * @return this instance for fluid chaining
     */
    public GeomAbline<DF> tooltip(boolean enabled) {
        this.tooltipEnabled = enabled;
        return this;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (this.data != null) {
            renderDataDriven(gc, ctx);
        } else {
            renderFixed(gc, sx, sy, ctx.plot().coord());
        }

        gc.setLineDashes(null);
        gc.restore();
    }

    private void renderDataDriven(DrawSurface gc, PanelContext<DF> ctx) {
        var ext = ctx.plot().extractor();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        // Restrict the layer's own rows to the current facet panel, if any.
        // As usual, a summary frame without the facet column draws in every panel.
        DF frame = PanelFacet.restrict(data, ext, ctx.facetValues());
        if (frame == null) return;

        var slopes = ext.getColumn(frame, slopeColumn);
        var intercepts = ext.getColumn(frame, interceptColumn);
        var n = ext.getRowCount(frame);
        if (n == 0) return;

        var rawColor = colorColumn != null ? ext.getColumn(frame, colorColumn) : null;
        var continuous = colorColumn != null
                && ext.columnType(frame, colorColumn) == DataExtractor.ColumnType.NUMBER;
        MinMax colorRange = continuous ? expandColorRange(ext.getMinMax(data, colorColumn)) : null;

        for (var i = 0; i < n; i++) {
            var sv = slopes.get(i);
            var iv = intercepts.get(i);
            if (!(sv instanceof Number slopeNum) || !(iv instanceof Number interceptNum)) continue;

            var lineColor = color;
            if (rawColor != null && i < rawColor.size() && rawColor.get(i) != null) {
                lineColor = continuous
                        ? new ContinuousColorScale().colorFor(
                                ((Number) rawColor.get(i)).doubleValue(), colorRange.min(), colorRange.max())
                        : Scale.resolveGlobalColor(data, ext, colorColumn, rawColor.get(i), ctx.plot().scaleSpec(),
                                ctx.plot().theme().categoricalPalette(), ctx.plot().theme().fallbackColor());
            }

            strokeLine(gc, slopeNum.doubleValue(), interceptNum.doubleValue(), lineColor, sx, sy, coord);
        }
    }

    private static MinMax expandColorRange(MinMax bounds) {
        var range = bounds.max() - bounds.min();
        var pad = range > 0 ? range * 0.05 : 1.0;
        return new MinMax(bounds.min() - pad, bounds.max() + pad);
    }

    private void renderFixed(DrawSurface gc, Scale sx, Scale sy, Coord coord) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        if (dashes != null) {
            gc.setLineDashes(dashes);
        }
        strokeLine(gc, slope, intercept, color, sx, sy, coord);
    }

    private void strokeLine(DrawSurface gc, double slope, double intercept, Color lineColor,
                            Scale sx, Scale sy, Coord coord) {
        gc.setStroke(lineColor);
        gc.setLineWidth(width);
        if (dashes != null) {
            gc.setLineDashes(dashes);
        }

        // Sample across the visible x-range rather than joining the two ends: on a
        // transformed axis the line is straight in data space but curved in pixels.
        // Routing each sample through the coordinate system makes the same curve
        // wind around a polar disc instead of staying a straight Cartesian line.
        var x0 = sx.minData();
        var x1 = sx.maxData();
        var steps = SAMPLES;
        gc.beginPath();
        for (var i = 0; i <= steps; i++) {
            var x = x0 + (x1 - x0) * i / steps;
            var px = coord.xPixel(sx, sy, x, intercept + slope * x);
            var py = coord.yPixel(sx, sy, x, intercept + slope * x);
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        if (!tooltipEnabled || coord instanceof CoordPolar) {
            return null;
        }
        double band = Math.max(4.0, width / 2.0 + 3.0);
        if (this.data != null) {
            DF frame = PanelFacet.restrict(this.data, ext, ctx.facetValues());
            if (frame == null) {
                return null;
            }
            var slopes = ext.getColumn(frame, slopeColumn);
            var intercepts = ext.getColumn(frame, interceptColumn);
            var n = ext.getRowCount(frame);
            for (var i = 0; i < n; i++) {
                var sv = slopes.get(i);
                var iv = intercepts.get(i);
                if (!(sv instanceof Number slopeNum) || !(iv instanceof Number interceptNum)) {
                    continue;
                }
                if (near(mx, my, slopeNum.doubleValue(), interceptNum.doubleValue(), band, sx, sy, coord)) {
                    return equation(slopeNum.doubleValue(), interceptNum.doubleValue());
                }
            }
            return null;
        }
        if (slope == null || intercept == null) {
            return null;
        }
        return near(mx, my, slope, intercept, band, sx, sy, coord)
                ? equation(slope, intercept) : null;
    }

    /** Whether the mouse sits within {@code band} pixels of the sampled line. */
    private static boolean near(double mx, double my, double slope, double intercept, double band,
                                Scale sx, Scale sy, Coord coord) {
        if (coord.isFlipped()) {
            // Inverting the line is awkward when flipped; walk the sampled segment
            // points instead and take the smallest perpendicular distance.
            return minSegmentDistance(mx, my, slope, intercept, sx, sy, coord) <= band;
        }
        var x = sx.toData(mx);
        var y = intercept + slope * x;
        var py = sy.toPixel(y);
        return Math.abs(my - py) <= band;
    }

    /** Perpendicular distance from the mouse to the closest sampled segment. */
    private static double minSegmentDistance(double mx, double my, double slope, double intercept,
                                             Scale sx, Scale sy, Coord coord) {
        var x0 = sx.minData();
        var x1 = sx.maxData();
        double best = Double.MAX_VALUE;
        double pxPrev = 0.0, pyPrev = 0.0;
        for (var i = 0; i <= SAMPLES; i++) {
            var x = x0 + (x1 - x0) * i / SAMPLES;
            var px = coord.xPixel(sx, sy, x, intercept + slope * x);
            var py = coord.yPixel(sx, sy, x, intercept + slope * x);
            if (i > 0) {
                best = Math.min(best, Math.sqrt(
                        PolygonMath.distanceSqToSegment(mx, my, pxPrev, pyPrev, px, py)));
            }
            pxPrev = px;
            pyPrev = py;
        }
        return best;
    }

    private static String equation(double slope, double intercept) {
        return "y = " + Scale.formatTick(intercept) + " + "
                + Scale.formatTick(slope) + "·x";
    }
}
