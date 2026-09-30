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
import java.util.function.DoubleBinaryOperator;

/**
 * Configurator for the {@code Geoms.function3d()} geometry layer: evaluates a
 * user-supplied {@code f(x, y) = z} over a regular grid via
 * {@code Stats.function3d()} and renders the result as a surface.
 * <p>
 * The evaluation domain and resolution are configured with
 * {@link #xlim(double, double)}, {@link #ylim(double, double)} and
 * {@link #n(int)}; {@link #trim(boolean)} controls whether points outside the
 * domain are dropped.
 *
 * @param <DF> the data-frame type
 */
public class Function3dConfigurator<DF>
        extends BasePolygon3dConfigurator<DF, GeomSurface3d<DF>, Function3dConfigurator<DF>> {

    private final DoubleBinaryOperator fun;
    private double[] xlim;
    private double[] ylim;
    private Integer n;
    private Boolean trim;

    /**
     * Creates a configurator for the given 3D function-surface geometry layer.
     *
     * @param geom the underlying surface geometry layer
     * @param spec the specification to configure
     * @param fun  the surface function {@code (x, y) -> z}
     */
    public Function3dConfigurator(GeomSurface3d<DF> geom, Polygon3dSpec spec, DoubleBinaryOperator fun) {
        super(geom, spec);
        this.fun = fun;
    }

    /**
     * {@return this} Sets the x range the function is evaluated over.
     *
     * @param min the lower x bound
     * @param max the upper x bound
     */
    public Function3dConfigurator<DF> xlim(double min, double max) {
        this.xlim = new double[] {min, max};
        return self();
    }

    /**
     * {@return this} Sets the y range the function is evaluated over.
     *
     * @param min the lower y bound
     * @param max the upper y bound
     */
    public Function3dConfigurator<DF> ylim(double min, double max) {
        this.ylim = new double[] {min, max};
        return self();
    }

    /**
     * {@return this} Sets the grid resolution per dimension.
     *
     * @param n the number of evaluation points per axis (default 40)
     */
    public Function3dConfigurator<DF> n(int n) { this.n = n; return self(); }

    /**
     * {@return this} Sets whether points outside the configured range are
     * trimmed from the grid.
     *
     * @param trim {@code true} (default) to drop out-of-range points
     */
    public Function3dConfigurator<DF> trim(boolean trim) { this.trim = trim; return self(); }

    @Override
    protected Map<String, Object> statParams() {
        var m = super.statParams();
        m.put("fun", fun);
        putIfNotNull(m, "xlim", xlim);
        putIfNotNull(m, "ylim", ylim);
        putIfNotNull(m, "n", n);
        putIfNotNull(m, "trim", trim);
        return m;
    }
}
