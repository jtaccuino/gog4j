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

import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.scale.ResolvedScales;

/**
 * The prepared state of a colour-mapped {@link GeomSmooth} layer, computed once
 * during {@link Layer#prepare} over the global DataFrame: the per-group model
 * fits, the shared resolved colour scales, and the global X range used for
 * {@code fullrange} extrapolation.
 *
 * @param grouped      the per-group model fits in scale order
 * @param scales       the shared resolved scales (continuous-aware colour)
 * @param groupCol     the column the layer's colour/group is mapped to
 * @param globalXRange the global {@code [min, max]} of the X aesthetic
 */
public record SmoothData(GroupedSmooths grouped, ResolvedScales<?> scales, String groupCol,
                         MinMax globalXRange) implements LayerData {
}
