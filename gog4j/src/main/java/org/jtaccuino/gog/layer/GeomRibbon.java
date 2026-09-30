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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * {@code Geoms.ribbon()} geometry: a filled band between the {@code ymin} and
 * {@code ymax} curves across the ordered {@code x} values — the classic
 * confidence/quantile band and a natural companion to
 * {@code Stats.summary().meanSe()}.
 * <p>
 * Reads the {@code x}/{@code ymin}/{@code ymax} columns directly. When
 * {@code color} or {@code fill} is mapped, one ribbon is drawn per category
 * with a semi-transparent fill so overlapping bands stay visible.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomRibbon<DF> implements Layer<DF> {

    private Color defaultFill = Color.web("#3182bd", 0.35);
    private Color defaultColor = Color.web("#333333");
    private boolean outline = true;
    private double alpha = 0.35;
    private double lineWidth = 1.0;

    /**
     * Constructs a {@code Geoms.ribbon()} geometry.
     */
    public GeomRibbon() {
    }

    /**
     * Sets the band fill colour.
     *
     * @param c fill colour
     * @return this geometry for fluid chaining
     */
    public GeomRibbon<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the band outline colour.
     *
     * @param c outline colour
     * @return this geometry for fluid chaining
     */
    public GeomRibbon<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the fill opacity applied to every band.
     *
     * @param a fill opacity in {@code [0, 1]}, {@code 0.35} by default
     * @return this geometry for fluid chaining
     */
    public GeomRibbon<DF> alpha(double a) {
        this.alpha = a;
        return this;
    }

    /**
     * Enables or disables the band outline.
     *
     * @param on {@code true} to stroke the upper edge, {@code false} to fill only
     * @return this geometry for fluid chaining
     */
    public GeomRibbon<DF> outline(boolean on) {
        this.outline = on;
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
        var yminCol = aes.ymin();
        var ymaxCol = aes.ymax();
        if (xCol == null || yminCol == null || ymaxCol == null) {
            return;
        }
        var xData = ext.getColumn(df, xCol);
        var yminData = ext.getColumn(df, yminCol);
        var ymaxData = ext.getColumn(df, ymaxCol);
        var n = ext.getRowCount(df);

        var groupCol = aes.color() != null ? aes.color() : aes.fill();
        var groupData = groupCol != null ? ext.getColumn(df, groupCol) : null;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (groupData == null) {
            drawOneRibbon(gc, sx, sy, coord, xData, yminData, ymaxData, n, defaultFill, defaultColor);
        } else {
            var groups = new LinkedHashSet<Object>();
            for (var i = 0; i < n; i++) {
                if (groupData.get(i) != null) {
                    groups.add(groupData.get(i));
                }
            }
            for (var g : groups) {
                var xs = new ArrayList<Double>();
                var lows = new ArrayList<Double>();
                var highs = new ArrayList<Double>();
                for (var i = 0; i < n; i++) {
                    if (!Objects.equals(groupData.get(i), g)) {
                        continue;
                    }
                    var rx = xData.get(i);
                    var rmin = yminData.get(i);
                    var rmax = ymaxData.get(i);
                    if (rx == null || rmin == null || rmax == null) {
                        continue;
                    }
                    xs.add(Values.toDouble(rx));
                    lows.add(Values.toDouble(rmin));
                    highs.add(Values.toDouble(rmax));
                }
                var color = groupCol != null
                        ? scales.resolvedColorFor(groupCol, g)
                        : defaultFill;
                if (color == null) color = defaultFill;
                drawRibbonSorted(gc, sx, sy, coord, xs, lows, highs, color, defaultColor);
            }
        }
        gc.restore();
    }

    private void drawRibbonSorted(DrawSurface gc, Scale sx, Scale sy, Coord coord,
            List<Double> xs, List<Double> lows, List<Double> highs, Color baseColor, Color outlineColor) {
        var order = new ArrayList<Integer>();
        for (var i = 0; i < xs.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(xs::get));
        var n = order.size();
        if (n < 2) {
            return;
        }
        var px = new double[n * 2];
        var py = new double[n * 2];
        for (var k = 0; k < n; k++) {
            var i = order.get(k);
            px[k] = coord.xPixel(sx, sy, xs.get(i), highs.get(i));
            py[k] = coord.yPixel(sx, sy, xs.get(i), highs.get(i));
        }
        for (var k = 0; k < n; k++) {
            var i = order.get(n - 1 - k);
            px[n + k] = coord.xPixel(sx, sy, xs.get(i), lows.get(i));
            py[n + k] = coord.yPixel(sx, sy, xs.get(i), lows.get(i));
        }
        var fill = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        gc.setFill(fill);
        gc.fillPolygon(px, py, n * 2);
        if (outline) {
            gc.setStroke(outlineColor.darker());
            gc.setLineWidth(lineWidth);
            for (var k = 0; k < n - 1; k++) {
                var i = order.get(k);
                var j = order.get(k + 1);
                gc.strokeLine(coord.xPixel(sx, sy, xs.get(i), highs.get(i)),
                        coord.yPixel(sx, sy, xs.get(i), highs.get(i)),
                        coord.xPixel(sx, sy, xs.get(j), highs.get(j)),
                        coord.yPixel(sx, sy, xs.get(j), highs.get(j)));
            }
        }
    }

    private void drawOneRibbon(DrawSurface gc, Scale sx, Scale sy, Coord coord,
            List<?> xData, List<?> yminData, List<?> ymaxData, int n, Color fillBase, Color strokeBase) {
        var xs = new ArrayList<Double>();
        var lows = new ArrayList<Double>();
        var highs = new ArrayList<Double>();
        for (var i = 0; i < n; i++) {
            var rx = xData.get(i);
            var rmin = yminData.get(i);
            var rmax = ymaxData.get(i);
            if (rx == null || rmin == null || rmax == null) {
                continue;
            }
            xs.add(Values.toDouble(rx));
            lows.add(Values.toDouble(rmin));
            highs.add(Values.toDouble(rmax));
        }
        drawRibbonSorted(gc, sx, sy, coord, xs, lows, highs, fillBase, strokeBase);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
