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
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;

/**
 * Configurator for the {@code Geoms.density3d()} geometry layer: estimates a
 * 2-D Gaussian kernel density over a point grid via
 * {@code Stats.density3d()} and renders it as a surface.
 *
 * @param <DF> the data-frame type
 */
public class Density3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomSurface3d<DF>, Density3dConfigurator<DF>> {

    private double[] bandwidth;
    private Double adjust;
    private Integer n;
    private Boolean trim;
    private Double pad;
    private Double minNdensity;
    private GridGeometry grid;
    private GridDirection direction;
    private double[] xlim;
    private double[] ylim;

    /**
     * Creates a configurator for the given 3D density-surface geometry layer.
     *
     * @param geom the underlying surface geometry layer
     * @param spec the specification to configure
     */
    public Density3dConfigurator(GeomSurface3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the smoothing bandwidth in data units, as a single
     * shared value or a two-element array {@code [h_x, h_y]} (default: the
     * data-driven bandwidth).
     *
     * @param bandwidth the bandwidth value(s)
     */
    public Density3dConfigurator<DF> bandwidth(double... bandwidth) { this.bandwidth = bandwidth; return self(); }

    /**
     * {@return this} Multiplies the bandwidth by a scale factor.
     *
     * @param adjust the bandwidth multiplier (default 1.0)
     */
    public Density3dConfigurator<DF> adjust(double adjust) { this.adjust = adjust; return self(); }

    /**
     * {@return this} Sets the grid resolution per dimension.
     *
     * @param n the number of grid points per axis (default 40)
     */
    public Density3dConfigurator<DF> n(int n) { this.n = n; return self(); }

    /**
     * {@return this} Sets whether points outside the padded data range are
     * trimmed from the grid.
     *
     * @param trim {@code true} (default) to drop out-of-range points
     */
    public Density3dConfigurator<DF> trim(boolean trim) { this.trim = trim; return self(); }

    /**
     * {@return this} Sets the padding fraction added to the data range when the
     * grid limits are derived from the data.
     *
     * @param pad the padding fraction (default 0.1)
     */
    public Density3dConfigurator<DF> pad(double pad) { this.pad = pad; return self(); }

    /**
     * {@return this} Sets the minimum normalized density to render, dropping
     * grid points whose density falls below it.
     *
     * @param minNdensity the minimum {@code density / max(density)} (default 0.0)
     */
    public Density3dConfigurator<DF> minNdensity(double minNdensity) { this.minNdensity = minNdensity; return self(); }

    /**
     * {@return this} Sets the grid geometry for the density surface.
     *
     * @param grid the {@link GridGeometry} (default {@link GridGeometry#RECTANGLE})
     */
    public Density3dConfigurator<DF> grid(GridGeometry grid) { this.grid = grid; return self(); }

    /**
     * {@return this} Sets the ridge/grid direction.
     *
     * @param direction the {@link GridDirection} ({@link GridDirection#Y} by default)
     */
    public Density3dConfigurator<DF> direction(GridDirection direction) { this.direction = direction; return self(); }

    /**
     * {@return this} Sets the x range of the density grid, overriding the
     * padded data range.
     *
     * @param min the lower x bound
     * @param max the upper x bound
     */
    public Density3dConfigurator<DF> xlim(double min, double max) {
        this.xlim = new double[] {min, max};
        return self();
    }

    /**
     * {@return this} Sets the y range of the density grid, overriding the
     * padded data range.
     *
     * @param min the lower y bound
     * @param max the upper y bound
     */
    public Density3dConfigurator<DF> ylim(double min, double max) {
        this.ylim = new double[] {min, max};
        return self();
    }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "h", bandwidth);
        putIfNotNull(m, "adjust", adjust);
        putIfNotNull(m, "n", n);
        putIfNotNull(m, "trim", trim);
        putIfNotNull(m, "pad", pad);
        putIfNotNull(m, "minNdensity", minNdensity);
        putIfNotNull(m, "grid", grid);
        putIfNotNull(m, "direction", direction);
        putIfNotNull(m, "xlim", xlim);
        putIfNotNull(m, "ylim", ylim);
        return m;
    }
}
