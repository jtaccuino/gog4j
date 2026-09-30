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
import javafx.scene.paint.Color;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.stat.SmoothDomain;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Configurator for the {@code Geoms.smooth3d()} geometry layer: fits a loess
 * or lm surface and draws the fitted polygon panels with optional data-point
 * and residual-segment overlays.
 *
 * @param <DF> the data-frame type
 */
public class Smooth3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomSmooth3d<DF>, Smooth3dConfigurator<DF>> {

    private SmoothMethod method;
    private Double span;
    private Boolean se;
    private Double level;
    private SmoothDomain domain;
    private GridGeometry grid;
    private GridDirection direction;
    private Integer n;
    private Boolean points;
    private Boolean residuals;

    /**
     * Creates a configurator for the given 3D smooth geometry layer.
     *
     * @param geom the underlying smooth geometry layer
     * @param spec the specification to configure
     */
    public Smooth3dConfigurator(GeomSmooth3d<DF> geom, Polygon3dSpec spec) {
        super(geom, spec);
    }

    /**
     * {@return this} Sets the smoothing method.
     *
     * @param method the {@link SmoothMethod} ({@link SmoothMethod#LOESS} by default)
     */
    public Smooth3dConfigurator<DF> method(SmoothMethod method) { this.method = method; return self(); }

    /**
     * {@return this} Sets the loess span.
     *
     * @param span the smoothing span (default 0.75)
     */
    public Smooth3dConfigurator<DF> span(double span) { this.span = span; return self(); }

    /**
     * {@return this} Sets whether the standard-error confidence surfaces are
     * drawn.
     *
     * @param se {@code true} to draw the {@code level} confidence surfaces
     */
    public Smooth3dConfigurator<DF> se(boolean se) { this.se = se; return self(); }

    /**
     * {@return this} Sets the confidence level for the {@code se} surfaces.
     *
     * @param level the confidence level in (0, 1) (default 0.95)
     */
    public Smooth3dConfigurator<DF> level(double level) { this.level = level; return self(); }

    /**
     * {@return this} Sets the domain the fitted surface covers.
     *
     * @param domain the {@link SmoothDomain} ({@link SmoothDomain#CHULL} by default)
     */
    public Smooth3dConfigurator<DF> domain(SmoothDomain domain) { this.domain = domain; return self(); }

    /**
     * {@return this} Sets the grid geometry for the fitted surface.
     *
     * @param grid the {@link GridGeometry} (default {@link GridGeometry#RECTANGLE})
     */
    public Smooth3dConfigurator<DF> grid(GridGeometry grid) { this.grid = grid; return self(); }

    /**
     * {@return this} Sets the ridge/grid direction.
     *
     * @param direction the {@link GridDirection} ({@link GridDirection#X} by default)
     */
    public Smooth3dConfigurator<DF> direction(GridDirection direction) { this.direction = direction; return self(); }

    /**
     * {@return this} Sets the grid resolution.
     *
     * @param n the number of grid points per axis (default 40)
     */
    public Smooth3dConfigurator<DF> n(int n) { this.n = n; return self(); }

    /**
     * {@return this} Sets whether the observed data points are overlaid.
     *
     * @param points {@code true} to draw the data points
     */
    public Smooth3dConfigurator<DF> points(boolean points) { this.points = points; return self(); }

    /**
     * {@return this} Sets whether residual segments from each data point to the
     * fitted surface are drawn.
     *
     * @param residuals {@code true} to draw the residual segments
     */
    public Smooth3dConfigurator<DF> residuals(boolean residuals) { this.residuals = residuals; return self(); }

    /**
     * {@return this} Sets the colour of the data-point overlay.
     *
     * @param colour the point colour, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> pointColour(Color colour) { spec().pointColour(colour); return self(); }

    /**
     * {@return this} Sets the fill of the data-point overlay.
     *
     * @param fill the point fill, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> pointFill(Color fill) { spec().pointFill(fill); return self(); }

    /**
     * {@return this} Sets the size of the data-point overlay.
     *
     * @param size the point diameter, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> pointSize(double size) { spec().pointSize(size); return self(); }

    /**
     * {@return this} Sets the shape of the data-point overlay.
     *
     * @param shape the point shape, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> pointShape(PointShape shape) { spec().pointShape(shape); return self(); }

    /**
     * {@return this} Sets the opacity of the data-point overlay.
     *
     * @param alpha the point opacity, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> pointAlpha(double alpha) { spec().pointAlpha(alpha); return self(); }

    /**
     * {@return this} Sets the colour of the residual segments.
     *
     * @param colour the segment colour, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> residualColour(Color colour) { spec().residualColour(colour); return self(); }

    /**
     * {@return this} Sets the stroke width of the residual segments.
     *
     * @param linewidth the segment stroke width
     */
    public Smooth3dConfigurator<DF> residualLinewidth(double linewidth) { spec().residualLinewidth(linewidth); return self(); }

    /**
     * {@return this} Sets the opacity of the residual segments.
     *
     * @param alpha the segment opacity, or {@code null} to inherit
     */
    public Smooth3dConfigurator<DF> residualAlpha(double alpha) { spec().residualAlpha(alpha); return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        putIfNotNull(m, "method", method);
        putIfNotNull(m, "span", span);
        putIfNotNull(m, "se", se);
        putIfNotNull(m, "level", level);
        putIfNotNull(m, "domain", domain);
        putIfNotNull(m, "grid", grid);
        putIfNotNull(m, "direction", direction);
        putIfNotNull(m, "n", n);
        putIfNotNull(m, "points", points);
        putIfNotNull(m, "residuals", residuals);
        return m;
    }
}
