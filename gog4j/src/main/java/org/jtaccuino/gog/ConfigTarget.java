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

import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.layer.PositionAdjust;
import org.jtaccuino.gog.layer.StatHost;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.stat.Stat;

/**
 * The surface of a spec that geometry and scale configurators mutate.
 * <p>
 * Both {@link PlotDescriptor} and {@link Plot} expose this surface, so the
 * declarative configurator builders ({@code Ggplot}'s {@code geoms}/{@code scales}
 * block) bind once to {@link ConfigTarget} and operate on either the mutable
 * spec (further customized, embedded in a {@code PlotMatrix}) or the ready-made
 * plot.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public interface ConfigTarget<DF> {

    /**
     * Registers an internal geometry layer bound to this spec's {@link Aes}.
     *
     * @param layer the geometry layer to register
     * @return this spec, for chaining
     */
    ConfigTarget<DF> registerInternalLayer(Layer<DF> layer);

    /**
     * Registers an internal geometry layer carrying its own local {@link Aes},
     * living outside the mapping given to {@code Ggplot} (e.g. reference lines,
     * annotations).
     *
     * @param layer     the geometry layer to register
     * @param localAes  the layer-local mapping
     * @return this spec, for chaining
     */
    ConfigTarget<DF> registerInternalLayer(Layer<DF> layer, Aes localAes);

    /**
     * The current scale specification of this spec, which scale configurators
     * mutate in place.
     *
     * @return this spec's {@link ScaleSpec}
     */
    ScaleSpec getScaleSpec();

    /**
     * Assembles an all-in-one layer from a stat-consuming geometry, a stat, a
     * position adjustment, a layer-local mapping, and a params bag — the
     * gog4j counterpart of the
     * {@code Plot#layer(...)}. Shared by every
     * receiver so bar-family configurators need no per-receiver variant.
     *
     * @param geom     the stat-consuming geometry layer
     * @param stat     the statistic to run
     * @param position the position adjustment
     * @param mapping  the layer-local aesthetic mapping, merged over the global one
     * @param params   the stat config and geom aesthetic defaults
     * @return this spec, for chaining
     */
    default ConfigTarget<DF> layer(Layer<DF> geom, Stat<DF> stat, PositionAdjust position,
                                   Aes mapping, LayerParams params) {
        if (geom instanceof StatHost) {
            @SuppressWarnings("unchecked")
            var host = (StatHost<DF>) geom;
            host.attach(stat, position, params);
        }
        registerInternalLayer(geom, mapping);
        return this;
    }
}
