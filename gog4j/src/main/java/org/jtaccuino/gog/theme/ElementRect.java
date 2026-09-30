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
 * A themed rectangular element, following the {@code ElementRect(fill,
 * colour, linewidth, alpha)}. Used wherever the theme draws a filled rectangle
 * with an optional outline — most notably the panels of the 3D cube, where
 * each cube face is a themed rect.
 * <p>
 * The {@code alpha} attribute is a multiplicative opacity applied on top of the
 * opacity already carried by the {@code fill} and {@code colour}, matching
 * {@code ElementRect}'s semantics: the same element can be reused at reduced
 * opacity (e.g. foreground cube panels at 20% opacity) without restating its
 * colours.
 *
 * @param fill     the fill colour, or {@code null} for no fill
 * @param colour   the outline colour, or {@code null} for no outline
 * @param linewidth the outline stroke width in pixels
 * @param alpha    the multiplicative opacity in [0, 1], applied to both fill
 *                 and outline
 */
public record ElementRect(Color fill, Color colour, double linewidth, double alpha) {

    /**
     * Creates an opaque element: the colours carry their own opacity and the
     * element {@code alpha} defaults to {@code 1.0}.
     *
     * @param fill   the fill colour, or {@code null} for no fill
     * @param colour the outline colour, or {@code null} for no outline
     * @param linewidth the outline stroke width in pixels
     * @return the fully-opaque element
     */
    public static ElementRect of(Color fill, Color colour, double linewidth) {
        return new ElementRect(fill, colour, linewidth, 1.0);
    }

    /**
     * {@return this element with the multiplicative opacity set to {@code alpha}}
     * <p>
     * Colours returned by {@link #fill()} and {@link #colour()} multiply their
     * own opacity by this factor.
     *
     * @param alpha new multiplicative opacity in [0, 1]
     */
    public ElementRect withAlpha(double alpha) {
        return new ElementRect(fill, colour, linewidth, alpha);
    }

    /**
     * {@return the effective fill colour, with {@link #alpha()} composed into
     * the fill's own opacity, or {@code null} if this element has no fill}
     */
    @Override
    public Color fill() {
        return fill == null ? null : Colors.withAlpha(fill, alpha);
    }

    /**
     * {@return the effective outline colour, with {@link #alpha()} composed
     * into the colour's own opacity, or {@code null} if this element has no
     * outline}
     */
    @Override
    public Color colour() {
        return colour == null ? null : Colors.withAlpha(colour, alpha);
    }
}
