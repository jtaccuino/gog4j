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
import static org.jtaccuino.gog.AesValue.afterStat;
import static org.jtaccuino.gog.Coords.coord3d;
import static org.jtaccuino.gog.Geoms.function3d;
import static org.jtaccuino.gog.Ggplot.composedPlot;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Guides.guide;
import static org.jtaccuino.gog.Guides.guideColorbar3d;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleColorViridisC;
import static org.jtaccuino.gog.scale.Scales.scaleFillViridisC;

import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.ComposedPlot;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.coord.ScaleMode;
import org.jtaccuino.gog.data.EmptyDataFrame;
import org.jtaccuino.gog.theme.GuidePosition;
import org.jtaccuino.gog.theme.Theme;

/**
 * The "Controlling the 3D view" examples, composed as multi-panel figures: a
 * shared {@code Geoms.function3d(sin(x)*cos(y))} base plot rendered under
 * every {@code coord3d()} view control and arranged side by side (rotation,
 * perspective/distance, scales and ratio, panel selection, zoom/clip/expand,
 * axis label placement, and the 3D theme elements).
 */
public class CoordView3dArticlePlots {

    /** Utility class; not meant to be instantiated. */
    private CoordView3dArticlePlots() {
    }

    /** The {@code sin(x)*cos(y)} surface evaluated by the article's base plot. */
    private static double sincos(double x, double y) {
        return Math.sin(x) * Math.cos(y);
    }

    /**
     * The article's shared base plot: the {@code sin(x)*cos(y)} function
     * surface coloured by its own z, lit from the default direction with a
     * direct light, viridis scales, and a 3-D colourbar. Each cell keeps its
     * own small title (the view parameter being demonstrated), matching the
     * article's per-panel {@code ggtitle()}.
     *
     * @param title the panel title
     * @param coord the coordinate system to attach
     * @return the base plot
     */
    private static Plot<EmptyDataFrame> base(String title, Coord3D coord) {
        return ggplot(aes())
                .geoms(function3d(CoordView3dArticlePlots::sincos)
                        .xlim(-Math.PI, Math.PI)
                        .ylim(-Math.PI, Math.PI)
                        .n(40)
                        .mapping(aes()
                                .x(afterStat("x")).y(afterStat("y")).z(afterStat("z"))
                                .fill(afterStat("z")).color(afterStat("z"))))
                .light(Light3d.builder().method(Light3d.Method.DIRECT).contrast(0.7).build())
                .scales(scaleFillViridisC())
                .scales(scaleColorViridisC())
                .guides(guide(Aesthetic.FILL, guideColorbar3d()))
                .theme(t -> t.guidePosition(GuidePosition.RIGHT))
                .coord(coord)
                .labs(labs(title, "x", "y"));
    }

    /** {@return the rotation panel: default, all-zero, and arbitrary angles} */
    public static Plot<EmptyDataFrame> createRotationDefault() {
        return base("Default", coord3d());
    }

    /** {@return the rotation panel: all angles zero} */
    public static Plot<EmptyDataFrame> createRotationZero() {
        return base("All angles 0", coord3d().pitch(0).roll(0).yaw(0));
    }

    /** {@return the rotation panel: an arbitrary combination} */
    public static Plot<EmptyDataFrame> createRotationArbitrary() {
        return base("Arbitrary combination", coord3d().pitch(20).roll(40).yaw(60));
    }

    /** {@return the rotation panel: pitch 30} */
    public static Plot<EmptyDataFrame> createRotationPitch30() {
        return base("pitch = 30", coord3d().pitch(30).roll(0).yaw(0));
    }

    /** {@return the rotation panel: roll 30} */
    public static Plot<EmptyDataFrame> createRotationRoll30() {
        return base("roll = 30", coord3d().pitch(0).roll(30).yaw(0));
    }

    /** {@return the rotation panel: yaw 30} */
    public static Plot<EmptyDataFrame> createRotationYaw30() {
        return base("yaw = 30", coord3d().pitch(0).roll(0).yaw(30));
    }

    /**
     * The article's rotation figure: the default view, the zero-rotation case,
     * an arbitrary combination, and each single-angle rotation, in a 2×3 grid.
     *
     * @return the rotation composition
     */
    public static ComposedPlot createRotation() {
        return composedPlot(
                createRotationDefault(), createRotationZero(), createRotationArbitrary(),
                createRotationPitch30(), createRotationRoll30(), createRotationYaw30())
                .rows(2).cols(3)
                .title("coord3d() rotation");
    }

    /** {@return the perspective panel: dist 1.1} */
    public static Plot<EmptyDataFrame> createPerspectiveDist11() {
        return base("dist = 1.1", coord3d().yaw(45).dist(1.1));
    }

    /** {@return the perspective panel: dist 2} */
    public static Plot<EmptyDataFrame> createPerspectiveDist2() {
        return base("dist = 2", coord3d().yaw(45).dist(2));
    }

    /** {@return the perspective panel: orthographic} */
    public static Plot<EmptyDataFrame> createPerspectiveOrtho() {
        return base("persp = FALSE", coord3d().yaw(45).persp(false));
    }

    /**
     * The article's perspective figure: two camera distances and the
     * orthographic projection, side by side.
     *
     * @return the perspective composition
     */
    public static ComposedPlot createPerspective() {
        return composedPlot(
                createPerspectiveDist11(), createPerspectiveDist2(), createPerspectiveOrtho())
                .rows(1).cols(3)
                .title("coord3d() perspective and distance");
    }

    /** {@return the scales panel: free} */
    public static Plot<EmptyDataFrame> createScalesFree() {
        return base("scales = \"free\"", coord3d().scales(ScaleMode.FREE));
    }

    /** {@return the scales panel: fixed} */
    public static Plot<EmptyDataFrame> createScalesFixed() {
        return base("scales = \"fixed\"", coord3d().scales(ScaleMode.FIXED));
    }

    /**
     * The article's scales figure: free versus fixed axis scaling.
     *
     * @return the scales composition
     */
    public static ComposedPlot createScales() {
        return composedPlot(createScalesFree(), createScalesFixed())
                .rows(1).cols(2)
                .title("coord3d() scales");
    }

    /** {@return the ratio panel: free scales with a 1:3:1 ratio} */
    public static Plot<EmptyDataFrame> createRatioFree() {
        return base("free, ratio = (1, 3, 1)", coord3d().scales(ScaleMode.FREE).ratio(1, 3, 1));
    }

    /** {@return the ratio panel: fixed scales with a 1:3:1 ratio} */
    public static Plot<EmptyDataFrame> createRatioFixed() {
        return base("fixed, ratio = (1, 3, 1)", coord3d().scales(ScaleMode.FIXED).ratio(1, 3, 1));
    }

    /**
     * The article's ratio figure: the 1:3:1 axis ratio under free and fixed
     * scales.
     *
     * @return the ratio composition
     */
    public static ComposedPlot createRatio() {
        return composedPlot(createRatioFree(), createRatioFixed())
                .rows(1).cols(2)
                .title("coord3d() ratio");
    }

    /** {@return the panels panel: background (the default)} */
    public static Plot<EmptyDataFrame> createPanelsBackground() {
        return base("\"background\" (default)", coord3d().panels(CubePanel.BACKGROUND));
    }

    /** {@return the panels panel: the explicit xmin/xmax/zmax faces} */
    public static Plot<EmptyDataFrame> createPanelsFaces() {
        return base("c(\"xmin\", \"xmax\", \"zmax\")",
                coord3d().panels(CubePanel.XMIN, CubePanel.XMAX, CubePanel.ZMAX));
    }

    /** {@return the panels panel: all six faces} */
    public static Plot<EmptyDataFrame> createPanelsAll() {
        return base("\"all\"", coord3d().panels(CubePanel.ALL));
    }

    /** {@return the panels panel: no faces} */
    public static Plot<EmptyDataFrame> createPanelsNone() {
        return base("\"none\"", coord3d().panels(CubePanel.NONE));
    }

    /**
     * The article's panels figure: background, specific faces, all faces, and
     * no faces, in a 2×2 grid.
     *
     * @return the panels composition
     */
    public static ComposedPlot createPanels() {
        return composedPlot(
                createPanelsBackground(), createPanelsFaces(), createPanelsAll(), createPanelsNone())
                .rows(2).cols(2)
                .title("coord3d() panels");
    }

    /** {@return the zoom panel: zoom 0.7} */
    public static Plot<EmptyDataFrame> createZoom07() {
        return base("zoom = 0.7", coord3d().zoom(0.7));
    }

    /** {@return the zoom panel: zoom 1} */
    public static Plot<EmptyDataFrame> createZoom1() {
        return base("zoom = 1", coord3d().zoom(1));
    }

    /** {@return the zoom panel: zoom 1.5 with clipping on} */
    public static Plot<EmptyDataFrame> createZoom15Clip() {
        return base("zoom = 1.5, clip = \"on\"", coord3d().zoom(1.5).clip(true));
    }

    /** {@return the expand panel: default expansion} */
    public static Plot<EmptyDataFrame> createExpandTrue() {
        return base("expand = TRUE", coord3d().expand(true));
    }

    /** {@return the expand panel: no expansion} */
    public static Plot<EmptyDataFrame> createExpandFalse() {
        return base("expand = FALSE", coord3d().expand(false));
    }

    /**
     * The article's zoom figure: zoom 0.7, 1, and 1.5 with clipping, side by
     * side.
     *
     * @return the zoom composition
     */
    public static ComposedPlot createZoom() {
        return composedPlot(createZoom07(), createZoom1(), createZoom15Clip())
                .rows(1).cols(3)
                .title("coord3d() zoom and clip");
    }

    /**
     * The article's expand figure: default versus disabled expansion.
     *
     * @return the expand composition
     */
    public static ComposedPlot createExpand() {
        return composedPlot(createExpandTrue(), createExpandFalse())
                .rows(1).cols(2)
                .title("coord3d() expand");
    }

    /** {@return the label panel: automatic placement} */
    public static Plot<EmptyDataFrame> createLabelsAuto() {
        return base("auto (default)", coord3d());
    }

    /** {@return the label panel: manual x/z label edges} */
    public static Plot<EmptyDataFrame> createLabelsManual() {
        return base("manual placement",
                coord3d().xlabels(CubeFace.YMAX, CubeFace.ZMAX)
                        .zlabels(CubeFace.XMAX, CubeFace.YMIN));
    }

    /** {@return the rotate-labels panel: labels follow the projected axis} */
    public static Plot<EmptyDataFrame> createRotateLabelsTrue() {
        return base("rotate_labels = TRUE", coord3d().rotateLabels(true));
    }

    /** {@return the rotate-labels panel: labels stay horizontal} */
    public static Plot<EmptyDataFrame> createRotateLabelsFalse() {
        return base("rotate_labels = FALSE", coord3d().rotateLabels(false));
    }

    /**
     * The article's label-placement figure: auto versus manual edges, and the
     * rotated versus fixed label orientation.
     *
     * @return the label composition
     */
    public static ComposedPlot createLabels() {
        return composedPlot(
                createLabelsAuto(), createLabelsManual(),
                createRotateLabelsTrue(), createRotateLabelsFalse())
                .rows(2).cols(2)
                .title("coord3d() axis label placement");
    }

    /** {@return the theming panel: the dark theme} */
    public static Plot<EmptyDataFrame> createThemeDark() {
        return base("theme_dark()", coord3d()).theme(Theme.theme_dark());
    }

    /** {@return the theming panel: the minimal theme} */
    public static Plot<EmptyDataFrame> createThemeMinimal() {
        return base("theme_minimal()", coord3d()).theme(Theme.theme_minimal());
    }

    /**
     * The article's theming figure: the dark and minimal complete themes side
     * by side.
     *
     * @return the theming composition
     */
    public static ComposedPlot createThemes() {
        return composedPlot(createThemeDark(), createThemeMinimal())
                .rows(1).cols(2)
                .title("coord3d() theming");
    }

    /** {@return the foreground-panel theming figure: all faces, themed} */
    public static ComposedPlot createThemeForeground() {
        return composedPlot(
                base("theme_gray() + foreground panels",
                        coord3d().panels(CubePanel.ALL)).theme(Theme.theme_gray()),
                base("theme_light() + panel.foreground",
                        coord3d().panels(CubePanel.ALL)).theme(Theme.theme_light()))
                .rows(1).cols(2)
                .title("coord3d() foreground panels");
    }

    /** {@return the per-axis theme figure: custom tick, text, and title colours} */
    public static ComposedPlot createThemeAxes() {
        return composedPlot(
                base("per-axis text theme", coord3d().panels(CubePanel.ALL))
                        .theme(t -> t.axisTickColor(Color.web("#666666"))
                                .axisTitleColor(Color.web("#8b0000"))
                                .tickLabelColor(Color.web("#003366"))),
                base("depth-scaled furniture", coord3d().panels(CubePanel.ALL))
                        .theme(Theme.theme_light()))
                .rows(1).cols(2)
                .title("coord3d() axis text theme");
    }
}
