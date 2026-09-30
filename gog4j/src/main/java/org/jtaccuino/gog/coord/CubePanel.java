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
 * One selectable panel of the cube drawn by a {@link Coord3D}, identified by
 * the position rules that decide which cube faces a layer paints: the
 * positional panels ({@code near}/{@code far}/{@code left}/{@code right}/
 * {@code top}/{@code bottom}/{@code front}/{@code back}), the {@code all}/
 * {@code none} switches, the foreground/background sets, or an explicit
 * {@link CubeFace}.
 */
public enum CubePanel {

    /** The faces on the side of the cube away from the viewer. */
    BACKGROUND,

    /** The faces on the side of the cube facing the viewer. */
    FOREGROUND,

    /** Every face of the cube. */
    ALL,

    /** No face of the cube. */
    NONE,

    /** The face whose silhouette corners project nearest to the viewer. */
    NEAR,

    /** The face whose silhouette corners project furthest from the viewer. */
    FAR,

    /** The face whose center projects nearest to the viewer. */
    FRONT,

    /** The face whose center projects furthest from the viewer. */
    BACK,

    /** The face whose center projects furthest to the left. */
    LEFT,

    /** The face whose center projects furthest to the right. */
    RIGHT,

    /** The face whose center projects furthest upward. */
    TOP,

    /** The face whose center projects furthest downward. */
    BOTTOM,

    /** The face on the minimum-{@code x} side of the cube. */
    XMIN,

    /** The face on the maximum-{@code x} side of the cube. */
    XMAX,

    /** The face on the minimum-{@code y} side of the cube. */
    YMIN,

    /** The face on the maximum-{@code y} side of the cube. */
    YMAX,

    /** The face on the minimum-{@code z} side of the cube. */
    ZMIN,

    /** The face on the maximum-{@code z} side of the cube. */
    ZMAX;

    /** {@return the cube face this panel constant names explicitly, or
     * {@code null} for the positional rules and set switches} */
    public CubeFace asFace() {
        return switch (this) {
            case XMIN -> CubeFace.XMIN;
            case XMAX -> CubeFace.XMAX;
            case YMIN -> CubeFace.YMIN;
            case YMAX -> CubeFace.YMAX;
            case ZMIN -> CubeFace.ZMIN;
            case ZMAX -> CubeFace.ZMAX;
            default -> null;
        };
    }
}
