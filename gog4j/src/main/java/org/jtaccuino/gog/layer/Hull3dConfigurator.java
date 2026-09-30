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
import org.jtaccuino.gog.geometry.HullMethod;

/**
 * Configurator for the {@code Geoms.hull3d()} geometry layer: a convex or
 * alpha surface hull over a 3-D point cloud.
 *
 * @param <DF> the data-frame type
 */
public class Hull3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomHull3d<DF>, Hull3dConfigurator<DF>> {

    private HullMethod method;
    private Double radius;
    private Boolean singular;

    /**
     * Creates a configurator for the given 3D hull geometry layer.
     *
     * @param geom the underlying hull geometry layer
     * @param spec the specification to configure
     */
    public Hull3dConfigurator(GeomHull3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the hull triangulation method.
     *
     * @param method the {@link HullMethod} ({@link HullMethod#CONVEX} by default)
     */
    public Hull3dConfigurator<DF> method(HullMethod method) { this.method = method; return self(); }

    /**
     * {@return this} Sets the alpha radius for {@link HullMethod#ALPHA}.
     *
     * @param radius the alpha radius in data units, or {@code null} for the
     *               data-scale heuristic
     */
    public Hull3dConfigurator<DF> radius(Double radius) { this.radius = radius; return self(); }

    /**
     * {@return this} Sets whether singular faces are included for
     * {@link HullMethod#ALPHA}.
     *
     * @param singular {@code true} to include dangling singular faces
     */
    public Hull3dConfigurator<DF> singular(boolean singular) { this.singular = singular; return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "method", method);
        putIfNotNull(m, "radius", radius);
        putIfNotNull(m, "singular", singular);
        return m;
    }
}
