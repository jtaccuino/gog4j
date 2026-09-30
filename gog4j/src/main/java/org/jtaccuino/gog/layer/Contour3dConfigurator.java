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

/**
 * Configurator for the {@code Geoms.contour3d()} geometry layer, which
 * converts point grids into contour band polygons.
 *
 * @param <DF> the data-frame type
 */
public class Contour3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomContour3d<DF>, Contour3dConfigurator<DF>> {

    /**
     * Creates a configurator for the given 3D contour geometry layer.
     *
     * @param geom the underlying contour geometry layer
     * @param spec the specification to configure
     */
    public Contour3dConfigurator(GeomContour3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the contour bin count.
     *
     * @param bins the number of bins (default 10)
     */
    public Contour3dConfigurator<DF> bins(int bins) { spec().bins(bins); return self(); }

    /**
     * {@return this} Sets the contour bin width.
     *
     * @param binwidth the bin width, or {@code null}
     */
    public Contour3dConfigurator<DF> binwidth(Double binwidth) { spec().binwidth(binwidth); return self(); }

    /**
     * {@return this} Sets the explicit contour break levels.
     *
     * @param breaks the break levels, or {@code null}
     */
    public Contour3dConfigurator<DF> breaks(double... breaks) { spec().breaks(breaks); return self(); }
}
