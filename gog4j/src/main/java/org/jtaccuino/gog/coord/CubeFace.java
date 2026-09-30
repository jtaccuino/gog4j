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
 * One of the six faces of the cube drawn by a {@link Coord3D}, named after
 * the axis boundary the face lies on. The ordinal of each constant matches
 * the face index used by {@link Coord3D} (and by the cube's face table), so the
 * ordinal can stand in for the historical string-based index.
 */
public enum CubeFace {

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
    ZMAX
}
