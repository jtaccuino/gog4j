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
 * Configurator for the {@code Geoms.bar3d()} geometry layer: 3-D bars from
 * 2-D counting or binning.
 *
 * @param <DF> the data-frame type
 */
public class Bar3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomBar3d<DF>, Bar3dConfigurator<DF>> {

    private int[] bins;
    private double[] binwidth;
    private Boolean drop;
    private double[] width;
    private CubeFace[] faces;

    /**
     * Creates a configurator for the given 3D bar geometry layer.
     *
     * @param geom the underlying bar geometry layer
     * @param spec the specification to configure
     */
    public Bar3dConfigurator(GeomBar3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the number of bins in each dimension.
     *
     * @param bins a single bin count used for both axes, or a two-element array
     *             {@code [bins_x, bins_y]} (default 10)
     */
    public Bar3dConfigurator<DF> bins(int... bins) { this.bins = bins; return self(); }

    /**
     * {@return this} Sets the bin width in each dimension, overriding {@code bins}.
     *
     * @param binwidth a single width, or a two-element array {@code [w_x, w_y]}
     */
    public Bar3dConfigurator<DF> binwidth(double... binwidth) { this.binwidth = binwidth; return self(); }

    /**
     * {@return this} Sets whether empty bins/combinations are skipped.
     *
     * @param drop {@code true} (default) to skip empty bins, {@code false} to
     *             render them as zero-height columns
     */
    public Bar3dConfigurator<DF> drop(boolean drop) { this.drop = drop; return self(); }

    /**
     * {@return this} Sets the column width as a fraction of the bin spacing.
     *
     * @param width a single factor, or a two-element array {@code [w_x, w_y]}
     *              (default 1.0)
     */
    public Bar3dConfigurator<DF> width(double... width) { this.width = width; return self(); }

    /**
     * {@return this} Sets which bar faces are rendered.
     *
     * @param faces the cube faces to render; empty (default) renders all faces
     */
    public Bar3dConfigurator<DF> faces(CubeFace... faces) { this.faces = faces; return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "bins", bins);
        putIfNotNull(m, "binwidth", binwidth);
        putIfNotNull(m, "drop", drop);
        putIfNotNull(m, "width", width);
        putIfNotNull(m, "faces", faces);
        return m;
    }
}
