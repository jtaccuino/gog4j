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
package org.jtaccuino.gog.theme;

import javafx.scene.paint.Color;

/**
 * Theme-derived drawing constants for the 3D cube, so the coordinate system
 * stays decoupled from the plot's theme — the {@link AxisStyle} counterpart for
 * the third dimension. Every visual element of the cube has its own hook in
 * {@link Theme3d}; this bundle resolves them once, mirroring how
 * {@link AxisStyle#from(Theme)} centralizes axis styling.
 * <p>
 * Themes that do not implement the optional {@link Theme3d} element set keep
 * the conventional cube look: identical to the values the 3D coordinate system
 * used before the elements existed.
 *
 * @param panelPad        the horizontal/vertical inset of the cube within its
 *                        panel, in pixels
 * @param sizeScale       the fraction of the inner panel used for the cube
 *                        projection scale
 * @param zCategoricalPad the fractional [0,1] inset along a categorical z-axis
 * @param zTickLength     the tick length of the cube axes, in projection units
 * @param panel           the {@code panel.background} rect of the cube (back
 *                        three face panels)
 * @param panelForeground the {@code panel.foreground} rect of the cube (front
 *                        three face panels)
 * @param grid            the {@code grid} colour of the cube face panels
 * @param gridForeground  the {@code grid.foreground} colour of the front
 *                        face panels
 * @param gridLineWidth   the stroke width of the cube grid lines, in pixels
 * @param tickLabelPad    the extra pixel clearance between a cube tick label
 *                        and its tick mark
 * @param axisTitlePad    the extra pixel clearance between a cube axis title
 *                        and the tick labels (the title's own half-extent is
 *                        added on top)
 * @param depthScaleStrength the strength of the depth scale applied to 3D
 *                        geometry
 * @param depthScaleMidpoint the depth at which depth scaling leaves geometry
 *                        unscaled
 * @param point3dStrokeWidth the stroke width of the outline drawn around
 *                        depth-scaled 3D points, in pixels
 * @param point3dHitTolerance the extra pixel tolerance beyond a 3D point's
 *                        radius when hit-testing during interactive locating
 * @param zAxis           the axis style of the cube's z-axis (ticks, labels,
 *                        and title)
 */
public record CubeStyle(double panelPad, double sizeScale, double zCategoricalPad, double zTickLength,
        ElementRect panel, ElementRect panelForeground, Color grid, Color gridForeground, double gridLineWidth,
        double tickLabelPad, double axisTitlePad,
        double depthScaleStrength, double depthScaleMidpoint,
        double point3dStrokeWidth, double point3dHitTolerance, AxisStyle zAxis) {

    /**
     * The conventional cube style, used when a theme does not implement the
     * {@link Theme3d} element set.
     */
    public static final CubeStyle DEFAULTS = new CubeStyle(            20.0, 0.65, 0.03, 5.0,
            ElementRect.of(Color.rgb(240, 240, 240, 0.4), Color.rgb(200, 200, 200, 0.7), 1.0),
            ElementRect.of(Color.rgb(240, 240, 240, 0.4), Color.rgb(200, 200, 200, 0.7), 1.0)
                    .withAlpha(0.2),
            Color.rgb(200, 200, 200, 0.3), Color.rgb(200, 200, 200, 0.3), 0.5,
            4.0, 8.0, 0.3, 0.5, 1.0, 3.0, null);

    /**
     * Resolves the cube drawing constants from a theme's {@link Theme3d}
     * element set, falling back to {@link #DEFAULTS} for themes that do not
     * implement the optional elements.
     *
     * @param theme the theme to source the drawing constants from
     * @return the resolved cube style
     */
    public static CubeStyle from(Theme theme) {
        if (!(theme instanceof Theme3d t)) {
            return DEFAULTS;
        }
        var zAxis = AxisStyle.forZ(theme);
        return new CubeStyle(
                t.cubePanelPad(), t.cubeSizeScale(), t.zCategoricalPad(), t.zTickLength(),
                t.cubePanel(), t.cubePanelForeground(), t.cubeGrid(), t.cubeGridForeground(),
                t.cubeGridLineWidth(), t.cubeTickLabelPad(), t.cubeAxisTitlePad(),
                t.depthScaleStrength(), t.depthScaleMidpoint(),
                t.point3dStrokeWidth(), t.point3dHitTolerance(), zAxis);
    }

    /**
     * Scales a geometric quantity by its depth: nearer elements (larger
     * {@code depthScale}) grow, farther ones shrink, an element at depth
     * {@code 0} is suppressed entirely, and an element at depth {@code 1} is
     * left unchanged.
     *
     * @param depthScale the element's projected depth, usually from
     *                   {@code Projection.depthScale()}
     * @param strength   the strength of the effect, usually
     *                   {@link #depthScaleStrength()}
     * @return the multiplicative depth factor
     */
    public static double depthFactor(double depthScale, double strength) {
        return Math.pow(Math.max(0, depthScale), strength);
    }
}
