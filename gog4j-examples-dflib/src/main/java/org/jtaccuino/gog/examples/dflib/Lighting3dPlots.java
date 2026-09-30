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
package org.jtaccuino.gog.examples.dflib;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.col3d;
import static org.jtaccuino.gog.Geoms.function3d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.surface3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Guides.guide;
import static org.jtaccuino.gog.Guides.guideColorbar3d;
import static org.jtaccuino.gog.Guides.guideLegend3d;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.Random;
import java.util.function.DoubleBinaryOperator;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.layer.Polygon3dSpec;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SampleGuide;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The 3-D lighting examples: the {@code Light3d} pipeline behind every 3D
 * layer — the {@code method} (diffuse/direct/rgb/none), the HSV and HSL blend
 * modes, contrast strength, the scene vs camera anchors, positional lights
 * with distance falloff, and backface negation.
 */
public class Lighting3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Lighting3dPlots() {
    }

    /** {@return a single flat column exposing its faces for lighting} */
    private static DataFrame column() {
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {3, 4}));
    }

    /** {@return a regular 5×5 grid with a noisy column height} */
    private static DataFrame grid() {
        var rnd = new Random(7);
        var xs = new double[25];
        var ys = new double[25];
        var zs = new double[25];
        int k = 0;
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                xs[k] = x;
                ys[k] = y;
                zs[k] = x + y + rnd.nextGaussian() * 0.5;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return a regular 20×20 grid of x, y over [-3, 3] on the sombrero} */
    private static DataFrame sombreroGrid() {
        int n = 20;
        var xs = new double[n * n];
        var ys = new double[n * n];
        var zs = new double[n * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            double x = -3 + 6.0 * i / (n - 1);
            for (int j = 0; j < n; j++) {
                double y = -3 + 6.0 * j / (n - 1);
                double r = Math.sqrt(x * x + y * y) + 1e-9;
                xs[k] = x;
                ys[k] = y;
                zs[k] = Math.sin(r) / r;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    private static Plot<DataFrame> columnPlot(String title, Light3d light) {
        // The layer carries no explicit light, so the plot-level light applies.
        return ggplot(column(), aes().x("x").y("y").z("z"))
                .geoms(col3d().fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")))
                .coord(Coords.coord3d().panels(CubePanel.NONE))
                .light(light)
                .labs(labs(title, "x", "y"));
    }

    /** {@return no shading at all — same as a {@code null} light} */
    @SamplePlot(description = "Flat 3D column faces with lighting disabled.",
            title = "3D Light: Method None",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createMethodNone() {
        return columnPlot("light(method = 'none') flat faces", Light3d.none());
    }

    /** {@return the classic diffuse shading from above-front} */
    @SamplePlot(description = "Classic diffuse shading of 3D columns from above-front.",
            title = "3D Light: Method Diffuse",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createMethodDiffuse() {
        return columnPlot("light(method = 'diffuse')",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.7, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return direct shading: unused side of the light clamps flat} */
    @SamplePlot(description = "Direct 3D shading: the far side of each face clamps flat.",
            title = "3D Light: Method Direct",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createMethodDirect() {
        return columnPlot("light(method = 'direct')",
                Light3d.of(Light3d.Method.DIRECT, Light3d.Mode.HSL, true, true,
                        1.0, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return rgb lighting replacing the fill with a per-normal colour} */
    @SamplePlot(description = "RGB 3D lighting replacing the fill with per-normal colours.",
            title = "3D Light: Method RGB",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createMethodRgb() {
        return columnPlot("light(method = 'rgb', mode = 'rgb')",
                Light3d.of(Light3d.Method.RGB, Light3d.Mode.HSV, true, true,
                        1.0, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return hue-preserving shading in HSV space (modifies brightness)} */
    @SamplePlot(description = "Hue-preserving 3D shading in HSV space.",
            title = "3D Light: HSV Mode",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createModeHsv() {
        return columnPlot("light(mode = 'hsv')",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSV, true, true,
                        0.7, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return hue-preserving shading in HSL space (modifies lightness)} */
    @SamplePlot(description = "Hue-preserving 3D shading in HSL space.",
            title = "3D Light: HSL Mode",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createModeHsl() {
        return columnPlot("light(mode = 'hsl')",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.7, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return a deliberately gentle shade over the faces} */
    @SamplePlot(description = "3D shading with a gentle 0.2 contrast.",
            title = "3D Light: Low Contrast",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createContrastLow() {
        return columnPlot("light(contrast = 0.2)",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.2, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return the light direction sweeping from above-front to below-back} */
    @SamplePlot(description = "The 3D light direction sweeping from above-front to below-back.",
            title = "3D Light: Direction",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createDirection() {
        return columnPlot("light(direction = c(0.5, 0, -1))",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.8, new double[] {0.5, 0, -1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0));
    }

    /** {@return a camera-anchored light that rotates with the view} */
    @SamplePlot(description = "A camera-anchored 3D light that rotates with the view.",
            title = "3D Light: Camera Anchor",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createCameraAnchor() {
        return columnPlot("light(anchor = 'camera')",
                Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.7, new double[] {0, 0, 1}, null, false,
                        Light3d.Anchor.CAMERA, -1, 0));
    }

    /** {@return a point light with inverse-square distance falloff} */
    @SamplePlot(description = "A positional 3D light with inverse-square falloff.",
            title = "3D Light: Positional",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createPositional() {
        return ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(col3d().fill(Color.web("#9467bd")).color(Color.web("#5a2d78")))
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.9, new double[] {3, 3, 2}, new double[] {3, 3, 2}, true,
                        Light3d.Anchor.SCENE, -1, 0))
                .labs(labs("light(position = 3, 3, 2, distance_falloff = TRUE)",
                        "x", "y"));
    }

    /** {@return backfaces negated so inner walls read as dark mirrors} */
    @SamplePlot(description = "Backfaces negated so inner 3D walls read as dark mirrors.",
            title = "3D Light: Backfaces",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createBackfaces() {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(new double[] {1, 2, 3, 4}),
                        Series.ofDouble(new double[] {1, 1, 1, 1}),
                        Series.ofDouble(new double[] {3, 4, 2, 5}));
        return ggplot(df, aes().x("x").y("y").z("z"))
                .geoms(col3d().fill(Color.web("#6a3d9a")).color(Color.web("#3a1d5a")))
                .coord(Coords.coord3d().panels(CubePanel.NONE))
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.8, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .labs(labs("light(backface_scale = -1)", "x", "y"));
    }

    /**
     * Both {@code Plot.light(Light3d)} and {@code coord3d(light = ...)} fill
     * the same slot, so the two must not be combined at once.
     *
     * @return a column lit from the coord level
     */
    @SamplePlot(description = "A 3D light supplied directly on the coord3d() call.",
            title = "3D Light: Coord Light",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createCoordLight() {
        return ggplot(column(), aes().x("x").y("y").z("z"))
                // The layer carries no explicit light, so the coord-level light
                // below shines through untouched.
                .geoms(col3d().fill(Color.web("#ff7f00")).color(Color.web("#8a4400")))
                .coord(Coords.coord3d().panels(CubePanel.NONE)
                        .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL,
                                true, true, 0.5, new double[] {-0.5, 0, 1}, null,
                                false, Light3d.Anchor.SCENE, -1, 0)))
                .labs(labs("light supplied on coord3d()", "x", "y"));
    }

    /**
     * The same plot-level light as {@link #createMethodDiffuse()}, now shading
     * a {@code Geoms.surface3d()} terrain instead of a column: surfaces inherit
     * the coord/plot light unless the layer sets its own.
     *
     * @return the sombrero surface lit from above-front
     */
    @SamplePlot(description = "A 3D surface terrain lit from above-front.",
            title = "3D Light: Lit Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SURFACE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createSurfaceLit() {
        return ggplot(sombreroGrid(), aes().x("x").y("y").z("z"))
                .geoms(surface3d().fill(Color.web("#1f78b4")).linewidth(0.05))
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.6, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .labs(labs("Geoms.surface3d() lit from above-front", "x", "y"));
    }

    /**
     * A {@code Geoms.function3d()} surface defined by an explicit function,
     * shaded by a directional light sweeping from the side.
     *
     * @return the two-hump function surface with a side light
     */
    @SamplePlot(description = "A 3D function surface shaded by a side light.",
            title = "3D Light: Lit Function",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createFunctionLit() {
        DoubleBinaryOperator peaks = (x, y) -> 3 * (1 - x) * (1 - x)
                * Math.exp(-(x * x) - (y + 1) * (y + 1))
                - 10 * (x / 5 - Math.pow(x, 3) - Math.pow(y, 5)) * Math.exp(-x * x - y * y)
                - 1.0 / 3 * Math.exp(-(x + 1) * (x + 1) - y * y);
        return ggplot(sombreroGrid(), aes().x("x").y("y").z("z"))
                .geoms(function3d(peaks)
                        .xlim(-3, 3).ylim(-3, 3).n(50)
                        .sortMethod(Polygon3dSpec.SortMethod.PAIRWISE)
                        .fill(Color.web("#e31a1c")).linewidth(0.05))
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.7, new double[] {0, -0.6, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .labs(labs("Geoms.function3d() with a side light", "x", "y"));
    }

    /**
     * A 3-D scatter whose point fill shares the plotted z column's continuous
     * colour scale, rendered with a {@code guideColorbar3d()} — a colourbar
     * shaded as if lit from one side, matching the cube's lighting.
     *
     * @return the 3-D colourbar guide figure
     */
    @SamplePlot(description = "A 3D colourbar guide shaded as if lit, sharing the fill scale.",
            title = "3D Light: Colourbar Guide",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            guides = {SampleGuide.COLORBAR},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createGuideColorbar3d() {
        return ggplot(grid(), aes().x("x").y("y").z("z").color("z"))
                .geoms(point3d())
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.6, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .guides(guide(Aesthetic.COLOR, guideColorbar3d()))
                .labs(labs("guideColorbar3d() sharing the fill scale",
                        "x", "y"));
    }

    /**
     * A 3-D scatter grouped by a categorical column, rendered with a
     * {@code guideLegend3d()} — each key shaded as a uniformly lit tile.
     *
     * @return the 3-D legend guide figure
     */
    @SamplePlot(description = "A 3D legend guide with uniformly lit keys.",
            title = "3D Light: Legend Guide",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            guides = {SampleGuide.KEY_COLOUR},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createGuideLegend3d() {
        var df = DataFrame.byColumn("x", "y", "z", "grp")
                .of(Series.ofDouble(new double[] {1, 2, 3, 4, 5, 6}),
                        Series.ofDouble(new double[] {1, 1, 2, 2, 3, 3}),
                        Series.ofDouble(new double[] {3, 4, 5, 2, 3, 6}),
                        Series.of("first", "second", "third",
                                "first", "second", "third"));
        return ggplot(df, aes().x("x").y("y").z("z").color("grp"))
                .geoms(point3d())
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.6, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .guides(guide(Aesthetic.COLOR, guideLegend3d()))
                .labs(labs("guideLegend3d() with lit keys", "x", "y"));
    }
}
