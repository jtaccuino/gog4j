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
 * Specialized builder configurator for {@link GeomCrossbar}.
 * <p>
 * Provides fluid method chaining for the box styling ({@link #fill(Color)},
 * {@link #color(Color)}, {@link #width(double)}, {@link #fatten(double)},
 * {@link #lineWidth(double)}) and registration into a {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class CrossbarConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomCrossbar<DF> geom;

    /**
     * Constructs a new {@code CrossbarConfigurator} wrapping a
     * {@link GeomCrossbar} instance.
     *
     * @param geom the crossbar geometry layer instance
     */
    public CrossbarConfigurator(GeomCrossbar<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the box fill colour for ungrouped rendering.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this configurator for fluid chaining
     */
    public CrossbarConfigurator<DF> fill(Color color) {
        this.geom.fill(color);
        return this;
    }

    /**
     * Sets the box stroke and centre-line colour for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public CrossbarConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the box width in x-data units.
     *
     * @param width the box width, {@code 0.9} by default
     * @return this configurator for fluid chaining
     */
    public CrossbarConfigurator<DF> width(double width) {
        this.geom.width(width);
        return this;
    }

    /**
     * Sets the centre-line width multiplier.
     *
     * @param fatten the multiplier, {@code 2.5} by default
     * @return this configurator for fluid chaining
     */
    public CrossbarConfigurator<DF> fatten(double fatten) {
        this.geom.fatten(fatten);
        return this;
    }

    /**
     * Sets the box outline stroke width in pixels.
     *
     * @param width the line width in pixels
     * @return this configurator for fluid chaining
     */
    public CrossbarConfigurator<DF> lineWidth(double width) {
        this.geom.lineWidth(width);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom);
    }
}
