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
 * Configurator for building and registering {@link GeomArea} geometry layers
 * on an {@link Plot}.
 *
 * @param <DF> the DataFrame type
 */
public class AreaConfigurator<DF> implements LayerConfigurator<DF> {

    private final GeomArea<DF> geomArea;

    /**
     * Creates an area configurator wrapping the given {@link GeomArea}.
     *
     * @param geomArea the area geometry layer
     */
    public AreaConfigurator(GeomArea<DF> geomArea) {
        this.geomArea = geomArea;
    }

    /**
     * Sets a constant fill color for the area geometry.
     *
     * @param color the JavaFX fill {@link Color}
     * @return this {@code AreaConfigurator} for fluid chaining
     */
    public AreaConfigurator<DF> fill(Color color) {
        this.geomArea.fill(color);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomArea);
    }

    /**
     * Sets the position adjustment mode (stack, identity, etc.) for the area geometry.
     *
     * @param position the {@link Position} layout mode
     * @return this {@code AreaConfigurator} for fluid chaining
     */
    public AreaConfigurator<DF> position(Position position) {
        this.geomArea.position(position);
        return this;
    }
}
