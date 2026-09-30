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

import org.jtaccuino.gog.render.DrawSurface;

/**
 * The empty geometry: a layer that draws nothing.
 * <p>
 * Registered through {@code Geoms.none()}, it expresses an explicitly blank
 * cell in a {@code PlotMatrix} — distinct from leaving the cell unpopulated —
 * while keeping the rest of the descriptor spec (aes, theme, options) intact.
 * A cell whose layers are all {@code GeomNone} (or that carries no layers at
 * all) is skipped by the matrix renderer, which reserves its grid slot without
 * drawing a panel.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public final class GeomNone<DF> implements Layer<DF> {

    /** Creates a {@code GeomNone} blank layer. */
    public GeomNone() {
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        // Intentionally empty: Geoms.none() draws no geometry and leaves the
        // cell's grid slot blank.
    }
}
