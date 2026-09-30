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
package org.jtaccuino.gog;

import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CoordFixed;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.coord.ScaleExpansion;
import org.jtaccuino.gog.scale.ScaleTransform;

/**
 * Factory for creating coordinate system specifications.
 */
public class Coords {

    /** Utility class; not meant to be instantiated. */
    private Coords() {}

    /**
     * Creates a standard Cartesian coordinate system.
     *
     * @return a new {@link Coord2D} with no axis flipping
     */
    public static Coord2D coordCartesian() {
        return new Coord2D(false);
    }

    /**
     * Creates a flipped coordinate system, swapping the x and y axes.
     *
     * @return a new {@link Coord2D} with flipped axes
     */
    public static Coord2D coordFlip() {
        return new Coord2D(true);
    }

    /**
     * Creates a polar coordinate system, the {@code Coords.coordPolar()}: the
     * x aesthetic maps to the angle and y to the radius, turning bars into
     * wedges (a coxcomb). Chain {@code .theta("y")} to swap the roles for a pie.
     *
     * @return a new {@link CoordPolar}
     */
    public static CoordPolar coordPolar() {
        return new CoordPolar();
    }

    /**
     * Creates a radial coordinate system, the {@code Coords.coordRadial()}: a
     * polar layout that can be narrowed to a fan with {@code .start(…)} and
     * {@code .end(…)} radians and given a hollow centre with
     * {@code .innerRadius(…)}. Unlike {@link #coordPolar()}, it expands the
     * continuous scales (the {@code expand = TRUE} default), so pie
     * charts call {@code .expand(ScaleExpansion.OFF)} to fill the disc.
     *
     * @return a new {@link CoordPolar} configured for radial coordinates
     */
    public static CoordPolar coordRadial() {
        return coordPolar().expand(ScaleExpansion.ON);
    }

    /**
     * Creates a fixed-aspect Cartesian coordinate system, the
     * {@code Coords.coordEqual()}: the panel window is centre-cropped so one data
     * unit spans the same number of pixels on both continuous axes.
     * <p>
     * Equality is measured in transformed (pixel) space, so under log or sqrt
     * axis scales the geometry stays undistorted on screen instead of the raw
     * data ranges matching. Requesting it on a discrete axis fails at plot
     * time with an {@link IllegalArgumentException}.
     *
     * @return a new {@link CoordFixed} with equal pixels per unit on both axes
     */
    public static CoordFixed coordEqual() {
        return coordEqual(1.0);
    }

    /**
     * Creates a fixed-aspect Cartesian coordinate system with a custom
     * pixels-per-unit relation, the {@code ratio} argument of the
     * {@code Coords.coordEqual()}.
     *
     * @param ratio the desired pixels-per-x-unit divided by pixels-per-y-unit
     * @return a new {@link CoordFixed} with the given aspect ratio
     * @throws IllegalArgumentException when the ratio is not a positive finite number
     */
    public static CoordFixed coordEqual(double ratio) {
        return new CoordFixed(ratio);
    }

    /**
     * Creates a Cartesian coordinate system with per-axis scale transforms,
     * the {@code Coords.coordTrans()}: every data value runs through the given
     * transform before mapping to pixels, so {1, 10, 100, 1000} on a
     * {@link ScaleTransform#LOG10} axis sit at equidistant pixels.
     * <p>
     * The transforms override the scale-spec transforms (one source of truth),
     * replacing {@code scaleYLog10} on the overridden axis. Transformed axes
     * must be continuous; passing a transform for a discrete axis fails at
     * plot time with an {@link IllegalArgumentException}.
     *
     * @param xTransform the transform for the x data aesthetic, or {@code null} to keep the spec's
     * @param yTransform the transform for the y data aesthetic, or {@code null} to keep the spec's
     * @return a new {@link Coord2D} with the given axis transforms
     */
    public static Coord2D coordTrans(ScaleTransform xTransform, ScaleTransform yTransform) {
        return new Coord2D(false).trans(xTransform, yTransform);
    }

    /**
     * Creates a 3D coordinate system: a perspective-axonometric projection of
     * the {@code x}/{@code y}/{@code z} aesthetics onto the panel, with the
     * default view (pitch 0, roll −60, yaw −30 degrees, camera distance 2).
     * Chain the fluent view controls ({@code pitch}/{@code roll}/{@code yaw}/
     * {@code dist}/{@code persp}/{@code zoom}), the axis behaviour
     * ({@code scales}/{@code ratio}/{@code expand}/{@code clip}), and the
     * cube surfaces ({@code panels}) to shape the rendering.
     *
     * @return a new {@link Coord3D} with the default 3D view
     */
    public static Coord3D coord3d() {
        return new Coord3D();
    }
}
