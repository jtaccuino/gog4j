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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ColorResolver;
import org.jtaccuino.gog.scale.DiscreteShapeScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.scale.SizeScale;

/**
 * Dedicated compilation root for the colour-mapped point emission loop.
 * <p>
 * A layer that maps a colour/fill column (or a per-point alpha) resolves a
 * distinct style per point, so it takes the generic per-point loop. Like
 * {@link PointEmitter}, this object is a small separate final class whose hot
 * loop forms its own C2 compilation unit: when the loop lives inside the large
 * {@code GeomPoint.render} method, the JIT's per-compilation inlining budget
 * can run out before the per-point leaves ({@link Scale#toPixel(double)},
 * {@link ColorResolver#forValue(Object)}, {@link PointShape#draw}...) are
 * inlined, leaving them as runtime virtual calls on deep composed/matrix
 * render chains. Keeping the loop decoupled from the upstream call depth lets
 * the JIT always fully inline the leaves into a tight loop.
 * <p>
 * Paint-stable bucketing and the style cache are owned by the caller and
 * passed in through the context: {@code emit} fills the buffers when
 * {@code paintStable} is set and mutates the colour {@code styleCache} exactly
 * as the former inline loop did.
 */
final class ColorEmitter {

    /** Counts reported by the colour-mapped emitter for the phase event. */
    record Counts(int skipped) {}

    /**
     * Mutable paint-stable buffers owned by the caller (allocated once per
     * layer render, never per point) and filled by {@link #emit}. A plain
     * class rather than a record so the arrays can live as components without
     * tripping ErrorProne's {@code ArrayRecordComponent}.
     */
    static final class PaintStableBuffer {
        final double[] xs;
        final double[] ys;
        final int[] styleIdx;
        final double[] stableRadii;
        final PointShape[] stableShapes;

        PaintStableBuffer(double[] xs, double[] ys, int[] styleIdx,
                double[] stableRadii, PointShape[] stableShapes) {
            this.xs = xs;
            this.ys = ys;
            this.styleIdx = styleIdx;
            this.stableRadii = stableRadii;
            this.stableShapes = stableShapes;
        }
    }

    private ColorEmitter() {
    }

    /**
     * Emits a colour-mapped point layer through a dedicated tight loop: per
     * point it resolves position (with optional jitter), the mapped fill and
     * per-point alpha (with the caller's colour cache), the mapped shape and
     * size, then either buffers the point for paint-stable emission or draws
     * it immediately.
     * <p>
     * The inputs are passed as scalar parameters rather than a context record
     * on purpose: the loop is large enough that C2 often compiles it as its own
     * compilation unit instead of inlining it into {@code render}, and a record
     * handed across that boundary escapes scalar replacement — one heap
     * allocation per layer. On dense multi-panel renders (a scatter matrix,
     * where the same emitter runs once per panel) that per-layer allocation
     * shows up as a measurable slowdown, so the hot entry point is kept
     * allocation-free. The paint-stable buffers and the colour cache are owned
     * by the caller and passed in as references: {@code emit} fills the buffers
     * when {@code paintStable} is set and mutates the colour {@code styleCache}
     * exactly as the former inline loop did.
     *
     * @param gc               the target drawing surface
     * @param rawX             the raw x column, or {@code null}
     * @param rawY             the raw y column, or {@code null}
     * @param rawColor         the per-point colour column, or {@code null}
     * @param rawAlpha         the per-point alpha column, or {@code null}
     * @param rawShape         the per-point shape column, or {@code null}
     * @param rawSize          the per-point size column, or {@code null}
     * @param n                number of rows
     * @param yFromX           whether the y aesthetic reads the x value through the x scale
     * @param xFromY           whether the x aesthetic reads the y value through the y scale
     * @param hasShapeColumn   whether a per-point shape column is mapped
     * @param hasPerPointAlpha whether a per-point alpha column is mapped
     * @param mapThroughCoord  whether to route points through the coordinate system
     *                         (polar, or a positionOnFace face projection)
     * @param flipped          whether the axes are flipped
     * @param xDataScale       the data x scale (flip-adjusted)
     * @param yDataScale       the data y scale (flip-adjusted)
     * @param sx               the pixel x scale
     * @param sy               the pixel y scale
     * @param coord            the coordinate system
     * @param defaultFill      the layer's default fill colour
     * @param defaultShape     the layer's default point shape
     * @param shapeScale       the shape scale, or {@code null}
     * @param sizeRange        the size range, or {@code null}
     * @param alphaRange       the alpha range, or {@code null}
     * @param baseOpacity      the layer's base opacity
     * @param radius           the default point radius in pixels
     * @param colorResolver    the colour resolver, or {@code null}
     * @param scaleSpec        the plot's scale spec (manual size overrides)
     * @param paintStable      whether to buffer points for paint-stable ordering
     * @param suppressStrokes  whether to omit point outlines (small-dot fast path)
     * @param buf              the paint-stable buffers, or {@code null} when not buffering
     * @param styles           the style bucket list (mutated when buffering)
     * @param styleBuckets     the style-to-bucket map (mutated when buffering)
     * @param styleCache       the derived-colour cache (always mutated)
     * @param linearPixels     whether {@code sx}/{@code sy} are identity, non-reversed scales
     * @param sxMul            pixel-x slope ({@code 0} when not linear)
     * @param sxAdd            pixel-x intercept ({@code 0} when not linear)
     * @param syMul            pixel-y slope ({@code 0} when not linear)
     * @param syAdd            pixel-y intercept ({@code 0} when not linear)
     * @param geom             the owning geometry (jitter)
     * @return the skipped point count
     */
    static Counts emit(DrawSurface gc,
            List<?> rawX, List<?> rawY,
            List<?> rawColor, List<?> rawAlpha, List<?> rawShape, List<?> rawSize,
            int n,
            boolean yFromX, boolean xFromY,
            boolean hasShapeColumn, boolean hasPerPointAlpha,
            boolean mapThroughCoord, boolean flipped,
            Scale xDataScale, Scale yDataScale,
            Scale sx, Scale sy,
            Coord coord,
            Color defaultFill,
            PointShape defaultShape,
            DiscreteShapeScale shapeScale,
            MinMax sizeRange, MinMax alphaRange,
            double baseOpacity, double radius,
            ColorResolver colorResolver,
            ScaleSpec scaleSpec,
            boolean paintStable, boolean suppressStrokes,
            PaintStableBuffer buf,
            List<GeomPoint.PointStyle> styles,
            Map<GeomPoint.PointStyle, Integer> styleBuckets,
            Map<Color, GeomPoint.PointStyle> styleCache,
            boolean linearPixels, double sxMul, double sxAdd, double syMul, double syAdd,
            GeomPoint<?> geom) {
        var xs = buf == null ? null : buf.xs;
        var ys = buf == null ? null : buf.ys;
        var styleIdx = buf == null ? null : buf.styleIdx;
        var stableRadii = buf == null ? null : buf.stableRadii;
        var stableShapes = buf == null ? null : buf.stableShapes;

        Color lastFill = null;
        Color lastStroke = null;
        int skipped = 0;
        int stableCount = 0;
        for (var i = 0; i < n; i++) {
            var rx = rawX == null ? null : rawX.get(i);
            var ry = rawY == null ? null : rawY.get(i);
            if ((rx == null && !xFromY) || (ry == null && !yFromX)) {
                skipped++;
                continue;
            }

            // Position afterScale references feed the referenced axis's
            // value through its scale transform (e.g. y = afterScale("x")
            // places the point at the transformed x value).
            var xDouble = geom.jitterX(xFromY
                    ? yDataScale.transform().forward(yDataScale.toData(ry))
                    : xDataScale.toData(rx), i);
            var yDouble = geom.jitterY(yFromX
                    ? xDataScale.transform().forward(xDataScale.toData(rx))
                    : yDataScale.toData(ry), i);

            // cx = coord.xPixel(...) = sx.toPixel(flipped ? yDouble : xDouble)
            // cy = coord.yPixel(...) = sy.toPixel(flipped ? xDouble : yDouble)
            double cx;
            double cy;
            if (mapThroughCoord) {
                cx = coord.xPixel(sx, sy, xDouble, yDouble);
                cy = coord.yPixel(sx, sy, xDouble, yDouble);
            } else if (linearPixels) {
                cx = sxAdd + sxMul * (flipped ? yDouble : xDouble);
                cy = syAdd + syMul * (flipped ? xDouble : yDouble);
            } else {
                cx = sx.toPixel(flipped ? yDouble : xDouble);
                cy = sy.toPixel(flipped ? xDouble : yDouble);
            }

            // 1. Color resolution (categorical mapping or manual/default):
            // every point resolves its fill from the colour column and
            // per-point alpha below.
            GeomPoint.PointStyle style;
            var fill = defaultFill;
            if (rawColor != null && i < rawColor.size()) {
                var rg = rawColor.get(i);
                if (rg != null) {
                    var resolved = colorResolver == null ? null : colorResolver.forValue(rg);
                    if (resolved != null) {
                        fill = resolved;
                    }
                }
            }
            double opacity = baseOpacity;
            if (hasPerPointAlpha && i < rawAlpha.size()) {
                var ra = rawAlpha.get(i);
                if (ra instanceof Number num) {
                    double norm = (num.doubleValue() - alphaRange.min())
                            / (alphaRange.max() - alphaRange.min());
                    norm = Math.max(0.0, Math.min(1.0, norm));
                    opacity *= norm;
                }
            }
            if (opacity < 1.0) {
                if (hasPerPointAlpha) {
                    fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getOpacity() * opacity);
                    style = styleCache.computeIfAbsent(fill, f -> new GeomPoint.PointStyle(f, f.darker()));
                } else {
                    var base = baseOpacity;
                    style = styleCache.computeIfAbsent(fill,
                            f -> new GeomPoint.PointStyle(alpha(f, base), alpha(f, base).darker()));
                    fill = style.fill();
                }
            } else {
                style = styleCache.computeIfAbsent(fill, f -> new GeomPoint.PointStyle(f, f.darker()));
            }

            // 2. Shape resolution (dynamic data mapping)
            PointShape activeShape = defaultShape;
            if (hasShapeColumn && i < rawShape.size()) {
                var rsh = rawShape.get(i);
                if (rsh != null) {
                    activeShape = shapeScale.shapeFor(rsh);
                }
            }

            // 3. Size resolution: a manual size override wins, otherwise
            // area-proportional radii over the column's global range
            // (conventional size-scale semantics).
            double drawRadius = radius;
            if (rawSize != null && i < rawSize.size()) {
                var rs = rawSize.get(i);
                if (rs != null) {
                    var manual = scaleSpec.manualSizeFor(rs);
                    if (manual != null) {
                        drawRadius = manual;
                    } else if (rs instanceof Number num && sizeRange != null) {
                        drawRadius = SizeScale.radiusFor(num.doubleValue(), sizeRange);
                    }
                }
            }

            if (paintStable) {
                var bucket = styleBuckets.computeIfAbsent(style,
                        s -> {
                            styles.add(s);
                            return styles.size() - 1;
                        });
                xs[stableCount] = cx;
                ys[stableCount] = cy;
                styleIdx[stableCount] = bucket;
                if (stableRadii != null) {
                    stableRadii[stableCount] = drawRadius;
                }
                if (stableShapes != null) {
                    stableShapes[stableCount] = activeShape;
                }
                stableCount++;
            } else {
                if (!Objects.equals(style.fill(), lastFill)) {
                    gc.setFill(style.fill());
                    lastFill = style.fill();
                }
                if (!Objects.equals(style.darker(), lastStroke)) {
                    gc.setStroke(style.darker());
                    lastStroke = style.darker();
                }
                // Cross specialty: needs a slightly thicker stroke
                if (hasShapeColumn) {
                    gc.setLineWidth(activeShape == PointShape.CROSS ? 1.5 : 1.0);
                }
                boolean suppress = suppressStrokes
                        && drawRadius <= GeomPoint.SUPPRESS_STROKE_RADIUS;
                if (suppress) {
                    activeShape.draw(gc, cx, cy, drawRadius, true);
                } else {
                    activeShape.draw(gc, cx, cy, drawRadius);
                }
            }
        }
        return new Counts(skipped);
    }

    private static Color alpha(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getOpacity() * opacity);
    }
}
