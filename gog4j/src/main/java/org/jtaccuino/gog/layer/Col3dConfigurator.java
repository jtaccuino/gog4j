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
 * Configurator for the {@code Geoms.col3d()} geometry layer: rectangular 3-D
 * columns from a {@code zmin} base up to each row's {@code z}.
 *
 * @param <DF> the data-frame type
 */
public class Col3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomCol3d<DF>, Col3dConfigurator<DF>> {

    private Double width;
    private CubeFace[] faces;
    private Double zmin;

    /**
     * Creates a configurator for the given 3D column geometry layer.
     *
     * @param geom the underlying column geometry layer
     * @param spec the specification to configure
     */
    public Col3dConfigurator(GeomCol3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the column width as a fraction of the grid spacing.
     *
     * @param width the width factor (default 1.0; use less for gaps)
     */
    public Col3dConfigurator<DF> width(double width) { this.width = width; return self(); }

    /**
     * {@return this} Sets which column faces are rendered.
     *
     * @param faces the cube faces to render; empty (default) renders all faces
     */
    public Col3dConfigurator<DF> faces(CubeFace... faces) { this.faces = faces; return self(); }

    /**
     * {@return this} Sets a uniform base level for every column, overriding
     * any {@code zmin} aesthetic.
     *
     * @param zmin the base z value, or {@code null} for the aesthetic/default 0
     */
    public Col3dConfigurator<DF> zmin(Double zmin) { this.zmin = zmin; return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "width", width);
        putIfNotNull(m, "faces", faces);
        putIfNotNull(m, "zmin", zmin);
        return m;
    }
}
