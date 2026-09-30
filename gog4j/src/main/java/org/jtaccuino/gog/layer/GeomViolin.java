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
import org.jtaccuino.gog.stat.DensityModels;

/**
 * Violin plot geometry layer implementation.
 * <p>
 * A violin is a mirrored kernel-density estimate drawn the way a boxplot sits
 * on its group axis: each categorical x group renders a symmetric density
 * shape whose horizontal extent is proportional to the local density of the
 * y values. This mirrors the {@code Geoms.violin()} / {@code yDensityTransform()}
 * (Hintze &amp; Nelson 1998).
 * <p>
 * The density is a Gaussian kernel density estimate, using Silverman's rule of
 * thumb for the bandwidth ({@code Silverman's rule of thumb}) with a multiplicative
 * {@code adjust} parameter. Options mirror the reference implementation:
 * <ul>
 *   <li>{@code trim} — cut the tails of each violin at the data range (default {@code true})</li>
 *   <li>{@code scale} — {@code "area"} keeps all violins at the same area,
 *       {@code "count"} scales the area with the sample size, and
 *       {@code "width"} gives every violin the same maximum width</li>
 *   <li>{@code drawQuantiles} — optional horizontal marks at the 25/50/75%
 *       percentiles, as in the {@code quantileDrawing}</li>
 * </ul>
 * Horizontal orientation via {@link Coord#isFlipped()} is supported.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomViolin<DF> implements Layer<DF> {

    /** Grid resolution of each mirrored density profile. */
    private static final int GRID_POINTS = 128;

    private double widthFactor = 0.9;
    private Color defaultFill = Color.web("#9ecae1");
    private Color defaultStroke = Color.web("#3182bd");
    private String scale = "area";
    private boolean trim = true;
    private double adjust = 1.0;
    private double[] drawQuantiles = null;
    private Color quantileColor = Color.web("#1c5480");
    private double quantileLineWidth = 1.5;
    private Position position = Position.DODGE;

    /**
     * Creates a violin geometry with default density and styling parameters.
     */
    public GeomViolin() {
    }

    /**
     * Sets the position adjustment mode. {@link Position#DODGE} (default)
     * splits each group into sub-groups by the mapped fill/color column and
     * places them side by side (the default for {@code Geoms.violin}).
     * {@link Position#IDENTITY} draws all sub-violins at the group's position,
     * overlapping at full width.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> position(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Sets the violin width factor relative to the available tick spacing.
     * the {@code width = 0.9} maps to a factor of {@code 0.9}.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> widthFactor(double widthFactor) {
        this.widthFactor = widthFactor;
        return this;
    }

    /**
     * Sets the constant fill color of the violins.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the stroke color of the violin outlines.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> color(Color c) {
        this.defaultStroke = c;
        return this;
    }

    /**
     * Controls how the width of the violins is scaled:
     * <ul>
     *   <li>{@code "area"} (default) — all violins have the same area</li>
     *   <li>{@code "count"} — area is proportional to the number of observations</li>
     *   <li>{@code "width"} — all violins have the same maximum width</li>
     * </ul>
     *
     * @param scale the scaling rule, one of {@code "area"}, {@code "count"}, {@code "width"}
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> scale(String scale) {
        this.scale = scale;
        return this;
    }

    /**
     * If {@code true} (default), trims the tails of the violins to the range of
     * the data. If {@code false}, the kernel-density tails extend up to three
     * bandwidths past the observed range.
     *
     * @param trim {@code true} to trim the tails to the data range
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> trim(boolean trim) {
        this.trim = trim;
        return this;
    }

    /**
     * Sets the multiplicative bandwidth adjustment (default {@code 1.0}). A
     * value below one fits the density more closely to the data, a value above
     * one smooths it more heavily.
     *
     * @param adjust the bandwidth multiplier
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> adjust(double adjust) {
        this.adjust = adjust;
        return this;
    }

    /**
     * Draws horizontal marks across each violin at the given quantiles, e.g.
     * {@code drawQuantiles(0.25, 0.5, 0.75)} for the quartiles. Passing
     * {@code null} or no arguments disables the marks.
     *
     * @param quantiles the quantile probabilities to mark
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> drawQuantiles(double... quantiles) {
        this.drawQuantiles = (quantiles == null || quantiles.length == 0) ? null : quantiles.clone();
        return this;
    }

    /**
     * Sets the color of the quantile marks.
     *
     * @param c the JavaFX {@link Color} of the quantile lines
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> quantileColor(Color c) {
        this.quantileColor = c;
        return this;
    }

    /**
     * Sets the stroke width of the quantile marks in pixels.
     *
     * @param width the quantile line stroke width in pixels
     * @return this {@code GeomViolin} instance for fluid chaining
     */
    public GeomViolin<DF> quantileLineWidth(double width) {
        this.quantileLineWidth = width;
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
        //    sub-groups are rendered side by side, like the default dodge position.
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

        // The discrete (group) scale and the continuous (value) scale depend on orientation
        var groupScale = coord.isFlipped() ? sy : sx;
        var valueScale = coord.isFlipped() ? sx : sy;

        // 2. Sort groups by data-space position on the group axis
        groupOrder.sort((a, b) -> Double.compare(groupScale.toData(a), groupScale.toData(b)));

        if (coord instanceof CoordPolar polar) {
            renderPolar(gc, polar, scales, fillCol, groups, groupOrder);
            gc.restore();
            return;
        }

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

        final var baseWidth = Math.min(minPixelSpacing * widthFactor, 40.0);

        // 4. The largest sub-group anchors the area/count scaling
        var maxN = 0;
        for (var rx : groupOrder) {
            for (var values : groups.get(rx).values()) {
                if (values.size() >= 2) {
                    maxN = Math.max(maxN, values.size());
                }
            }
        }
        if (maxN < 2) {
            gc.restore();
            return;
        }

        // 5. Render each sub-group, dodged within its group's slot
        for (var rx : groupOrder) {
            var subGroups = groups.get(rx);
            var subOrder = new ArrayList<Object>(subGroups.keySet());
            subOrder.sort(Comparator.nullsFirst(Comparator.comparing(Object::toString)));
            if (subOrder.isEmpty()) {
                continue;
            }
            var groupPix = groupScale.toPixel(groupScale.toData(rx));
            var dodge = position == Position.DODGE;
            var subWidth = dodge ? baseWidth / subOrder.size() : baseWidth;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.size() < 2) {
                    continue;
                }
                var density = estimate(values);
                var scaleFactor = scaleFactor(values.size(), maxN);
                var subHalfWidth = subWidth / 2.0;
                var center = dodge
                        ? groupPix + (i - (subOrder.size() - 1) / 2.0) * subWidth
                        : groupPix;

                Color fill = resolveColor(scales, fillCol, fv);
                Color stroke = defaultStroke != null ? defaultStroke : fill.darker();

                if (coord.isFlipped()) {
                    renderHorizontal(gc, valueScale, center, density, subHalfWidth, scaleFactor, fill, stroke);
                } else {
                    renderVertical(gc, valueScale, center, density, subHalfWidth, scaleFactor, fill, stroke);
                }
            }
        }

        gc.restore();
    }

    /**
     * Renders the grouped violins through a polar coordinate system, the
     * {@code Geoms.violin() + coordRadial(start, end)} counterpart of the polar
     * boxplot: the group axis becomes the angle (each category sweeps a sector
     * of the fan) and the value axis the radius. Each violin's density profile
     * turns the mirrored shape inside out — the horizontal half-width of the
     * Cartesian polygon becomes an angular half-width at every radius.
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

        // The largest sub-group anchors the area/count scaling
        var maxN = 0;
        for (var rx : groupOrder) {
            for (var values : groups.get(rx).values()) {
                if (values.size() >= 2) {
                    maxN = Math.max(maxN, values.size());
                }
            }
        }
        if (maxN < 2) {
            return;
        }

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
            var subHalfBand = subBand / 2.0;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.size() < 2) {
                    continue;
                }
                var density = estimate(values);
                var scaleFactor = scaleFactor(values.size(), maxN);
                var center = dodge
                        ? centerData + (i - (subOrder.size() - 1) / 2.0) * subBand
                        : centerData;

                Color fill = resolveColor(scales, fillCol, fv);
                Color stroke = defaultStroke != null ? defaultStroke : fill.darker();

                renderPolarViolin(gc, polar, center, subHalfBand, scaleFactor, density, fill, stroke);
            }
        }
    }

    /**
     * Renders a single violin of a polar plot: the mirrored density profile as
     * a filled and outlined path whose vertices run from the inner radius out
     * along the one flank of the sector and back along the other.
     */
    private void renderPolarViolin(DrawSurface gc, CoordPolar polar, double center, double subHalfBand,
                                   double scaleFactor, DensityEstimate density, Color fill, Color stroke) {
        int grid = density.values().length;
        var values = density.values();
        var scaled = density.scaled();
        var cx = polar.centerX();
        var cy = polar.centerY();

        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);

        // Left flank from the inner radius outwards, right flank back inwards,
        // so the silhouette bulges angularly where the density is highest.
        gc.beginPath();
        for (var i = 0; i < grid; i++) {
            var half = subHalfBand * scaled[i] * scaleFactor;
            var a = polar.thetaAngle(center - half);
            var r = polar.rPixel(values[i]);
            var x = cx + r * Math.cos(a);
            var y = cy + r * Math.sin(a);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        for (var i = grid - 1; i >= 0; i--) {
            var half = subHalfBand * scaled[i] * scaleFactor;
            var a = polar.thetaAngle(center + half);
            var r = polar.rPixel(values[i]);
            gc.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
        }
        gc.closePath();
        gc.fill();
        gc.stroke();

        if (drawQuantiles != null) {
            drawPolarQuantileMarks(gc, polar, center, subHalfBand, scaleFactor, density);
        }
    }

    /**
     * Draws an arc mark across the polar violin at each requested quantile,
     * following the ring at the quantile's radius and spanning the violin's
     * angular width there.
     */
    private void drawPolarQuantileMarks(DrawSurface gc, CoordPolar polar, double center, double subHalfBand,
                                        double scaleFactor, DensityEstimate density) {
        gc.setStroke(quantileColor != null ? quantileColor : defaultStroke);
        gc.setLineWidth(quantileLineWidth);
        var cx = polar.centerX();
        var cy = polar.centerY();
        for (var q : drawQuantiles) {
            if (q < 0.0 || q > 1.0) {
                continue;
            }
            var quantileValue = quantile(density.sorted(), q);
            // With unit base width and factor, halfWidthAt returns the raw
            // scaled density interpolated at the quantile.
            var interp = halfWidthAt(quantileValue, density, 1.0, 1.0);
            var half = subHalfBand * interp * scaleFactor;
            var r = polar.rPixel(quantileValue);
            var aLow = polar.thetaAngle(center - half);
            var aHigh = polar.thetaAngle(center + half);
            gc.strokeArc(cx - r, cy - r, 2.0 * r, 2.0 * r, Math.toDegrees(aLow), Math.toDegrees(aHigh - aLow));
        }
        gc.setLineWidth(1.0);
    }

    /**
     * Renders a vertical (y value) violin group centered at {@code cx}.
     */
    private void renderVertical(DrawSurface gc, Scale valueScale, double cx, DensityEstimate density,
                                double baseHalfWidth, double scaleFactor, Color fill, Color stroke) {
        int grid = density.values().length;
        var xPts = new double[grid * 2];
        var yPts = new double[grid * 2];

        // Left flank rises bottom-to-top, right flank descends top-to-bottom
        for (var i = 0; i < grid; i++) {
            var pix = valueScale.toPixel(density.values()[i]);
            var half = baseHalfWidth * density.scaled()[i] * scaleFactor;
            xPts[i] = cx - half;
            yPts[i] = pix;
            xPts[grid * 2 - 1 - i] = cx + half;
            yPts[grid * 2 - 1 - i] = pix;
        }

        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);
        gc.fillPolygon(xPts, yPts, grid * 2);
        gc.strokePolygon(xPts, yPts, grid * 2);

        if (drawQuantiles != null) {
            drawQuantileMarks(gc, valueScale, cx, density, baseHalfWidth, scaleFactor, true);
        }
    }

    /**
     * Renders a horizontal (flipped, x value) violin group centered at {@code cy}.
     */
    private void renderHorizontal(DrawSurface gc, Scale valueScale, double cy, DensityEstimate density,
                                  double baseHalfWidth, double scaleFactor, Color fill, Color stroke) {
        int grid = density.values().length;
        var xPts = new double[grid * 2];
        var yPts = new double[grid * 2];

        for (var i = 0; i < grid; i++) {
            var pix = valueScale.toPixel(density.values()[i]);
            var half = baseHalfWidth * density.scaled()[i] * scaleFactor;
            xPts[i] = pix;
            yPts[i] = cy - half;
            xPts[grid * 2 - 1 - i] = pix;
            yPts[grid * 2 - 1 - i] = cy + half;
        }

        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);
        gc.fillPolygon(xPts, yPts, grid * 2);
        gc.strokePolygon(xPts, yPts, grid * 2);

        if (drawQuantiles != null) {
            drawQuantileMarks(gc, valueScale, cy, density, baseHalfWidth, scaleFactor, false);
        }
    }

    /**
     * Draws a short horizontal (or vertical, when flipped) mark across the
     * violin at each requested quantile, tapering with the violin's width at
     * that height.
     */
    private void drawQuantileMarks(DrawSurface gc, Scale valueScale, double groupPix, DensityEstimate density,
                                   double baseHalfWidth, double scaleFactor, boolean vertical) {
        gc.setStroke(quantileColor != null ? quantileColor : defaultStroke);
        gc.setLineWidth(quantileLineWidth);
        for (var q : drawQuantiles) {
            if (q < 0.0 || q > 1.0) {
                continue;
            }
            var quantileValue = quantile(density.sorted(), q);
            var half = halfWidthAt(quantileValue, density, baseHalfWidth, scaleFactor);
            var pix = valueScale.toPixel(quantileValue);
            if (vertical) {
                gc.strokeLine(groupPix - half, pix, groupPix + half, pix);
            } else {
                gc.strokeLine(pix, groupPix - half, pix, groupPix + half);
            }
        }
        gc.setLineWidth(1.0);
    }

    /**
     * Resolves the fill color for a sub-group, either from the mapped color/fill
     * column or from the configured default fill. The scale is shared with the
     * legend, so the violin fills and the legend always agree.
     */
    private Color resolveColor(ResolvedScales<DF> scales, String fillCol, Object fv) {
        if (fillCol != null && fv != null) {
            var c = scales.resolvedColorFor(fillCol, fv);
            return c != null ? c : defaultFill;
        }
        return defaultFill;
    }

    /**
     * Computes the width multiplier for a group: {@code 1} for {@code "width"},
     * {@code sqrt(n / maxN)} for {@code "area"}, {@code n / maxN} for {@code "count"}.
     */
    private double scaleFactor(int n, int maxN) {
        if (maxN <= 1) {
            return 1.0;
        }
        return switch (scale) {
            case "count" -> (double) n / maxN;
            case "width" -> 1.0;
            default -> Math.sqrt((double) n / maxN);
        };
    }

    /**
     * Estimates the half-width of a violin at a given data value, interpolating
     * the mirrored density profile linearly between grid samples.
     */
    private double halfWidthAt(double dataValue, DensityEstimate density, double baseHalfWidth, double scaleFactor) {
        var values = density.values();
        var scaled = density.scaled();
        var idx = 0;
        for (var i = 0; i < values.length - 1; i++) {
            if (dataValue >= values[i] && dataValue <= values[i + 1]) {
                idx = i;
                break;
            }
            if (i == values.length - 2) {
                return 0.0;
            }
        }
        var denom = values[idx + 1] - values[idx];
        var t = denom == 0 ? 0.0 : (dataValue - values[idx]) / denom;
        var interp = scaled[idx] + t * (scaled[idx + 1] - scaled[idx]);
        return baseHalfWidth * interp * scaleFactor;
    }

    /**
     * Estimates the Gaussian kernel density of a group, sampled on a fixed grid.
     * The bandwidth follows Silverman's rule of thumb ({@code Silverman's rule of thumb})
     * multiplied by {@code adjust}.
     */
    private DensityEstimate estimate(List<Double> data) {
        var n = data.size();
        Collections.sort(data);
        var min = data.get(0);
        var max = data.get(n - 1);
        var bw = DensityModels.bandwidthNrd0(data) * adjust;

        var lo = trim ? min : min - 3 * bw;
        var hi = trim ? max : max + 3 * bw;
        if (hi <= lo) {
            hi = lo + 1.0;
        }

        var grid = GRID_POINTS;
        var values = new double[grid];
        var density = new double[grid];
        var step = (hi - lo) / (grid - 1);
        var invBw = 1.0 / bw;
        var norm = 1.0 / Math.sqrt(2.0 * Math.PI);
        var kernelNorm = norm * invBw / n;

        // Linear binning, the same scheme a density estimator uses: every observation
        // spreads its weight onto the two neighbouring grid samples, and the
        // estimate is the convolution of those bin weights with the kernel.
        // This turns the KDE from O(points × grid) into O(points + grid²),
        // independent of the input size.
        var weights = new double[grid];
        for (var d : data) {
            var pos = (d - lo) / step;
            var j = (int) pos;
            if (j < 0 || j > grid - 1) {
                continue;
            }
            if (j == grid - 1) {
                weights[grid - 1] += 1.0;
                continue;
            }
            var frac = pos - j;
            weights[j] += 1.0 - frac;
            weights[j + 1] += frac;
        }
        for (var i = 0; i < grid; i++) {
            var v = lo + i * step;
            values[i] = v;
            var sum = 0.0;
            for (var j = 0; j < grid; j++) {
                var w = weights[j];
                if (w == 0.0) {
                    continue;
                }
                var u = (i - j) * step * invBw;
                sum += w * Math.exp(-0.5 * u * u);
            }
            density[i] = sum * kernelNorm;
        }

        var maxDensity = 0.0;
        for (var d : density) {
            maxDensity = Math.max(maxDensity, d);
        }
        var scaled = new double[grid];
        for (var i = 0; i < grid; i++) {
            scaled[i] = maxDensity > 0 ? density[i] / maxDensity : 0.0;
        }

        return new DensityEstimate(values, scaled, density, bw, n, Collections.unmodifiableList(data));
    }

    /**
     * Linear-interpolation quantile (equivalent to the type-7 default).
     */
    private double quantile(List<Double> sorted, double p) {
        return DensityModels.quantile(sorted, p);
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
        var valueScale = coord.isFlipped() ? sx : sy;
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
        final var baseWidth = Math.min(minPixelSpacing * widthFactor, 40.0);

        var maxN = 0;
        for (var rx : groupOrder) {
            for (var values : groups.get(rx).values()) {
                if (values.size() >= 2) {
                    maxN = Math.max(maxN, values.size());
                }
            }
        }
        if (maxN < 2) {
            return null;
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
            var subWidth = dodge ? baseWidth / subOrder.size() : baseWidth;

            for (var i = 0; i < subOrder.size(); i++) {
                var fv = subOrder.get(i);
                var values = subGroups.get(fv);
                if (values.size() < 2) {
                    continue;
                }
                var density = estimate(values);
                var scaleFactor = scaleFactor(values.size(), maxN);
                var subHalfWidth = subWidth / 2.0;
                var center = dodge
                        ? groupPix + (i - (subOrder.size() - 1) / 2.0) * subWidth
                        : groupPix;

                var grid = density.values().length;
                var gridPix = new double[grid];
                var halfPix = new double[grid];
                for (var g = 0; g < grid; g++) {
                    gridPix[g] = valueScale.toPixel(density.values()[g]);
                    halfPix[g] = subHalfWidth * density.scaled()[g] * scaleFactor;
                }

                if (coord.isFlipped()) {
                    var half = interpolateHalfAt(gridPix, halfPix, mx);
                    if (half > 0 && Math.abs(my - center) <= half) {
                        return formatTooltip(rx, fv, density);
                    }
                } else {
                    var half = interpolateHalfAt(gridPix, halfPix, my);
                    if (half > 0 && Math.abs(mx - center) <= half) {
                        return formatTooltip(rx, fv, density);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Interpolates the half-width profile at a pixel height, so the hit test
     * honours the violin's silhouette rather than a box around it.
     */
    private double interpolateHalfAt(double[] gridPix, double[] halfPix, double target) {
        for (var i = 0; i < gridPix.length - 1; i++) {
            var lo = Math.min(gridPix[i], gridPix[i + 1]);
            var hi = Math.max(gridPix[i], gridPix[i + 1]);
            if (target >= lo && target <= hi) {
                var denom = hi - lo;
                var t = denom == 0 ? 0.0 : (target - lo) / denom;
                var h1 = halfPix[i];
                var h2 = halfPix[i + 1];
                if (gridPix[i] > gridPix[i + 1]) {
                    t = 1.0 - t;
                }
                return h1 + t * (h2 - h1);
            }
        }
        return 0.0;
    }

    private String formatTooltip(Object rx, Object fv, DensityEstimate density) {
        var group = rx == null ? "" : String.valueOf(rx);
        if (fv != null) {
            group += "\n" + fv;
        }
        return String.format("%s\nn = %d\nmedian = %,.1f\nbandwidth = %,.2f",
                group, density.n(), quantile(density.sorted(), 0.50), density.bw());
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx, boolean xDiscrete, boolean yDiscrete) {
        var aes = ctx.aes();
        var coord = ctx.coord();
        var extractor = ctx.extractor();
        var df = ctx.globalDf();
        if (trim || aes.x() == null || aes.y() == null) {
            return bounds;
        }
        // Untrimmed tails reach ~3 bandwidths past the data; widen the value axis so
        // the pointed tails are visible rather than clipped against the panel edge.
        var rawX = extractor.getColumn(df, aes.x());
        var rawY = extractor.getColumn(df, aes.y());
        var maxBw = 0.0;
        var groups = new LinkedHashMap<Object, List<Double>>();
        for (var i = 0; i < extractor.getRowCount(df); i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            if (rx == null || ry == null) {
                continue;
            }
            groups.computeIfAbsent(rx, k -> new ArrayList<>()).add(Values.toDouble(ry));
        }
        for (var values : groups.values()) {
            if (values.size() < 2) {
                continue;
            }
            Collections.sort(values);
            maxBw = Math.max(maxBw, DensityModels.bandwidthNrd0(values) * adjust);
        }
        var extend = 3.0 * maxBw;
        if (extend <= 0) {
            return bounds;
        }
        if (coord.isFlipped()) {
            return new Bounds(bounds.xMin() - extend, bounds.xMax() + extend, bounds.yMin(), bounds.yMax());
        }
        return new Bounds(bounds.xMin(), bounds.xMax(), bounds.yMin() - extend, bounds.yMax() + extend);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        // One polygon of ~2*GRID_POINTS vertices per group
        return Math.max(GRID_POINTS, ctx.plot().extractor().getRowCount(ctx.partitionDf()));
    }

    /**
     * Immutable kernel-density profile for one violin group.
     *
     * @param values  the grid data values the density is sampled at
     * @param scaled  the density scaled to a maximum of 1
     * @param density the raw kernel-density estimate per grid sample
     * @param bw      the effective bandwidth used (after {@code adjust})
     * @param n       the number of observations in the group
     * @param sorted  the group's raw values in ascending order
     */
    @SuppressWarnings("ArrayRecordComponent") // transient per-group estimate, arrays are never mutated
    private record DensityEstimate(double[] values, double[] scaled, double[] density, double bw, int n, List<Double> sorted) {
    }
}
