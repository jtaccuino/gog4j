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
package org.jtaccuino.gog.coord;

import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.scale.Scale;

/**
 * Coordinate system interface for {@link org.jtaccuino.gog.Plot}.
 * Implementations control axis orientation (flipped/normal) and
 * optional manual axis limits (zoom/pan).
 */
public interface Coord {

    /**
     * Returns whether this coordinate system swaps the X and Y axes
     * (e.g. coordFlip).
     *
     * @return {@code true} if axes are flipped
     */
    boolean isFlipped();

    /**
     * Maps a data point to its horizontal pixel position through this
     * coordinate system. The default treats the point as a plain Cartesian
     * position, routing the x aesthetic through the horizontal scale and the
     * y aesthetic through the vertical scale, swapping the two when the axes
     * are flipped. A polar implementation reinterprets the same pair as an
     * angle and a radius instead.
     *
     * @param sx    the scale of the horizontal axis
     * @param sy    the scale of the vertical axis
     * @param xData the position of the x aesthetic in data space
     * @param yData the position of the y aesthetic in data space
     * @return the horizontal pixel coordinate
     */
    default double xPixel(Scale sx, Scale sy, double xData, double yData) {
        return isFlipped() ? sx.toPixel(yData) : sx.toPixel(xData);
    }

    /**
     * Maps a data point to its vertical pixel position through this
     * coordinate system — the {@link #xPixel(Scale, Scale, double, double)}
     * counterpart.
     *
     * @param sx    the scale of the horizontal axis
     * @param sy    the scale of the vertical axis
     * @param xData the position of the x aesthetic in data space
     * @param yData the position of the y aesthetic in data space
     * @return the vertical pixel coordinate
     */
    default double yPixel(Scale sx, Scale sy, double xData, double yData) {
        return isFlipped() ? sy.toPixel(xData) : sy.toPixel(yData);
    }

    /**
     * Modifies or overrides the computed data bounds for the X-axis,
     * for example when the user sets hard zoom limits via xlim.
     *
     * @param computedBounds the auto-computed data range [min, max]
     * @return the adjusted range
     */
    MinMax adjustXBounds(MinMax computedBounds);

    /**
     * Modifies or overrides the computed data bounds for the Y-axis,
     * for example when the user sets hard zoom limits via ylim.
     *
     * @param computedBounds the auto-computed data range [min, max]
     * @return the adjusted range
     */
    MinMax adjustYBounds(MinMax computedBounds);
}
