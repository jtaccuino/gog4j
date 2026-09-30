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
package org.jtaccuino.gog.layer;

/**
 * Shared formatting of 3D hover tooltips, so every 3D geometry reports
 * data-space values in the same {@code X/Y/Z} layout (optionally prefixed with
 * the hovered element's group).
 */
final class Tooltip3d {

    private Tooltip3d() {
    }

    /**
     * {@return a tooltip reporting a single {@code (x, y, z)} position}
     *
     * @param x the already-formatted x value
     * @param y the already-formatted y value
     * @param z the already-formatted z value
     */
    static String position(Object x, Object y, Object z) {
        return "X: " + x + "\nY: " + y + "\nZ: " + z;
    }

    /**
     * {@return a grouped tooltip reporting a single {@code (x, y, z)} position}
     *
     * @param group the group label, printed verbatim (including {@code null})
     * @param x the already-formatted x value
     * @param y the already-formatted y value
     * @param z the already-formatted z value
     */
    static String position(Object group, Object x, Object y, Object z) {
        return "Group: " + group + "\n" + position(x, y, z);
    }

    /**
     * {@return a tooltip reporting both endpoints of a 3D segment}
     *
     * @param x1 the start x value
     * @param y1 the start y value
     * @param z1 the start z value
     * @param x2 the end x value
     * @param y2 the end y value
     * @param z2 the end z value
     */
    static String segment(Object x1, Object y1, Object z1, Object x2, Object y2, Object z2) {
        return "X: " + x1 + "\nY: " + y1 + "\nZ: " + z1
                + "\nXend: " + x2 + "\nYend: " + y2 + "\nZend: " + z2;
    }
}
