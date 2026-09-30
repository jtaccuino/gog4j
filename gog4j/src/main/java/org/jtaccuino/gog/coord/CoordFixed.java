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

/**
 * A fixed-aspect Cartesian coordinate system, the counterpart of the
 * {@code Coords.coordEqual()} (with {@code ratio} guarding the pixels-per-unit
 * relation between the two continuous axes). The panel window is
 * centre-cropped so equal data ranges occupy equal screen distances: a
 * one-unit step along either axis always measures the same number of pixels
 * (at {@code ratio = 1}).
 * <p>
 * Because cropping happens in pixel space after the axis transforms, the
 * equality holds for transformed axes too — under log or sqrt scales a data
 * decade spans the same pixels as a data unit on the other axis, which keeps
 * the geometry undistorted on screen.
 * <p>
 * Both axes must be continuous; requesting a fixed aspect on a discrete axis
 * is rejected at plot time with an {@link IllegalArgumentException}.
 */
public final class CoordFixed extends Coord2D {

    private final double ratio;

    /**
     * Creates a fixed-aspect Cartesian coordinate system.
     * @param ratio the desired pixels-per-x-unit divided by pixels-per-y-unit
     * @throws IllegalArgumentException when the ratio is not a positive finite number
     */
    public CoordFixed(double ratio) {
        super(false);
        if (ratio <= 0 || !Double.isFinite(ratio)) {
            throw new IllegalArgumentException(
                    "coordEqual requires a positive finite ratio, got " + ratio);
        }
        this.ratio = ratio;
    }

    /** {@return the desired pixels-per-x-unit divided by pixels-per-y-unit} */
    public double ratio() {
        return ratio;
    }

    /**
     * Centre-crops a pixel window so that each data axis keeps exactly
     * {@code ratio} pixels per unit in its transformed space.
     *
     * @param minX left edge of the window
     * @param maxX right edge of the window
     * @param minY top edge of the window (smaller y pixel)
     * @param maxY bottom edge of the window (larger y pixel)
     * @param spanX the x data span in transformed units
     * @param spanY the y data span in transformed units
     * @return {@code {minX, maxX, minY, maxY}} possibly narrowed along the
     *         dimension that has spare pixels, or the input unchanged when a
     *         span is non-positive (a degenerate axis leaves the panel intact)
     */
    public double[] equalizeWindow(double minX, double maxX, double minY, double maxY,
                                   double spanX, double spanY) {
        double fullW = maxX - minX;
        double fullH = maxY - minY;
        if (spanX <= 0 || spanY <= 0 || fullW <= 0 || fullH <= 0) {
            return new double[] {minX, maxX, minY, maxY};
        }

        double unitX = fullW / spanX;
        double unitY = fullH / spanY;

        // Case A: the x axis fills the full window, the y axis is cropped to
        // match the ratio. (unitX / unitY == ratio by construction.)
        double neededH = (unitX / ratio) * spanY;
        if (neededH <= fullH) {
            double padding = (fullH - neededH) / 2.0;
            return new double[] {minX, maxX, minY + padding, maxY - padding};
        }

        // Case B: the y axis fills the full window, the x axis is cropped.
        double neededW = (ratio * unitY) * spanX;
        double padding = (fullW - neededW) / 2.0;
        return new double[] {minX + padding, maxX - padding, minY, maxY};
    }
}
