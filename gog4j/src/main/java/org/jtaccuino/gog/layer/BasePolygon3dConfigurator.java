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

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatParams;

/**
 * Fluent configurator (builder) for the 3D polygon geometry family
 * ({@code Geoms.polygon3d()}, {@code Geoms.surface3d()},
 * {@code Geoms.ridgeline3d()}, {@code Geoms.contour3d()},
 * {@code Geoms.smooth3d()}).
 * <p>
 * The common parameters configure the geometry's rendering ({@link Polygon3dSpec})
 * and let the caller override the attached {@link Stat} and the layer-local
 * aesthetic mapping. Subclasses add the geometry-specific tessellation and
 * stat parameters as dedicated typed fields, assembling them into the stat's
 * configuration bag in {@link #statParams()}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 * @param <G>  the concrete geometry type
 * @param <S>  the concrete configurator type (for fluid chaining)
 */
public abstract class BasePolygon3dConfigurator<DF, G extends GeomPolygon3d<DF>,
        S extends BasePolygon3dConfigurator<DF, G, S>> implements LayerConfigurator<DF> {

    private final G geom;
    private final Polygon3dSpec spec;
    private boolean dropMissing;
    private Stat<DF> stat;
    private Aes localAes;

    /**
     * Creates a configurator for the given geometry and specification.
     *
     * @param geom the geometry layer to configure
     * @param spec the specification to configure
     */
    protected BasePolygon3dConfigurator(G geom, Polygon3dSpec spec) {
        this.geom = geom;
        this.spec = spec;
        this.geom.setSpec(spec);
    }

    /** {@return this} The concrete configurator for fluid chaining. */
    @SuppressWarnings("unchecked")
    protected S self() {
        return (S) this;
    }

    /** {@return the configured geometry layer} */
    protected G geom() {
        return geom;
    }

    /** {@return the configured specification} */
    protected Polygon3dSpec spec() {
        return spec;
    }

    /**
     * {@return this} Sets a constant fill colour for the polygons.
     *
     * @param color the JavaFX fill {@link Color}
     */
    public S fill(Color color) { this.spec.fill(color); return self(); }

    /**
     * {@return this} Sets a constant stroke colour for the polygons.
     *
     * @param color the JavaFX stroke {@link Color}, or {@code null} to skip
     */
    public S color(Color color) { this.spec.colour(color); return self(); }

    /**
     * {@return this} Sets the polygon stroke width in pixels.
     *
     * @param linewidth the stroke width in pixels
     */
    public S linewidth(double linewidth) { this.spec.linewidth(linewidth); return self(); }

    /**
     * {@return this} Sets the polygon opacity in [0, 1].
     *
     * @param alpha the opacity between 0.0 and 1.0
     */
    public S alpha(double alpha) { this.spec.alpha(alpha); return self(); }

    /**
     * {@return this} Sets the depth-ordering strategy.
     *
     * @param sortMethod the {@link Polygon3dSpec.SortMethod}
     */
    public S sortMethod(Polygon3dSpec.SortMethod sortMethod) { this.spec.sortMethod(sortMethod); return self(); }

    /**
     * {@return this} Sets whether stroke widths and point sizes scale with distance.
     *
     * @param scaleDepth {@code true} to scale by depth (default)
     */
    public S scaleDepth(boolean scaleDepth) { this.spec.scaleDepth(scaleDepth); return self(); }

    /**
     * {@return this} Sets whether back faces are dropped.
     *
     * @param cullBackfaces {@code true} to cull back faces
     */
    public S cullBackfaces(boolean cullBackfaces) { this.spec.cullBackfaces(cullBackfaces); return self(); }

    /**
     * {@return this} Sets whether each polygon is replaced by its convex hull.
     *
     * @param forceConvex {@code true} to draw convex hulls
     */
    public S forceConvex(boolean forceConvex) { this.spec.forceConvex(forceConvex); return self(); }

    /**
     * {@return this} Sets the {@link Light3d} used to shade the polygons.
     *
     * @param light the light source, or {@code null} to disable shading
     */
    public S light(Light3d light) { this.spec.light(light); return self(); }

    /**
     * {@return this} Overrides the statistic this layer runs. Pass
     * {@link org.jtaccuino.gog.stat.Stats#identity()} to draw the raw columns,
     * or any other {@link Stat}.
     *
     * @param stat the statistic to run, or {@code null} for the geometry's default
     */
    public S stat(Stat<DF> stat) { this.stat = stat; return self(); }

    /**
     * {@return this} Sets a layer-local aesthetic mapping, merged over the
     * plot-global {@code aes()}.
     *
     * @param mapping the layer-local {@link Aes}
     */
    public S mapping(Aes mapping) { this.localAes = mapping; return self(); }

    /**
     * {@return this} Sets whether rows with missing coordinates are silently
     * dropped instead of failing the whole layer (the {@code na.rm} stat
     * parameter; default {@code false}).
     *
     * @param dropMissing {@code true} to drop rows with missing coordinates
     */
    public S dropMissing(boolean dropMissing) { this.dropMissing = dropMissing; return self(); }

    /**
     * Assembles the stat's configuration bag from this configurator's dedicated
     * typed fields, in insertion order. Subclasses extend this with their own
     * stat parameters, adding a key only for fields the caller set so the stat's
     * defaults apply otherwise.
     *
     * @return a fresh, modifiable parameter map
     */
    protected Map<String, Object> statParams() {
        var m = new LinkedHashMap<String, Object>();
        if (dropMissing) {
            m.put("na.rm", true);
        }
        return m;
    }

    /**
     * Puts a stat parameter into the configuration bag only when the caller
     * actually set it, so the stat's defaults apply for unset fields.
     *
     * @param m     the parameter map to populate
     * @param key   the parameter key
     * @param value the parameter value, or {@code null} to skip the key
     */
    protected static void putIfNotNull(Map<String, Object> m, String key, Object value) {
        if (value != null) {
            m.put(key, value);
        }
    }

    @Override
    public void configure(ConfigTarget<DF> target) {
        var lp = LayerParams.builder().stat(StatParams.of(statParams())).build();
        target.layer(geom, stat != null ? stat : geom.defaultStat(),
                new PositionAdjust.Identity(), localAes, lp);
    }
}
