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
 * {@code Geoms.linerange()} geometry: a vertical line per row between the
 * {@code ymin} and {@code ymax} aesthetics centred on {@code x} — the
 * cap-less relative of {@link GeomErrorbar}.
 * <p>
 * Reads the {@code x}/{@code ymin}/{@code ymax} columns directly (typically the
 * output of {@code Stats.summary}); {@code color} maps one colour per category.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomLinerange<DF> implements Layer<DF> {

    private Color defaultColor = Color.web("#333333");
    private double width = 1.0;

    /**
     * Constructs a {@code Geoms.linerange()} geometry.
     */
    public GeomLinerange() {
    }

    /**
     * Sets the line colour.
     *
     * @param c line colour
     * @return this geometry for fluid chaining
     */
    public GeomLinerange<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomLinerange<DF> width(double w) {
        this.width = w;
        return this;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var df = ctx.partitionDf();
        var xCol = aes.x();
        var yminCol = aes.ymin();
        var ymaxCol = aes.ymax();
        if (xCol == null || yminCol == null || ymaxCol == null) {
            return;
        }
        var xData = ext.getColumn(df, xCol);
        var yminData = ext.getColumn(df, yminCol);
        var ymaxData = ext.getColumn(df, ymaxCol);
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
            var rx = xData.get(i);
            var rmin = yminData.get(i);
            var rmax = ymaxData.get(i);
            if (rx == null || rmin == null || rmax == null) {
                continue;
            }
            var cx = sx.toPixel(sx.toData(rx));
            var top = sy.toPixel(Values.toDouble(rmax));
            var bottom = sy.toPixel(Values.toDouble(rmin));
            var color = colorData != null ? colorResolver.forValue(colorData.get(i)) : defaultColor;
            if (color == null) color = defaultColor;
            gc.setStroke(color);
            gc.beginPath();
            gc.moveTo(cx, top);
            gc.lineTo(cx, bottom);
            gc.stroke();
        }
        gc.restore();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
