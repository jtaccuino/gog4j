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
import static org.jtaccuino.gog.Geoms.bar3d;
import static org.jtaccuino.gog.Geoms.col3d;
import static org.jtaccuino.gog.Geoms.hull3d;
import static org.jtaccuino.gog.Geoms.voxel3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.Random;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.geometry.HullMethod;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The 3-D volume examples: {@code Geoms.col3d()}, {@code Geoms.bar3d()},
 * {@code Geoms.voxel3d()} and {@code Geoms.hull3d()} (convex and alpha), all
 * over a {@code coord3d()} cube.
 */
public class Volume3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Volume3dPlots() {
    }

    /** {@return a regular 5×5 grid with a noisy column height} */
    private static DataFrame columnGrid() {
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
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return discrete category counts for {@code Geoms.bar3d()}} */
    private static DataFrame discreteCounts() {
        var rnd = new Random(11);
        var xs = new String[200];
        var ys = new String[200];
        char[] xl = {'a', 'b', 'c', 'd'};
        char[] yl = {'A', 'B', 'C'};
        for (int i = 0; i < 200; i++) {
            xs[i] = String.valueOf(xl[rnd.nextInt(xl.length)]);
            ys[i] = String.valueOf(yl[rnd.nextInt(yl.length)]);
        }
        return DataFrame.byColumn("x", "y").of(Series.of(xs), Series.of(ys));
    }

    /** {@return continuous point pairs for the 2-D histogram} */
    private static DataFrame continuousPairs() {
        var rnd = new Random(13);
        int n = 800;
        var xs = new double[n];
        var ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = rnd.nextGaussian() * 2;
            ys[i] = rnd.nextGaussian() * 2;
        }
        return DataFrame.byColumn("x", "y").of(Series.ofDouble(xs), Series.ofDouble(ys));
    }

    /** {@return a sparse voxel grid} */
    private static DataFrame voxelData() {
        var rnd = new Random(17);
        int n = 120;
        var xs = new double[n];
        var ys = new double[n];
        var zs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = Math.round(rnd.nextGaussian() * 1.8);
            ys[i] = Math.round(rnd.nextGaussian() * 1.8);
            zs[i] = Math.round(rnd.nextGaussian() * 1.8);
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return points sampled on a unit sphere for the convex hull} */
    private static DataFrame sphereData() {
        var rnd = new Random(23);
        int n = 500;
        var xs = new double[n];
        var ys = new double[n];
        var zs = new double[n];
        for (int i = 0; i < n; i++) {
            double u = rnd.nextDouble();
            double v = rnd.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            xs[i] = Math.sin(phi) * Math.cos(theta);
            ys[i] = Math.sin(phi) * Math.sin(theta);
            zs[i] = Math.cos(phi);
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return a solid torus whose hole a convex hull cannot represent} */
    private static DataFrame torusData() {
        var rnd = new Random(1);
        int n = 1500;
        var xs = new double[n];
        var ys = new double[n];
        var zs = new double[n];
        for (int i = 0; i < n; i++) {
            double theta = rnd.nextDouble() * 2 * Math.PI;
            double phi = rnd.nextDouble() * 2 * Math.PI;
            double rho = Math.sqrt(rnd.nextDouble());
            xs[i] = (3 + rho * Math.cos(phi)) * Math.cos(theta);
            ys[i] = (3 + rho * Math.cos(phi)) * Math.sin(theta);
            zs[i] = rho * Math.sin(phi);
        }
        return DataFrame.byColumn("x", "y", "z").of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return a single column with no background panels — isolates the faces} */

    @SamplePlot(description = "A single 3D column with no background panels.",
            title = "3D: Single Column",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.PANELS})
    public static Plot<DataFrame> createSingleColumn() {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {3, 4}));
        return ggplot(df, aes().x("x").y("y").z("z"))
                .geoms(col3d().fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")))
                .coord(Coords.coord3d().panels(CubePanel.NONE))
                .labs(labs("single column, faces only", "x", "y"));
    }

    /** {@return 3-D columns from a grid — {@code Geoms.col3d()}} */

    @SamplePlot(description = "3D columns from a regular grid.",
            title = "3D: Column Grid",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createColumns() {
        return ggplot(columnGrid(), aes().x("x").y("y").z("z"))
                .geoms(col3d().fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.col3d(): grid columns", "x", "y"));
    }

    /** {@return columns with a uniform base level and gaps — {@code zmin}/{@code width}} */

    @SamplePlot(description = "3D columns with a raised base and gaps.",
            title = "3D: Columns zmin + gaps",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createColumnsZminGaps() {
        return ggplot(columnGrid(), aes().x("x").y("y").z("z"))
                .geoms(col3d().zmin(1.0).width(0.7)
                        .fill(Color.web("#1f78b4")).color(Color.web("#0f3868")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.col3d().zmin(1.0).width(0.7)", "x", "y"));
    }

    /** {@return columns showing only the top and front faces} */

    @SamplePlot(description = "3D columns showing only the top and front faces.",
            title = "3D: Column Faces",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.PANELS})
    public static Plot<DataFrame> createColumnsFaces() {
        return ggplot(columnGrid(), aes().x("x").y("y").z("z"))
                .geoms(col3d().faces(CubeFace.ZMAX, CubeFace.YMIN, CubeFace.XMIN)
                        .fill(Color.web("#9467bd")).color(Color.web("#5a2d78")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.col3d().faces(ZMAX, YMIN, XMIN)", "x", "y"));
    }

    /** {@return 3-D bars from discrete counts — {@code Geoms.bar3d()}} */

    @SamplePlot(description = "3D bars from discrete category counts.",
            title = "3D: Discrete Bars",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.BAR},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createBarsDiscrete() {
        return ggplot(discreteCounts(), aes().x("x").y("y"))
                .geoms(bar3d().fill(Color.web("#e31a1c")).color(Color.web("#8a1010")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.bar3d(): discrete counts", "x", "y"));
    }

    /** {@return a 3-D histogram of continuous data — {@code Geoms.bar3d().bins(12, 12)}} */

    @SamplePlot(description = "A 3D histogram of continuous data.",
            title = "3D: 2D Histogram",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.BAR},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createBarsContinuous() {
        return ggplot(continuousPairs(), aes().x("x").y("y"))
                .geoms(bar3d().bins(12, 12).fill(Color.web("#ff7f00")).color(Color.web("#8a4400")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.bar3d(): 2-D histogram", "x", "y"));
    }

    /** {@return 3-D voxels — {@code Geoms.voxel3d()}} */

    @SamplePlot(description = "Sparse 3D voxels.",
            title = "3D: Voxels",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.VOXEL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createVoxels() {
        return ggplot(voxelData(), aes().x("x").y("y").z("z"))
                .geoms(voxel3d().fill(Color.web("#6a3d9a")).color(Color.web("#3a1d5a")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.voxel3d(): sparse cubes", "x", "y"));
    }

    /** {@return the convex hull of a sphere — {@code Geoms.hull3d().method(CONVEX)}} */

    @SamplePlot(description = "The convex hull of a sphere point cloud.",
            title = "3D: Convex Hull",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.HULL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createHullConvex() {
        return ggplot(sphereData(), aes().x("x").y("y").z("z"))
                .geoms(hull3d().method(HullMethod.CONVEX).fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.hull3d().method(CONVEX)", "x", "y"));
    }

    /** {@return the alpha shape of a torus, preserving the hole} */

    @SamplePlot(description = "The alpha hull of a torus, preserving the hole.",
            title = "3D: Alpha Hull",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.HULL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createHullAlpha() {
        return ggplot(torusData(), aes().x("x").y("y").z("z"))
                .geoms(hull3d().method(HullMethod.ALPHA).radius(0.6)
                        .fill(Color.web("#1f78b4")).color(Color.web("#0f3868")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.hull3d().method(ALPHA).radius(0.6)", "x", "y"));
    }
}
