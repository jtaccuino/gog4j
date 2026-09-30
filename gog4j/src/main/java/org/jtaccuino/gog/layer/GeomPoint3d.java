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
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.SizeScale;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * Renders 3D points via axonometric projection onto a 2D canvas.
 * <p>
 * Delegates to {@link Coord3D#projectData} for the 3D-to-2D mapping and
 * supports categorical z-axes, color/grouping, size and alpha mapped from data
 * columns ({@code aes(size=…)} / {@code aes(alpha=…)}), and 2D reference
 * elements projected onto cube faces: reference lines connecting each point to
 * its shadows, and reference circles or dots sitting on the configured faces.
 * All elements are depth-sorted in one pass so far objects never paint over
 * nearer ones.
 *
 * @param <DF> the data-frame type
 */
public class GeomPoint3d<DF> implements Layer<DF> {

    private Point3dSpec spec = new Point3dSpec();

    /** The layer's position adjustment, e.g. {@code Positions.positionOnFace(...)} to flatten it onto a cube face. */
    private PositionAdjust position = Position.IDENTITY;

    /**
     * Creates a 3D point geometry with default rendering options.
     */
    public GeomPoint3d() {
    }

    private record ResolvedPoint3d(double x, double y, double z, double sx, double sy,
                      double depthScale, double radius, double alpha, int index) {}

    /** One paintable reference element of a raw 3-D point. */
    private sealed interface Element permits RawPointElement, RefLineElement, CircleElement, RefDotElement {

        /** The index into the resolved-point list this element draws. */
        int index();

        /** The camera nearness used for depth ordering (larger = nearer). */
        double nearness();
    }

    /** The data point itself, drawn at its projected position. */
    private record RawPointElement(int index, double nearness) implements Element {
    }

    /** A reference line from the point to a face. */
    private record RefLineElement(int index, CubeFace face, double nearness) implements Element {
    }

    /** A reference circle on a face. */
    @SuppressWarnings("ArrayRecordComponent") // read-only render carrier
    private record CircleElement(int index, CubeFace face, double nearness, double[] polygon) implements Element {
    }

    /** A reference dot on a face. */
    private record RefDotElement(int index, CubeFace face, double nearness) implements Element {
    }

    /**
     * Replaces the current rendering specification entirely.
     *
     * @param spec the new {@link Point3dSpec} to apply
     */
    public void setSpec(Point3dSpec spec) { this.spec = spec; }

    /**
     * {@return this} Sets a constant fill color for all points.
     *
     * @param color the JavaFX fill {@link Color}
     */
    public GeomPoint3d<DF> color(Color color) { this.spec.color(color); return this; }

    /**
     * {@return this} Sets the point diameter in pixels.
     *
     * @param size the point diameter in pixels
     */
    public GeomPoint3d<DF> size(double size) { this.spec.size(size); return this; }

    /** {@return this} Sets the opacity in [0, 1].
     *
     * @param opacity the opacity between 0.0 and 1.0 */
    public GeomPoint3d<DF> opacity(double opacity) { this.spec.opacity(opacity); return this; }

    /**
     * Sets the layer's position adjustment — e.g. cube-face flattening via
     * {@link Positions#positionOnFace} so every point projects onto one face of
     * the cube (an orthogonal shadow of the point cloud).
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code GeomPoint3d} instance for fluid chaining
     */
    public GeomPoint3d<DF> position(PositionAdjust position) {
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
        var coord = ctx.plot().coord();
        var theme = ctx.plot().theme();
        var df = ctx.partitionDf();
        if (!(coord instanceof Coord3D coord3d) || aes.z() == null) return;

        var sizeColumn = aes.size();
        var rawSize = sizeColumn != null ? ext.getColumn(df, sizeColumn) : null;
        var sizeRange = rawSize != null ? scales.numericRange(sizeColumn) : null;
        var scaleSpec = ctx.plot().scaleSpec();

        var baseRadius = spec.getSize() / 2.0;
        var baseOpacity = spec.getOpacity();

        var cube = CubeStyle.from(theme);
        var colorGroup = aes.color() != null ? aes.color() : aes.fill();
        var rawColorGroup = colorGroup != null ? ext.getColumn(df, colorGroup) : null;
        var color = spec.getColor();
        var mapper = Primitive3dMapper.forPrimitives(ctx, spec.getColor(), baseOpacity, cube.depthScaleStrength());

        var pts = new ArrayList<ResolvedPoint3d>(mapper.rowCount());
        for (var i = 0; i < mapper.rowCount(); i++) {
            var r = mapper.resolve(i);
            if (r == null) continue;

            double radius = baseRadius;
            if (rawSize != null && i < rawSize.size()) {
                var rs = rawSize.get(i);
                if (rs != null) {
                    var manual = scaleSpec.manualSizeFor(rs);
                    if (manual != null) {
                        radius = manual;
                    } else if (rs instanceof Number num && sizeRange != null) {
                        radius = SizeScale.radiusFor(num.doubleValue(), sizeRange);
                    }
                }
            }
            pts.add(new ResolvedPoint3d(r.x(), r.y(), r.z(), r.sx(), r.sy(), r.depthScale(), radius, r.alpha(), i));
        }

        if (pts.isEmpty()) return;

        var elements = new ArrayList<Element>(pts.size());
        var labels = new ArrayList<String>(pts.size());
        for (var k = 0; k < pts.size(); k++) {
            var p = pts.get(k);
            if (spec.isRawPoints()) {
                elements.add(new RawPointElement(k, p.depthScale()));
                labels.add("pointRaw" + p.index());
            }
            for (CubeFace face : spec.getRefFaces()) {
                var proj = coord3d.projectToFace(p.x, p.y, p.z, face);
                if (proj == null) continue;
                switch (spec.getRefPoints()) {
                    case CIRCLES -> {
                        var poly = circlePolygon(coord3d, p.x, p.y, p.z, face);
                        if (poly != null) {
                            elements.add(new CircleElement(k, face, proj.depthScale(), poly));
                            labels.add("circle" + face + "_" + p.index());
                        }
                    }
                    case POINTS -> {
                        elements.add(new RefDotElement(k, face, proj.depthScale()));
                        labels.add("point" + face + "_" + p.index());
                    }
                    default -> { }
                }
                if (spec.isRefLines()) {
                    elements.add(new RefLineElement(k, face, (p.depthScale() + proj.depthScale()) / 2.0));
                    labels.add("segment" + face + "_" + p.index());
                }
            }
        }

        if (elements.isEmpty()) return;

        double[] nearness = new double[elements.size()];
        for (int k = 0; k < elements.size(); k++) nearness[k] = elements.get(k).nearness();
        var groupCol = aes.group();
        String[] groupLabels = null;
        if (groupCol != null) {
            var rawGroup = ext.getColumn(df, groupCol);
            if (rawGroup != null) {
                groupLabels = new String[elements.size()];
                for (int k = 0; k < elements.size(); k++) {
                    var gv = rawGroup.get(elements.get(k).index());
                    groupLabels[k] = gv == null ? null : gv.toString() + "/" + labels.get(k);
                }
            }
        }
        int[] renderOrder = groupLabels != null
                ? DepthSorter.order(groupLabels, nearness)
                : DepthSorter.order(labels.toArray(String[]::new), nearness);

        gc.setLineWidth(cube.point3dStrokeWidth());
        var fallbackColor = theme.fallbackColor();
        double depthStrength = cube.depthScaleStrength();

        for (int oi = 0; oi < renderOrder.length; oi++) {
            var el = elements.get(renderOrder[oi]);
            var p = pts.get(el.index());
            var px = p.sx;
            var py = p.sy;
            switch (el) {
                case RawPointElement r -> drawRawPoint(gc, scales, colorGroup, rawColorGroup,
                        p, color, fallbackColor, depthStrength);
                case RefLineElement r -> {
                    var proj = coord3d.projectToFace(p.x, p.y, p.z, r.face());
                    var lineColor = spec.getRefLineColor() != null ? spec.getRefLineColor()
                            : resolvedFill(scales, colorGroup, rawColorGroup, p, color, fallbackColor);
                    var avgDepth = (p.depthScale + proj.depthScale()) / 2.0;
                    var lw = Math.max(0, spec.getRefLineLinewidth() * CubeStyle.depthFactor(avgDepth, depthStrength));
                    var alpha = spec.getRefLineAlpha() != null ? spec.getRefLineAlpha() : 0.5;
                    gc.setStroke(Colors.withAlpha(lineColor, alpha));
                    gc.setLineWidth(lw);
                    gc.setLineDashes(spec.getRefLineDashes());
                    gc.strokeLine(px, py, proj.sx(), proj.sy());
                    gc.setLineDashes((double[]) null);
                }
                case CircleElement c -> {
                    var fill = spec.getRefPointFill() != null ? spec.getRefPointFill()
                            : resolvedFill(scales, colorGroup, rawColorGroup, p, color, fallbackColor);
                    var stroke = spec.getRefPointColor();
                    var alpha = spec.getRefPointAlpha() != null ? spec.getRefPointAlpha()
                            : (p.alpha < 1.0 ? p.alpha : 0.5);
                    var depthFactor = CubeStyle.depthFactor(p.depthScale, depthStrength);
                    fill = Colors.withAlpha(fill, alpha);
                    gc.setFill(fill);
                    int m = c.polygon().length / 2;
                    var xs = new double[m];
                    var ys = new double[m];
                    for (int k = 0; k < m; k++) {
                        xs[k] = c.polygon()[k * 2];
                        ys[k] = c.polygon()[k * 2 + 1];
                    }
                    gc.fillPolygon(xs, ys, m);
                    if (stroke != null) {
                        gc.setStroke(Colors.withAlpha(stroke, alpha));
                        gc.setLineWidth(Math.max(0, spec.getRefPointStroke() * depthFactor));
                        gc.strokePolygon(xs, ys, m);
                    }
                }
                case RefDotElement d -> {
                    var fill = spec.getRefPointColor() != null ? spec.getRefPointColor()
                            : resolvedFill(scales, colorGroup, rawColorGroup, p, color, fallbackColor);
                    var alpha = spec.getRefPointAlpha() != null ? spec.getRefPointAlpha()
                            : (p.alpha < 1.0 ? p.alpha : 0.5);
                    var depthFactor = CubeStyle.depthFactor(p.depthScale, depthStrength);
                    double radius = spec.getRefPointSize() != null
                            ? spec.getRefPointSize() * depthFactor
                            : p.radius * (1.0 / 3.0) * depthFactor;
                    var shape = spec.getRefPointShape() != null ? spec.getRefPointShape() : spec.getShape();
                    gc.setFill(Colors.withAlpha(fill, alpha));
                    shape.draw(gc, px, py, radius);
                }
            }
        }
    }

    private void drawRawPoint(DrawSurface gc, Object scales,
            String colorGroup, Object rawColorGroup, ResolvedPoint3d p, Color color, Color fallbackColor,
            double depthStrength) {
        @SuppressWarnings("unchecked")
        var resolved = (ResolvedScales<?>) scales;
        Color fill;
        if (colorGroup != null && rawColorGroup != null) {
            var gv = ((List<?>) rawColorGroup).get(p.index());
            if (gv != null) {
                var c = resolved.resolvedColorFor(colorGroup, gv);
                fill = c != null ? c : fallbackColor;
            } else {
                fill = fallbackColor;
            }
        } else if (color != null) {
            fill = color;
        } else {
            fill = fallbackColor;
        }
        if (p.alpha < 1.0) {
            fill = Colors.withAlpha(fill, p.alpha);
        }
        double depthFactor = CubeStyle.depthFactor(p.depthScale(), depthStrength);
        gc.setFill(fill);
        gc.setStroke(fill.darker());
        spec.getShape().draw(gc, p.sx(), p.sy(), p.radius() * depthFactor);
    }

    private static Color resolvedFill(Object scales,
            String colorGroup, Object rawColorGroup, ResolvedPoint3d p, Color color, Color fallbackColor) {
        if (colorGroup != null && rawColorGroup != null) {
            @SuppressWarnings("unchecked")
            var resolved = (ResolvedScales<?>) scales;
            var gv = ((List<?>) rawColorGroup).get(p.index());
            if (gv != null) {
                var c = resolved.resolvedColorFor(colorGroup, gv);
                return c != null ? c : fallbackColor;
            }
            return fallbackColor;
        }
        return color != null ? color : fallbackColor;
    }

    /** The screen-space polygon of a reference circle lying on a cube face. */
    private double[] circlePolygon(Coord3D coord3d, double x, double y, double z, CubeFace face) {
        int axis = Coord3D.faceAxis(face);
        if (axis < 0) return null;
        double fixed = coord3d.faceCoordinate(face);
        double fraction = spec.getRefCircleRadius() / 100.0;
        double rx = coord3d.dataSpanX() * fraction;
        double ry = coord3d.dataSpanY() * fraction;
        double rz = coord3d.dataSpanZ() * fraction;
        double r1 = switch (axis) {
            case 0 -> ry;
            case 1 -> rx;
            default -> rx;
        };
        double r2 = switch (axis) {
            case 0 -> rz;
            case 1 -> rz;
            default -> ry;
        };
        int m = Math.max(3, spec.getRefCircleVertices());
        var out = new double[m * 2];
        for (int k = 0; k < m; k++) {
            double a = 2 * Math.PI * k / m;
            double[] v = {x, y, z};
            switch (axis) {
                case 0 -> { v[1] = y + r1 * Math.cos(a); v[2] = z + r2 * Math.sin(a); }
                case 1 -> { v[0] = x + r1 * Math.cos(a); v[2] = z + r2 * Math.sin(a); }
                default -> { v[0] = x + r1 * Math.cos(a); v[1] = y + r2 * Math.sin(a); }
            }
            v[axis] = fixed;
            var proj = coord3d.projectData(v[0], v[1], v[2]);
            out[k * 2] = proj.sx();
            out[k * 2 + 1] = proj.sy();
        }
        return out;
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        if (!(coord instanceof Coord3D coord3d) || aes.z() == null) return null;

        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var rawZ = ext.getColumn(df, aes.z());
        var n = ext.getRowCount(df);
        if (n == 0) return null;

        var colorGroup = aes.color() != null ? aes.color() : aes.fill();
        var rawColorGroup = colorGroup != null ? ext.getColumn(df, colorGroup) : null;

        var pts = new ArrayList<ResolvedPoint3d>(n);
        for (var i = 0; i < n; i++) {
            var rx = rawX.get(i); var ry = rawY.get(i); var rz = rawZ.get(i);
            if (rx == null || ry == null || rz == null) continue;
            double x = Values.toDouble(rx);
            double y = Values.toDouble(ry);
            double z = coord3d.toZValue(rz);
            var proj = coord3d.projectData(x, y, z);
            pts.add(new ResolvedPoint3d(x, y, z, proj.sx(), proj.sy(), proj.depthScale(), 0, 1, i));
        }

        if (pts.isEmpty()) return null;

        double hitTolerance = spec.getSize() / 2.0 + CubeStyle.from(ctx.plot().theme()).point3dHitTolerance();
        ResolvedPoint3d best = null;
        for (var p : pts) {
            var dx = mx - p.sx();
            var dy = my - p.sy();
            if (Math.sqrt(dx * dx + dy * dy) <= hitTolerance
                    && (best == null || p.depthScale() > best.depthScale())) {
                best = p;
            }
        }
        if (best == null) {
            return null;
        }
        var i = best.index();
        var labelX = Values.label(rawX.get(i));
        var labelY = Values.label(rawY.get(i));
        var labelZ = Values.label(rawZ.get(i));

        if (colorGroup != null && rawColorGroup != null) {
            return Tooltip3d.position(rawColorGroup.get(i), labelX, labelY, labelZ);
        }
        return Tooltip3d.position(labelX, labelY, labelZ);
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        int n = ctx.plot().extractor().getRowCount(ctx.partitionDf());
        int faces = Math.max(1, spec.getRefFaces().length);
        int perPoint = (spec.isRawPoints() ? 1 : 0) + (spec.isRefLines() ? faces : 0) + faces;
        return n * perPoint;
    }
}
