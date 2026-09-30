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
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;

/**
 * {@code Geoms.errorbarh()} geometry: a horizontal whisker per row between the
 * {@code xmin} and {@code xmax} aesthetics centred on the {@code y} value, with
 * a vertical cap tick at each end — the flipped mirror of {@link GeomErrorbar}.
 * <p>
 * Reads the {@code y}/{@code xmin}/{@code xmax} columns directly;
 * {@code color} maps one colour per category.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomErrorbarh<DF> implements Layer<DF> {

    private Color defaultColor = Color.web("#333333");
    private double width = 1.0;
    private double capHeight = 0.02;

    /**
     * Constructs a {@code Geoms.errorbarh()} geometry.
     */
    public GeomErrorbarh() {
    }

    /**
     * Sets the whisker stroke colour.
     *
     * @param c line colour
     * @return this geometry for fluid chaining
     */
    public GeomErrorbarh<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the whisker stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomErrorbarh<DF> width(double w) {
        this.width = w;
        return this;
    }

    /**
     * Sets the cap height as a fraction of the y scale's unit spacing.
     *
     * @param capHeight the cap tick height in y-data units, {@code 0.02} by default
     * @return this geometry for fluid chaining
     */
    public GeomErrorbarh<DF> capHeight(double capHeight) {
        this.capHeight = capHeight;
        return this;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var df = ctx.partitionDf();
        var yCol = aes.y();
        var xminCol = aes.xmin();
        var xmaxCol = aes.xmax();
        if (yCol == null || xminCol == null || xmaxCol == null) {
            return;
        }
        var yData = ext.getColumn(df, yCol);
        var xminData = ext.getColumn(df, xminCol);
        var xmaxData = ext.getColumn(df, xmaxCol);
        var n = ext.getRowCount(df);

        var colorCol = aes.color();
        var colorData = colorCol != null ? ext.getColumn(df, colorCol) : null;
        var scales = ctx.plot().scales();

        gc.setLineWidth(width);
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var colorResolver = colorCol != null ? scales.colorResolverFor(colorCol) : null;

        for (var i = 0; i < n; i++) {
            var ry = yData.get(i);
            var rmin = xminData.get(i);
            var rmax = xmaxData.get(i);
            if (ry == null || rmin == null || rmax == null) {
                continue;
            }
            var cy = sy.toPixel(sy.toData(ry));
            var left = sx.toPixel(Values.toDouble(rmin));
            var right = sx.toPixel(Values.toDouble(rmax));
            var yDataVal = sy.toData(ry);
            var halfCap = Math.abs(0.5 * (sy.toPixel(yDataVal + capHeight) - sy.toPixel(yDataVal - capHeight)));

            var color = colorData != null && colorResolver != null ? colorResolver.forValue(colorData.get(i)) : defaultColor;
            if (color == null) color = defaultColor;
            gc.setStroke(color);
            gc.beginPath();
            gc.moveTo(left, cy);
            gc.lineTo(right, cy);
            gc.stroke();
            gc.beginPath();
            gc.moveTo(left, cy - halfCap);
            gc.lineTo(left, cy + halfCap);
            gc.stroke();
            gc.beginPath();
            gc.moveTo(right, cy - halfCap);
            gc.lineTo(right, cy + halfCap);
            gc.stroke();
        }
        gc.restore();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
