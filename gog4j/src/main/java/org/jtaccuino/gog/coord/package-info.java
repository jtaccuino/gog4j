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

/**
 * Coordinate systems that map the plot's data domain onto the panel.
 *
 * <p>Provides the Cartesian systems (including flipped and fixed-aspect
 * variants), the polar and radial systems for pies, coxcombs, roses, and fans,
 * and the perspective 3-D cube projection. A coordinate system owns its panel
 * bounds, tick generation, label measurement, and clipping, keeping geometry
 * layers independent of layout.
 */
package org.jtaccuino.gog.coord;
