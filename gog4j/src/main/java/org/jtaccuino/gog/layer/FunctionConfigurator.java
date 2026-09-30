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

import java.util.function.DoubleUnaryOperator;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Configurator for building and registering {@link GeomFunction} geometry
 * layers (mathematical curves) on an {@link Plot}.
 *
 * @param <DF> the DataFrame type
 */
public class FunctionConfigurator<DF> implements LayerConfigurator<DF> {

    private final GeomFunction<DF> geomFunction;
    private Aes localAes = null;

    /**
     * Creates a configurator wrapping the given function geometry layer.
     *
     * @param geomFunction the function geometry layer
     */
    public FunctionConfigurator(GeomFunction<DF> geomFunction) {
        this.geomFunction = geomFunction;
    }

    /**
     * Attaches a local aesthetic mapping to this function layer. Because a
     * function carries no data of its own, any {@code color} mapping here is
     * interpreted as a constant group label: the curve is stroked with its
     * palette colour and, with several labelled functions, a matching legend
     * key is produced.
     *
     * @param localAes the local aesthetic mapping for this function layer
     * @return this configurator for fluid chaining
     */
    public FunctionConfigurator<DF> aes(Aes localAes) {
        this.localAes = localAes;
        return this;
    }

    /**
     * Restricts the function's x range; without data this also drives the
     * plot's x axis.
     *
     * @param min the lower x bound
     * @param max the upper x bound
     * @return this configurator for fluid chaining
     */
    public FunctionConfigurator<DF> xlim(double min, double max) {
        this.geomFunction.xlim(min, max);
        return this;
    }

    /**
     * Sets the number of points the function is evaluated at along the x axis.
     *
     * @param n the number of interpolation points
     * @return this configurator for fluid chaining
     */
    public FunctionConfigurator<DF> n(int n) {
        this.geomFunction.n(n);
        return this;
    }

    /**
     * Sets the stroke colour of the curve.
     *
     * @param color the curve colour
     * @return this configurator for fluid chaining
     */
    public FunctionConfigurator<DF> color(Color color) {
        this.geomFunction.color(color);
        return this;
    }

    /**
     * Sets the stroke width of the curve in pixels.
     *
     * @param width the stroke width
     * @return this configurator for fluid chaining
     */
    public FunctionConfigurator<DF> width(double width) {
        this.geomFunction.width(width);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        if (this.localAes != null) {
            plot.registerInternalLayer(this.geomFunction, this.localAes);
        } else {
            plot.registerInternalLayer(this.geomFunction);
        }
    }
}
