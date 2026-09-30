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

import org.jtaccuino.gog.geometry.GridDirection;

/**
 * Configurator for the {@code Geoms.ridgeline3d()} geometry layer, which
 * converts point grids into closed ridge polygons.
 *
 * @param <DF> the data-frame type
 */
public class Ridgeline3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomRidgeline3d<DF>, Ridgeline3dConfigurator<DF>> {

    /**
     * Creates a configurator for the given 3D ridgeline geometry layer.
     *
     * @param geom the underlying ridgeline geometry layer
     * @param spec the specification to configure
     */
    public Ridgeline3dConfigurator(GeomRidgeline3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the ridge direction.
     *
     * @param direction the {@link GridDirection} ({@link GridDirection#X} by default)
     */
    public Ridgeline3dConfigurator<DF> direction(GridDirection direction) { spec().direction(direction); return self(); }

    /**
     * {@return this} Sets the base z the ridges span down to.
     *
     * @param base the base z value, or {@code null} for the data minimum
     */
    public Ridgeline3dConfigurator<DF> base(Double base) { spec().base(base); return self(); }
}
