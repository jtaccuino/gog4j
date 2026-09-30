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
 * Specialized builder configurator for {@link GeomHistogram}.
 * <p>
 * Provides fluid method chaining for the binning controls ({@link #bins(int)},
 * {@link #binwidth(double)}, {@link #boundary(double)}), the position layout
 * ({@link #position(Position)}), the bar styling ({@link #fill(Color)},
 * {@link #color(Color)}), and registration into a {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class HistogramConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomHistogram<DF> geom;
    private Aes localAes;

    /**
     * Constructs a new {@code HistogramConfigurator} wrapping a
     * {@link GeomHistogram} instance.
     *
     * @param geom the histogram geometry layer instance
     */
    public HistogramConfigurator(GeomHistogram<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the (approximate) number of bins over the data range.
     *
     * @param bins the bin count, {@code 30} by default
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> bins(int bins) {
        this.geom.bins(bins);
        return this;
    }

    /**
     * Sets a fixed bin width in data units.
     *
     * @param binwidth the width of every bin
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> binwidth(double binwidth) {
        this.geom.binwidth(binwidth);
        return this;
    }

    /**
     * Anchors the bin-edge grid so an edge falls on the given value.
     *
     * @param boundary the edge alignment point
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> boundary(double boundary) {
        this.geom.boundary(boundary);
        return this;
    }

    /**
     * Sets the position adjustment mode (identity, dodge, or stack).
     *
     * @param position the {@link Position} layout mode
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> position(Position position) {
        this.geom.position(position);
        return this;
    }

    /**
     * Sets the fill color of the histogram bars for ungrouped rendering.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> fill(Color color) {
        this.geom.fill(color);
        return this;
    }

    /**
     * Sets the stroke color of the histogram bars for ungrouped rendering.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> color(Color color) {
        this.geom.color(color);
        return this;
    }

    /**
     * Sets a layer-local aesthetic mapping, merged over the plot-global
     * {@code aes()} for this layer only.
     *
     * @param aes the local aesthetic mappings
     * @return this configurator for fluid chaining
     */
    public HistogramConfigurator<DF> mapping(Aes aes) {
        this.localAes = aes;
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geom, localAes);
    }
}
