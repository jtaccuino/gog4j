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
 * Layer configurator builder for scatter plot point geometries ({@link GeomPoint}).
 * <p>
 * Provides fluid chaining methods to configure point size, color, and symbol shape.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class PointConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomPoint<DF> geomPoint;
    private final PointSpec spec;

    /**
     * Constructs a new {@code PointConfigurator} wrapping a {@link GeomPoint} and {@link PointSpec}.
     *
     * @param geomPoint the target point geometry layer
     * @param spec the point specification
     */
    public PointConfigurator(GeomPoint<DF> geomPoint, PointSpec spec) {
        this.geomPoint = geomPoint;
        this.spec = spec;
        this.geomPoint.setSpec(spec);
    }

    /**
     * Sets the point diameter size in pixels.
     *
     * @param size diameter in pixels
     * @return this {@code PointConfigurator} instance for fluid chaining
     */
    public PointConfigurator<DF> size(double size) {
        this.spec.size(size);
        return this;
    }

    /**
     * Sets a constant fill and outline color for points.
     *
     * @param color JavaFX {@link Color}
     * @return this {@code PointConfigurator} instance for fluid chaining
     */
    public PointConfigurator<DF> color(Color color) {
        this.spec.color(color);
        return this;
    }

    /**
     * Sets a constant point symbol shape.
     *
     * @param shape the {@link PointShape} symbol (e.g., CIRCLE, SQUARE, TRIANGLE, DIAMOND, PLUS, CROSS)
     * @return this {@code PointConfigurator} instance for fluid chaining
     */
    public PointConfigurator<DF> shape(PointShape shape) {
        this.spec.shape(shape);
        return this;
    }

    /**
     * Sets the opacity for rendered points.
     *
     * @param opacity between 0.0 (fully transparent) and 1.0 (fully opaque)
     * @return this {@code PointConfigurator} instance for fluid chaining
     */
    public PointConfigurator<DF> opacity(double opacity) {
        this.spec.opacity(opacity);
        return this;
    }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} so the scatter projects onto a face of a
     * 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code PointConfigurator} instance for fluid chaining
     */
    public PointConfigurator<DF> position(PositionAdjust position) {
        this.geomPoint.position(position);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomPoint);
    }
}
