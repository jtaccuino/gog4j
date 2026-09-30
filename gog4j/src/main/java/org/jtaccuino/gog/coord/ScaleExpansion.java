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
 * Whether a polar coordinate system expands the continuous scales by the
 * {@link CoordPolar#EXPANSION} padding, the {@code expand} argument of
 * the {@code Coords.coordRadial()}.
 */
public enum ScaleExpansion {

    /** Follow the coordinate system's default: expand only inside a partial fan. */
    AUTO,

    /** Always expand the continuous scale limits. */
    ON,

    /** Take the limits straight from the scale, so a pie fills the disc. */
    OFF
}
