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
 * Specialized builder configurator for {@link GeomPointrange}.
 * <p>
 * Provides fluid method chaining for the stroke and point styling
 * ({@link #color(Color)}, {@link #width(double)}, {@link #pointRadius(double)})
 * and registration into a {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class PointrangeConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomPointrange<DF> geom;

    /**
     * Constructs a new {@code PointrangeConfigurator} wrapping a
     * {@link GeomPointrange} instance.
     *
     * @param geom the pointrange geometry layer instance
     */
    public PointrangeConfigurator(GeomPointrange<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the line and point colour for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public PointrangeConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param width the line width in pixels
     * @return this configurator for fluid chaining
     */
    public PointrangeConfigurator<DF> width(double width) {
        this.geom.width(width);
        return this;
    }

    /**
     * Sets the point radius in pixels.
     *
     * @param radius the point radius in pixels
     * @return this configurator for fluid chaining
     */
    public PointrangeConfigurator<DF> pointRadius(double radius) {
        this.geom.pointRadius(radius);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom);
    }
}
