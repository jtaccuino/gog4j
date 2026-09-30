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

/**
 * A line dash pattern for the {@code linetype} aesthetic, following the
 * {@code Scales.scaleLinetypeManual(values = ...)} entries.
 * <p>
 * A pattern is the alternating on/off lengths consumed by
 * {@link org.jtaccuino.gog.render.DrawSurface#setLineDashes(double...)};
 * an empty array denotes a solid line. Use the {@link #of(double...)} factory
 * for arbitrary patterns, {@link #named(String)} for a conventional line-type name,
 * or the predefined {@link LineTypes} enum constants
 * ({@code SOLID}, {@code DASHED}, {@code DOTTED}, {@code DOTDASH},
 * {@code LONGDASH}, {@code TWODASH}).
 *
 * @see LineTypes
 * @see LineDash
 */
public sealed interface LineType permits LineType.Pattern, LineTypes {

    /**
     * Returns the alternating on/off dash lengths; an empty array denotes a
     * solid line. The array is never {@code null}.
     *
     * @return the dash lengths
     */
    double[] dashes();

    /**
     * Creates a custom dash pattern from alternating on/off lengths.
     *
     * @param dashes the on/off lengths; an empty varargs denotes a solid line
     * @return the new pattern
     */
    static LineType of(double... dashes) {
        return new Pattern(dashes);
    }

    /**
     * Creates the dash pattern for a conventional line-type name (case-insensitive),
     * e.g. {@code "dashed"}, {@code "dotted"}, {@code "dotdash"},
     * {@code "longdash"}, {@code "twodash"}. Unknown names resolve to solid.
     *
     * @param name the line-type name
     * @return the matching pattern
     */
    static LineType named(String name) {
        var dashes = LineDash.of(name);
        return new Pattern(dashes == null ? new double[0] : dashes);
    }

    /**
     * An immutable custom dash pattern.
     */
    final class Pattern implements LineType {

        private final double[] dashes;

        /**
         * Normalises to a defensive copy; {@code null} becomes solid (empty).
         *
         * @param dashes the alternating on/off lengths
         */
        Pattern(double[] dashes) {
            this.dashes = dashes == null ? new double[0] : dashes.clone();
        }

        @Override
        public double[] dashes() {
            return dashes.clone();
        }
    }
}
