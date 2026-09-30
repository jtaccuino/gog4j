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
 * Specialized builder configurator for 2D density contour geometries
 * ({@link GeomDensity2d}).
 * <p>
 * Provides fluid method chaining for configuring the kernel-density estimate
 * (grid resolution {@link #n(int)}, bandwidth adjustment
 * {@link #adjust(double)} / {@link #adjust(double, double)}), the contouring
 * ({@link #bins(int)}, {@link #contourVar(String)}), the rendering style
 * ({@link #filled(boolean)}, {@link #alpha(double)}, {@link #lineWidth(double)},
 * {@link #cmap(String)}, {@link #color(Color)}), then registers the layer into
 * an {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class Density2dConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomDensity2d<DF> geomDensity2d;

    /**
     * Constructs a new {@code Density2dConfigurator} wrapping a
     * {@link GeomDensity2d} instance.
     *
     * @param geomDensity2d the 2D density geometry layer instance
     */
    public Density2dConfigurator(GeomDensity2d<DF> geomDensity2d) {
        this.geomDensity2d = geomDensity2d;
    }

    /**
     * Sets the number of grid points the estimate is sampled at in each
     * direction (default {@code 100}).
     *
     * @param n the grid resolution
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> n(int n) {
        this.geomDensity2d.n(n);
        return this;
    }

    /**
     * Sets the multiplicative bandwidth adjustment applied to both axes
     * (default {@code 1.0}).
     *
     * @param adjust the bandwidth multiplier for both axes
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> adjust(double adjust) {
        this.geomDensity2d.adjust(adjust);
        return this;
    }

    /**
     * Sets independent bandwidth adjustments for the x and y axes.
     *
     * @param xAdjust the bandwidth multiplier for the x axis
     * @param yAdjust the bandwidth multiplier for the y axis
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> adjust(double xAdjust, double yAdjust) {
        this.geomDensity2d.adjust(xAdjust, yAdjust);
        return this;
    }

    /**
     * Sets the number of contour levels (default {@code 10}).
     *
     * @param bins the number of evenly spaced contour levels
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> bins(int bins) {
        this.geomDensity2d.bins(bins);
        return this;
    }

    /**
     * Sets the statistic the contours trace: {@code "density"} (default),
     * {@code "ndensity"}, or {@code "count"}.
     *
     * @param contourVar one of {@code "density"}, {@code "ndensity"}, {@code "count"}
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> contourVar(String contourVar) {
        this.geomDensity2d.contourVar(contourVar);
        return this;
    }

    /**
     * Turns on filled contour bands (the {@code Geoms.density2dFilled()}
     * rendering) instead of contour lines.
     *
     * @param filled {@code true} to fill the bands between contour levels
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> filled(boolean filled) {
        this.geomDensity2d.filled(filled);
        return this;
    }

    /**
     * Sets the fill opacity of the contour bands (default {@code 0.5}).
     *
     * @param alpha fill opacity between 0 and 1
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> alpha(double alpha) {
        this.geomDensity2d.alpha(alpha);
        return this;
    }

    /**
     * Sets the stroke width of the contour lines (default {@code 0.5}).
     *
     * @param lineWidth the stroke width in pixels
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> lineWidth(double lineWidth) {
        this.geomDensity2d.lineWidth(lineWidth);
        return this;
    }

    /**
     * Sets the colour ramp used by the filled bands (default {@code viridis}).
     *
     * @param cmapName the colour ramp name
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> cmap(String cmapName) {
        this.geomDensity2d.cmap(cmapName);
        return this;
    }

    /**
     * Sets the constant stroke color of the contour lines.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> color(Color color) {
        this.geomDensity2d.color(color);
        return this;
    }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} so the density contours lie flat on a
     * face of a 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code Density2dConfigurator} instance for fluid chaining
     */
    public Density2dConfigurator<DF> position(PositionAdjust position) {
        this.geomDensity2d.position(position);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomDensity2d);
    }
}
