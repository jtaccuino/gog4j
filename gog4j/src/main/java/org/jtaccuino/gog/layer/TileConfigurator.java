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

import org.jtaccuino.gog.ConfigTarget;

/**
 * Declarative builder for a {@code Geoms.tile()} heatmap layer: configures the
 * colour ramp and tile-border width, optionally places the tiles on a cube
 * face of a 3-D scene via {@link #position(PositionAdjust)}, then registers
 * the layer into a {@link ConfigTarget}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class TileConfigurator<DF> extends BasePrimitive3dConfigurator<DF, GeomTile<DF>, TileConfigurator<DF>> {

    /**
     * Constructs a new {@code TileConfigurator} wrapping a {@link GeomTile}.
     *
     * @param geomTile the target heatmap tile geometry layer
     */
    public TileConfigurator(GeomTile<DF> geomTile) {
        super(geomTile);
    }

    /**
     * Sets the color ramp name (viridis, plasma, blues, greens, reds, greys).
     *
     * @param cmapName the colour ramp name
     * @return this {@code TileConfigurator} instance for fluid chaining
     */
    public TileConfigurator<DF> cmap(String cmapName) {
        this.geom().cmap(cmapName);
        return this;
    }

    /**
     * Sets the stroke width between tiles.
     *
     * @param lineWidth the tile border stroke width in pixels
     * @return this {@code TileConfigurator} instance for fluid chaining
     */
    public TileConfigurator<DF> lineWidth(double lineWidth) {
        this.geom().lineWidth(lineWidth);
        return this;
    }

    /**
     * Sets the layer's position adjustment — e.g. cube-face placement via
     * {@link Positions#positionOnFace} so the heatmap lies flat on a face of a
     * 3-D scene.
     *
     * @param position the {@link PositionAdjust} to apply
     * @return this {@code TileConfigurator} instance for fluid chaining
     */
    public TileConfigurator<DF> position(PositionAdjust position) {
        this.geom().position(position);
        return this;
    }
}
