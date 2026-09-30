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
import java.util.LinkedHashMap;
import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Shared connected-line geometry base for the line family
 * ({@link GeomPath}, {@link GeomStep}). Draws points as a single stroked path,
 * grouping by the {@code group}/{@code color} aesthetic and resolving each
 * group's colour from the shared scale.
 * <p>
 * The base connects consecutive points with straight segments in row order.
 * Subclasses may override how consecutive points are joined
 * ({@code stepTo}) or whether points are pre-sorted by the independent axis
 * ({@code sortByX}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
abstract class LineGeom<DF> implements Layer<DF> {

    /** Default stroke color when no color aesthetic or explicit color is set. */
    protected Color defaultColor = Color.web("#333333");

    /** Stroke width of the connecting line, in pixels. */
    protected double width = 1.5;

    /**
     * Sets the stroke color for the geometry.
     *
     * @param c line color
     * @return this geometry for fluid chaining
     */
    public LineGeom<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public LineGeom<DF> width(double w) {
        this.width = w;
        return this;
    }

    /**
     * Whether points should be sorted by the independent axis before connecting.
     * {@code Geoms.path()} preserves row order ({@code false}); {@code Geoms.step()}
     * sorts ({@code true}).
     *
     * @return {@code true} to sort by x
     */
    protected abstract boolean sortByX();

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var scales = ctx.plot().scales();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        var xData = ext.getColumn(df, aes.x());
        var yData = ext.getColumn(df, aes.y());
        var n = ext.getRowCount(df);
        if (n < 2) return;

        var groupCol = aes.group();
        if (groupCol == null) groupCol = aes.color();

        gc.setLineWidth(width);
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (groupCol == null) {
            gc.setStroke(defaultColor);
            drawGroup(gc, xData, yData, sx, sy, coord, indices(n));
        } else {
            var groupData = ext.getColumn(df, groupCol);
            var groups = new LinkedHashMap<Object, List<Integer>>();
            for (var i = 0; i < n; i++) {
                groups.computeIfAbsent(groupData.get(i), k -> new ArrayList<>()).add(i);
            }
            for (var entry : groups.entrySet()) {
                var indices = entry.getValue();
                if (indices.size() < 2) continue;
                var color = scales.resolvedColorFor(groupCol, entry.getKey());
                gc.setStroke(color != null ? color : defaultColor);
                drawGroup(gc, xData, yData, sx, sy, coord, indices);
            }
        }
        gc.restore();
    }

    private List<Integer> indices(int n) {
        var list = new ArrayList<Integer>(n);
        for (var i = 0; i < n; i++) list.add(i);
        return list;
    }

    private void drawGroup(DrawSurface gc, List<?> xData, List<?> yData, Scale sx, Scale sy,
                           Coord coord, List<Integer> indices) {
        if (sortByX()) {
            var sorted = new ArrayList<>(indices);
            sorted.sort(Comparator.comparingDouble(idx -> sx.toData(xData.get(idx))));
            indices = sorted;
        }

        gc.beginPath();
        var first = true;
        double prevX = 0.0;
        double prevY = 0.0;
        for (var idx : indices) {
            var rawX = xData.get(idx);
            var rawY = yData.get(idx);
            if (rawX == null || rawY == null) continue;
            double xAsDouble = sx.toData(rawX);
            double yAsDouble = sy.toData(rawY);
            var cx = coord.xPixel(sx, sy, xAsDouble, yAsDouble);
            var cy = coord.yPixel(sx, sy, xAsDouble, yAsDouble);
            if (first) {
                gc.moveTo(cx, cy);
                first = false;
            } else {
                stepTo(gc, prevX, prevY, cx, cy);
            }
            prevX = cx;
            prevY = cy;
        }
        gc.stroke();
    }

    /**
     * Emits the connecting geometry between the previous point and the current
     * one. The default draws a straight segment; {@link GeomStep} overrides it
     * to draw a staircase.
     *
     * @param gc the drawing surface
     * @param prevX previous x (pixels)
     * @param prevY previous y (pixels)
     * @param cx     current x (pixels)
     * @param cy     current y (pixels)
     */
    protected void stepTo(DrawSurface gc, double prevX, double prevY, double cx, double cy) {
        gc.lineTo(cx, cy);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
