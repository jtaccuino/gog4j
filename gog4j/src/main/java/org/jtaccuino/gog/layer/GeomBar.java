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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.stat.Stats;

/**
 * Bar chart geometry layer implementation for rendering vertical and horizontal bar plots.
 * <p>
 * Supports position layouts via shared {@link Position} enum (including {@code IDENTITY},
 * {@code DODGE} for clustered bars, and {@code STACK} for stacked bars), horizontal orientations
 * via {@link Coord#isFlipped()}, custom bar width factors, dynamic palette coloring, and hover hit-testing.
 * <p>
 * Owns {@code Geoms.bar()}/{@code Geoms.col()}. Mapped through {@code Plot#layer(...)} it acts as the
 * stat-consuming half of a {@code Plot#layer(...)} call: the attached
 * {@link Stat} runs over each panel's data and the bars are drawn from the {@code StatData} output,
 * so computed variables such as {@code afterStat(count)} can drive the bar height and fill.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomBar<DF> extends StackableGeom<DF> implements StatHost<DF>, ConstantColorLayer {

    private double widthFactor = 0.8;
    private Color defaultFill = Color.web("#3182bd");
    private Stat<DF> attachedStat;
    private LayerParams attachedParams = LayerParams.empty();

    /**
     * Creates a bar geometry with the default {@link Position#STACK} layout,
     * following the {@code Geoms.bar()} ({@code position = "stack"}).
     */
    public GeomBar() {
        super(Position.STACK);
    }

    /**
     * Creates a bar geometry with the given default position adjustment.
     *
     * @param defaultPosition the initial {@link Position} layout mode
     */
    protected GeomBar(Position defaultPosition) {
        super(defaultPosition);
    }

    /**
     * The stat this geometry runs by default when bound through
     * {@code Plot.layer(...)} without an explicit stat — {@code Stats.count()}
     * for {@code Geoms.bar()}, following the per-geom {@code default statistic}.
     *
     * @return the default {@link Stat}, never {@code null}
     */
    @Override
    public Stat<DF> defaultStat() {
        return Stats.count();
    }

    /**
     * Sets the position adjustment mode (identity, dodge, or stack).
     *
     * @param position the {@link Position} layout mode
     * @return this {@code GeomBar} instance for fluid chaining
     */
    public GeomBar<DF> position(Position position) {
        setPosition(position);
        return this;
    }

    /**
     * Sets the full parameterised position adjustment ({@code Positions.dodge(width)},
     * {@code Positions.stack(reverse)}, ...).
     *
     * @param adjust the {@link PositionAdjust} to apply
     * @return this {@code GeomBar} instance for fluid chaining
     */
    public GeomBar<DF> position(PositionAdjust adjust) {
        setAdjust(adjust);
        return this;
    }

    /**
     * Sets the bar width factor relative to available tick spacing. This is the
     * legacy spelling for a dodge width; prefer {@link Positions#dodge(double)}
     * when configuring via a {@link PositionAdjust}.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code GeomBar} instance for fluid chaining
     */
    public GeomBar<DF> widthFactor(double widthFactor) {
        this.widthFactor = widthFactor;
        // For a dodged geometry the width factor IS the legacy spelling of the
        // dodge width: push it into the adjustment so it takes effect instead of
        // the default (0.9). Non-dodged geometries keep their own factor.
        // The default DODGE constant reports DODGE as its mode but is not a
        // Dodge record, so dispatch on the mode rather than the record type.
        if (adjust.mode() == Position.DODGE) {
            this.adjust = Positions.dodge(widthFactor);
        }
        return this;
    }

    /**
     * Sets a constant default fill color for ungrouped bar rendering.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomBar} instance for fluid chaining
     */
    public GeomBar<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    @Override
    public void attach(Stat<DF> stat, PositionAdjust position, LayerParams params) {
        this.attachedStat = stat;
        this.attachedParams = params == null ? LayerParams.empty() : params;
        setAdjust(position);
    }

    /**
     * Returns the constant fill colour this bar layer paints with when no
     * colour/fill aesthetic is mapped: the layer {@link LayerParams} fill if
     * given, else the configured geometry fill, else the geometry default.
     *
     * @return constant fill {@link Color}, never {@code null}
     */
    @Override
    public Color getCustomColor() {
        return attachedParams.fill() != null ? attachedParams.fill() : defaultFill;
    }

    @Override
    public LayerData prepare(PlotContext<DF> ctx) {
        if (attachedStat == null) {
            return LayerData.NONE;
        }
        var statData = attachedStat.computeLayer(ctx.globalDf(), ctx.extractor(), ctx.aes(), attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return LayerData.NONE;
        }
        return new LayerData.StatLayerData<>(statData, ctx.extractor());
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        if (attachedStat == null) {
            return super.expandDomain(bounds, ctx, xDiscrete, yDiscrete);
        }
        var statData = attachedStat.computeLayer(ctx.globalDf(), ctx.extractor(), ctx.aes(), attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return bounds;
        }
        var valueCol = valueColumn(statData, ctx.aes());
        if (valueCol == null) {
            return bounds;
        }
        var values = statData.columnAsDoubles(valueCol);
        var xs = statData.column("x");
        var stackTop = new HashMap<Object, Double>();
        double dataMaxY = 0.0;
        for (var i = 0; i < values.size(); i++) {
            var v = values.get(i);
            if (Double.isNaN(v)) {
                continue;
            }
            double y = v;
            if (position == Position.STACK && xs != null) {
                y = stackTop.merge(xs.get(i), v, Double::sum);
            }
            if (y > dataMaxY) {
                dataMaxY = y;
            }
        }
        return new Bounds(bounds.xMin(), bounds.xMax(),
                Math.min(bounds.yMin(), 0.0), Math.max(bounds.yMax(), dataMaxY));
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        if (attachedStat != null) {
            renderStat(gc, ctx);
            return;
        }
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

        var groupCol = AesRendering.rawGroupColumn(aes);

        var baselineY = sy.minPixel();
        var baselineX = sx.minPixel();

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        // A polar coordinate system replaces each bar rectangle with an annular
        // wedge: the angular span of the bar and its radial extent.
        if (coord instanceof CoordPolar polar) {
            renderPolar(gc, df, ext, aes, sx, sy, polar, rawX, rawY, n, scales);
            gc.restore();
            return;
        }

        var finalBarWidth = resolvedBarWidth(coord, sx, sy, rawX, n);

        // --- Render pipeline ---
        if (groupCol == null || ext.getColumn(df, groupCol) == null) {
            // --- Case A: Ungrouped bars ---
            // Identity bars (Geoms.col / bar with stat identity) use the same
            // constant-fill default as the counted stat path — the layer params
            // fill, else the configured geometry fill, else the geometry
            // default — so paint and tooltip fallback always agree.
            var baseFill = attachedParams.fill() != null ? attachedParams.fill() : defaultFill;
            var baseStroke = attachedParams.color() != null ? attachedParams.color() : baseFill.darker();
            gc.setFill(baseFill);
            gc.setStroke(baseStroke);

            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                if (rx == null || ry == null) {
                    continue;
                }

                double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
                var yDouble = (coord.isFlipped() ? sx : sy).toData(ry);

                if (coord.isFlipped()) {
                    var cx = coord.xPixel(sx, sy, xDouble, yDouble);
                    var cy = coord.yPixel(sx, sy, xDouble, yDouble);
                    var barTop = cy - (finalBarWidth / 2.0);
                    var barWidth = Math.abs(cx - baselineX);
                    gc.fillRect(baselineX, barTop, barWidth, finalBarWidth);
                    gc.strokeRect(baselineX, barTop, barWidth, finalBarWidth);
                } else {
                    var cx = coord.xPixel(sx, sy, xDouble, yDouble);
                    var cy = coord.yPixel(sx, sy, xDouble, yDouble);
                    var barLeft = cx - (finalBarWidth / 2.0);
                    var barHeight = Math.abs(baselineY - cy);
                    gc.fillRect(barLeft, cy, finalBarWidth, barHeight);
                    gc.strokeRect(barLeft, cy, finalBarWidth, barHeight);
                }
            }
        } else {
            // --- Case B: Categorical grouping (DODGE / STACK) ---
            var groupColName = groupCol;
            var groupData = ext.getColumn(df, groupColName);

            var grid = new LinkedHashMap<Double, Map<Object, Double>>();
            var allGroups = new ArrayList<Object>();

            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                var rg = groupData.get(i);
                if (rx == null || ry == null || rg == null) {
                    continue;
                }

                double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
                if (!allGroups.contains(rg)) {
                    allGroups.add(rg);
                }
                grid.computeIfAbsent(xDouble, k -> new HashMap<>()).put(rg, ((Number) ry).doubleValue());
            }

            allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));
            if (adjust instanceof PositionAdjust.Stack s && s.reverse()) {
                Collections.reverse(allGroups);
            }
            var numGroups = allGroups.size();
            var subBarWidth = finalBarWidth / numGroups;

            for (var xEntry : grid.entrySet()) {
                var xDouble = xEntry.getKey();
                var groupMap = xEntry.getValue();
                double currentStackOffset = 0.0;

                if (coord.isFlipped()) {
                    var cy = sy.toPixel(xDouble);
                    var exactBarTop = cy - (finalBarWidth / 2.0);

                    for (var gIdx = 0; gIdx < numGroups; gIdx++) {
                        var groupKey = allGroups.get(gIdx);
                        if (!groupMap.containsKey(groupKey)) {
                            continue;
                        }

                        var yDouble = groupMap.get(groupKey);
                        var resolvedColor = scales.resolvedColorFor(groupCol, groupKey);
                        var color = resolvedColor != null ? resolvedColor : defaultFill;

                        gc.setFill(color);
                        gc.setStroke(color.darker());

                        gc.setStroke(color.darker());

                        if (position == Position.DODGE) {
                            var specificTop = exactBarTop + (gIdx * subBarWidth);
                            var cx = sx.toPixel(yDouble);
                            gc.fillRect(baselineX, specificTop, Math.abs(cx - baselineX), subBarWidth);
                            gc.strokeRect(baselineX, specificTop, Math.abs(cx - baselineX), subBarWidth);
                        } else if (position == Position.STACK) {
                            var lowerBound = currentStackOffset;
                            var upperBound = currentStackOffset + yDouble;
                            var pLower = (lowerBound == 0.0) ? baselineX : sx.toPixel(lowerBound);
                            var pUpper = sx.toPixel(upperBound);

                            gc.fillRect(pLower, exactBarTop, Math.abs(pUpper - pLower), finalBarWidth);
                            gc.strokeRect(pLower, exactBarTop, Math.abs(pUpper - pLower), finalBarWidth);
                            currentStackOffset = upperBound;
                        }
                    }
                } else {
                    var cx = sx.toPixel(xDouble);
                    var exactBarLeft = cx - (finalBarWidth / 2.0); // Guaranteed centered within the width cap

                    for (var gIdx = 0; gIdx < numGroups; gIdx++) {
                        var groupKey = allGroups.get(gIdx);
                        if (!groupMap.containsKey(groupKey)) {
                            continue;
                        }

                        var yDouble = groupMap.get(groupKey);
                        var resolvedColor = scales.resolvedColorFor(groupCol, groupKey);
                        var color = resolvedColor != null ? resolvedColor : defaultFill;

                        gc.setFill(color);
                        gc.setStroke(color.darker());

                        if (position == Position.DODGE) {
                            var specificLeft = exactBarLeft + (gIdx * subBarWidth);
                            var cy = sy.toPixel(yDouble);
                            gc.fillRect(specificLeft, cy, subBarWidth, Math.abs(baselineY - cy));
                            gc.strokeRect(specificLeft, cy, subBarWidth, Math.abs(baselineY - cy));
                        } else if (position == Position.STACK) {
                            var lowerBound = currentStackOffset;
                            var upperBound = currentStackOffset + yDouble;
                            var pLower = (lowerBound == 0.0) ? baselineY : sy.toPixel(lowerBound);
                            var pUpper = sy.toPixel(upperBound);

                            // finalBarWidth is enforced unconditionally
                            gc.fillRect(exactBarLeft, pUpper, finalBarWidth, Math.abs(pLower - pUpper));
                            gc.strokeRect(exactBarLeft, pUpper, finalBarWidth, Math.abs(pLower - pUpper));

                            currentStackOffset = upperBound;
                        }
                    }
                }
            }
        }

        gc.restore();
    }

    /**
     * The stat-consuming render path used when a {@link Stat} was attached via
     * {@link Plot#layer}. Runs the stat over the panel's partition and draws one
     * bar per stat row: the x position comes from the stat's {@code x} column,
     * the bar height from the mapped (possibly computed) value column — by
     * default the {@code count} output of {@code Stats.count} — and the fill from
     * the mapped {@code afterStat} variable when one is present.
     */
    private void renderStat(DrawSurface gc, PanelContext<DF> ctx) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var scales = ctx.plot().scales();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();

        var statData = attachedStat.computeLayer(df, ext, aes, attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return;
        }
        var xValues = statData.column("x");
        if (xValues == null) {
            return;
        }
        var n = statData.rowCount();
        var values = AesRendering.computedHeights(ctx, statData, aes);
        if (values == null) {
            var valueCol = valueColumn(statData, aes);
            if (valueCol == null) {
                return;
            }
            values = statData.columnAsDoubles(valueCol);
        }

        // A computed (afterStat) fill, an after-scale reference, or a staged
        // pipeline drives per-bar colour; a constant fill default comes from the
        // layer params, and falls back to the geom default fill.
        var fillValue = aes.fillValue();
        var fillRes = AesRendering.resolution(ctx, aes, statData, null);
        var baseFill = attachedParams.fill() != null ? attachedParams.fill() : defaultFill;
        var baseStroke = attachedParams.color() != null ? attachedParams.color() : baseFill.darker();

        var baselineY = sy.minPixel();
        var baselineX = sx.minPixel();
        var finalBarWidth = resolvedBarWidth(coord, sx, sy, xValues, n);

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        // Rows sharing an x position accumulate into one stack under STACK.
        var stackTop = new HashMap<Double, Double>();
        for (var i = 0; i < n; i++) {
            var rx = xValues.get(i);
            var value = values.get(i);
            if (rx == null || Double.isNaN(value)) {
                continue;
            }
            double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
            var yDouble = value;
            if (position == Position.STACK) {
                yDouble = stackTop.merge(xDouble, value, Double::sum);
            }

            var fill = baseFill;
            var stroke = baseStroke;
            var barFill = AesRendering.fillColor(statData, scales, fillRes, fillValue, i);
            if (barFill != null) {
                fill = barFill;
                stroke = barFill.darker();
            }
            gc.setFill(fill);
            gc.setStroke(stroke);

            if (coord.isFlipped()) {
                var cy = sy.toPixel(xDouble);
                var cx = sx.toPixel(yDouble);
                var barTop = cy - (finalBarWidth / 2.0);
                var barWidth = Math.abs(cx - baselineX);
                gc.fillRect(baselineX, barTop, barWidth, finalBarWidth);
                gc.strokeRect(baselineX, barTop, barWidth, finalBarWidth);
            } else {
                var cx = sx.toPixel(xDouble);
                var cy = sy.toPixel(yDouble);
                var barLeft = cx - (finalBarWidth / 2.0);
                var barHeight = Math.abs(baselineY - cy);
                gc.fillRect(barLeft, cy, finalBarWidth, barHeight);
                gc.strokeRect(barLeft, cy, finalBarWidth, barHeight);
            }
        }
        gc.restore();
    }

    /**
     * The stat-consuming hover hit-test, mirroring {@code renderStat}: locates
     * the bar whose rectangle contains the mouse point and reports its x
     * category and value.
     */
    private String locateStat(PanelContext<DF> ctx, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();

        var statData = attachedStat.computeLayer(df, ext, aes, attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return null;
        }
        var xValues = statData.column("x");
        if (xValues == null) {
            return null;
        }
        var n = statData.rowCount();
        var values = AesRendering.computedHeights(ctx, statData, aes);
        if (values == null) {
            var valueCol = valueColumn(statData, aes);
            if (valueCol == null) {
                return null;
            }
            values = statData.columnAsDoubles(valueCol);
        }

        var baselineY = sy.minPixel();
        var baselineX = sx.minPixel();
        var finalBarWidth = resolvedBarWidth(coord, sx, sy, xValues, n);
        var xIsDate = xValues.stream().anyMatch(LocalDate.class::isInstance);
        var xScale = coord.isFlipped() ? sy : sx;

        var stackTop = new HashMap<Double, Double>();
        for (var i = 0; i < n; i++) {
            var rx = xValues.get(i);
            var value = values.get(i);
            if (rx == null || Double.isNaN(value)) {
                continue;
            }
            double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
            var yDouble = value;
            if (position == Position.STACK) {
                yDouble = stackTop.merge(xDouble, value, Double::sum);
            }

            double bx, by, bw, bh;
            if (coord.isFlipped()) {
                var cy = sy.toPixel(xDouble);
                var cx = sx.toPixel(yDouble);
                by = cy - (finalBarWidth / 2.0);
                bx = baselineX;
                bw = Math.abs(cx - baselineX);
                bh = finalBarWidth;
            } else {
                var cx = sx.toPixel(xDouble);
                var cy = sy.toPixel(yDouble);
                bx = cx - (finalBarWidth / 2.0);
                by = cy;
                bw = finalBarWidth;
                bh = Math.abs(baselineY - cy);
            }
            if (mx >= bx && mx <= (bx + bw) && my >= by && my <= (by + bh)) {
                var labelX = AesRendering.xAxisName(ctx) + ": " + AesRendering.xValueLabel(xScale, xIsDate, xDouble);
                var colourHint = AesRendering.statColourHint(statData, aes, i);
                return AesRendering.statValueTooltip(colourHint, labelX, yDouble);
            }
        }
        return null;
    }

    /**
     * The bar width shared by the raw and stat-consuming render and locate
     * paths, computed from the x values' nearest-neighbour pixel spacing and a
     * position-dependent width factor.
     * <p>
     * For a dodged cluster the dodge width IS the cluster width as a fraction
     * of group spacing (conventional semantics): no arbitrary pixel cap, so
     * {@code Positions.dodge(width)} visibly widens/narrows the bars. Stacked
     * bars are capped at 26 pixels, everything else at 30.
     */
    private double resolvedBarWidth(Coord coord, Scale sx, Scale sy, List<?> xValues, int n) {
        var uniqueXVals = new ArrayList<Double>();
        for (var i = 0; i < n; i++) {
            var rx = xValues.get(i);
            if (rx == null) {
                continue;
            }
            double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
            if (!uniqueXVals.contains(xDouble)) {
                uniqueXVals.add(xDouble);
            }
        }
        uniqueXVals.sort(Double::compareTo);

        var minPixelSpacing = Double.MAX_VALUE;
        if (uniqueXVals.size() > 1) {
            for (var i = 0; i < uniqueXVals.size() - 1; i++) {
                double dist = coord.isFlipped()
                        ? Math.abs(sy.toPixel(uniqueXVals.get(i + 1)) - sy.toPixel(uniqueXVals.get(i)))
                        : Math.abs(sx.toPixel(uniqueXVals.get(i + 1)) - sx.toPixel(uniqueXVals.get(i)));
                if (dist > 0 && dist < minPixelSpacing) {
                    minPixelSpacing = dist;
                }
            }
        }
        if (minPixelSpacing == Double.MAX_VALUE || minPixelSpacing <= 0) {
            minPixelSpacing = coord.isFlipped() ? Math.abs(sy.maxPixel() - sy.minPixel()) : Math.abs(sx.maxPixel() - sx.minPixel());
        }

        var effectiveWidthFactor = widthFactor;
        if (position == Position.IDENTITY) {
            effectiveWidthFactor = 1.0;
        } else if (position == Position.STACK) {
            effectiveWidthFactor = 0.40;
        } else if (position == Position.DODGE) {
            effectiveWidthFactor = adjust instanceof PositionAdjust.Dodge d
                    ? d.width()
                    : Positions.DODGE_WIDTH;
        }

        var calculatedWidth = minPixelSpacing * effectiveWidthFactor;
        if (position == Position.DODGE) {
            return calculatedWidth;
        }
        if (position == Position.STACK) {
            return (calculatedWidth > 26.0) ? 26.0 : calculatedWidth;
        }
        return (calculatedWidth > 30.0) ? 30.0 : calculatedWidth;
    }

    /**
     * The value column driving bar height for the stat-consuming path: the
     * mapped {@code y} when it names a stat output variable (an {@code afterStat}
     * mapping), else the stat's {@code count} output, else the first numeric
     * output column that is not {@code x}.
     */
    private static String valueColumn(StatData statData, Aes aes) {
        var y = aes == null ? null : aes.y();
        if (y != null) {
            var bare = Aes.isComputed(y) ? Aes.statColumn(y) : y;
            if (statData.columnNames().contains(bare)) {
                return bare;
            }
        }
        if (statData.columnNames().contains("count")) {
            return "count";
        }
        for (var name : statData.columnNames()) {
            if (!name.equals("x")) {
                var col = statData.columnAsDoubles(name);
                for (var v : col) {
                    if (!Double.isNaN(v)) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The polar rendering path. Bars accumulate the value along the radial
     * axis (rose, theta = "x") or the angular axis (pie, theta = "y"); in
     * either case x positions the band the bar occupies and y carries the
     * value being drawn. Stacked groups pile their wedges into rings.
     */
    private void renderPolar(DrawSurface gc, DF data, DataExtractor<DF> ext, Aes aes,
                             Scale sx, Scale sy, CoordPolar polar,
                             List<?> rawX, List<?> rawY, int n, ResolvedScales<DF> scales) {
        var groupCol = AesRendering.rawGroupColumn(aes);
        var groupData = groupCol != null ? ext.getColumn(data, groupCol) : null;

        // One bar per x position; groups at the same position merge (STACK) or
        // share the band (IDENTITY).
        var grid = new LinkedHashMap<Double, Map<Object, Double>>();
        var allGroups = new ArrayList<Object>();
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            var group = groupData == null ? null : groupData.get(i);
            if (rx == null || ry == null || (groupData != null && group == null)) {
                continue;
            }
            var xPos = sx.toData(rx);
            var value = ((Number) sy.toData(ry)).doubleValue();
            if (group != null && !allGroups.contains(group)) {
                allGroups.add(group);
            }
            grid.computeIfAbsent(xPos, k -> new HashMap<>()).merge(group, value, Double::sum);
        }
        if (grid.isEmpty()) {
            return;
        }
        allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));
        if (adjust instanceof PositionAdjust.Stack s && s.reverse()) {
            Collections.reverse(allGroups);
        }

        var band = bandWidth(sx);
        var half = band * widthFactor / 2.0;
        var stacking = position == Position.STACK;
        var order = allGroups.isEmpty() ? Collections.<Object>singletonList(null) : allGroups;

        if (polar.isThetaX()) {
            // Rose: each x band is one angular wedge, groups stack along the radius.
            for (var entry : grid.entrySet()) {
                var xPos = entry.getKey();
                var groupMap = entry.getValue();
                double cumulative = 0.0;
                for (var group : order) {
                    var value = groupMap.get(group);
                    if (value == null) {
                        continue;
                    }
                    var rLow = stacking ? cumulative : 0.0;
                    var rHigh = stacking ? cumulative + value : value;
                    paintWedge(gc, polar, xPos - half, xPos + half, rLow, rHigh, wedgeColor(group, scales, groupCol));
                    if (stacking) {
                        cumulative = rHigh;
                    }
                }
            }
            return;
        }

        // Pie: the stacked counts sweep the angle; a full turn covers the sum of
        // every wedge, so the disc is partitioned exactly like the
        // coordPolar(theta = "y") regardless of the count axis headroom.
        double total = 0.0;
        for (var entry : grid.entrySet()) {
            for (var group : order) {
                var value = entry.getValue().get(group);
                if (value != null) {
                    total += value;
                }
            }
        }
        var thetaScale = polar.thetaScale();
        var lo = thetaScale.minData();
        var hi = thetaScale.maxData();
        total = total > 0 ? total : (hi - lo);
        for (var entry : grid.entrySet()) {
            var xPos = entry.getKey();
            var groupMap = entry.getValue();
            double cumulative = 0.0;
            for (var group : order) {
                var value = groupMap.get(group);
                if (value == null) {
                    continue;
                }
                var thLow = lo + cumulative / total * (hi - lo);
                var thHigh = lo + (cumulative + value) / total * (hi - lo);
                paintWedge(gc, polar, thLow, thHigh, xPos - half, xPos + half, wedgeColor(group, scales, groupCol));
                cumulative += value;
            }
        }
    }

    /**
     * The width of one band on the x scale, in data units — a category for a
     * discrete axis, the finest tick spacing otherwise.
     */
    private static double bandWidth(Scale scale) {
        if (scale.isDiscrete()) {
            return 1.0;
        }
        var ticks = scale.calculateTicks(10, null);
        var min = Double.MAX_VALUE;
        for (var i = 1; i < ticks.size(); i++) {
            var d = ticks.get(i) - ticks.get(i - 1);
            if (d > 0 && d < min) {
                min = d;
            }
        }
        return min == Double.MAX_VALUE ? 1.0 : Math.max(min, 1e-6);
    }

    private void paintWedge(DrawSurface gc, CoordPolar polar,
                            double thLow, double thHigh, double rLow, double rHigh, Color color) {
        gc.setFill(color);
        polar.drawWedge(gc, thLow, thHigh, rLow, rHigh);
    }

    private Color wedgeColor(Object group, ResolvedScales<DF> scales, String groupCol) {
        if (group == null) {
            return defaultFill;
        }
        var c = scales.resolvedColorFor(groupCol, group);
        return c != null ? c : defaultFill;
    }

    /**
     * The polar hover hit-test, mirroring {@code renderPolar}: the same band,
     * stack and pie-partition arithmetic, answering with the wedge that
     * contains the mouse point.
     */
    private String locatePolar(CoordPolar polar, DataExtractor<DF> ext, DF data, Aes aes,
                               Scale sx, Scale sy, List<?> rawX, List<?> rawY, int n,
                               Scale xScale, boolean xIsDate, String xName, double mx, double my) {
        var groupCol = AesRendering.rawGroupColumn(aes);
        var groupData = groupCol != null ? ext.getColumn(data, groupCol) : null;

        var grid = new LinkedHashMap<Double, Map<Object, Double>>();
        var allGroups = new ArrayList<Object>();
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            var group = groupData == null ? null : groupData.get(i);
            if (rx == null || ry == null || (groupData != null && group == null)) {
                continue;
            }
            var xPos = sx.toData(rx);
            var value = ((Number) sy.toData(ry)).doubleValue();
            if (group != null && !allGroups.contains(group)) {
                allGroups.add(group);
            }
            grid.computeIfAbsent(xPos, k -> new HashMap<>()).merge(group, value, Double::sum);
        }
        if (grid.isEmpty()) {
            return null;
        }
        allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));
        if (adjust instanceof PositionAdjust.Stack s && s.reverse()) {
            Collections.reverse(allGroups);
        }

        var band = bandWidth(sx);
        var half = band * widthFactor / 2.0;
        var stacking = position == Position.STACK;
        var order = allGroups.isEmpty() ? Collections.<Object>singletonList(null) : allGroups;

        if (polar.isThetaX()) {
            for (var entry : grid.entrySet()) {
                var xPos = entry.getKey();
                var groupMap = entry.getValue();
                double cumulative = 0.0;
                for (var group : order) {
                    var value = groupMap.get(group);
                    if (value == null) {
                        continue;
                    }
                    var rLow = stacking ? cumulative : 0.0;
                    var rHigh = stacking ? cumulative + value : value;
                    if (wedgeContains(polar, xPos - half, xPos + half, rLow, rHigh, mx, my)) {
                        return polarTooltip(group, xPos, value, xScale, xIsDate, xName);
                    }
                    if (stacking) {
                        cumulative = rHigh;
                    }
                }
            }
            return null;
        }

        // Pie: the stacked counts sweep the angle, exactly as renderPolar does.
        double total = 0.0;
        for (var entry : grid.entrySet()) {
            for (var group : order) {
                var value = entry.getValue().get(group);
                if (value != null) {
                    total += value;
                }
            }
        }
        var thetaScale = polar.thetaScale();
        var lo = thetaScale.minData();
        var hi = thetaScale.maxData();
        total = total > 0 ? total : (hi - lo);
        for (var entry : grid.entrySet()) {
            var xPos = entry.getKey();
            var groupMap = entry.getValue();
            double cumulative = 0.0;
            for (var group : order) {
                var value = groupMap.get(group);
                if (value == null) {
                    continue;
                }
                var thLow = lo + cumulative / total * (hi - lo);
                var thHigh = lo + (cumulative + value) / total * (hi - lo);
                if (wedgeContains(polar, thLow, thHigh, xPos - half, xPos + half, mx, my)) {
                    return polarTooltip(group, xPos, value, xScale, xIsDate, xName);
                }
                cumulative += value;
            }
        }
        return null;
    }

    /**
     * Tests whether a mouse point falls inside the annular wedge between two
     * theta values and two radius values, using the same angular convention as
     * {@link CoordPolar#thetaAngle(double)} (zero at twelve o'clock, positive
     * clockwise on screen).
     */
    private static boolean wedgeContains(CoordPolar polar, double thLow, double thHigh,
                                         double rLow, double rHigh, double mx, double my) {
        var dx = mx - polar.centerX();
        var dy = my - polar.centerY();
        var r = Math.hypot(dx, dy);
        var rInner = polar.rPixel(rLow);
        var rOuter = polar.rPixel(rHigh);
        if (r < rInner - 0.5 || r > rOuter + 0.5) {
            return false;
        }
        var raw0 = polar.thetaAngle(thLow);
        var raw1 = polar.thetaAngle(thHigh);
        // A band spanning the whole turn (the rings of a bulls-eye) covers every
        // angle, so only the radial band is tested.
        if (Math.abs(raw1 - raw0) >= 2.0 * Math.PI - 1e-9) {
            return true;
        }
        var m = mod2pi(Math.atan2(dy, dx));
        var a0 = mod2pi(raw0);
        var a1 = mod2pi(raw1);
        if (a1 >= a0) {
            return m >= a0 && m <= a1;
        }
        return m >= a0 || m <= a1;
    }

    /** Normalises an angle in radians into {@code [0, 2π)}. */
    private static double mod2pi(double a) {
        var r = a % (2.0 * Math.PI);
        return r < 0 ? r + 2.0 * Math.PI : r;
    }

    private static String polarTooltip(Object group, double xPos, double value,
                                       Scale xScale, boolean xIsDate, String xName) {
        var labelX = xName + ": " + AesRendering.xValueLabel(xScale, xIsDate, xPos);
        if (group == null) {
            return AesRendering.statValueTooltip(null, labelX, value);
        }
        return AesRendering.groupedValueTooltip(group, labelX, value);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        if (attachedStat != null) {
            return locateStat(ctx, mx, my);
        }
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

        var xIsDate = ext.columnType(df, aes.x()) == DataExtractor.ColumnType.DATE;
        var xScale = coord.isFlipped() ? sy : sx;
        var xName = AesRendering.xAxisName(ctx);

        var groupCol = AesRendering.rawGroupColumn(aes);

        // A polar coordinate system turns each bar into an annular wedge, so the
        // mouse point must be tested against angular and radial spans instead of
        // a Cartesian rectangle.
        if (coord instanceof CoordPolar polar) {
            return locatePolar(polar, ext, df, aes, sx, sy, rawX, rawY, n, xScale, xIsDate, xName, mx, my);
        }

        var finalBarWidth = resolvedBarWidth(coord, sx, sy, rawX, n);

        var baselineY = sy.minPixel();
        var baselineX = sx.minPixel();

        // --- Case A: Ungrouped bars ---
        if (groupCol == null || ext.getColumn(df, groupCol) == null) {
            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                if (rx == null || ry == null) {
                    continue;
                }

                double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
                var yDouble = (coord.isFlipped() ? sx : sy).toData(ry);

                double bx, by, bw, bh;
                var bxCut = coord.xPixel(sx, sy, xDouble, yDouble);
                var byCut = coord.yPixel(sx, sy, xDouble, yDouble);
                if (coord.isFlipped()) {
                    by = byCut - (finalBarWidth / 2.0);
                    bx = baselineX;
                    bw = Math.abs(bxCut - baselineX);
                    bh = finalBarWidth;
                } else {
                    bx = bxCut - (finalBarWidth / 2.0);
                    by = byCut;
                    bw = finalBarWidth;
                    bh = Math.abs(baselineY - byCut);
                }

                if (mx >= bx && mx <= (bx + bw) && my >= by && my <= (by + bh)) {
                    var labelX = xName + ": " + ((rx instanceof LocalDate ld) ? ld.toString() : String.valueOf(rx));
                    return AesRendering.statValueTooltip(null, labelX, yDouble);
                }
            }
        } else {
            // --- Case B: Categorical grouping (DODGE / STACK) ---
            var groupColName = groupCol;
            var groupData = ext.getColumn(df, groupColName);

            var grid = new LinkedHashMap<Double, Map<Object, Double>>();
            var allGroups = new ArrayList<Object>();

            for (var i = 0; i < n; i++) {
                var rx = rawX.get(i);
                var ry = rawY.get(i);
                var rg = groupData.get(i);
                if (rx == null || ry == null || rg == null) {
                    continue;
                }
                double xDouble = (coord.isFlipped() ? sy : sx).toData(rx);
                if (!allGroups.contains(rg)) {
                    allGroups.add(rg);
                }
                grid.computeIfAbsent(xDouble, k -> new HashMap<>()).put(rg, ((Number) ry).doubleValue());
            }
            allGroups.sort((a, b) -> a.toString().compareTo(b.toString()));
            if (adjust instanceof PositionAdjust.Stack s && s.reverse()) {
                Collections.reverse(allGroups);
            }
            var numGroups = allGroups.size();
            var subBarWidth = finalBarWidth / numGroups;

            for (var xEntry : grid.entrySet()) {
                var xDouble = xEntry.getKey();
                var groupMap = xEntry.getValue();
                double currentStackOffset = 0.0;

                if (coord.isFlipped()) {
                    // === Horizontal mode hit-test ===
                    var cy = sy.toPixel(xDouble);
                    var exactBarTop = cy - (finalBarWidth / 2.0);

                    for (var gIdx = 0; gIdx < numGroups; gIdx++) {
                        var groupKey = allGroups.get(gIdx);
                        if (!groupMap.containsKey(groupKey)) {
                            continue;
                        }

                        var yDouble = groupMap.get(groupKey);

                        if (position == Position.DODGE) {
                            var specificTop = exactBarTop + (gIdx * subBarWidth);
                            var cx = sx.toPixel(yDouble);
                            if (mx >= baselineX && mx <= cx && my >= specificTop && my <= (specificTop + subBarWidth)) {
                                var labelX = xName + ": " + AesRendering.xValueLabel(xScale, xIsDate, xDouble);
                                return AesRendering.groupedValueTooltip(groupKey, labelX, yDouble);
                            }
                        } else if (position == Position.STACK) {
                            // === Horizontal stack hover-test ===
                            var lower = currentStackOffset;
                            var upper = currentStackOffset + yDouble;
                            var pLower = (lower == 0.0) ? baselineX : sx.toPixel(lower);
                            var pUpper = sx.toPixel(upper);

                            if (mx >= pLower && mx <= pUpper && my >= exactBarTop && my <= (exactBarTop + finalBarWidth)) {
                                var labelX = xName + ": " + AesRendering.xValueLabel(xScale, xIsDate, xDouble);
                                return AesRendering.groupedValueTooltip(groupKey, labelX, yDouble);
                            }
                            currentStackOffset = upper;
                        }
                    }
                } else {
                    // === Vertical standard mode hit-test ===
                    var cx = sx.toPixel(xDouble);
                    var exactBarLeft = cx - (finalBarWidth / 2.0);

                    for (var gIdx = 0; gIdx < numGroups; gIdx++) {
                        var groupKey = allGroups.get(gIdx);
                        if (!groupMap.containsKey(groupKey)) {
                            continue;
                        }

                        var yDouble = groupMap.get(groupKey);

                        if (position == Position.DODGE) {
                            var specificLeft = exactBarLeft + (gIdx * subBarWidth);
                            var cy = sy.toPixel(yDouble);
                            if (mx >= specificLeft && mx <= (specificLeft + subBarWidth) && my >= cy && my <= baselineY) {
                                var labelX = xName + ": " + AesRendering.xValueLabel(xScale, xIsDate, xDouble);
                                return AesRendering.groupedValueTooltip(groupKey, labelX, yDouble);
                            }
                        } else if (position == Position.STACK) {
                            // === Vertical stack hover-test ===
                            var lower = currentStackOffset;
                            var upper = currentStackOffset + yDouble;
                            var pLower = (lower == 0.0) ? baselineY : sy.toPixel(lower);
                            var pUpper = sy.toPixel(upper);

                            // In JavaFX pUpper is numerically smaller (higher in the window) than pLower
                            if (mx >= exactBarLeft && mx <= (exactBarLeft + finalBarWidth) && my <= pLower && my >= pUpper) {
                                var labelX = xName + ": " + AesRendering.xValueLabel(xScale, xIsDate, xDouble);
                                return AesRendering.groupedValueTooltip(groupKey, labelX, yDouble);
                            }
                            currentStackOffset = upper;
                        }
                    }
                }
            }
        }
        return null;
    }
}
