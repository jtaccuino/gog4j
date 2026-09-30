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
package org.jtaccuino.gog.render;

import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Precise text measurement for layout code.
 * <p>
 * The axes and the plot's prediction passes measure text width with a scratch
 * {@link Text} node rather than guessing from character counts; the guides
 * historically estimated label widths as {@code length * 6}. This utility
 * surfaces the exact {@link Text#getLayoutBounds()} measurement so guide
 * layout can size itself to the text actually rendered, in the same font
 * {@link org.jtaccuino.gog.theme.Theme} supplies.
 * <p>
 * Instances are stateless: a single shared scratch node is reused under the
 * assumption that measurement happens on a single rendering thread, matching
 * how the coordinate systems already measure text.
 */
public final class TextMeasurer {

    private static final Text SCRATCH = new Text();

    private TextMeasurer() {
    }

    /**
     * The width of the given text laid out in the given font, in pixels.
     * <p>
     * Must be called on the FX Application Thread; the underlying
     * {@link Text} node is not thread-safe.
     *
     * @param text the text to measure
     * @param font the font the text is laid out in
     * @return the text's laid-out width in pixels
     * @throws IllegalStateException if called off the FX Application Thread
     */
    public static double width(String text, Font font) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("TextMeasurer.width() must be called on the FX Application Thread");
        }
        SCRATCH.setText(text);
        SCRATCH.setFont(font);
        return SCRATCH.getLayoutBounds().getWidth();
    }
}
