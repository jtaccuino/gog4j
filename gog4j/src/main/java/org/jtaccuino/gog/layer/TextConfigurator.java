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
 * Layer configurator builder for text annotations ({@link GeomText}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class TextConfigurator<DF> implements LayerConfigurator<DF> {

    private final GeomText<DF> geom;

    /**
     * Creates a configurator for the given text annotation layer.
     *
     * @param geom the text layer to configure
     */
    public TextConfigurator(GeomText<DF> geom) {
        this.geom = geom;
    }

    /**
     * Sets the font size in points.
     *
     * @param fontSize the font size
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> size(double fontSize) {
        geom.size(fontSize);
        return this;
    }

    /**
     * Sets the text colour.
     *
     * @param color the fill colour
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> color(Color color) {
        geom.color(color);
        return this;
    }

    /**
     * Renders the labels in a bold face.
     *
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> bold() {
        geom.bold();
        return this;
    }

    /**
     * Sets the pixel offset of each label relative to its data point.
     *
     * @param dx horizontal offset in pixels
     * @param dy vertical offset in pixels; negative places the label above
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> nudge(double dx, double dy) {
        geom.nudge(dx, dy);
        return this;
    }

    /**
     * Enables or disables the collision avoidance that staggers overlapping labels.
     *
     * @param avoid {@code true} to stagger colliding labels
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> avoidOverlap(boolean avoid) {
        geom.avoidOverlap(avoid);
        return this;
    }

    /**
     * Caps how many labels are drawn, in row order.
     *
     * @param maxLabels the maximum number of labels to render
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> maxLabels(int maxLabels) {
        geom.maxLabels(maxLabels);
        return this;
    }

    /**
     * Sets the rotation of the labels in degrees clockwise from horizontal.
     *
     * @param angle the clockwise rotation in degrees
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> angle(double angle) {
        geom.angle(angle);
        return this;
    }

    /**
     * Sets the horizontal justification of the labels against their points.
     *
     * @param hjust 0 left, 0.5 centred, 1 right
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> hjust(double hjust) {
        geom.hjust(hjust);
        return this;
    }

    /**
     * Sets the vertical justification of the labels against their points.
     *
     * @param vjust 0 bottom, 0.5 centred, 1 top
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> vjust(double vjust) {
        geom.vjust(vjust);
        return this;
    }

    /**
     * Anchors every label at a constant data-space y position.
     *
     * @param y the fixed data value of the label's y coordinate
     * @return this configurator for fluid chaining
     */
    public TextConfigurator<DF> y(double y) {
        geom.y(y);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(geom);
    }
}
