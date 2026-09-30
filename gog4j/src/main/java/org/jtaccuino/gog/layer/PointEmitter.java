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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.DiscreteShapeScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.scale.SizeScale;

/**
 * Dedicated compilation root for the constant-style point emission loop.
 * <p>
 * A layer without a colour/fill mapping and without a per-point alpha draws
 * every point with one fixed style. {@link GeomPoint} resolves and applies
 * that style once, then hands off to {@link #emit}: this object is a small,
 * separate final class whose single hot method forms its own compilation
 * unit. Composed and matrix plots call leaves through deep render chains, and
 * when the loop lives inside the large {@code GeomPoint.render} method (which
 * also carries the whole colour-mapped machinery) the JIT's per-compilation
 * inlining budget can run out before the per-point leaf calls are inlined,
 * leaving them as runtime virtual calls. Keeping the loop here, decoupled from
 * the upstream call depth, lets the JIT always fully inline the per-point
 * leaves ({@link Scale#toPixel(double)}, {@link PointShape#draw}...) into a
 * tight loop regardless of how the plotting code above it is compiled.
 */
final class PointEmitter {

    /** Counts reported by the constant-style emitter for the phase event. */
    record Counts(int skipped, int binned) {}

    private PointEmitter() {
    }

    /**
     * Emits a constant-style point layer through a dedicated tight loop. The
     * fixed fill and stroke are already applied to {@code gc}, so this method
     * only resolves positions, optional shape/size overrides and the stroke
     * suppression flag, then draws. All mapped-layer concerns (colour buckets,
     * paint-stable buffering, per-point alpha) stay in {@link GeomPoint#render}.
     * <p>
     * The inputs are passed as scalar parameters rather than a context record
     * on purpose: the loop is large enough that C2 often compiles it as its own
     * compilation unit instead of inlining it into {@code render}, and a record
     * handed across that boundary escapes scalar replacement — one heap
     * allocation per layer. On dense multi-panel renders (a scatter matrix,
     * where the same emitter runs once per panel) that per-layer allocation
     * shows up as a measurable slowdown, so the hot entry point is kept
     * allocation-free.
     *
     * @param gc               the target drawing surface (fill/stroke already set)
     * @param rawX             the raw x column, or {@code null}
     * @param rawY             the raw y column, or {@code null}
     * @param rawShape         the per-point shape column, or {@code null}
     * @param rawSize          the per-point size column, or {@code null}
     * @param n                number of rows
     * @param yFromX           whether the y aesthetic reads the x value through the x scale
     * @param xFromY           whether the x aesthetic reads the y value through the y scale
     * @param xDataScale       the data x scale (flip-adjusted)
     * @param yDataScale       the data y scale (flip-adjusted)
     * @param sx               the pixel x scale
     * @param sy               the pixel y scale
     * @param coord            the coordinate system
     * @param flipped          whether the axes are flipped
     * @param mapThroughCoord  whether to route points through the coordinate system
     *                         (polar, or a positionOnFace face projection)
     * @param radius           the default point radius in pixels
     * @param defaultShape     the default point shape
     * @param shapeScale       the shape scale, or {@code null}
     * @param sizeRange        the size range, or {@code null}
     * @param scaleSpec        the plot's scale spec (manual size overrides)
     * @param suppressStrokes  whether to omit point outlines (small-dot fast path)
     * @param binOverdraw      whether to pixel-quantize identical dots (FAST path)
     * @param linearPixels     whether {@code sx}/{@code sy} are identity, non-reversed scales
     * @param sxMul            pixel-x slope ({@code 0} when not linear)
     * @param sxAdd            pixel-x intercept ({@code 0} when not linear)
     * @param syMul            pixel-y slope ({@code 0} when not linear)
     * @param syAdd            pixel-y intercept ({@code 0} when not linear)
     * @param geom             the owning geometry (jitter + jitter flag)
     * @return the skipped and overdraw-binned point counts
     */
    static Counts emit(DrawSurface gc, List<?> rawX, List<?> rawY, List<?> rawShape, List<?> rawSize,
            int n, boolean yFromX, boolean xFromY,
            Scale xDataScale, Scale yDataScale, Scale sx, Scale sy,
            Coord coord, boolean flipped, boolean mapThroughCoord,
            double radius, PointShape defaultShape,
            DiscreteShapeScale shapeScale, MinMax sizeRange,
            ScaleSpec scaleSpec,
            boolean suppressStrokes, boolean binOverdraw,
            boolean linearPixels, double sxMul, double sxAdd, double syMul, double syAdd,
            GeomPoint<?> geom) {
        var jittered = geom.usesJitter();

        var xt = xDataScale.transform();
        var yt = yDataScale.transform();
        // Interactive speed path (RenderMode.FAST, Canvas backend only; vector
        // export always resolves to FULL): overdraw binning pixel-quantizes an
        // identical opaque dot per occupied pixel, so a dense point layer
        // records at most one primitive per occupied pixel instead of one per
        // point. Safe only when every point shares the same fill, shape, size,
        // and full opacity. The surviving point is drawn at the integer pixel
        // center, keeping its oval on-pixel.
        Set<Long> binned = binOverdraw ? new HashSet<>() : null;
        int skipped = 0;
        int binnedOut = 0;
        for (var i = 0; i < n; i++) {
            var rx = rawX == null ? null : rawX.get(i);
            var ry = rawY == null ? null : rawY.get(i);
            if ((rx == null && !xFromY) || (ry == null && !yFromX)) {
                skipped++;
                continue;
            }
            var xDouble = xFromY ? yt.forward(yDataScale.toData(ry)) : xDataScale.toData(rx);
            var yDouble = yFromX ? xt.forward(xDataScale.toData(rx)) : yDataScale.toData(ry);
            if (jittered) {
                xDouble = geom.jitterX(xDouble, i);
                yDouble = geom.jitterY(yDouble, i);
            }
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
            if (binOverdraw) {
                long key = (Math.round(cx) << 32) | (Math.round(cy) & 0xffffffffL);
                if (!binned.add(key)) {
                    binnedOut++;
                    continue;
                }
                cx = Math.round(cx);
                cy = Math.round(cy);
            }
            PointShape activeShape = defaultShape;
            if (rawShape != null && i < rawShape.size()) {
                var rsh = rawShape.get(i);
                if (rsh != null) {
                    activeShape = shapeScale.shapeFor(rsh);
                }
                gc.setLineWidth(activeShape == PointShape.CROSS ? 1.5 : 1.0);
            }
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
            if (suppressStrokes && drawRadius <= GeomPoint.SUPPRESS_STROKE_RADIUS) {
                activeShape.draw(gc, cx, cy, drawRadius, true);
            } else {
                // The 3-arg overload passes a constant {@code false}: the JIT
                // can eliminate the stroke branch entirely, matching the
                // monolithic render loop's per-point cost.
                activeShape.draw(gc, cx, cy, drawRadius);
            }
        }
        return new Counts(skipped, binnedOut);
    }
}
