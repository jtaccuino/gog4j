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
 * Specialized builder configurator for box-and-whiskers geometries ({@link GeomBoxplot}).
 * <p>
 * Provides fluid method chaining for configuring boxplot styling (e.g. {@link #fill(Color)})
 * and registering the layer into an {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class BoxplotConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomBoxplot<DF> geomBoxplot;

    /**
     * Constructs a new {@code BoxplotConfigurator} wrapping a {@link GeomBoxplot} instance.
     *
     * @param geomBoxplot the boxplot geometry layer instance
     */
    public BoxplotConfigurator(GeomBoxplot<DF> geomBoxplot) {
        this.geomBoxplot = geomBoxplot;
    }

    /**
     * Sets the position adjustment mode. {@link Position#DODGE} (default)
     * splits each group into sub-groups by the mapped fill/color column and
     * places them side by side (the default for {@code Geoms.boxplot}).
     * {@link Position#IDENTITY} draws all sub-boxes at the group's position,
     * overlapping at full width.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> position(Position position) {
        this.geomBoxplot.position(position);
        return this;
    }

    /**
     * Sets the fraction of each dodge slot occupied by a box, leaving the
     * remainder as spacing between neighbouring dodged boxes. {@code 1.0}
     * makes the boxes touch, {@code 0.9} (default) leaves a small gap.
     *
     * @param dodgeWidth box width as a fraction of the dodge slot, between 0.0 and 1.0
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> dodgeWidth(double dodgeWidth) {
        this.geomBoxplot.dodgeWidth(dodgeWidth);
        return this;
    }

    /**
     * Sets the box width factor relative to the available tick spacing.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> widthFactor(double widthFactor) {
        this.geomBoxplot.widthFactor(widthFactor);
        return this;
    }

    /**
     * Sets the constant fill color of the boxes.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> fill(Color color) {
        this.geomBoxplot.fill(color);
        return this;
    }

    /**
     * Sets the stroke color of the boxes and whiskers.
     *
     * @param color the JavaFX stroke {@link Color}
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> color(Color color) {
        this.geomBoxplot.color(color);
        return this;
    }

    /**
     * Sets the fill color of the outlier points.
     *
     * @param color the JavaFX outlier {@link Color}
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> outlierColor(Color color) {
        this.geomBoxplot.outlierColor(color);
        return this;
    }

    /**
     * Sets the outlier point radius in pixels.
     *
     * @param size the outlier point radius in pixels
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> outlierSize(double size) {
        this.geomBoxplot.outlierSize(size);
        return this;
    }

    /**
     * Sets the stroke width of the median band in pixels.
     *
     * @param width the median line stroke width in pixels
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> medianLineWidth(double width) {
        this.geomBoxplot.medianLineWidth(width);
        return this;
    }

    /**
     * Enables or disables notched box plots. Notches extend
     * {@code 1.58 * IQR / sqrt(n)} around the median, giving a roughly 95%
     * confidence interval for comparing medians.
     *
     * @param notch {@code true} to draw notches
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> notch(boolean notch) {
        this.geomBoxplot.notch(notch);
        return this;
    }

    /**
     * Sets the width of the notch relative to the box body (default {@code 0.5}).
     *
     * @param notchwidth notch width factor between 0.0 and 1.0
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> notchwidth(double notchwidth) {
        this.geomBoxplot.notchwidth(notchwidth);
        return this;
    }

    /**
     * If {@code true}, boxes are drawn with widths proportional to the square root
     * of the number of observations in each group (relative to the largest group).
     *
     * @param varwidth {@code true} for variable box widths
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> varwidth(boolean varwidth) {
        this.geomBoxplot.varwidth(varwidth);
        return this;
    }

    /**
     * Enables or disables outlier rendering entirely.
     *
     * @param draw {@code true} to render outlier points, {@code false} to suppress them
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> drawOutliers(boolean draw) {
        this.geomBoxplot.drawOutliers(draw);
        return this;
    }

    /**
     * Sets the {@link PointShape} symbol used for outlier points, or {@code null}
     * to suppress outliers completely.
     *
     * @param shape the outlier point shape, or {@code null} to hide outliers
     * @return this {@code BoxplotConfigurator} instance for fluid chaining
     */
    public BoxplotConfigurator<DF> outlierShape(PointShape shape) {
        this.geomBoxplot.outlierShape(shape);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomBoxplot);
    }
}
