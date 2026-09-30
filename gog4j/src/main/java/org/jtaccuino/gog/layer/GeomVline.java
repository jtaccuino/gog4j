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

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Reference line geometry drawn at a constant value of the X aesthetic
 * ({@code Geoms.vline}). Typical uses are a target date, a cut-off, or a
 * reference observation.
 * <p>
 * Under {@link Coord#isFlipped() flipped} coordinates the line follows the
 * data axis, and is therefore drawn horizontally.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomVline<DF> implements Layer<DF> {

    /** Semi-opaque backdrop drawn behind the caption so it survives dense data. */
    private static final Color HALO = Color.web("#ffffff", 0.85);

    private final Double xIntercept;
    private DF data;
    private String xColumn;
    private String colorColumn = null;
    private Color color = Color.web("#d62728");
    private double width = 1.0;
    private double[] dashes = null;
    private String annotation = null;

    /** Whether hovering the line surfaces a tooltip; off by default. */
    private boolean tooltipEnabled = false;

    /**
     * Creates a vertical reference line at a constant data-space X value.
     *
     * @param xIntercept the data-space X value at which the line is drawn
     */
    public GeomVline(double xIntercept) {
        this.xIntercept = xIntercept;
        this.data = null;
        this.xColumn = null;
    }

    /**
     * Creates a bare builder instance; configure it via
     * {@link #data(Object)} and {@link #xintercept(String)}.
     */
    public GeomVline() {
        this.xIntercept = null;
        this.data = null;
        this.xColumn = null;
    }

    /**
     * Creates a data-driven vertical reference line that reads its X intercept
     * values from a column of the given frame, drawing one line per row. Under a
     * facet the frame's own rows are matched to the current panel by the facet
     * column, so each panel can draw its own lines.
     *
     * @param data     the frame holding the intercept values
     * @param xColumn  the column holding the X intercept values
     */
    public GeomVline(DF data, String xColumn) {
        this.xIntercept = null;
        this.data = data;
        this.xColumn = xColumn;
    }

    /**
     * Maps the line colour to a column of the data-driven frame, mirroring
     * {@code colourMapping} in the reference implementation. Numeric columns use the continuous
     * ramp; categorical columns use the palette.
     *
     * @param column the column to map colour from
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> color(String column) {
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
     * Returns the data frame the intercept values are read from, or {@code null}
     * for a constant line. Lets the plot derive the legend range.
     *
     * @return the data frame, or {@code null}
     */
    public DF data() {
        return data;
    }

    /**
     * Associates the data frame the intercept values are read from.
     *
     * @param data frame holding the X intercept values
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> data(DF data) {
        this.data = data;
        return this;
    }

    /**
     * Names the column of {@link #data} holding the X intercept values.
     *
     * @param column the column name
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> xintercept(String column) {
        this.xColumn = column;
        return this;
    }

    /**
     * Sets the line colour.
     *
     * @param color the stroke colour
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the stroke width in pixels.
     *
     * @param width the stroke width
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Renders the line dashed rather than solid.
     *
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> dashed() {
        return dashes(6.0, 4.0);
    }

    /**
     * Sets an explicit dash pattern.
     *
     * @param pattern alternating on/off segment lengths in pixels
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> dashes(double... pattern) {
        this.dashes = pattern;
        return this;
    }

    /**
     * Adds a short caption drawn at the top of the line, inside the panel.
     *
     * @param text the caption, or {@code null} for none
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> annotate(String text) {
        this.annotation = text;
        return this;
    }

    /**
     * Enables or disables a hover tooltip on this reference line. Disabled by
     * by default.
     *
     * @param enabled {@code true} to surface the line's value on hover
     * @return this instance for fluid chaining
     */
    public GeomVline<DF> tooltip(boolean enabled) {
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

        var intercepts = ext.getColumn(frame, xColumn);
        var n = ext.getRowCount(frame);
        if (n == 0) return;

        var rawColor = colorColumn != null ? ext.getColumn(frame, colorColumn) : null;
        var continuous = colorColumn != null
                && ext.columnType(frame, colorColumn) == DataExtractor.ColumnType.NUMBER;
        // The colour range comes from the whole summary frame, not the panel subset,
        // so the gradient stays stable across facets — as the default's scale does.
        MinMax colorRange = continuous ? expandColorRange(ext.getMinMax(data, colorColumn)) : null;

        for (var i = 0; i < n; i++) {
            var v = intercepts.get(i);
            if (!(v instanceof Number num)) continue;

            var lineColor = color;
            if (rawColor != null && i < rawColor.size() && rawColor.get(i) != null) {
                lineColor = continuous
                        ? new ContinuousColorScale().colorFor(
                                ((Number) rawColor.get(i)).doubleValue(), colorRange.min(), colorRange.max())
                        : Scale.resolveGlobalColor(data, ext, colorColumn, rawColor.get(i), ctx.plot().scaleSpec(),
                                ctx.plot().theme().categoricalPalette(), ctx.plot().theme().fallbackColor());
            }

            gc.setStroke(lineColor);
            gc.setLineWidth(width);
            if (dashes != null && dashes.length > 0) {
                gc.setLineDashes(dashes);
            }
            if (coord instanceof CoordPolar polar) {
                // A constant x value is a spoke of constant angle: the cut-off
                // ray of a spiral or a radial fan.
                polar.strokeSpoke(gc, num.doubleValue());
            } else if (coord.isFlipped()) {
                var py = sy.toPixel(num.doubleValue());
                gc.strokeLine(sx.minPixel(), py, sx.maxPixel(), py);
            } else {
                var px = sx.toPixel(num.doubleValue());
                gc.strokeLine(px, sy.minPixel(), px, sy.maxPixel());
            }
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
        if (dashes != null && dashes.length > 0) {
            gc.setLineDashes(dashes);
        }

        if (coord instanceof CoordPolar polar) {
            // A constant x value is a spoke of constant angle across the disc.
            polar.strokeSpoke(gc, xIntercept);
            if (annotation != null && !annotation.isBlank()) {
                var a = polar.thetaAngle(xIntercept);
                // A spoke caption hangs just inside the rim so the polar panel
                // clip (the disc) keeps it fully readable.
                var r = polar.radius() - 6;
                var ax = polar.centerX() + r * Math.cos(a);
                var ay = polar.centerY() + r * Math.sin(a);
                gc.setLineDashes(null);
                gc.setFont(Font.font("System", 9));
                gc.setTextAlign(TextAlignment.CENTER);
                // Centered on the spoke position; set explicitly so the look
                // does not depend on leaked coordinate-system state.
                gc.setTextBaseline(VPos.CENTER);
                // A halo keeps the caption readable where it crosses the spoke,
                // without needing to know the panel's background colour.
                gc.setStroke(HALO);
                gc.setLineWidth(3.0);
                gc.strokeText(annotation, ax, ay + 4);
                gc.setFill(color);
                gc.fillText(annotation, ax, ay + 4);
                gc.setTextBaseline(VPos.BASELINE);
            }
        } else if (coord.isFlipped()) {
            // The X aesthetic drives the vertical scale, so the constant is a horizontal line
            var py = sy.toPixel(xIntercept);
            gc.strokeLine(sx.minPixel(), py, sx.maxPixel(), py);
        } else {
            var px = sx.toPixel(xIntercept);
            gc.strokeLine(px, sy.minPixel(), px, sy.maxPixel());
            if (annotation != null && !annotation.isBlank()) {
                gc.setLineDashes(null);
                gc.setFont(Font.font("System", 9));
                gc.setTextAlign(TextAlignment.LEFT);
                // A halo keeps the caption readable where it crosses dense data,
                // without needing to know the panel's background colour.
                gc.setStroke(HALO);
                gc.setLineWidth(3.0);
                gc.strokeText(annotation, px + 5, sy.minPixel() + 10);
                gc.setFill(color);
                gc.fillText(annotation, px + 5, sy.minPixel() + 10);
            }
        }
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
            // Data-driven intercepts: one line per row of the (facet-restricted) frame.
            DF frame = PanelFacet.restrict(this.data, ext, ctx.facetValues());
            if (frame == null) {
                return null;
            }
            var intercepts = ext.getColumn(frame, xColumn);
            var n = ext.getRowCount(frame);
            for (var i = 0; i < n; i++) {
                var v = intercepts.get(i);
                if (!(v instanceof Number num)) {
                    continue;
                }
                if (coord.isFlipped()) {
                    if (Math.abs(my - sy.toPixel(num.doubleValue())) <= band) {
                        return annotation != null ? annotation : "x = " + Scale.formatTick(num.doubleValue());
                    }
                } else if (Math.abs(mx - sx.toPixel(num.doubleValue())) <= band) {
                    return annotation != null ? annotation : "x = " + Scale.formatTick(num.doubleValue());
                }
            }
            return null;
        }
        if (xIntercept == null) {
            return null;
        }
        double px;
        if (coord.isFlipped()) {
            px = sy.toPixel(xIntercept);
            if (Math.abs(my - px) > band) {
                return null;
            }
        } else {
            px = sx.toPixel(xIntercept);
            if (Math.abs(mx - px) > band) {
                return null;
            }
        }
        return annotation != null ? annotation : "x = " + Scale.formatTick(xIntercept);
    }
}
