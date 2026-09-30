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

/**
 * An inclusive numeric range — the {@code [min, max]} extent of a continuous
 * axis or a data column. Carries the min/max pairs that the plotting pipeline
 * folds between data extraction, scale expansion, and coordinate limits.
 *
 * @param min the lower bound
 * @param max the upper bound
 */
public record MinMax(double min, double max) {

    /** The unit interval {@code [0, 1]}, used when no column provides a range. */
    public static final MinMax UNIT = new MinMax(0.0, 1.0);
}
