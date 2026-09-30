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
import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * {@link org.jtaccuino.gog.Geoms#segment3d()} geometry: one directed 3D
 * segment per row from
 * {@code (x, y, z)} to {@code (xend, yend, zend)}, projected with
 * {@link Coord3D} and depth-sorted back-to-front. Line width is scaled by the
 * segment's distance from the camera (unless {@link #scaleDepth(boolean)
 * disabled}), and each segment is depth-sorted as an independent primitive, so
 * far and near segments interleave correctly.
 * <p>
 * The {@code aes} mapping may also provide {@code colour}, {@code alpha}
 * (both mapped per row), {@code group} (per-row grouping for labelling), and a
 * constant {@code linetype} via {@link #linetype(double...)}.
 * <p>
 * Subclasses such as {@link GeomPath3d} build connected segments from their
 * data and share this sorting-plus-painting engine via
 * {@link #draw(DrawSurface, PanelContext, List)}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomSegment3d<DF> implements Layer<DF> {

    private Color defaultColor = Color.web("#333333");
    private double linewidth = 1.0;
    private double[] dashes;
    private Double defaultAlpha;
    private boolean scaleDepth = true;

    /**
     * Constructs a {@link org.jtaccuino.gog.Geoms#segment3d()} geometry.
     */
    public GeomSegment3d() {
    }

    /**
     * One resolved, paintable 3D segment.
     *
     * @param x1 the start point's x coordinate
     * @param y1 the start point's y coordinate
     * @param z1 the start point's z coordinate
     * @param x2 the end point's x coordinate
     * @param y2 the end point's y coordinate
     * @param z2 the end point's z coordinate
     * @param stroke the resolved stroke colour
     * @param alpha the resolved opacity
     */
    protected record Seg(double x1, double y1, double z1, double x2, double y2, double z2,
                         Color stroke, double alpha) {}

    /**
     * Sets the stroke color for the segments.
     *
     * @param c line color
     * @return this geometry for fluid chaining
     */
    public GeomSegment3d<DF> color(Color c) { this.defaultColor = c; return this; }

    /**
     * Sets the segment stroke width in pixels (before depth scaling).
     *
     * @param w line width in pixels
     * @return this geometry for fluid chaining
     */
    public GeomSegment3d<DF> linewidth(double w) { this.linewidth = w; return this; }

    /**
     * Sets the dash pattern (linetype) of the segments.
     *
     * @param dashes alternating on/off lengths, or {@code null} for solid
     * @return this geometry for fluid chaining
     */
    public GeomSegment3d<DF> linetype(double... dashes) { this.dashes = dashes; return this; }

    /**
     * Sets a constant transparency applied to every segment.
     *
     * @param alpha opacity between 0.0 and 1.0
     * @return this geometry for fluid chaining
     */
    public GeomSegment3d<DF> alpha(double alpha) { this.defaultAlpha = alpha; return this; }

    /**
     * Enables (default) or disables the perspective line-width cue.
     *
     * @param scaleDepth {@code true} to deepen far segments, {@code false} for constant width
     * @return this geometry for fluid chaining
     */
    public GeomSegment3d<DF> scaleDepth(boolean scaleDepth) { this.scaleDepth = scaleDepth; return this; }

    /** {@return the constant stroke color} */
    protected Color color() { return defaultColor; }

    /** {@return the base line width in pixels} */
    protected double linewidth() { return linewidth; }

    /** {@return the dash pattern, or {@code null} for solid lines} */
    protected double[] dashes() { return dashes; }

    /** {@return whether the perspective depth cue is enabled} */
    protected boolean scaleDepth() { return scaleDepth; }

    /** {@return the constant transparency, or {@code null}} */
    protected Double alpha() { return defaultAlpha; }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        if (!(ctx.plot().coord() instanceof Coord3D) || ctx.plot().aes().z() == null) return;
        draw(gc, ctx, buildSegments(ctx));
    }

    /**
     * Builds this geometry's segments from the panel data. The default
     * implementation draws one segment per data row; {@link GeomPath3d}
     * overrides it to connect consecutive points within each group.
     *
     * @param ctx the panel context providing the data and scales
     * @return the resolved segments in row order
     */
    protected List<Seg> buildSegments(PanelContext<DF> ctx) {
        var aes = ctx.plot().aes();
        var xCol = aes.x();
        var yCol = aes.y();
        var zCol = aes.z();
        if (xCol == null || yCol == null || zCol == null) return List.of();

        var df = ctx.partitionDf();
        var ext = ctx.plot().extractor();
        var xEndData = aes.xend() != null ? ext.getColumn(df, aes.xend()) : null;
        var yEndData = aes.yend() != null ? ext.getColumn(df, aes.yend()) : null;
        var zEndData = aes.zend() != null ? ext.getColumn(df, aes.zend()) : null;
        var coord = (Coord3D) ctx.plot().coord();

        var mapper = Primitive3dMapper.forPrimitives(ctx, defaultColor, defaultAlpha != null ? defaultAlpha : 1.0, 0.0);

        var segs = new ArrayList<Seg>(mapper.rowCount());
        for (var i = 0; i < mapper.rowCount(); i++) {
            if (!mapper.has(i)) continue;
            var ex = xEndData != null ? xEndData.get(i) : mapper.rawX(i);
            var ey = yEndData != null ? yEndData.get(i) : mapper.rawY(i);
            var ez = zEndData != null ? zEndData.get(i) : mapper.rawZ(i);
            if (ex == null || ey == null || ez == null) continue;

            segs.add(new Seg(mapper.x(i), mapper.y(i), mapper.z(i),
                    Values.toDouble(ex), Values.toDouble(ey), coord.toZValue(ez),
                    mapper.color(i), mapper.alpha(i)));
        }
        return segs;
    }

    /**
     * Depth-sorts and paints the given segments back-to-front.
     *
     * @param gc   the surface to draw onto
     * @param ctx  the panel context (for the projection)
     * @param segs the segments to paint
     */
    protected void draw(DrawSurface gc, PanelContext<DF> ctx, List<Seg> segs) {
        if (segs.isEmpty()) return;
        var coord = (Coord3D) ctx.plot().coord();
        var cube = CubeStyle.from(ctx.plot().theme());
        double strength = scaleDepth ? cube.depthScaleStrength() : 0.0;

        var nearness = new double[segs.size()];
        var starts = new Coord3D.ProjResult[segs.size()];
        var ends = new Coord3D.ProjResult[segs.size()];
        for (int s = 0; s < segs.size(); s++) {
            var seg = segs.get(s);
            starts[s] = coord.projectData(seg.x1(), seg.y1(), seg.z1());
            ends[s] = coord.projectData(seg.x2(), seg.y2(), seg.z2());
            nearness[s] = (starts[s].depthScale() + ends[s].depthScale()) / 2.0;
        }
        int[] order = DepthSorter.order(nearness);

        gc.setLineDashes(dashes);
        for (var oi : order) {
            var seg = segs.get(oi);
            var a = starts[oi];
            var b = ends[oi];
            double ds = CubeStyle.depthFactor(nearness[oi], strength);
            gc.setStroke(Colors.withAlpha(seg.stroke(), seg.alpha()));
            gc.setLineWidth(Math.max(0, linewidth * ds));
            gc.strokeLine(a.sx(), a.sy(), b.sx(), b.sy());
        }
        gc.setLineDashes((double[]) null);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        if (!(ctx.plot().coord() instanceof Coord3D coord3d) || ctx.plot().aes().z() == null) return null;
        var segs = buildSegments(ctx);
        if (segs.isEmpty()) return null;
        double tolerance = 4.0 + linewidth / 2.0;
        Seg best = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (var seg : segs) {
            var a = coord3d.projectData(seg.x1(), seg.y1(), seg.z1());
            var b = coord3d.projectData(seg.x2(), seg.y2(), seg.z2());
            double distSq = PolygonMath.distanceSqToSegment(mx, my, a.sx(), a.sy(), b.sx(), b.sy());
            if (distSq <= tolerance * tolerance && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = seg;
            }
        }
        if (best == null) return null;
        return Tooltip3d.segment(Values.label(best.x1()), Values.label(best.y1()), Values.label(best.z1()),
                Values.label(best.x2()), Values.label(best.y2()), Values.label(best.z2()));
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
