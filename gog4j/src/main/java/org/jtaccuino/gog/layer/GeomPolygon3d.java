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
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.Pairwise3dSorter;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.geometry.VectorMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.stat.Stats;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * The base 3D polygon geometry layer: renders depth-sorted polygon rings (plus
 * the point and segment primitives of
 * {@link org.jtaccuino.gog.Geoms#smooth3d()})
 * through axonometric projection onto a 2D canvas.
 * <p>
 * Subclasses feed the base with data-space polygons via
 * {@link #polygonize(StatData, PanelContext)}: {@link GeomSurface3d} builds
 * grid/Delaunay tiles, {@link GeomRidgeline3d} builds ridge loops,
 * {@link GeomContour3d} builds contour bands, and {@link GeomSmooth3d} draws
 * the pre-built polygon rows of
 * {@link org.jtaccuino.gog.stat.Stats#smooth3d()}. The base runs the
 * attached stat (see {@link StatHost}) over each panel's partition, projects
 * every polygon, orders the primitives back-to-front ({@link DepthSorter} or
 * {@link Pairwise3dSorter}), and fills/strokes them with depth-scaled line
 * widths, optional back-face culling and {@link Light3d} shading.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomPolygon3d<DF> implements Layer<DF>, StatHost<DF> {

    /**
     * A data-space polygon ring plus the stat rows its vertices came from.
     *
     * @param x the ring's x coordinates
     * @param y the ring's y coordinates
     * @param z the ring's z coordinates
     * @param group the group label, or an internal mesh identifier
     * @param srcRow the source stat row index for each vertex
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only geometry carrier
    protected record Poly3d(double[] x, double[] y, double[] z, String group, int[] srcRow) {

        int n() {
            return x.length;
        }
    }

    /** A single data point or segment endpoint read from the stat output. */
    private record Vertex3D(double x, double y, double z, String group, int srcRow) {
    }

    /** One paintable element: a polygon, a point, or a two-vertex segment. */
    private sealed interface Element permits PolygonElement, SegmentElement, PointElement {
        double nearness();
        String group();
    }

    /** A filled polygon ring, carrying its projected vertex arrays. */
    @SuppressWarnings("ArrayRecordComponent") // read-only render carrier
    private record PolygonElement(double[] screenX, double[] screenY, double[] depthScale,
                                  double nearness, String group, Color fill, Color stroke,
                                  double linewidth, double alpha, double[] normal)
            implements Element {
    }

    /** A two-vertex segment, carrying its two projected endpoints. */
    private record SegmentElement(double startX, double startY, double startDepthScale,
                                  double endX, double endY, double endDepthScale,
                                  double nearness, String group, Color fill, Color stroke,
                                  double linewidth, double alpha)
            implements Element {
    }

    /** A single point, carrying its projected position. */
    private record PointElement(double x, double y, double depthScale,
                                double nearness, String group, Color fill, Color stroke,
                                double linewidth, double alpha, double pointSize)
            implements Element {
    }

    /** The resolved shading of one polygon element, parallel to the element list. */
    private record Lighting(double lightL, Color rgbLight, boolean backface) {
        /** The shading of an unlit element (a point or segment). */
        static Lighting none() {
            return new Lighting(Double.NaN, null, false);
        }
    }

    /** A positional-light falloff pending global resolution once all distances are known. */
    private record Falloff(int elementIndex, double dot, double dist) {
    }

    private static final double SEAM_HIDE_WIDTH = 1.25;

    private Polygon3dSpec spec;
    private Stat<DF> attachedStat;

    /** The layer's attached stat/layer params, read by subclasses (e.g. the
     * function-surface domain report) during the fit phase. */
    protected LayerParams attachedParams = LayerParams.empty();

    /**
     * Creates a polygon geometry with the default rendering specification.
     */
    public GeomPolygon3d() {
        this.spec = new Polygon3dSpec();
    }

    /**
     * Creates a polygon geometry with the given rendering specification.
     *
     * @param spec the {@link Polygon3dSpec} to use
     */
    protected GeomPolygon3d(Polygon3dSpec spec) {
        this.spec = spec;
    }

    /**
     * Replaces the current rendering specification entirely.
     *
     * @param spec the new {@link Polygon3dSpec} to apply
     */
    public void setSpec(Polygon3dSpec spec) {
        this.spec = spec;
    }

    /** {@return the current rendering specification} */
    public Polygon3dSpec getSpec() {
        return spec;
    }

    /** {@return this} Sets a constant fill colour for the polygons.
     *
     * @param color the JavaFX fill {@link Color} */
    public GeomPolygon3d<DF> fill(Color color) { this.spec.fill(color); return this; }

    /** {@return this} Sets a constant stroke colour for the polygons.
     *
     * @param color the JavaFX stroke {@link Color}, or {@code null} to skip */
    public GeomPolygon3d<DF> color(Color color) { this.spec.colour(color); return this; }

    /** {@return this} Sets the polygon stroke width in pixels.
     *
     * @param linewidth the stroke width in pixels */
    public GeomPolygon3d<DF> linewidth(double linewidth) { this.spec.linewidth(linewidth); return this; }

    /** {@return this} Sets the polygon opacity in [0, 1].
     *
     * @param opacity the opacity between 0.0 and 1.0 */
    public GeomPolygon3d<DF> opacity(double opacity) { this.spec.alpha(opacity); return this; }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.identity3d();
    }

    @Override
    public void attach(Stat<DF> stat, PositionAdjust position, LayerParams params) {
        this.attachedStat = stat;
        this.attachedParams = params == null ? LayerParams.empty() : params;
    }

    @Override
    public LayerData prepare(PlotContext<DF> ctx) {
        if (attachedStat == null) {
            return LayerData.NONE;
        }
        var statData = attachedStat.computeLayer(ctx.globalDf(), ctx.extractor(), ctx.aes(), attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return LayerData.NONE;
        }
        return new LayerData.StatLayerData<>(statData, ctx.extractor());
    }

    /**
     * The stat output the geometry draws: the attached stat run over the
     * panel's partition, or {@link org.jtaccuino.gog.stat.Stats#identity3d()}
     * when no stat is bound.
     *
     * @param ctx the panel-scoped context
     * @return the {@link StatData} to render, or {@code null} when empty
     */
    protected StatData statData(PanelContext<DF> ctx) {
        Stat<DF> stat = attachedStat != null ? attachedStat : Stats.<DF>identity3d();
        var statData = stat.computeLayer(ctx.partitionDf(), ctx.plot().extractor(),
                ctx.plot().aes(), attachedParams.stat());
        if (statData == null || statData.isEmpty()) {
            return null;
        }
        return statData;
    }

    /**
     * The stat output the solid geometry emits, run over the plot-global data
     * so the reported corner extents are complete before any panel partition.
     *
     * @param ctx the plot-scoped context
     * @return the {@link StatData}, or {@code null} when empty or stat-less
     */
    private StatData solidStat(PlotContext<DF> ctx) {
        Stat<DF> stat = attachedStat != null ? attachedStat : defaultStat();
        if (stat == null) {
            return null;
        }
        var statData = stat.computeLayer(ctx.globalDf(), ctx.extractor(), ctx.aes(), attachedParams.stat());
        return statData == null || statData.isEmpty() ? null : statData;
    }

    /**
     * The x/y domain a solid (bar/column/voxel) geometry occupies: the outer
     * corners its stat emits. Folded into the scale domain, the cube walls
     * then sit flush against the extreme bars instead of at the raw data
     * range, which would leave each edge bar overhanging half its width
     * outside the cube.
     *
     * @param bounds the raw data domain
     * @param ctx    the plot-scoped context
     * @return the corner-covering {@link Layer.Bounds}, or {@code bounds} when the
     *         stat cannot run
     */
    protected Layer.Bounds expandSolidDomain(Layer.Bounds bounds, PlotContext<DF> ctx) {
        var statData = solidStat(ctx);
        if (statData == null) {
            return bounds;
        }
        var xs = statData.columnAsDoubles("x");
        var ys = statData.columnAsDoubles("y");
        if (xs == null || ys == null) {
            return bounds;
        }
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < statData.rowCount(); i++) {
            double x = xs.get(i);
            if (Double.isFinite(x)) {
                xMin = Math.min(xMin, x);
                xMax = Math.max(xMax, x);
            }
            double y = ys.get(i);
            if (Double.isFinite(y)) {
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        if (xMin > xMax || yMin > yMax) {
            return bounds;
        }
        return new Layer.Bounds(xMin, xMax, yMin, yMax);
    }

    /**
     * The z extent a solid (bar/column/voxel) geometry occupies: the outer
     * corner heights its stat emits. Like {@link #expandSolidDomain}, this lets
     * the cube floor and ceiling sit flush against the extreme corners — a
     * column rooted at a {@code zmin} below the data minimum would otherwise
     * poke through the floor of the cube.
     *
     * @param zBounds the raw z domain
     * @param ctx     the plot-scoped context
     * @return the z corner range, or {@code zBounds} when the stat cannot run
     */
    public MinMax expandSolidZ(MinMax zBounds, PlotContext<DF> ctx) {
        var statData = solidStat(ctx);
        if (statData == null) {
            return zBounds;
        }
        var zs = statData.columnAsDoubles("z");
        if (zs == null) {
            return zBounds;
        }
        double zMin = Double.POSITIVE_INFINITY, zMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < statData.rowCount(); i++) {
            double z = zs.get(i);
            if (Double.isFinite(z)) {
                zMin = Math.min(zMin, z);
                zMax = Math.max(zMax, z);
            }
        }
        if (zMin > zMax) {
            return zBounds;
        }
        return new MinMax(zMin, zMax);
    }

    /**
     * Turns the stat output rows into data-space polygons. The base groups the
     * stat rows by their {@code group} column — the identity behaviour that
     * draws pre-polygonized output such as
     * {@link org.jtaccuino.gog.stat.Stats#smooth3d()}'s panels.
     * Subclasses override this to tessellate point rows (tiles, ridgelines,
     * contours).
     *
     * @param statData the stat output
     * @param ctx      the panel-scoped context
     * @return the polygon rings to draw
     */
    protected List<Poly3d> polygonize(StatData statData, PanelContext<DF> ctx) {
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        int n = statData.rowCount();
        var groups = new LinkedHashMap<String, List<Integer>>();
        for (int i = 0; i < n; i++) {
            if (isPolygonRow(statData, i)) {
                groups.computeIfAbsent(String.valueOf(gCol.get(i)), k -> new ArrayList<>()).add(i);
            }
        }
        var out = new ArrayList<Poly3d>();
        for (var e : groups.entrySet()) {
            var rows = e.getValue();
            int m = rows.size();
            var xs = new double[m];
            var ys = new double[m];
            var zs = new double[m];
            var src = new int[m];
            for (int k = 0; k < m; k++) {
                int r = rows.get(k);
                xs[k] = Values.toDouble(xCol.get(r));
                ys[k] = Values.toDouble(yCol.get(r));
                zs[k] = Values.toDouble(zCol.get(r));
                src[k] = r;
            }
            out.add(new Poly3d(xs, ys, zs, e.getKey(), src));
        }
        return out;
    }

    /** {@return whether a stat row belongs to a filled polygon primitive} */
    private static boolean isPolygonRow(StatData statData, int i) {
        var prim = statData.column("prim");
        return prim == null || "polygon".equals(prim.get(i));
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var plot = ctx.plot();
        var aes = plot.aes();
        var coord = plot.coord();
        if (!(coord instanceof Coord3D coord3d)) {
            return;
        }
        var statData = statData(ctx);
        if (statData == null) {
            return;
        }
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        if (xCol == null || yCol == null || zCol == null || gCol == null) {
            return;
        }

        var light = effectiveLight(ctx);
        boolean shade = light != null && light.method() != Light3d.Method.NONE;
        boolean backfaceShade = light != null
                && light.method() != Light3d.Method.RGB
                && (light.backfaceScale() != 1 || light.backfaceOffset() != 0);
        double backfaceSign = (spec.isCullBackfaces() || backfaceShade)
                ? backfaceReferenceSign(coord3d) : 0;

        var bounds = statBounds(statData);
        var points = new ArrayList<Vertex3D>();
        var segments = new ArrayList<Vertex3D>();
        collectPrimRows(statData, points, segments);
        var elements = new ArrayList<Element>();
        var lightings = new ArrayList<Lighting>();
        var falloffs = new ArrayList<Falloff>();
        var cube = CubeStyle.from(plot.theme());

        buildPolygonElements(ctx, statData, aes, coord3d, light, shade, backfaceShade,
                backfaceSign, bounds, elements, lightings, falloffs);
        buildPointElements(coord3d, points, elements, lightings);
        buildSegmentElements(coord3d, segments, elements, lightings);

        if (elements.isEmpty()) {
            return;
        }

        if (light != null && shade) {
            finalizeLight(light, lightings, falloffs);
        }

        int[] order = renderOrder(elements, spec.getSortMethod());

        var fallback = plot.theme().fallbackColor();
        double depthStrength = cube.depthScaleStrength();
        for (int oi : order) {
            var el = elements.get(oi);
            double depthFactor = spec.isScaleDepth()
                    ? CubeStyle.depthFactor(el.nearness(), depthStrength)
                    : 1.0;
            switch (el) {
                case PolygonElement pe -> drawPolygon(gc, pe, light, fallback, depthFactor,
                        lightings.get(oi));
                case PointElement pt -> drawPoint(gc, pt, fallback, depthFactor);
                case SegmentElement se -> drawSegment(gc, se, fallback, depthFactor);
            }
        }
    }

    private void buildPolygonElements(PanelContext<DF> ctx, StatData statData, Aes aes,
            Coord3D coord3d, Light3d light, boolean shade, boolean backfaceShade,
            double backfaceSign, Bounds bounds, List<Element> elements,
            List<Lighting> lightings, List<Falloff> falloffs) {
        for (var p : polygonize(statData, ctx)) {
            double[][] proj = projectLoop(coord3d, p);
            if (spec.isCullBackfaces() && Math.signum(proj[2][0]) != 0
                    && backfaceSign != 0 && Math.signum(signedArea(proj[0], proj[1])) != backfaceSign) {
                continue;
            }
            int[] keep = spec.isForceConvex()
                    ? PolygonMath.convexHull(proj[0], proj[1])
                    : null;
            double[] sx = proj[0];
            double[] sy = proj[1];
            double[] depth = proj[2];
            if (keep != null && keep.length >= 3) {
                sx = pick(sx, keep);
                sy = pick(sy, keep);
                depth = pick(depth, keep);
            }
            double nearness = mean(depth);
            double[] normal = shade && p.n() >= 3
                    ? polygonNormal(p, bounds)
                    : null;
            var lighting = normal != null && light != null
                    ? lightingForPolygon(coord3d, light, p, sx, sy, normal, backfaceShade,
                            backfaceSign, elements.size(), falloffs)
                    : Lighting.none();
            elements.add(new PolygonElement(sx, sy, depth, nearness, p.group(),
                    resolveFill(ctx, statData, aes, p),
                    resolveStroke(ctx, statData, aes, p),
                    spec.getLinewidth(), spec.getAlpha(), normal));
            lightings.add(lighting);
        }
    }

    private void buildPointElements(Coord3D coord3d, List<Vertex3D> points,
            List<Element> elements, List<Lighting> lightings) {
        for (var p : points) {
            var pr = coord3d.projectData(p.x(), p.y(), p.z());
            double size = spec.getPointSize() != null ? spec.getPointSize() : 1.0;
            elements.add(new PointElement(pr.sx(), pr.sy(), pr.depthScale(), pr.depthScale(),
                    p.group(), pointFill(), pointStroke(), spec.getLinewidth(),
                    spec.getPointAlpha() != null ? spec.getPointAlpha() : spec.getAlpha(),
                    size));
            lightings.add(Lighting.none());
        }
    }

    private void buildSegmentElements(Coord3D coord3d, List<Vertex3D> segments,
            List<Element> elements, List<Lighting> lightings) {
        for (int i = 0; i + 1 < segments.size(); i += 2) {
            var a = segments.get(i);
            var b = segments.get(i + 1);
            var pa = coord3d.projectData(a.x(), a.y(), a.z());
            var pb = coord3d.projectData(b.x(), b.y(), b.z());
            double nearness = (pa.depthScale() + pb.depthScale()) / 2.0;
            elements.add(new SegmentElement(pa.sx(), pa.sy(), pa.depthScale(),
                    pb.sx(), pb.sy(), pb.depthScale(), nearness, a.group(),
                    null, residualStroke(), spec.getResidualLinewidth(),
                    spec.getResidualAlpha() != null ? spec.getResidualAlpha() : spec.getAlpha()));
            lightings.add(Lighting.none());
        }
    }

    private void collectPrimRows(StatData statData, List<Vertex3D> points, List<Vertex3D> segments) {
        var prim = statData.column("prim");
        if (prim == null) {
            return;
        }
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        for (int i = 0; i < statData.rowCount(); i++) {
            var kind = prim.get(i);
            if ("point".equals(kind)) {
                points.add(new Vertex3D(Values.toDouble(xCol.get(i)), Values.toDouble(yCol.get(i)),
                        Values.toDouble(zCol.get(i)), String.valueOf(gCol.get(i)), i));
            } else if ("segment".equals(kind)) {
                segments.add(new Vertex3D(Values.toDouble(xCol.get(i)), Values.toDouble(yCol.get(i)),
                        Values.toDouble(zCol.get(i)), String.valueOf(gCol.get(i)), i));
            }
        }
        segments.sort((a, b) -> a.group().compareTo(b.group()));
    }

    /** Projects a data-space polygon, returning parallel screen/depth arrays. */
    private static double[][] projectLoop(Coord3D coord3d, Poly3d p) {
        int m = p.n();
        var sx = new double[m];
        var sy = new double[m];
        var depth = new double[m];
        for (int k = 0; k < m; k++) {
            var pr = coord3d.projectData(p.x()[k], p.y()[k], p.z()[k]);
            sx[k] = pr.sx();
            sy[k] = pr.sy();
            depth[k] = pr.depthScale();
        }
        return new double[][] {sx, sy, depth};
    }

    /**
     * The projected winding sign of a front-facing reference square: the bottom
     * face of the cube (a horizontal x–y square) as seen from above.
     */
    private static double backfaceReferenceSign(Coord3D coord3d) {
        double x0 = coord3d.faceCoordinate(CubeFace.XMIN);
        double x1 = coord3d.faceCoordinate(CubeFace.XMAX);
        double y0 = coord3d.faceCoordinate(CubeFace.YMIN);
        double y1 = coord3d.faceCoordinate(CubeFace.YMAX);
        double z0 = coord3d.faceCoordinate(CubeFace.ZMIN);
        var a = coord3d.projectData(x0, y0, z0);
        var b = coord3d.projectData(x1, y0, z0);
        var c = coord3d.projectData(x1, y1, z0);
        var d = coord3d.projectData(x0, y1, z0);
        return Math.signum(signedArea(new double[] {a.sx(), b.sx(), c.sx(), d.sx()},
                new double[] {a.sy(), b.sy(), c.sy(), d.sy()}));
    }

    private static double signedArea(double[] sx, double[] sy) {
        double area = 0;
        for (int i = 0; i < sx.length; i++) {
            int j = (i + 1) % sx.length;
            area += sx[i] * sy[j] - sx[j] * sy[i];
        }
        return area;
    }

    private static double[] pick(double[] src, int[] idx) {
        var out = new double[idx.length];
        for (int i = 0; i < idx.length; i++) {
            out[i] = src[idx[i]];
        }
        return out;
    }

    private static double mean(double[] v) {
        double s = 0;
        for (double d : v) {
            s += d;
        }
        return v.length == 0 ? 0 : s / v.length;
    }

    /** The data bounds across all stat rows, for normalising light shading. */
    private record Bounds(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
    }

    private static Bounds statBounds(StatData statData) {
        var xs = statData.columnAsDoubles("x");
        var ys = statData.columnAsDoubles("y");
        var zs = statData.columnAsDoubles("z");
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY, zMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < xs.size(); i++) {
            double x = xs.get(i), y = ys.get(i), z = zs.get(i);
            if (Double.isFinite(x)) { xMin = Math.min(xMin, x); xMax = Math.max(xMax, x); }
            if (Double.isFinite(y)) { yMin = Math.min(yMin, y); yMax = Math.max(yMax, y); }
            if (Double.isFinite(z)) { zMin = Math.min(zMin, z); zMax = Math.max(zMax, z); }
        }
        return new Bounds(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    private int[] renderOrder(List<Element> elements, Polygon3dSpec.SortMethod sortMethod) {
        var method = sortMethod;
        if (method == Polygon3dSpec.SortMethod.AUTO) {
            // Pairwise below 500 primitives, painter's algorithm above.
            method = elements.size() <= 500 ? Polygon3dSpec.SortMethod.PAIRWISE
                    : Polygon3dSpec.SortMethod.PAINTER;
        }
        if (method == Polygon3dSpec.SortMethod.PAIRWISE) {
            // The elements store nearness (depthScale: larger = nearer), but
            // the sorter expects larger = farther.  fromNearness() negates to
            // match the sorter's convention.
            var prims = new ArrayList<Pairwise3dSorter.Primitive>(elements.size());
            for (var el : elements) {
                prims.add(switch (el) {
                    case PolygonElement pe -> Pairwise3dSorter.PrimitivePolygon.fromNearness(
                            pe.screenX(), pe.screenY(), pe.depthScale());
                    case SegmentElement se -> Pairwise3dSorter.PrimitiveSegment.fromNearness(
                            se.startX(), se.startY(), se.startDepthScale(),
                            se.endX(), se.endY(), se.endDepthScale());
                    case PointElement pt -> Pairwise3dSorter.PrimitivePoint.fromNearness(
                            pt.x(), pt.y(), pt.depthScale());
                });
            }
            return Pairwise3dSorter.sort(prims);
        }
        var groups = new String[elements.size()];
        var nearness = new double[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            var el = elements.get(i);
            groups[i] = el instanceof PointElement ? "pt" + i : el.group();
            nearness[i] = el.nearness();
        }
        return DepthSorter.order(groups, nearness);
    }

    private void drawPolygon(DrawSurface gc, PolygonElement el, Light3d light, Color fallback,
            double depthFactor, Lighting lighting) {
        Color fill = el.fill() != null ? el.fill() : fallback;
        fill = applyLight(fill, el, light, true, lighting);
        if (el.alpha() < 1.0) {
            fill = Colors.withAlpha(fill, el.alpha());
        }
        gc.setFill(fill);
        gc.fillPolygon(el.screenX(), el.screenY(), el.screenX().length);
        if (el.stroke() != null) {
            var stroke = applyLight(el.stroke(), el, light, false, lighting);
            if (el.alpha() < 1.0) {
                stroke = Colors.withAlpha(stroke, el.alpha());
            }
            gc.setStroke(stroke);
            gc.setLineWidth(Math.max(0, el.linewidth() * depthFactor));
            gc.setLineDashes(spec.getLinetype());
            gc.strokePolygon(el.screenX(), el.screenY(), el.screenX().length);
            gc.setLineDashes((double[]) null);
        } else {
            gc.setStroke(fill);
            gc.setLineWidth(SEAM_HIDE_WIDTH);
            gc.strokePolygon(el.screenX(), el.screenY(), el.screenX().length);
        }
    }

    private void drawPoint(DrawSurface gc, PointElement el, Color fallback, double depthFactor) {
        var shape = spec.getPointShape() != null ? spec.getPointShape() : PointShape.CIRCLE;
        var fill = el.fill() != null ? el.fill() : fallback;
        if (el.alpha() < 1.0) {
            fill = Colors.withAlpha(fill, el.alpha());
        }
        double radius = Math.max(0.5, el.pointSize() / 2.0) * depthFactor;
        gc.setFill(fill);
        if (el.stroke() != null) {
            gc.setStroke(el.stroke());
            gc.setLineWidth(Math.max(0, spec.getPointStroke() * depthFactor));
        }
        shape.draw(gc, el.x(), el.y(), radius);
    }

    private void drawSegment(DrawSurface gc, SegmentElement el, Color fallback, double depthFactor) {
        var stroke = el.stroke() != null ? el.stroke() : fallback;
        if (el.alpha() < 1.0) {
            stroke = Colors.withAlpha(stroke, el.alpha());
        }
        gc.setStroke(stroke);
        gc.setLineWidth(Math.max(0, el.linewidth() * depthFactor));
        gc.setLineDashes(spec.getResidualLinetype());
        gc.strokeLine(el.startX(), el.startY(), el.endX(), el.endY());
        gc.setLineDashes((double[]) null);
    }

    /**
     * The per-polygon fill colour: a mapped {@code fill} aesthetic (resolved
     * against the scales over the averaged per-vertex values), then the layer's
     * constant fill default, then the spec's fill.
     */
    private Color resolveFill(PanelContext<DF> ctx, StatData statData, Aes aes, Poly3d p) {
        var scales = ctx.plot().scales();
        if (aes.fill() != null) {
            var value = polygonValue(statData, Aes.statColumn(aes.fill()), p);
            if (value != null) {
                var c = scales.resolvedColorFor(aes.fill(), value);
                if (c != null) {
                    return c;
                }
            }
        }
        if (attachedParams.fill() != null) {
            return attachedParams.fill();
        }
        return spec.getFill();
    }

    /** The per-polygon stroke colour: mapped {@code color}, then params, then spec. */
    private Color resolveStroke(PanelContext<DF> ctx, StatData statData, Aes aes, Poly3d p) {
        var scales = ctx.plot().scales();
        if (aes.color() != null) {
            var value = polygonValue(statData, Aes.statColumn(aes.color()), p);
            if (value != null) {
                var c = scales.resolvedColorFor(aes.color(), value);
                if (c != null) {
                    return c;
                }
            }
        }
        if (attachedParams.color() != null) {
            return attachedParams.color();
        }
        return spec.getColour();
    }

    private Color pointFill() {
        if (spec.getPointFill() != null) {
            return spec.getPointFill();
        }
        if (spec.getPointColour() != null) {
            return spec.getPointColour();
        }
        if (attachedParams.fill() != null) {
            return attachedParams.fill();
        }
        return spec.getFill();
    }

    private Color pointStroke() {
        if (spec.getPointColour() != null) {
            return spec.getPointColour();
        }
        return spec.getColour();
    }

    private Color residualStroke() {
        if (spec.getResidualColour() != null) {
            return spec.getResidualColour();
        }
        return spec.getColour();
    }

    /**
     * The average (numeric) or first (other) value of a stat column over a
     * polygon's source rows; when a polygon carries no source rows the loop's
     * own z values stand in for the {@code z} column.
     */
    private static Object polygonValue(StatData statData, String column, Poly3d p) {
        var col = statData.column(column);
        if (col == null) {
            return null;
        }
        if (p.srcRow() != null) {
            double sum = 0;
            int count = 0;
            Object first = null;
            for (int r : p.srcRow()) {
                var v = col.get(r);
                if (first == null && v != null) {
                    first = v;
                }
                if (v instanceof Number num) {
                    sum += num.doubleValue();
                    count++;
                }
            }
            if (count > 0) {
                return sum / count;
            }
            return first;
        }
        if ("z".equals(column)) {
            return mean(p.z());
        }
        return null;
    }

    /**
     * The outward normal of a data-space polygon, normalised by the stat data
     * bounds so the cube's proportions are factored out.
     */

    private static double[] polygonNormal(Poly3d p, Bounds b) {
        int m = p.n();
        var v = new double[m][3];
        for (int i = 0; i < m; i++) {
            v[i][0] = (p.x()[i] - b.xMin()) / span(b.xMax() - b.xMin());
            v[i][1] = (p.y()[i] - b.yMin()) / span(b.yMax() - b.yMin());
            v[i][2] = (p.z()[i] - b.zMin()) / span(b.zMax() - b.zMin());
        }
        double ax = v[1][0] - v[0][0], ay = v[1][1] - v[0][1], az = v[1][2] - v[0][2];
        double bx = v[2][0] - v[0][0], by = v[2][1] - v[0][1], bz = v[2][2] - v[0][2];
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-12) {
            return null;
        }
        return new double[] {nx / len, ny / len, nz / len};
    }

    private static double span(double v) {
        return Math.abs(v) < 1e-12 ? 1.0 : v;
    }

    /**
     * Resolves the shading of one polygon: an RGB light produces a replacement
     * colour, a positional light with falloff records a pending {@link Falloff}
     * (resolved once all distances are known), and everything else computes the
     * light value and backface flag directly.
     */
    private Lighting lightingForPolygon(Coord3D coord3d, Light3d l3, Poly3d p,
            double[] sx, double[] sy, double[] normal, boolean backfaceShade,
            double backfaceSign, int elementIndex, List<Falloff> falloffs) {
        double[] n = l3.anchor() == Light3d.Anchor.CAMERA && l3.position() == null
                ? coord3d.rotateNormalToCamera(normal)
                : normal;
        double[] dir = l3.direction();
        if (l3.method() == Light3d.Method.RGB) {
            return new Lighting(Double.NaN, computeRgbLight(n, dir), false);
        }
        double dot;
        if (l3.position() != null) {
            double cx = mean(p.x()), cy = mean(p.y()), cz = mean(p.z());
            double vx = l3.position()[0] - cx;
            double vy = l3.position()[1] - cy;
            double vz = l3.position()[2] - cz;
            double dist = Math.sqrt(vx * vx + vy * vy + vz * vz);
            dot = dist > 0 ? (vx * dir[0] + vy * dir[1] + vz * dir[2]) / dist : 0;
            if (l3.distanceFalloff() && dist > 0) {
                falloffs.add(new Falloff(elementIndex, dot, dist));
                return Lighting.none();
            }
        } else {
            dot = n[0] * dir[0] + n[1] * dir[1] + n[2] * dir[2];
        }
        double lightL = l3.method() == Light3d.Method.DIRECT ? Math.max(0, dot) : dot;
        boolean backface = backfaceShade && backfaceSign != 0
                && Math.signum(signedArea(sx, sy)) != backfaceSign;
        return new Lighting(lightL, null, backface);
    }

    /**
     * The effective light for the current layer, resolved in this order: the
     * layer-level spec light, else the coord- or plot-level light, else
     * the spec's default light ({@link Polygon3dSpec#getDefaultLight()}), else
     * {@code null} for flat rendering. A layer light of {@code null} inherits
     * the level below.
     */
    private Light3d effectiveLight(PanelContext<DF> ctx) {
        if (spec.getLight() != null) {
            return spec.getLight();
        }
        Light3d coordLight = null;
        if (ctx.plot().coord() instanceof Coord3D coord3d) {
            coordLight = coord3d.getLight();
        }
        Light3d plotLight = ctx.plot().light();
        if (coordLight != null && plotLight != null) {
            throw new IllegalArgumentException(
                    "supply the 3D light on either coord3d(light=...) or Plot.light(...), not both");
        }
        Light3d inherited = coordLight != null ? coordLight : plotLight;
        return inherited != null ? inherited : spec.getDefaultLight();
    }

    /**
     * Finalises the per-polygon light values: positional lights with distance
     * falloff need the maximum inverse-square distance across all faces, and a
     * backface {@code backface_scale}/{@code backface_offset} pair modifies the
     * backfacing polygons' values once they are complete.
     */
    private void finalizeLight(Light3d light, List<Lighting> lightings, List<Falloff> falloffs) {
        double maxS = -1;
        for (var f : falloffs) {
            maxS = Math.max(maxS, 1 / (f.dist() * f.dist()));
        }
        for (var f : falloffs) {
            double falloff = maxS > 0 ? (1 / (f.dist() * f.dist())) / maxS : 0;
            double l = light.method() == Light3d.Method.DIRECT
                    ? f.dot() * falloff
                    : -1 + falloff * (f.dot() + 1);
            lightings.set(f.elementIndex(), new Lighting(l, null, false));
        }
        boolean backfaceShade = light.method() != Light3d.Method.RGB
                && (light.backfaceScale() != 1 || light.backfaceOffset() != 0);
        if (!backfaceShade) {
            return;
        }
        for (int i = 0; i < lightings.size(); i++) {
            var li = lightings.get(i);
            if (li.backface() && !Double.isNaN(li.lightL())) {
                lightings.set(i, new Lighting(
                        li.lightL() * light.backfaceScale() + light.backfaceOffset(),
                        li.rgbLight(), true));
            }
        }
    }

    /**
     * The RGB light method: rotates each normal so the neutral
     * {@code (1, 1, 1)/√3} direction aligns with the light, then maps the
     * channels to {@code [0, 1]} colours that replace the fill and stroke.
     */
    private static Color computeRgbLight(double[] normal, double[] dir) {
        double w = 1 / Math.sqrt(3);
        double[][] r = VectorMath.rotationAligning(new double[] {w, w, w}, dir);
        double nx = r[0][0] * normal[0] + r[0][1] * normal[1] + r[0][2] * normal[2];
        double ny = r[1][0] * normal[0] + r[1][1] * normal[1] + r[1][2] * normal[2];
        double nz = r[2][0] * normal[0] + r[2][1] * normal[1] + r[2][2] * normal[2];
        return new Color((nx + 1) / 2, (ny + 1) / 2, (nz + 1) / 2, 1);
    }

    /**
     * Shading: the polygon's light value is normalised ({@code diffuse} lights
     * spread over {@code [0, 1]}) and blended into the base colour at
     * {@code contrast} strength — brightening the value/lightness channel
     * toward 1 above 0.5, darkening toward 0 below, preserving hue and
     * saturation. An {@code rgb} light {@link Light3d.Method#RGB} replaces the
     * colour outright. The {@code fill}/{@code color} targets follow the
     * light's flags.
     */
    private Color applyLight(Color base, PolygonElement el, Light3d light, boolean fillTarget,
            Lighting lighting) {
        if (light == null || el.normal() == null) {
            return base;
        }
        // The rgb method substitutes a derived colour and never touches lightL.
        if (lighting.rgbLight() != null) {
            return lighting.rgbLight();
        }
        // A face without a resolved light value (e.g. positional lights pending
        // falloff) renders unshaded.
        if (Double.isNaN(lighting.lightL())) {
            return base;
        }
        boolean shade = fillTarget ? light.fill() : light.color();
        if (!shade || light.method() == Light3d.Method.NONE) {
            return base;
        }
        double lN = light.method() == Light3d.Method.DIFFUSE
                ? (lighting.lightL() + 1.0) / 2.0
                : lighting.lightL();
        lN = clamp(lN);
        // Light contrast blend: above 0.5 brighten toward 1, below
        // darken toward 0, by (|lN − 0.5| × 2 × contrast).
        double blend = (lN - 0.5) * 2.0 * light.contrast();
        if (light.mode() == Light3d.Mode.HSL) {
            double[] hsl = rgbToHsl(base);
            if (blend >= 0) {
                hsl[2] += blend * (1.0 - hsl[2]);
            } else {
                hsl[2] *= 1.0 + blend;
            }
            return hslToRgb(hsl[0], hsl[1], clamp(hsl[2]), base.getOpacity());
        }
        double brightness = base.getBrightness();
        if (blend >= 0) {
            brightness += blend * (1.0 - brightness);
        } else {
            brightness *= 1.0 + blend;
        }
        return Color.hsb(base.getHue(), base.getSaturation(), clamp(brightness),
                base.getOpacity());
    }

    /** {@return the base colour in standard {@code (h, s, l)} terms, hue in degrees} */
    private static double[] rgbToHsl(Color c) {
        double r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        if (max == min) {
            return new double[] {0, 0, l};
        }
        double d = max - min;
        double s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        double h;
        if (max == r) {
            h = (g - b) / d + (g < b ? 6 : 0);
        } else if (max == g) {
            h = (b - r) / d + 2;
        } else {
            h = (r - g) / d + 4;
        }
        return new double[] {h * 60, s, l};
    }

    private static Color hslToRgb(double h, double s, double l, double opacity) {
        if (s == 0) {
            return new Color(l, l, l, opacity);
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        double hk = h / 360;
        return new Color(clamp(hueChannel(p, q, hk + 1 / 3.0)),
                clamp(hueChannel(p, q, hk)),
                clamp(hueChannel(p, q, hk - 1 / 3.0)), opacity);
    }

    private static double hueChannel(double p, double q, double t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1 / 6.0) {
            return p + (q - p) * 6 * t;
        }
        if (t < 1 / 2.0) {
            return q;
        }
        if (t < 2 / 3.0) {
            return p + (q - p) * (2 / 3.0 - t) * 6;
        }
        return p;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var statData = statData(ctx);
        if (statData == null) {
            return null;
        }
        var coord = ctx.plot().coord();
        if (!(coord instanceof Coord3D coord3d)) {
            return null;
        }
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        if (xCol == null || yCol == null || zCol == null || gCol == null) {
            return null;
        }
        var polygons = polygonize(statData, ctx);
        Poly3d hit = null;
        double bestNearness = Double.NEGATIVE_INFINITY;
        for (var p : polygons) {
            double[][] proj = projectLoop(coord3d, p);
            if (PolygonMath.pointInPolygon(mx, my, proj[0], proj[1])) {
                double nearness = mean(proj[2]);
                if (nearness > bestNearness) {
                    bestNearness = nearness;
                    hit = p;
                }
            }
        }
        if (hit == null) {
            return null;
        }
        var labelX = Values.label(mean(hit.x()));
        var labelY = Values.label(mean(hit.y()));
        var labelZ = Values.label(mean(hit.z()));
        var group = userGroup(hit.group());
        return group == null
                ? Tooltip3d.position(labelX, labelY, labelZ)
                : Tooltip3d.position(group, labelX, labelY, labelZ);
    }

    /**
     * {@return the data-space group label of a hovered polygon, or {@code null}
     * when the stat's group value is an internal mesh identifier (a cube-face,
     * tile, voxel, or triangle id) rather than a user-supplied data group}.
     *
     * @param group the raw {@code group} value from the stat output
     */
    private static String userGroup(String group) {
        if (group == null || group.isEmpty() || group.equals("-1") || group.contains("__")) {
            return null;
        }
        return group;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        int n = ctx.plot().extractor().getRowCount(ctx.partitionDf());
        return Math.max(1, n / 2);
    }
}
