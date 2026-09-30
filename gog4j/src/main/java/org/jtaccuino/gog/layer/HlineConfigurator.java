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
 * Layer configurator builder for horizontal reference lines ({@link GeomHline}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class HlineConfigurator<DF> implements LayerConfigurator<DF> {

    private final GeomHline<DF> geom;

    /**
     * Creates a configurator for the given horizontal reference line layer.
     *
     * @param geom the reference line layer to configure
     */
    public HlineConfigurator(GeomHline<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the line colour.
     *
     * @param color the stroke colour
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> color(Color color) {
        geom.color(color);
        return this;
    }

    /**
     * Maps the line colour to a column of the data-driven frame, mirroring
     * {@code colourMapping} in the reference implementation. Numeric columns use the continuous
     * ramp; categorical columns use the palette.
     *
     * @param column the column to map colour from
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> color(String column) {
        geom.color(column);
        return this;
    }

    /**
     * Associates the data frame the Y intercept values are read from.
     *
     * @param data frame holding the intercept values
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> data(DF data) {
        geom.data(data);
        return this;
    }

    /**
     * Names the column holding the Y intercept values.
     *
     * @param column the column name
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> yintercept(String column) {
        geom.yintercept(column);
        return this;
    }

    /**
     * Sets the stroke width in pixels.
     *
     * @param width the stroke width
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> width(double width) {
        geom.width(width);
        return this;
    }

    /**
     * Renders the line dashed rather than solid.
     *
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> dashed() {
        geom.dashed();
        return this;
    }

    /**
     * Sets an explicit dash pattern.
     *
     * @param pattern alternating on/off segment lengths in pixels
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> dashes(double... pattern) {
        geom.dashes(pattern);
        return this;
    }

    /**
     * Adds a short caption drawn at the right-hand end of the line.
     *
     * @param text the caption
     * @return this configurator for fluid chaining
     */
    public HlineConfigurator<DF> annotate(String text) {
        geom.annotate(text);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(geom);
    }
}
