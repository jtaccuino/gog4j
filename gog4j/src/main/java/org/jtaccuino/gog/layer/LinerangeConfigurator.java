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
 * Specialized builder configurator for {@link GeomLinerange}.
 * <p>
 * Provides fluid method chaining for the line styling ({@link #color(Color)},
 * {@link #width(double)}) and registration into a {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class LinerangeConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomLinerange<DF> geom;

    /**
     * Constructs a new {@code LinerangeConfigurator} wrapping a
     * {@link GeomLinerange} instance.
     *
     * @param geom the linerange geometry layer instance
     */
    public LinerangeConfigurator(GeomLinerange<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the line colour for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public LinerangeConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param width the line width in pixels
     * @return this configurator for fluid chaining
     */
    public LinerangeConfigurator<DF> width(double width) {
        this.geom.width(width);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom);
    }
}
