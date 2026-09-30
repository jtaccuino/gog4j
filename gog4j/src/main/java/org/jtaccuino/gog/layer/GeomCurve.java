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
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * {@code Geoms.curve()} geometry: a quadratic Bézier arc per row, drawn from
 * {@code (x, y)} to {@code (xend, yend)} and bowed toward a control point whose
 * offset is driven by {@code curvature} (0 = straight, 1 = full bow). The curve
 * is sampled into straight segments so it renders on any {@link DrawSurface}
 * without native Bézier support.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomCurve<DF> implements Layer<DF> {

    /**
     * Constructs a {@code Geoms.curve()} geometry.
     */
    public GeomCurve() {
    }

    private Color defaultColor = Color.web("#333333");
    private double width = 1.0;
    private double curvature = 0.5;
    private int segments = 32;

    /**
     * Sets the stroke color for the curves.
     *
     * @param c line color
     * @return this geometry for fluid chaining
     */
    public GeomCurve<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the curve stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomCurve<DF> width(double w) {
        this.width = w;
        return this;
    }

    /**
     * Sets the bow strength of the curve in the range {@code [0, 1]}. Zero
     * produces a straight line; larger values bow the arc strongly to one side.
     *
     * @param c the curvature in {@code [0, 1]}
     * @return this geometry for fluid chaining
     */
    public GeomCurve<DF> curvature(double c) {
        if (c < 0.0) c = 0.0;
        if (c > 1.0) c = 1.0;
        this.curvature = c;
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
        var xCol = aes.x();
        var yCol = aes.y();
        if (xCol == null || yCol == null) return;

        var xData = ext.getColumn(df, xCol);
        var yData = ext.getColumn(df, yCol);
        var xEndData = aes.xend() != null ? ext.getColumn(df, aes.xend()) : null;
        var yEndData = aes.yend() != null ? ext.getColumn(df, aes.yend()) : null;
        var n = ext.getRowCount(df);

        var colorCol = aes.color();
        var colorData = colorCol != null ? ext.getColumn(df, colorCol) : null;

        gc.setLineWidth(width);
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var colorResolver = colorCol != null ? scales.colorResolverFor(colorCol) : null;

        for (var i = 0; i < n; i++) {
            var rawX = xData.get(i);
            var rawY = yData.get(i);
            if (rawX == null || rawY == null) continue;
            var endX = xEndData != null ? xEndData.get(i) : rawX;
            var endY = yEndData != null ? yEndData.get(i) : rawY;
            if (endX == null || endY == null) continue;

            double x0 = coord.xPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            double y0 = coord.yPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            double x2 = coord.xPixel(sx, sy, sx.toData(endX), sy.toData(endY));
            double y2 = coord.yPixel(sx, sy, sx.toData(endX), sy.toData(endY));

            var resolved = colorData != null ? colorResolver.forValue(colorData.get(i)) : null;
            gc.setStroke(resolved != null ? resolved : defaultColor);
            drawCurve(gc, x0, y0, x2, y2);
        }
        gc.restore();
    }

    private void drawCurve(DrawSurface gc, double x0, double y0, double x2, double y2) {
        double midX = (x0 + x2) / 2.0;
        double midY = (y0 + y2) / 2.0;
        double dx = x2 - x0;
        double dy = y2 - y0;
        double len = Math.hypot(dx, dy);
        double nx = len == 0.0 ? 0.0 : -dy / len;
        double ny = len == 0.0 ? 1.0 : dx / len;
        double offset = curvature * len * 0.5;
        double cpx = midX + nx * offset;
        double cpy = midY + ny * offset;

        gc.beginPath();
        gc.moveTo(x0, y0);
        for (var s = 1; s <= segments; s++) {
            double t = s / (double) segments;
            double u = 1.0 - t;
            double x = u * u * x0 + 2.0 * u * t * cpx + t * t * x2;
            double y = u * u * y0 + 2.0 * u * t * cpy + t * t * y2;
            gc.lineTo(x, y);
        }
        gc.stroke();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
