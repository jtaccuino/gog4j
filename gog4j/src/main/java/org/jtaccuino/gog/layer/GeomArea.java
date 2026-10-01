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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.data.Temporals;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Renders an area (ribbon / polygon) geometry, typically used for stacked area charts
 * or confidence bands. Supports configurable position adjustment (stack/identity)
 * and per-class colour mapping.
 *
 * @param <DF> the data-frame type
 */
public class GeomArea<DF> extends StackableGeom<DF> {

    private Color defaultFill = Color.web("#3182bd", 0.6);

    /**
     * Creates an area geometry stacked from the zero baseline.
     */
    public GeomArea() {
        super(Position.STACK);
    }

    /**
     * Sets the position adjustment mode (stack, identity, etc.) for the area geometry.
     *
     * @param pos the {@link Position} layout mode
     * @return this {@code GeomArea} for fluid chaining
     */
    public GeomArea<DF> position(Position pos) {
        setPosition(pos);
        return this;
    }

    /**
     * Sets a constant fill color for the area geometry.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomArea} for fluid chaining
     */
    public GeomArea<DF> fill(Color c) {
        this.defaultFill = c;
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
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var n = ext.getRowCount(df);
        if (n == 0) {
            return;
        }

        var groupCol = aes.color();
        if (groupCol == null) {
            groupCol = aes.fill();
        }

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (groupCol == null) {
            var xPoints = new ArrayList<Double>();
            var yPoints = new ArrayList<Double>();
            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                if (rx == null || ry == null) {
                    continue;
                }
                double xDouble = sx.toData(rx);
                xPoints.add(xDouble);
                yPoints.add(sy.toData(ry));
            }
            drawSingleAreaStacked(gc, xPoints, new ArrayList<>(Collections.nCopies(xPoints.size(), 0.0)), yPoints, sx, sy, coord, defaultFill);
        } else {
            var groupColName = groupCol;
            var groupData = ext.getColumn(df, groupColName);

            var allGroups = new ArrayList<Object>();
            var uniqueXVals = new ArrayList<Double>();

            var dataMap = new HashMap<Double, Map<Object, Double>>();

            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                var rg = groupData.get(i);
                if (rx == null || ry == null || rg == null) {
                    continue;
                }
                double xDouble = sx.toData(rx);

                if (!allGroups.contains(rg)) {
                    allGroups.add(rg);
                }
                if (!uniqueXVals.contains(xDouble)) {
                    uniqueXVals.add(xDouble);
                }

                // Aggregate duplicate entries within the same month (safety guard)
                dataMap.computeIfAbsent(xDouble, k -> new HashMap<>())
                        .merge(rg, sy.toData(ry), Double::sum);
            }

            // Uncompromising sort of master lists for matrix symmetry
            allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));
            uniqueXVals.sort(Double::compareTo);

            // Maintain a global baseline for each X value
            var currentBaselines = new HashMap<Double, Double>();
            for (var x : uniqueXVals) {
                currentBaselines.put(x, 0.0);
            }

            // Iterate in strict parallel over the ordered groups
            for (var gIdx = 0; gIdx < allGroups.size(); gIdx++) {
                var groupKey = allGroups.get(gIdx);
                var xPoints = new ArrayList<Double>();
                var yLowerPoints = new ArrayList<Double>();
                var yUpperPoints = new ArrayList<Double>();

                for (var x : uniqueXVals) {
                    var animalMap = dataMap.get(x);
                    // Get the actual value from the matrix, or 0.0 if absent
                    var val = animalMap == null ? 0.0 : animalMap.getOrDefault(groupKey, 0.0);

                    if (position == Position.STACK) {
                        var lower = currentBaselines.get(x);
                        var upper = lower + val;
                        xPoints.add(x);
                        yLowerPoints.add(lower);
                        yUpperPoints.add(upper);
                        currentBaselines.put(x, upper);
                    } else {
                        // IDENTITY mode: each area starts unstacked at the true axis floor (0.0)
                        xPoints.add(x);
                        yLowerPoints.add(0.0);
                        yUpperPoints.add(val);
                    }
                }

                var resolved = scales.resolvedColorFor(groupCol, groupKey);
                var color = resolved != null ? resolved : defaultFill;
                drawSingleAreaStacked(gc, xPoints, yLowerPoints, yUpperPoints, sx, sy, coord, color);
            }
        }
        gc.restore();
    }

    private void drawSingleAreaStacked(DrawSurface gc, List<Double> xPts, List<Double> yLower, List<Double> yUpper, Scale sx, Scale sy, Coord coord, Color baseColor) {
        var size = xPts.size();
        if (size < 2) {
            return;
        }

        var px = new double[size * 2];
        var py = new double[size * 2];

        // 1. Forward: along the upper edge (flush left to right)
        for (var i = 0; i < size; i++) {
            px[i] = coord.xPixel(sx, sy, xPts.get(i), yUpper.get(i));
            py[i] = coord.yPixel(sx, sy, xPts.get(i), yUpper.get(i));
        }

        // 2. Backward: along the lower edge (flush right to left clockwise)
        for (var i = 0; i < size; i++) {
            var idx = size - 1 - i;
            px[size + i] = coord.xPixel(sx, sy, xPts.get(idx), yLower.get(idx));
            py[size + i] = coord.yPixel(sx, sy, xPts.get(idx), yLower.get(idx));
        }

        // In STACK mode use high opacity (0.85) since nothing overlaps.
        // In IDENTITY mode use a light alpha (0.35) so underlying colors shine through.
        var alpha = (position == Position.STACK) ? 0.85 : 0.35;

        var transparentFill = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        gc.setFill(transparentFill);
        gc.fillPolygon(px, py, size * 2);

        // Stroke the upper contour line
        gc.setStroke(baseColor.darker());
        gc.setLineWidth(1.8);
        for (var i = 0; i < size - 1; i++) {
            gc.strokeLine(coord.xPixel(sx, sy, xPts.get(i), yUpper.get(i)),
                    coord.yPixel(sx, sy, xPts.get(i), yUpper.get(i)),
                    coord.xPixel(sx, sy, xPts.get(i + 1), yUpper.get(i + 1)),
                    coord.yPixel(sx, sy, xPts.get(i + 1), yUpper.get(i + 1)));
        }
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
        if (n == 0) {
            return null;
        }

        var xType = ext.columnType(df, aes.x());
        var xIsDate = xType == DataExtractor.ColumnType.DATE;
        var xIsTimestamp = xType == DataExtractor.ColumnType.TIMESTAMP;

        var groupCol = aes.color();
        if (groupCol == null) {
            groupCol = aes.fill();
        }
        if (groupCol == null) {
            return null;
        }

        var groupColName = groupCol;
        var groupData = ext.getColumn(df, groupColName);

        // 1. Find the nearest year in time
        var bestX = Double.MAX_VALUE;
        double targetXVal = 0;
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            if (rx == null) {
                continue;
            }
            double xDouble = sx.toData(rx);
            double cx = coord.isFlipped() ? sy.toPixel(xDouble) : sx.toPixel(xDouble);
            double dist = coord.isFlipped() ? Math.abs(my - cx) : Math.abs(mx - cx);
            if (dist < bestX) {
                bestX = dist;
                targetXVal = xDouble;
            }
        }

        if (bestX > 25.0) {
            return null; // Abort if mouse is horizontally too far
        }
        // 2. Collect exact layer values for this specific year
        var allGroups = new ArrayList<Object>();
        var groupValuesForX = new HashMap<Object, Double>();
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            var rg = groupData.get(i);
            if (rx == null || ry == null || rg == null) {
                continue;
            }
            double xDouble = sx.toData(rx);

            if (Math.abs(xDouble - targetXVal) < 0.01) {
                if (!allGroups.contains(rg)) {
                    allGroups.add(rg);
                }
                // Aggregate to stay exactly in sync with the render matrix
                groupValuesForX.merge(rg, sy.toData(ry), Double::sum);
            }
        }
        allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));

        // =========================================================================
        // 3. Intelligent layer scan (adapts reactively to the mode)
        // =========================================================================
        if (position == Position.STACK) {
            // === Case A: Stacked mode ===
            // Scan bottom-to-top through the actual layer corridors
            double currentLower = 0.0;
            for (var groupKey : allGroups) {
                var val = groupValuesForX.getOrDefault(groupKey, 0.0);
                var currentUpper = currentLower + val;

                double pLower = coord.isFlipped() ? sx.toPixel(currentLower) : sy.toPixel(currentLower);
                double pUpper = coord.isFlipped() ? sx.toPixel(currentUpper) : sy.toPixel(currentUpper);

                var isInside = coord.isFlipped() ? (mx >= pLower && mx <= pUpper) : (my <= pLower && my >= pUpper);

                if (isInside) {
                    var labelX = xIsDate ? LocalDate.ofEpochDay((long) targetXVal).toString()
                            : xIsTimestamp ? Temporals.label(Temporals.instantAt(targetXVal))
                            : String.valueOf(targetXVal);
                    return String.format("Area: %s\nDate: %s\nValue: %,.1f", groupKey, labelX, val);
                }
                currentLower = currentUpper;
            }
        } else {
            // === Case B: Identity mode (unstacked) ===
            // Reverse scan: since areas overlap, test from top Z-level
            // (last drawn group) down to the first group
            for (var i = allGroups.size() - 1; i >= 0; i--) {
                var groupKey = allGroups.get(i);
                var val = groupValuesForX.getOrDefault(groupKey, 0.0);

                // In identity mode all areas start at the true axis floor (0.0)
                double pLower = coord.isFlipped() ? sx.toPixel(0.0) : sy.toPixel(0.0);
                double pUpper = coord.isFlipped() ? sx.toPixel(val) : sy.toPixel(val);

                // Check if mouse is inside this unstacked area
                var isInside = coord.isFlipped() ? (mx >= pLower && mx <= pUpper) : (my <= pLower && my >= pUpper);

                if (isInside) {
                    var labelX = xIsDate ? LocalDate.ofEpochDay((long) targetXVal).toString()
                            : xIsTimestamp ? Temporals.label(Temporals.instantAt(targetXVal))
                            : String.valueOf(targetXVal);
                    return String.format("Area: %s\nDate: %s\nValue: %,.1f", groupKey, labelX, val);
                }
            }
        }

        return null;
    }
}
