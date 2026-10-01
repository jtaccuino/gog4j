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
import java.util.Arrays;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.data.Temporals;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Renders a tile (heatmap) geometry, mapping fill values through a
 * {@link ContinuousColorScale} onto axis-aligned rectangles.
 * <p>
 * Tiles are centered on their data values via the scale, and their
 * width/height is derived from the minimum non-zero gap between sorted
 * values along each axis.  Edge tiles may extend slightly beyond the
 * plotting area so that they stay aligned with axis ticks.
 *
 * @param <DF> the data-frame type
 */
public class GeomTile<DF> implements Layer<DF> {

    private String cmapName = "viridis";
    private double lineWidth;

    /** The layer's position adjustment, e.g. {@code positionOnFace()} for cube-face placement. */
    private PositionAdjust position = Position.IDENTITY;

    /**
     * Creates a tile (heatmap) geometry with the default colour ramp.
     */
    public GeomTile() {
    }

    /**
     * {@return this} Sets the color ramp name (viridis, plasma, blues, greens, reds, greys).
     *
     * @param name the colour ramp name
     */
    public GeomTile<DF> cmap(String name) { this.cmapName = name; return this; }

    /** {@return the currently configured color ramp name} */
    public String getCmapName() { return cmapName; }

    /**
     * Sets the stroke width between tiles. When > 0, each tile is shrunk by
     * half the line width and stroked with white, creating visible grid lines.
     * Default is 0 (directly adjacent, no visible gap).
     *
     * @param lw the tile border stroke width in pixels
     * @return this {@code GeomTile} for fluid chaining
     */
    public GeomTile<DF> lineWidth(double lw) { this.lineWidth = lw; return this; }

    /** {@return the current tile border stroke width} */
    public double getLineWidth() { return lineWidth; }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} so the heatmap lies flat on a face of a
     * 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code GeomTile} for fluid chaining
     */
    public GeomTile<DF> position(PositionAdjust position) {
        this.position = position;
        return this;
    }

    @Override
    public PositionAdjust positionAdjust() {
        return position;
    }

    @Override
    public boolean wantsDefaultExpansion() {
        // Heatmap tiles sit flush with the panel edges; the default scale
        // headroom would leave an unwanted gap around the grid.
        return false;
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        var ext = ctx.extractor();
        var df = ctx.globalDf();
        var aes = ctx.aes();
        double xMin = bounds.xMin(), xMax = bounds.xMax();
        double yMin = bounds.yMin(), yMax = bounds.yMax();
        if (!xDiscrete) {
            double step = estimateStep(ext, df, aes.x());
            if (step > 0) { xMin -= step / 2; xMax += step / 2; }
        }
        if (!yDiscrete) {
            double step = estimateStep(ext, df, aes.y());
            if (step > 0) { yMin -= step / 2; yMax += step / 2; }
        }
        return new Bounds(xMin, xMax, yMin, yMax);
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var fillCol = aes.fill();
        if (fillCol == null) return;
        var rawFill = ext.getColumn(df, fillCol);
        var n = ext.getRowCount(df);
        if (n == 0) return;

        double[] xs = new double[n];
        double[] ys = new double[n];
        double[] fills = new double[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            var xv = rawX.get(i);
            var yv = rawY.get(i);
            var fv = rawFill.get(i);
            if (xv != null && yv != null && fv instanceof Number fn) {
                xs[count] = sx.toData(xv);
                ys[count] = sy.toData(yv);
                fills[count] = fn.doubleValue();
                count++;
            }
        }
        if (count == 0) return;

        double fillMin = fills[0], fillMax = fills[0];
        for (int i = 1; i < count; i++) {
            if (fills[i] < fillMin) fillMin = fills[i];
            if (fills[i] > fillMax) fillMax = fills[i];
        }

        var scale = new ContinuousColorScale(cmapName);

        double tileW = estimateStep(xs, count);
        double tileH = estimateStep(ys, count);

        if (coord instanceof CoordPolar polar) {
            // A polar coordinate system turns each heatmap cell into an annular
            // wedge: the x step sweeps the angle, the y step spans the radius.
            renderPolar(gc, polar, xs, ys, fills, count, scale, fillMin, fillMax, tileW, tileH);
            return;
        }

        boolean facePlaced = coord instanceof Coord3D coord3d && coord3d.isFaceRendering();

        double xRange = sx.maxData() - sx.minData();
        double yRange = sy.maxData() - sy.minData();
        double xPixRange = Math.abs(sx.maxPixel() - sx.minPixel());
        double yPixRange = Math.abs(sy.maxPixel() - sy.minPixel());
        double hwPx = tileW / xRange * xPixRange / 2.0;
        double hhPx = tileH / yRange * yPixRange / 2.0;

        double inset = lineWidth > 0 ? lineWidth / 2.0 : 0;
        double seam = lineWidth > 0 ? 0.0 : 0.5;

        for (int i = 0; i < count; i++) {
            var fill = scale.colorFor(fills[i], fillMin, fillMax);
            if (facePlaced) {
                // A face-placed heatmap lies on the cube face plane: each cell is
                // a parallelogram whose four data-space corners are projected
                // through the 3-D camera, so the grid recedes with the face
                // instead of billboarding toward the viewer.
                drawFaceTile(gc, coord, sx, sy, xs[i], ys[i], tileW / 2.0, tileH / 2.0, fill);
                continue;
            }
            double px = coord.xPixel(sx, sy, xs[i], ys[i]);
            double py = coord.yPixel(sx, sy, xs[i], ys[i]);
            double left = px - hwPx + inset - seam;
            double top = py - hhPx + inset - seam;
            double w = hwPx * 2 - inset * 2 + seam * 2;
            double h = hhPx * 2 - inset * 2 + seam * 2;

            gc.setFill(fill);
            gc.fillRect(left, top, w, h);

            if (lineWidth > 0) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(lineWidth);
                gc.strokeRect(px - hwPx + inset, py - hhPx + inset,
                              hwPx * 2 - inset * 2, hhPx * 2 - inset * 2);
            }
        }
    }

    /**
     * Paints one face-placed heatmap cell as a projected quad: the cell's four
     * data-space corners (center ± half a step along each in-face axis) are run
     * through the face coordinate system, then the quad is filled and optionally
     * outlined. The pixel seam/inset of the flat rect renderer is applied
     * radially — expanding 0.5 px to avoid hairline cracks, or shrinking by half
     * the line width so the white grid lines read.
     */
    private void drawFaceTile(DrawSurface gc, Coord coord, Scale sx, Scale sy,
                              double xc, double yc, double hw, double hh, Color fill) {
        double[] px = new double[4];
        double[] py = new double[4];
        px[0] = coord.xPixel(sx, sy, xc - hw, yc - hh);
        py[0] = coord.yPixel(sx, sy, xc - hw, yc - hh);
        px[1] = coord.xPixel(sx, sy, xc + hw, yc - hh);
        py[1] = coord.yPixel(sx, sy, xc + hw, yc - hh);
        px[2] = coord.xPixel(sx, sy, xc + hw, yc + hh);
        py[2] = coord.yPixel(sx, sy, xc + hw, yc + hh);
        px[3] = coord.xPixel(sx, sy, xc - hw, yc + hh);
        py[3] = coord.yPixel(sx, sy, xc - hw, yc + hh);

        double d = lineWidth > 0 ? -lineWidth / 2.0 : 0.5;
        if (d != 0) {
            double cx = (px[0] + px[1] + px[2] + px[3]) / 4.0;
            double cy = (py[0] + py[1] + py[2] + py[3]) / 4.0;
            for (int k = 0; k < 4; k++) {
                double dx = px[k] - cx;
                double dy = py[k] - cy;
                double len = Math.hypot(dx, dy);
                if (len > 1e-9) {
                    double f = (len + d) / len;
                    px[k] = cx + dx * f;
                    py[k] = cy + dy * f;
                }
            }
        }

        gc.setFill(fill);
        gc.fillPolygon(px, py, 4);
        if (lineWidth > 0) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(lineWidth);
            gc.strokePolygon(px, py, 4);
        }
    }

    /**
     * Paints the heatmap cells of a polar plot as annular wedges: each tile is
     * the sector between {@code x ± tileW/2} angles and {@code y ± tileH/2}
     * radii, filled through the continuous colour scale. An optional white
     * seam outline (when {@link #lineWidth(double) lineWidth} is set) strokes
     * the wedge's borders so adjacent cells read as a grid.
     */
    private void renderPolar(DrawSurface gc, CoordPolar polar, double[] xs, double[] ys, double[] fills,
                             int count, ContinuousColorScale scale, double fillMin, double fillMax,
                             double tileW, double tileH) {
        var hw = tileW / 2.0;
        var hh = tileH / 2.0;
        for (int i = 0; i < count; i++) {
            var fill = scale.colorFor(fills[i], fillMin, fillMax);
            gc.setFill(fill);
            polar.drawWedge(gc, xs[i] - hw, xs[i] + hw, ys[i] - hh, ys[i] + hh);
            if (lineWidth > 0) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(lineWidth);
                polar.strokeWedge(gc, xs[i] - hw, xs[i] + hw, ys[i] - hh, ys[i] + hh);
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
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var fillCol = aes.fill();
        if (fillCol == null) return null;
        var rawFill = ext.getColumn(df, fillCol);
        var n = ext.getRowCount(df);
        if (n == 0) return null;

        double[] xs = new double[n];
        double[] ys = new double[n];
        double[] fills = new double[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            var xv = rawX.get(i);
            var yv = rawY.get(i);
            var fv = rawFill.get(i);
            if (xv != null && yv != null && fv instanceof Number fn) {
                xs[count] = sx.toData(xv);
                ys[count] = sy.toData(yv);
                fills[count] = fn.doubleValue();
                count++;
            }
        }
        if (count == 0) return null;

        double tileW = estimateStep(xs, count);
        double tileH = estimateStep(ys, count);

        if (coord instanceof CoordPolar polar) {
            var hw = tileW / 2.0;
            var hh = tileH / 2.0;
            for (int i = 0; i < count; i++) {
                if (wedgeContains(polar, xs[i] - hw, xs[i] + hw, ys[i] - hh, ys[i] + hh, mx, my)) {
                    return tileTooltip(sx, sy, xs[i], ys[i], fills[i]);
                }
            }
            return null;
        }

        double xRange = sx.maxData() - sx.minData();
        double yRange = sy.maxData() - sy.minData();
        double xPixRange = Math.abs(sx.maxPixel() - sx.minPixel());
        double yPixRange = Math.abs(sy.maxPixel() - sy.minPixel());
        double hwPx = tileW / xRange * xPixRange / 2.0;
        double hhPx = tileH / yRange * yPixRange / 2.0;
        double inset = lineWidth > 0 ? lineWidth / 2.0 : 0;
        double seam = lineWidth > 0 ? 0.0 : 0.5;

        for (int i = 0; i < count; i++) {
            double px = coord.xPixel(sx, sy, xs[i], ys[i]);
            double py = coord.yPixel(sx, sy, xs[i], ys[i]);
            double left = px - hwPx + inset - seam;
            double top = py - hhPx + inset - seam;
            double right = px + hwPx - inset + seam;
            double bottom = py + hhPx - inset + seam;

            if (mx >= left && mx <= right && my >= top && my <= bottom) {
                return tileTooltip(sx, sy, xs[i], ys[i], fills[i]);
            }
        }
        return null;
    }

    private static String tileTooltip(Scale sx, Scale sy, double x, double y, double value) {
        return String.format("X: %s\nY: %s\nValue: %,.2f", sx.getLabel(x), sy.getLabel(y), value);
    }

    /**
     * Estimates the grid step from a data column, as the minimum non-zero gap
     * between sorted unique values. Used to expand continuous axis bounds by
     * half a tile so edge tiles are not clipped.
     *
     * @param <DF>    the DataFrame type
     * @param ext     the data extractor
     * @param df      the DataFrame
     * @param column  the numeric or temporal column name
     * @return the estimated step, or {@code 1.0} if no gap could be derived
     */
    public static <DF> double estimateStep(DataExtractor<DF> ext, DF df, String column) {
        var raw = ext.getColumn(df, column);
        var n = ext.getRowCount(df);
        double[] vals = new double[n];
        int k = 0;
        for (var v : raw) {
            if (v instanceof Number num) vals[k++] = num.doubleValue();
            else if (v instanceof LocalDate ld) vals[k++] = ld.toEpochDay();
            else if (Temporals.isTimestamp(v)) vals[k++] = Temporals.toEpochMillis(v);
        }
        return estimateStep(vals, k);
    }

    /** @return the minimum non-zero gap between sorted values, or {@code 1.0} if no gap found */
    private static double estimateStep(double[] vals, int n) {
        if (n <= 1) return 1.0;
        double[] sorted = Arrays.copyOf(vals, n);
        Arrays.sort(sorted);
        double minGap = Double.MAX_VALUE;
        for (int i = 1; i < n; i++) {
            double gap = sorted[i] - sorted[i - 1];
            if (gap > 1e-10 && gap < minGap) minGap = gap;
        }
        return minGap < Double.MAX_VALUE ? minGap : 1.0;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }

    /**
     * Whether the mouse position falls inside the annular wedge between two
     * theta values and two radius values, using the same angular convention as
     * {@link CoordPolar#thetaAngle(double)} (zero at twelve o'clock, positive
     * clockwise on screen) — the polar hit-test of {@code locate}.
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
}
