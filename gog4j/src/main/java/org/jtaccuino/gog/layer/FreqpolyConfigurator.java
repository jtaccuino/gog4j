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
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Specialized builder configurator for {@link GeomFreqpoly}.
 * <p>
 * Provides fluid method chaining for the binning controls ({@link #bins(int)},
 * {@link #binwidth(double)}, {@link #boundary(double)}), the line styling
 * ({@link #color(Color)}, {@link #width(double)}), and registration into a
 * {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class FreqpolyConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomFreqpoly<DF> geom;
    private Aes localAes;

    /**
     * Constructs a new {@code FreqpolyConfigurator} wrapping a
     * {@link GeomFreqpoly} instance.
     *
     * @param geom the frequency-polygon geometry layer instance
     */
    public FreqpolyConfigurator(GeomFreqpoly<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the (approximate) number of bins over the data range.
     *
     * @param bins the bin count, {@code 30} by default
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> bins(int bins) {
        this.geom.bins(bins);
        return this;
    }

    /**
     * Sets a fixed bin width in data units.
     *
     * @param binwidth the width of every bin
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> binwidth(double binwidth) {
        this.geom.binwidth(binwidth);
        return this;
    }

    /**
     * Anchors the bin-edge grid so an edge falls on the given value.
     *
     * @param boundary the edge alignment point
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> boundary(double boundary) {
        this.geom.boundary(boundary);
        return this;
    }

    /**
     * Sets the line color for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets the line stroke width in pixels.
     *
     * @param width the line width in pixels
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> width(double width) {
        this.geom.width(width);
        return this;
    }

    /**
     * Sets a layer-local aesthetic mapping, merged over the plot-global
     * {@code aes()} for this layer only.
     *
     * @param aes the local aesthetic mappings
     * @return this configurator for fluid chaining
     */
    public FreqpolyConfigurator<DF> mapping(Aes aes) {
        this.localAes = aes;
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom, localAes);
    }
}
