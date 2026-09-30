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
import static org.jtaccuino.gog.Geoms.contour3d;
import static org.jtaccuino.gog.Geoms.density3d;
import static org.jtaccuino.gog.Geoms.function3d;
import static org.jtaccuino.gog.Geoms.ridgeline3d;
import static org.jtaccuino.gog.Geoms.smooth3d;
import static org.jtaccuino.gog.Geoms.surface3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.Random;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.stat.SmoothDomain;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * The 3-D surface-family examples: {@code Geoms.surface3d()},
 * {@code Geoms.function3d()}, {@code Geoms.density3d()},
 * {@code Geoms.ridgeline3d()}, {@code Geoms.contour3d()} and
 * {@code Geoms.smooth3d()}, all over a {@code coord3d()} cube.
 */
public class Surface3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Surface3dPlots() {
    }

    /**
     * The sombrero function {@code z = sin(r) / r}, the classic 3D surface.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the sombrero height
     */
    private static double sombrero(double x, double y) {
        double r = Math.sqrt(x * x + y * y) + 1e-9;
        return Math.sin(r) / r;
    }

    /** {@return a regular 20×20 grid of x, y over {@code [-3, 3]}} */
    private static DataFrame gridData() {
        int n = 20;
        var xs = new double[n * n];
        var ys = new double[n * n];
        var zs = new double[n * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            double x = -3 + 6.0 * i / (n - 1);
            for (int j = 0; j < n; j++) {
                double y = -3 + 6.0 * j / (n - 1);
                xs[k] = x;
                ys[k] = y;
                zs[k] = sombrero(x, y);
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return scattered noisy data around a tilted plane for {@code Geoms.smooth3d()}} */
    private static DataFrame scatterData() {
        var rnd = new Random(42);
        int n = 400;
        var xs = new double[n];
        var ys = new double[n];
        var zs = new double[n];
        for (int i = 0; i < n; i++) {
            double x = rnd.nextDouble() * 8 - 4;
            double y = rnd.nextDouble() * 8 - 4;
            xs[i] = x;
            ys[i] = y;
            zs[i] = 0.5 * x - 0.25 * y + 2 * rnd.nextGaussian();
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /**
     * A slice-grid for {@code Geoms.ridgeline3d()}: one Gaussian ridge per unique x.
     *
     * @return the ridgeline grid data
     */
    private static DataFrame ridgeData() {
        int slices = 5;
        int per = 41;
        var xs = new double[slices * per];
        var ys = new double[slices * per];
        var zs = new double[slices * per];
        int k = 0;
        for (int s = 0; s < slices; s++) {
            double x = s;
            for (int i = 0; i < per; i++) {
                double y = -3 + 6.0 * i / (per - 1);
                double d = Math.exp(-0.5 * (y - (x - 2)) * (y - (x - 2)));
                xs[k] = x;
                ys[k] = y;
                zs[k] = 1.6 * d + 0.4;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return a sombrero surface from a regular grid — {@code Geoms.surface3d()}} */
        @SamplePlot(description = "The sombrero surface on a regular 20x20 grid.",
            title = "3D: Sombrero Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SURFACE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createGridSurface() {
        return ggplot(gridData(), aes().x("x").y("y").z("z"))
                .geoms(surface3d().fill(Color.web("#2ca02c")).linewidth(0.05))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.surface3d(): sombrero", "x", "y"));
    }

    /** {@return the same surface with right-triangle tiles and per-tile z fill} */
        @SamplePlot(description = "The sombrero with right-triangle tiles.",
            title = "3D: Right-Triangle Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SURFACE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createGridSurfaceRight() {
        return ggplot(gridData(), aes().x("x").y("y").z("z"))
                .geoms(surface3d().grid(GridGeometry.RIGHT1).fill(Color.web("#1f78b4")).linewidth(0.05))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.surface3d().grid(RIGHT1)", "x", "y"));
    }

    /** {@return the sombrero evaluated by {@code Geoms.function3d()}} */
        @SamplePlot(description = "The sombrero evaluated by Geoms.function3d().",
            title = "3D: Function Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createFunctionSurface() {
        return ggplot(gridData(), aes().x("x").y("y").z("z"))
                .geoms(function3d(Surface3dPlots::sombrero)
                        .xlim(-3, 3)
                        .ylim(-3, 3)
                        .n(50)
                        .fill(Color.web("#e31a1c")).linewidth(0.05))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.function3d(): sin(r)/r", "x", "y"));
    }

    /** {@return a 2-D kernel-density surface of the scatter — {@code Geoms.density3d()}} */
        @SamplePlot(description = "A 2D kernel-density surface of scattered data.",
            title = "3D: Density Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.DENSITY},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createDensitySurface() {
        return ggplot(scatterData(), aes().x("x").y("y").z("z"))
                .geoms(density3d().fill(Color.web("#9467bd")).linewidth(0.05))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.density3d(): kernel surface", "x", "y"));
    }

    /** {@return Gaussian ridges along the x axis — {@code Geoms.ridgeline3d()}} */
        @SamplePlot(description = "Gaussian ridges along the x axis.",
            title = "3D: Ridgeline",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.RIDGELINE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createRidgeline() {
        return ggplot(ridgeData(), aes().x("x").y("y").z("z"))
                .geoms(ridgeline3d().fill(Color.web("#ff7f00")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.ridgeline3d(): slices", "x", "y"));
    }

    /** {@return contour bands of the sombrero — {@code Geoms.contour3d()}} */
        @SamplePlot(description = "Contour bands of the sombrero surface.",
            title = "3D: Contour",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.CONTOUR},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createContour() {
        return ggplot(gridData(), aes().x("x").y("y").z("z"))
                .geoms(contour3d().bins(12).alpha(0.85))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.contour3d(): layer cake", "x", "y"));
    }

    /** {@return a loess surface fit with confidence surfaces — {@code Geoms.smooth3d()}} */
        @SamplePlot(description = "A loess surface fit with confidence surfaces.",
            title = "3D: Loess Surface",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SMOOTH},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createSmooth() {
        return ggplot(scatterData(), aes().x("x").y("y").z("z"))
                .geoms(smooth3d()
                        .method(SmoothMethod.LOESS).se(true).n(25)
                        .fill(Color.web("#6a3d9a")).color(Color.web("#4a2d6a"))
                        .points(true).pointColour(Color.web("#e31a1c")).pointSize(1.5)
                        .residuals(true).residualColour(Color.web("#888888")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.smooth3d(): loess + se", "x", "y"));
    }

    /** {@return an lm plane fit over the full domain — {@code Geoms.smooth3d()}} */
        @SamplePlot(description = "An lm plane fit over the full domain.",
            title = "3D: LM Plane Fit",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SMOOTH},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createSmoothLm() {
        return ggplot(scatterData(), aes().x("x").y("y").z("z"))
                .geoms(smooth3d().method(SmoothMethod.LM).domain(SmoothDomain.FULL).n(30)
                        .fill(Color.web("#1f78b4")).color(Color.web("#0f3868")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.smooth3d().method(LM).domain(FULL)", "x", "y"));
    }
}
