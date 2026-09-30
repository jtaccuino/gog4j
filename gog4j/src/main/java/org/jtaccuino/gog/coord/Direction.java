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
 * The sweep direction of increasing theta in a polar coordinate system:
 * whether angles grow clockwise or counterclockwise on screen.
 */
public enum Direction {

    /** Angles grow clockwise on screen, the {@code Coords.coordPolar} default. */
    CLOCKWISE(1),

    /** Angles grow counterclockwise on screen, the {@code direction = -1} reversal. */
    COUNTERCLOCKWISE(-1);

    private final int sign;

    Direction(int sign) {
        this.sign = sign;
    }

    /** {@return the multiplier ({@code 1} or {@code -1}) that flips angular positions} */
    public int sign() {
        return sign;
    }
}
