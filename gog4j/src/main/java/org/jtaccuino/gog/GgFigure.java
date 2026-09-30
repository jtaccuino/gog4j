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
package org.jtaccuino.gog;

import org.jtaccuino.gog.render.DrawSurface;

/**
 * A renderable grammar-of-graphics figure: anything that can draw itself onto
 * a {@link DrawSurface} within a bounded box.
 * <p>
 * The figure contract is closed to the three library-owned figure types — a
 * single {@link Plot}, a {@link PlotMatrix} of many cells, and a
 * {@link ComposedPlot} arrangement of figures — all hosted on the shared
 * {@link GgFigurePane} FX lifecycle, so exporters (and the generic
 * {@code renderTo} machinery) can treat every figure uniformly: a matrix is
 * just a figure that tiles its children within the box, a composition one that
 * lays out its leaves.
 * <p>
 * A figure must not save or touch the surface's global state beyond what it
 * restores itself: it may translate and clip to the box, but must leave the
 * surface logically where it found it (the caller may continue drawing after
 * this call).
 */
public sealed interface GgFigure permits GgFigurePane {

    /**
     * Draws this figure onto the given surface within the given box.
     *
     * @param surface the draw surface to render onto
     * @param width   the available width in pixels
     * @param height  the available height in pixels
     */
    void renderTo(DrawSurface surface, double width, double height);

    /**
     * Repaints this figure onto its own backing canvas at its current node
     * size, mirroring JavaFX's resize/repaint pulse. Figure types that host a
     * {@code Canvas} (a {@code Plot}, {@code PlotMatrix}, {@code ComposedPlot})
     * re-render it; a figure without a backing canvas has nothing to repaint
     * and keeps its default no-op behaviour.
     */
    default void redraw() {
        // No backing canvas to repaint.
    }

    /**
     * Marks this figure's rendered content as stale and schedules a full
     * repaint, even when the backing canvas already matches the node size.
     * <p>
     * This is the interaction contract: mutate the figure's descriptor state
     * (e.g. {@code PlotDescriptor#coord()}, scales, or aesthetics) and then
     * {@code markDirty()} to signal the change to the next repaint. Figure
     * types that host a {@code Canvas} rebuild it; a figure without a backing
     * canvas keeps its default no-op behaviour.
     */
    default void markDirty() {
        redraw();
    }
}
