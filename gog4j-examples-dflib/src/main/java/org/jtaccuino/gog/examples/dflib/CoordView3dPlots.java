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
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.ScaleMode;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * 3D view sweep examples exercising the {@code coord3d()} view controls:
 * rotation, camera distance and projection, axis scales and ratios, the cube
 * face panels, clipping, expansion, and the axis furniture placement.
 */
public class CoordView3dPlots {

    /** Utility class; not meant to be instantiated. */
    private CoordView3dPlots() {
    }

    /**
     * The default 3-D view: pitch 0, roll −60, yaw −30, distance 2.
     *
     * @return the default-view example plot
     */
    @SamplePlot(description = "The default 3D cube view: pitch 0, roll -60, yaw -30.",
            title = "3D: Default View",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createDefaultView() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d())
                .labs(labs("Coord3D: Default View", "carat", "price"));
    }

    /**
     * A view rotated away from the default to show the cube frontally.
     *
     * @return the rotation-sweep example plot
     */
    @SamplePlot(description = "A view rotated to show the cube frontally.",
            title = "3D: Rotation Sweep",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createRotationSweep() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().pitch(35).roll(-75).yaw(-55))
                .labs(labs("Coord3D: Rotation (pitch 35, roll -75, yaw -55)", "carat", "price"));
    }

    /**
     * Orthographic projection keeps parallel edges and drops the depth size cue.
     *
     * @return the orthographic example plot
     */
    @SamplePlot(description = "Orthographic projection keeps parallel edges.",
            title = "3D: Orthographic",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createOrthographic() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().persp(false))
                .labs(labs("Coord3D: Orthographic (persp off)", "carat", "price"));
    }

    /**
     * A camera closer than the default strengthens the local perspective.
     *
     * @return the close-camera example plot
     */
    @SamplePlot(description = "A closer camera strengthens the local perspective.",
            title = "3D: Close Camera",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createCloseCamera() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().dist(1.25))
                .labs(labs("Coord3D: Close camera (dist 1.25)", "carat", "price"));
    }

    /**
     * A distant camera flattens the scene towards the orthographic limit.
     *
     * @return the distant-camera example plot
     */
    @SamplePlot(description = "A distant camera flattens the scene.",
            title = "3D: Distant Camera",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createDistantCamera() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().dist(4))
                .labs(labs("Coord3D: Distant camera (dist 4)", "carat", "price"));
    }

    /**
     * Fixed scales preserve the data aspect instead of normalizing each axis.
     *
     * @return the fixed-scales example plot
     */
    @SamplePlot(description = "Fixed scales preserve the data aspect.",
            title = "3D: Fixed Scales",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createFixedScales() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().scales(ScaleMode.FIXED))
                .labs(labs("Coord3D: Fixed scales", "carat", "price"));
    }

    /**
     * Non-uniform ratios emphasize one axis relative to the others.
     *
     * @return the non-uniform-ratio example plot
     */
    @SamplePlot(description = "Non-uniform ratios emphasize one axis.",
            title = "3D: Non-Uniform Ratio",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createNonUniformRatio() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().ratio(1.6, 1, 1.3))
                .labs(labs("Coord3D: Ratio (1.6, 1, 1.3)", "carat", "price"));
    }

    /**
     * Zooming beyond the fitted cube magnifies the projected scene.
     *
     * @return the zoomed example plot
     */
    @SamplePlot(description = "Zooming magnifies the projected scene.",
            title = "3D: Zoomed",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createZoomed() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().zoom(1.25))
                .labs(labs("Coord3D: Zoom 1.25", "carat", "price"));
    }

    /**
     * All six faces are drawn, with the foreground ones in the fg alpha.
     *
     * @return the all-panels example plot
     */
    @SamplePlot(description = "All six cube faces are drawn.",
            title = "3D: Panels All",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.PANELS})
    public static Plot<DataFrame> createAllPanels() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().panels(CubePanel.ALL))
                .labs(labs("Coord3D: Panels all", "carat", "price"));
    }

    /**
     * Only the foreground faces are drawn — an open box towards the camera.
     *
     * @return the foreground-panels example plot
     */
    @SamplePlot(description = "Only the foreground cube faces are drawn.",
            title = "3D: Panels Foreground",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.PANELS})
    public static Plot<DataFrame> createForegroundPanels() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().panels(CubePanel.FOREGROUND))
                .labs(labs("Coord3D: Panels foreground", "carat", "price"));
    }

    /**
     * No grid faces at all: the geometry floats in the empty cube.
     *
     * @return the no-panels example plot
     */
    @SamplePlot(description = "No grid faces: geometry floats in the empty cube.",
            title = "3D: Panels None",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.PANELS})
    public static Plot<DataFrame> createNoPanels() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().panels(CubePanel.NONE))
                .labs(labs("Coord3D: Panels none", "carat", "price"));
    }

    /**
     * Clipping confines overflow to the panel bounds.
     *
     * @return the clipped example plot
     */
    @SamplePlot(description = "Clipping confines overflow to the panel bounds.",
            title = "3D: Clip On",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createClipped() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().clip(true))
                .labs(labs("Coord3D: Clip on", "carat", "price"));
    }

    /**
     * Disabling the default expansion tightens the cube around the data.
     *
     * @return the tight-bounds example plot
     */
    @SamplePlot(description = "Disabling the default expansion tightens the cube.",
            title = "3D: Expand Off",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createTightBounds() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().expand(false))
                .labs(labs("Coord3D: Expand off", "carat", "price"));
    }

    /**
     * Axis titles centered on their edges instead of at the near end.
     *
     * @return the centered-titles example plot
     */
    public static Plot<DataFrame> createCenteredTitles() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d())
                .labs(labs("Coord3D: Center titles", "carat", "price"));
    }

    /**
     * Explicit edges for the x and z label chains
     * ({@code coord3d().xlabels(CubeFace.YMAX, CubeFace.ZMAX)
     * .zlabels(CubeFace.XMAX, CubeFace.YMIN)}),
     * with the y axis left on automatic peripheral selection.
     *
     * @return the custom-label-edges example plot
     */
    public static Plot<DataFrame> createCustomLabelEdges() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d()
                        .xlabels(CubeFace.YMAX, CubeFace.ZMAX)
                        .zlabels(CubeFace.XMAX, CubeFace.YMIN))
                .labs(labs("Coord3D: Custom label edges", "carat", "price"));
    }

    /**
     * Zero depth scaling disables the perspective size cue on all furniture.
     *
     * @return the flat-depth-scale example plot
     */
    @SamplePlot(description = "Zero depth scaling disables the perspective size cue.",
            title = "3D: Flat Depth Scale",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createFlatDepthScale() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d().scaleDepth(0, 0, 0, 0).rotateLabels(false))
                .labs(labs("Coord3D: Flat depth scale", "carat", "price"));
    }

    private static DataFrame data() {
        return DiamondsDatasets.loadDiamonds().head(1500);
    }
}
