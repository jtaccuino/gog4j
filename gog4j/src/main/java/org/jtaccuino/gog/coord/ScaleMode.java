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
 * How {@link Coord3D} combines the per-axis aspect ratios with the observed
 * data ranges before projecting the cube.
 */
public enum ScaleMode {

    /** Uses the configured {@code ratio} per axis as-is, the default. */
    FREE,

    /**
     * Scales the per-axis ratios by each axis' data span so all three axes
     * share the same effective unit length.
     */
    FIXED
}
