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
 * Renders a closed polygon geometry, the radar-chart workhorse: one polygon
 * per group with its vertices sorted along the x aesthetic and the loop closed
 * back to the first vertex. Every vertex is routed through the coordinate
 * system and categorical x values are resolved by the scale, so a String metric
 * column maps to evenly spaced spokes of a {@code coordPolar} disc — the
 * classic spider / web chart. Unlike {@link GeomArea}, the polygon does not
 * extend a baseline edge to the axis floor: it fills only the region between
 * the value vertices themselves.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomPolygon<DF> implements Layer<DF> {

    private Color defaultColor = Color.web("#333333");
    private boolean filled = true;
    private double fillAlpha = 0.35;
    private double lineWidth = 1.6;

    /**
     * Creates a polygon geometry with default stroke and fill styling.
     */
    public GeomPolygon() {
    }

    /**
     * Sets the base color for an ungrouped polygon, and the stroke colour of a
     * grouped one.
     *
     * @param c the JavaFX {@link Color}
     * @return this {@code GeomPolygon} for fluid chaining
     */
    public GeomPolygon<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the fill opacity of each polygon (default 0.35), so overlapping
     * webs remain readable.
     *
     * @param alpha the fill alpha in the range [0, 1]
     * @return this {@code GeomPolygon} for fluid chaining
     */
    public GeomPolygon<DF> alpha(double alpha) {
        this.fillAlpha = alpha;
        return this;
    }

    /**
     * Sets the outline stroke width in pixels.
     *
     * @param w the stroke width in pixels
     * @return this {@code GeomPolygon} for fluid chaining
     */
    public GeomPolygon<DF> lineWidth(double w) {
        this.lineWidth = w;
        return this;
    }

    /**
     * Controls whether the polygon is filled ({@code true}, the default) or
     * drawn as a closed outline only.
     *
     * @param fill {@code false} to stroke just the outline
     * @return this {@code GeomPolygon} for fluid chaining
     */
    public GeomPolygon<DF> filled(boolean fill) {
        this.filled = fill;
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
        var xData = ext.getColumn(df, aes.x());
        var yData = ext.getColumn(df, aes.y());
        var n = ext.getRowCount(df);
        if (n < 3) {
            return;
        }

        var groupCol = aes.group();
        if (groupCol == null) {
            groupCol = aes.color();
        }
        if (groupCol == null) {
            groupCol = aes.fill();
        }

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (groupCol == null) {
            drawPolygon(gc, xData, yData, sx, sy, coord, defaultColor, null);
        } else {
            var groupData = ext.getColumn(df, groupCol);
            var groups = new LinkedHashMap<Object, List<Integer>>();
            for (var i = 0; i < n; i++) {
                groups.computeIfAbsent(groupData.get(i), k -> new ArrayList<>()).add(i);
            }

            for (var entry : groups.entrySet()) {
                var color = scales.resolvedColorFor(groupCol, entry.getKey());
                drawPolygon(gc, xData, yData, sx, sy, coord, color != null ? color : defaultColor, entry.getValue());
            }
        }
        gc.restore();
    }

    private void drawPolygon(DrawSurface gc, List<?> xData, List<?> yData, Scale sx, Scale sy, Coord coord,
                             Color color, List<Integer> indices) {
        if (indices == null) {
            indices = new ArrayList<>(xData.size());
            for (var i = 0; i < xData.size(); i++) {
                indices.add(i);
            }
        }
        if (indices.size() < 3) {
            return;
        }

        // Sort along the theta order so a radar connects neighbouring spokes.
        indices.sort(Comparator.comparingDouble(idx -> sx.toData(xData.get(idx))));

        var px = new double[indices.size()];
        var py = new double[indices.size()];
        var count = 0;
        for (var idx : indices) {
            var rawX = xData.get(idx);
            var rawY = yData.get(idx);
            if (rawX == null || rawY == null) {
                continue;
            }
            px[count] = coord.xPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            py[count] = coord.yPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            count++;
        }
        if (count < 3) {
            return;
        }

        // Close the loop back to the first vertex.
        var closedPx = new double[count + 1];
        var closedPy = new double[count + 1];
        System.arraycopy(px, 0, closedPx, 0, count);
        System.arraycopy(py, 0, closedPy, 0, count);
        closedPx[count] = px[0];
        closedPy[count] = py[0];

        if (filled) {
            gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
            gc.fillPolygon(closedPx, closedPy, count + 1);
        }
        gc.setStroke(color.darker());
        gc.setLineWidth(lineWidth);
        gc.strokePolygon(closedPx, closedPy, count + 1);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return 1;
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        var xData = ext.getColumn(df, aes.x());
        var yData = ext.getColumn(df, aes.y());
        var n = ext.getRowCount(df);
        if (n < 3) {
            return null;
        }
        var groupCol = aes.group();
        if (groupCol == null) {
            groupCol = aes.color();
        }
        if (groupCol == null) {
            groupCol = aes.fill();
        }
        if (groupCol == null || ext.getColumn(df, groupCol) == null) {
            if (polygonContains(xData, yData, null, mx, my, sx, sy, coord)) {
                return "polygon";
            }
            return null;
        }
        var groupData = ext.getColumn(df, groupCol);
        var groups = new LinkedHashMap<Object, List<Integer>>();
        for (var i = 0; i < n; i++) {
            groups.computeIfAbsent(groupData.get(i), k -> new ArrayList<>()).add(i);
        }
        for (var indices : groups.values()) {
            if (polygonContains(xData, yData, indices, mx, my, sx, sy, coord)) {
                return "polygon";
            }
        }
        return null;
    }

    /** Whether the mouse lies inside the polygon with the given sorted vertices. */
    private static boolean polygonContains(List<?> xData, List<?> yData, List<Integer> indices,
                                           double mx, double my, Scale sx, Scale sy, Coord coord) {
        if (indices == null) {
            indices = new ArrayList<>(xData.size());
            for (var i = 0; i < xData.size(); i++) {
                indices.add(i);
            }
        }
        if (indices.size() < 3) {
            return false;
        }
        indices.sort(Comparator.comparingDouble(idx -> sx.toData(xData.get(idx))));
        var px = new double[indices.size()];
        var py = new double[indices.size()];
        var count = 0;
        for (var idx : indices) {
            var rawX = xData.get(idx);
            var rawY = yData.get(idx);
            if (rawX == null || rawY == null) {
                continue;
            }
            px[count] = coord.xPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            py[count] = coord.yPixel(sx, sy, sx.toData(rawX), sy.toData(rawY));
            count++;
        }
        if (count < 3) {
            return false;
        }
        return pointInPolygon(mx, my, px, py, count);
    }

    /** Ray-casting point-in-polygon test over {@code count} vertices. */
    private static boolean pointInPolygon(double mx, double my, double[] px, double[] py, int count) {
        boolean inside = false;
        for (int i = 0, j = count - 1; i < count; j = i++) {
            if ((py[i] > my) != (py[j] > my)
                    && mx < (px[j] - px[i]) * (my - py[i]) / (py[j] - py[i]) + px[i]) {
                inside = !inside;
            }
        }
        return inside;
    }
}
