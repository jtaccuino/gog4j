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
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatBin;
import org.jtaccuino.gog.stat.StatParams;
import org.jtaccuino.gog.stat.Stats;

/**
 * {@code Geoms.freqpoly()} geometry: {@code Stats.bin()} drawn as a polyline
 * through the bin centres, with the per-bin count on the vertical axis — the
 * frequency-polygon counterpart of {@link GeomHistogram}.
 * <p>
 * Like the histogram it maps only {@code aes(x)} and bins a continuous
 * variable; instead of bars it connects the count of each successive bin with a
 * line, which makes it a natural choice for overlaying several distributions. A
 * {@code color}/{@code fill} column draws one line per category, each binned
 * over the same shared edge grid.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomFreqpoly<DF> implements Layer<DF>, StatHost<DF> {

    private StatBin.BinParams params = StatBin.BinParams.defaults();
    private Color defaultColor = Color.web("#3182bd");
    private double width = 1.5;
    private boolean statAttached;
    private StatParams attachedStatParams;
    private Color attachedColor;

    /**
     * Creates a frequency-polygon geometry.
     */
    public GeomFreqpoly() {
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.bin();
    }

    /**
     * Sets the (approximate) number of bins over the data range.
     *
     * @param bins the bin count, {@code 30} by default
     * @return this {@code GeomFreqpoly} for fluid chaining
     */
    public GeomFreqpoly<DF> bins(int bins) {
        this.params = params.bins(bins);
        return this;
    }

    /**
     * Sets a fixed bin width in data units.
     *
     * @param binwidth the width of every bin
     * @return this {@code GeomFreqpoly} for fluid chaining
     */
    public GeomFreqpoly<DF> binwidth(double binwidth) {
        this.params = params.binwidth(binwidth);
        return this;
    }

    /**
     * Anchors the bin-edge grid so an edge falls on the given value.
     *
     * @param boundary the edge alignment point
     * @return this {@code GeomFreqpoly} for fluid chaining
     */
    public GeomFreqpoly<DF> boundary(double boundary) {
        this.params = params.boundary(boundary);
        return this;
    }

    /**
     * Sets the line color for ungrouped rendering.
     *
     * @param c the JavaFX stroke {@link Color}
     * @return this {@code GeomFreqpoly} for fluid chaining
     */
    public GeomFreqpoly<DF> color(Color c) {
        this.defaultColor = c;
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param w the line width in pixels
     * @return this {@code GeomFreqpoly} for fluid chaining
     */
    public GeomFreqpoly<DF> width(double w) {
        this.width = w;
        return this;
    }

    @Override
    public void attach(Stat<DF> stat, PositionAdjust position, LayerParams params) {
        if (!(stat instanceof StatBin<?>)) {
            throw new IllegalArgumentException(
                    "A frequency-polygon geometry hosts a binning transform only, got " + stat.getClass().getSimpleName());
        }
        this.statAttached = true;
        this.attachedStatParams = params == null ? StatParams.empty() : params.stat();
        this.attachedColor = params == null ? null : params.color();
    }

    /**
     * The binning parameters in effect: those supplied through the layer's
     * {@code StatParams} when the geometry was bound via {@code Plot.layer(...)},
     * otherwise the geometry's own fluent settings.
     */
    private StatBin.BinParams effectiveParams() {
        if (!statAttached) {
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
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        var ext = ctx.extractor();
        var aes = ctx.aes();
        var xCol = aes.x() == null ? null : ext.getColumn(ctx.globalDf(), aes.x());
        if (xCol == null) {
            return bounds;
        }
        var xData = collectX(xCol);
        if (xData.isEmpty()) {
            return bounds;
        }
var edges = Stats.bin().edges(xData, effectiveParams());
        var result = Stats.bin().fitInto(xData, edges);
        double maxCount = 0;
        for (var c : result.columnAsDoubles("count")) {
            if (c > maxCount) maxCount = c;
        }
        double dataMinX = bounds.xMin();
        double dataMaxX = bounds.xMax();
        if (dataMinX > edges[0]) dataMinX = edges[0];
        if (dataMaxX < edges[edges.length - 1]) dataMaxX = edges[edges.length - 1];
        double dataMinY = Math.min(bounds.yMin(), 0.0);
        double dataMaxY = Math.max(bounds.yMax(), maxCount);
        return new Bounds(dataMinX, dataMaxX, dataMinY, dataMaxY);
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
        // A computed (afterStat) column names a stat-output variable, not a
        // per-row grouping column — it must not send the render down the
        // grouped path.
        var groupData = groupCol != null ? ext.getColumn(df, groupCol) : null;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();
        gc.setLineWidth(width);

        if (groupCol == null || groupData == null) {
            var result = Stats.bin().fitInto(xData, edges);
            gc.setStroke(statAttached && attachedColor != null ? attachedColor : defaultColor);
            drawLine(gc, result, sx, sy, coord);
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
            for (var group : order) {
                var result = Stats.bin().fitInto(groups.get(group), edges);
                var resolvedColor = scales.resolvedColorFor(groupCol, group);
                gc.setStroke(resolvedColor != null ? resolvedColor : defaultColor);
                drawLine(gc, result, sx, sy, coord);
            }
        }

        gc.restore();
    }

    private void drawLine(DrawSurface gc, org.jtaccuino.gog.stat.StatData result,
                          Scale sx, Scale sy, org.jtaccuino.gog.coord.Coord coord) {
        var xc = result.columnAsDoubles("x");
        var counts = result.columnAsDoubles("count");
        gc.beginPath();
        var first = true;
        for (var i = 0; i < counts.size(); i++) {
            if (counts.get(i) == 0.0) {
                continue;
            }
            var px = coord.xPixel(sx, sy, xc.get(i), counts.get(i));
            var py = coord.yPixel(sx, sy, xc.get(i), counts.get(i));
            if (first) {
                gc.moveTo(px, py);
                first = false;
            } else {
                gc.lineTo(px, py);
            }
        }
        gc.stroke();
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }

    private static List<Double> collectX(List<?> xCol) {
        return xCol.stream()
                .filter(Objects::nonNull)
                .map(Values::toDouble)
                .collect(Collectors.toList());
    }
}
