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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.jfr.PointRenderEvent;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.SizeScale;
import org.jtaccuino.gog.theme.Colors;

/**
 * Scatter plot geometry layer implementation for rendering points.
 * <p>
 * Supports mapping data attributes to X/Y canvas positions, point color,
 * point size, and categorical symbols/shapes ({@link PointShape}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomPoint<DF> implements Layer<DF>, ConstantColorLayer {

    /** The point styling configuration, shared with point subclasses. */
    protected PointSpec spec = new PointSpec();

    /** The layer's position adjustment, e.g. {@code positionOnFace()} for cube-face placement. */
    protected PositionAdjust position = Position.IDENTITY;

    /**
     * Points with a radius at or below this value are drawn fill-only when the
     * {@link RenderMode.Flag#SUPPRESS_STROKES} optimization is active: their
     * 1px outline is invisible noise on a 2-5px dot, and skipping the
     * {@code strokeXxx} primitive halves the recorded display-list calls per
     * point. Larger points keep their outline.
     */
    static final double SUPPRESS_STROKE_RADIUS = 2.5;

    /**
     * Below this point count the interleaved single-pass draw is kept: the
     * buffering and counting-sort overhead of paint-stable ordering outweighs
     * the paint-churn saving on small scatters. Package-visible so an A/B test
     * can force either path (0 = always bucket, a huge value = never bucket).
     */
    static volatile int PAINT_STABLE_THRESHOLD = 1024;

    /**
     * Creates a scatter geometry with default point styling.
     */
    public GeomPoint() {
    }

    /**
     * Sets the point specification for this layer.
     *
     * @param spec the {@link PointSpec} configuring point properties
     */
    public void setSpec(PointSpec spec) {
        this.spec = spec;
    }

    /**
     * Sets a constant point fill/stroke color for this layer.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code GeomPoint} instance for fluid chaining
     */
    public GeomPoint<DF> color(Color color) {
        this.spec.color(color);
        return this;
    }

    /**
     * Sets a constant point diameter size in pixels for this layer.
     *
     * @param size the point diameter in pixels
     * @return this {@code GeomPoint} instance for fluid chaining
     */
    public GeomPoint<DF> size(double size) {
        this.spec.size(size);
        return this;
    }

    /**
     * Returns the custom color configured on this point layer spec, if any.
     *
     * @return custom {@link Color}, or {@code null} if unconfigured
     */
    @Override
    public Color getCustomColor() {
        return this.spec.getColor();
    }

    /**
     * Returns the {@link PointShape} symbol configured on this point layer spec.
     *
     * @return point shape symbol
     */
    public PointShape getShape() {
        return this.spec.getShape();
    }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} for a 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code GeomPoint} instance for fluid chaining
     */
    public GeomPoint<DF> position(PositionAdjust position) {
        this.position = position;
        return this;
    }

    @Override
    public PositionAdjust positionAdjust() {
        return position;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var scales = ctx.plot().scales();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var theme = ctx.plot().theme();
        var df = ctx.partitionDf();
        var xCol = AesValue.rawColumn(aes.xValue());
        var rawX = xCol == null ? null : ext.getColumn(df, xCol);
        var yCol = AesValue.rawColumn(aes.yValue());
        var rawY = yCol == null ? null : ext.getColumn(df, yCol);
        var n = ext.getRowCount(df);
        if (n == 0) return;

        // afterScale position references (x = afterScale("y"), y = afterScale("x"))
        // map the referenced axis's value through its scale transform instead of
        // reading a raw column, so the corresponding raw column is absent.
        var yFromX = aes.yValue() instanceof AesValue.AfterScale scaleY
                && scaleY.aesthetic() == Aesthetic.X;
        var xFromY = aes.xValue() instanceof AesValue.AfterScale scaleX
                && scaleX.aesthetic() == Aesthetic.Y;

        var colorGroup = AesRendering.colourGroupColumn(aes);
        var shapeGroup = AesValue.rawColumn(aes.shapeValue());
        var sizeColumn = AesValue.rawColumn(aes.sizeValue());
        var alphaColumn = AesValue.rawColumn(aes.alphaValue());

        // Resolve the mapped columns once up front: extractors may materialise a
        // fresh list per call, which would make a per-point lookup quadratic.
        var rawColor = colorGroup != null ? ext.getColumn(df, colorGroup) : null;
        var rawShape = shapeGroup != null ? ext.getColumn(df, shapeGroup) : null;
        var rawSize = sizeColumn != null ? ext.getColumn(df, sizeColumn) : null;
        var rawAlpha = alphaColumn != null ? ext.getColumn(df, alphaColumn) : null;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();

        var radius = (spec.hasExplicitSize() ? spec.getSize() : theme.defaultPointSize()) / 2.0;
        var shapeScale = scales.shapeScale(shapeGroup);
        var sizeRange = rawSize != null ? scales.numericRange(sizeColumn) : null;
        var alphaRange = rawAlpha != null ? scales.numericRange(alphaColumn) : null;
        var colorResolver = rawColor != null ? scales.colorResolverFor(colorGroup) : null;

        // Resolve loop-invariant state once instead of once per point: the axis
        // flip, the manual/default fill, the base opacity and the default shape.
        // Polar coords and the positionOnFace face projection override
        // xPixel/yPixel with their own mapping, so the plain Cartesian inline
        // here only applies to flat coordinate systems.
        var flipped = coord.isFlipped();
        var xDataScale = flipped ? sy : sx;
        var yDataScale = flipped ? sx : sy;
        // Polar coords and the positionOnFace face projection override
        // xPixel/yPixel with their own mapping, so the plain Cartesian inline
        // in the emitters only applies to flat coordinate systems. The emitters
        // route each point through the coordinate system exactly when this flag
        // is set.
        var mapThroughCoord = coord instanceof CoordPolar || position instanceof PositionAdjust.PositionOnFace;
        var baseOpacity = spec.getOpacity();
        var defaultFill = spec.getColor() != null ? spec.getColor() : theme.fallbackColor();
        var defaultShape = spec.getShape();
        var hasShapeColumn = rawShape != null;
        var hasPerPointAlpha = rawAlpha != null && alphaRange != null;
        gc.setLineWidth(!hasShapeColumn && defaultShape == PointShape.CROSS ? 1.5 : 1.0);

        var pointEvt = new PointRenderEvent();
        pointEvt.pointCount = n;
        pointEvt.begin();

        var mode = ctx.renderMode();

        // A layer without a colour/fill mapping and without a per-point alpha
        // draws every point with one fixed style: resolve and apply it once up
        // front, then hand off to the dedicated constant-style emitter. Keeping
        // that loop in its own small compilation unit ({@link PointEmitter})
        // lets the JIT fully inline the per-point leaves even when the layer is
        // reached through deep composed/matrix render chains, so a dense
        // translucent constant-style scatter (opacityConstant) stays as fast as
        // classic per-point emission.
        var constantStyle = rawColor == null && !hasPerPointAlpha;
        if (constantStyle) {
            var fixedStyle = baseOpacity < 1.0
                    ? new PointStyle(Colors.withAlpha(defaultFill, baseOpacity), Colors.withAlpha(defaultFill, baseOpacity).darker())
                    : new PointStyle(defaultFill, defaultFill.darker());
            gc.setFill(fixedStyle.fill());
            gc.setStroke(fixedStyle.darker());
            prepareJitter(ctx, rawX, rawY);
            var sxLin = sx.linearPixelMapping();
            var syLin = sy.linearPixelMapping();
            var linearPixels = sxLin != null && syLin != null;
            var counts = PointEmitter.emit(gc, rawX, rawY, rawShape, rawSize, n,
                    yFromX, xFromY, xDataScale, yDataScale, sx, sy, coord, flipped, mapThroughCoord,
                    radius, defaultShape, shapeScale, sizeRange, ctx.plot().scaleSpec(),
                    mode.has(RenderMode.Flag.SUPPRESS_STROKES),
                    mode.has(RenderMode.Flag.BIN_OVERDRAW) && baseOpacity >= 1.0
                            && rawShape == null && rawSize == null,
                    linearPixels,
                    linearPixels ? sxLin[0] : 0, linearPixels ? sxLin[1] : 0,
                    linearPixels ? syLin[0] : 0, linearPixels ? syLin[1] : 0,
                    this);
            pointEvt.skippedCount = counts.skipped();
            pointEvt.binnedCount = counts.binned();
            pointEvt.end();
            pointEvt.commit();
            gc.restore();
            return;
        }
        prepareJitter(ctx, rawX, rawY);

        var paintStable = n >= PAINT_STABLE_THRESHOLD;
        ColorEmitter.PaintStableBuffer buf = null;
        List<PointStyle> styles = null;
        Map<PointStyle, Integer> styleBuckets = null;
        if (paintStable) {
            var xs = new double[n];
            var ys = new double[n];
            var styleIdx = new int[n];
            var stableRadii = rawSize != null ? new double[n] : null;
            var stableShapes = hasShapeColumn ? new PointShape[n] : null;
            buf = new ColorEmitter.PaintStableBuffer(xs, ys, styleIdx, stableRadii, stableShapes);
            styles = new ArrayList<>();
            styleBuckets = new HashMap<>();
        }

        // The colour-mapped per-point loop (position, fill/alpha, shape, size,
        // paint-stable bucketing or immediate draw) lives in its own small
        // compilation unit ({@link ColorEmitter}) so the JIT can fully inline
        // the per-point leaves even through deep composed/matrix render chains.
        Map<Color, PointStyle> styleCache = new HashMap<>();
        var sxLin = sx.linearPixelMapping();
        var syLin = sy.linearPixelMapping();
        var linearPixels = sxLin != null && syLin != null;
        var counts = ColorEmitter.emit(gc, rawX, rawY, rawColor, rawAlpha, rawShape, rawSize, n,
                yFromX, xFromY, hasShapeColumn, hasPerPointAlpha, mapThroughCoord, flipped,
                xDataScale, yDataScale, sx, sy, coord, defaultFill, defaultShape, shapeScale,
                sizeRange, alphaRange, baseOpacity, radius, colorResolver, ctx.plot().scaleSpec(),
                paintStable, mode.has(RenderMode.Flag.SUPPRESS_STROKES),
                buf, styles, styleBuckets, styleCache,
                linearPixels,
                linearPixels ? sxLin[0] : 0, linearPixels ? sxLin[1] : 0,
                linearPixels ? syLin[0] : 0, linearPixels ? syLin[1] : 0,
                this);
        pointEvt.skippedCount = counts.skipped();
        pointEvt.binnedCount = 0;
        pointEvt.end();
        pointEvt.commit();
        if (paintStable) {
            emitPaintStable(gc, buf.xs, buf.ys, buf.styleIdx, buf.stableRadii, buf.stableShapes, styles,
                    radius, defaultShape, hasShapeColumn, n - counts.skipped(),
                    mode.has(RenderMode.Flag.SUPPRESS_STROKES));
        }
        gc.restore();
    }

    /**
     * Emits buffered points in paint-stable order: a stable counting sort over
     * the per-point style bucket, then one {@code setFill}/{@code setStroke}
     * per bucket with all of its points drawn contiguously.
     *
     * @param gc               the target drawing surface
     * @param xs               buffered point x pixels
     * @param ys               buffered point y pixels
     * @param styleIdx         buffered style bucket per point
     * @param radii            buffered per-point radii, or {@code null} when the
     *                         layer draws a constant radius
     * @param shapes           buffered per-point shapes, or {@code null} when the
     *                         layer draws a constant shape
     * @param styles           bucket styles, indexed by bucket id
     * @param defaultRadius    the layer's constant radius when {@code radii} is null
     * @param defaultShape     the layer's constant shape when {@code shapes} is null
     * @param hasShapeColumn   whether the layer maps a per-point shape column
     * @param count            number of buffered points
     * @param suppressStrokes  whether to omit point outlines (small-dot fast path)
     */
    private static void emitPaintStable(DrawSurface gc, double[] xs, double[] ys, int[] styleIdx,
            double[] radii, PointShape[] shapes, List<PointStyle> styles,
            double defaultRadius, PointShape defaultShape, boolean hasShapeColumn, int count,
            boolean suppressStrokes) {
        if (count == 0) {
            return;
        }
        int bucketCount = styles.size();
        int[] counts = new int[bucketCount];
        for (var i = 0; i < count; i++) {
            counts[styleIdx[i]]++;
        }
        int[] offsets = new int[bucketCount];
        var total = 0;
        for (var b = 0; b < bucketCount; b++) {
            offsets[b] = total;
            total += counts[b];
        }
        // Stable counting sort: iterate the buffered points in order and place
        // each into its bucket slot, preserving within-bucket row order.
        int[] order = new int[count];
        int[] cursor = offsets.clone();
        for (var i = 0; i < count; i++) {
            order[cursor[styleIdx[i]]++] = i;
        }
        for (var b = 0; b < bucketCount; b++) {
            var style = styles.get(b);
            gc.setFill(style.fill());
            gc.setStroke(style.darker());
            var end = offsets[b] + counts[b];
            for (var k = offsets[b]; k < end; k++) {
                var idx = order[k];
                var shape = shapes != null ? shapes[idx] : defaultShape;
                if (hasShapeColumn) {
                    gc.setLineWidth(shape == PointShape.CROSS ? 1.5 : 1.0);
                }
                var r = radii != null ? radii[idx] : defaultRadius;
                shape.draw(gc, xs[idx], ys[idx], r,
                        suppressStrokes && r <= SUPPRESS_STROKE_RADIUS);
            }
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
        var labsSpec = ctx.plot().labs();
        var xCol = AesValue.rawColumn(aes.xValue());
        var rawX = xCol == null ? null : ext.getColumn(df, xCol);
        var yCol = AesValue.rawColumn(aes.yValue());
        var rawY = yCol == null ? null : ext.getColumn(df, yCol);
        var n = ext.getRowCount(df);
        if (n == 0) {
            return null;
        }

        // afterScale position references place points on a computed value that
        // the raw frame does not carry; the hit test mirrors the renderer's
        // forwarded-value math below.
        var yFromX = aes.yValue() instanceof AesValue.AfterScale scaleY
                && scaleY.aesthetic() == Aesthetic.X;
        var xFromY = aes.xValue() instanceof AesValue.AfterScale scaleX
                && scaleX.aesthetic() == Aesthetic.Y;
        var xDataScale = coord.isFlipped() ? sy : sx;
        var yDataScale = coord.isFlipped() ? sx : sy;

        var colorGroup = AesRendering.colourGroupColumn(aes);
        var rawSize = aes.sizeValue() != null && aes.sizeValue().isRaw()
                ? ext.getColumn(df, aes.size()) : null;
        var rawAlpha = aes.alphaValue() != null && aes.alphaValue().isRaw()
                ? ext.getColumn(df, aes.alpha()) : null;

        var hitTolerance = (spec.hasExplicitSize() ? spec.getSize() : ctx.plot().theme().defaultPointSize()) / 2 + 3.0;
        prepareJitter(ctx, rawX, rawY);

        // Scan topmost-first: points render in row order, so later rows cover
        // earlier ones; the hover must report the point actually visible under
        // the cursor (e.g. an overlapping cluster along a diagonal), not the
        // first row merely within tolerance of it.
        for (var i = n - 1; i >= 0; i--) {
            var rx = rawX == null ? null : rawX.get(i);
            var ry = rawY == null ? null : rawY.get(i);
            if ((rx == null && !xFromY) || (ry == null && !yFromX)) {
                continue;
            }

            var xDouble = jitterX(xFromY
                    ? yDataScale.transform().forward(yDataScale.toData(ry))
                    : xDataScale.toData(rx), i);
            var yDouble = jitterY(yFromX
                    ? xDataScale.transform().forward(xDataScale.toData(rx))
                    : yDataScale.toData(ry), i);

            var cx = coord.xPixel(sx, sy, xDouble, yDouble);
            var cy = coord.yPixel(sx, sy, xDouble, yDouble);

            var dx = mx - cx;
            var dy = my - cy;
            var distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= hitTolerance) {
                // The tooltip reports every aesthetic the row carries. Field
                // labels prefer the labs-provided name (a per-aesthetic guide
                // title) over the raw column; category values go through the
                // label dictionary.
                var tooltip = new StringBuilder();
                if (colorGroup != null) {
                    var groupColumn = ext.getColumn(df, colorGroup);
                    var groupVal = groupColumn != null && i < groupColumn.size()
                            ? groupColumn.get(i) : null;
                    if (groupVal != null) {
                        tooltip.append(groupDisplayName(aes, colorGroup, labsSpec)).append(": ")
                                .append(Values.label(labsSpec != null ? labsSpec.map(Values.label(groupVal)) : groupVal))
                                .append('\n');
                    }
                }
                if (aes.shape() != null) {
                    var shapeColumn = ext.getColumn(df, aes.shape());
                    var shapeVal = shapeColumn != null && i < shapeColumn.size()
                            ? shapeColumn.get(i) : null;
                    if (shapeVal != null) {
                        tooltip.append(groupDisplayName(aes, aes.shape(), labsSpec)).append(": ")
                                .append(Values.label(labsSpec != null ? labsSpec.map(Values.label(shapeVal)) : shapeVal))
                                .append('\n');
                    }
                }
                if (rawSize != null && i < rawSize.size() && rawSize.get(i) != null) {
                    var sizeVal = rawSize.get(i);
                    tooltip.append(groupDisplayName(aes, aes.size(), labsSpec)).append(": ")
                            .append(Values.label(sizeVal)).append('\n');
                }
                if (rawAlpha != null && i < rawAlpha.size() && rawAlpha.get(i) != null) {
                    var alphaVal = rawAlpha.get(i);
                    tooltip.append(groupDisplayName(aes, aes.alpha(), labsSpec)).append(": ")
                            .append(Values.label(alphaVal)).append('\n');
                }
                tooltip.append(axisDisplayName(true, labsSpec)).append(": ")
                        .append(Values.label(mappedValue(rx, labsSpec))).append('\n');
                tooltip.append(axisDisplayName(false, labsSpec)).append(": ")
                        .append(String.format("%,.2f", yDouble));
                return tooltip.toString();
            }
        }
        return null;
    }

    /**
     * The display name for a mapped aesthetic in tooltips: the per-aesthetic
     * guide title when set ({@code labs(colour = "Drive type")}), else the
     * raw column name.
     */
    private String groupDisplayName(Aes aes, String columnName, LabsSpec labsSpec) {
        if (labsSpec == null || columnName == null) {
            return columnName;
        }
        var aesthetic = columnName.equals(aes.color()) ? Aesthetic.COLOR
                : columnName.equals(aes.fill()) ? Aesthetic.FILL
                : columnName.equals(aes.size()) ? Aesthetic.SIZE
                : columnName.equals(aes.shape()) ? Aesthetic.SHAPE
                : columnName.equals(aes.alpha()) ? Aesthetic.ALPHA
                : null;
        if (aesthetic != null) {
            var titled = labsSpec.legendTitle(aesthetic);
            if (titled != null && !titled.isEmpty()) {
                return titled;
            }
        }
        return columnName;
    }

    /**
     * The tooltip label for an axis: the labs-provided axis label when set,
     * else the generic {@code X}/{@code Y}.
     */
    private String axisDisplayName(boolean xAxis, LabsSpec labsSpec) {
        if (labsSpec == null) {
            return xAxis ? "X" : "Y";
        }
        var label = xAxis ? labsSpec.xLabel() : labsSpec.yLabel();
        return label == null || label.isEmpty() ? (xAxis ? "X" : "Y") : label;
    }

    /** Categorical values go through the label dictionary; others as-is. */
    private Object mappedValue(Object value, LabsSpec labsSpec) {
        if (value instanceof String s && labsSpec != null) {
            return labsSpec.map(s);
        }
        return value;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }

    /**
     * Hook invoked by {@link #render} and {@link #locate} before the per-point
     * scan, giving subclasses (e.g. {@link GeomJitter}) a chance to derive
     * per-point position offsets from the raw x/y frames. The default is a
     * no-op.
     *
     * @param ctx   the panel context
     * @param rawX  the raw x column, or {@code null}
     * @param rawY  the raw y column, or {@code null}
     */
    protected void prepareJitter(PanelContext<DF> ctx, List<?> rawX, List<?> rawY) {
    }

    /**
     * Jitter applied to the x data-space value of the given row. The default
     * leaves the value unchanged.
     *
     * @param xData  the x value in data units
     * @param row    the zero-based data row
     * @return the jittered x value
     */
    protected double jitterX(double xData, int row) {
        return xData;
    }

    /**
     * Jitter applied to the y data-space value of the given row. The default
     * leaves the value unchanged.
     *
     * @param yData  the y value in data units
     * @param row    the zero-based data row
     * @return the jittered y value
     */
    protected double jitterY(double yData, int row) {
        return yData;
    }

    /**
     * Whether this layer derives per-point position offsets via
     * {@link #jitterX}/{@link #jitterY}. Subclasses such as {@link GeomJitter}
     * override this to {@code true}; the constant-style emitter then applies
     * the jittered offsets in its loop.
     *
     * @return {@code true} when the layer applies per-point jitter
     */
    protected boolean usesJitter() {
        return false;
    }

    /** The fill color and its darker stroke companion, resolved together. */
    record PointStyle(Color fill, Color darker) {}
}
