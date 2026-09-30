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
import org.jtaccuino.gog.stat.Stat;

/**
 * A geometry layer of a plot.
 * <p>
 * Implementations (such as {@link GeomPoint}, {@link GeomLine}, {@link GeomBar}, {@link GeomArea}, {@link GeomSmooth})
 * draw onto a backend-neutral {@link DrawSurface} and perform hover hit-testing.
 * Because they never touch a concrete canvas, the same layer code produces both
 * the on-screen rendering and the vector export.
 * <p>
 * A layer is driven by a two-phase lifecycle. The plot invokes
 * {@link #prepare(PlotContext)} once per render pass over the unpartitioned
 * master data, then invokes {@link #render(DrawSurface, PanelContext, LayerData)}
 * once per facet panel and {@link #locate(PanelContext, LayerData, double, double)}
 * for mouse hover hit-testing. Any state computed once over the global data in
 * {@code prepare} is returned as a {@link LayerData} and threaded through every
 * subsequent call, so layers never recompute palettes or model fits per panel.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public interface Layer<DF> {

    /**
     * A continuous axis domain — the {@code [min, max]} extent of one axis, or
     * of both axes together. Used by {@link Layer}s to report the domain they
     * would like to be drawn on.
     *
     * @param xMin the minimum x coordinate of the domain
     * @param xMax the maximum x coordinate of the domain
     * @param yMin the minimum y coordinate of the domain
     * @param yMax the maximum y coordinate of the domain
     */
    static record Bounds(double xMin, double xMax, double yMin, double yMax) {
    }

    /**
     * Prepares the layer's per-render state over the global data. Called once
     * per render pass, before any panel is drawn. Layers that map no expensive
     * global computation return {@link LayerData#NONE}; layers that fit models
     * or build per-column palettes over the global data return an immutable
     * {@link LayerData} holding the results, which is then threaded through
     * {@link #render} and {@link #locate}.
     *
     * @param ctx the plot-scoped context
     * @return the prepared state for this render pass, never {@code null}
     */
    default LayerData prepare(PlotContext<DF> ctx) {
        return LayerData.NONE;
    }

    /**
     * Renders the geometry layer onto the specified {@link DrawSurface}.
     *
     * @param gc         the target drawing surface
     * @param ctx        the panel-scoped context (partition data and scales)
     * @param data       the prepared state from {@link #prepare}, possibly
     *                   {@link LayerData#NONE}
     */
    void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data);

    /**
     * Performs hit-testing at specified canvas mouse coordinates {@code (mx, my)} for interactive tooltips.
     *
     * @param ctx  the panel-scoped context
     * @param data prepared state from {@link #prepare}
     * @param mx   mouse X coordinate in canvas pixels
     * @param my   mouse Y coordinate in canvas pixels
     * @return formatted tooltip description text if the mouse intersects a geometry element, or {@code null} otherwise
     */
    default String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        return null;
    }

    /**
     * Indicates whether this geometry can lay its elements out cumulatively
     * (stacking or dodging along the baseline) — {@code true} for bar and area
     * geoms, {@code false} for points, lines, and the like.
     * <p>
     * Lets aggregation code in the plot pipeline behave polymorphically instead
     * of checking concrete geom types.
     *
     * @return {@code true} if this layer takes part in baseline stacking/dodging
     */
    default boolean stackable() {
        return false;
    }

    /**
     * The layer's active position adjustment mode (see {@link Position}).
     * Only meaningful for layers that are {@link #stackable()}; other layers
     * always report {@link Position#IDENTITY}.
     *
     * @return the current {@link Position}, never {@code null}
     */
    default Position position() {
        return Position.IDENTITY;
    }

    /**
     * The layer's full parameterised position adjustment — the gog4j
     * counterpart of the per-layer {@code position}. Stackable geoms
     * report their configured {@link PositionAdjust}; face-placement geoms
     * ({@code positionOnFace()}) report their {@link PositionAdjust.PositionOnFace};
     * everything else falls back to {@link Position#IDENTITY}.
     * <p>
     * The plot consults this when deciding how to transform a layer's marks
     * before drawing, so a layer can be handed any full adjustment through its
     * configurator regardless of whether it is stackable.
     *
     * @return the effective {@link PositionAdjust}, never {@code null}
     */
    default PositionAdjust positionAdjust() {
        return Position.IDENTITY;
    }

    /**
     * Whether this layer wants the scale's default {@code expansion(mult = 0.05)}
     * headroom applied to the continuous axis domain. Layers that must sit flush
     * with the panel edges (e.g. {@code Geoms.tile()} heatmaps) opt out, so the
     * plot folds their bounds without the default padding.
     *
     * @return {@code true} to keep the default scale expansion, {@code false} to
     *         skip it
     */
    default boolean wantsDefaultExpansion() {
        return true;
    }

    /**
     * Gives the geometry a chance to widen the continuous axis domain it is
     * drawn on, so the finished figure reflects the layer's layout rather than
     * the raw data range. Stackable bars and areas push a zero baseline into
     * view and extend the axis to cumulative group tops; tiles nudge the axes
     * out by half a cell so the outer tiles are not clipped.
     * <p>
     * The geometry owns its axis rules here, so the plot never has to reason
     * about concrete geom types — it only asks every layer for its preferred
     * {@link Bounds} and folds the union.
     *
     * @param bounds    the raw data domain (after any explicit coordinate limits)
     * @param ctx       the plot-scoped context
     * @param xDiscrete whether the x axis is categorical
     * @param yDiscrete whether the y axis is categorical
     * @return the domain this layer wants to be drawn on; return {@code bounds}
     *         unchanged when the layer needs no expansion
     */
    default Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                                boolean xDiscrete, boolean yDiscrete) {
        return bounds;
    }

    /**
     * A rough count of the shapes this layer will draw, used by backends to
     * decide how to handle it — a vector exporter turns a scatter layer of
     * hundreds of thousands of points into an embedded raster rather than emit
     * an element per point.
     * <p>
     * The default of {@code 0} means "cheap, or unknown"; layers that draw one
     * shape per row should report the row count.
     *
     * @param ctx the panel-scoped context
     * @return the estimated number of primitives
     */
    default int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return 0;
    }

    /**
     * The {@link Stat} this geometry runs by default when bound through
     * {@code Plot.layer(...)} without an explicit stat — the gog4j counterpart
     * of the per-geom {@code default statistic}. {@code Geoms.bar()} reports
     * {@code Stats.count()}, {@code Geoms.col()} reports identity (no stat), and
     * so on. Non-stat-consuming geoms keep the default of {@code null}, meaning
     * "draw the raw columns".
     *
     * @return the default stat, or {@code null} for identity/raw rendering
     */
    default Stat<DF> defaultStat() {
        return null;
    }
}
