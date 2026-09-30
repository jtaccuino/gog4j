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
package org.jtaccuino.gog;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.layer.AblineConfigurator;
import org.jtaccuino.gog.layer.AreaConfigurator;
import org.jtaccuino.gog.layer.Bar3dConfigurator;
import org.jtaccuino.gog.layer.BarConfigurator;
import org.jtaccuino.gog.layer.BoxplotConfigurator;
import org.jtaccuino.gog.layer.Col3dConfigurator;
import org.jtaccuino.gog.layer.ColConfigurator;
import org.jtaccuino.gog.layer.Contour3dConfigurator;
import org.jtaccuino.gog.layer.CrossbarConfigurator;
import org.jtaccuino.gog.layer.Density2dConfigurator;
import org.jtaccuino.gog.layer.Density3dConfigurator;
import org.jtaccuino.gog.layer.DensityConfigurator;
import org.jtaccuino.gog.layer.ErrorbarConfigurator;
import org.jtaccuino.gog.layer.ErrorbarhConfigurator;
import org.jtaccuino.gog.layer.FreqpolyConfigurator;
import org.jtaccuino.gog.layer.Function3dConfigurator;
import org.jtaccuino.gog.layer.FunctionConfigurator;
import org.jtaccuino.gog.layer.GeomAbline;
import org.jtaccuino.gog.layer.GeomArea;
import org.jtaccuino.gog.layer.GeomBar;
import org.jtaccuino.gog.layer.GeomBar3d;
import org.jtaccuino.gog.layer.GeomBoxplot;
import org.jtaccuino.gog.layer.GeomCol;
import org.jtaccuino.gog.layer.GeomCol3d;
import org.jtaccuino.gog.layer.GeomContour3d;
import org.jtaccuino.gog.layer.GeomCrossbar;
import org.jtaccuino.gog.layer.GeomCurve;
import org.jtaccuino.gog.layer.GeomDensity;
import org.jtaccuino.gog.layer.GeomDensity2d;
import org.jtaccuino.gog.layer.GeomErrorbar;
import org.jtaccuino.gog.layer.GeomErrorbarh;
import org.jtaccuino.gog.layer.GeomFreqpoly;
import org.jtaccuino.gog.layer.GeomFunction;
import org.jtaccuino.gog.layer.GeomHistogram;
import org.jtaccuino.gog.layer.GeomHline;
import org.jtaccuino.gog.layer.GeomHull3d;
import org.jtaccuino.gog.layer.GeomJitter;
import org.jtaccuino.gog.layer.GeomLine;
import org.jtaccuino.gog.layer.GeomLinerange;
import org.jtaccuino.gog.layer.GeomNone;
import org.jtaccuino.gog.layer.GeomPath;
import org.jtaccuino.gog.layer.GeomPath3d;
import org.jtaccuino.gog.layer.GeomPoint;
import org.jtaccuino.gog.layer.GeomPoint3d;
import org.jtaccuino.gog.layer.GeomPointrange;
import org.jtaccuino.gog.layer.GeomPolygon;
import org.jtaccuino.gog.layer.GeomPolygon3d;
import org.jtaccuino.gog.layer.GeomRibbon;
import org.jtaccuino.gog.layer.GeomRidgeline3d;
import org.jtaccuino.gog.layer.GeomSegment;
import org.jtaccuino.gog.layer.GeomSegment3d;
import org.jtaccuino.gog.layer.GeomSmooth;
import org.jtaccuino.gog.layer.GeomSmooth3d;
import org.jtaccuino.gog.layer.GeomStep;
import org.jtaccuino.gog.layer.GeomSurface3d;
import org.jtaccuino.gog.layer.GeomText;
import org.jtaccuino.gog.layer.GeomText3d;
import org.jtaccuino.gog.layer.GeomTile;
import org.jtaccuino.gog.layer.GeomViolin;
import org.jtaccuino.gog.layer.GeomVline;
import org.jtaccuino.gog.layer.GeomVoxel3d;
import org.jtaccuino.gog.layer.HistogramConfigurator;
import org.jtaccuino.gog.layer.HlineConfigurator;
import org.jtaccuino.gog.layer.Hull3dConfigurator;
import org.jtaccuino.gog.layer.JitterConfigurator;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.layer.LinerangeConfigurator;
import org.jtaccuino.gog.layer.Point3dConfigurator;
import org.jtaccuino.gog.layer.Point3dSpec;
import org.jtaccuino.gog.layer.PointConfigurator;
import org.jtaccuino.gog.layer.PointSpec;
import org.jtaccuino.gog.layer.PointrangeConfigurator;
import org.jtaccuino.gog.layer.Polygon3dConfigurator;
import org.jtaccuino.gog.layer.Polygon3dSpec;
import org.jtaccuino.gog.layer.PolygonConfigurator;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.layer.PositionAdjust;
import org.jtaccuino.gog.layer.RibbonConfigurator;
import org.jtaccuino.gog.layer.Ridgeline3dConfigurator;
import org.jtaccuino.gog.layer.Segment3dConfigurator;
import org.jtaccuino.gog.layer.Smooth3dConfigurator;
import org.jtaccuino.gog.layer.SmoothConfigurator;
import org.jtaccuino.gog.layer.Surface3dConfigurator;
import org.jtaccuino.gog.layer.Text3dConfigurator;
import org.jtaccuino.gog.layer.TextConfigurator;
import org.jtaccuino.gog.layer.TileConfigurator;
import org.jtaccuino.gog.layer.ViolinConfigurator;
import org.jtaccuino.gog.layer.VlineConfigurator;
import org.jtaccuino.gog.layer.Voxel3dConfigurator;
import org.jtaccuino.gog.stat.Averaging;
import org.jtaccuino.gog.stat.SmoothMethod;
import org.jtaccuino.gog.stat.StatCor;
import org.jtaccuino.gog.stat.Stats;

/**
 * Factory class providing static builder methods for creating plot geometries (geoms).
 * <p>
 * Supports scatter plots ({@link #point()}), trendlines ({@link #smooth()}),
 * line charts ({@link #line()}), bar charts ({@link #bar()}), column charts
 * ({@link #col()}), area charts ({@link #area()}),
 * box-and-whiskers ({@link #boxplot()}), violin plots ({@link #violin()}),
 * kernel-density estimates ({@link #density()}), and mathematical curves
 * ({@link #function(java.util.function.DoubleUnaryOperator)}), plus 2D density
 * contours ({@link #density2d()}).
 */
public class Geoms {
    /** Utility class; not meant to be instantiated. */
    private Geoms() {}

    /**
     * Creates an explicitly blank {@code Geoms.none()} layer that renders
     * nothing.
     * <p>
     * A {@code PlotMatrix} cell whose layers are all {@code GeomNone} (or that
     * carries no layers at all) reserves its grid slot without drawing a
     * panel, so e.g. a mixed-type matrix can leave triangular regions empty
     * while the surrounding cells keep their aligned frame.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} registering the empty layer
     */
    public static <DF> LayerConfigurator<DF> none() {
        return plot -> plot.registerInternalLayer(new GeomNone<DF>());
    }

    /**
     * Creates a reactive {@code Geoms.point()} scatter plot layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return a {@link PointConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> PointConfigurator<DF> point() {
        var baseGeom = new GeomPoint<Object>();
        var spec = new PointSpec();
        return new PointConfigurator<>((GeomPoint<DF>) baseGeom, spec);
    }

    /**
     * Creates a {@code Geoms.point()} scatter plot layer with fixed color and point size.
     *
     * @param <DF> the DataFrame type
     * @param color the point fill and stroke color
     * @param size the point diameter in pixels
     * @return a {@link PointConfigurator} instance for fluid method chaining
     */
    public static <DF> PointConfigurator<DF> point(Color color, double size) {
        return Geoms.<DF>point().color(color).size(size);
    }

    /**
     * Creates a {@code Geoms.jitter()} jittered scatter layer configurator.
     * <p>
     * A convenient shortcut for {@code Geoms.point(position = "jitter")}: adds a small
     * amount of random variation to the location of each point to handle overplotting
     * (e.g. overlaying raw observations on a boxplot). Jitter offsets are deterministic
     * per data row and seeded, so rendering is stable across repaints.
     *
     * @param <DF> the DataFrame type
     * @return a {@link JitterConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> JitterConfigurator<DF> jitter() {
        var baseGeom = new GeomJitter<Object>();
        var spec = new PointSpec();
        return new JitterConfigurator<>((GeomJitter<DF>) baseGeom, spec);
    }

    /**
     * Creates a reactive {@code Geoms.smooth()} statistical trendline layer configurator.
     * Defaults to LOESS smoothing with a confidence interval ribbon (SE = true).
     *
     * @param <DF> the DataFrame type
     * @return a {@link SmoothConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> SmoothConfigurator<DF> smooth() {
        var baseGeom = new GeomSmooth<Object>();
        return new SmoothConfigurator<>((GeomSmooth<DF>) baseGeom).se(true).method(SmoothMethod.LOESS);
    }

    /**
     * Creates a {@code Geoms.smooth()} LOESS trendline layer with a specified bandwidth (span).
     *
     * @param <DF> the DataFrame type
     * @param span the LOESS bandwidth parameter (e.g., 0.3 for fine-grained, 0.75 default)
     * @return a {@link SmoothConfigurator} instance for fluid method chaining
     */
    public static <DF> SmoothConfigurator<DF> smooth(double span) {
        return Geoms.<DF>smooth().span(span);
    }

    /**
     * Creates a {@code Geoms.smooth()} trendline layer with a specific smoothing method and confidence interval flag.
     *
     * @param <DF> the DataFrame type
     * @param method the smoothing model ({@link SmoothMethod#LOESS} or {@link SmoothMethod#LM})
     * @param se {@code true} to render the standard error confidence interval ribbon, {@code false} otherwise
     * @return a {@link SmoothConfigurator} instance for fluid method chaining
     */
    public static <DF> SmoothConfigurator<DF> smooth(SmoothMethod method, boolean se) {
        return Geoms.<DF>smooth().method(method).se(se);
    }

    /**
     * Creates a {@code Geoms.smooth()} trendline layer with local aesthetic mappings overriding global mappings.
     *
     * @param <DF> the DataFrame type
     * @param localAes local aesthetic mapping for this smooth layer
     * @return a {@link SmoothConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> SmoothConfigurator<DF> smooth(Aes localAes) {
        var baseGeom = new GeomSmooth<Object>();
        return new SmoothConfigurator<>(
            (GeomSmooth<DF>) baseGeom,
            localAes
        );
    }

    /**
     * Creates a {@code Geoms.function()} mathematical-curve layer configurator.
     * <p>
     * Evaluates the given function at evenly spaced points along the x axis and
     * draws the result as a continuous line — superimposing a known curve on top
     * of an existing plot, or on an empty plot whose x range is set via
     * {@link FunctionConfigurator#xlim(double, double)}.
     *
     * @param <DF> the DataFrame type
     * @param fun  the function to draw, mapping an x value to a y value
     * @return a {@link FunctionConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> FunctionConfigurator<DF> function(DoubleUnaryOperator fun) {
        return new FunctionConfigurator<>((GeomFunction<DF>) new GeomFunction<Object>(fun));
    }

    /**
     * Creates a {@code Geoms.point3d()} 3D scatter plot layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Point3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Point3dConfigurator<DF> point3d() {
        var baseGeom = new GeomPoint3d<Object>();
        var spec = new Point3dSpec();
        return new Point3dConfigurator<>((GeomPoint3d<DF>) baseGeom, spec);
    }

    /**
     * Creates a {@code Geoms.polygon3d()} 3D polygon layer configurator:
     * renders depth-sorted polygon rings from the raw (or stat-transformed)
     * data.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Polygon3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Polygon3dConfigurator<DF> polygon3d() {
        var geom = new GeomPolygon3d<Object>();
        return new Polygon3dConfigurator<>((GeomPolygon3d<DF>) geom, new Polygon3dSpec());
    }

    /**
     * Creates a {@code Geoms.surface3d()} 3D surface layer configurator:
     * tessellates a point grid into polygon tiles, running
     * {@code Stats.surface3d()} by default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Surface3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Surface3dConfigurator<DF> surface3d() {
        var geom = new GeomSurface3d<Object>();
        return new Surface3dConfigurator<>((GeomSurface3d<DF>) geom, new Polygon3dSpec()
                .fill(Color.rgb(153, 153, 153)).colour(null));
    }

    /**
     * Creates a {@code Geoms.function3d()} 3D function-surface layer
     * configurator: evaluates {@code fun(x, y)} over a regular grid via
     * {@code Stats.function3d()} and renders it as a surface.
     *
     * @param <DF> the DataFrame type
     * @param fun  the surface function {@code (x, y) -> z}
     * @return a {@link Function3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Function3dConfigurator<DF> function3d(DoubleBinaryOperator fun) {
        var geom = new GeomSurface3d<Object>();
        return new Function3dConfigurator<>((GeomSurface3d<DF>) geom, new Polygon3dSpec()
                .fill(Color.rgb(153, 153, 153)).colour(null), fun)
                .stat(Stats.function3d());
    }

    /**
     * Creates a {@code Geoms.density3d()} 3D density-surface layer
     * configurator: estimates a 2-D Gaussian kernel density over a point grid
     * via {@code Stats.density3d()} and renders it as a surface.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Density3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Density3dConfigurator<DF> density3d() {
        var geom = new GeomSurface3d<Object>();
        return new Density3dConfigurator<>((GeomSurface3d<DF>) geom, new Polygon3dSpec()
                .fill(Color.rgb(153, 153, 153)).colour(null))
                .stat(Stats.density3d());
    }

    /**
     * Creates a {@code Geoms.ridgeline3d()} 3D ridgeline layer configurator:
     * converts a point grid into closed ridge polygons, running
     * {@code Stats.identity3d()} by default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Ridgeline3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Ridgeline3dConfigurator<DF> ridgeline3d() {
        var geom = new GeomRidgeline3d<Object>();
        return new Ridgeline3dConfigurator<>((GeomRidgeline3d<DF>) geom, new Polygon3dSpec()
                .fill(Color.web("white"))
                .colour(Color.web("black"))
                .sortMethod(Polygon3dSpec.SortMethod.PAIRWISE));
    }

    /**
     * Creates a {@code Geoms.contour3d()} 3D contour layer configurator:
     * converts a point grid into contour band polygons, running
     * {@code Stats.surface3d()} by default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Contour3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Contour3dConfigurator<DF> contour3d() {
        var geom = new GeomContour3d<Object>();
        return new Contour3dConfigurator<>((GeomContour3d<DF>) geom, new Polygon3dSpec()
                .fill(Color.web("black"))
                .colour(Color.web("white"))
                .sortMethod(Polygon3dSpec.SortMethod.PAIRWISE));
    }

    /**
     * Creates a {@code Geoms.smooth3d()} 3D smooth layer configurator: fits a
     * loess or lm surface via {@code Stats.smooth3d()} and draws the fitted
     * polygon panels, with optional data-point and residual-segment overlays.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Smooth3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Smooth3dConfigurator<DF> smooth3d() {
        var geom = new GeomSmooth3d<Object>();
        return new Smooth3dConfigurator<>((GeomSmooth3d<DF>) geom, new Polygon3dSpec());
    }

    /**
     * Creates a {@code Geoms.col3d()} 3D column layer configurator: turns grid
     * points into rectangular columns from a {@code zmin} base up to each
     * row's {@code z}, running {@code Stats.col3d()} by default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Col3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Col3dConfigurator<DF> col3d() {
        var geom = new GeomCol3d<Object>();
        return new Col3dConfigurator<>((GeomCol3d<DF>) geom, Polygon3dSpec.solid());
    }

    /**
     * Creates a {@code Geoms.bar3d()} 3D bar layer configurator: counts or
     * bins a 2-D layout into 3-D bars, running {@code Stats.bar3d()} by
     * default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Bar3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Bar3dConfigurator<DF> bar3d() {
        var geom = new GeomBar3d<Object>();
        return new Bar3dConfigurator<>((GeomBar3d<DF>) geom, Polygon3dSpec.solid())
                .mapping(Aes.aes().z(AesValue.afterStat("count")));
    }

    /**
     * Creates a {@code Geoms.voxel3d()} 3D voxel layer configurator: fixed-size
     * cubes centered on sparse point coordinates, running
     * {@code Stats.voxel3d()} by default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Voxel3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Voxel3dConfigurator<DF> voxel3d() {
        var geom = new GeomVoxel3d<Object>();
        return new Voxel3dConfigurator<>((GeomVoxel3d<DF>) geom, Polygon3dSpec.solid());
    }

    /**
     * Creates a {@code Geoms.hull3d()} 3D hull layer configurator: a convex or
     * alpha surface hull over a point cloud, running {@code Stats.hull3d()} by
     * default.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Hull3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Hull3dConfigurator<DF> hull3d() {
        var geom = new GeomHull3d<Object>();
        return new Hull3dConfigurator<>((GeomHull3d<DF>) geom, new Polygon3dSpec().cullBackfaces(true));
    }

    /**
     * Creates a {@code Geoms.line()} line chart layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} for registering the line layer
     */
    public static <DF> LayerConfigurator<DF> line() {
        return plot -> plot.registerInternalLayer(new GeomLine<DF>());
    }

    /**
     * Creates a {@code Geoms.line()} line chart layer with specified line color and width.
     *
     * @param <DF> the DataFrame type
     * @param color the stroke color of the line
     * @param width the line stroke width in pixels
     * @return a {@link LayerConfigurator} for registering the line layer
     */
    public static <DF> LayerConfigurator<DF> line(Color color, double width) {
        return plot -> plot.registerInternalLayer(new GeomLine<DF>().color(color).width(width));
    }

    /**
     * Creates a {@code Geoms.line()} line chart layer with an explicit averaging filter mode and line width.
     *
     * @param <DF> the DataFrame type
     * @param averaging the averaging aggregation strategy
     * @param width the line stroke width in pixels
     * @return a {@link LayerConfigurator} for registering the line layer
     */
    public static <DF> LayerConfigurator<DF> line(Averaging averaging, double width) {
        return plot -> plot.registerInternalLayer(new GeomLine<DF>().averaging(averaging).width(width));
    }

    /**
     * Creates a {@code Geoms.polygon()} layer: a closed polygon per group whose
     * vertices are routed through the coordinate system, the classic radar or
     * spider web chart when combined with {@link org.jtaccuino.gog.Coords#coordPolar()}. A
     * categorical x column maps to the angular spokes, and {@code aes().group()}
     * (or {@code color}/{@code fill}) splits the data into one polygon each.
     *
     * @param <DF> the DataFrame type
     * @return a {@link PolygonConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> PolygonConfigurator<DF> polygon() {
        return new PolygonConfigurator<>((GeomPolygon<DF>) new GeomPolygon<Object>());
    }

    /**
     * Creates a classic {@code Geoms.bar()} bar chart layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return a {@link BarConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> BarConfigurator<DF> bar() {
        return new BarConfigurator<>((GeomBar<DF>) new GeomBar<Object>());
    }

    /**
     * Creates a {@code Geoms.bar()} bar chart layer with a specific position layout mode.
     *
     * @param <DF>     the DataFrame type
     * @param position the position layout mode (e.g., dodge, stack, identity)
     * @return a {@link BarConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> BarConfigurator<DF> bar(Position position) {
        return new BarConfigurator<>((GeomBar<DF>) new GeomBar<Object>().position(position));
    }

    /**
     * Creates a {@code Geoms.bar()} bar chart layer with a parameterised position
     * adjustment ({@code Positions.dodge(width)}, {@code Positions.stack(reverse)}, ...).
     *
     * @param <DF>  the DataFrame type
     * @param adjust the {@link PositionAdjust} to apply
     * @return a {@link BarConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> BarConfigurator<DF> bar(PositionAdjust adjust) {
        return new BarConfigurator<>((GeomBar<DF>) new GeomBar<Object>().position(adjust));
    }

    /**
     * Creates the bare {@code Geoms.bar()} geometry without any stat, position,
     * or mapping — the low-level building block for hand-assembling a layer via
     * {@link Plot#layer(org.jtaccuino.gog.layer.Layer, Stat, PositionAdjust, Aes, LayerParams)}:
     * {@code layer(barGeom(), Stats.count(), Positions.identity(), mapping, params)}.
     * <p>
     * The ergonomic {@link #bar()} configurator wraps the same geometry and
     * fuses {@code Stats.count()} (and the default stack position) by itself;
     * reach for {@code barGeom()} when a part of the layer must be supplied
     * explicitly.
     *
     * @param <DF> the DataFrame type
     * @return a bare {@link GeomBar} instance
     */
    public static <DF> GeomBar<DF> barGeom() {
        return new GeomBar<DF>();
    }

    /**
     * Creates a {@code Geoms.col()} column chart layer configurator.
     * <p>
     * The column heights represent the values of the {@code y} aesthetic
     * directly ({@code stat = "identity"} in the reference implementation), leaving the data as is —
     * the chart to reach for when the heights are already in the data rather
     * than counted. Columns sharing an x position are stacked by default;
     * pass {@link #col(Position)} to dodge them instead.
     *
     * @param <DF> the DataFrame type
     * @return a {@link ColConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ColConfigurator<DF> col() {
        return new ColConfigurator<>((GeomCol<DF>) new GeomCol<Object>());
    }

    /**
     * Creates a {@code Geoms.col()} column chart layer with a specific position
     * layout mode.
     *
     * @param <DF>     the DataFrame type
     * @param position the position layout mode (e.g., dodge, stack, identity)
     * @return a {@link ColConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ColConfigurator<DF> col(Position position) {
        return new ColConfigurator<>((GeomCol<DF>) new GeomCol<Object>().position(position));
    }

    /**
     * Creates a {@code Geoms.col()} column chart layer with a parameterised
     * position adjustment ({@code Positions.dodge(width)}, {@code Positions.stack(reverse)}, ...).
     *
     * @param <DF>   the DataFrame type
     * @param adjust the {@link PositionAdjust} to apply
     * @return a {@link ColConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ColConfigurator<DF> col(PositionAdjust adjust) {
        return new ColConfigurator<>((GeomCol<DF>) new GeomCol<Object>().position(adjust));
    }

    /**
     * Creates the bare {@code Geoms.col()} geometry without any stat, position,
     * or mapping — the low-level building block for hand-assembling a layer via
     * {@link Plot#layer(org.jtaccuino.gog.layer.Layer, Stat, PositionAdjust, Aes, LayerParams)}.
     * {@code Geoms.col()} inherits the identity statistic, so
     * {@code layer(colGeom(), Stats.identity(), Positions.identity(), mapping, params)}
     * draws the raw {@code y} values as columns.
     *
     * @param <DF> the DataFrame type
     * @return a bare {@link GeomCol} instance
     */
    public static <DF> GeomCol<DF> colGeom() {
        return new GeomCol<DF>();
    }

    /**
     * Creates an {@code area()} filled area chart layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return an {@link AreaConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> AreaConfigurator<DF> area() {
        return new AreaConfigurator<>((GeomArea<DF>) new GeomArea<Object>());
    }

    /**
     * Creates a {@code Geoms.boxplot()} box-and-whiskers layer configurator.
     * <p>
     * Renders a Tukey box-and-whiskers summary per categorical x group: hinges at the
     * 25th/75th percentiles, a median band, whiskers to {@code 1.5 × IQR}, and outliers.
     *
     * @param <DF> the DataFrame type
     * @return a {@link BoxplotConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> BoxplotConfigurator<DF> boxplot() {
        return new BoxplotConfigurator<>((GeomBoxplot<DF>) new GeomBoxplot<Object>());
    }

    /**
     * Creates a {@code Geoms.violin()} violin plot layer configurator.
     * <p>
     * Mirrors a Gaussian kernel-density estimate per categorical x group — a
     * compact display of the full continuous distribution, blending a boxplot
     * and a density plot (Hintze &amp; Nelson 1998). Supports tail trimming,
     * bandwidth adjustment, area/count/width scaling, and optional quartile
     * marks.
     *
     * @param <DF> the DataFrame type
     * @return a {@link ViolinConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ViolinConfigurator<DF> violin() {
        return new ViolinConfigurator<>((GeomViolin<DF>) new GeomViolin<Object>());
    }

    /**
     * Creates a {@code Geoms.density()} kernel-density estimate layer configurator.
     * <p>
     * Computes and draws a smoothed density estimate of a continuous variable —
     * the alternative to a histogram for data from an underlying smooth
     * distribution. Map only {@code aes(x)} (or only {@code aes(y)}) and the
     * estimate runs along the other axis; a colour/fill column draws one
     * estimate per category, which can be stacked ({@code position = "stack"})
     * or normalized to a conditional density ({@code position = "fill"}).
     *
     * @param <DF> the DataFrame type
     * @return a {@link DensityConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> DensityConfigurator<DF> density() {
        return new DensityConfigurator<>((GeomDensity<DF>) new GeomDensity<Object>());
    }

    /**
     * Creates a {@code Geoms.density2d()} contour layer configurator.
     * <p>
     * Runs a two-dimensional Gaussian kernel-density estimate over the points
     * mapped to {@code aes(x)} and {@code aes(y)} — the 2D counterpart of
     * {@link #density()} — and strokes the contours of the resulting surface.
     * A colour column draws one set of contours per category. Options mirror
     * the reference recipe: the grid resolution ({@code n}), the bandwidth adjustment
     * ({@code adjust}), the number of contour levels ({@code bins}), and the
     * contoured statistic ({@code contour_var}).
     *
     * @param <DF> the DataFrame type
     * @return a {@link Density2dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Density2dConfigurator<DF> density2d() {
        return new Density2dConfigurator<>((GeomDensity2d<DF>) new GeomDensity2d<Object>());
    }

    /**
     * Creates a {@code Geoms.density2dFilled()} filled-band configurator.
     * <p>
     * The same 2D kernel-density estimate as {@link #density2d()}, drawn as
     * filled bands between the contour levels on a continuous colour ramp
     * instead of as contour lines — {@code Geoms.density2dFilled()} in
     * grammar-of-graphics terms.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Density2dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Density2dConfigurator<DF> density2dFilled() {
        return new Density2dConfigurator<>((GeomDensity2d<DF>) new GeomDensity2d<Object>().filled(true).alpha(0.5));
    }

    /**
     * Creates a {@code Geoms.histogram()} frequency layer configurator.
     * <p>
     * {@code Stats.bin()} over a continuous {@code aes(x)} drawn as bars of
     * height equal to the per-bin observation count. Bars sharing a position
     * are stacked by default; pass {@link #histogram()} options such as
     * {@code .bins(...)} to control the binning, {@code .fill(...)} for a
     * constant colour, or map a {@code fill}/{@code color} aesthetic for one
     * stacked series per group.
     *
     * @param <DF> the DataFrame type
     * @return a {@link HistogramConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> HistogramConfigurator<DF> histogram() {
        return new HistogramConfigurator<>((GeomHistogram<DF>) new GeomHistogram<Object>());
    }

    /**
     * Creates the bare {@code Geoms.histogram()} geometry without any stat,
     * position, or mapping — the low-level building block for hand-assembling a
     * layer via
     * {@link Plot#layer(org.jtaccuino.gog.layer.Layer, Stat, PositionAdjust, Aes, LayerParams)}.
     * {@code Geoms.histogram()} runs {@code Stats.bin()} by default, so
     * {@code layer(histogramGeom(), Stats.bin(), Positions.identity(), mapping, params)}
     * bins a continuous {@code x} into bars.
     *
     * @param <DF> the DataFrame type
     * @return a bare {@link GeomHistogram} instance
     */
    public static <DF> GeomHistogram<DF> histogramGeom() {
        return new GeomHistogram<DF>();
    }

    /**
     * Creates a {@code Geoms.freqpoly()} polyline configurator.
     * <p>
     * {@code Stats.bin()} drawn as a line through the bin centres with the
     * per-bin count on the vertical axis — ideal for overlaying several
     * distributions. Maps only {@code aes(x)}; map a {@code color}/{@code fill}
     * aesthetic for one line per category.
     *
     * @param <DF> the DataFrame type
     * @return a {@link FreqpolyConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> FreqpolyConfigurator<DF> freqpoly() {
        return new FreqpolyConfigurator<>((GeomFreqpoly<DF>) new GeomFreqpoly<Object>());
    }

    /**
     * Creates the bare {@code Geoms.freqpoly()} geometry without any stat,
     * position, or mapping — the low-level building block for hand-assembling a
     * layer via
     * {@link Plot#layer(org.jtaccuino.gog.layer.Layer, Stat, PositionAdjust, Aes, LayerParams)}.
     * {@code Geoms.freqpoly()} runs {@code Stats.bin()} by default, so
     * {@code layer(freqpolyGeom(), Stats.bin(), Positions.identity(), mapping, params)}
     * bins a continuous {@code x} into a frequency polyline.
     *
     * @param <DF> the DataFrame type
     * @return a bare {@link GeomFreqpoly} instance
     */
    public static <DF> GeomFreqpoly<DF> freqpolyGeom() {
        return new GeomFreqpoly<DF>();
    }

    /**
     * Creates a {@code Geoms.crossbar()} box configurator.
     * <p>
     * A filled horizontal box per row spanning the {@code ymin}/{@code ymax}
     * aesthetics centred on {@code x} with a thicker centre line at {@code y}.
     * Consumes the output of {@code Stats.summary} (columns {@code x}, {@code y},
     * {@code ymin}, {@code ymax}); map a {@code fill}/{@code color} aesthetic
     * for one bar per category.
     *
     * @param <DF> the DataFrame type
     * @return a {@link CrossbarConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> CrossbarConfigurator<DF> crossbar() {
        return new CrossbarConfigurator<>((GeomCrossbar<DF>) new GeomCrossbar<Object>());
    }

    /**
     * Creates a {@code Geoms.errorbar()} whisker configurator.
     * <p>
     * A vertical whisker per row between the {@code ymin}/{@code ymax}
     * aesthetics centred on {@code x} with a cap tick at each end. Consumes the
     * output of {@code Stats.summary}; map a {@code color} aesthetic for one
     * whisker per category.
     *
     * @param <DF> the DataFrame type
     * @return an {@link ErrorbarConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ErrorbarConfigurator<DF> errorbar() {
        return new ErrorbarConfigurator<>((GeomErrorbar<DF>) new GeomErrorbar<Object>());
    }

    /**
     * Creates a {@code Geoms.errorbarh()} horizontal whisker configurator.
     * <p>
     * A horizontal whisker per row between the {@code xmin}/{@code xmax}
     * aesthetics centred on {@code y} with a cap tick at each end — the flipped
     * mirror of {@link #errorbar()}. Consumes the output of {@code Stats.summary}
     * on a horizontal layout; map a {@code color} aesthetic for one whisker per
     * category.
     *
     * @param <DF> the DataFrame type
     * @return an {@link ErrorbarhConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> ErrorbarhConfigurator<DF> errorbarh() {
        return new ErrorbarhConfigurator<>((GeomErrorbarh<DF>) new GeomErrorbarh<Object>());
    }

    /**
     * Creates a {@code Geoms.linerange()} cap-less whisker configurator.
     * <p>
     * A vertical line per row between the {@code ymin}/{@code ymax} aesthetics
     * centred on {@code x} — {@link #errorbar()} without the cap ticks.
     * Consumes the output of {@code Stats.summary}; map a {@code color}
     * aesthetic for one line per category.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LinerangeConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> LinerangeConfigurator<DF> linerange() {
        return new LinerangeConfigurator<>((GeomLinerange<DF>) new GeomLinerange<Object>());
    }

    /**
     * Creates a {@code Geoms.pointrange()} whisker-and-point configurator.
     * <p>
     * A vertical line per row between the {@code ymin}/{@code ymax} aesthetics
     * centred on {@code x} with a point drawn at {@code (x, y)}.
     * Consumes the output of {@code Stats.summary}; map a {@code color}
     * aesthetic for one pointrange per category.
     *
     * @param <DF> the DataFrame type
     * @return a {@link PointrangeConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> PointrangeConfigurator<DF> pointrange() {
        return new PointrangeConfigurator<>((GeomPointrange<DF>) new GeomPointrange<Object>());
    }

    /**
     * Creates a {@code Geoms.ribbon()} band configurator.
     * <p>
     * A filled band between the {@code ymin}/{@code ymax} curves across the
     * ordered {@code x} values — the classic confidence or quantile band.
     * Consumes the output of {@code Stats.summary}; map a {@code color}/
     * {@code fill} aesthetic for one ribbon per category.
     *
     * @param <DF> the DataFrame type
     * @return a {@link RibbonConfigurator} for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> RibbonConfigurator<DF> ribbon() {
        return new RibbonConfigurator<>((GeomRibbon<DF>) new GeomRibbon<Object>());
    }

    /**
     * Creates a {@code Geoms.hline()} horizontal reference line at a constant
     * Y value — a significance threshold, a target, or a zero baseline.
     *
     * @param <DF>       the DataFrame type
     * @param yIntercept the data-space Y value at which to draw the line
     * @return an {@link HlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> HlineConfigurator<DF> hline(double yIntercept) {
        return new HlineConfigurator<>((GeomHline<DF>) new GeomHline<Object>(yIntercept));
    }

    /**
     * Creates a bare {@code Geoms.hline()} builder. Pair it with the fluent
     * {@link HlineConfigurator} methods to supply a data frame and intercept
     * column — {@code hline().data(summary).yintercept("wt")} — mirroring
     * the {@code Geoms.hline(aes(yintercept = wt), mean_wt)}.
     *
     * @param <DF> the DataFrame type
     * @return an {@link HlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> HlineConfigurator<DF> hline() {
        return new HlineConfigurator<>((GeomHline<DF>) new GeomHline<Object>());
    }

    /**
     * Creates a data-driven {@code Geoms.hline()} that reads its Y intercept
     * values from a column of the given frame, drawing one line per row. Under a
     * facet the frame's own rows are matched to the current panel by the facet
     * column — the mtcars analogue of the
     * {@code Geoms.hline(aes(yintercept = wt), mean_wt)} inside
     * {@code Facets.wrap(~cyl)}.
     *
     * @param <DF>     the DataFrame type
     * @param data     the frame holding the intercept values
     * @param yColumn  the column holding the Y intercept values
     * @return an {@link HlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> HlineConfigurator<DF> hline(DF data, String yColumn) {
        return new HlineConfigurator<>((GeomHline<DF>) new GeomHline<Object>(data, yColumn));
    }

    /**
     * Creates a {@code Geoms.abline()} reference line with the given slope and
     * intercept in data space. {@code abline(1, 0)} is the identity line, the
     * null expectation of a quantile-quantile plot.
     *
     * @param <DF>      the DataFrame type
     * @param slope     the slope in data units
     * @param intercept the value of Y where X is zero
     * @return an {@link AblineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> AblineConfigurator<DF> abline(double slope, double intercept) {
        return new AblineConfigurator<>((GeomAbline<DF>) new GeomAbline<Object>(slope, intercept));
    }

    /**
     * Creates a bare {@code Geoms.abline()} builder. Pair it with the fluent
     * {@link AblineConfigurator} methods to supply a data frame and line
     * parameter columns — {@code abline().data(fit).slope("slope").intercept("intercept")} —
     * following the {@code Geoms.abline(aes(slope = a, intercept = b), fit)}.
     *
     * @param <DF> the DataFrame type
     * @return an {@link AblineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> AblineConfigurator<DF> abline() {
        return new AblineConfigurator<>((GeomAbline<DF>) new GeomAbline<Object>());
    }

    /**
     * Creates a data-driven {@code Geoms.abline()} that reads its slope and
     * intercept values from columns of the given frame, drawing one line per
     * row. Under a facet the frame's own rows are matched to the current panel
     * by the facet column, so each panel can draw its own regression line.
     *
     * @param <DF>            the DataFrame type
     * @param data            the frame holding the line parameters
     * @param slopeColumn     the column holding the slope values
     * @param interceptColumn the column holding the intercept values
     * @return an {@link AblineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> AblineConfigurator<DF> abline(DF data, String slopeColumn, String interceptColumn) {
        return new AblineConfigurator<>((GeomAbline<DF>) new GeomAbline<Object>(data, slopeColumn, interceptColumn));
    }

    /**
     * Creates a {@code Geoms.vline()} vertical reference line at a constant
     * X value — a target date, a cut-off, or a reference observation.
     *
     * @param <DF>       the DataFrame type
     * @param xIntercept the data-space X value at which to draw the line
     * @return a {@link VlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> VlineConfigurator<DF> vline(double xIntercept) {
        return new VlineConfigurator<>((GeomVline<DF>) new GeomVline<Object>(xIntercept));
    }

    /**
     * Creates a bare {@code Geoms.vline()} builder. Pair it with the fluent
     * {@link VlineConfigurator} methods to supply a data frame and intercept
     * column — {@code vline().data(summary).xintercept("mpg")} — mirroring
     * the {@code Geoms.vline(aes(xintercept = mpg), mean_mpg)}.
     *
     * @param <DF> the DataFrame type
     * @return a {@link VlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> VlineConfigurator<DF> vline() {
        return new VlineConfigurator<>((GeomVline<DF>) new GeomVline<Object>());
    }

    /**
     * Creates a data-driven {@code Geoms.vline()} that reads its X intercept
     * values from a column of the given frame, drawing one line per row. Under a
     * facet the frame's own rows are matched to the current panel by the facet
     * column, so each panel can draw its own lines.
     *
     * @param <DF>     the DataFrame type
     * @param data     the frame holding the intercept values
     * @param xColumn  the column holding the X intercept values
     * @return a {@link VlineConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> VlineConfigurator<DF> vline(DF data, String xColumn) {
        return new VlineConfigurator<>((GeomVline<DF>) new GeomVline<Object>(data, xColumn));
    }

    /**
     * Creates a {@code Geoms.text()} annotation layer that draws the value of the
     * {@link Aes#label(String) label} aesthetic beside each point. Rows with a
     * blank label are skipped, so a sparse label column names only the
     * observations worth calling out.
     *
     * @param <DF> the DataFrame type
     * @return a {@link TextConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> TextConfigurator<DF> text() {
        return new TextConfigurator<>((GeomText<DF>) new GeomText<Object>());
    }

    /**
     * Creates a {@code Geoms.text()} layer hosting {@code Stats.cor()} — the
     * correlation annotation of a continuous {@code (x, y)} pairing, and the
     * geometry behind the correlation-only upper triangle of
     * {@code matrixPlot()}. It runs {@code Stats.cor()} over the panel data and
     * renders the value of the {@link Aes#label(AesValue) label} aesthetic by
     * default bound to {@link AesValue#afterStat(AesValue.ComputedVariable)}
     * {@code CORR}, so rows whose anchor is remote stay blank. The label
     * colour follows the theme's
     * {@link org.jtaccuino.gog.theme.Theme#textColor() text color}; only the
     * matrix-cell layout is baked in — compact 9&nbsp;pt labelling nudged
     * −6/−6 pixels with no collision staggering.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} registering the correlation text layer
     */
    public static <DF> LayerConfigurator<DF> corrText() {
        return plot -> plot.registerInternalLayer(
                new GeomText<DF>(new StatCor<DF>())
                        .nudge(-6, -6)
                        .avoidOverlap(false));
    }

    /**
     * Creates a {@code Geoms.text3d()} 3D text label layer configurator
     * (billboard method): labels are drawn flat, always facing the camera, at
     * their projected 3D anchors, size-scaled and depth-sorted.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Text3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Text3dConfigurator<DF> text3d() {
        return new Text3dConfigurator<>((GeomText3d<DF>) new GeomText3d<Object>());
    }

    /**
     * Creates a {@code Geoms.segment3d()} 3D segment layer configurator:
     * one directed segment per row from {@code (x, y, z)} to
     * {@code (xend, yend, zend)}, depth-sorted back-to-front.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Segment3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Segment3dConfigurator<DF> segment3d() {
        return new Segment3dConfigurator<>((GeomSegment3d<DF>) new GeomSegment3d<Object>());
    }

    /**
     * Creates a {@code Geoms.path3d()} 3D path layer configurator: connects
     * observations in 3D space in row order, splitting each group into
     * depth-sortable segments {@code (StatPath3D)}.
     *
     * @param <DF> the DataFrame type
     * @return a {@link Segment3dConfigurator} instance for fluid method chaining
     */
    @SuppressWarnings("unchecked")
    public static <DF> Segment3dConfigurator<DF> path3d() {
        return new Segment3dConfigurator<>((GeomPath3d<DF>) new GeomPath3d<Object>());
    }

    /**
     * Creates a {@code Geoms.tile()} heatmap tile layer configurator.
     *
     * @param <DF> the DataFrame type
     * @return a {@link TileConfigurator} for fluid configuration of the tile layer
     */
    public static <DF> TileConfigurator<DF> tile() {
        return new TileConfigurator<>(new GeomTile<DF>());
    }

    /**
     * Creates a {@code Geoms.tile()} heatmap tile layer with a specific colormap.
     *
     * @param <DF> the DataFrame type
     * @param cmapName the colormap name ("viridis", "plasma", "blues", etc.)
     * @return a {@link TileConfigurator} for fluid configuration of the tile layer
     */
    public static <DF> TileConfigurator<DF> tile(String cmapName) {
        return new TileConfigurator<>(new GeomTile<DF>().cmap(cmapName));
    }

    /**
     * Creates a {@code Geoms.tile()} heatmap tile layer with a specific line width for tile borders.
     *
     * @param <DF>      the DataFrame type
     * @param lineWidth stroke width for tile borders (0 for directly adjacent tiles)
     * @return a {@link TileConfigurator} for fluid configuration of the tile layer
     */
    public static <DF> TileConfigurator<DF> tile(double lineWidth) {
        return new TileConfigurator<>(new GeomTile<DF>().lineWidth(lineWidth));
    }

    /**
     * Creates a {@code Geoms.tile()} heatmap tile layer with a specific colormap and line width.
     *
     * @param <DF>      the DataFrame type
     * @param cmapName  the colormap name ("viridis", "plasma", "blues", etc.)
     * @param lineWidth stroke width for tile borders (0 for directly adjacent tiles)
     * @return a {@link TileConfigurator} for fluid configuration of the tile layer
     */
    public static <DF> TileConfigurator<DF> tile(String cmapName, double lineWidth) {
        return new TileConfigurator<>(new GeomTile<DF>().cmap(cmapName).lineWidth(lineWidth));
    }

    /**
     * Creates a {@code Geoms.path()} layer drawing a multi-segment polyline in
     * row order — the geometry for trajectories and cycles, where the sequence
     * of points matters rather than x-sorted order.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} for registering the path layer
     */
    public static <DF> LayerConfigurator<DF> path() {
        return plot -> plot.registerInternalLayer(new GeomPath<DF>());
    }

    /**
     * Creates a {@code Geoms.path()} layer with specified line color and width.
     *
     * @param <DF>  the DataFrame type
     * @param color the stroke color of the path
     * @param width the path stroke width in pixels
     * @return a {@link LayerConfigurator} for registering the path layer
     */
    public static <DF> LayerConfigurator<DF> path(Color color, double width) {
        return plot -> plot.registerInternalLayer(new GeomPath<DF>().color(color).width(width));
    }

    /**
     * Creates a {@code Geoms.step()} staircase layer drawing points as steps
     * (vertical jump then horizontal run), sorted by the independent axis.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} for registering the step layer
     */
    public static <DF> LayerConfigurator<DF> step() {
        return plot -> plot.registerInternalLayer(new GeomStep<DF>());
    }

    /**
     * Creates a {@code Geoms.step()} staircase layer with specified line color and width.
     *
     * @param <DF>  the DataFrame type
     * @param color the stroke color of the steps
     * @param width the step stroke width in pixels
     * @return a {@link LayerConfigurator} for registering the step layer
     */
    public static <DF> LayerConfigurator<DF> step(Color color, double width) {
        return plot -> plot.registerInternalLayer(new GeomStep<DF>().color(color).width(width));
    }

    /**
     * Creates a {@code Geoms.segment()} layer drawing one directed line per row
     * from {@code (x, y)} to {@code (xend, yend)}.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} for registering the segment layer
     */
    public static <DF> LayerConfigurator<DF> segment() {
        return plot -> plot.registerInternalLayer(new GeomSegment<DF>());
    }

    /**
     * Creates a {@code Geoms.segment()} layer with specified line color and width.
     *
     * @param <DF>  the DataFrame type
     * @param color the stroke color of the segments
     * @param width the segment stroke width in pixels
     * @return a {@link LayerConfigurator} for registering the segment layer
     */
    public static <DF> LayerConfigurator<DF> segment(Color color, double width) {
        return plot -> plot.registerInternalLayer(new GeomSegment<DF>().color(color).width(width));
    }

    /**
     * Creates a {@code Geoms.curve()} layer drawing one bowed quadratic arc per
     * row from {@code (x, y)} to {@code (xend, yend)}, with a default curvature
     * of 0.5.
     *
     * @param <DF> the DataFrame type
     * @return a {@link LayerConfigurator} for registering the curve layer
     */
    public static <DF> LayerConfigurator<DF> curve() {
        return plot -> plot.registerInternalLayer(new GeomCurve<DF>());
    }

    /**
     * Creates a {@code Geoms.curve()} layer with specified line color, width, and curvature.
     *
     * @param <DF>       the DataFrame type
     * @param color      the stroke color of the curves
     * @param width      the curve stroke width in pixels
     * @param curvature  the bow strength in {@code [0, 1]} (0 = straight line)
     * @return a {@link LayerConfigurator} for registering the curve layer
     */
    public static <DF> LayerConfigurator<DF> curve(Color color, double width, double curvature) {
        return plot -> plot.registerInternalLayer(new GeomCurve<DF>().color(color).width(width).curvature(curvature));
    }
}
