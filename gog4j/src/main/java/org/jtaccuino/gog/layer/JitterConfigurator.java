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

import javafx.scene.paint.Color;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Layer configurator builder for jittered scatter point geometries ({@link GeomJitter}).
 * <p>
 * Provides fluid chaining methods to configure jitter amount, point size, color,
 * symbol shape, opacity, and the random seed.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class JitterConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomJitter<DF> geomJitter;
    private final PointSpec spec;

    /**
     * Constructs a new {@code JitterConfigurator} wrapping a {@link GeomJitter} and {@link PointSpec}.
     *
     * @param geomJitter the target jittered point geometry layer
     * @param spec the point specification
     */
    public JitterConfigurator(GeomJitter<DF> geomJitter, PointSpec spec) {
        this.geomJitter = geomJitter;
        this.spec = spec;
        this.geomJitter.setSpec(spec);
    }

    /**
     * Sets the amount of horizontal jitter in data units. Jitter is added in both
     * positive and negative directions, so the total spread is twice this value.
     *
     * @param width jitter width in data units
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> width(double width) {
        this.geomJitter.width(width);
        return this;
    }

    /**
     * Sets the amount of vertical jitter in data units. Jitter is added in both
     * positive and negative directions, so the total spread is twice this value.
     *
     * @param height jitter height in data units
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> height(double height) {
        this.geomJitter.height(height);
        return this;
    }

    /**
     * Sets the point diameter size in pixels.
     *
     * @param size diameter in pixels
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> size(double size) {
        this.spec.size(size);
        return this;
    }

    /**
     * Sets a constant fill and outline color for points.
     *
     * @param color JavaFX {@link Color}
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> color(Color color) {
        this.spec.color(color);
        return this;
    }

    /**
     * Sets a constant point symbol shape.
     *
     * @param shape the {@link PointShape} symbol
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> shape(PointShape shape) {
        this.spec.shape(shape);
        return this;
    }

    /**
     * Sets the opacity for rendered points.
     *
     * @param opacity between 0.0 (fully transparent) and 1.0 (fully opaque)
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> opacity(double opacity) {
        this.spec.opacity(opacity);
        return this;
    }

    /**
     * Sets the random seed used to generate deterministic per-row jitter offsets.
     *
     * @param seed the RNG seed
     * @return this {@code JitterConfigurator} instance for fluid chaining
     */
    public JitterConfigurator<DF> seed(long seed) {
        this.geomJitter.seed(seed);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomJitter);
    }
}
