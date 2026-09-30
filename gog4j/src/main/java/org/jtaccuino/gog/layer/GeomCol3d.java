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

import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.Stats;

/**
 * The {@link org.jtaccuino.gog.Geoms#col3d()} geometry layer: renders the
 * rectangular 3-D columns emitted by
 * {@link org.jtaccuino.gog.stat.Stats#col3d()} through the shared
 * {@link GeomPolygon3d} renderer. Solid geometries default to back-face
 * culling.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomCol3d<DF> extends GeomPolygon3d<DF> {

    /**
     * Creates a column geometry with solid defaults
     * ({@code cullBackfaces = true}) and a default light so the front side
     * faces shade at different brightnesses.
     */
    public GeomCol3d() {
        super(Polygon3dSpec.solid());
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.col3d();
    }

    @Override
    public Layer.Bounds expandDomain(Layer.Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        return expandSolidDomain(bounds, ctx);
    }
}
