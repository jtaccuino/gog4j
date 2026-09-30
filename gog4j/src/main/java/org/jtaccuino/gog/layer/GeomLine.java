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
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.DiscreteLinetypeScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.stat.Averaging;

/**
 * Line chart geometry layer implementation for rendering lines connecting sequential data points.
 * <p>
 * Supports sorting data points along the X-axis, grouping by categorical attributes,
 * custom line stroke width and color, and optional smoothing/averaging filters ({@link Averaging}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomLine<DF> implements Layer<DF> {
    private Color defaultColor = Color.web("#333333");
    private double width = 1.5;
    private Averaging averaging = Averaging.rollingWindow(1);

    /**
     * Creates a line geometry with default stroke styling.
     */
    public GeomLine() {
    }

    /**
     * Sets the stroke color for the line layer.
     *
     * @param c line color
     * @return this {@code GeomLine} instance for fluid chaining
     */
    public GeomLine<DF> color(Color c) { this.defaultColor = c; return this; }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param w line width in pixels
     * @return this {@code GeomLine} instance for fluid chaining
     */
    public GeomLine<DF> width(double w) { this.width = w; return this; }

    /**
     * Configures an averaging/smoothing strategy for time-series line aggregation.
     *
     * @param averaging the {@link Averaging} aggregation strategy
     * @return this {@code GeomLine} instance for fluid chaining
     */
    public GeomLine<DF> averaging(Averaging averaging) {
        this.averaging = averaging;
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
        if (n < 2) return;

        // Grouping follows group, then colour, then linetype; a linetype-only
        // mapping splits the data into groups without assigning any colour.
        var groupCol = aes.group();
        if (groupCol == null) groupCol = aes.color();
        if (groupCol == null) groupCol = aes.linetype();
        // The colour scale is derived from the actual colour (or group) column
        // only, never from the linetype fallback, so a linetype-only mapping
        // draws every group in the layer's default colour.
        var colorColumn = aes.color() != null ? aes.color() : aes.group();
        var linTypeScale = scales.linetypeScale(aes.linetype());
        // Per-group dashes only apply when the grouping column is the linetype
        // column itself; a linetype mapping on a different column cannot be
        // represented without cross-product grouping, so those stay solid.
        boolean dashByGroup = aes.linetype() != null && aes.linetype().equals(groupCol);

        gc.setLineWidth(width);
        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (groupCol == null) {
            // Truly ungrouped: a single line in the default colour adopting the
            // first category's dash (or staying solid).
            gc.setStroke(defaultColor);
            gc.setLineDashes(singleDash(linTypeScale));
            var indices = createSequentialIndices(n);
            sortIndicesByX(indices, xData, sx);
            drawPointsAsLine(gc, xData, yData, sx, sy, coord, indices);
        } else {
            var groupData = ext.getColumn(df, groupCol);
            var groups = new LinkedHashMap<Object, List<Integer>>();
            for (var i = 0; i < n; i++) {
                groups.computeIfAbsent(groupData.get(i), k -> new ArrayList<>()).add(i);
            }

            for (var entry : groups.entrySet()) {
                var indices = entry.getValue();
                if (indices.size() < 2) continue;

                sortIndicesByX(indices, xData, sx);

                var color = colorColumn != null ? scales.resolvedColorFor(colorColumn, entry.getKey()) : defaultColor;
                gc.setStroke(color != null ? color : defaultColor);
                double[] dash = dashByGroup ? linTypeScale.patternFor(entry.getKey()) : singleDash(linTypeScale);
                gc.setLineDashes(dash);
                drawPointsAsLine(gc, xData, yData, sx, sy, coord, indices);
            }
            gc.setLineDashes(null);
        }
        gc.restore();
    }

    /** The first category's dash pattern, or {@code null} when none is mapped. */
    private double[] singleDash(DiscreteLinetypeScale linTypeScale) {
        if (linTypeScale != null && !linTypeScale.categories().isEmpty()) {
            return linTypeScale.patternFor(linTypeScale.categories().get(0));
        }
        return null;
    }

    private List<Integer> createSequentialIndices(int n) {
        var list = new ArrayList<Integer>(n);
        for (var i = 0; i < n; i++) list.add(i);
        return list;
    }

    private void sortIndicesByX(List<Integer> indices, List<?> xData, Scale sx) {
        indices.sort(Comparator.comparingDouble(idx -> sx.toData(xData.get(idx))));
    }

    private void drawPointsAsLine(DrawSurface gc, List<?> xData, List<?> yData, Scale sx, Scale sy,
                                  Coord coord, List<Integer> indices) {
        gc.beginPath();
        var first = true;

        // Computation is fully delegated to the configured strategy object
        var smoothedY = averaging.apply(indices, yData);

        for (var i = 0; i < indices.size(); i++) {
            var idx = indices.get(i);
            var rawX = xData.get(idx);
            var rawY = smoothedY.get(i); // Get the already-filtered value

            if (rawX == null || rawY == null) continue;

            double xAsDouble = sx.toData(rawX);
            // Route the pair through the coordinate system so a polar layout wraps
            // the line around the disc instead of drawing it in Cartesian pixels.
            var cx = coord.xPixel(sx, sy, xAsDouble, ((Number) rawY).doubleValue());
            var cy = coord.yPixel(sx, sy, xAsDouble, ((Number) rawY).doubleValue());

            if (first) {
                gc.moveTo(cx, cy);
                first = false;
            } else {
                gc.lineTo(cx, cy);
            }
        }
        gc.stroke();
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var n = ext.getRowCount(df);
        if (n == 0) return null;

        var groupCol = aes.color();
        if (groupCol == null) groupCol = aes.fill();
        if (groupCol == null) groupCol = aes.group();

        // Find the point on the line nearest to the mouse (horizontal, or vertical when flipped)
        var bestIndex = -1;
        var minDistance = Double.MAX_VALUE;
        var proximityTolerance = 15.0; // Mouse must be within 15 pixels of the line

        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx == null || ry == null) continue;

double xDouble = sx.toData(rx);
            var yDouble = sy.toData(ry);

            double cx = coord.xPixel(sx, sy, xDouble, yDouble);
            double cy = coord.yPixel(sx, sy, xDouble, yDouble);

            // Measure distance to the mouse
            double dist;
            if (coord.isFlipped()) {
                // When coordFlip is active, the Y-position is the category axis
                dist = Math.abs(my - cy);
            } else {
                // Default: X-position is the time/category axis
                dist = Math.abs(mx - cx);
            }

            if (dist < minDistance) {
                minDistance = dist;
                bestIndex = i;
            }
        }

        // If a close point was found, check whether the other axis is also within tolerance
        if (bestIndex >= 0 && minDistance <= proximityTolerance) {
            var rx = rawX.get(bestIndex);
            var ry = rawY.get(bestIndex);

            double xDouble = sx.toData(rx);
            var yDouble = sy.toData(ry);

            double cx = coord.xPixel(sx, sy, xDouble, yDouble);
            double cy = coord.yPixel(sx, sy, xDouble, yDouble);
            if (coord.isFlipped()) {
                if (Math.abs(mx - cx) > 25.0) return null;
            } else {
                if (Math.abs(my - cy) > 25.0) return null;
            }

            var labelX = Values.label(rx);

            if (groupCol != null && ext.getColumn(df, groupCol) != null) {
                var groupVal = ext.getColumn(df, groupCol).get(bestIndex);
                return String.format("Series: %s\nDate: %s\nValue: %,.1f", groupVal, labelX, yDouble);
            }
            return String.format("Date: %s\nValue: %,.1f", labelX, yDouble);
        }

        return null;
    }
}
