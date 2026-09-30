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

import java.util.Map;
import org.jtaccuino.gog.coord.CubeFace;

/**
 * Configurator for the {@code Geoms.voxel3d()} geometry layer: fixed-size 3-D
 * cubes centered on sparse point coordinates.
 *
 * @param <DF> the data-frame type
 */
public class Voxel3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomVoxel3d<DF>, Voxel3dConfigurator<DF>> {

    private Double width;
    private CubeFace[] faces;

    /**
     * Creates a configurator for the given 3D voxel geometry layer.
     *
     * @param geom the underlying voxel geometry layer
     * @param spec the specification to configure
     */
    public Voxel3dConfigurator(GeomVoxel3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the voxel width as a fraction of the grid spacing.
     *
     * @param width the width factor (default 1.0)
     */
    public Voxel3dConfigurator<DF> width(double width) { this.width = width; return self(); }

    /**
     * {@return this} Sets which voxel faces are rendered.
     *
     * @param faces the cube faces to render; empty (default) renders all faces
     */
    public Voxel3dConfigurator<DF> faces(CubeFace... faces) { this.faces = faces; return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "width", width);
        putIfNotNull(m, "faces", faces);
        return m;
    }
}
