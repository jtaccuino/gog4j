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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.stat.SmoothMethod;
import org.jtaccuino.gog.stat.SmoothParams;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.stat.Stats;

/**
 * Renders a smoothed conditional mean (trend line) with optional confidence band.
 * <p>
 * Delegates to {@link org.jtaccuino.gog.stat.StatSmooth} for the underlying statistical computation
 * and supports both LOESS and linear-model smoothing methods.
 *
 * @param <DF> the data-frame type
 */
public class GeomSmooth<DF> implements Layer<DF> {

    private SmoothParams params = SmoothParams.defaults();
    private Color lineColor = Color.BLACK;
    private Color bandColor = Color.web("#999999", 0.20);
    private Color customBandColor = null;

    /**
     * Per-panel fitted trend lines for ungrouped plots (case B in render).
     * The fit is deterministic over the immutable spec and panel partition, so
     * it is computed once per panel and replayed on every redraw instead of
     * re-running the LOESS/LM every pass. Keyed by the panel's facet values.
     */
    private final Map<String, StatData> panelFits = new ConcurrentHashMap<>();

    /**
     * Creates a smoothed-mean geometry with default smoothing options.
     */
    public GeomSmooth() {
    }

    /**
     * Sets the smoothing method (LOESS or LM).
     *
     * @param m the {@link SmoothMethod} to use
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> method(SmoothMethod m) {
        this.params = params.method(m);
        return this;
    }

    /**
     * Sets the smoothing span parameter (0.0 to 1.0).
     *
     * @param s the span parameter (default 0.75)
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> span(double s) {
        this.params = params.span(s);
        return this;
    }

    /**
     * Enables or disables the confidence band (standard error ribbon).
     *
     * @param b {@code true} to show the confidence band
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> se(boolean b) {
        this.params = params.se(b);
        return this;
    }

    /**
     * Sets a constant line color for the smooth trend line.
     *
     * @param c the JavaFX {@link Color}
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> color(Color c) {
        this.lineColor = c;
        return this;
    }

    /**
     * Enables global extrapolation of the smooth curve across the full
     * plot width rather than only within each group's data range.
     *
     * @param b {@code true} to extrapolate across the full range
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> fullrange(boolean b) {
        this.params = params.fullrange(b);
        return this;
    }

    /**
     * Sets a custom fill color for the confidence band,
     * overriding the default per-group color.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomSmooth} for fluid chaining
     */
    public GeomSmooth<DF> fill(Color c) {
        this.customBandColor = c;
        return this;
    }

    /**
     * Returns the custom confidence band fill color, if configured.
     *
     * @return the custom band color, or {@code null}
     */
    public Color getCustomBandColor() {
        return this.customBandColor;
    }

    /**
     * Whether the confidence band is enabled.
     *
     * @return {@code true} if the confidence band is drawn
     */
    public boolean isSe() {
        return this.params.se();
    }

    @Override
    public LayerData prepare(PlotContext<DF> ctx) {
        var ext = ctx.extractor();
        var aes = ctx.aes();
        panelFits.clear();
        var groupCol = aes.color();
        if (groupCol == null) {
            groupCol = aes.fill();
        }
        if (groupCol == null || ext.getColumn(ctx.globalDf(), groupCol) == null) {
            return LayerData.NONE;
        }

        // Case A: grouped trend lines trained on global data
        var globalGroupValues = ext.getColumn(ctx.globalDf(), groupCol);
        var globalXValues = ext.getColumn(ctx.globalDf(), aes.x());
        var globalYValues = ext.getColumn(ctx.globalDf(), aes.y());

        var uniqueGroups = new ArrayList<>(globalGroupValues.stream()
                .filter(g -> g != null)
                .distinct()
                .toList());
        uniqueGroups.sort((a, b) -> a.toString().compareTo(b.toString()));

        double globalMinX = Double.MAX_VALUE;
        double globalMaxX = -Double.MAX_VALUE;
        for (var gx : globalXValues) {
            if (gx != null) {
                double xVal = Values.toDouble(gx);
                if (xVal < globalMinX) globalMinX = xVal;
                if (xVal > globalMaxX) globalMaxX = xVal;
            }
        }

        var fitList = new ArrayList<StatData>();
        var groupNameList = new ArrayList<Object>();
        var stat = Stats.smooth();
        for (var group : uniqueGroups) {
            var gx = new ArrayList<Double>();
            var gy = new ArrayList<Double>();
            for (int i = 0; i < globalXValues.size(); i++) {
                if (group.equals(globalGroupValues.get(i))) {
                    var rx = globalXValues.get(i);
                    var ry = globalYValues.get(i);
                    if (rx != null && ry != null) {
                        gx.add(Values.toDouble(rx));
                        gy.add(Values.toDouble(ry));
                    }
                }
            }
            if (gx.size() < 3) continue;
            fitList.add(stat.fit(gx, gy, smoothParams(globalMinX, globalMaxX)));
            groupNameList.add(group);
        }
        var grouped = new GroupedSmooths(groupNameList, fitList);
        var globalXRange = new MinMax(globalMinX, globalMaxX);
        return new SmoothData(grouped, ctx.scales(), groupCol, globalXRange);
    }

    /**
     * Builds the {@link SmoothParams} for this layer's configured method, span,
     * confidence and full-range flags over the given global X domain.
     *
     * @param globalMinX the left edge of the full-range grid
     * @param globalMaxX the right edge of the full-range grid
     * @return the typed stat parameters
     */
    private SmoothParams smoothParams(double globalMinX, double globalMaxX) {
        return this.params.globalRange(globalMinX, globalMaxX);
    }

    /**
     * Returns the cached per-panel fit for the given render context, computing
     * it once per panel on first use and reusing it for every subsequent pass.
     *
     * @param ctx the panel render context
     * @return the fitted trend data for the panel
     */
    private StatData panelFit(PanelContext<DF> ctx) {
        return panelFits.computeIfAbsent(ctx.facetValues().toString(), k -> computePanelFit(ctx));
    }

    private StatData computePanelFit(PanelContext<DF> ctx) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var df = ctx.partitionDf();
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var xData = new ArrayList<Double>();
        var yData = new ArrayList<Double>();
        for (var i = 0; i < ext.getRowCount(df); i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx != null && ry != null) {
                xData.add(Values.toDouble(rx));
                yData.add(Values.toDouble(ry));
            }
        }
        var globalXColumn = ext.getColumn(ctx.plot().globalDf(), aes.x());
        double globalMinX = Double.MAX_VALUE;
        double globalMaxX = -Double.MAX_VALUE;
        for (var gx : globalXColumn) {
            if (gx != null) {
                double xVal = Values.toDouble(gx);
                if (xVal < globalMinX) globalMinX = xVal;
                if (xVal > globalMaxX) globalMaxX = xVal;
            }
        }
        return Stats.smooth().fit(xData, yData, smoothParams(globalMinX, globalMaxX));
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
        if (ext.getRowCount(df) < 3 && !(data instanceof SmoothData)) return;
        var linTypeScale = scales.linetypeScale(aes.linetype());

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        if (data instanceof SmoothData sd) {
            // Case A: grouped trend lines from precomputed fits
            for (int i = 0; i < sd.grouped().groups().size(); i++) {
                var group = sd.grouped().groups().get(i);
                var res = sd.grouped().fits().get(i);
                var resolved = sd.scales().resolvedColorFor(sd.groupCol(), group);
                var dynColor = resolved != null ? resolved : lineColor;
                var finalBandColor = (this.customBandColor != null)
                        ? new Color(customBandColor.getRed(), customBandColor.getGreen(), customBandColor.getBlue(), 0.15)
                        : new Color(dynColor.getRed(), dynColor.getGreen(), dynColor.getBlue(), 0.12);
                drawSmoothCurves(gc, res, sx, sy, coord, dynColor,
                        new Color(finalBandColor.getRed(), finalBandColor.getGreen(), finalBandColor.getBlue(), 0.12),
                        linTypeScale != null ? linTypeScale.patternFor(group) : null);
            }
        } else {
            // Case B: single trend line fitted on partition data. The fit is
            // deterministic per panel, so it runs once per panel and is cached.
            var res = panelFit(ctx);
            double[] singleDash = linTypeScale != null && !linTypeScale.categories().isEmpty()
                    ? linTypeScale.patternFor(linTypeScale.categories().get(0)) : null;
            drawSmoothCurves(gc, res, sx, sy, coord, lineColor, bandColor, singleDash);
        }

        gc.restore();
    }

    private void drawSmoothCurves(DrawSurface gc, StatData res, Scale sx, Scale sy, Coord coord, Color lineC, Color bandC, double[] dashes) {
        var xp = res.columnAsDoubles("x");
        var yp = res.columnAsDoubles("y");
        var yMin = res.columnAsDoubles("ymin");
        var yMax = res.columnAsDoubles("ymax");
        int size = xp.size();
        if (size < 2) {
            return;
        }

        // 1. Draw the gray confidence band (closes seamlessly clockwise).
        // Both edges route through the coordinate system, so a polar layout wraps
        // the ribbon around the disc instead of drawing it in Cartesian pixels.
        if (params.se()) {
            var px = new double[size * 2];
            var py = new double[size * 2];

            for (var i = 0; i < size; i++) {
                px[i] = coord.xPixel(sx, sy, xp.get(i), yMax.get(i));
                py[i] = coord.yPixel(sx, sy, xp.get(i), yMax.get(i));
            }
            for (var i = 0; i < size; i++) {
                var idx = size - 1 - i;
                px[size + i] = coord.xPixel(sx, sy, xp.get(idx), yMin.get(idx));
                py[size + i] = coord.yPixel(sx, sy, xp.get(idx), yMin.get(idx));
            }
            gc.setFill(bandC);
            gc.fillPolygon(px, py, size * 2);
        }

        // 2. Draw the thick trend line as a single connected path. Emitting a
        // separate segment per fitted point would join badly and, on a vector
        // backend, produce one element per observation.
        gc.setStroke(lineC);
        gc.setLineWidth(2.5);
        gc.setLineDashes(dashes);
        gc.beginPath();
        for (var i = 0; i < size; i++) {
            var px = coord.xPixel(sx, sy, xp.get(i), yp.get(i));
            var py = coord.yPixel(sx, sy, xp.get(i), yp.get(i));
            if (i == 0) {
                gc.moveTo(px, py);
            } else {
                gc.lineTo(px, py);
            }
        }
        gc.stroke();
        gc.setLineDashes(null);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        if (ext.getRowCount(df) < 3 && !(data instanceof SmoothData)) return null;

        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var xData = new ArrayList<Double>();
        var yData = new ArrayList<Double>();
        for (var i = 0; i < ext.getRowCount(df); i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx != null && ry != null) {
                xData.add(Values.toDouble(rx));
                yData.add(Values.toDouble(ry));
            }
        }

        double globalMinX = Double.MAX_VALUE;
        double globalMaxX = -Double.MAX_VALUE;
        if (params.fullrange()) {
            if (data instanceof SmoothData sd && sd.globalXRange() != null) {
                globalMinX = sd.globalXRange().min();
                globalMaxX = sd.globalXRange().max();
            } else {
                var globalXColumn = ext.getColumn(ctx.plot().globalDf(), aes.x());
                for (var gx : globalXColumn) {
                    if (gx != null) {
                        double xVal = Values.toDouble(gx);
                        if (xVal < globalMinX) globalMinX = xVal;
                        if (xVal > globalMaxX) globalMaxX = xVal;
                    }
                }
            }
        }

        var proximityTolerance = 20.0;

        if (data instanceof SmoothData sd && sd.grouped() != null) {
            var groupCol = aes.color() != null ? aes.color() : aes.fill();
            var groupValues = groupCol != null ? ext.getColumn(df, groupCol) : null;
            var uniqueGroups = new ArrayList<Object>();
            if (groupValues != null) {
                for (var g : groupValues) {
                    if (g != null && !uniqueGroups.contains(g)) {
                        uniqueGroups.add(g);
                    }
                }
            }
            for (var group : uniqueGroups) {
                int fitIdx = sd.grouped().groups().indexOf(group);
                if (fitIdx < 0) continue;
                var res = sd.grouped().fits().get(fitIdx);
                var foundText = checkNearestSmoothPoint(mx, my, res, sx, sy, coord, proximityTolerance, group.toString());
                if (foundText != null) return foundText;
            }
        } else {
            var res = Stats.smooth().fit(xData, yData, smoothParams(globalMinX, globalMaxX));
            return checkNearestSmoothPoint(mx, my, res, sx, sy, coord, proximityTolerance, null);
        }
        return null;
    }

    /**
     * Helper method: finds the nearest point on a computed curve to the mouse.
     */
    private String checkNearestSmoothPoint(double mx, double my, StatData res, Scale sx, Scale sy, Coord coord, double tolerance, String groupName) {
        var xp = res.columnAsDoubles("x");
        var yp = res.columnAsDoubles("y");
        var yMin = res.columnAsDoubles("ymin");
        var yMax = res.columnAsDoubles("ymax");
        int size = xp.size();
        if (size < 2) {
            return null;
        }

        int bestIdx = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            double cx = coord.xPixel(sx, sy, xp.get(i), yp.get(i));
            double cy = coord.yPixel(sx, sy, xp.get(i), yp.get(i));

            // Measure distance strictly along the independent axis (X in standard mode)
            double dist = coord.isFlipped() ? Math.abs(my - cy) : Math.abs(mx - cx);
            if (dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
        }

        // If a nearby curve segment was hit, check vertical distance to the line
        if (bestIdx >= 0 && minDistance <= tolerance) {
            double cx = coord.xPixel(sx, sy, xp.get(bestIdx), yp.get(bestIdx));
            double cy = coord.yPixel(sx, sy, xp.get(bestIdx), yp.get(bestIdx));

            // Additional safety check: mouse must not be too far vertically from the line
            double verticalDist = coord.isFlipped() ? Math.abs(mx - cx) : Math.abs(my - cy);
            if (verticalDist > 35.0) {
                return null;
            }

            // Format tooltip text (by default style)
            var methodLabel = (params.method() == SmoothMethod.LM) ? "Linear" : "LOESS";
            var labelX = String.format(Locale.US, "%.1f", xp.get(bestIdx)).replaceAll("\\.0$", "");

            var sb = new StringBuilder();
            if (groupName != null) {
                sb.append(String.format("Group: %s\n", groupName));
            }
            sb.append(String.format("Method: %s\nX-Val: %s\nTrend: %,.1f", methodLabel, labelX, yp.get(bestIdx)));

            // If confidence band is active, display the range
            if (params.se() && yMin != null && yMax != null) {
                sb.append(String.format("\n95%% CI: [%,.1f - %,.1f]", yMin.get(bestIdx), yMax.get(bestIdx)));
            }

            return sb.toString();
        }

        return null;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
