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
 * Theme-derived drawing constants for the axes of a chart, so coordinate
 * systems stay decoupled from the plot's theme. Each visual element of an
 * axis has its own hook so it can be styled independently; by default the
 * values derive from the {@link Theme} fields (axis line, tick label, and
 * axis title) with no customisation needed.
 *
 * @param axisLineColor the color of the axis spines
 * @param axisLineWidth the stroke width of the axis spines
 * @param tickColor     the color of the tick marks
 * @param tickLabelColor the color of the tick value labels
 * @param tickLabelFont the font of the tick value labels
 * @param titleColor    the color of the axis titles
 * @param titleFont     the font of the axis titles
 */
public record AxisStyle(Color axisLineColor, double axisLineWidth, Color tickColor, Color tickLabelColor,
        Font tickLabelFont, Color titleColor, Font titleFont) {

    /**
     * Builds an {@code AxisStyle} from a theme's axis-related hooks.
     *
     * @param theme the theme to source the drawing constants from
     * @return the derived axis style
     */
    public static AxisStyle from(Theme theme) {
        return new AxisStyle(
                theme.axisLineColor(),
                theme.axisLineWidth(),
                theme.axisTickColor(),
                theme.tickLabelColor(),
                theme.tickLabelFont(),
                theme.axisTitleColor(),
                theme.axisTitleFont());
    }

    /**
     * Builds the {@code AxisStyle} of a 3D cube's z-axis, sourcing the tick,
     * label, and title styles from the theme's z-axis element chain (see
     * {@link Theme3d}), which inherit their 2D counterparts by default. The
     * axis line itself is shared with the 2D axes.
     *
     * @param theme the theme to source the drawing constants from
     * @return the derived z-axis style
     */
    public static AxisStyle forZ(Theme theme) {
        var t3 = theme instanceof Theme3d thr ? thr : null;
        var tickColor = t3 != null ? t3.axisTicksZColor() : theme.axisTickColor();
        var tickLabelColor = t3 != null ? t3.axisTextZColor() : theme.tickLabelColor();
        var tickLabelFont = t3 != null ? t3.axisTextZFont() : theme.tickLabelFont();
        var titleColor = t3 != null ? t3.axisTitleZColor() : theme.axisTitleColor();
        var titleFont = t3 != null ? t3.axisTitleZFont() : theme.axisTitleFont();
        return new AxisStyle(theme.axisLineColor(), theme.axisLineWidth(),
                tickColor, tickLabelColor, tickLabelFont, titleColor, titleFont);
    }
}
