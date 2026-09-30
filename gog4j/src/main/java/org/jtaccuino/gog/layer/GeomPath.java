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

import javafx.scene.paint.Color;

/**
 * {@code Geoms.path()} geometry: a multi-segment polyline drawn in row order,
 * connecting consecutive data points without sorting by the independent axis.
 * <p>
 * Unlike {@link GeomLine} (which sorts by x and is meant for single-valued
 * functions of x), a path traces the actual sequence of points and is the
 * geometry of choice for trajectories, cycles, and arbitrary polylines.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomPath<DF> extends LineGeom<DF> {

    /**
     * Constructs a {@code Geoms.path()} geometry.
     */
    public GeomPath() {
    }

    @Override
    protected boolean sortByX() {
        return false;
    }

    @Override
    public GeomPath<DF> color(Color c) {
        super.color(c);
        return this;
    }

    @Override
    public GeomPath<DF> width(double w) {
        super.width(w);
        return this;
    }
}
