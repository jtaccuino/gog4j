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
 * Specialized builder configurator for violin geometries ({@link GeomViolin}).
 * <p>
 * Provides fluid method chaining for configuring violin styling (e.g.
 * {@link #fill(Color)}), density bandwidth ({@link #adjust(double)}), tail
 * trimming ({@link #trim(boolean)}), and width scaling
 * ({@link #scale(String)}), then registers the layer into an {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class ViolinConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomViolin<DF> geomViolin;

    /**
     * Constructs a new {@code ViolinConfigurator} wrapping a {@link GeomViolin} instance.
     *
     * @param geomViolin the violin geometry layer instance
     */
    public ViolinConfigurator(GeomViolin<DF> geomViolin) {
        this.geomViolin = geomViolin;
    }

    /**
     * Sets the position adjustment mode. {@link Position#DODGE} (default)
     * splits each group into sub-groups by the mapped fill/color column and
     * places them side by side (the default for {@code Geoms.violin}).
     * {@link Position#IDENTITY} draws all sub-violins at the group's position,
     * overlapping at full width.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> position(Position position) {
        this.geomViolin.position(position);
        return this;
    }

    /**
     * Sets the violin width factor relative to the available tick spacing.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> widthFactor(double widthFactor) {
        this.geomViolin.widthFactor(widthFactor);
        return this;
    }

    /**
     * Sets the constant fill color of the violins.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> fill(Color color) {
        this.geomViolin.fill(color);
        return this;
    }

    /**
     * Sets the stroke color of the violin outlines.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> color(Color color) {
        this.geomViolin.color(color);
        return this;
    }

    /**
     * Controls how the width of the violins is scaled: {@code "area"}
     * (default) keeps all violins at the same area, {@code "count"} scales the
     * area with the sample size, and {@code "width"} gives every violin the
     * same maximum width.
     *
     * @param scale one of {@code "area"}, {@code "count"}, {@code "width"}
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> scale(String scale) {
        this.geomViolin.scale(scale);
        return this;
    }

    /**
     * If {@code true} (default), trims the tails of the violins to the range of
     * the data. If {@code false}, the kernel-density tails extend up to three
     * bandwidths past the observed range.
     *
     * @param trim {@code true} to trim the tails to the data range
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> trim(boolean trim) {
        this.geomViolin.trim(trim);
        return this;
    }

    /**
     * Sets the multiplicative bandwidth adjustment (default {@code 1.0}).
     *
     * @param adjust the bandwidth multiplier
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> adjust(double adjust) {
        this.geomViolin.adjust(adjust);
        return this;
    }

    /**
     * Draws horizontal marks across each violin at the given quantiles, e.g.
     * {@code drawQuantiles(0.25, 0.5, 0.75)} for the quartiles.
     *
     * @param quantiles the quantile probabilities to mark
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> drawQuantiles(double... quantiles) {
        this.geomViolin.drawQuantiles(quantiles);
        return this;
    }

    /**
     * Sets the color of the quantile marks.
     *
     * @param color the JavaFX {@link Color} of the quantile lines
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> quantileColor(Color color) {
        this.geomViolin.quantileColor(color);
        return this;
    }

    /**
     * Sets the stroke width of the quantile marks in pixels.
     *
     * @param width the quantile line stroke width in pixels
     * @return this {@code ViolinConfigurator} instance for fluid chaining
     */
    public ViolinConfigurator<DF> quantileLineWidth(double width) {
        this.geomViolin.quantileLineWidth(width);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomViolin);
    }
}
