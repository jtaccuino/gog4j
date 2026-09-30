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
 * A colour scale for the {@code color}/{@code fill} aesthetics. The concrete
 * implementation is the single source of truth that both picks the default
 * guide and supplies its data: a {@link ContinuousColorScale} renders a
 * colourbar with ticks at its breaks, a {@link DiscreteColorScale} a legend
 * with one key per category.
 */
public interface ColorScale {

    /**
     * Whether the scale maps continuous numeric/date values to colours
     * (colourbar guide) or categories to colours (legend guide).
     *
     * @return {@code true} for a continuous scale, {@code false} for a discrete one
     */
    boolean isContinuous();
}
