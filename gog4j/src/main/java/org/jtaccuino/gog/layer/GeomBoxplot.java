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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;

/**
 * Box-and-whiskers plot geometry layer implementation (Tukey boxplot).
 * <p>
 * Groups the dataset by the categorical x aesthetic and renders a five-number
 * summary per group: the box spans the 25th–75th percentiles (hinges), the band
 * marks the median, and whiskers extend to the most extreme values within
 * {@code 1.5 × IQR} of the hinges. Values beyond those fences are drawn as
 * outlier points. Horizontal orientation via {@link Coord#isFlipped()} is supported.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomBoxplot<DF> implements Layer<DF> {

    private double widthFactor = 0.8;
    private Color defaultFill = Color.web("#3182bd");
    private Color defaultStroke = Color.web("#1c5480");
    private Color defaultOutlierColor = Color.web("#e31a1c");
    private double outlierSize = 2.0;
    private double medianLineWidth = 2.5;
    private boolean notch = false;
    private double notchwidth = 0.5;
    private boolean varwidth = false;
    private boolean drawOutliers = true;
    private PointShape outlierShape = PointShape.CIRCLE;
    private Position position = Position.DODGE;
    private double dodgeWidth = 0.9;

    /**
     * Creates a box-and-whiskers geometry with default styling.
     */
    public GeomBoxplot() {
    }

    /**
     * Sets the position adjustment mode. {@link Position#DODGE} (default)
     * splits each group into sub-groups by the mapped fill/color column and
     * places them side by side (the default for {@code Geoms.boxplot}).
     * {@link Position#IDENTITY} draws all sub-boxes at the group's position,
     * overlapping at full width.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> position(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Sets the fraction of each dodge slot occupied by a box, leaving the
     * remainder as spacing between neighbouring dodged boxes. A value of
     * {@code 1.0} makes the boxes touch, {@code 0.9} (default) leaves a small
     * gap, following the {@code Positions.dodge(width = 0.9)}.
     *
     * @param dodgeWidth box width as a fraction of the dodge slot, between 0.0 and 1.0
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> dodgeWidth(double dodgeWidth) {
        this.dodgeWidth = dodgeWidth;
        return this;
    }

    /**
     * Sets the box width factor relative to the available tick spacing.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> widthFactor(double widthFactor) {
        this.widthFactor = widthFactor;
        return this;
    }

    /**
     * Sets the constant fill color of the boxes.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the stroke color of the boxes and whiskers.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> color(Color c) {
        this.defaultStroke = c;
        return this;
    }

    /**
     * Sets the fill color of the outlier points.
     *
     * @param c the JavaFX outlier {@link Color}
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> outlierColor(Color c) {
        this.defaultOutlierColor = c;
        return this;
    }

    /**
     * Sets the outlier point radius in pixels.
     *
     * @param size the outlier point radius in pixels
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> outlierSize(double size) {
        this.outlierSize = size;
        return this;
    }

    /**
     * Sets the stroke width of the median band in pixels.
     *
     * @param width the median line stroke width in pixels
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> medianLineWidth(double width) {
        this.medianLineWidth = width;
        return this;
    }

    /**
     * Enables or disables notched box plots. Notches extend
     * {@code 1.58 * IQR / sqrt(n)} around the median, giving a roughly 95%
     * confidence interval for comparing medians (McGill et al. 1978).
     *
     * @param notch {@code true} to draw notches
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> notch(boolean notch) {
        this.notch = notch;
        return this;
    }

    /**
     * Sets the width of the notch relative to the box body (default {@code 0.5}).
     *
     * @param notchwidth notch width factor between 0.0 and 1.0
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> notchwidth(double notchwidth) {
        this.notchwidth = notchwidth;
        return this;
    }

    /**
     * If {@code true}, boxes are drawn with widths proportional to the square root
     * of the number of observations in each group (relative to the largest group).
     *
     * @param varwidth {@code true} for variable box widths
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> varwidth(boolean varwidth) {
        this.varwidth = varwidth;
        return this;
    }

    /**
     * Enables or disables outlier rendering entirely.
     *
     * @param draw {@code true} to render outlier points, {@code false} to suppress them
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> drawOutliers(boolean draw) {
        this.drawOutliers = draw;
        return this;
    }

    /**
     * Sets the {@link PointShape} symbol used for outlier points, or {@code null}
     * to suppress outliers completely.
     *
     * @param shape the outlier point shape, or {@code null} to hide outliers
     * @return this {@code GeomBoxplot} instance for fluid chaining
     */
    public GeomBoxplot<DF> outlierShape(PointShape shape) {
        this.outlierShape = shape;
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

        var fillCol = aes.fill();
        if (fillCol == null) {
            fillCol = aes.color();
        }
        var rawFill = fillCol != null ? ext.getColumn(df, fillCol) : null;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        // 1. Group y values by x value, then by fill value (sub-groups).
        //    When a fill/color column is mapped and a group contains several
        //    distinct values (e.g. aes(fill = drv) across a class group), the
        //    sub-groups are rendered side by side, like the Positions.dodge.
        var groups = new LinkedHashMap<Object, Map<Object, List<Double>>>();
        var groupOrder = new ArrayList<Object>();
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx == null || ry == null) {
                continue;
            }
            var fv = (rawFill != null && i < rawFill.size()) ? rawFill.get(i) : null;
            var byFill = groups.computeIfAbsent(rx, k -> {
                groupOrder.add(rx);
                return new LinkedHashMap<>();
            });
            byFill.computeIfAbsent(fv, k -> new ArrayList<>()).add(Values.toDouble(ry));
        }
        if (groupOrder.isEmpty()) {
            gc.restore();
            return;
        }

        if (coord instanceof CoordPolar polar) {
            renderPolar(gc, polar, scales, fillCol, groups, groupOrder);
            gc.restore();
            return;
        }

        // The discrete (group) scale and the continuous (value) scale depend on orientation
        var groupScale = coord.isFlipped() ? sy : sx;
        var valueScale = coord.isFlipped() ? sx : sy;

        // 2. Sort groups by data-space position on the group axis
        groupOrder.sort((a, b) -> Double.compare(groupScale.toData(a), groupScale.toData(b)));

        // 3. Precise pixel spacing computation between group centers
        var minPixelSpacing = Double.MAX_VALUE;
        for (var i = 0; i < groupOrder.size() - 1; i++) {
            double dist = Math.abs(groupScale.toPixel(groupScale.toData(groupOrder.get(i + 1)))
                    - groupScale.toPixel(groupScale.toData(groupOrder.get(i))));
            if (dist > 0 && dist < minPixelSpacing) {
                minPixelSpacing = dist;
            }
        }
        if (minPixelSpacing == Double.MAX_VALUE || minPixelSpacing <= 0) {
            minPixelSpacing = Math.abs(groupScale.maxPixel() - groupScale.minPixel());
        }

        final var baseBoxWidth = Math.min(minPixelSpacing * widthFactor, 40.0);

        // When varwidth is enabled, scale each sub-box by sqrt(sub-group size)
        // relative to the largest sub-group
        var maxSqrtN = 0.0;
        if (varwidth) {
            for (var rx : groupOrder) {
                for (var values : groups.get(rx).values()) {
                    if (values.isEmpty()) {
                        continue;
                    }
                    maxSqrtN = Math.max(maxSqrtN, Math.sqrt(values.size()));
                }
            }
        }

        // 4. Render each sub-group, dodged within its group's slot
        for (var rx : groupOrder) {
            var subGroups = groups.get(rx);
            var subOrder = new ArrayList<Object>(subGroups.keySet());
            subOrder.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
            if (subOrder.isEmpty()) {
                continue;
            }
            var groupPix = groupScale.toPixel(groupScale.toData(rx));
            var dodge = position == Position.DODGE;
            var subWidth = dodge ? baseBoxWidth / subOrder.size() : baseBoxWidth;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.isEmpty()) {
                    continue;
                }
                Collections.sort(values);
                var stats = computeStats(values);
                var center = dodge
                        ? groupPix + (i - (subOrder.size() - 1) / 2.0) * subWidth
                        : groupPix;

                var boxWidth = (dodge && subOrder.size() > 1) ? subWidth * dodgeWidth : subWidth;
                if (varwidth && maxSqrtN > 0) {
                    boxWidth = Math.min(boxWidth * (Math.sqrt(values.size()) / maxSqrtN), 40.0);
                }

                Color fill = resolveColor(scales, fillCol, fv);
                Color stroke = defaultStroke != null ? defaultStroke : fill.darker();

                if (coord.isFlipped()) {
                    renderHorizontal(gc, valueScale, center, stats, boxWidth, fill, stroke);
                } else {
                    renderVertical(gc, valueScale, center, stats, boxWidth, fill, stroke);
                }
            }
        }

        gc.restore();
    }

    /**
     * Renders the grouped boxes through a polar coordinate system, the
     * {@code Geoms.boxplot() + Coords.coordRadial(start, end)} of the 3.5.0 reference release
     * release post: the group axis becomes the angle (so each category sweeps
     * a sector of the fan) and the value axis the radius. Each box is drawn as
     * an annular wedge from the lower to the upper hinge with its whiskers as
     * radial lines, its median and whisker caps as chords across the sector.
     */
    private void renderPolar(DrawSurface gc, CoordPolar polar, ResolvedScales<DF> scales, String fillCol,
                             Map<Object, Map<Object, List<Double>>> groups, List<Object> groupOrder) {
        var thetaScale = polar.thetaScale();

        // The band spacing in theta data units between neighbouring groups;
        // a discrete axis spaces its categories one unit apart.
        var spacing = Double.MAX_VALUE;
        for (var i = 0; i < groupOrder.size() - 1; i++) {
            var d = Math.abs(thetaScale.toData(groupOrder.get(i + 1)) - thetaScale.toData(groupOrder.get(i)));
            if (d > 0 && d < spacing) {
                spacing = d;
            }
        }
        if (spacing == Double.MAX_VALUE || spacing <= 0) {
            spacing = 1.0;
        }
        var baseBand = spacing * widthFactor;

        for (var rx : groupOrder) {
            var subGroups = groups.get(rx);
            var subOrder = new ArrayList<Object>(subGroups.keySet());
            subOrder.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
            if (subOrder.isEmpty()) {
                continue;
            }
            var centerData = thetaScale.toData(rx);
            var dodge = position == Position.DODGE;
            var subBand = dodge ? baseBand / subOrder.size() : baseBand;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.isEmpty()) {
                    continue;
                }
                Collections.sort(values);
                var stats = computeStats(values);
                var center = dodge
                        ? centerData + (i - (subOrder.size() - 1) / 2.0) * subBand
                        : centerData;
                var boxBand = (dodge && subOrder.size() > 1) ? subBand * dodgeWidth : subBand;

                Color fill = resolveColor(scales, fillCol, fv);
                Color stroke = defaultStroke != null ? defaultStroke : fill.darker();

                renderPolarBox(gc, polar, center, boxBand, stats, fill, stroke);
            }
        }
    }

    /**
     * Renders a single box of a polar boxplot: the box body as a filled and
     * outlined annular wedge from q1 to q3, the whiskers as radial lines at
     * the sector's centre, and the median and the whisker caps as chords
     * spanning the box's angular width.
     */
    private void renderPolarBox(DrawSurface gc, CoordPolar polar, double center, double boxBand,
                                BoxStats stats, Color fill, Color stroke) {
        var thLow = center - boxBand / 2.0;
        var thHigh = center + boxBand / 2.0;
        var cx = polar.centerX();
        var cy = polar.centerY();

        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);
        polar.drawWedge(gc, thLow, thHigh, stats.q1, stats.q3);
        polar.strokeWedge(gc, thLow, thHigh, stats.q1, stats.q3);

        // Whiskers run along the sector's centre ray (straight radial lines);
        // the caps and the median band follow the curvature of the ring at
        // their radius, so they are drawn as arcs across the box's angular
        // width rather than as chords.
        var am = polar.thetaAngle(center);
        var aLow = polar.thetaAngle(thLow);
        var aHigh = polar.thetaAngle(thHigh);
        var sweepDeg = Math.toDegrees(aHigh - aLow);
        var rUp = polar.rPixel(stats.upperWhisker);
        var rLo = polar.rPixel(stats.lowerWhisker);
        gc.strokeLine(cx + rLo * Math.cos(am), cy + rLo * Math.sin(am), cx + rUp * Math.cos(am), cy + rUp * Math.sin(am));
        gc.strokeArc(cx - rUp, cy - rUp, 2.0 * rUp, 2.0 * rUp, Math.toDegrees(aLow), sweepDeg);
        gc.strokeArc(cx - rLo, cy - rLo, 2.0 * rLo, 2.0 * rLo, Math.toDegrees(aLow), sweepDeg);

        gc.setLineWidth(medianLineWidth);
        var rMed = polar.rPixel(stats.median);
        gc.strokeArc(cx - rMed, cy - rMed, 2.0 * rMed, 2.0 * rMed, Math.toDegrees(aLow), sweepDeg);
        gc.setLineWidth(1.0);

        if (drawOutliers && outlierShape != null) {
            gc.setFill(defaultOutlierColor);
            gc.setStroke(defaultOutlierColor.darker());
            for (var o : stats.outliers) {
                var ro = polar.rPixel(o);
                outlierShape.draw(gc, cx + ro * Math.cos(am), cy + ro * Math.sin(am), outlierSize);
            }
        }
    }

    /**
     * Renders a vertical (y value) boxplot group centered at {@code cx}.
     */
    private void renderVertical(DrawSurface gc, Scale valueScale, double cx, BoxStats stats, double boxWidth, Color fill, Color stroke) {
        var left = cx - boxWidth / 2.0;
        var right = cx + boxWidth / 2.0;

        // Box (hinges q1..q3)
        var pQ1 = valueScale.toPixel(stats.q1);
        var pQ3 = valueScale.toPixel(stats.q3);
        var pMed = valueScale.toPixel(stats.median);
        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);

        double medLeft = left;
        double medRight = right;
        if (notch && stats.notchLower != null && stats.notchUpper != null) {
            var pNotchLower = valueScale.toPixel(stats.notchLower);
            var pNotchUpper = valueScale.toPixel(stats.notchUpper);
            var notchindent = (1.0 - notchwidth) * boxWidth / 2.0;
            medLeft = left + notchindent;
            medRight = right - notchindent;
            // Polygon mirrors the GeomCrossbar notched box: left edge pinches
            // inward at the median, then out to the upper/lower notch bounds.
            gc.fillPolygon(
                new double[]{left, left, medLeft, left, left, right, right, medRight, right, right, left},
                new double[]{pQ3, pNotchUpper, pMed, pNotchLower, pQ1, pQ1, pNotchLower, pMed, pNotchUpper, pQ3, pQ3},
                11
            );
            gc.strokePolygon(
                new double[]{left, left, medLeft, left, left, right, right, medRight, right, right, left},
                new double[]{pQ3, pNotchUpper, pMed, pNotchLower, pQ1, pQ1, pNotchLower, pMed, pNotchUpper, pQ3, pQ3},
                11
            );
        } else {
            gc.fillRect(left, Math.min(pQ1, pQ3), boxWidth, Math.abs(pQ3 - pQ1));
            gc.strokeRect(left, Math.min(pQ1, pQ3), boxWidth, Math.abs(pQ3 - pQ1));
        }

        // Whiskers + end caps
        var pUpper = valueScale.toPixel(stats.upperWhisker);
        var pLower = valueScale.toPixel(stats.lowerWhisker);
        gc.strokeLine(cx, pUpper, cx, pLower);
        gc.strokeLine(left, pUpper, right, pUpper);
        gc.strokeLine(left, pLower, right, pLower);

        // Median band
        gc.setLineWidth(medianLineWidth);
        gc.strokeLine(medLeft, pMed, medRight, pMed);
        gc.setLineWidth(1.0);

        // Outliers
        if (drawOutliers && outlierShape != null) {
            gc.setFill(defaultOutlierColor);
            gc.setStroke(defaultOutlierColor.darker());
            for (var o : stats.outliers) {
                var po = valueScale.toPixel(o);
                outlierShape.draw(gc, cx, po, outlierSize);
            }
        }
    }

    /**
     * Renders a horizontal (flipped, x value) boxplot group centered at {@code cy}.
     */
    private void renderHorizontal(DrawSurface gc, Scale valueScale, double cy, BoxStats stats, double boxWidth, Color fill, Color stroke) {
        var top = cy - boxWidth / 2.0;
        var bottom = cy + boxWidth / 2.0;

        // Box (hinges q1..q3)
        var pQ1 = valueScale.toPixel(stats.q1);
        var pQ3 = valueScale.toPixel(stats.q3);
        var pMed = valueScale.toPixel(stats.median);
        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);

        double medTop = top;
        double medBottom = bottom;
        if (notch && stats.notchLower != null && stats.notchUpper != null) {
            var pNotchLower = valueScale.toPixel(stats.notchLower);
            var pNotchUpper = valueScale.toPixel(stats.notchUpper);
            var notchindent = (1.0 - notchwidth) * boxWidth / 2.0;
            medTop = top + notchindent;
            medBottom = bottom - notchindent;
            gc.fillPolygon(
                new double[]{pQ1, pNotchLower, pMed, pNotchUpper, pQ3, pQ3, pNotchUpper, pMed, pNotchLower, pQ1, pQ1},
                new double[]{top, top, medTop, top, top, bottom, bottom, medBottom, bottom, bottom, top},
                11
            );
            gc.strokePolygon(
                new double[]{pQ1, pNotchLower, pMed, pNotchUpper, pQ3, pQ3, pNotchUpper, pMed, pNotchLower, pQ1, pQ1},
                new double[]{top, top, medTop, top, top, bottom, bottom, medBottom, bottom, bottom, top},
                11
            );
        } else {
            gc.fillRect(Math.min(pQ1, pQ3), top, Math.abs(pQ3 - pQ1), boxWidth);
            gc.strokeRect(Math.min(pQ1, pQ3), top, Math.abs(pQ3 - pQ1), boxWidth);
        }

        // Whiskers + end caps
        var pUpper = valueScale.toPixel(stats.upperWhisker);
        var pLower = valueScale.toPixel(stats.lowerWhisker);
        gc.strokeLine(pUpper, cy, pLower, cy);
        gc.strokeLine(pUpper, top, pUpper, bottom);
        gc.strokeLine(pLower, top, pLower, bottom);

        // Median band
        gc.setLineWidth(medianLineWidth);
        gc.strokeLine(pMed, medTop, pMed, medBottom);
        gc.setLineWidth(1.0);

        // Outliers
        if (drawOutliers && outlierShape != null) {
            gc.setFill(defaultOutlierColor);
            gc.setStroke(defaultOutlierColor.darker());
            for (var o : stats.outliers) {
                var po = valueScale.toPixel(o);
                outlierShape.draw(gc, po, cy, outlierSize);
            }
        }
    }

    /**
     * Resolves the fill color for a sub-group, either from the mapped color/fill
     * column or from the configured default fill. The scale is shared with the
     * legend, so the box fills and the legend always agree.
     */
    private Color resolveColor(ResolvedScales<DF> scales, String fillCol, Object fv) {
        if (fillCol != null && fv != null) {
            var c = scales.resolvedColorFor(fillCol, fv);
            return c != null ? c : defaultFill;
        }
        return defaultFill;
    }

    /**
     * Computes the Tukey five-number summary and outlier set for a sorted value list.
     */
    private BoxStats computeStats(List<Double> sorted) {
        var q1 = quantile(sorted, 0.25);
        var median = quantile(sorted, 0.50);
        var q3 = quantile(sorted, 0.75);
        var iqr = q3 - q1;
        var lowerFence = q1 - 1.5 * iqr;
        var upperFence = q3 + 1.5 * iqr;

        double lowerWhisker = q1;
        double upperWhisker = q3;
        var outliers = new ArrayList<Double>();
        for (var v : sorted) {
            if (v < lowerFence || v > upperFence) {
                outliers.add(v);
            }
        }
        for (var v : sorted) {
            if (v >= lowerFence) {
                lowerWhisker = v;
                break;
            }
        }
        for (int i = sorted.size() - 1; i >= 0; i--) {
            var v = sorted.get(i);
            if (v <= upperFence) {
                upperWhisker = v;
                break;
            }
        }
        Double notchLower = null;
        Double notchUpper = null;
        if (notch) {
            // 1.58 * IQR / sqrt(n) gives a roughly 95% CI for comparing medians (McGill et al. 1978).
            // Clamp to the hinges so the notch can never poke outside the box.
            var notchHalf = 1.58 * iqr / Math.sqrt(sorted.size());
            notchLower = Math.max(median - notchHalf, q1);
            notchUpper = Math.min(median + notchHalf, q3);
        }
        return new BoxStats(q1, median, q3, lowerWhisker, upperWhisker, outliers, sorted.size(), notchLower, notchUpper);
    }

    /**
     * Linear-interpolation quantile (equivalent to the type-7 default).
     */
    private double quantile(List<Double> sorted, double p) {
        var n = sorted.size();
        if (n == 1) {
            return sorted.get(0);
        }
        var h = (n - 1) * p;
        var lo = (int) Math.floor(h);
        var hi = (int) Math.ceil(h);
        var frac = h - lo;
        return sorted.get(lo) + frac * (sorted.get(hi) - sorted.get(lo));
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

        var fillCol = aes.fill();
        if (fillCol == null) {
            fillCol = aes.color();
        }
        var rawFill = fillCol != null ? ext.getColumn(df, fillCol) : null;

        var groups = new LinkedHashMap<Object, Map<Object, List<Double>>>();
        var groupOrder = new ArrayList<Object>();
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx == null || ry == null) {
                continue;
            }
            var fv = (rawFill != null && i < rawFill.size()) ? rawFill.get(i) : null;
            var byFill = groups.computeIfAbsent(rx, k -> {
                groupOrder.add(rx);
                return new LinkedHashMap<>();
            });
            byFill.computeIfAbsent(fv, k -> new ArrayList<>()).add(Values.toDouble(ry));
        }
        if (groupOrder.isEmpty()) {
            return null;
        }
        var groupScale = coord.isFlipped() ? sy : sx;
        groupOrder.sort((a, b) -> Double.compare(groupScale.toData(a), groupScale.toData(b)));

        var minPixelSpacing = Double.MAX_VALUE;
        for (var i = 0; i < groupOrder.size() - 1; i++) {
            double dist = Math.abs(groupScale.toPixel(groupScale.toData(groupOrder.get(i + 1)))
                    - groupScale.toPixel(groupScale.toData(groupOrder.get(i))));
            if (dist > 0 && dist < minPixelSpacing) {
                minPixelSpacing = dist;
            }
        }
        if (minPixelSpacing == Double.MAX_VALUE || minPixelSpacing <= 0) {
            minPixelSpacing = Math.abs(groupScale.maxPixel() - groupScale.minPixel());
        }
        final var baseBoxWidth = Math.min(minPixelSpacing * widthFactor, 40.0);

        var maxSqrtN = 0.0;
        if (varwidth) {
            for (var rx : groupOrder) {
                for (var values : groups.get(rx).values()) {
                    if (values.isEmpty()) {
                        continue;
                    }
                    maxSqrtN = Math.max(maxSqrtN, Math.sqrt(values.size()));
                }
            }
        }

        for (var rx : groupOrder) {
            var subGroups = groups.get(rx);
            var subOrder = new ArrayList<Object>(subGroups.keySet());
            subOrder.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
            if (subOrder.isEmpty()) {
                continue;
            }
            var groupPix = groupScale.toPixel(groupScale.toData(rx));
            var dodge = position == Position.DODGE;
            var subWidth = dodge ? baseBoxWidth / subOrder.size() : baseBoxWidth;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.isEmpty()) {
                    continue;
                }
                Collections.sort(values);
                var stats = computeStats(values);
                var center = dodge
                        ? groupPix + (i - (subOrder.size() - 1) / 2.0) * subWidth
                        : groupPix;

                var boxWidth = (dodge && subOrder.size() > 1) ? subWidth * dodgeWidth : subWidth;
                if (varwidth && maxSqrtN > 0) {
                    boxWidth = Math.min(boxWidth * (Math.sqrt(values.size()) / maxSqrtN), 40.0);
                }

                if (coord.isFlipped()) {
                    if (my >= center - boxWidth / 2.0 && my <= center + boxWidth / 2.0) {
                        return formatTooltip(rx, fv, stats);
                    }
                } else {
                    if (mx >= center - boxWidth / 2.0 && mx <= center + boxWidth / 2.0) {
                        return formatTooltip(rx, fv, stats);
                    }
                }
            }
        }
        return null;
    }

    private String formatTooltip(Object rx, Object fv, BoxStats stats) {
        var group = rx == null ? "" : String.valueOf(rx);
        if (fv != null) {
            group += "\n" + fv;
        }
        return String.format("%s\nn = %d\nmedian = %,.1f\nQ1 = %,.1f\nQ3 = %,.1f\nIQR = %,.1f\nwhiskers = %,.1f .. %,.1f",
                group,
                stats.count,
                stats.median, stats.q1, stats.q3, stats.q3 - stats.q1,
                stats.lowerWhisker, stats.upperWhisker);
    }

    /**
     * Immutable Tukey summary for a single boxplot group.
     */
    private record BoxStats(double q1, double median, double q3, double lowerWhisker, double upperWhisker, List<Double> outliers, int count, Double notchLower, Double notchUpper) {
    }
}
