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
package org.jtaccuino.gog.dflib.apps;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javax.imageio.ImageIO;
import org.jtaccuino.gog.GgFigure;
import org.jtaccuino.gog.examples.dflib.AnnotatePlots;
import org.jtaccuino.gog.examples.dflib.Annotation3dPlots;
import org.jtaccuino.gog.examples.dflib.AnscombePlots;
import org.jtaccuino.gog.examples.dflib.BarColStatsPlots;
import org.jtaccuino.gog.examples.dflib.ColPlots;
import org.jtaccuino.gog.examples.dflib.ComposedPlots;
import org.jtaccuino.gog.examples.dflib.CoordPlots;
import org.jtaccuino.gog.examples.dflib.CoordView3dArticlePlots;
import org.jtaccuino.gog.examples.dflib.CoordView3dPlots;
import org.jtaccuino.gog.examples.dflib.Density2dPlots;
import org.jtaccuino.gog.examples.dflib.DensityPlots;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots3d;
import org.jtaccuino.gog.examples.dflib.FacetGridPlots;
import org.jtaccuino.gog.examples.dflib.FaithfulPlots;
import org.jtaccuino.gog.examples.dflib.FunctionPlots;
import org.jtaccuino.gog.examples.dflib.GuidePlots;
import org.jtaccuino.gog.examples.dflib.GwasPlots;
import org.jtaccuino.gog.examples.dflib.LayerPlots;
import org.jtaccuino.gog.examples.dflib.Lighting3dPlots;
import org.jtaccuino.gog.examples.dflib.LineTypePlots;
import org.jtaccuino.gog.examples.dflib.MatrixPlots;
import org.jtaccuino.gog.examples.dflib.MeatPlots;
import org.jtaccuino.gog.examples.dflib.Mixing3dPlots;
import org.jtaccuino.gog.examples.dflib.MpgPlots;
import org.jtaccuino.gog.examples.dflib.MtcarsPlots;
import org.jtaccuino.gog.examples.dflib.OrbitPlots;
import org.jtaccuino.gog.examples.dflib.Plot3dPlots;
import org.jtaccuino.gog.examples.dflib.PolarLinesPlots;
import org.jtaccuino.gog.examples.dflib.PolarPlots;
import org.jtaccuino.gog.examples.dflib.PolarRadarPlots;
import org.jtaccuino.gog.examples.dflib.PolarRadialPlots;
import org.jtaccuino.gog.examples.dflib.ScalePlots;
import org.jtaccuino.gog.examples.dflib.StatPlots;
import org.jtaccuino.gog.examples.dflib.SummaryStatPlots;
import org.jtaccuino.gog.examples.dflib.Surface3dPlots;
import org.jtaccuino.gog.examples.dflib.TickGridPlots;
import org.jtaccuino.gog.examples.dflib.ViolinPlots;
import org.jtaccuino.gog.examples.dflib.Volume3dPlots;
import org.jtaccuino.gog.examples.dflib.WirePlots;
import org.jtaccuino.gog.render.SvgExporter;

/**
 * Renders every example plot to both PNG and SVG without opening a window, so
 * the documentation gallery can be regenerated reproducibly and the raster and
 * vector output of each figure can be compared side by side.
 * <p>
 * Run it with the output directory as the first argument:
 * <pre>{@code
 * ./gradlew exportDocs
 * }</pre>
 * A plot renders itself during JavaFX layout, so each one is placed in an
 * off-screen {@link Scene} that is laid out but never shown.
 */
public class PlotExporter {

    /** Supersampling factor; the scene renders large and the PNG keeps that resolution. */
    private static final double SCALE = 2.0;

    /**
     * Layers with more shapes than this are embedded in the SVG as a raster
     * image. Emitting the genome-wide scatter as vector would run to tens of
     * megabytes and defeat most vector editors.
     */
    private static final int RASTERIZE_ABOVE = 50_000;

    /** Subdirectory holding the gallery thumbnails. */
    private static final String THUMB_DIR = "thumbs";

    /**
     * Thumbnail width in pixels. The gallery lays figures out in two columns of
     * roughly 400 px, so this still has pixels to spare on a high-density display
     * while costing a fraction of the full rendering.
     */
    private static final int THUMB_WIDTH = 640;

    /** Canvas that suits an ordinary single-panel chart. */
    private static final int W = 900, H = 560;

    /** Faceted plots need more room per panel. */
    private static final int FACET_W = 1150, FACET_H = 720;

    /** 3D projections read better on a squarer canvas. */
    private static final int CUBE_W = 900, CUBE_H = 700;

    /** Composed 3-D grids hold square cubes, so a single row of them needs a
     * height close to the cell width, not a tall canvas; these keep the one-row
     * compositions compact (no large unused band below the cubes). */
    private static final int CUBE_2COL_W = 900, CUBE_2COL_H = 520;
    private static final int CUBE_3COL_W = 1150, CUBE_3COL_H = 400;

    /**
     * The gallery's sections, in presentation order: one per geometry, each with
     * the sentence that introduces it.
     */
    private static final Map<String, String> SECTIONS = new LinkedHashMap<>();

    /** Utility class; not meant to be instantiated. */
    private PlotExporter() {
    }

    /**
     * One entry of the gallery: the figure, the canvas it looks best on, and
     * where it belongs in the index. Genome-wide views want a wide panel, a
     * quantile-quantile plot a square one. Figures are any {@link GgFigure} — a
     * single {@code Plot} or a composite {@code PlotMatrix}.
     *
     * @param plot    the figure to render
     * @param width   logical canvas width in pixels
     * @param height  logical canvas height in pixels
     * @param section the geometry this figure demonstrates, keyed into {@link #SECTIONS}
     * @param caption the one-line description shown under the thumbnail
     */
    private record Figure(GgFigure plot, int width, int height, String section, String caption) {
    }

    static {
        SECTIONS.put("point", "`Geoms.point()` — scatter plots, with colour, shape, and opacity mapped from data.");
        SECTIONS.put("smooth", "`Geoms.smooth()` — LOESS and linear-model trend lines, with optional confidence bands.");
        SECTIONS.put("stat", "`Stats.smooth()` — the statistical transformation behind the smoother, used directly as a pure data transform.");
        SECTIONS.put("stat-summary", "`Stats.bin()`/`Stats.summary()` — aggregated distributions and ranges: `Geoms.histogram()`, `Geoms.freqpoly()`, and the error/range family (`Geoms.errorbar()`, `Geoms.crossbar()`, `Geoms.pointrange()`, `Geoms.ribbon()`).");
        SECTIONS.put("line", "`Geoms.line()` — connected time series, optionally averaged.");
        SECTIONS.put("bar", "`Geoms.bar()` — bars whose heights are the observation counts per x category (`Stats.count()`), plus computed `afterStat()` fills.");
        SECTIONS.put("col", "`Geoms.col()` — columns whose heights are the values in the data, stacked, dodged, and horizontal.");
        SECTIONS.put("bar-col-stats", "`Geoms.bar()` vs `Geoms.col()` — the `Stats.count()`/`Stats.identity()` split and how each geometry can be overridden to the other's statistic.");
        SECTIONS.put("area", "`Geoms.area()` — filled areas over a continuous axis.");
        SECTIONS.put("boxplot", "`Geoms.boxplot()` — Tukey summaries, with notches, variable width, and outlier control.");
        SECTIONS.put("jitter", "`Geoms.jitter()` — deterministic jitter against overplotting.");
        SECTIONS.put("violin", "`Geoms.violin()` — mirrored density plots, with trimming, bandwidth, and area/count/width scaling.");
        SECTIONS.put("density", "`Geoms.density()` — smoothed kernel-density estimates, with bandwidth, boundary correction, and stack/fill positions.");
        SECTIONS.put("density2d", "`Geoms.density2d()` — contours of a two-dimensional kernel-density estimate, as lines or filled bands.");
        SECTIONS.put("function", "`Geoms.function()` — mathematical curves drawn from a function, alone or overlaid on data.");
        SECTIONS.put("tile", "`Geoms.tile()` — heatmaps with a continuous colour scale.");
        SECTIONS.put("point3d", "`Geoms.point3d()` — three-dimensional scatter under `Coord3D`.");
        SECTIONS.put("3d-primitives", "`Geoms.segment3d()`, `Geoms.path3d()`, `Geoms.text3d()` — the 3-D primitives: per-row segments, grouped paths, billboard labels, the reference circles/lines of `Geoms.point3d()`, and the data-free `Annotations3d` family.");
        SECTIONS.put("3d-surfaces", "`Geoms.surface3d()`, `Geoms.function3d()`, `Geoms.density3d()`, `Geoms.ridgeline3d()`, `Geoms.contour3d()`, `Geoms.smooth3d()` — the 3-D surfaces: grid/Delaunay tiles, function surfaces, 2-D kernel densities, ridge lines, contour bands, and fitted smooth surfaces with confidence panels.");
        SECTIONS.put("3d-volumes", "`Geoms.col3d()`, `Geoms.bar3d()`, `Geoms.voxel3d()`, `Geoms.hull3d()` — the 3-D volumes: grid columns, counted/binned bars, sparse voxels, and convex or alpha surface hulls.");
        SECTIONS.put("3d-lighting", "`light(...)` — the 3-D lighting across the polygon family: the `none`/`diffuse`/`direct`/`rgb` methods, HSL vs HSV blending, contrast, the light direction, a camera-anchored light, a positional light with falloff, back-face shading, a light supplied through `coord3d()`, `Geoms.surface3d()`/`Geoms.function3d()` surfaces lit from the plot level, and the 3-D-shaded `Guides.guideColorbar3d()`/`Guides.guideLegend3d()` guides.");
        SECTIONS.put("3d-mixing", "`Positions.positionOnFace()` — mixing 2D and 3D layers on the cube: `Geoms.density2d()` contours and `Geoms.tile()` heatmaps placed on a face, and a 3D point cloud flattened onto a face.");
        SECTIONS.put("coord3d", "`Coords.coord3d()` — the 3-D view: rotation, camera, scales/ratio, panels, clip, expand, and axis furniture.");
        SECTIONS.put("coord3d-article", "The `coord3d()` view-control figures — a shared `Geoms.function3d(...)` surface coloured by z, composed into multi-panel grids: rotation, perspective/distance, scales/ratio, panels, zoom/clip/expand, label placement, and theming.");
        SECTIONS.put("3d-dsl", "`plot3d()`/`ggplot3d()` — the discoverable 3D entry points and the `PlotDescriptor3D` cube DSL: default view, rotation with hidden panels, and a plot-level light.");
        SECTIONS.put("3d-orbit", "`Orbit3d` — drag-to-rotate 3-D plots and the figure-wide dirty-repaint contract (`markDirty()`): a gesture showcase on the `SpherePointsDatasets` cloud, the `CubeOrbitControl` mini-globe on a lit `MountainDatasets` surface, plus the surface and convex-hull completeness figures over the demo data.");
        SECTIONS.put("reference", "`Geoms.hline()`, `Geoms.vline()`, `Geoms.abline()`, `Geoms.text()` — reference lines and annotations.");
        SECTIONS.put("guide", "guides — legend placement, key grids, reversal, and the legend box.");
        SECTIONS.put("scales", "scales — reversed axes, named colour ramps and gradients, manual shape/size maps, axis limits, and binned continuous colour scales.");        SECTIONS.put("facet-grid", "`Facets.grid()` — a matrix of panels spanned by row and column faceting variables, with free scales, free space, margins, and strip switching.");
        SECTIONS.put("polar", "`coordPolar()` — angular coordinates: coxcombs, pies, bulls-eyes, and donuts.");
        SECTIONS.put("layer", "`Plot#layer(...)` — all-in-one layers assembling a geom, stat, position, mapping, and params, plus `aesOf(map)` and computed `afterStat()` aesthetics.");
        SECTIONS.put("coords", "`coordEqual()` and `coordTrans()` — fixed-aspect and per-axis scale-transformed coordinate systems.");
        SECTIONS.put("annotate", "`annotate()` — data-free annotation layers, and log axes that automatically draw their sub-decade minor ticks.");
        SECTIONS.put("grid", "ticks & grid — the theme's major/minor grid and tick configuration surface, with conventional defaults and per-theme overrides.");
        SECTIONS.put("matrix", "`matrixPlot()` — an <i>n</i>×<i>n</i> matrix over several variables, dispatching every cell from the column types: densities/scatters/correlations for numeric pairs, box plots and faceted histograms at the numeric/categorical boundary, count bars for categorical pairs (generalized-pairs).");
        SECTIONS.put("composed", "`composedPlot()` — multi-plot composition: an aligned grid of several figures with a shared title and an optional collected legend.");
    }

    /**
     * Renders every registered example plot to a PNG.
     *
     * @param args optional output directory; defaults to {@code docs/images}
     * @throws InterruptedException if interrupted while waiting for the JavaFX thread
     */
    public static void main(String[] args) throws InterruptedException {
        var outputDir = new File(args.length > 0 ? args[0] : "docs/images");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Could not create output directory " + outputDir);
        }

        // The exporter only ever renders off-screen, so it can run on the JavaFX 26
        // headless glass platform — no display, no window manager, identical on a
        // developer machine and a CI runner. The headless glass has no GPU
        // pipeline, so it renders through the software prism, which also sidesteps
        // the GPU texture budget a long headless run would otherwise exhaust (the
        // blank dense-scatter layers seen before). Explicit -D overrides win.
        System.setProperty("glass.platform", System.getProperty("glass.platform", "headless"));
        System.setProperty("prism.order", System.getProperty("prism.order", "sw"));

        var done = new CountDownLatch(1);
        var failure = new Throwable[1];

        Platform.startup(() -> {
            try {
                var thumbDir = new File(outputDir, THUMB_DIR);
                if (!thumbDir.exists() && !thumbDir.mkdirs()) {
                    throw new IllegalStateException("Could not create " + thumbDir);
                }

                for (var entry : gallery().entrySet()) {
                    var figure = entry.getValue();
                    var rendered = render(figure);

                    var png = new File(outputDir, entry.getKey() + ".png");
                    write(rendered, png);

                    // The gallery shows these rather than the full images: at two
                    // columns a browser would otherwise download several times the
                    // pixels it displays.
                    var thumb = new File(thumbDir, entry.getKey() + ".png");
                    write(downscale(rendered, THUMB_WIDTH), thumb);

                    // The vector twin: dense scatter layers are embedded as a raster
                    // so the file stays openable, everything else stays editable.
                    var svg = new File(outputDir, entry.getKey() + ".svg");
                    new SvgExporter()
                            .size(figure.width(), figure.height())
                            .rasterizeAbove(RASTERIZE_ABOVE)
                            .write(figure.plot(), svg.toPath());

                    System.out.printf("%-34s %,10d PNG  %,8d thumb  %,10d SVG%n",
                                      entry.getKey(), png.length(), thumb.length(), svg.length());

                    // The WritableImage captured by render() backs a native texture
                    // the runtime holds in a soft cache until memory pressure. With
                    // dozens of figures in one process those accumulate unless we
                    // give the collector a nudge after each one, so the dense
                    // scatter layers towards the end of the run still fit.
                    sweep();
                }
                writeGalleryIndex(outputDir);
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                done.countDown();
            }
        });

        done.await();
        Platform.exit();

        if (failure[0] != null) {
            throw new IllegalStateException("Plot export failed", failure[0]);
        }
        System.out.println("Wrote gallery to " + Path.of(outputDir.getAbsolutePath()).normalize());
        // The headless JavaFX runtime keeps a non-daemon thread alive after
        // Platform.exit(), so terminate the process explicitly once the gallery
        // is on disk.
        System.exit(0);
    }

    /**
     * Prompts collection so the runtime can drop native texture buffers that the
     * previous figure's snapshot still holds in a soft cache. The loop runs on a
     * single JavaFX thread, so without this nudge one texture per figure stays
     * resident for the whole run and the later, dense figures run out of room.
     */
    private static void sweep() {
        System.gc();
    }

    /**
     * The plots to export, keyed by output file name.
     *
     * @return an ordered map of file name to figure
     */
    private static Map<String, Figure> gallery() {
        var plots = new LinkedHashMap<String, Figure>();

        // --- Geoms.point() ---
        plots.put("anscombe-1-raw-scatter", new Figure(AnscombePlots.createRawScatterPlot(), W, H,
                "point", "Anscombe's quartet, plotted raw"));
        plots.put("anscombe-2-colour-mapped", new Figure(AnscombePlots.createColorMappedPlot(), W, H,
                "point", "The same data with colour mapped to the dataset"));
        plots.put("diamonds-01-scatter", new Figure(DiamondsPlots.createScatterPlot(), W, H,
                "point", "54,000 diamonds: price against carat"));
        plots.put("diamonds-02-by-clarity", new Figure(DiamondsPlots.createColorByClarity(), W, H,
                "point", "Colour mapped to clarity, eight categories"));
        plots.put("diamonds-07-alpha", new Figure(DiamondsPlots.createAlphaPlot(), W, H,
                "point", "Opacity 0.15 to reveal density in overplotted regions"));
        plots.put("diamonds-04-facet-colour", new Figure(DiamondsPlots.createFacetByColor(), FACET_W, FACET_H,
                "point", "Faceted by colour grade"));
        plots.put("diamonds-08-facet-clarity-cut", new Figure(DiamondsPlots.createFacetClarityColorCut(), FACET_W, FACET_H,
                "point", "Faceted by clarity, coloured by cut"));
        plots.put("mpg-06-dual-aesthetics", new Figure(MpgPlots.createDualMappingPlot(), W, H,
                "point", "Colour and shape mapped to the same variable"));

        // --- Geoms.smooth() ---
        plots.put("mpg-01-smooth-loess", new Figure(MpgPlots.createStandardSmooth(), W, H,
                "smooth", "LOESS conditional mean with confidence band"));
        plots.put("mpg-02-smooth-span", new Figure(MpgPlots.createWiggleySmooth(), W, H,
                "smooth", "A tighter bandwidth, span = 0.3"));
        plots.put("mpg-03-smooth-no-se", new Figure(MpgPlots.createNoSeSmooth(), W, H,
                "smooth", "Linear model without the confidence band"));
        plots.put("mpg-04-linear-models", new Figure(MpgPlots.createLinearModelGrouped(), W, H,
                "smooth", "One linear model per drive type"));
        plots.put("mpg-05-local-smooth", new Figure(MpgPlots.createLocalAesSmooth(), W, H,
                "smooth", "Global points with a per-group trend via local aesthetics"));
        plots.put("mpg-08-two-local-smooth", new Figure(MpgPlots.createTwoLocalSmoothMappings(), W, H,
                "smooth", "Two smooth layers, each with its own per-geom colour mapping"));
        plots.put("anscombe-3-faceted", new Figure(AnscombePlots.createFinalFacettedPlot(), FACET_W, FACET_H,
                "smooth", "The quartet faceted, each with its own regression"));
        plots.put("diamonds-05-smooth-loess", new Figure(DiamondsPlots.createSmoothLoess(), W, H,
                "smooth", "LOESS over 54,000 observations"));
        plots.put("diamonds-06-smooth-linear", new Figure(DiamondsPlots.createSmoothLinear(), W, H,
                "smooth", "Linear model over the same data"));
        plots.put("diamonds-03-facet-clarity", new Figure(DiamondsPlots.createFacetByClarity(), FACET_W, FACET_H,
                "smooth", "Faceted by clarity, each panel smoothed"));
        plots.put("meat-1-trend-line", new Figure(MeatPlots.createSingleTrendLine(), W, H,
                "smooth", "Square points with a linear trend over a date axis"));

        // --- Geoms.line() ---
        plots.put("meat-2-multiple-trends", new Figure(MeatPlots.createMultipleTrendLines(), W, H,
                "line", "One series per animal, colour mapped"));
        plots.put("meat-3-faceted", new Figure(MeatPlots.createFinalFacettedPlot(), FACET_W, FACET_H,
                "line", "The same series faceted into small multiples"));

        // --- Geoms.col() (identity columns) ---
        plots.put("meat-4-bar-chart", new Figure(MeatPlots.createBarChartComparison(), W, H,
                "col", "Dodged columns comparing production"));
        plots.put("meat-5-horizontal-bars", new Figure(MeatPlots.createHorizontalBarChart(), W, H,
                "col", "The same columns under `coordFlip()`"));
        plots.put("meat-6-dodge-bars", new Figure(MeatPlots.createHorizontalDodgeBarChart(), W, H,
                "col", "Horizontal columns, dodged by group"));
        plots.put("meat-8-stacked-bars", new Figure(MeatPlots.createStackedBarChart(), W, H,
                "col", "Stacked columns accumulating total production"));
        plots.put("mtcars-faceted-stacked-bars", new Figure(MtcarsPlots.createFacetedStackedBar(), FACET_W, FACET_H,
                "col", "Faceted, stacked columns — cars by gears, stacked by transmission"));

        // --- Geoms.col() ---
        plots.put("mpg-col-01-basic", new Figure(ColPlots.createBasicCol(), W, H,
                "col", "Columns whose heights are the values in the data — `Geoms.col()` leaves the data as is"));
        plots.put("mpg-col-02-points", new Figure(ColPlots.createColVsPoint(), W, H,
                "col", "The same values as points, which need no zero baseline"));
        plots.put("mpg-col-03-stacked", new Figure(ColPlots.createStackedCol(), W, H,
                "col", "Stacked columns — mean mileage by class, split and piled by drive type"));
        plots.put("mpg-col-04-dodged", new Figure(ColPlots.createDodgedCol(), W, H,
                "col", "Dodged columns — the drive-type segments side by side"));
        plots.put("mpg-col-04b-dodge-width", new Figure(ColPlots.createDodgedColWidth(), W, H,
                "col", "Dodged columns with an explicit `Positions.dodge(width = 0.5)`"));
        plots.put("diamonds-col-05-horizontal", new Figure(ColPlots.createHorizontalCol(), W, H,
                "col", "Horizontal columns — mean diamond price per cut under `coordFlip()`"));
        plots.put("meat-col-06-time-axis", new Figure(ColPlots.createTimeAxisCol(), W, H,
                "col", "Columns over a time axis — total annual meat production"));

        // --- Geoms.bar() (Stats.count) ---
        plots.put("bar-01-class-count", new Figure(BarColStatsPlots.createClassCount(), W, H,
                "bar", "`Geoms.bar()` counts the rows per class — the bar height is the count"));
        plots.put("bar-02-cyl-count", new Figure(BarColStatsPlots.createCylCount(), W, H,
                "bar", "`Geoms.bar()` fill = `afterStat(count)` — bars coloured by their count"));

        // --- Geoms.bar() vs Geoms.col() statistics ---
        plots.put("barc-stats-01-bar-counts", new Figure(BarColStatsPlots.createCylPlainCount(), W, H,
                "bar-col-stats", "`Geoms.bar()` runs `Stats.count()` — count the rows per cylinder"));
        plots.put("barc-stats-02-col-values", new Figure(BarColStatsPlots.createMeanMpgCol(), W, H,
                "bar-col-stats", "`Geoms.col()` runs `Stats.identity()` — the y values are the heights"));
        plots.put("barc-stats-03-bar-identity", new Figure(BarColStatsPlots.createBarIdentityOverride(), W, H,
                "bar-col-stats", "`Geoms.bar(stat = \"identity\")` draws raw values, exactly like `Geoms.col()`"));
        plots.put("barc-stats-04-col-count", new Figure(BarColStatsPlots.createColCountOverride(), W, H,
                "bar-col-stats", "`Geoms.col(stat = \"count\")` counts rows, exactly like `Geoms.bar()`"));
        plots.put("barc-stats-05-bar-after-stat-prop", new Figure(BarColStatsPlots.createAfterStatProp(), W, H,
                "bar-col-stats", "`Geoms.bar()` fill = `afterStat(prop)` — bars coloured by their share"));

        // --- Geoms.area() ---
        plots.put("meat-7-area-chart", new Figure(MeatPlots.createAreaChart(), W, H,
                "area", "Production density over time"));

        // --- Geoms.boxplot() ---
        plots.put("mpg-08-boxplot", new Figure(MpgPlots.createBoxplot(), W, H,
                "boxplot", "Highway mileage by vehicle class"));
        plots.put("mpg-09-boxplot-horizontal", new Figure(MpgPlots.createHorizontalBoxplot(), W, H,
                "boxplot", "Horizontal orientation via `coordFlip()`"));
        plots.put("mpg-10-boxplot-notched", new Figure(MpgPlots.createNotchedBoxplot(), W, H,
                "boxplot", "Notched boxes indicating median confidence"));
        plots.put("mpg-11-boxplot-varwidth", new Figure(MpgPlots.createVarWidthBoxplot(), W, H,
                "boxplot", "Box width proportional to group size"));
        plots.put("mpg-12-boxplot-diamond-outliers", new Figure(MpgPlots.createOutlierShapeBoxplot(), W, H,
                "boxplot", "Outliers drawn as diamonds"));
        plots.put("mpg-13-boxplot-no-outliers", new Figure(MpgPlots.createNoOutlierBoxplot(), W, H,
                "boxplot", "Outliers suppressed entirely"));

        // --- Geoms.jitter() ---
        plots.put("mpg-14-boxplot-jitter", new Figure(MpgPlots.createBoxplotJitter(), W, H,
                "jitter", "Raw observations jittered over a boxplot"));
        plots.put("mpg-15-jitter", new Figure(MpgPlots.createDefaultJitter(), W, H,
                "jitter", "Default jitter width"));
        plots.put("mpg-16-jitter-coloured", new Figure(MpgPlots.createColoredJitter(), W, H,
                "jitter", "Jittered points coloured by class"));
        plots.put("mpg-17-jitter-narrow", new Figure(MpgPlots.createNarrowJitter(), W, H,
                "jitter", "Narrow jitter, width = 0.25"));
        plots.put("mpg-18-jitter-wide", new Figure(MpgPlots.createWideJitter(), W, H,
                "jitter", "Wide jitter in both directions"));
        plots.put("mpg-19-boxplot-fill-dodge", new Figure(MpgPlots.createFillMappedBoxplot(), W, H,
                "boxplot", "Boxes split per drive type, dodged side by side"));

        // --- Geoms.violin() ---
        plots.put("mpg-violin-01-basic", new Figure(ViolinPlots.createBasicViolin(), W, H,
                "violin", "Highway mileage distribution per vehicle class"));
        plots.put("mpg-violin-02-horizontal", new Figure(ViolinPlots.createHorizontalViolin(), W, H,
                "violin", "Horizontal orientation via `coordFlip()`"));
        plots.put("mpg-violin-03-jitter", new Figure(ViolinPlots.createViolinJitter(), W, H,
                "violin", "Raw observations jittered over the violins"));
        plots.put("mpg-violin-04-scale-count", new Figure(ViolinPlots.createScaleCount(), W, H,
                "violin", "Area proportional to sample size — `scale = 'count'`"));
        plots.put("mpg-violin-05-scale-width", new Figure(ViolinPlots.createScaleWidth(), W, H,
                "violin", "Uniform maximum width — `scale = 'width'`"));
        plots.put("mpg-violin-06-trim-false", new Figure(ViolinPlots.createTrimFalse(), W, H,
                "violin", "Kernel tails untrimmed — `trim = false`"));
        plots.put("mpg-violin-07-adjust", new Figure(ViolinPlots.createAdjustHalf(), W, H,
                "violin", "A smaller bandwidth for a closer fit — `adjust = 0.5`"));
        plots.put("mpg-violin-08-fill-mapped", new Figure(ViolinPlots.createFillMapped(), W, H,
                "violin", "Fill mapped to drive type"));
        plots.put("mpg-violin-09-fixed-style", new Figure(ViolinPlots.createFixedStyle(), W, H,
                "violin", "Fixed grey fill with a blue outline"));
        plots.put("mpg-violin-10-quantiles", new Figure(ViolinPlots.createDrawQuantiles(), W, H,
                "violin", "Quartile marks — `quantileDrawing = c(0.25, 0.5, 0.75)`"));

        // --- Geoms.density() ---
        plots.put("diamonds-density-01-basic", new Figure(DensityPlots.createBasicDensity(), W, H,
                "density", "Diamond carat density, the smoothed histogram alternative"));
        plots.put("diamonds-density-02-flipped", new Figure(DensityPlots.createFlippedDensity(), W, H,
                "density", "Flipped orientation — `aes(y = carat)` runs the estimate along x"));
        plots.put("diamonds-density-03-adjust-small", new Figure(DensityPlots.createAdjustSmall(), W, H,
                "density", "A tighter bandwidth for a closer fit — `adjust = 1/5`"));
        plots.put("diamonds-density-04-adjust-large", new Figure(DensityPlots.createAdjustLarge(), W, H,
                "density", "A wider bandwidth for a smoother estimate — `adjust = 5`"));
        plots.put("diamonds-density-05-colour-mapped", new Figure(DensityPlots.createColourMapped(), W, H,
                "density", "One density per cut, colour mapped, zoomed to `xlim(55, 70)`"));
        plots.put("diamonds-density-06-fill-mapped", new Figure(DensityPlots.createFillMapped(), W, H,
                "density", "Overlapping filled densities — `alpha = 0.1`"));
        plots.put("diamonds-density-07-bounds", new Figure(DensityPlots.createBounds(), W, H,
                "density", "Boundary correction — `bounds = c(1, Inf)` vs the plain estimate"));
        plots.put("diamonds-density-08-stacked", new Figure(DensityPlots.createStacked(), W, H,
                "density", "Stacked densities — `position = 'stack'`"));
        plots.put("diamonds-density-09-stacked-count", new Figure(DensityPlots.createStackedCount(), W, H,
                "density", "Stacked count densities — `afterStat(count)` preserves marginal densities"));
        plots.put("diamonds-density-10-fill-position", new Figure(DensityPlots.createFillPosition(), W, H,
                "density", "Conditional density — `position = 'fill'`"));

        // --- GeomDensity2d() ---
        plots.put("faithful-density2d-01-basic", new Figure(Density2dPlots.createBasic(), W, H,
                "density2d", "Contours of eruption density over the raw observations"));
        plots.put("faithful-density2d-02-zoom", new Figure(Density2dPlots.createZoomed(), W, H,
                "density2d", "Zoomed to the two eruption clusters with more contours"));
        plots.put("faithful-density2d-03-adjust", new Figure(Density2dPlots.createAdjust(), W, H,
                "density2d", "A wider bandwidth smooths the two-regime separation — `adjust = 2`"));
        plots.put("diamonds-density2d-04-colour-mapped", new Figure(Density2dPlots.createColourMapped(), W, H,
                "density2d", "One contour set per cut — `colourMapping = cut)`"));
        plots.put("faithful-density2d-05-filled", new Figure(Density2dPlots.createFilled(), W, H,
                "density2d", "Filled contour bands — `Geoms.density2dFilled(alpha = 0.5)`"));
        plots.put("faithful-density2d-06-filled-lines", new Figure(Density2dPlots.createFilledLines(), W, H,
                "density2d", "Filled bands under thin black contour lines"));
        plots.put("mpg-density2d-07-bins", new Figure(Density2dPlots.createMpgBins(), W, H,
                "density2d", "Dense contours of fuel economy — `bins = 15`"));

        // --- Geoms.function() ---
        plots.put("function-01-overlay", new Figure(FunctionPlots.createOverlayOnDensity(), W, H,
                "function", "The standard normal density overlaid on the empirical density"));
        plots.put("function-02-alone", new Figure(FunctionPlots.createFunctionAlone(), W, H,
                "function", "`Geoms.function(fun = normalDensity)` alone, with `xlim(-5, 5)` setting the x axis"));
        plots.put("function-03-shifted", new Figure(FunctionPlots.createShiftedGaussian(), W, H,
                "function", "`fun = normalDensity(x, 2, .5)` — a shifted and narrowed Gaussian"));
        plots.put("function-04-two", new Figure(FunctionPlots.createTwoFunctions(), W, H,
                "function", "Two distributions — normal against a t with `df = 1`"));
        plots.put("function-05-anonymous", new Figure(FunctionPlots.createAnonymousFunction(), W, H,
                "function", "A bespoke anonymous function: `\\(x) 0.5 * exp(-|x|)`"));
        plots.put("function-06-restrict", new Figure(FunctionPlots.createRestrictedXlim(), W, H,
                "function", "The overlaid curve restricted to `xlim(-1, 1)`"));
        plots.put("function-07-widen", new Figure(FunctionPlots.createWidenedXlim(), W, H,
                "function", "The overlaid curve widened to `xlim(-7, 7)`"));
        plots.put("function-08-bessel", new Figure(FunctionPlots.createBesselFunctions(), W, H,
                "function", "Bessel functions J0 and J2, each stroked via a per-geom colour label legend"));

        // --- Geoms.tile() ---
        plots.put("faithful-1-heatmap", new Figure(FaithfulPlots.createHeatmap(), W, H,
                "tile", "Old Faithful eruption density, viridis colour map"));
        plots.put("meat-9-heatmap", new Figure(MeatPlots.createHeatmap(), W, H,
                "tile", "Meat consumption as a heatmap"));

        // --- Geoms.point3d() ---
        plots.put("mpg-07-scatter-3d", new Figure(MpgPlots.create3dScatterPlot(), CUBE_W, CUBE_H,
                "point3d", "Displacement, mileage, and drive type in three axes"));
        plots.put("diamonds-09-scatter-3d", new Figure(DiamondsPlots3d.create3dScatterPriceCaratDepth(), CUBE_W, CUBE_H,
                "point3d", "Price against carat and depth"));
        plots.put("diamonds-10-scatter-3d-depth", new Figure(DiamondsPlots3d.create3dScatterDepthPriceTable(), CUBE_W, CUBE_H,
                "point3d", "Depth, table, and price"));

        // --- 3-D primitives ---
        plots.put("3d-lines", new Figure(Annotation3dPlots.createRefLines(), CUBE_W, CUBE_H,
                "3d-primitives", "Reference lines from each point down to the zmin face"));
        plots.put("3d-circles", new Figure(Annotation3dPlots.createRefCircles(), CUBE_W, CUBE_H,
                "3d-primitives", "Reference circles lying flat on the zmin face"));
        plots.put("3d-segments", new Figure(Annotation3dPlots.createSegments3d(), CUBE_W, CUBE_H,
                "3d-primitives", "One directed segment per row — `Geoms.segment3d()`"));
        plots.put("3d-path", new Figure(Annotation3dPlots.createPath3d(), CUBE_W, CUBE_H,
                "3d-primitives", "A winding path through the cube — `Geoms.path3d()`"));
        plots.put("3d-text", new Figure(Annotation3dPlots.createText3d(), CUBE_W, CUBE_H,
                "3d-primitives", "Billboard cut labels — `Geoms.text3d()`"));
        plots.put("3d-annotate", new Figure(Annotation3dPlots.createAnnotate3d(), CUBE_W, CUBE_H,
                "3d-primitives", "The data-free `Annotations3d` point, text, and segment layers"));

        // --- 3-D surfaces ---
        plots.put("surface-grid", new Figure(Surface3dPlots.createGridSurface(), CUBE_W, CUBE_H,
                "3d-surfaces", "Sombrero from a regular grid — `Geoms.surface3d()`"));
        plots.put("surface-right", new Figure(Surface3dPlots.createGridSurfaceRight(), CUBE_W, CUBE_H,
                "3d-surfaces", "Right-triangle tiles — `Geoms.surface3d().grid(GridGeometry.RIGHT1)`"));
        plots.put("surface-function", new Figure(Surface3dPlots.createFunctionSurface(), CUBE_W, CUBE_H,
                "3d-surfaces", "Sombrero evaluated over a grid — `Geoms.function3d()`"));
        plots.put("surface-density", new Figure(Surface3dPlots.createDensitySurface(), CUBE_W, CUBE_H,
                "3d-surfaces", "2-D kernel density surface — `Geoms.density3d()`"));
        plots.put("surface-ridgeline", new Figure(Surface3dPlots.createRidgeline(), CUBE_W, CUBE_H,
                "3d-surfaces", "Gaussian ridges — `Geoms.ridgeline3d()`"));
        plots.put("surface-contour", new Figure(Surface3dPlots.createContour(), CUBE_W, CUBE_H,
                "3d-surfaces", "Contour bands — `Geoms.contour3d()`"));
        plots.put("surface-smooth", new Figure(Surface3dPlots.createSmooth(), CUBE_W, CUBE_H,
                "3d-surfaces", "Loess surface with se panels, points, residuals — `Geoms.smooth3d()`"));
        plots.put("surface-smooth-lm", new Figure(Surface3dPlots.createSmoothLm(), CUBE_W, CUBE_H,
                "3d-surfaces", "Full-domain lm plane — `Geoms.smooth3d().method(SmoothMethod.LM)`"));

        // --- 3-D volumes ---
        plots.put("volume-col-grid", new Figure(Volume3dPlots.createColumns(), CUBE_W, CUBE_H,
                "3d-volumes", "Grid columns — `Geoms.col3d()`"));
        plots.put("volume-col-zmin", new Figure(Volume3dPlots.createColumnsZminGaps(), CUBE_W, CUBE_H,
                "3d-volumes", "Columns with a base level and gaps — `Geoms.col3d().zmin(...).width(...)`"));
        plots.put("volume-col-faces", new Figure(Volume3dPlots.createColumnsFaces(), CUBE_W, CUBE_H,
                "3d-volumes", "A subset of column faces — `Geoms.col3d().faces(...)`"));
        plots.put("volume-bar-discrete", new Figure(Volume3dPlots.createBarsDiscrete(), CUBE_W, CUBE_H,
                "3d-volumes", "Discrete count bars — `Geoms.bar3d()`"));
        plots.put("volume-bar-histogram", new Figure(Volume3dPlots.createBarsContinuous(), CUBE_W, CUBE_H,
                "3d-volumes", "2-D histogram bars — `Geoms.bar3d().bins(...)`"));
        plots.put("volume-voxel", new Figure(Volume3dPlots.createVoxels(), CUBE_W, CUBE_H,
                "3d-volumes", "Sparse voxels — `Geoms.voxel3d()`"));
        plots.put("volume-hull-convex", new Figure(Volume3dPlots.createHullConvex(), CUBE_W, CUBE_H,
                "3d-volumes", "Convex hull of a sphere — `Geoms.hull3d().method(HullMethod.CONVEX)`"));
        plots.put("volume-hull-alpha", new Figure(Volume3dPlots.createHullAlpha(), CUBE_W, CUBE_H,
                "3d-volumes", "Alpha shape of a torus — `Geoms.hull3d().method(HullMethod.ALPHA)`"));

        // --- 3-D lighting ---
        plots.put("light-method-none", new Figure(Lighting3dPlots.createMethodNone(), CUBE_W, CUBE_H,
                "3d-lighting", "Flat faces — `Light3d.Method.NONE`"));
        plots.put("light-method-diffuse", new Figure(Lighting3dPlots.createMethodDiffuse(), CUBE_W, CUBE_H,
                "3d-lighting", "Diffuse shading from above-front — `Light3d.Method.DIFFUSE`"));
        plots.put("light-method-direct", new Figure(Lighting3dPlots.createMethodDirect(), CUBE_W, CUBE_H,
                "3d-lighting", "Directional shading clamped at zero — `Light3d.Method.DIRECT`"));
        plots.put("light-method-rgb", new Figure(Lighting3dPlots.createMethodRgb(), CUBE_W, CUBE_H,
                "3d-lighting", "Normals mapped to colours — `Light3d.Method.RGB`"));
        plots.put("light-mode-hsv", new Figure(Lighting3dPlots.createModeHsv(), CUBE_W, CUBE_H,
                "3d-lighting", "Blend in HSV space — `Light3d.Mode.HSV`"));
        plots.put("light-mode-hsl", new Figure(Lighting3dPlots.createModeHsl(), CUBE_W, CUBE_H,
                "3d-lighting", "Blend in HSL space — `Light3d.Mode.HSL`"));
        plots.put("light-contrast", new Figure(Lighting3dPlots.createContrastLow(), CUBE_W, CUBE_H,
                "3d-lighting", "A gentle shade — `Light3d.builder().contrast(0.2)`"));
        plots.put("light-direction", new Figure(Lighting3dPlots.createDirection(), CUBE_W, CUBE_H,
                "3d-lighting", "Light from below-back — `Light3d.builder().direction(0.5, 0, -1)`"));
        plots.put("light-anchor-camera", new Figure(Lighting3dPlots.createCameraAnchor(), CUBE_W, CUBE_H,
                "3d-lighting", "A camera-anchored light — `Light3d.builder().anchor(Anchor.CAMERA)`"));
        plots.put("light-positional", new Figure(Lighting3dPlots.createPositional(), CUBE_W, CUBE_H,
                "3d-lighting", "Point light with distance falloff — `Light3d.builder().position(...)`"));
        plots.put("light-backfaces", new Figure(Lighting3dPlots.createBackfaces(), CUBE_W, CUBE_H,
                "3d-lighting", "Negated backfaces — `Light3d.builder().backfaceScale(-1)`"));
        plots.put("light-coord", new Figure(Lighting3dPlots.createCoordLight(), CUBE_W, CUBE_H,
                "3d-lighting", "The light supplied on `coord3d().light(...)`"));
        plots.put("light-surface", new Figure(Lighting3dPlots.createSurfaceLit(), CUBE_W, CUBE_H,
                "3d-lighting", "A `Geoms.surface3d()` terrain lit from above-front"));
        plots.put("light-function", new Figure(Lighting3dPlots.createFunctionLit(), CUBE_W, CUBE_H,
                "3d-lighting", "A `Geoms.function3d()` surface with a side light"));
        plots.put("light-guide-colorbar", new Figure(Lighting3dPlots.createGuideColorbar3d(), CUBE_W, CUBE_H,
                "3d-lighting", "A `Guides.guideColorbar3d()` shading the shared fill scale"));
        plots.put("light-guide-legend", new Figure(Lighting3dPlots.createGuideLegend3d(), CUBE_W, CUBE_H,
                "3d-lighting", "A `Guides.guideLegend3d()` with lit keys"));

        // --- positionOnFace(): 2D/3D layer mixing ---
        plots.put("mixed-density-on-face", new Figure(Mixing3dPlots.createDensityOnFaces(), CUBE_W, CUBE_H,
                "3d-mixing", "`Geoms.density2d()` placed on the zmin face"));
        plots.put("mixed-tiles-on-face", new Figure(Mixing3dPlots.createTilesOnFaces(), CUBE_W, CUBE_H,
                "3d-mixing", "`Geoms.tile()` heatmap on the zmax face"));
        plots.put("mixed-flattened-points", new Figure(Mixing3dPlots.createFlattened3d(), CUBE_W, CUBE_H,
                "3d-mixing", "`Geoms.point3d()` flattened onto the zmin face"));

        // --- coord3d() view controls ---
        plots.put("coord3d-default-view", new Figure(CoordView3dPlots.createDefaultView(), CUBE_W, CUBE_H,
                "coord3d", "The default 3-D view (pitch 0, roll -60, yaw -30, dist 2)"));
        plots.put("coord3d-rotation", new Figure(CoordView3dPlots.createRotationSweep(), CUBE_W, CUBE_H,
                "coord3d", "Rotation sweep — `coord3d().pitch(35).roll(-75).yaw(-55)`"));
        plots.put("coord3d-orthographic", new Figure(CoordView3dPlots.createOrthographic(), CUBE_W, CUBE_H,
                "coord3d", "Orthographic projection — `coord3d().persp(false)`"));
        plots.put("coord3d-close-camera", new Figure(CoordView3dPlots.createCloseCamera(), CUBE_W, CUBE_H,
                "coord3d", "Close camera — `coord3d().dist(1.25)` augments the perspective"));
        plots.put("coord3d-distant-camera", new Figure(CoordView3dPlots.createDistantCamera(), CUBE_W, CUBE_H,
                "coord3d", "Distant camera — `coord3d().dist(4)` flattens towards orthographic"));
        plots.put("coord3d-fixed-scales", new Figure(CoordView3dPlots.createFixedScales(), CUBE_W, CUBE_H,
                "coord3d", "Fixed scales preserve the raw data aspect — `coord3d().scales(ScaleMode.FIXED)`"));
        plots.put("coord3d-ratio", new Figure(CoordView3dPlots.createNonUniformRatio(), CUBE_W, CUBE_H,
                "coord3d", "Non-uniform axis ratio — `coord3d().ratio(1.6, 1, 1.3)`"));
        plots.put("coord3d-zoom", new Figure(CoordView3dPlots.createZoomed(), CUBE_W, CUBE_H,
                "coord3d", "Zoom 1.25 magnifies the fitted cube"));
        plots.put("coord3d-panels-all", new Figure(CoordView3dPlots.createAllPanels(), CUBE_W, CUBE_H,
                "coord3d", "All six cube faces — `coord3d().panels(CubePanel.ALL)`"));
        plots.put("coord3d-panels-foreground", new Figure(CoordView3dPlots.createForegroundPanels(), CUBE_W, CUBE_H,
                "coord3d", "Foreground faces only — an open box towards the camera"));
        plots.put("coord3d-panels-none", new Figure(CoordView3dPlots.createNoPanels(), CUBE_W, CUBE_H,
                "coord3d", "No cube faces — `coord3d().panels(CubePanel.NONE)`"));
        plots.put("coord3d-clip", new Figure(CoordView3dPlots.createClipped(), CUBE_W, CUBE_H,
                "coord3d", "Clipping confines overflow to the panel"));
        plots.put("coord3d-tight-bounds", new Figure(CoordView3dPlots.createTightBounds(), CUBE_W, CUBE_H,
                "coord3d", "No scale expansion — `coord3d().expand(false)`"));
        plots.put("coord3d-centered-titles", new Figure(CoordView3dPlots.createCenteredTitles(), CUBE_W, CUBE_H,
                "coord3d", "Axis titles centered on their edges"));
        plots.put("coord3d-label-edges", new Figure(CoordView3dPlots.createCustomLabelEdges(), CUBE_W, CUBE_H,
                "coord3d", "Explicit facing edges for each label chain"));
        plots.put("coord3d-flat-depth-scale", new Figure(CoordView3dPlots.createFlatDepthScale(), CUBE_W, CUBE_H,
                "coord3d", "Zero depth scaling plus unrotated labels"));

        // --- coord3d() view-control compositions ---
        plots.put("article-rotation", new Figure(CoordView3dArticlePlots.createRotation(), FACET_W, FACET_H,
                "coord3d-article", "Rotation — the default, all-zero, an arbitrary combination, and single-angle rotations"));
        plots.put("article-perspective", new Figure(CoordView3dArticlePlots.createPerspective(), CUBE_3COL_W, CUBE_3COL_H,
                "coord3d-article", "Perspective and distance — dist 1.1, dist 2, and orthographic (persp = FALSE)"));
        plots.put("article-scales", new Figure(CoordView3dArticlePlots.createScales(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Scales — free versus fixed axis scaling"));
        plots.put("article-ratio", new Figure(CoordView3dArticlePlots.createRatio(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Ratio — a 1:3:1 axis ratio under free and fixed scales"));
        plots.put("article-panels", new Figure(CoordView3dArticlePlots.createPanels(), CUBE_W, CUBE_H,
                "coord3d-article", "Panels — background (default), explicit faces, all, and none"));
        plots.put("article-zoom", new Figure(CoordView3dArticlePlots.createZoom(), CUBE_3COL_W, CUBE_3COL_H,
                "coord3d-article", "Zoom and clip — zoom 0.7, 1, and 1.5 with clipping on"));
        plots.put("article-expand", new Figure(CoordView3dArticlePlots.createExpand(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Expand — default versus disabled expansion"));
        plots.put("article-labels", new Figure(CoordView3dArticlePlots.createLabels(), CUBE_W, CUBE_H,
                "coord3d-article", "Axis label placement — auto, manual edges, and rotated/fixed labels"));
        plots.put("article-themes", new Figure(CoordView3dArticlePlots.createThemes(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Theming — the dark and minimal complete themes"));
        plots.put("article-theme-foreground", new Figure(CoordView3dArticlePlots.createThemeForeground(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Foreground panel theming — all faces under gray and light themes"));
        plots.put("article-theme-axes", new Figure(CoordView3dArticlePlots.createThemeAxes(), CUBE_2COL_W, CUBE_2COL_H,
                "coord3d-article", "Per-axis text theming — tick, text, and title colours"));

        // --- plot3d()/ggplot3d() 3D DSL ---
        plots.put("dsl3d-scatter", new Figure(Plot3dPlots.createScatter(), CUBE_W, CUBE_H,
                "3d-dsl", "The simplest 3D recipe — `ggplot3d(df, aes(...))` with the default view"));
        plots.put("dsl3d-view", new Figure(Plot3dPlots.createView(), CUBE_W, CUBE_H,
                "3d-dsl", "Cube controls — `plot3d().view(35, -75, -55)`"));
        plots.put("dsl3d-light", new Figure(Plot3dPlots.createLight(), CUBE_W, CUBE_H,
                "3d-dsl", "A plot-level light — `plot3d().light(Light3d.defaultLight())`"));

        // --- Orbit3d() / demo data ---
        plots.put("orbit-sphere-points", new Figure(OrbitPlots.createSpherePoints(), CUBE_W, CUBE_H,
                "3d-orbit", "The orbit gesture showcase — the lumpy sphere point cloud, draggable in the showcase app"));
        plots.put("orbit-mountain-lit", new Figure(OrbitPlots.createLitMountain(), CUBE_W, CUBE_H,
                "3d-orbit", "The mini-globe showcase — the mountain lit from above-front, rotated by the axis mini-globe control in the showcase app"));
        plots.put("orbit-mountain-surface", new Figure(OrbitPlots.createMountainSurface(), CUBE_W, CUBE_H,
                "3d-orbit", "The 45×45 mountain surface — `Geoms.surface3d()` over the MountainDatasets demo grid"));
        plots.put("orbit-sphere-hull", new Figure(OrbitPlots.createSphereHull(), CUBE_W, CUBE_H,
                "3d-orbit", "Convex hull of the sphere — `Geoms.hull3d().method(CONVEX)` over the SpherePointsDatasets cloud"));

        // --- Reference lines and annotations ---
        plots.put("mtcars-vline-5", new Figure(MtcarsPlots.createFixedVline(), W, H,
                "reference", "Vertical reference line — `vline(xintercept = 5)`"));
        plots.put("mtcars-vline-1-to-5", new Figure(MtcarsPlots.createMultipleVlines(), W, H,
                "reference", "Several vertical rules at once — `vline(xintercept = 1:5)`"));
        plots.put("mtcars-hline-20", new Figure(MtcarsPlots.createFixedHline(), W, H,
                "reference", "Horizontal reference line — `hline(yintercept = 20)`"));
        plots.put("mtcars-abline-intercept", new Figure(MtcarsPlots.createAblineIntercept(), W, H,
                "reference", "A diagonal from an intercept alone — `abline(intercept = 20)`"));
        plots.put("mtcars-abline-best-fit", new Figure(MtcarsPlots.createAblineBestFit(), W, H,
                "reference", "Least-squares fit — `abline(intercept = 37, slope = -5)`"));
        plots.put("mtcars-abline-outside-range", new Figure(MtcarsPlots.createAblineOutsideRange(), W, H,
                "reference", "The default diagonal, outside the data range"));
        plots.put("mtcars-lm-smooth", new Figure(MtcarsPlots.createLmSmoothAlternative(), W, H,
                "reference", "The same fit via `Geoms.smooth(method = 'lm')`"));
        plots.put("mtcars-faceted-hline", new Figure(MtcarsPlots.createFacetedHline(), W, H,
                "reference", "Per-panel `hline()` from a summary frame — `Geoms.hline(yinterceptMapping, mean_wt)` inside `Facets.wrap(~cyl)`"));
        plots.put("mtcars-faceted-vline", new Figure(MtcarsPlots.createFacetedVline(), W, H,
                "reference", "Per-panel `vline()` from a summary frame — `Geoms.vline(aes(xintercept = mpg), mean_mpg)` inside `Facets.wrap(~cyl)`"));
        plots.put("mtcars-faceted-hline-colour", new Figure(MtcarsPlots.createColourMappedHline(), W, H,
                "reference", "Per-panel `hline()` with colour mapped to the intercept column — `Geoms.hline(aes(yintercept = wt, colour = wt), mean_wt)`"));
        plots.put("mtcars-faceted-abline", new Figure(MtcarsPlots.createFacetedAbline(), W, H,
                "reference", "Per-panel regression line — `Geoms.abline(aes(slope = slope, intercept = intercept), fit)` inside `Facets.wrap(~cyl)`"));
        plots.put("manhattan-height", new Figure(GwasPlots.createManhattan(), 1400, 620,
                "reference", "Manhattan plot with a dashed significance threshold"));
        plots.put("manhattan-height-annotated", new Figure(GwasPlots.createAnnotatedManhattan(), 1400, 620,
                "reference", "The same view with lead SNPs highlighted and loci named by `Geoms.text()`"));
        plots.put("manhattan-chromosome-3", new Figure(GwasPlots.createChromosomeZoom(), 1400, 620,
                "reference", "A single chromosome in close-up"));
        plots.put("gwas-qq-plot", new Figure(GwasPlots.createQqPlot(), 760, 700,
                "reference", "Quantile-quantile plot with the identity line from `Geoms.abline()`"));

        // --- guides ---
        plots.put("guide-1-two-column-legend", new Figure(GuidePlots.createTwoColumnLegend(), W, H,
                "guide", "Discrete legend with two key columns — `Guides.guideLegend(ncol = 2)`"));
        plots.put("guide-2-byrow-legend", new Figure(GuidePlots.createByRowLegend(), W, H,
                "guide", "Two columns filled row by row — `Guides.guideLegend(ncol = 2, byrow = TRUE)`"));
        plots.put("guide-3-reversed-legend", new Figure(GuidePlots.createReversedLegend(), W, H,
                "guide", "Keys in reverse order — `Guides.guideLegend(reverse = TRUE)`"));
        plots.put("guide-4-top-legend", new Figure(GuidePlots.createTopLegend(), W, H,
                "guide", "Legend on top — `theme(GuidePosition = 'top')`"));
        plots.put("guide-5-bottom-legend", new Figure(GuidePlots.createBottomLegend(), W, H,
                "guide", "Legend on the bottom — `theme(GuidePosition = 'bottom')`"));
        plots.put("guide-6-left-legend", new Figure(GuidePlots.createLeftLegend(), W, H,
                "guide", "Legend on the left — `theme(GuidePosition = 'left')`"));
        plots.put("guide-7-legend-box", new Figure(GuidePlots.createLegendBox(), W, H,
                "guide", "A box behind the legend — `theme(legend.background)`"));
        plots.put("guide-8-multi-guide", new Figure(GuidePlots.createMultiGuide(), W, H,
                "guide", "Two guides stacked: a fill legend and a colour legend"));
        plots.put("guide-9-horizontal-colourbar", new Figure(GuidePlots.createHorizontalColorbar(), W, H,
                "guide", "Horizontal colourbar on top — `Guides.guideColorbar(direction = 'horizontal')`"));
        plots.put("guide-10-shape-only-legend", new Figure(GuidePlots.createShapeOnlyLegend(), W, H,
                "guide", "Shape-only legend with neutral point glyphs — `aes(shape = ...)`"));
        plots.put("guide-11-merged-colour-shape", new Figure(GuidePlots.createMergedColorShapeLegend(), W, H,
                "guide", "One merged legend for colour and shape mapped to the same column"));
        plots.put("guide-12-split-placement", new Figure(GuidePlots.createSplitPlacement(), W, H,
                "guide", "Per-guide placement — fill legend right, colour legend bottom"));
        plots.put("guide-13-inside-placement", new Figure(GuidePlots.createInsidePlacement(), W, H,
                "guide", "Legend inside the panel — `theme(GuidePosition = 'inside')`"));
        plots.put("guide-14-per-aesthetic-titles", new Figure(GuidePlots.createPerAestheticTitles(), W, H,
                "guide", "Per-aesthetic titles — `labs(colour = ..., shape = ...)`"));
        plots.put("guide-15-size-only-legend", new Figure(GuidePlots.createSizeOnlyLegend(), W, H,
                "guide", "Size-only legend — `aes(size = ...)` with area-proportional dots"));
        plots.put("guide-16-colour-and-size", new Figure(GuidePlots.createColorAndSize(), W, H,
                "guide", "Colour and size legends side by side"));
        plots.put("guide-17-themed-legend", new Figure(GuidePlots.createThemedLegend(), W, H,
                "guide", "Per-guide style overrides — `GuideTheme.title(...).withKeys(...)`"));
        plots.put("guide-18-merged-ratings", new Figure(GuidePlots.createMergedRatingsLegend(), W, H,
                "guide", "Legends merge when two aesthetics share the same category labels"));
        plots.put("guide-19-anchored-inside", new Figure(GuidePlots.createAnchoredInsideLegend(), W, H,
                "guide", "Inside legend anchored to the panel's top-left — `inside(x, y)`"));
        plots.put("guide-20-alpha", new Figure(GuidePlots.createAlphaGuide(), W, H,
                "guide", "Continuous alpha — `aes(alpha = \"carat\")` with gradient bar"));
        plots.put("guide-21-alpha-colour", new Figure(GuidePlots.createAlphaWithColourGuide(), W, H,
                "guide", "Alpha + colour — one gradient bar per cut class, tinted in its colour"));
        plots.put("guide-22-overflow-to-bottom", new Figure(GuidePlots.createOverflowToBottom(), W, H,
                "guide", "Overflow to the bottom — `theme(legend.overflow = 'flow')` folds a tall side strip onto the bottom"));
        plots.put("guide-23-overflow-hybrid", new Figure(GuidePlots.createHybridOverflow(), W, H,
                "guide", "Hybrid overflow — guides that fit stay on the side, the rest flow to the bottom"));
        plots.put("guide-24-overflow-uniform-scale", new Figure(GuidePlots.createUniformScaledOverflow(), W, H,
                "guide", "Uniform scale — `legend.overflow = 'scale'` shrinks geometry and labels alike"));
        plots.put("guide-25-overflow-geometry-scale", new Figure(GuidePlots.createGeometryScaledOverflow(), W, H,
                "guide", "Geometry scale — geometry compresses while labels keep their full size"));
        plots.put("guide-26-size-legend-separated", new Figure(GuidePlots.createSeparatedSizeLegend(), W, H,
                "guide", "Separated size legend — `SizeLegendStyle.SEPARATED` gives every key its own non-overlapping row"));

        // --- coordPolar() ---
        plots.put("polar-1-coxcomb", new Figure(PolarPlots.createCoxcomb(), W, H,
                "polar", "Coxcomb — `coordPolar()` maps x to the angle"));
        plots.put("polar-2-coloured-coxcomb", new Figure(PolarPlots.createColouredCoxcomb(), W, H,
                "polar", "Coxcomb coloured by cylinder count"));
        plots.put("polar-3-pie", new Figure(PolarPlots.createPie(), W, H,
                "polar", "Pie — `coordPolar(theta = 'y')` maps y to the angle"));
        plots.put("polar-4-bullseye", new Figure(PolarPlots.createBullseye(), W, H,
                "polar", "Bulls-eye — each category on its own ring"));
        plots.put("polar-5-donut", new Figure(PolarPlots.createDonut(), W, H,
                "polar", "Donut — a pie with a hollow centre"));
        plots.put("polar-6-stacked-rose", new Figure(PolarPlots.createStackedRose(), W, H,
                "polar", "Stacked rose — diamonds cut by clarity, rings per cut"));
        plots.put("polar-7-radial-scatter", new Figure(PolarPlots.createRadialScatter(), W, H,
                "polar", "Radial scatter — coordRadial fan of disp vs mpg"));
        plots.put("polar-8-half-circle", new Figure(PolarRadialPlots.createHalfCircle(), W, H,
                "polar", "Half circle — coordRadial −0.5π to +0.5π"));
        plots.put("polar-9-quarter-circle", new Figure(PolarRadialPlots.createQuarterCircle(), W, H,
                "polar", "Quarter circle — coordRadial 0 to +0.5π"));
        plots.put("polar-10-donut", new Figure(PolarRadialPlots.createDonutScatter(), W, H,
                "polar", "Donut — inner.radius 0.3 with radialAxisInside"));
        plots.put("polar-11-radial-pie", new Figure(PolarRadialPlots.createRadialPieDefault(), W, H,
                "polar", "Radial pie — default expansion leaves a twelve-o'clock notch"));
        plots.put("polar-12-radial-pie-noexpand", new Figure(PolarRadialPlots.createRadialPieNoExpand(), W, H,
                "polar", "Radial pie — expand = FALSE fills the disc"));
        plots.put("polar-13-radial-pie-clean", new Figure(PolarRadialPlots.createRadialPieClean(), W, H,
                "polar", "Radial pie — expand = FALSE with both axes hidden"));
        plots.put("polar-14-wind-rose", new Figure(PolarRadialPlots.createWindRose(), W, H,
                "polar", "Wind rose — rotateAngle = TRUE lines the names along each radius"));
        plots.put("polar-15-radial-boxplot", new Figure(PolarRadialPlots.createRadialBoxplot(), W, H,
                "polar", "Radial boxplot — 270° fan of class vs displ, theta labels tangent"));
        plots.put("polar-16-radial-violin", new Figure(PolarRadialPlots.createRadialViolin(), W, H,
                "polar", "Radial violin — class density profiles bulging around the 270° fan"));
        plots.put("polar-17-spiral", new Figure(PolarLinesPlots.createSpiral(), W, H,
                "polar", "Spiral — Geoms.line wraps a time series around a full turn"));
        plots.put("polar-18-radial-smooth", new Figure(PolarLinesPlots.createRadialSmooth(), W, H,
                "polar", "Radial smooth — LOESS trend and confidence band on the radial scatter fan"));
        plots.put("polar-19-significance-ring", new Figure(PolarLinesPlots.createSignificanceRing(), W, H,
                "polar", "Significance ring — Geoms.hline is a ring of constant radius on the rose"));
        plots.put("polar-20-cutoff-spoke", new Figure(PolarLinesPlots.createCutoffSpoke(), W, H,
                "polar", "Cut-off spoke — Geoms.vline is a ray of constant angle on the spiral"));
        plots.put("polar-21-identity-line", new Figure(PolarLinesPlots.createIdentityLine(), W, H,
                "polar", "Radial trendline — Geoms.abline winds a straight data line through the radial fan"));
        plots.put("polar-22-circular-heatmap", new Figure(PolarLinesPlots.createCircularHeatmap(), W, H,
                "polar", "Circular heatmap — Geoms.tile cells become annular wedges"));
        plots.put("polar-23-density-rose", new Figure(PolarLinesPlots.createDensityRose(), W, H,
                "polar", "Density rose — Geoms.density wraps the estimate around a half fan"));
        plots.put("polar-24-spiral-ribbons", new Figure(PolarLinesPlots.createSpiralRibbons(), W, H,
                "polar", "Spiral ribbons — Geoms.area stacks production bands around a full turn"));
        plots.put("polar-25-radial-contours", new Figure(PolarLinesPlots.createRadialContours(), W, H,
                "polar", "Radial contours — Geoms.density2d wraps the bivariate density around the fan"));
        plots.put("polar-26-radar-profile", new Figure(PolarRadarPlots.createRadarProfile(), W, H,
                "polar", "Radar profile — Geoms.polygon fills one car's normalised web"));
        plots.put("polar-27-radar-comparison", new Figure(PolarRadarPlots.createRadarComparison(), W, H,
                "polar", "Radar comparison — two cars overlaid as colour-coded polygons"));
        plots.put("polar-28-radar-cylinders", new Figure(PolarRadarPlots.createRadarByCylinder(), W, H,
                "polar", "Radar by cylinder group — average profiles of 4, 6, and 8-cylinder cars"));
        plots.put("polar-29-radar-outline", new Figure(PolarRadarPlots.createRadarOutline(), W, H,
                "polar", "Radar outline — the web drawn as an unfilled closed polygon"));

        // --- Geoms.path/step/segment/curve ---
        plots.put("wire-01-step", new Figure(WirePlots.createStep(), W, H,
                "path", "Geoms.step() — the classic staircase, two points per step"));
        plots.put("wire-02-path", new Figure(WirePlots.createPath(), W, H,
                "path", "Geoms.path() — a trajectory traced in row order, unsorted"));
        plots.put("wire-03-segment", new Figure(WirePlots.createSegment(), W, H,
                "path", "Geoms.segment() — one directed line per row, from (x,y) to (xend,yend)"));
        plots.put("wire-04-curve", new Figure(WirePlots.createCurve(), W, H,
                "path", "Geoms.curve() — bowed quadratic arcs over the same end points"));

        // --- Stats.smooth() ---
        plots.put("stat-01-loess", new Figure(StatPlots.createLoess(), W, H,
                "stat", "StatSmooth.fit(): LOESS (span = 0.6), drawn straight from the returned StatData"));
        plots.put("stat-02-lm", new Figure(StatPlots.createLm(), W, H,
                "stat", "StatSmooth.fit(): linear model, a straight least-squares line"));
        plots.put("stat-03-span", new Figure(StatPlots.createSpan(), W, H,
                "stat", "StatSmooth.fit(): LOESS span = 0.3 — a wigglier curve from the same stat"));
        plots.put("stat-04-fullrange", new Figure(StatPlots.createFullrange(), W, H,
                "stat", "StatSmooth.fit(): fullrange extrapolation evaluated over [0, 6.5]"));

        // --- Stats.bin() / Stats.summary() ---
        plots.put("summary-01-histogram", new Figure(SummaryStatPlots.createHistogram(), W, H,
                "stat-summary", "Geoms.histogram() — Stats.bin() over a bell-shaped score"));
        plots.put("summary-02-freqpoly", new Figure(SummaryStatPlots.createFreqpoly(), W, H,
                "stat-summary", "Geoms.freqpoly() — two distributions, one line per group"));
        plots.put("summary-03-pointrange", new Figure(SummaryStatPlots.createPointrange(), W, H,
                "stat-summary", "Geoms.pointrange() — Stats.summary mean ± sd per group"));
        plots.put("summary-04-errorbar-crossbar", new Figure(SummaryStatPlots.createErrorbarCrossbar(), W, H,
                "stat-summary", "Geoms.errorbar() whiskers over Geoms.crossbar() boxes"));
        plots.put("summary-05-ribbon", new Figure(SummaryStatPlots.createRibbon(), W, H,
                "stat-summary", "Geoms.ribbon() — a standalone uncertainty band"));

        // --- Facets.grid() ---
        plots.put("facetgrid-01-drv-cyl", new Figure(FacetGridPlots.createDrvCylGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(rows = drv, cols = cyl)"));
        plots.put("facetgrid-02-cyl-drv", new Figure(FacetGridPlots.createCylDrvGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(rows = cyl, cols = drv) — row and column variables swapped"));
        plots.put("facetgrid-03-free-scales", new Figure(FacetGridPlots.createFreeScalesGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., scales = \"free\") — each panel owns its axes"));
        plots.put("facetgrid-04-free-scales-space", new Figure(FacetGridPlots.createFreeScalesFreeSpaceGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., scales = \"free\", space = \"free\")"));
        plots.put("facetgrid-05-vs-am-gear", new Figure(FacetGridPlots.createVsAmGearGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(rows = vars(vs, am), cols = vars(gear))"));
        plots.put("facetgrid-06-margins-true", new Figure(FacetGridPlots.createMarginsTrueGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., margins = TRUE) — aggregate row and column panels"));
        plots.put("facetgrid-07-margins-am", new Figure(FacetGridPlots.createMarginsAmGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., margins = \"am\") — one margin dimension only"));
        plots.put("facetgrid-08-as-table-false", new Figure(FacetGridPlots.createAsTableFalseGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., as.table = FALSE) — row levels bottom-up"));
        plots.put("facetgrid-09-switch-both", new Figure(FacetGridPlots.createSwitchBothGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., switch = \"both\") — strip labels on the outside edges"));
        plots.put("facetgrid-10-hline", new Figure(FacetGridPlots.createGridHline(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(rows = year, cols = cyl) with a per-cylinder Geoms.hline"));
        plots.put("facetgrid-11-switch-x", new Figure(FacetGridPlots.createSwitchXGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., switch = \"x\") — x-axis on the top row, strips at the bottom"));
        plots.put("facetgrid-12-switch-y", new Figure(FacetGridPlots.createSwitchYGrid(), FACET_W, FACET_H,
                "facet-grid", "Facets.grid(..., switch = \"y\") — y-axis on the right column, strips at the left"));

        plots.put("linetype-01-line-group-dashes", new Figure(LineTypePlots.createLineGroupDashes(), W, H,
                "line", "linetype maps the group: each drive type is a distinct dash"));
        plots.put("linetype-02-color-and-line", new Figure(LineTypePlots.createColorAndLineType(), W, H,
                "line", "colour + linetype on the same column; legend keys carry the dash"));
        plots.put("linetype-03-smooth-dashed", new Figure(LineTypePlots.createSmoothLineType(), W, H,
                "line", "dashed per-group linear trend lines with a matching legend"));

        plots.put("scales-01-reverse-x", new Figure(ScalePlots.createReverseX(), W, H,
                "scales", "Reversed X axis — largest value sits on the left — `Scales.scaleXReverse()`"));
        plots.put("scales-02-viridis-c", new Figure(ScalePlots.createViridisC(), W, H,
                "scales", "Continuous colour scale over the viridis ramp — `Scales.scaleColorViridisC()`"));
        plots.put("scales-03-gradient", new Figure(ScalePlots.createGradient(), W, H,
                "scales", "Two-stop low-to-high colour gradient — `Scales.scaleColorGradient(low, high)`"));
        plots.put("scales-04-binned", new Figure(ScalePlots.createBinned(), W, H,
                "scales", "Continuous colour scale binned into six steps with a colorsteps guide — `Scales.scaleColorBinned(6)`"));
        plots.put("scales-05-viridis-d", new Figure(ScalePlots.createViridisD(), W, H,
                "scales", "Discrete colour scale over the viridis palette — `Scales.scaleColorViridisD()`"));
        plots.put("scales-06-brewer", new Figure(ScalePlots.createBrewer(), W, H,
                "scales", "Discrete colour scale over the Dark2 ColorBrewer palette — `Scales.scaleColorBrewer(\"Dark2\")`"));
        plots.put("scales-07-limits", new Figure(ScalePlots.createLimits(), W, H,
                "scales", "Explicit Y limits clamp the plotted range — `Scales.scaleYLimits(min, max)`"));
        plots.put("scales-08-manual-shape", new Figure(ScalePlots.createManualShape(), W, H,
                "scales", "Explicit point shapes per cylinder count — `Scales.scaleShapeManual(values = ...)`"));
        plots.put("scales-09-manual-size", new Figure(ScalePlots.createManualSize(), W, H,
                "scales", "Explicit point radii per cylinder count — `Scales.scaleSizeManual(values = ...)`"));
        plots.put("scales-10-axis-position-x-top", new Figure(ScalePlots.createAxisPositionXTop(), W, H,
                "scales", "X axis on the top edge — `Scales.scaleXContinuous(position = \"top\")`"));
        plots.put("scales-11-axis-position-y-right", new Figure(ScalePlots.createAxisPositionYRight(), W, H,
                "scales", "Y axis on the right edge — `Scales.scaleYContinuous(position = \"right\")`"));
        plots.put("scales-12-axis-position-both", new Figure(ScalePlots.createAxisPositionBoth(), W, H,
                "scales", "Both axes on the outer edges — x on top, y on the right"));

        // --- Plot.layer(...) / aesOf / afterStat() ---
        plots.put("layer-01-bar-count", new Figure(LayerPlots.createLayerBarCount(), W, H,
                "layer", "Geoms.bar via the bar() factory — Stats.count() is the default"));
        plots.put("layer-02-bar-after-stat", new Figure(LayerPlots.createLayerBarAfterStat(), W, H,
                "layer", "Geoms.bar mapping fill = afterStat(count) — bars coloured by their count, via bar().mapping(...)"));
        plots.put("layer-03-aes-of-histogram", new Figure(LayerPlots.createAesOfHistogram(), W, H,
                "layer", "aesOf(map) for the global mapping plus a per-geom mapping(fill = afterStat(density)) on Geoms.histogram"));
        plots.put("layer-04-layer-from-parts", new Figure(LayerPlots.createLayerFromParts(), W, H,
                "layer", "Geoms.bar from parts via Plot#layer(...) using barGeom(), Stats.count() and Positions.identity()"));

        // --- CoordEqual / CoordTrans ---
        plots.put("coords-01-coord-equal", new Figure(CoordPlots.createCoordEqual(), W, H,
                "coords", "`coordEqual()` — equal pixels per unit on both continuous axes, so the panel is centre-cropped to match the aspect"));
        plots.put("coords-02-coord-trans", new Figure(CoordPlots.createCoordTrans(), W, H,
                "coords", "`coordTrans(…, LOG10)` — a log10 y-axis where every decade spans an equal distance in pixels"));

        // --- annotate() / log axis ticks ---
        plots.put("annotate-01-annotations", new Figure(AnnotatePlots.createAnnotations(), W, H,
                "annotate", "`annotate()` — rect, segment, point, text, and hline data-free layers on top of a scatter"));
        plots.put("annotate-02-logticks", new Figure(AnnotatePlots.createLogticks(), W, H,
                "annotate", "automatic sub-decade minor ticks of a log10 axis"));
        plots.put("annotate-03-minor-grid-linear", new Figure(AnnotatePlots.createMinorGridLinear(), W, H,
                "annotate", "`showMinorGrid(true)` — the theme-level minor grid on a linear axis"));

        // --- theme-level tick / grid configurations ---
        plots.put("grid-01-defaults", new Figure(TickGridPlots.createDefaults(), W, H,
                "grid", "Tick & grid defaults — major + minor grid (theme color at half width), major + minor axis ticks"));
        plots.put("grid-02-no-minor", new Figure(TickGridPlots.createNoMinor(), W, H,
                "grid", "`showMinorGrid(false).showMinorTicks(false)` — a major-only grid, like `theme(showMinorGrid() = an empty element(), showMinorTicks() = an empty element())`"));
        plots.put("grid-03-custom-minor", new Figure(TickGridPlots.createCustomMinor(), W, H,
                "grid", "Custom minor grid color/width and minor tick color — `minorGridLineColor()`/`minorGridLineWidth()`/`minorTickColor()`/`minorTickLength()`"));
        plots.put("grid-04-no-x-ticks", new Figure(TickGridPlots.createNoXAxisTicks(), W, H,
                "grid", "`showXAxisTicks(false)` — the X-axis keeps labels but loses every tick mark, like `theme(showXAxisTicks() = an empty element())`"));

        // --- matrixPlot() scatter-plot matrix ---
        plots.put("matrix-01-penguins", new Figure(MatrixPlots.createPenguinsMatrix(), W, H,
                "matrix", "`matrixPlot()` — a generalized-pairs matrix over the Palmer Penguins measurements: histogram diagonal, lower-triangle scatters with the Pearson `r`, upper-triangle text"));
        plots.put("matrix-02-penguins-hue", new Figure(MatrixPlots.createPenguinsMatrixBySpecies(), W, H,
                "matrix", "`matrixPlot(df, aes().color(\"species\"), …)` — a scatter-plot matrix gesture: translucent per-species density curves on the diagonal, small hue-coloured scatter points in the lower triangle, per-species 2D density contours in the upper, and a single legend shared by every cell"));
        plots.put("matrix-03-tips", new Figure(MatrixPlots.createTipsMatrix(), W, H,
                "matrix", "`matrixPlot(tips, …)` — a tips scatter-plot matrix over the numeric columns (total bill, tip, party size)"));
        plots.put("matrix-04-tips-hue", new Figure(MatrixPlots.createTipsMatrixBySmoker(), W, H,
                "matrix", "`matrixPlot(tips, aes().color(\"smoker\"), …)` — the tips matrix coloured by smoker, with a single shared legend"));
        plots.put("matrix-05-tips-mixed", new Figure(MatrixPlots.createTipsMixed(), W, H,
                "matrix", "`matrixPlot(tips, …)` — type dispatch over mixed tips columns: `total_bill` (numeric) with `day`/`sex` (categorical) renders densities, box plots, faceted histograms and count bars"));
        plots.put("matrix-06-penguins-mixed", new Figure(MatrixPlots.createPenguinsMixed(), W, H,
                "matrix", "`matrixPlot(penguins, …)` — type dispatch over mixed penguins columns — `bill_length_mm` (numeric) with `species`/`island` (categorical)"));
        plots.put("matrix-07-diamonds", new Figure(MatrixPlots.createDiamondsMatrix(), W, H,
                "matrix", "`matrixPlot(diamonds, …)` — the full 53,940-row diamonds dataset: carat, depth, price, table"));
        plots.put("matrix-08-diamonds-hue", new Figure(MatrixPlots.createDiamondsMatrixByCut(), W, H,
                "matrix", "`matrixPlot(diamonds, aes().color(\"cut\"), …)` — the full diamonds matrix coloured by cut"));
        plots.put("matrix-09-diamonds-mixed", new Figure(MatrixPlots.createDiamondsMixed(), W, H,
                "matrix", "`matrixPlot(diamonds, …)` — type dispatch over full diamonds: `carat` (numeric) with `cut`/`clarity` (categorical)"));
        plots.put("matrix-10-mtcars", new Figure(MatrixPlots.createMtcarsMatrix(), W, H,
                "matrix", "`matrixPlot(mtcars, …)` — a numeric mtcars matrix: mpg, displacement, horsepower, weight"));
        plots.put("matrix-11-mtcars-hue", new Figure(MatrixPlots.createMtcarsMatrixByCyl(), W, H,
                "matrix", "`matrixPlot(mtcars, aes().color(\"cyl\"), …)` — the mtcars matrix coloured by cylinder count"));
        plots.put("matrix-12-mpg", new Figure(MatrixPlots.createMpgMatrix(), W, H,
                "matrix", "`matrixPlot(mpg, …)` — an mpg fuel-economy matrix: displacement, city and highway MPG"));
        plots.put("matrix-13-mpg-hue", new Figure(MatrixPlots.createMpgMatrixByClass(), W, H,
                "matrix", "`matrixPlot(mpg, aes().color(\"class\"), …)` — the mpg matrix coloured by vehicle class"));

        // --- composedPlot() multi-plot composition ---
        plots.put("composed-01-penguins-scatter-density", new Figure(ComposedPlots.createPenguinsScatterDensity(), W, H,
                "composed", "`composedPlot(...)` — a penguins scatter and density side by side"));
        plots.put("composed-02-penguins-matrix-density", new Figure(ComposedPlots.createPenguinsMatrixAndDensity(), FACET_W, FACET_H,
                "composed", "`composedPlot(matrixPlot(...), ...)` — a penguins matrix beside a body-mass density"));
        plots.put("composed-03-tips-bill-by-day", new Figure(ComposedPlots.createTipsBillByDay(), W, H,
                "composed", "`composedPlot(...).above(...)` — tips bill/tip histograms stacked above day/smoker box plots"));
        plots.put("composed-04-tips-grid", new Figure(ComposedPlots.createTipsGridBySmoker(), FACET_W, FACET_H,
                "composed", "`composedPlot(...).rows(2).cols(2)` — a 2×2 tips summary grid"));
        plots.put("composed-05-mtcars-grid", new Figure(ComposedPlots.createMtcarsGrid(), FACET_W, FACET_H,
                "composed", "`composedPlot(...).rows(2).cols(2)` — a 2×2 mtcars pairwise grid"));
        plots.put("composed-06-mtcars-grid-legend", new Figure(ComposedPlots.createMtcarsGridLegend(), W, H,
                "composed", "`composedPlot(...).legend(source).collectLegends(true)` — a composed grid whose guides are hoisted into a single shared legend"));
        plots.put("composed-07-diamonds-grid", new Figure(ComposedPlots.createDiamondsGrid(), FACET_W, FACET_H,
                "composed", "`composedPlot(...)` — a 2×2 diamonds grid over the full 53,940-row dataset"));
        plots.put("composed-08-diamonds-legend", new Figure(ComposedPlots.createDiamondsByCutLegend(), W, H,
                "composed", "`composedPlot(...).legend(...)` — a diamonds scatter and density sharing one collected cut legend"));
        plots.put("composed-09-mpg-grid", new Figure(ComposedPlots.createMpgClassGrid(), FACET_W, FACET_H,
                "composed", "`composedPlot(...).rows(2).cols(2)` — a 2×2 mpg grid mixing scatters, a histogram and a bar"));
        plots.put("composed-10-penguins-tagged-grid", new Figure(ComposedPlots.createPenguinsTaggedGrid(), FACET_W, FACET_H,
                "composed", "`composedPlot(...).tags(true)` — a 2×2 penguins grid coloured by species with lettered panel tags `a`–`d`"));

        return plots;
    }

    /**
     * Writes the gallery index next to the images, grouped by geometry.
     * <p>
     * Generating it alongside the figures keeps the two from drifting apart: a
     * plot added to {@link #gallery()} appears in the index automatically, and
     * an index entry can never point at a file that was not rendered.
     * <p>
     * Thumbnails link to the SVG rather than the compressed {@code .svgz}: a
     * browser only decompresses the latter when the server sends
     * {@code Content-Encoding: gzip}, which GitHub does not do for repository
     * files, so an {@code .svgz} link would download a file instead of showing
     * a figure.
     *
     * @param outputDir the directory the figures were written to
     */
    private static void writeGalleryIndex(File outputDir) {
        var figures = gallery();
        var md = new StringBuilder();

        md.append("# gog4j — Example Gallery\n\n")
          .append("Every figure below is rendered by [`org.jtaccuino.gog.dflib.apps.PlotExporter`](")
          .append("../gog4j-examples-dflib/src/main/java/org/jtaccuino/gog/dflib/apps/PlotExporter.java) from the example sources, ")
          .append("in both raster and vector form.\n\n")
          .append("**Click any figure to open it as SVG** — scalable, editable, and readable ")
          .append("directly in the browser. The thumbnails below keep this page light; ")
          .append("each caption also links the full-size PNG.\n\n")
          .append("Regenerate everything, this page included, with:\n\n")
          .append("```bash\n./gradlew :gog4j-examples-dflib:exportDocs\n```\n\n");

        // Table of contents
        md.append("## Contents\n\n");
        for (var section : SECTIONS.entrySet()) {
            var count = figures.values().stream().filter(f -> f.section().equals(section.getKey())).count();
            md.append("- [").append(headingOf(section.getValue())).append("](#")
              .append(anchorOf(section.getValue())).append(") — ").append(count)
              .append(count == 1 ? " figure\n" : " figures\n");
        }
        md.append('\n');

        for (var section : SECTIONS.entrySet()) {
            md.append("## ").append(headingOf(section.getValue())).append("\n\n")
              .append(section.getValue()).append("\n\n");

            var inSection = figures.entrySet().stream()
                    .filter(e -> e.getValue().section().equals(section.getKey()))
                    .toList();

            // Links are relative to the index, which sits one level above the images
            var dir = outputDir.getName() + "/";

            // Two columns keeps each figure legible without a horizontal scroll
            md.append("|   |   |\n|---|---|\n");
            for (var i = 0; i < inSection.size(); i += 2) {
                md.append('|').append(cell(inSection.get(i), dir))
                  .append('|').append(i + 1 < inSection.size() ? cell(inSection.get(i + 1), dir) : " ")
                  .append("|\n");
            }
            md.append('\n');
        }

        var target = new File(outputDir.getParentFile() == null ? new File(".") : outputDir.getParentFile(),
                              "GALLERY.md");
        try {
            Files.writeString(target.toPath(), md.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
        System.out.printf("%-34s %,10d bytes MD%n", "GALLERY.md", target.length());
    }

    /**
     * One gallery cell: a thumbnail linked to the SVG, with the caption and both
     * full-size formats underneath.
     */
    private static String cell(Map.Entry<String, Figure> entry, String dir) {
        var name = entry.getKey();
        var caption = entry.getValue().caption();
        return " [![" + caption + "](" + dir + THUMB_DIR + "/" + name + ".png)](" + dir + name + ".svg)"
                + "<br>**" + caption + "**<br>"
                + "[SVG](" + dir + name + ".svg) · [PNG](" + dir + name + ".png) ";
    }

    /** The heading is the part of the section description before the em dash. */
    private static String headingOf(String description) {
        var dash = description.indexOf('—');
        return dash < 0 ? description : description.substring(0, dash).trim();
    }

    /** GitHub's anchor rule: lowercase, punctuation dropped, spaces to hyphens. */
    private static String anchorOf(String description) {
        return headingOf(description).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 _-]", "")
                .replace(' ', '-');
    }

    /**
     * Lays out a plot in an off-screen scene and captures it.
     *
     * @param figure the figure to render
     * @return the rendered image, converted pixel by pixel to stay free of the
     *         {@code javafx-swing} module
     */
    private static BufferedImage render(Figure figure) {
        var holder = new StackPane((Pane) figure.plot());
        holder.setPrefSize(figure.width(), figure.height());
        // The scene is never shown; it exists so that layout and CSS resolve.
        new Scene(holder, figure.width(), figure.height());

        // Force a full layout pass: the plot paints its canvas from layoutChildren()
        holder.applyCss();
        holder.layout();
        figure.plot().redraw();

        // Snapshot the node rather than the scene: only Node.snapshot honours the
        // scaling transform that gives the PNG its supersampled resolution.
        var params = new SnapshotParameters();
        params.setTransform(new Scale(SCALE, SCALE));
        params.setFill(Color.WHITE);
        var target = new WritableImage((int) (figure.width() * SCALE), (int) (figure.height() * SCALE));
        var snapshot = holder.snapshot(params, target);

        var w = (int) snapshot.getWidth();
        var h = (int) snapshot.getHeight();
        var buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var reader = snapshot.getPixelReader();
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    /**
     * Shrinks an image to a target width by repeated halving.
     * <p>
     * Going from the supersampled rendering straight to thumbnail size in one
     * step aliases badly — thin grid lines and small glyphs break up. Halving
     * until the last step is under a factor of two keeps every source pixel
     * contributing, which is what makes the small version still legible.
     *
     * @param source      the image to shrink
     * @param targetWidth the desired width in pixels
     * @return the scaled image, or the source if it is already small enough
     */
    private static BufferedImage downscale(BufferedImage source, int targetWidth) {
        if (source.getWidth() <= targetWidth) {
            return source;
        }
        var current = source;
        while (current.getWidth() / 2 > targetWidth) {
            current = resize(current, current.getWidth() / 2, Math.max(1, current.getHeight() / 2));
        }
        var height = Math.max(1, (int) Math.round(
                current.getHeight() * (double) targetWidth / current.getWidth()));
        return resize(current, targetWidth, height);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        var out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    /**
     * Writes an image to disk as a PNG.
     *
     * @param buffered the image to write
     * @param target   the destination file
     */
    private static void write(BufferedImage buffered, File target) {
        try {
            ImageIO.write(buffered, "png", target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }
}
