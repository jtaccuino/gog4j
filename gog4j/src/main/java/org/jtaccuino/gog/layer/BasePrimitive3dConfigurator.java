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

import org.jtaccuino.gog.ConfigTarget;

/**
 * Shared plumbing for the 3-D primitive geometry configurators (points, text,
 * segments and tiles): a reference to the single geometry layer being built
 * plus the {@link ConfigTarget} registration every such configurator performs.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 * @param <G>  the 3-D primitive geometry layer being configured
 * @param <S>  the concrete configurator type (for fluent chaining)
 */
public abstract class BasePrimitive3dConfigurator<DF, G extends Layer<DF>, S extends BasePrimitive3dConfigurator<DF, G, S>> implements LayerConfigurator<DF> {

    /** The geometry layer being configured. */
    protected final G geom;

    /**
     * Creates a configurator around the given 3-D primitive geometry layer.
     *
     * @param geom the geometry layer to configure
     */
    protected BasePrimitive3dConfigurator(G geom) {
        this.geom = geom;
    }

    /**
     * {@return the geometry layer being configured}
     */
    protected G geom() {
        return geom;
    }

    @Override
    public void configure(ConfigTarget<DF> target) {
        target.registerInternalLayer(geom);
    }
}
