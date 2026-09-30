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
import static org.jtaccuino.gog.Geoms.density2d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.layer.Positions.positionOnFace;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.layer.GeomTile;
import org.jtaccuino.gog.layer.Point3dConfigurator;
import org.jtaccuino.gog.layer.Point3dSpec;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * 2-D/3-D mixing examples: {@code Positions.positionOnFace()} places layers
 * onto the faces of the 3-D cube. A native 3-D layer (mapping a {@code z}
 * aesthetic) is flattened onto a face like an orthogonal shadow; a 2-D layer
 * (x/y only) has its marks placed along two in-face dimensions with the third
 * fixed at the face plane. The examples pair a floating point cloud with
 * {@code Geoms.density2d()} contours or a {@code Geoms.tile()} heatmap on a
 * face, and flatten the cloud itself onto a face.
 */
public class Mixing3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Mixing3dPlots() {
    }

    /**
     * {@return a regular 5×5 grid whose column height and heatmap value derive
     * from the x/y position} — one frame of data shared by the 3-D cloud, the
     * density estimate and the heatmap.
     */
    private static DataFrame grid() {
        var xs = new double[25];
        var ys = new double[25];
        var zs = new double[25];
        var fills = new double[25];
        int k = 0;
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                xs[k] = x;
                ys[k] = y;
                zs[k] = x + y;
                fills[k] = x * y;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z", "fill")
                .of(Series.ofDouble(xs), Series.ofDouble(ys),
                        Series.ofDouble(zs), Series.ofDouble(fills));
    }

    /**
     * The floating 3-D point cloud shared by the mixed figures, with reference
     * elements switched off so the face layer reads cleanly.
     *
     * @return a {@code Geoms.point3d()} layer with no shadow furniture
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Point3dConfigurator<DataFrame> cloud() {
        return (Point3dConfigurator<DataFrame>) (Point3dConfigurator) point3d()
                .color(Color.web("#1f78b4"))
                .rawPoints(true).refLines(false).refPoints(Point3dSpec.RefPoints.NONE);
    }

    /**
     * A 2-D density estimate placed flat on the bottom face of the cube, under
     * the floating point cloud it summarises —
     * {@code Positions.positionOnFace(CubeFace.ZMIN, "x", "y")}.
     *
     * @return the density-on-face mixed figure
     */
    @SamplePlot(description = "2D density contours placed flat on the cube floor under 3D points.",
            title = "3D Mix: Density on Faces",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.MIXING})
    public static Plot<DataFrame> createDensityOnFaces() {
        return ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(cloud())
                .geoms(density2d().bins(6).lineWidth(1.2)
                        .position(positionOnFace(CubeFace.ZMIN, "x", "y")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.density2d() placed on the zmin face",
                        "x", "y"));
    }

    /**
     * A {@code Geoms.tile()} heatmap drawn flat on the top face of the cube,
     * with the 3-D point cloud floating above it.
     *
     * @return the tiles-on-face mixed figure
     */
    @SamplePlot(description = "A tile heatmap drawn flat on the cube top under 3D points.",
            title = "3D Mix: Tiles on Faces",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT, SampleGeom.TILE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.MIXING})
    public static Plot<DataFrame> createTilesOnFaces() {
        return ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(cloud())
                .layer(new GeomTile<DataFrame>().cmap("plasma")
                                .position(positionOnFace(CubeFace.ZMAX, "x", "y")),
                        aes().fill("fill"))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.tile() heatmap on the zmax face", "x", "y"));
    }

    /**
     * The 3-D cloud alongside its own orthogonal shadow on the bottom face:
     * the same {@code Geoms.point3d()} layer drawn once in the cube and once
     * flattened onto {@code zmin} via {@code positionOnFace("zmin")}.
     *
     * @return the flattened-3-D mixed figure
     */
    @SamplePlot(description = "A 3D point cloud alongside its own shadow on the cube floor.",
            title = "3D Mix: Flattened 3D",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.MIXING})
    public static Plot<DataFrame> createFlattened3d() {
        return ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(cloud())
                .geoms(point3d().color(Color.web("#d62728"))
                        .size(5).rawPoints(true).refLines(false)
                        .refPoints(Point3dSpec.RefPoints.NONE)
                        .position(positionOnFace(CubeFace.ZMIN)))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.point3d() flattened onto the zmin face",
                        "x", "y"));
    }
}
