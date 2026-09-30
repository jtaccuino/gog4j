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
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Configurator for building and registering {@link GeomSmooth} geometry layers
 * (LOESS/LM trend lines) on an {@link Plot}.
 *
 * @param <DF> the DataFrame type
 */
public class SmoothConfigurator<DF> implements LayerConfigurator<DF> {

    private final GeomSmooth<DF> geomSmooth;
    private Aes localAes = null;

    /**
     * Creates a smooth configurator wrapping the given {@link GeomSmooth}.
     *
     * @param geomSmooth the smooth geometry layer
     */
    public SmoothConfigurator(GeomSmooth<DF> geomSmooth) {
        this.geomSmooth = geomSmooth;
    }

    /**
     * Creates a smooth configurator with a local aesthetic mapping override.
     *
     * @param geomSmooth the smooth geometry layer
     * @param localAes   local aesthetic mappings overriding global mappings
     */
    public SmoothConfigurator(GeomSmooth<DF> geomSmooth, Aes localAes) {
        this.geomSmooth = geomSmooth;
        this.localAes = localAes;
    }

    /**
     * Sets the smoothing method (LOESS or LM).
     *
     * @param method the {@link SmoothMethod} to use
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> method(SmoothMethod method) {
        this.geomSmooth.method(method);
        return this;
    }

    /**
     * Sets the smoothing span parameter (0.0 to 1.0).
     * Smaller values produce more wiggly curves.
     *
     * @param span the span parameter (default 0.75)
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> span(double span) {
        this.geomSmooth.span(span);
        return this;
    }

    /**
     * Enables or disables the confidence band (standard error ribbon).
     *
     * @param se {@code true} to show the confidence band
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> se(boolean se) {
        this.geomSmooth.se(se);
        return this;
    }

    /**
     * Sets a constant line color for the smooth trend line.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> color(Color color) {
        this.geomSmooth.color(color);
        return this;
    }

    /**
     * Forces the confidence band (se) to a constant fill color,
     * independent of the categorical line color.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> fill(Color color) {
        this.geomSmooth.fill(color);
        return this;
    }

    /**
     * Enables global extrapolation of the smooth curve across the full
     * plot width, rather than only within each group's data range.
     *
     * @param fullrange {@code true} to extrapolate across the full range
     * @return this {@code SmoothConfigurator} for fluid chaining
     */
    public SmoothConfigurator<DF> fullrange(boolean fullrange) {
        this.geomSmooth.fullrange(fullrange);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        if (this.localAes != null) {
            plot.registerInternalLayer(this.geomSmooth, this.localAes);
        } else {
            plot.registerInternalLayer(this.geomSmooth);
        }
    }
}
