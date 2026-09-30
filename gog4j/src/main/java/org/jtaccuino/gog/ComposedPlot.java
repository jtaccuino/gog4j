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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.jfr.ComposedRenderEvent;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.Theme;

/**
 * A multi-plot composite node arranging several {@link GgFigure}s (plots,
 * matrices, or other compositions) into one aligned grid, drawn into a single
 * SVG or JavaFX {@link Canvas}.
 * <p>
 * Figures are placed row-major by default and every cell shares a uniform panel
 * frame: {@link Plot} leaves are measured with {@link Plot#insetsFor(double, double)}
 * and re-stamped with the shared left/bottom {@link PlotDescriptor#panelInsets(double, double, double, double)}
 * so their panel boxes align at the bottom-left corner (bottom-left alignment semantics)
 * while each plot keeps its own top/right labels and guides.
 * <p>
 * The composition carries its own title, subtitle, caption, and per-figure
 * panel tags, plus an optional shared legend band rendered from a
 * {@link PlotDescriptor#guidesOnly()} copy of a designated source (pair with
 * {@link #collectLegends(boolean)} to suppress each leaf's own guides).
 * <p>
 * {@code ComposedPlot} extends {@link Pane} so it slots into a JavaFX scene
 * graph like any {@code Plot}, re-rendering on resize via
 * {@code layoutChildren() → redraw()}.
 */
public non-sealed class ComposedPlot extends GridFigurePane<ComposedPlot> {

    private final List<GgFigure> leaves = new ArrayList<>();
    private int nrow;
    private int ncol;
    private boolean byrow = true;
    private double[] columnWeights;
    private double[] rowWeights;
    private double hGap;
    private double vGap;
    private boolean alignPanels = true;
    private String[] tags;
    private PlotDescriptor<?> legendSource;
    private boolean collectLegends;
    private RenderMode renderMode = RenderMode.FULL;
    // Pre-built per-leaf Plot copies with scales/stat already prepared, keyed
    // by the original leaf (identity).  At render time only the panel insets
    // (and optionally guides) are re-stamped on the descriptor — no re-prepare.
    private final IdentityHashMap<GgFigure, Plot<?>> alignedLeafPlots =
            new IdentityHashMap<>();
    // Lazily built and prepared legend plot, reused across redraws.
    private Plot<?> cachedLegendPlot;

    /**
     * Creates an empty composition; use {@code Ggplot.composedPlot(...)} or the
     * fluent appenders ({@link #beside(GgFigure)}, {@link #above(GgFigure)}) to
     * fill it.
     *
     * @param figures the figures to compose (may be empty)
     */
    public ComposedPlot(GgFigure... figures) {
        leaves.addAll(List.of(figures));
    }

    /**
     * Appends a figure to the right, arranging the composition as a single row.
     *
     * @param figure the figure to add to the right
     * @return this composition for fluid chaining
     */
    public ComposedPlot beside(GgFigure figure) {
        leaves.add(figure);
        nrow = 1;
        ncol = 0;
        byrow = true;
        return this;
    }

    /**
     * Appends a figure below, arranging the composition as a single column.
     *
     * @param figure the figure to add below
     * @return this composition for fluid chaining
     */
    public ComposedPlot above(GgFigure figure) {
        leaves.add(figure);
        nrow = 0;
        ncol = 1;
        byrow = true;
        return this;
    }

    /**
     * Sets the quality-vs-speed tradeoff propagated to every {@link Plot} leaf
     * of this composition. When a leaf renders to a vector backend
     * ({@code SvgDrawSurface}) the effective mode is always
     * {@link RenderMode#FULL}.
     *
     * @param renderMode the {@link RenderMode} to apply to the leaves
     * @return this composition for fluid chaining
     */
    public ComposedPlot renderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        return this;
    }

    /**
     * The quality-vs-speed tradeoff propagated to every {@link Plot} leaf.
     *
     * @return the configured {@link RenderMode}
     */
    public RenderMode renderMode() {
        return renderMode;
    }

    /**
     * Sets the number of grid rows. The column count is derived from the figure
     * count when {@code ncol} is unset. Equivalent to {@link #nrow(int)}.
     *
     * @param rows the row count (positive)
     * @return this composition for fluid chaining
     */
    public ComposedPlot rows(int rows) {
        return nrow(rows);
    }

    /**
     * Sets the number of grid columns. The row count is derived from the figure
     * count when {@code nrow} is unset. Equivalent to {@link #ncol(int)}.
     *
     * @param cols the column count (positive)
     * @return this composition for fluid chaining
     */
    public ComposedPlot cols(int cols) {
        return ncol(cols);
    }

    /**
     * Sets the number of grid rows, mirroring the {@code nrow}.
     *
     * @param rows the row count (positive)
     * @return this composition for fluid chaining
     */
    public ComposedPlot nrow(int rows) {
        this.nrow = rows;
        return this;
    }

    /**
     * Sets the number of grid columns, mirroring the {@code ncol}.
     *
     * @param cols the column count (positive)
     * @return this composition for fluid chaining
     */
    public ComposedPlot ncol(int cols) {
        this.ncol = cols;
        return this;
    }

    /**
     * Sets the fill order of the grid: {@code true} (the default) fills
     * row-major, {@code false} column-major.
     *
     * @param byrow {@code true} to fill row-major
     * @return this composition for fluid chaining
     */
    public ComposedPlot byrow(boolean byrow) {
        this.byrow = byrow;
        return this;
    }

    /**
     * Reserves proportional column weights across the grid. Fewer entries than
     * the grid's column count are left-padded with 1.0 (uniform columns).
     *
     * @param weights the column weights
     * @return this composition for fluid chaining
     */
    public ComposedPlot columnWeights(double... weights) {
        this.columnWeights = weights.clone();
        return this;
    }

    /**
     * Reserves proportional row weights across the grid. Fewer entries than
     * the grid's row count are left-padded with 1.0 (uniform rows).
     *
     * @param weights the row weights
     * @return this composition for fluid chaining
     */
    public ComposedPlot rowWeights(double... weights) {
        this.rowWeights = weights.clone();
        return this;
    }

    /**
     * Sets the horizontal and vertical gap between grid cells.
     *
     * @param gap the gap in device-independent pixels
     * @return this composition for fluid chaining
     */
    public ComposedPlot gap(double gap) {
        this.hGap = gap;
        this.vGap = gap;
        return this;
    }

    /**
     * Toggles bottom-left panel alignment for {@link Plot} leaves. When enabled
     * (the default) each plot is measured and re-stamped with a shared
     * left/bottom inset so its panel box aligns with every other panel.
     *
     * @param align {@code true} to align panels
     * @return this composition for fluid chaining
     */
    public ComposedPlot align(boolean align) {
        this.alignPanels = align;
        return this;
    }

    /**
     * Enables per-figure panel tags. Calling with no arguments draws the
     * automatic letters {@code a}, {@code b}, {@code c}, &hellip;; explicit
     * tag text is used when given. Pass an empty array via
     * {@code tags(new String[0])} for the automatic letters.
     *
     * @param tags the tag text for each panel, or none for the automatic letters
     * @return this composition for fluid chaining
     */
    public ComposedPlot tags(String... tags) {
        this.tags = tags.clone();
        return this;
    }

    /**
     * Toggles automatic lettered panel tags ({@code a}, {@code b}, &hellip;).
     *
     * @param on {@code true} to enable automatic tags
     * @return this composition for fluid chaining
     */
    public ComposedPlot tags(boolean on) {
        this.tags = on ? new String[0] : null;
        return this;
    }

    /**
     * Designates the descriptor whose guides render as a single shared legend
     * in the band to the right of the grid. Pair with
     * {@link #collectLegends(boolean)} to also suppress every {@link Plot}
     * leaf's own guides so the composition shows one legend.
     *
     * @param source the descriptor carrying the shared guides
     * @return this composition for fluid chaining
     */
    public ComposedPlot legend(PlotDescriptor<?> source) {
        this.legendSource = source;
        return this;
    }

    /**
     * When a shared legend is set, suppresses the guides of every {@link Plot}
     * leaf (their render copies are stamped {@link Guides#none()}), mirroring
     * {@code PlotMatrix.legendCell}'s hoist-and-dedupe.
     *
     * @param collect {@code true} to suppress per-plot guides
     * @return this composition for fluid chaining
     */
    public ComposedPlot collectLegends(boolean collect) {
        this.collectLegends = collect;
        return this;
    }

    /**
     * The composed figures, in fill order.
     *
     * @return the leaves
     */
    public List<GgFigure> leaves() {
        return List.copyOf(leaves);
    }

    /** Whether every leaf is a standalone 3-D plot, so the grid rows can be
     * sized to the cube's square aspect instead of filling the given height. */
    private boolean is3dGrid() {
        if (leaves.isEmpty()) {
            return false;
        }
        for (GgFigure leaf : leaves) {
            if (!(leaf instanceof Plot<?> p && p.descriptor().coord() instanceof Coord3D)) {
                return false;
            }
        }
        return true;
    }

    // ─── GgFigure / Canvas / redraw ───────────────────────────────────

    /**
     * Applies the given theme to the whole composition: every leaf figure (and
     * its aligned-copy plot), the legend source and any cached legend plot,
     * then re-resolves and repaints this figure itself.
     *
     * @param theme the theme to apply
     */
    @Override
    public void applyTheme(Theme theme) {
        for (GgFigure leaf : leaves) {
            if (leaf instanceof GgFigurePane figure) {
                figure.applyTheme(theme);
            }
        }
        for (var aligned : alignedLeafPlots.values()) {
            aligned.applyTheme(theme);
        }
        if (legendSource != null) {
            legendSource.theme(theme);
        }
        if (cachedLegendPlot != null) {
            cachedLegendPlot.applyTheme(theme);
        }
        super.applyTheme(theme);
    }

    /**
     * Pre-builds and prepares all leaf {@code Plot}s (and the legend plot) on
     * the calling thread (typically the sampler's loader pool) so the first
     * {@link #layoutChildren() → redraw()} on the JavaFX thread performs only
     * canvas painting with no scale/stat work.
     */
    @Override
    public void prepareAsync() {
        setThemeIfAbsent(composedTheme());
        for (GgFigure leaf : leaves) {
            if (leaf instanceof Plot<?> p) {
                p.prepareAsync();
                // Build and prepare an aligned-copy plot that renderLeaf can
                // reuse across redraws — only panel insets/guides are re-stamped.
                alignedLeafPlots.computeIfAbsent(p, k -> {
                    @SuppressWarnings("unchecked")
                    var copy = ((Plot<?>) k).descriptor().copy();
                    @SuppressWarnings("unchecked")
                    Plot<?> aligned = (Plot<?>) Ggplot.ggplot((PlotDescriptor) copy);
                    return aligned;
                }).prepareAsync();
            } else if (leaf instanceof GgFigurePane gfp) {
                gfp.prepareAsync();
            }
        }
        if (legendSource != null) {
            var legend = legendSource.copy().guidesOnly();
            legend.theme(t -> t.axisLabelZone(0));
            @SuppressWarnings("unchecked")
            Plot<?> lp = (Plot<?>) Ggplot.ggplot((PlotDescriptor) legend);
            cachedLegendPlot = lp;
            lp.prepareAsync();
        }
    }

    @Override
    public void renderTo(DrawSurface surface, double width, double height) {
        if (width <= 0 || height <= 0 || leaves.isEmpty()) {
            return;
        }
        var renderStart = System.nanoTime();
        var renderEvt = new ComposedRenderEvent();
        boolean enabled = renderEvt.isEnabled();
        if (enabled) {
            renderEvt.figureCount = leaves.size();
            renderEvt.rows = nrows();
            renderEvt.cols = ncols();
        }
        renderEvt.begin();
        try {
            renderToInternal(surface, width, height);
        } finally {
            renderEvt.end();
            renderEvt.commit();
            recordRenderNanos(renderStart);
        }
    }

    private void renderToInternal(DrawSurface surface, double width, double height) {
        int ncols = ncols();
        int nrows = nrows();
        var theme = composedTheme();
        setThemeIfAbsent(theme);
        double titleH = reservedTitleBand(theme);
        double legendW = legendSource != null ? PlotDescriptor.legendBandWidth(legendSource) : 0.0;
        // Without a collected guide the grid keeps the theme's panel-to-guide
        // gap as its right margin, so the last panel does not run flush against
        // the figure edge; with a legend the band itself provides that space.
        double rightMargin = legendW > 0 ? 0.0 : theme.marginRight();
        double gridW = width - legendW - rightMargin - (ncols - 1) * hGap;
        double gridH = height - titleH - (nrows - 1) * vGap;
        double[] colW = GridMath.proportional(GridMath.padded(columnWeights, ncols), gridW);
        double[] rowH;
        if (is3dGrid()) {
            // A 3D cube renders square: it fills min(cell width, cell height),
            // so a cell that is much taller than wide wastes vertical space
            // around a vertically-centred cube. Size each row to the width of
            // its widest cell (square cells) when they fit in the given height,
            // leaving any surplus as a single band at the bottom instead of
            // scattered through every cell; when the grid is too tall (e.g. a
            // 2x2 arrangement) fall back to filling the available height.
            rowH = new double[nrows];
            for (int r = 0; r < nrows; r++) {
                double w = 0;
                for (int i = 0; i < leaves.size(); i++) {
                    int[] rc = cellOf(i, nrows, ncols);
                    if (rc[0] == r) {
                        w = Math.max(w, colW[rc[1]]);
                    }
                }
                rowH[r] = w;
            }
            double squareTotal = 0;
            for (double h : rowH) {
                squareTotal += h;
            }
            squareTotal += (nrows - 1) * vGap;
            if (squareTotal > gridH) {
                rowH = GridMath.proportional(GridMath.padded(rowWeights, nrows), gridH);
            }
        } else {
            rowH = GridMath.proportional(GridMath.padded(rowWeights, nrows), gridH);
        }
        double[] colX = GridMath.leadingOffsets(colW, hGap);
        double[] rowY = new double[nrows];
        double accY = titleH;
        for (int r = 0; r < nrows; r++) {
            rowY[r] = accY;
            accY += rowH[r] + vGap;
        }

        // Shared bottom-left panel insets across every Plot leaf, measured at
        // its assigned cell size.
        double sharedLeft = 0.0;
        double sharedBottom = 0.0;
        if (alignPanels) {
            for (int i = 0; i < leaves.size(); i++) {
                if (leaves.get(i) instanceof Plot<?> p) {
                    int[] rc = cellOf(i, nrows, ncols);
                    var insets = p.insetsFor(colW[rc[1]], rowH[rc[0]]);
                    sharedLeft = Math.max(sharedLeft, insets.left());
                    sharedBottom = Math.max(sharedBottom, insets.bottom());
                }
            }
        }

        for (int i = 0; i < leaves.size(); i++) {
            int[] rc = cellOf(i, nrows, ncols);
            double cx = colX[rc[1]];
            double cy = rowY[rc[0]];
            surface.save();
            surface.translate(cx, cy);
            try {
                renderLeaf(surface, leaves.get(i), colW[rc[1]], rowH[rc[0]], sharedLeft, sharedBottom);
                // The tag must draw inside the cell's translated frame so each
                // letter lands in that panel's own corner, not the figure's.
                String tag = tagOf(i);
                if (tag != null) {
                    drawTag(surface, tag, theme, theme.panelTagOffset(), theme.panelTagOffset());
                }
            } finally {
                surface.restore();
            }
        }
        if (title != null || subtitle != null) {
            drawMainTitle(surface, width, titleH, theme);
        }
        if (legendSource != null) {
            renderLegend(surface, width, height, legendW);
        }
        if (caption != null) {
            drawCaption(surface, width, height, legendW, theme);
        }
    }

    /**
     * Renders a single leaf, aligning Plot leaves by re-stamping shared insets
     * on a pre-built, already-prepared aligned copy.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderLeaf(DrawSurface surface, GgFigure leaf, double w, double h,
            double sharedLeft, double sharedBottom) {
        if (leaf instanceof Plot<?> p && alignPanels) {
            var insets = p.insetsFor(w, h);
            var aligned = alignedLeafPlots.get(p);
            if (aligned == null) {
                // Not warmed via prepareAsync(); build on the spot.
                aligned = (Plot<?>) Ggplot.ggplot((PlotDescriptor) p.descriptor().copy());
                aligned.renderMode(renderMode);
                aligned.prepareAsync();
                alignedLeafPlots.put(p, aligned);
            }
            var desc = aligned.descriptor();
            double top = insets.top();
            // The plot's top label zone is only needed when it holds a panel
            // tag, a title of its own, or a top-positioned x axis. When none
            // of those apply the zone would render as empty padding between
            // the composed title and the panels, so reclaim it.
            if (tags == null && desc.labs().titleIsEmpty()
                    && insets.top() <= desc.theme().axisLabelZone()) {
                top = 0.0;
            }
            // The right label zone is empty for a normal plot (its y axis sits
            // on the left), so keep it only for a genuinely right-positioned y
            // axis. Reclaiming it lets the panels fill their cells and hug the
            // shared legend instead of leaving a wide empty strip.
            double right = insets.right() <= desc.theme().axisLabelZone() ? 0.0 : insets.right();
            desc.panelInsets(sharedLeft, right, top, sharedBottom);
            if (collectLegends && legendSource != null) {
                desc.guides(Guides.none());
            }
            aligned.renderTo(surface, w, h);
        } else {
            if (leaf instanceof Plot<?> raw) {
                if (!renderMode.equals(raw.renderMode())) {
                    raw.renderMode(renderMode);
                }
            } else if (leaf instanceof PlotMatrix<?> matrix) {
                if (!renderMode.equals(matrix.renderMode())) {
                    matrix.renderMode(renderMode);
                }
            } else if (leaf instanceof ComposedPlot composed) {
                if (!renderMode.equals(composed.renderMode())) {
                    composed.renderMode(renderMode);
                }
            }
            leaf.renderTo(surface, w, h);
        }
    }

    /** Renders the shared legend band from the designated source. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderLegend(DrawSurface surface, double width, double height,
            double legendW) {
        if (cachedLegendPlot == null) {
            var legend = legendSource.copy().guidesOnly();
            legend.renderMode(renderMode);
            // A standalone guide needs no axis label zone; dropping it lets the
            // legend keys start right below the figure's top edge.
            legend.theme(t -> t.axisLabelZone(0));
            cachedLegendPlot = (Plot<?>) Ggplot.ggplot((PlotDescriptor) legend);
        }
        surface.save();
        surface.translate(width - legendW, 0);
        try {
            cachedLegendPlot.renderTo(surface, legendW, height);
        } finally {
            surface.restore();
        }
    }

    /** The tag label for the figure at {@code index}, or {@code null}. */
    private String tagOf(int index) {
        if (tags == null) {
            return null;
        }
        if (tags.length > 0) {
            return index < tags.length ? tags[index] : null;
        }
        return String.valueOf((char) ('a' + index));
    }

    private void drawTag(DrawSurface surface, String text, Theme theme, double x, double y) {
        surface.save();
        surface.setFont(theme.titleFont());
        surface.setFill(theme.titleColor());
        surface.setTextAlign(TextAlignment.LEFT);
        surface.setTextBaseline(VPos.TOP);
        surface.fillText(text, x, y);
        surface.restore();
    }

    /** The theme for the composition's own chrome: the first leaf's, else gray. */
    private Theme composedTheme() {
        for (GgFigure leaf : leaves) {
            if (leaf instanceof Plot<?> p) {
                return p.descriptor().theme();
            }
        }
        return Theme.theme_gray();
    }

    /** The grid column count, derived from the row count or figure count. */
    private int ncols() {
        if (ncol > 0) {
            return ncol;
        }
        if (nrow > 0) {
            return (int) Math.ceil(leaves.size() / (double) nrow);
        }
        return Math.max(1, leaves.size());
    }

    /** The grid row count, derived from the column count or figure count. */
    private int nrows() {
        if (nrow > 0) {
            return nrow;
        }
        if (ncol > 0) {
            return (int) Math.ceil(leaves.size() / (double) ncol);
        }
        return 1;
    }

    /** The {@code (row, col)} cell of figure {@code index} in the fill order. */
    private int[] cellOf(int index, int nrows, int ncols) {
        int r = byrow ? index / ncols : index % nrows;
        int c = byrow ? index % ncols : index / nrows;
        return new int[] {r, c};
    }
}
