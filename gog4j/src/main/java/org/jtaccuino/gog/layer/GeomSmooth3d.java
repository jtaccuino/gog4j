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
 * The {@link org.jtaccuino.gog.Geoms#smooth3d()} geometry layer: draws the
 * polygon panels (and optional data points and residual segments) emitted by
 * {@link org.jtaccuino.gog.stat.Stats#smooth3d()}. The stat output already
 * carries pre-polygonized rows with a {@code prim} column, so the base
 * {@link GeomPolygon3d} identity polygonization is used unchanged; this layer
 * only supplies the default stat and the {@link Polygon3dSpec} styling for the
 * point/residual overlays.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomSmooth3d<DF> extends GeomPolygon3d<DF> {

    /**
     * Creates a smooth geometry with the {@link GeomPolygon3d} defaults of
     * {@link org.jtaccuino.gog.Geoms#smooth3d()} (a {@code grey50} fill and a
     * {@code grey50} stroke).
     */
    public GeomSmooth3d() {
        super();
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.smooth3d();
    }
}
