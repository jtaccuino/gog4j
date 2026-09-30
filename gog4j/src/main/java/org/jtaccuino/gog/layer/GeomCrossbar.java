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
 * {@code Geoms.crossbar()} geometry: a filled horizontal box per row spanning
 * the {@code ymin}/{@code ymax} aesthetics centred on {@code x}, with a thicker
 * centre line at {@code y} — a boxplot-like summary bar. The box's x extent is
 * the constant {@code width}; the centre line is {@code fatten} times the box
 * stroke width (conventional semantics).
 * <p>
 * Reads the {@code x}/{@code y}/{@code ymin}/{@code ymax} columns directly;
 * {@code fill} maps the box colour, {@code color} the stroke and centre line.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomCrossbar<DF> implements Layer<DF> {

    private Color defaultFill = Color.web("#3182bd", 0.6);
    private Color defaultColor = Color.web("#333333");
    private double width = 0.9;
    private double fatten = 2.5;
    private double lineWidth = 1.0;

    /**
     * Constructs a {@code Geoms.crossbar()} geometry.
     */
    public GeomCrossbar() {
    }

    /**
     * Sets the box fill colour.
     *
     * @param c fill colour
     * @return this geometry for fluid chaining
     */
    public GeomCrossbar<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the stroke and centre-line colour.
     *
     * @param c outline colour
     * @return this geometry for fluid chaining
     */
    public GeomCrossbar<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the box width in x units.
     *
     * @param w box width in x-data units, {@code 0.9} by default
     * @return this geometry for fluid chaining
     */
    public GeomCrossbar<DF> width(double w) {
        this.width = w;
        return this;
    }

    /**
     * Sets the multiplier applied to the box line width for the centre line.
     *
     * @param f centre-line width multiplier, {@code 2.5} by default
     * @return this geometry for fluid chaining
     */
    public GeomCrossbar<DF> fatten(double f) {
        this.fatten = f;
        return this;
    }

    /**
     * Sets the box outline stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomCrossbar<DF> lineWidth(double w) {
        this.lineWidth = w;
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
        var yCol = aes.y();
        var yminCol = aes.ymin();
        var ymaxCol = aes.ymax();
        if (xCol == null || yCol == null || yminCol == null || ymaxCol == null) {
            return;
        }
        var xData = ext.getColumn(df, xCol);
        var yData = ext.getColumn(df, yCol);
        var yminData = ext.getColumn(df, yminCol);
        var ymaxData = ext.getColumn(df, ymaxCol);
        var n = ext.getRowCount(df);

        var fillCol = aes.fill();
        var colorCol = aes.color();
        var fillData = fillCol != null ? ext.getColumn(df, fillCol) : null;
        var colorData = colorCol != null ? ext.getColumn(df, colorCol) : null;
        var scales = ctx.plot().scales();

        gc.setLineWidth(lineWidth);
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var fillResolver = fillCol != null ? scales.colorResolverFor(fillCol) : null;
        var colorResolver = colorCol != null ? scales.colorResolverFor(colorCol) : null;

        for (var i = 0; i < n; i++) {
            var rx = xData.get(i);
            var ry = yData.get(i);
            var rmin = yminData.get(i);
            var rmax = ymaxData.get(i);
            if (rx == null || ry == null || rmin == null || rmax == null) {
                continue;
            }
            var cx = sx.toPixel(sx.toData(rx));
            var top = sy.toPixel(Values.toDouble(rmax));
            var bottom = sy.toPixel(Values.toDouble(rmin));
            var center = sy.toPixel(Values.toDouble(ry));
            var xD = sx.toData(rx);
            var half = Math.abs(0.5 * (sx.toPixel(xD + width / 2.0) - sx.toPixel(xD - width / 2.0)));

            var fillColor = fillData != null ? fillResolver.forValue(fillData.get(i)) : defaultFill;
            if (fillColor == null) fillColor = defaultFill;
            var strokeColor = colorData != null ? colorResolver.forValue(colorData.get(i)) : defaultColor;
            if (strokeColor == null) strokeColor = defaultColor;

            gc.setFill(fillColor);
            gc.setStroke(strokeColor);
            gc.fillRect(cx - half, top, 2 * half, bottom - top);
            gc.strokeRect(cx - half, top, 2 * half, bottom - top);

            gc.setLineWidth(lineWidth * fatten);
            gc.beginPath();
            gc.moveTo(cx - half, center);
            gc.lineTo(cx + half, center);
            gc.stroke();
            gc.setLineWidth(lineWidth);
        }
        gc.restore();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
