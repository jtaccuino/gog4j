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
package org.jtaccuino.gog.scale;

/**
 * Where a position scale draws its axis: the bottom or top edge for an x
 * axis, the left or right edge for a y axis. Mirrors the default's
 * {@code Scales.scaleXContinuous(position = ...)} argument.
 */
public enum AxisPosition {
    /** The axis rides along the bottom edge (default for an x axis). */
    BOTTOM,
    /** The axis rides along the top edge. */
    TOP,
    /** The axis rides along the left edge (default for a y axis). */
    LEFT,
    /** The axis rides along the right edge. */
    RIGHT;
}
