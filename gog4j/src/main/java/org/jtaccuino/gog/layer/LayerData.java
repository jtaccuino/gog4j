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

import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.StatData;

/**
 * The prepared state a {@link Layer} computes once per render pass in its
 * {@link Layer#prepare} phase and carries into its {@link Layer#render} and
 * {@link Layer#locate} phases.
 *
 * <p>Layers that map no expensive global computation return the singletons
 * {@link #NONE}; a layer that fits models or builds per-column palettes over
 * the global data returns a dedicated immutable type holding the results.
 */
public interface LayerData {

    /**
     * The singleton prepared state for layers that need no precomputed data.
     * Rendering reads palettes eagerly from the shared
     * {@link org.jtaccuino.gog.scale.ResolvedScales} on
     * {@link PlotContext}.
     */
    LayerData NONE = new LayerData() {
    };

    /**
     * The prepared state of a stat-first layer: the {@link StatData} a
     * {@link org.jtaccuino.gog.stat.Stat} computed over the global data during
     * {@code prepare()}, together with the extractor used to read it. Rendering
     * consumes these columns directly, and the plot merges them into the scale
     * pipeline so {@code afterStat(...)} aesthetics (e.g. {@code ::density})
     * resolve to a scale.
     *
     * @param statData         the computed stat output columns
     * @param statExtractor    the data extractor for the {@code statData} frame
     * @param <DF>             the DataFrame type of the source frame
     */
    record StatLayerData<DF>(StatData statData, DataExtractor<DF> statExtractor) implements LayerData {
    }
}
