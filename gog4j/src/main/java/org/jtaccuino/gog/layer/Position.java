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
package org.jtaccuino.gog.layer;

/**
 * Position adjustment modes for geometries in canvas graphic space.
 * <p>
 * Every constant is itself a {@link PositionAdjust}: the shared default
 * adjustment of its family. Pass one straight to a {@code position(...)}
 * entry point for the default behaviour, or chain a parameterised variant
 * from the constant — {@code Position.DODGE.dodge(0.7)},
 * {@code Position.STACK.stack(true)} — when an explicit width or a reversed
 * stack is wanted.
 */
public enum Position implements PositionAdjust {

    /** Data flows independently without positioning offsets (default for point/line). */
    IDENTITY,
    /** Data elements dodge each other horizontally/vertically (default for bar clusters). */
    DODGE,
    /** Data elements stack on top of each other cumulatively (default for stacked area/bar). */
    STACK,
    /** Data elements stack and are normalized to fill the axis from zero to one (Positions.fill). */
    FILL;

    @Override
    public Position mode() {
        return this;
    }

    /**
     * {@return the {@link PositionAdjust} equivalent to this bare mode, with default parameters}
     * <p>
     * Every constant is its own default adjustment, so this returns the
     * constant itself. Geometry code that wants to read a numeric parameter
     * such as a dodge width should consult the {@link PositionAdjust} instead
     * of the enum.
     */
    public PositionAdjust adjust() {
        return this;
    }

    /**
     * {@return a default {@link PositionAdjust.Identity} (this constant), the
     * bare {@code Positions.identity()}}
     */
    public PositionAdjust identity() {
        return this;
    }

    /**
     * {@return a default-width {@link PositionAdjust.Dodge} (this constant),
     * following the {@code Positions.dodge()}}
     */
    public PositionAdjust dodge() {
        return this;
    }

    /**
     * {@return a {@link PositionAdjust.Dodge} separating categories by the given width fraction}
     *
     * @param width the dodge width as a fraction of the group spacing
     */
    public PositionAdjust dodge(double width) {
        return new PositionAdjust.Dodge(width);
    }

    /**
     * {@return a forward-ordered {@link PositionAdjust.Stack} (this constant),
     * following the {@code Positions.stack()}}
     */
    public PositionAdjust stack() {
        return this;
    }

    /**
     * {@return a {@link PositionAdjust.Stack} with the given ordering}
     *
     * @param reverse whether to reverse the stacking order
     */
    public PositionAdjust stack(boolean reverse) {
        return new PositionAdjust.Stack(reverse);
    }

    /**
     * {@return a default {@link PositionAdjust.Fill} (this constant), matching
     * the {@code Positions.fill()}}
     */
    public PositionAdjust fill() {
        return this;
    }
}
