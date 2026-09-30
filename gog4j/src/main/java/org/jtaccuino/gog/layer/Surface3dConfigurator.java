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

import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.SurfaceMethod;

/**
 * Configurator for the {@code Geoms.surface3d()} geometry layer, which
 * tessellates point grids into polygon tiles. Also backs
 * {@code Geoms.function3d()} and {@code Geoms.density3d()}, which run their
 * own stat over the surface geometry.
 *
 * @param <DF> the data-frame type
 */
public class Surface3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomSurface3d<DF>, Surface3dConfigurator<DF>> {

    /**
     * Creates a configurator for the given 3D surface geometry layer.
     *
     * @param geom the underlying surface geometry layer
     * @param spec the specification to configure
     */
    public Surface3dConfigurator(GeomSurface3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the surface tessellation method.
     *
     * @param method the {@link SurfaceMethod} ({@link SurfaceMethod#AUTO} by default)
     */
    public Surface3dConfigurator<DF> method(SurfaceMethod method) { spec().method(method); return self(); }

    /**
     * {@return this} Sets the grid geometry for regular-grid tessellation.
     *
     * @param grid the {@link GridGeometry} (default {@link GridGeometry#RECTANGLE})
     */
    public Surface3dConfigurator<DF> grid(GridGeometry grid) { spec().grid(grid); return self(); }
}
