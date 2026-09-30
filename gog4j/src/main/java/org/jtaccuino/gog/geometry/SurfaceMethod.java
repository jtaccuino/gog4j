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
 * How a point collection is turned into surface tiles, matching the
 * {@code method} argument of the point-to-tile tessellation step
 * ({@code "auto"}, {@code "grid"} or {@code "delaunay"}).
 */
public enum SurfaceMethod {

    /** Picks regular-grid tessellation for complete grids, Delaunay otherwise. */
    AUTO,

    /** Tessellates the (row, column) lattice as rectangular or triangular tiles. */
    GRID,

    /** Triangulates the points with a Delaunay triangulation. */
    DELAUNAY
}
