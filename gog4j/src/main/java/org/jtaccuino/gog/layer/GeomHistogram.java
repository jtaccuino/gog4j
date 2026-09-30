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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatBin;
import org.jtaccuino.gog.stat.StatParams;
import org.jtaccuino.gog.stat.Stats;

/**
 * {@code Geoms.histogram()} geometry: {@code Stats.bin()} as bars of height equal
 * to the per-bin observation count.
 * <p>
 * Maps only {@code aes(x)} (a continuous variable); {@code Stats.bin} groups the
 * values into nice bins and each bin is drawn as a vertical bar from the zero
 * baseline to its count. A {@code fill}/{@code color} column splits the data
 * into groups whose bars are stacked by default ({@link Position#STACK}) or
 * dodged ({@link Position#DODGE}); the count axis is driven by the bin totals
 * via {@link #expandDomain}.
 * <p>
 * The stacked/stackable behaviour is inherited from {@link StackableGeom}, so a
 * histogram participates in baseline stacking and reports its count domain like
 * any other stacked geometry.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomHistogram<DF> extends StackableGeom<DF> implements StatHost<DF> {

    private StatBin.BinParams params = StatBin.BinParams.defaults();
    private Color defaultFill = Color.web("#3182bd");
    private Color defaultColor = Color.web("#2b528f");
    private StatParams attachedStatParams;

    /**
     * Creates a histogram geometry with the default {@link Position#STACK}
     * layout.
     */
    public GeomHistogram() {
        super(Position.STACK);
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.bin();
    }

    /**
     * Sets the (approximate) number of bins over the data range.
     *
     * @param bins the bin count, {@code 30} by default
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> bins(int bins) {
        this.params = params.bins(bins);
        return this;
    }

    /**
     * Sets a fixed bin width in data units.
     *
     * @param binwidth the width of every bin
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> binwidth(double binwidth) {
        this.params = params.binwidth(binwidth);
        return this;
    }

    /**
     * Anchors the bin-edge grid so an edge falls on the given value.
     *
     * @param boundary the edge alignment point
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> boundary(double boundary) {
        this.params = params.boundary(boundary);
        return this;
    }

    /**
     * Sets the position adjustment mode (identity, dodge, or stack).
     *
     * @param position the {@link Position} layout mode
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> position(Position position) {
        setPosition(position);
        return this;
    }

    /**
     * Sets the fill color of the histogram bars for ungrouped rendering.
     *
     * @param c the JavaFX fill {@link Color}
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> fill(Color c) {
        this.defaultFill = c;
        return this;
    }

    /**
     * Sets the stroke color of the histogram bars for ungrouped rendering.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomHistogram} for fluid chaining
     */
    public GeomHistogram<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    @Override
    public void attach(Stat<DF> stat, PositionAdjust position, LayerParams params) {
        if (!(stat instanceof StatBin<?>)) {
            throw new IllegalArgumentException(
                    "A histogram geometry hosts a binning transform only, got " + stat.getClass().getSimpleName());
        }
        this.attachedStatParams = params == null ? StatParams.empty() : params.stat();
        if (params != null && params.fill() != null) {
            this.defaultFill = params.fill();
        }
        if (params != null && params.color() != null) {
            this.defaultColor = params.color();
        }
        setAdjust(position);
    }

    /**
     * The binning parameters in effect: those supplied through the layer's
     * {@code StatParams} when the geometry was bound via {@code Plot.layer(...)},
     * otherwise the geometry's own fluent settings.
     */
    private StatBin.BinParams effectiveParams() {
        if (attachedStatParams == null) {
            return params;
        }
        var out = StatBin.BinParams.defaults();
        if (attachedStatParams.contains("bins")) {
            out = out.bins(attachedStatParams.getInt("bins", 30));
        }
        if (attachedStatParams.contains("binwidth")) {
            out = out.binwidth(attachedStatParams.getDouble("binwidth", 0.0));
        }
        if (attachedStatParams.contains("boundary")) {
            out = out.boundary(attachedStatParams.getDouble("boundary", 0.0));
        }
        return out;
    }

    @Override
    public LayerData prepare(PlotContext<DF> ctx) {
        var ext = ctx.extractor();
        var aes = ctx.aes();
        var xCol = aes.x() == null ? null : ext.getColumn(ctx.globalDf(), aes.x());
        if (xCol == null) {
            return LayerData.NONE;
        }
        var xData = collectX(xCol);
        if (xData.isEmpty()) {
            return LayerData.NONE;
        }
        var result = Stats.bin().fitInto(xData, Stats.bin().edges(xData, effectiveParams()));
        return new LayerData.StatLayerData<>(result, ext);
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        var df = ctx.globalDf();
        var ext = ctx.extractor();
        var aes = ctx.aes();
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        if (xCol == null) {
            return bounds;
        }
        return expandBounds(bounds, xCol, ext, df, aes);
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var scales = ctx.plot().scales();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var df = ctx.partitionDf();
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        if (xCol == null) {
            return;
        }

        var xData = collectX(xCol);
        if (xData.isEmpty()) {
            return;
        }
var edges = Stats.bin().edges(xData, effectiveParams());

        var groupCol = AesRendering.rawGroupColumn(aes);
        // A computed (afterStat) column names a variable of the stat output,
        // not a grouping column of the raw frame — it must not send the render
        // down the grouped path.
        var groupData = groupCol != null ? ext.getColumn(df, groupCol) : null;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var baselinePixel = sy.toPixel(0.0);
        var nBins = edges.length - 1;

        if (groupCol == null || groupData == null) {
            var result = Stats.bin().fitInto(xData, edges);
            var fillValue = aes.fillValue();
            var fillRes = AesRendering.resolution(ctx, aes, result, null);
            drawBars(gc, result, sx, sy, defaultFill, defaultColor, baselinePixel,
                    scales, fillRes, fillValue);
        } else {
            // Partition rows into groups, bin each over the shared edge grid.
            var groups = new LinkedHashMap<Object, List<Double>>();
            for (var i = 0; i < ext.getRowCount(df); i++) {
                var rx = xCol.get(i);
                var rg = groupData.get(i);
                if (rx == null || rg == null) {
                    continue;
                }
                groups.computeIfAbsent(rg, k -> new ArrayList<>()).add(Values.toDouble(rx));
            }
            var order = new ArrayList<>(groups.keySet());
            order.sort((a, b) -> a.toString().compareTo(b.toString()));

            for (var i = 0; i < nBins; i++) {
                int cumulative = 0;
                for (var group : order) {
                    int count = countInBin(groups.get(group), edges, i);
                    if (count == 0) {
                        continue;
                    }
                    double barLeft = sx.toPixel(edges[i]);
                    double barRight = sx.toPixel(edges[i + 1]);
                    double bottom;
                    double top;
                    if (position == Position.STACK || position == Position.IDENTITY) {
                        bottom = sy.toPixel(cumulative);
                        top = sy.toPixel(cumulative + count);
                        if (position == Position.IDENTITY) {
                            cumulative = 0;
                        } else {
                            cumulative += count;
                        }
                    } else {
                        bottom = baselinePixel;
                        top = sy.toPixel(count);
                    }
                    var resolvedColor = scales.resolvedColorFor(groupCol, group);
                    var color = resolvedColor != null ? resolvedColor : defaultFill;
                    gc.setFill(color);
                    gc.setStroke(color.darker());
                    gc.fillRect(barLeft, Math.min(bottom, top), Math.abs(barRight - barLeft), Math.abs(bottom - top));
                    gc.strokeRect(barLeft, Math.min(bottom, top), Math.abs(barRight - barLeft), Math.abs(bottom - top));
                }
            }
        }

        gc.restore();
    }

    private Bounds expandBounds(Bounds bounds, List<?> xCol, DataExtractor<DF> ext,
                                DF df, org.jtaccuino.gog.Aes aes) {
        var xData = collectX(xCol);
        if (xData.isEmpty()) {
            return bounds;
        }
var edges = Stats.bin().edges(xData, effectiveParams());
        var nBins = edges.length - 1;

        var groupCol = AesRendering.rawGroupColumn(aes);
        // Mirror the render: a computed (afterStat) group column carries no
        // per-row grouping values, so it must not trigger the grouped path.
        var groupData = groupCol != null ? ext.getColumn(df, groupCol) : null;

        var binary = new double[nBins];
        if (groupCol == null || groupData == null) {
            var result = Stats.bin().fitInto(xData, edges);
            for (var i = 0; i < nBins; i++) {
                binary[i] = ((Number) result.column("count").get(i)).doubleValue();
            }
        } else {
            var groups = new LinkedHashMap<Object, List<Double>>();
            for (var i = 0; i < ext.getRowCount(df); i++) {
                var rx = xCol.get(i);
                var rg = groupData.get(i);
                if (rx == null || rg == null) {
                    continue;
                }
                groups.computeIfAbsent(rg, k -> new ArrayList<>()).add(Values.toDouble(rx));
            }
            var order = new ArrayList<>(groups.keySet());
            order.sort((a, b) -> a.toString().compareTo(b.toString()));
            for (var i = 0; i < nBins; i++) {
                double total = 0.0;
                for (var group : order) {
                    if (position == Position.STACK || position == Position.IDENTITY) {
                        total += countInBin(groups.get(group), edges, i);
                    } else {
                        total = Math.max(total, countInBin(groups.get(group), edges, i));
                    }
                }
                binary[i] = total;
            }
        }

        double maxCount = 0;
        for (double c : binary) {
            if (c > maxCount) maxCount = c;
        }
        double dataMinX = bounds.xMin();
        double dataMaxX = bounds.xMax();
        if (dataMinX > edges[0]) dataMinX = edges[0];
        if (dataMaxX < edges[nBins]) dataMaxX = edges[nBins];
        double dataMinY = Math.min(bounds.yMin(), 0.0);
        double dataMaxY = Math.max(bounds.yMax(), maxCount);
        return new Bounds(dataMinX, dataMaxX, dataMinY, dataMaxY);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var df = ctx.partitionDf();
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        if (xCol == null) {
            return null;
        }
        var xData = collectX(xCol);
        if (xData.isEmpty()) {
            return null;
        }
        var edges = Stats.bin().edges(xData, effectiveParams());
        var result = Stats.bin().fitInto(xData, edges);
        if (result == null || result.isEmpty()) {
            return null;
        }
        var xmin = result.columnAsDoubles("xmin");
        var xmax = result.columnAsDoubles("xmax");
        var counts = result.columnAsDoubles("count");
        if (xmin == null || xmax == null || counts == null) {
            return null;
        }
        var baselinePixel = sy.toPixel(0.0);
        for (var i = 0; i < counts.size(); i++) {
            double count = counts.get(i);
            if (count == 0.0) {
                continue;
            }
            double barLeft = sx.toPixel(xmin.get(i));
            double barRight = sx.toPixel(xmax.get(i));
            double top = sy.toPixel(count);
            if (mx >= barLeft && mx <= barRight && my >= top && my <= baselinePixel) {
                var colourHint = AesRendering.statColourHint(result, aes, i);
                var labelX = AesRendering.xAxisName(ctx) + ": "
                        + AesRendering.xValueLabel(sx, false, (xmin.get(i) + xmax.get(i)) / 2.0);
                return AesRendering.statValueTooltip(colourHint, labelX, count);
            }
        }
        return null;
    }

    private void drawBars(DrawSurface gc, org.jtaccuino.gog.stat.StatData result,
                          Scale sx, Scale sy, Color fill, Color stroke, double baselinePixel,
                          ResolvedScales<?> scales, AesValue.AesResolution<?> res, AesValue fillValue) {
        var xmin = result.columnAsDoubles("xmin");
        var xmax = result.columnAsDoubles("xmax");
        var counts = result.columnAsDoubles("count");
        for (var i = 0; i < counts.size(); i++) {
            double count = counts.get(i);
            if (count == 0.0) {
                continue;
            }
            var barFill = fill;
            var barStroke = stroke;
            var resolved = fillValue == null ? null
                    : AesRendering.fillColor(result, scales, res, fillValue, i);
            if (resolved != null) {
                barFill = resolved;
                barStroke = resolved.darker();
            }
            gc.setFill(barFill);
            gc.setStroke(barStroke);
            double barLeft = sx.toPixel(xmin.get(i));
            double barRight = sx.toPixel(xmax.get(i));
            double top = sy.toPixel(count);
            gc.fillRect(barLeft, top, Math.abs(barRight - barLeft), Math.abs(baselinePixel - top));
            gc.strokeRect(barLeft, top, Math.abs(barRight - barLeft), Math.abs(baselinePixel - top));
        }
    }

    private static int countInBin(List<Double> values, double[] edges, int bin) {
        double width = edges[1] - edges[0];
        int count = 0;
        for (double v : values) {
            if (Double.isNaN(v)) {
                continue;
            }
            int idx = (int) Math.floor((v - edges[0]) / width);
            if (idx == bin) {
                count++;
            }
        }
        return count;
    }

    private static List<Double> collectX(List<?> xCol) {
        return xCol.stream()
                .filter(Objects::nonNull)
                .map(Values::toDouble)
                .collect(Collectors.toList());
    }
}
