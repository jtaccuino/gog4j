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
 * Specialized builder configurator for {@link GeomRibbon}.
 * <p>
 * Provides fluid method chaining for the band styling ({@link #fill(Color)},
 * {@link #color(Color)}, {@link #alpha(double)}, {@link #outline(boolean)})
 * and registration into a {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class RibbonConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomRibbon<DF> geom;

    /**
     * Constructs a new {@code RibbonConfigurator} wrapping a
     * {@link GeomRibbon} instance.
     *
     * @param geom the ribbon geometry layer instance
     */
    public RibbonConfigurator(GeomRibbon<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the band fill colour for ungrouped rendering.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this configurator for fluid chaining
     */
    public RibbonConfigurator<DF> fill(Color color) {
        this.geom.fill(color);
        return this;
    }

    /**
     * Sets the band outline colour for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public RibbonConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the fill opacity applied to every band.
     *
     * @param alpha the fill opacity in {@code [0, 1]}
     * @return this configurator for fluid chaining
     */
    public RibbonConfigurator<DF> alpha(double alpha) {
        this.geom.alpha(alpha);
        return this;
    }

    /**
     * Enables or disables the band outline.
     *
     * @param on {@code true} to stroke the upper edge
     * @return this configurator for fluid chaining
     */
    public RibbonConfigurator<DF> outline(boolean on) {
        this.geom.outline(on);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom);
    }
}
