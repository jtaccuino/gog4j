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
import org.jtaccuino.gog.render.DrawSurface;

/**
 * {@code Geoms.step()} geometry: a staircase line that joins points by first
 * holding the dependent value constant while advancing the independent axis,
 * then jumping to the next value.
 * <p>
 * Points are sorted by the independent axis so the steps read left-to-right.
 * The staircase steps horizontally first, then vertically (the classic
 * {@code direction = "hv"} of the default) — the classic empirical cumulative /
 * step-plot rendering.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomStep<DF> extends LineGeom<DF> {

    /**
     * Constructs a {@code Geoms.step()} geometry.
     */
    public GeomStep() {
    }

    @Override
    protected boolean sortByX() {
        return true;
    }

    @Override
    protected void stepTo(DrawSurface gc, double prevX, double prevY, double cx, double cy) {
        gc.lineTo(cx, prevY);
        gc.lineTo(cx, cy);
    }

    @Override
    public GeomStep<DF> color(Color c) {
        super.color(c);
        return this;
    }

    @Override
    public GeomStep<DF> width(double w) {
        super.width(w);
        return this;
    }
}
