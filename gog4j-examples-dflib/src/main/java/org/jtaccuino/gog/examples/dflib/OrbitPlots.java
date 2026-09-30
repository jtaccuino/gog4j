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
import static org.jtaccuino.gog.Geoms.hull3d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.surface3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Ggplot.ggplot3d;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.dflib.data.MountainDatasets;
import org.jtaccuino.gog.dflib.data.SpherePointsDatasets;
import org.jtaccuino.gog.geometry.HullMethod;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The {@code Orbit3d} showcase and the demo-data completeness figures: a
 * sphere point cloud for gesture rotation, a lit mountain for the
 * {@link org.jtaccuino.gog.interaction.CubeOrbitControl} mini-globe, and the
 * {@code Geoms.surface3d()}/{@code Geoms.hull3d()} exercises over the two new
 * {@code MountainDatasets}/{@code SpherePointsDatasets} demo datasets.
 */
public class OrbitPlots {

    /** Utility class; not meant to be instantiated. */
    private OrbitPlots() {
    }

    /** {@return the gesture-rotation showcase: a coloured sphere point cloud} */
    @SamplePlot(description = "A coloured sphere point cloud ready for Orbit3d drag rotation.",
            title = "Orbit: Sphere Points",
            dataset = SampleDataset.SPHERE,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.ORBIT})
    public static Plot<DataFrame> createSpherePoints() {
        return ggplot3d(SpherePointsDatasets.loadSpherePoints(),
                aes().x("x").y("y").z("z").color("band"))
                .geoms(point3d().size(3.0))
                .labs(labs("Orbit3d: drag to rotate", "x", "y"));
    }

    /** {@return the mini-globe showcase: the mountain surface under a plot-level light} */
    @SamplePlot(description = "The lit mountain surface as a mini-globe Orbit3d showcase.",
            title = "Orbit: Lit Mountain",
            dataset = SampleDataset.MOUNTAIN,
            geoms = {SampleGeom.SURFACE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.ORBIT, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createLitMountain() {
        return ggplot(MountainDatasets.loadMountain(), aes().x("x").y("y").z("z"))
                .geoms(surface3d().fill(Color.web("#1f78b4")).linewidth(0.05))
                .coord(Coords.coord3d())
                .light(Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                        0.6, new double[] {-0.5, 0, 1}, null, false,
                        Light3d.Anchor.SCENE, -1, 0))
                .labs(labs("Orbit3d: mini-globe control", "x", "y"));
    }

    /** {@return the prairie mountain surface on the 45×45 demo grid} */
    @SamplePlot(description = "The 45x45 mountain demo grid as a green 3D surface.",
            title = "3D: Mountain Surface",
            dataset = SampleDataset.MOUNTAIN,
            geoms = {SampleGeom.SURFACE},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createMountainSurface() {
        return ggplot(MountainDatasets.loadMountain(), aes().x("x").y("y").z("z"))
                .geoms(surface3d().fill(Color.web("#2ca02c")).linewidth(0.05))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.surface3d(): mountain", "x", "y"));
    }

    /** {@return the convex hull of the lumpy sphere over its point cloud} */
    @SamplePlot(description = "The convex hull of the lumpy sphere over its point cloud.",
            title = "3D: Sphere Hull",
            dataset = SampleDataset.SPHERE,
            geoms = {SampleGeom.HULL, SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createSphereHull() {
        return ggplot(SpherePointsDatasets.loadSpherePoints(), aes().x("x").y("y").z("z"))
                .geoms(hull3d().method(HullMethod.CONVEX)
                                .fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")),
                        point3d().size(2.0))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.hull3d(): sphere points", "x", "y"));
    }
}
