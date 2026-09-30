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
package org.jtaccuino.gog.scale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps the conventional line-type names ({@code solid}, {@code dashed},
 * {@code dotted}, {@code dotdash}, {@code longdash}, {@code twodash},
 * {@code blank}) to the alternating on/off dash arrays consumed by
 * {@link org.jtaccuino.gog.render.DrawSurface#setLineDashes(double...)}.
 * <p>
 * A {@code null} (or empty) array denotes a solid line. The patterns follow
 * the conventions already used by the reference-line geometry helpers
 * (e.g. {@code dashed} = {@code {6.0, 4.0}}).
 *
 * <p>This is a stateless utility; {@link #cycle(int)} yields the repeated
 * pattern list used by a {@link DiscreteLinetypeScale}.
 */
public final class LineDash {

    private static final double[] DASHED = {6.0, 4.0};
    private static final double[] DOTTED = {1.0, 3.0};
    private static final double[] DOTDASH = {1.0, 3.0, 6.0, 3.0};
    private static final double[] LONGDASH = {6.0, 3.0};
    private static final double[] TWODASH = {2.0, 2.0};
    private static final double[] SOLID = null;

    private LineDash() {
        // utility class — no public constructor
    }

    /**
     * Returns the dash pattern for a conventional line-type name.
     *
     * @param name the line-type name (case-insensitive), e.g. {@code dashed}
     * @return the alternating on/off dash array, or {@code solid}/{@code null}
     *         for an unknown or {@code solid}/{@code blank} name
     */
    public static double[] of(String name) {
        if (name == null) {
            return SOLID;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "solid", "blank" -> SOLID;
            case "dashed" -> DASHED;
            case "dotted" -> DOTTED;
            case "dotdash" -> DOTDASH;
            case "longdash" -> LONGDASH;
            case "twodash" -> TWODASH;
            default -> SOLID;
        };
    }

    /**
     * Returns {@code count} distinct dash patterns, cycling through the
     * non-solid line types so each index maps to a visually distinct line.
     *
     * @param count the number of patterns to produce
     * @return a list of {@code count} dash arrays
     */
    public static List<double[]> cycle(int count) {
        var patterns = List.of(DASHED, DOTTED, DOTDASH, LONGDASH, TWODASH);
        var result = new ArrayList<double[]>(count);
        for (int i = 0; i < count; i++) {
            result.add(patterns.get(i % patterns.size()));
        }
        return result;
    }
}
