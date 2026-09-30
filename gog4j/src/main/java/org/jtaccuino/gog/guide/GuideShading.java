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

import org.jtaccuino.gog.MinMax;

/**
 * The 3-D shading applied to a guide's colours, giving the colorbar or legend
 * the same lit-from-one-side cue the cube faces carry: the shading applied by
 * {@code Guides.guideColorbar3d()} and {@code Guides.guideLegend3d()}, which
 * both default to a non-reversed {@code [-1, 1]} shade range.
 * <p>
 * The bar's gradient is modulated across {@link #range()}: each stop's ramp
 * position {@code t} maps to a shade factor
 * {@code min + t * (max - min)} blended into the colour — brightening it toward
 * white above 0.5, darkening toward black below, using the same shade-strength
 * blend the polygon lighting uses. Legend keys are shaded with the range's
 * upper (or, when {@link #reverse()}, lower) factor so every swatch reads as a
 * uniformly lit tile.
 *
 * @param range   the shade factors spanned across the bar (default
 *                {@code [-1, 1]}, i.e. the full dark-to-bright range)
 * @param reverse {@code true} to flip the shading direction so the bar's low
 *                end reads bright
 * @param hsl     {@code true} to blend in HSL lightness instead of the default
 *                HSV brightness channel
 */
public record GuideShading(MinMax range, boolean reverse, boolean hsl) {

    /** The default shading: full {@code [-1, 1]} ramp in HSV, not reversed. */
    public static final GuideShading DEFAULT = new GuideShading(new MinMax(-1.0, 1.0), false, false);

    /**
     * Compact constructor normalising a {@code null} range to the default.
     *
     * @param range   the shade factors, or {@code null} for {@code [-1, 1]}
     * @param reverse whether to flip the shading direction
     * @param hsl     whether to blend in HSL lightness instead of HSV brightness
     */
    public GuideShading {
        range = range == null ? new MinMax(-1.0, 1.0) : range;
    }

    /**
     * Creates shading with the given range in HSV, not reversed.
     *
     * @param range the shade factors spanned across the bar
     * @return a shading spec for the range
     */
    public static GuideShading of(MinMax range) {
        return new GuideShading(range, false, false);
    }

    /**
     * Creates shading with the given range and direction in HSV.
     *
     * @param range   the shade factors spanned across the bar
     * @param reverse {@code true} to flip the shading direction
     * @return a shading spec for the range and direction
     */
    public static GuideShading of(MinMax range, boolean reverse) {
        return new GuideShading(range, reverse, false);
    }

    /**
     * {@return the shade factor at the given position along the bar,
     * honouring the range and its direction}
     *
     * @param t the bar position in {@code [0, 1]} (low to high value)
     */
    public double shadeAt(double t) {
        if (reverse) {
            t = 1.0 - t;
        }
        return range.min() + t * (range.max() - range.min());
    }

    /**
     * {@return the fixed shade factor applied to every legend key}
     */
    public double keyShade() {
        return reverse ? range.min() : range.max();
    }
}
