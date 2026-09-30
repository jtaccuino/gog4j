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
 * Specialized builder configurator for density geometries ({@link GeomDensity}).
 * <p>
 * Provides fluid method chaining for configuring the kernel-density estimate
 * (bandwidth adjustment {@link #adjust(double)}, grid resolution
 * {@link #n(int)}, tail trimming {@link #trim(boolean)}, boundary reflection
 * {@link #bounds(double, double)}), the computed statistic
 * ({@link #stat(String)}), the position adjustment ({@link #position(Position)}),
 * and the area styling, then registers the layer into an {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class DensityConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomDensity<DF> geomDensity;

    /**
     * Constructs a new {@code DensityConfigurator} wrapping a {@link GeomDensity}
     * instance.
     *
     * @param geomDensity the density geometry layer instance
     */
    public DensityConfigurator(GeomDensity<DF> geomDensity) {
        this.geomDensity = geomDensity;
    }

    /**
     * Sets the multiplicative bandwidth adjustment (default {@code 1.0}).
     *
     * @param adjust the bandwidth multiplier
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> adjust(double adjust) {
        this.geomDensity.adjust(adjust);
        return this;
    }

    /**
     * Sets the number of equally spaced grid points the density is estimated at
     * (default {@code 512}).
     *
     * @param n the grid resolution
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> n(int n) {
        this.geomDensity.n(n);
        return this;
    }

    /**
     * If {@code true}, trims the tails of each estimate to the range of the
     * data. If {@code false} (default), the kernel tails extend up to three
     * bandwidths past the observed range.
     *
     * @param trim {@code true} to trim the tails to the data range
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> trim(boolean trim) {
        this.geomDensity.trim(trim);
        return this;
    }

    /**
     * Sets known lower and upper bounds for the estimated variable, e.g.
     * {@code bounds(1, Double.POSITIVE_INFINITY)}. Any finite bound corrects the
     * boundary bias by reflecting the tails around it; data points outside the
     * bounds are removed.
     *
     * @param lower the lower bound (or {@code -Inf} for none)
     * @param upper the upper bound (or {@code +Inf} for none)
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> bounds(double lower, double upper) {
        this.geomDensity.bounds(lower, upper);
        return this;
    }

    /**
     * Sets the statistic the density axis plots: {@code "density"} (default),
     * {@code "count"} (density × n, for stacked density plots), or
     * {@code "scaled"}.
     *
     * @param stat one of {@code "density"}, {@code "count"}, {@code "scaled"}
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> stat(String stat) {
        this.geomDensity.stat(stat);
        return this;
    }

    /**
     * Sets the position adjustment mode: {@link Position#IDENTITY} (default),
     * {@link Position#STACK}, or {@link Position#FILL}.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> position(Position position) {
        this.geomDensity.position(position);
        return this;
    }

    /**
     * Sets which part of the area silhouette is stroked: {@code "upper"}
     * (default), {@code "lower"}, {@code "both"}, or {@code "full"}.
     *
     * @param outlineType one of {@code "upper"}, {@code "lower"}, {@code "both"}, {@code "full"}
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> outlineType(String outlineType) {
        this.geomDensity.outlineType(outlineType);
        return this;
    }

    /**
     * Sets the constant fill color of the density areas.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> fill(Color color) {
        this.geomDensity.fill(color);
        return this;
    }

    /**
     * Sets the stroke color of the density curves.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> color(Color color) {
        this.geomDensity.color(color);
        return this;
    }

    /**
     * Sets the fill opacity of the density areas (default {@code 0.35}).
     *
     * @param alpha the fill opacity between 0 and 1
     * @return this {@code DensityConfigurator} instance for fluid chaining
     */
    public DensityConfigurator<DF> alpha(double alpha) {
        this.geomDensity.alpha(alpha);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomDensity);
    }
}
