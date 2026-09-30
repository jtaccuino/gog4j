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
 * A Cartesian coordinate system that extends {@link Coord2D} with fluent
 * factory methods for backward compatibility.
 */
public class CoordCartesian extends Coord2D {

    /** Creates a non-flipped Cartesian coordinate system. */
    public CoordCartesian() { super(); }

    /** Creates a Cartesian coordinate system with the axes optionally swapped.
     * @param flipped whether the x and y axes are swapped */
    public CoordCartesian(boolean flipped) { super(flipped); }

    /** {@return a new non-flipped Cartesian coordinate system} */
    public static CoordCartesian cartesian() { return new CoordCartesian(false); }

    /** {@return a new flipped Cartesian coordinate system} */
    public static CoordCartesian flip()      { return new CoordCartesian(true); }

    /**
     * Sets fixed x-axis limits.
     * @return this instance for chaining
     */
    @Override
    public CoordCartesian xlim(double min, double max) { super.xlim(min, max); return this; }

    /**
     * Sets fixed y-axis limits.
     * @return this instance for chaining
     */
    @Override
    public CoordCartesian ylim(double min, double max) { super.ylim(min, max); return this; }
}
