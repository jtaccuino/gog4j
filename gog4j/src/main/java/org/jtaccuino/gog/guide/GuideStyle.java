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
package org.jtaccuino.gog.guide;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jtaccuino.gog.theme.Theme;

/**
 * Theme-derived drawing constants for {@link Guide} rendering, so guides stay
 * decoupled from the plot's theme. Each visual element has its own hook so it
 * can be styled independently; values that should look the same by default are
 * derived from a common source in {@link #from}. The plot pipeline wires theme
 * values in via {@link #from} when wiring guides in.
 *
 * @param textColor  the color of general guide text
 * @param tickColor  the color of tick marks and rule lines on bar guides
 * @param titleColor the color for the guide title
 * @param titleFont  the font for the guide title
 * @param keyColor   the color of key and tick labels
 * @param keyFont    the font for keys and tick labels
 * @param frameColor the color of the colorbar's frame border
 */
public record GuideStyle(Color textColor, Color tickColor, Color titleColor, Font titleFont, Color keyColor,
        Font keyFont, Color frameColor) {

    /**
     * Builds a style from the given components.
     *
     * @param textColor  the color for general guide text
     * @param tickColor  the color for tick marks on bar guides
     * @param titleColor the color for the guide title
     * @param titleFont  the font for the guide title
     * @param keyColor   the color for key and tick labels
     * @param keyFont    the font for keys and tick labels
     * @param frameColor the color of the colorbar's frame border
     * @return the derived style
     */
    public static GuideStyle from(Color textColor, Color tickColor, Color titleColor, Font titleFont, Color keyColor,
            Font keyFont, Color frameColor) {
        return new GuideStyle(textColor, tickColor, titleColor, titleFont, keyColor, keyFont, frameColor);
    }

    /**
     * Builds a style from a {@link Theme}. Distinct elements are sourced from
     * distinct theme hooks so any element may be re-themed independently; by
     * default the label-tier colors all derive from the theme's text color so
     * the historical look is preserved when nothing is customized.
     *
     * @param theme the theme to source the drawing constants from
     * @return the derived style
     */
    public static GuideStyle from(Theme theme) {
        return new GuideStyle(
                theme.textColor(),
                theme.guideTickColor(),
                theme.guideTitleColor(),
                theme.guideTitleFont(),
                theme.textColor(),
                theme.guideKeyFont(),
                theme.guideFrameColor());
    }

    /**
     * Builds a style from the theme and overlays a guide's per-guide style
     * overrides: every non-null component of the {@link GuideTheme} replaces
     * the theme-derived value.
     *
     * @param theme the theme to source the drawing constants from
     * @param guide the guide whose overrides apply, or {@code null} for pure
     *              theme styling
     * @return a style combining the theme defaults with the guide's overrides
     */
    public static GuideStyle from(Theme theme, Guide<?> guide) {
        var style = from(theme);
        var override = guide == null ? null : guide.guideTheme();
        if (override == null) {
            return style;
        }
        return new GuideStyle(
                override.textColor() != null ? override.textColor() : style.textColor(),
                override.tickColor() != null ? override.tickColor() : style.tickColor(),
                override.titleColor() != null ? override.titleColor() : style.titleColor(),
                override.titleFont() != null ? override.titleFont() : style.titleFont(),
                override.keyColor() != null ? override.keyColor() : style.keyColor(),
                override.keyFont() != null ? override.keyFont() : style.keyFont(),
                override.frameColor() != null ? override.frameColor() : style.frameColor());
    }

    /**
     * Returns a copy of this style with its fonts scaled by the given factor.
     * Used to offset a drawing-surface scale transform when labels must keep
     * their visual size while surrounding geometry shrinks.
     *
     * @param f the font scale factor
     * @return the styled copy with scaled fonts
     */
    public GuideStyle withFontScale(double f) {
        return new GuideStyle(textColor, tickColor, titleColor,
                scaled(titleFont, f), keyColor, scaled(keyFont, f), frameColor);
    }

    private static Font scaled(Font font, double f) {
        return f == 1.0 ? font : Font.font(font.getFamily(), font.getSize() * f);
    }
}
