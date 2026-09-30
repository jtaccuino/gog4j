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
 * Specialized builder configurator for polygon geometries ({@link GeomPolygon}).
 * <p>
 * {@code Geoms.polygon()} draws one closed polygon per group, the geometry
 * behind radar and spider charts when combined with
 * {@link org.jtaccuino.gog.Coords#coordPolar()}. Provides fluid method chaining for the
 * outline colour ({@link #color(Color)}), stroke width ({@link #lineWidth(double)}),
 * fill opacity ({@link #alpha(double)}), and whether the polygon is filled at
 * all ({@link #filled(boolean)}), then registers the layer into an {@link Plot}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class PolygonConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomPolygon<DF> geomPolygon;

    /**
     * Constructs a new {@code PolygonConfigurator} wrapping a {@link GeomPolygon}
     * instance.
     *
     * @param geomPolygon the polygon geometry layer instance
     */
    public PolygonConfigurator(GeomPolygon<DF> geomPolygon) {
        this.geomPolygon = geomPolygon;
    }

    /**
     * Sets the base colour of an ungrouped polygon, and the stroke colour of a
     * grouped one.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code PolygonConfigurator} instance for fluid method chaining
     */
    public PolygonConfigurator<DF> color(Color color) {
        this.geomPolygon.color(color);
        return this;
    }

    /**
     * Sets the fill opacity of each polygon (default 0.35), so overlapping
     * webs remain readable.
     *
     * @param alpha fill alpha in the range [0, 1]
     * @return this {@code PolygonConfigurator} instance for fluid method chaining
     */
    public PolygonConfigurator<DF> alpha(double alpha) {
        this.geomPolygon.alpha(alpha);
        return this;
    }

    /**
     * Sets the outline stroke width in pixels.
     *
     * @param width the stroke width in pixels
     * @return this {@code PolygonConfigurator} instance for fluid method chaining
     */
    public PolygonConfigurator<DF> lineWidth(double width) {
        this.geomPolygon.lineWidth(width);
        return this;
    }

    /**
     * Controls whether the polygon is filled ({@code true}, the default) or
     * drawn as a closed outline only.
     *
     * @param filled {@code false} to stroke just the outline
     * @return this {@code PolygonConfigurator} instance for fluid method chaining
     */
    public PolygonConfigurator<DF> filled(boolean filled) {
        this.geomPolygon.filled(filled);
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        plot.registerInternalLayer(this.geomPolygon);
    }
}
