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
 * Specialized builder configurator for {@link GeomErrorbar}.
 * <p>
 * Provides fluid method chaining for the whisker styling ({@link #color(Color)},
 * {@link #width(double)}, {@link #capWidth(double)}) and registration into a
 * {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class ErrorbarConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomErrorbar<DF> geom;

    /**
     * Constructs a new {@code ErrorbarConfigurator} wrapping a
     * {@link GeomErrorbar} instance.
     *
     * @param geom the error-bar geometry layer instance
     */
    public ErrorbarConfigurator(GeomErrorbar<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the whisker stroke colour for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public ErrorbarConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the whisker stroke width in pixels.
     *
     * @param width the line width in pixels
     * @return this configurator for fluid chaining
     */
    public ErrorbarConfigurator<DF> width(double width) {
        this.geom.width(width);
        return this;
    }

    /**
     * Sets the cap tick width in x-data units.
     *
     * @param capWidth the cap tick width
     * @return this configurator for fluid chaining
     */
    public ErrorbarConfigurator<DF> capWidth(double capWidth) {
        this.geom.capWidth(capWidth);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom);
    }
}
