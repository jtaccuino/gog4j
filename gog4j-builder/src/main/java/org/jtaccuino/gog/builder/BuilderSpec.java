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
package org.jtaccuino.gog.builder;

import static org.jtaccuino.gog.Geoms.abline;
import static org.jtaccuino.gog.Geoms.area;
import static org.jtaccuino.gog.Geoms.bar;
import static org.jtaccuino.gog.Geoms.bar3d;
import static org.jtaccuino.gog.Geoms.boxplot;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.col3d;
import static org.jtaccuino.gog.Geoms.contour3d;
import static org.jtaccuino.gog.Geoms.crossbar;
import static org.jtaccuino.gog.Geoms.curve;
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.density2d;
import static org.jtaccuino.gog.Geoms.density2dFilled;
import static org.jtaccuino.gog.Geoms.density3d;
import static org.jtaccuino.gog.Geoms.errorbar;
import static org.jtaccuino.gog.Geoms.errorbarh;
import static org.jtaccuino.gog.Geoms.freqpoly;
import static org.jtaccuino.gog.Geoms.function3d;
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Geoms.hline;
import static org.jtaccuino.gog.Geoms.hull3d;
import static org.jtaccuino.gog.Geoms.jitter;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.linerange;
import static org.jtaccuino.gog.Geoms.path;
import static org.jtaccuino.gog.Geoms.path3d;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.pointrange;
import static org.jtaccuino.gog.Geoms.polygon;
import static org.jtaccuino.gog.Geoms.polygon3d;
import static org.jtaccuino.gog.Geoms.ribbon;
import static org.jtaccuino.gog.Geoms.ridgeline3d;
import static org.jtaccuino.gog.Geoms.segment;
import static org.jtaccuino.gog.Geoms.segment3d;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Geoms.smooth3d;
import static org.jtaccuino.gog.Geoms.step;
import static org.jtaccuino.gog.Geoms.surface3d;
import static org.jtaccuino.gog.Geoms.text;
import static org.jtaccuino.gog.Geoms.text3d;
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Geoms.violin;
import static org.jtaccuino.gog.Geoms.vline;
import static org.jtaccuino.gog.Geoms.voxel3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleColorBrewer;
import static org.jtaccuino.gog.scale.Scales.scaleColorManual;
import static org.jtaccuino.gog.scale.Scales.scaleColorViridisC;
import static org.jtaccuino.gog.scale.Scales.scaleColorViridisD;
import static org.jtaccuino.gog.scale.Scales.scaleFillBrewer;
import static org.jtaccuino.gog.scale.Scales.scaleFillViridisC;
import static org.jtaccuino.gog.scale.Scales.scaleFillViridisD;
import static org.jtaccuino.gog.scale.Scales.scaleXLog10;
import static org.jtaccuino.gog.scale.Scales.scaleXReverse;
import static org.jtaccuino.gog.scale.Scales.scaleXSqrt;
import static org.jtaccuino.gog.scale.Scales.scaleYLog10;
import static org.jtaccuino.gog.scale.Scales.scaleYReverse;
import static org.jtaccuino.gog.scale.Scales.scaleYSqrt;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.layer.AblineConfigurator;
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
import org.jtaccuino.gog.layer.HistogramConfigurator;
import org.jtaccuino.gog.layer.HlineConfigurator;
import org.jtaccuino.gog.layer.Hull3dConfigurator;
import org.jtaccuino.gog.layer.JitterConfigurator;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.layer.LinerangeConfigurator;
import org.jtaccuino.gog.layer.Point3dConfigurator;
import org.jtaccuino.gog.layer.PointConfigurator;
import org.jtaccuino.gog.layer.PointrangeConfigurator;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.layer.RibbonConfigurator;
import org.jtaccuino.gog.layer.Ridgeline3dConfigurator;
import org.jtaccuino.gog.layer.Segment3dConfigurator;
import org.jtaccuino.gog.layer.Smooth3dConfigurator;
import org.jtaccuino.gog.layer.SmoothConfigurator;
import org.jtaccuino.gog.layer.Surface3dConfigurator;
import org.jtaccuino.gog.layer.Text3dConfigurator;
import org.jtaccuino.gog.layer.TextConfigurator;
import org.jtaccuino.gog.layer.ViolinConfigurator;
import org.jtaccuino.gog.layer.VlineConfigurator;
import org.jtaccuino.gog.layer.Voxel3dConfigurator;
import org.jtaccuino.gog.scale.ColorManualScale;
import org.jtaccuino.gog.theme.Theme;

/**
 * Assembles a live {@link Plot} from a {@link BuilderModel}, mirroring exactly
 * what {@link PlotCodeGenerator} emits so the preview matches the generated
 * source. The builder constructs the plot directly (rather than a descriptor)
 * and lets the normal layout pass render it.
 */
public final class BuilderSpec {

    /** Utility class; not meant to be instantiated. */
    private BuilderSpec() {
    }

    /**
     * Builds the plot described by {@code model}, ready to be placed in a scene
     * for rendering.
     *
     * @param model the builder state
     * @return a new configured {@code Plot} node
     */
    public static Plot<Object> build(BuilderModel model) {
        if (model.dataset() == null) {
            throw new IllegalStateException("No dataset selected");
        }
        var plot = ggplot(model.dataset().data(), model.toAes());

        if (!model.geoms.isEmpty()) {
            plot.geoms(model.geoms.stream().map(BuilderSpec::geom).toList());
        }

        applyScales(plot, model);
        applyTheme(plot, model);
        applyCoord(plot, model);
        applyLabs(plot, model);
        applyFacets(plot, model);

        plot.renderMode(model.renderMode);
        return plot;
    }

    private static LayerConfigurator<Object> geom(BuilderModel.Geom g) {
        return switch (g.kind) {
            case POINT -> {
                PointConfigurator<Object> c = point();
                applyDoubles(g, c::size, c::opacity, BuilderModel.Params.SIZE, BuilderModel.Params.OPACITY);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyEnum(g, c::shape, BuilderModel.Params.SHAPE);
                yield c;
            }
            case JITTER -> {
                JitterConfigurator<Object> c = jitter();
                applyDoubles(g, c::width, c::height, c::size, c::opacity,
                        BuilderModel.Params.WIDTH, BuilderModel.Params.HEIGHT, BuilderModel.Params.SIZE, BuilderModel.Params.OPACITY);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyEnum(g, c::shape, BuilderModel.Params.SHAPE);
                yield c;
            }
            case LINE -> line();
            case SMOOTH -> {
                SmoothConfigurator<Object> c = smooth();
                applyEnum(g, c::method, BuilderModel.Params.METHOD);
                applyDouble(g, c::span, BuilderModel.Params.SPAN);
                applyFlag(g, c::se, BuilderModel.Params.SE);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case BAR -> {
                BarConfigurator<Object> c = bar();
                applyPosition(g, c::position);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                yield c;
            }
            case COL -> {
                ColConfigurator<Object> c = col();
                applyPosition(g, c::position);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                yield c;
            }
            case HISTOGRAM -> {
                HistogramConfigurator<Object> c = histogram();
                applyInt(g, c::bins, BuilderModel.Params.BINS);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                yield c;
            }
            case FREQPOLY -> {
                FreqpolyConfigurator<Object> c = freqpoly();
                applyInt(g, c::bins, BuilderModel.Params.BINS);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case DENSITY -> {
                DensityConfigurator<Object> c = density();
                applyDouble(g, c::adjust, BuilderModel.Params.ADJUST);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                yield c;
            }
            case DENSITY2D -> {
                Density2dConfigurator<Object> c = density2d();
                applyDouble(g, c::adjust, BuilderModel.Params.ADJUST);
                applyInt(g, c::bins, BuilderModel.Params.BINS);
                applyFlag(g, c::filled, BuilderModel.Params.FILLED);
                applyDouble(g, c::alpha, BuilderModel.Params.ALPHA);
                yield c;
            }
            case AREA -> {
                org.jtaccuino.gog.layer.AreaConfigurator<Object> c = area();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyPosition(g, c::position);
                yield c;
            }
            case BOXPLOT -> {
                BoxplotConfigurator<Object> c = boxplot();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case VIOLIN -> {
                ViolinConfigurator<Object> c = violin();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                yield c;
            }
            case TEXT -> {
                TextConfigurator<Object> c = text();
                applyDouble(g, c::size, BuilderModel.Params.SIZE);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                if (Boolean.TRUE.equals(g.flags.get(BuilderModel.Params.BOLD))) {
                    c.bold();
                }
                applyDouble(g, c::angle, BuilderModel.Params.ANGLE);
                applyDouble(g, c::hjust, BuilderModel.Params.HJUST);
                applyDouble(g, c::vjust, BuilderModel.Params.VJUST);
                yield c;
            }
            case POINT3D -> {
                Point3dConfigurator<Object> c = point3d();
                applyDouble(g, c::size, BuilderModel.Params.SIZE);
                applyDouble(g, c::opacity, BuilderModel.Params.OPACITY);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyEnum(g, c::shape, BuilderModel.Params.SHAPE);
                applyFlag(g, c::rawPoints, BuilderModel.Params.RAW_POINTS);
                applyFlag(g, c::refLines, BuilderModel.Params.REF_LINES);
                yield c;
            }
            case TEXT3D -> {
                Text3dConfigurator<Object> c = text3d();
                applyDouble(g, c::size, BuilderModel.Params.SIZE);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyFlagCall(g, c::bold, BuilderModel.Params.BOLD);
                yield c;
            }
            case SEGMENT3D -> {
                Segment3dConfigurator<Object> c = segment3d();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::linewidth, BuilderModel.Params.LINE_WIDTH);
                yield c;
            }
            case PATH3D -> {
                Segment3dConfigurator<Object> c = path3d();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::linewidth, BuilderModel.Params.LINE_WIDTH);
                yield c;
            }
            case BAR3D -> {
                Bar3dConfigurator<Object> c = bar3d();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyInt(g, c::bins, BuilderModel.Params.BINS);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case COL3D -> {
                Col3dConfigurator<Object> c = col3d();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyDouble(g, c::zmin, BuilderModel.Params.ZMIN);
                yield c;
            }
            case SURFACE3D -> {
                Surface3dConfigurator<Object> c = surface3d();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::linewidth, BuilderModel.Params.LINE_WIDTH);
                applyEnum(g, c::method, BuilderModel.Params.METHOD);
                applyEnum(g, c::grid, BuilderModel.Params.GRID);
                yield c;
            }
            case POLYGON3D -> {
                org.jtaccuino.gog.layer.Polygon3dConfigurator<Object> c = polygon3d();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::alpha, BuilderModel.Params.ALPHA);
                applyDouble(g, c::linewidth, BuilderModel.Params.LINE_WIDTH);
                yield c;
            }
            case VOXEL3D -> {
                Voxel3dConfigurator<Object> c = voxel3d();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case HULL3D -> {
                Hull3dConfigurator<Object> c = hull3d();
                applyEnum(g, c::method, BuilderModel.Params.METHOD);
                applyDouble(g, c::radius, BuilderModel.Params.RADIUS);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case RIDGELINE3D -> {
                Ridgeline3dConfigurator<Object> c = ridgeline3d();
                applyEnum(g, c::direction, BuilderModel.Params.DIRECTION);
                applyDouble(g, c::base, BuilderModel.Params.BASE);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case CONTOUR3D -> {
                Contour3dConfigurator<Object> c = contour3d();
                applyInt(g, c::bins, BuilderModel.Params.BINS);
                applyDouble(g, c::alpha, BuilderModel.Params.ALPHA);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case SMOOTH3D -> {
                Smooth3dConfigurator<Object> c = smooth3d();
                applyEnum(g, c::method, BuilderModel.Params.METHOD);
                applyDouble(g, c::span, BuilderModel.Params.SPAN);
                applyFlag(g, c::se, BuilderModel.Params.SE);
                applyDouble(g, c::level, BuilderModel.Params.LEVEL);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case DENSITY3D -> {
                Density3dConfigurator<Object> c = density3d();
                applyDouble(g, c::adjust, BuilderModel.Params.ADJUST);
                applyInt(g, c::n, BuilderModel.Params.N);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case FUNCTION3D -> {
                Function3dConfigurator<Object> c = function3d(BuilderSpec::default3dFunction);
                applyDoubleLimits(g, c::xlim, BuilderModel.Params.XLIM_MIN, BuilderModel.Params.XLIM_MAX);
                applyDoubleLimits(g, c::ylim, BuilderModel.Params.YLIM_MIN, BuilderModel.Params.YLIM_MAX);
                applyInt(g, c::n, BuilderModel.Params.N);
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                yield c;
            }
            case POLYGON -> {
                org.jtaccuino.gog.layer.PolygonConfigurator<Object> c = polygon();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::alpha, BuilderModel.Params.ALPHA);
                applyDouble(g, c::lineWidth, BuilderModel.Params.LINE_WIDTH);
                applyFlag(g, c::filled, BuilderModel.Params.FILLED);
                yield c;
            }
            case TILE -> tile();
            case PATH -> path();
            case STEP -> step();
            case SEGMENT -> segment();
            case CURVE -> curve();
            case CROSSBAR -> {
                CrossbarConfigurator<Object> c = crossbar();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case ERRORBAR -> {
                ErrorbarConfigurator<Object> c = errorbar();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case ERRORBARH -> {
                ErrorbarhConfigurator<Object> c = errorbarh();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case LINERANGE -> {
                LinerangeConfigurator<Object> c = linerange();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                yield c;
            }
            case POINTRANGE -> {
                PointrangeConfigurator<Object> c = pointrange();
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyDouble(g, c::pointRadius, BuilderModel.Params.POINT_RADIUS);
                yield c;
            }
            case RIBBON -> {
                RibbonConfigurator<Object> c = ribbon();
                applyColor(g, c::fill, BuilderModel.Params.FILL);
                applyDouble(g, c::alpha, BuilderModel.Params.ALPHA);
                yield c;
            }
            case HLINE -> {
                HlineConfigurator<Object> c = hline(requiredDouble(g, BuilderModel.Params.YINTERCEPT, 0.0));
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyFlagCall(g, c::dashed, BuilderModel.Params.DASHED);
                yield c;
            }
            case ABLINE -> {
                AblineConfigurator<Object> c = abline(requiredDouble(g, BuilderModel.Params.SLOPE, 0.0),
                        requiredDouble(g, BuilderModel.Params.INTERCEPT, 0.0));
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyFlagCall(g, c::dashed, BuilderModel.Params.DASHED);
                yield c;
            }
            case VLINE -> {
                VlineConfigurator<Object> c = vline(requiredDouble(g, BuilderModel.Params.XINTERCEPT, 0.0));
                applyColor(g, c::color, BuilderModel.Params.COLOR);
                applyDouble(g, c::width, BuilderModel.Params.WIDTH);
                applyFlagCall(g, c::dashed, BuilderModel.Params.DASHED);
                yield c;
            }
        };
    }

    // ─── Param application helpers ───────────────────────────────────────

    private static void applyDouble(BuilderModel.Geom g, DoubleConsumer set, String key) {
        var value = g.doubles.get(key);
        if (value != null) {
            set.accept(value);
        }
    }

    private static void applyDoubles(BuilderModel.Geom g, DoubleConsumer first, DoubleConsumer second,
            String firstKey, String secondKey) {
        applyDouble(g, first, firstKey);
        applyDouble(g, second, secondKey);
    }

    private static void applyDoubles(BuilderModel.Geom g, DoubleConsumer first, DoubleConsumer second,
            DoubleConsumer third, DoubleConsumer fourth, String firstKey, String secondKey,
            String thirdKey, String fourthKey) {
        applyDouble(g, first, firstKey);
        applyDouble(g, second, secondKey);
        applyDouble(g, third, thirdKey);
        applyDouble(g, fourth, fourthKey);
    }

    private static void applyColor(BuilderModel.Geom g, Consumer<Color> set, String key) {
        var value = g.colors.get(key);
        if (value != null) {
            set.accept(value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void applyEnum(BuilderModel.Geom g, Consumer<E> set, String key) {
        var value = g.enums.get(key);
        if (value != null) {
            set.accept((E) value);
        }
    }

    private static void applyFlag(BuilderModel.Geom g, Consumer<Boolean> set, String key) {
        var value = g.flags.get(key);
        if (value != null) {
            set.accept(value);
        }
    }

    private static void applyFlagCall(BuilderModel.Geom g, Runnable run, String key) {
        if (Boolean.TRUE.equals(g.flags.get(key))) {
            run.run();
        }
    }

    private static void applyInt(BuilderModel.Geom g, java.util.function.IntConsumer set, String key) {
        var value = g.doubles.get(key);
        if (value != null) {
            set.accept((int) Math.round(value));
        }
    }

    private static void applyDoubleLimits(BuilderModel.Geom g, java.util.function.BiConsumer<Double, Double> set,
            String minKey, String maxKey) {
        var min = g.doubles.get(minKey);
        var max = g.doubles.get(maxKey);
        if (min != null && max != null) {
            set.accept(min, max);
        }
    }

    /** The default surface the FUNCTION3D layer evaluates: the classic sombrero. */
    private static double default3dFunction(double x, double y) {
        double r = Math.sqrt(x * x + y * y) + 1e-9;
        return Math.sin(r) / r;
    }

    private static void applyPosition(BuilderModel.Geom g, Consumer<Position> set) {
        var value = g.enums.get(BuilderModel.Params.POSITION);
        if (value != null) {
            set.accept((Position) value);
        }
    }

    private static double requiredDouble(BuilderModel.Geom g, String key, double fallback) {
        var value = g.doubles.get(key);
        return value == null ? fallback : value;
    }

    // ─── Presentation ────────────────────────────────────────────────────

    private static void applyScales(Plot<Object> plot, BuilderModel model) {
        switch (model.scaleX) {
            case LOG10 -> plot.scales(scaleXLog10());
            case SQRT -> plot.scales(scaleXSqrt());
            case REVERSE -> plot.scales(scaleXReverse());
            case LIMITS -> {
                if (model.scaleXMin != null && model.scaleXMax != null) {
                    plot.scales(org.jtaccuino.gog.scale.Scales.scaleXLimits(model.scaleXMin, model.scaleXMax));
                }
            }
            case AUTO -> { }
        }
        switch (model.scaleY) {
            case LOG10 -> plot.scales(scaleYLog10());
            case SQRT -> plot.scales(scaleYSqrt());
            case REVERSE -> plot.scales(scaleYReverse());
            case LIMITS -> {
                if (model.scaleYMin != null && model.scaleYMax != null) {
                    plot.scales(org.jtaccuino.gog.scale.Scales.scaleYLimits(model.scaleYMin, model.scaleYMax));
                }
            }
            case AUTO -> { }
        }
        applyColorScale(plot, model, model.colorScale, model.color, model.colorPalette, true);
        applyColorScale(plot, model, model.fillScale, model.fill, model.fillPalette, false);
    }

    private static void applyColorScale(Plot<Object> plot, BuilderModel model,
            BuilderModel.ColorScaleKind kind, String column, String palette, boolean color) {
        switch (kind) {
            case VIRIDIS_C -> plot.scales(color ? scaleColorViridisC() : scaleFillViridisC());
            case VIRIDIS_D -> plot.scales(color ? scaleColorViridisD() : scaleFillViridisD());
            case BREWER -> {
                if (palette != null) {
                    plot.scales(color ? scaleColorBrewer(palette) : scaleFillBrewer(palette));
                }
            }
            case MANUAL -> {
                if (color && column != null && !model.manualColors.isEmpty()) {
                    var scale = scaleColorManual();
                    var values = model.distinctValues(column);
                    for (var i = 0; i < values.size(); i++) {
                        scale.color(values.get(i), manualColor(model, i));
                    }
                    plot.scales(scale);
                }
            }
            case AUTO -> { }
        }
    }

    private static Color manualColor(BuilderModel model, int index) {
        var colors = model.manualColors;
        return colors.get(index % colors.size());
    }

    private static void applyTheme(Plot<Object> plot, BuilderModel model) {
        switch (model.theme) {
            case GRAY -> plot.theme(Theme.theme_gray());
            case BW -> plot.theme(Theme.theme_bw());
            case DARK -> plot.theme(Theme.theme_dark());
        }
        if (!model.showXGrid) {
            plot.theme(t -> t.showXGrid(false));
        }
        if (!model.showYGrid) {
            plot.theme(t -> t.showYGrid(false));
        }
    }

    private static void applyCoord(Plot<Object> plot, BuilderModel model) {
        switch (model.coord) {
            case CARTESIAN -> {
                if ((model.coordXLimMin != null && model.coordXLimMax != null)
                        || (model.coordYLimMin != null && model.coordYLimMax != null)) {
                    var coord = org.jtaccuino.gog.coord.Coord2D.cartesian();
                    if (model.coordXLimMin != null && model.coordXLimMax != null) {
                        coord.xlim(model.coordXLimMin, model.coordXLimMax);
                    }
                    if (model.coordYLimMin != null && model.coordYLimMax != null) {
                        coord.ylim(model.coordYLimMin, model.coordYLimMax);
                    }
                    plot.coord(coord);
                }
            }
            case FLIP -> {
                var coord = Coords.coordFlip();
                if (model.coordXLimMin != null && model.coordXLimMax != null) {
                    coord.xlim(model.coordXLimMin, model.coordXLimMax);
                }
                if (model.coordYLimMin != null && model.coordYLimMax != null) {
                    coord.ylim(model.coordYLimMin, model.coordYLimMax);
                }
                plot.coord(coord);
            }
            case POLAR -> {
                var coord = Coords.coordPolar();
                if (model.polarTheta != null) {
                    coord.theta(model.polarTheta);
                }
                if (model.polarStart != null) {
                    coord.start(model.polarStart);
                }
                plot.coord(coord);
            }
            case EQUAL -> {
                if (model.equalRatio != null) {
                    plot.coord(Coords.coordEqual(model.equalRatio));
                } else {
                    plot.coord(Coords.coordEqual());
                }
            }
            case COORD3D -> {
                var coord = Coords.coord3d();
                if (model.coord3dPitch != null) {
                    coord.pitch(model.coord3dPitch);
                }
                if (model.coord3dRoll != null) {
                    coord.roll(model.coord3dRoll);
                }
                if (model.coord3dYaw != null) {
                    coord.yaw(model.coord3dYaw);
                }
                if (model.coord3dDist != null) {
                    coord.dist(model.coord3dDist);
                }
                if (model.coord3dZoom != null) {
                    coord.zoom(model.coord3dZoom);
                }
                if (model.coord3dPersp != null) {
                    coord.persp(model.coord3dPersp);
                }
                if (model.coord3dScaleMode != null) {
                    coord.scales(model.coord3dScaleMode);
                }
                if (model.coord3dPanels != null) {
                    coord.panels(model.coord3dPanels);
                }
                if (model.coord3dRatioX != null && model.coord3dRatioY != null && model.coord3dRatioZ != null) {
                    coord.ratio(model.coord3dRatioX, model.coord3dRatioY, model.coord3dRatioZ);
                }
                if (model.coord3dClip != null) {
                    coord.clip(model.coord3dClip);
                }
                if (model.coord3dExpand != null) {
                    coord.expand(model.coord3dExpand);
                }
                if (Boolean.TRUE.equals(model.coord3dLight)) {
                    coord.light(org.jtaccuino.gog.coord.Light3d.defaultLight());
                }
                plot.coord(coord);
            }
        }
    }

    private static void applyLabs(Plot<Object> plot, BuilderModel model) {
        if (model.title == null && model.xLabel == null && model.yLabel == null) {
            return;
        }
        plot.labs(labs(model.title, model.xLabel, model.yLabel));
    }

    private static void applyFacets(Plot<Object> plot, BuilderModel model) {
        switch (model.facet) {
            case NONE -> { }
            case WRAP -> {
                if (model.facetWrapColumn != null) {
                    int cols = model.facetWrapCols == null ? -1 : model.facetWrapCols;
                    plot.facets(org.jtaccuino.gog.Facets.wrap(model.facetWrapColumn, cols));
                }
            }
            case GRID -> {
                if (model.facetGridRow != null || model.facetGridCol != null) {
                    plot.facets(org.jtaccuino.gog.Facets.grid(
                            model.facetGridRow, model.facetGridCol, model.toGridOptions()));
                }
            }
        }
    }
}
