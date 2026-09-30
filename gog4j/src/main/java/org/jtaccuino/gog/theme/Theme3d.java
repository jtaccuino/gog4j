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
import javafx.scene.text.Font;

/**
 * The 3D cube theme elements: the cube-specific extension of the standard
 * themed element chain. Every visual constant of the 3D coordinate
 * system — the six cube face panels, their grid lines, the z-axis ticks and
 * labels, the cube geometry, and the per-point depth scaling — is a themed
 * hook, so a plot can restyle its cube exactly like it restyles its panels
 * and axes.
 * <p>
 * The elements inherit like conventional theme elements, so overriding one root
 * element restyles every derived child:
 * <ul>
 *   <li>{@code panel.foreground} ← {@code panel.background}: the front three
 *       cube faces inherit the back three, then drop to
 *       {@link #cubeForegroundAlpha()}</li>
 *   <li>{@code panel.border.foreground} ← {@code panel.border}: the front
 *       faces' outline inherits the panel outline</li>
 *   <li>{@code grid.foreground} ← {@code grid}: the front faces'
 *       grid lines inherit the panel grid</li>
 *   <li>z-axis tick ← {@code showMajorTicks()}, z-axis text ←
 *       {@code axis.text}, z-axis title ← {@code axis.title}: the
 *       z-axis elements inherit their 2D counterparts, and are never
 *       depth-scaled</li>
 * </ul>
 *
 * @see CubeStyle the resolved drawing bundle built from this element set
 */
public interface Theme3d extends Theme {

    // --- cube face panels (ElementRect with alpha) ---

    /**
     * The {@code panel.background} root element of the cube: the fill and
     * outline of the back three face panels.
     *
     * @return the panel rect element
     */
    default ElementRect cubePanel() {
        var border = panelBorderColor();
        var colour = border != null ? border : Color.rgb(180, 180, 180, 0.8);
        return ElementRect.of(plotBackground(), colour, 1.0);
    }

    /**
     * The opacity of the front three cube face panels, {@code panel.foreground}.
     *
     * @return the foreground panel opacity in [0, 1]
     */
    default double cubeForegroundAlpha() {
        return 0.2;
    }

    /**
     * The {@code panel.foreground} element: the front three face panels,
     * inheriting {@link #cubePanel()} at {@link #cubeForegroundAlpha()}.
     *
     * @return the foreground panel rect element
     */
    default ElementRect cubePanelForeground() {
        return cubePanel().withAlpha(cubeForegroundAlpha());
    }

    // --- cube grid lines ---

    /**
     * The {@code grid} root element of the cube: the grid lines drawn on
     * each face panel. It inherits the standard by default
     * {@code grid} (white for {@code theme_gray}), which is nearly
     * invisible against the light panels; the cube keeps a slightly darker
     * themed default so the grid reads, while still being overridable per
     * theme through its own {@code grid} hook.
     *
     * @return the grid line colour
     */
    default Color cubeGrid() {
        return gridLineColor();
    }

    /**
     * The {@code grid.foreground} element of the cube: the grid lines of
     * the front three face panels, inheriting {@link #cubeGrid()}.
     *
     * @return the foreground grid line colour
     */
    default Color cubeGridForeground() {
        return cubeGrid();
    }

    /**
     * The stroke width of the cube grid lines, in pixels.
     *
     * @return the grid line width
     */
    default double cubeGridLineWidth() {
        return 0.5;
    }

    // --- cube geometry ---

    /**
     * The horizontal/vertical inset of the cube within its panel, in pixels.
     *
     * @return the cube panel pad
     */
    default double cubePanelPad() {
        return 20.0;
    }

    /**
     * The fraction of the inner panel that the cube's projection scale uses.
     *
     * @return the cube size scale factor
     */
    default double cubeSizeScale() {
        return 0.65;
    }

    /**
     * The fractional [0,1] inset applied along a categorical z-axis so the
     * first and last categories never sit exactly on the cube faces.
     *
     * @return the categorical z-axis pad
     */
    default double zCategoricalPad() {
        return 0.03;
    }

    /**
     * The length of the tick marks on the z-axis, in data units along the
     * z-axis direction. The 2D axes measure ticks in pixels directly; the 3D
     * cube measures them in projection units because the z-axis direction is
     * foreshortened.
     *
     * @return the z-axis tick length
     */
    default double zTickLength() {
        return 5.0;
    }

    /**
     * The extra pixel clearance between a z-axis tick label and its tick mark,
     * on top of half the label's own width.
     *
     * @return the z-axis tick label pad
     */
    default double cubeTickLabelPad() {
        return 4.0;
    }

    /**
     * The extra pixel clearance between a z-axis title and the tick labels; the
     * title's own half-extent is added on top when placing it.
     *
     * @return the z-axis title pad
     */
    default double cubeAxisTitlePad() {
        return 8.0;
    }

    /**
     * The strength of the depth scaling applied to 3D geometry: the scale of
     * an element at depth {@code d} is {@code 1 + strength * (d - midpoint)}.
     *
     * @return the depth scale strength
     */
    default double depthScaleStrength() {
        return 0.3;
    }

    /**
     * The depth at which depth scaling leaves an element unscaled — the
     * midpoint of the depth range that {@link #depthScaleStrength()} scales
     * elements away from.
     *
     * @return the depth scale midpoint
     */
    default double depthScaleMidpoint() {
        return 0.5;
    }

    // --- 3D point layer defaults ---

    /**
     * The stroke width of the outline drawn around depth-scaled 3D points, in
     * pixels.
     *
     * @return the 3D point stroke width
     */
    default double point3dStrokeWidth() {
        return 1.0;
    }

    /**
     * The extra pixel tolerance beyond a 3D point's radius when hit-testing
     * the point during interactive locating.
     *
     * @return the 3D point hit-testing tolerance
     */
    default double point3dHitTolerance() {
        return 3.0;
    }

    // --- z-axis element chain ---

    /**
     * The z-axis tick colour, inheriting the 2D
     * {@link #axisTickColor()}.
     *
     * @return the z-axis tick colour
     */
    default Color axisTicksZColor() {
        return axisTickColor();
    }

    /**
     * The z-axis text colour, inheriting the 2D
     * {@link #tickLabelColor()}.
     *
     * @return the z-axis tick label colour
     */
    default Color axisTextZColor() {
        return tickLabelColor();
    }

    /**
     * The z-axis text font, inheriting the 2D
     * {@link #tickLabelFont()}.
     *
     * @return the z-axis tick label font
     */
    default Font axisTextZFont() {
        return tickLabelFont();
    }

    /**
     * The z-axis title colour, inheriting the 2D
     * {@link #axisTitleColor()}.
     *
     * @return the z-axis title colour
     */
    default Color axisTitleZColor() {
        return axisTitleColor();
    }

    /**
     * The z-axis title font, inheriting the 2D
     * {@link #axisTitleFont()}.
     *
     * @return the z-axis title font
     */
    default Font axisTitleZFont() {
        return axisTitleFont();
    }
}
