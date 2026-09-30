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

/**
 * How a guide strip that is taller than the panel it sits beside is handled.
 * A {@code RIGHT}/{@code LEFT} strip's natural, unconstrained height can exceed
 * the space between its top edge and the canvas bottom; these modes decide what
 * happens then.
 * <p>
 * There is no reference counterpart: the default renders legends at their natural
 * size and simply lets a fixed-size device clip them (mirrored by {@link #NONE}),
 * or grows the figure. Since this framework exports plots to a fixed canvas, an
 * explicit overflow policy is required.
 */
public enum GuideOverflow {

    /**
     * Renders the strip at its natural size, letting it extend past the canvas
     * edge where it is clipped by the viewer — the closest analogue of
     * the default behaviour on a fixed-size device.
     */
    NONE,

    /**
     * Shrinks the strip's geometry to fit the available height: bar lengths,
     * key boxes, and spacing scale down, while label fonts keep their full
     * size. Text may crowd on small fit factors.
     */
    SCALE_GEOMETRY,

    /**
     * Shrinks the strip's geometry and its label fonts together, proportionally,
     * so the composition stays readable at any fit factor.
     */
    SCALE_UNIFORM,

    /**
     * Keeps the guides that fit within the available height on the side strip
     * and relocates the remainder to a horizontal bottom strip.
     */
    HYBRID,

    /** Relocates the whole overflowing side group to a horizontal bottom strip. */
    FLOW_TO_BOTTOM
}
