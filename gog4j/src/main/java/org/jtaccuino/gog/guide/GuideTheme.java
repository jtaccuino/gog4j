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

/**
 * Per-guide style overrides applied over the theme-derived guide defaults.
 * Every component is nullable; {@code null} means "inherit from the theme".
 * Set via {@code Guide.theme(...)} on a concrete guide. Each visual element
 * of a guide may be themed independently.
 *
 * @param textColor  the general guide text color, or {@code null} to inherit
 * @param tickColor  the tick mark color, or {@code null} to inherit
 * @param titleColor the guide title color, or {@code null} to inherit
 * @param titleFont  the guide title font, or {@code null} to inherit
 * @param keyColor   the key label color, or {@code null} to inherit
 * @param keyFont    the key label font, or {@code null} to inherit
 * @param frameColor the guide frame color, or {@code null} to inherit
 */
public record GuideTheme(Color textColor, Color tickColor, Color titleColor, Font titleFont, Color keyColor,
        Font keyFont, Color frameColor) {

    /** An override with nothing set, inheriting the theme entirely. */
    public static final GuideTheme NONE = new GuideTheme(null, null, null, null, null, null, null);

    /**
     * {@return an override that styles only the title row}
     *
     * @param color the title color, or {@code null} to inherit
     * @param font  the title font, or {@code null} to inherit
     */
    public static GuideTheme title(Color color, Font font) {
        return new GuideTheme(null, null, color, font, null, null, null);
    }

    /**
     * {@return an override that styles only the key labels}
     *
     * @param color the key label color, or {@code null} to inherit
     * @param font  the key label font, or {@code null} to inherit
     */
    public static GuideTheme keys(Color color, Font font) {
        return new GuideTheme(null, null, null, null, color, font, null);
    }

    /** {@return a copy of this override with the given text color}
     *
     * @param color the text color, or {@code null} to inherit */
    public GuideTheme withTextColor(Color color) {
        return new GuideTheme(color, tickColor, titleColor, titleFont, keyColor, keyFont, frameColor);
    }

    /** {@return a copy of this override with the given tick color}
     *
     * @param color the tick mark color, or {@code null} to inherit */
    public GuideTheme withTickColor(Color color) {
        return new GuideTheme(textColor, color, titleColor, titleFont, keyColor, keyFont, frameColor);
    }

    /**
     * {@return a copy of this override with the given title styling}
     *
     * @param color the title color, or {@code null} to inherit
     * @param font  the title font, or {@code null} to inherit
     */
    public GuideTheme withTitle(Color color, Font font) {
        return new GuideTheme(textColor, tickColor, color, font, keyColor, keyFont, frameColor);
    }

    /**
     * {@return a copy of this override with the given key styling}
     *
     * @param color the key label color, or {@code null} to inherit
     * @param font  the key label font, or {@code null} to inherit
     */
    public GuideTheme withKeys(Color color, Font font) {
        return new GuideTheme(textColor, tickColor, titleColor, titleFont, color, font, frameColor);
    }

    /** {@return a copy of this override with the given frame color}
     *
     * @param color the frame color, or {@code null} to inherit */
    public GuideTheme withFrameColor(Color color) {
        return new GuideTheme(textColor, tickColor, titleColor, titleFont, keyColor, keyFont, color);
    }
}
