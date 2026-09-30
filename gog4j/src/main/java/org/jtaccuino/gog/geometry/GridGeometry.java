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
package org.jtaccuino.gog.geometry;

/**
 * The geometry of a regular lattice that {@link MeshUtil} tessellates into
 * surface tiles, matching the {@code grid} argument of the point-grid
 * generation and point-to-tile tessellation steps ({@code "rectangle"},
 * {@code "right1"}, {@code "right2"} or {@code "equilateral"}).
 */
public enum GridGeometry {

    /** Divides each cell of the lattice into a single rectangle. */
    RECTANGLE,

    /** Divides each cell of the lattice into two right triangles joining the
     * top-left to the bottom-right corner of the quad. */
    RIGHT1,

    /** Divides each cell of the lattice into two right triangles joining the
     * bottom-left to the top-right corner of the quad. */
    RIGHT2,

    /** Builds an equilateral-triangle lattice (trimmed to the data bounds). */
    EQUILATERAL
}
