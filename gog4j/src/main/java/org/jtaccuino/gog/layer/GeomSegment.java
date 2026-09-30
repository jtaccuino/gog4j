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
 * {@code Geoms.segment()} geometry: one directed line per row, drawn from
 * {@code (x, y)} to {@code (xend, yend)}. Every row produces an independent
 * segment, making this the geometry for force diagrams, regression-line
 * overlays, error bars, and flow maps.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomSegment<DF> implements Layer<DF> {

    /**
     * Constructs a {@code Geoms.segment()} geometry.
     */
    public GeomSegment() {
    }

    private Color defaultColor = Color.web("#333333");
    private double width = 1.0;

    /**
     * Sets the stroke color for the segments.
     *
     * @param c line color
     * @return this geometry for fluid chaining
     */
    public GeomSegment<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the segment stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomSegment<DF> width(double w) {
        this.width = w;
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

            double x0 = sx.toData(rawX);
            double y0 = sy.toData(rawY);
            double x1 = sx.toData(endX);
            double y1 = sy.toData(endY);

            var resolved = colorData != null ? colorResolver.forValue(colorData.get(i)) : null;
            gc.setStroke(resolved != null ? resolved : defaultColor);
            gc.beginPath();
            gc.moveTo(coord.xPixel(sx, sy, x0, y0), coord.yPixel(sx, sy, x0, y0));
            gc.lineTo(coord.xPixel(sx, sy, x1, y1), coord.yPixel(sx, sy, x1, y1));
            gc.stroke();
        }
        gc.restore();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
