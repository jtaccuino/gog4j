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
import java.util.List;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.jfr.MatrixLayoutEvent;
import org.jtaccuino.gog.layer.GeomNone;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.AxisStyle;
import org.jtaccuino.gog.theme.Theme;

/**
 * An {@code m×n} grid of {@link Plot} cells rendering {@link PlotDescriptor}
 * snapshots — the gog4j counterpart of the {@code Facets.grid} applied
 * across independent plots.
 * <p>
 * Each cell receives an independent {@link PlotDescriptor} copy whose
 * {@link PlotOptions} are resolved against the matrix's {@link PlotOptions.OuterLabels}
 * policy at the cell's grid position: under {@code MARGINS} only the outer
 * edges carry tick labels and axis titles; under {@code ALL} every cell
 * labels both axes.
 * <p>
 * Cells are rendered via {@link #renderTo}, which allocates a single
 * {@link Canvas} and orchestrates {@code save / translate / render / restore}
 * per cell on the active {@link DrawSurface}.
 *
 * <pre>{@code
 * Ggplot.ggplot(3, 2)
 *     .cells(d -> d.geoms(Geoms.point()).labs("title"))
 *     .renderTo(surface, 1200, 900);
 * }</pre>
 *
 * @param <DF> the DataFrame type representing each cell's dataset
 */
public non-sealed class PlotMatrix<DF> extends GridFigurePane<PlotMatrix<DF>> {

    private final int rows;
    private final int cols;
    private final List<List<PlotDescriptor<DF>>> cellSpecs;
    private final List<List<Plot<DF>>> cellPlots;
    private final double hGap;
    private final double vGap;
    private final boolean explicitGaps;
    private PlotOptions options;
    private RenderMode renderMode = RenderMode.FULL;
    private PlotDescriptor<DF> base;
    private String[] headers;
    private PlotDescriptor<DF> legendSource;

    private AxisLabels axisLabels = AxisLabels.INTERNAL;
    private double[] colWeights;
    private double[] rowWeights;
    private String[] columnLabels;
    private String xLabel;
    private String yLabel;
    // Lazily built and prepared legend plot, reused across redraws so
    // prepare() + Guides only runs once (instead of rebuilding a fresh Plot
    // from a descriptor copy every time layoutChildren fires).
    private Plot<DF> cachedLegendPlot;

    /**
     * Creates an empty {@code m×n} matrix ready for cell configuration.
     *
     * @param rows the number of rows (positive)
     * @param cols the number of columns (positive)
     */
    public PlotMatrix(int rows, int cols) {
        this(rows, cols, false, 0, 0);
    }

    /**
     * Creates an empty {@code m×n} matrix with explicit inter-cell gaps,
     * overriding the theme-derived defaults.
     *
     * @param rows the number of rows (positive)
     * @param cols the number of columns (positive)
     * @param hGap horizontal gap between cells, in pixels
     * @param vGap vertical gap between cells, in pixels
     */
    public PlotMatrix(int rows, int cols, double hGap, double vGap) {
        this(rows, cols, true, hGap, vGap);
    }

    @SuppressWarnings("this-escape")
    private PlotMatrix(int rows, int cols, boolean explicitGaps, double hGap, double vGap) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows and cols must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.hGap = hGap;
        this.vGap = vGap;
        this.explicitGaps = explicitGaps;
        this.cellSpecs = new ArrayList<>();
        this.cellPlots = new ArrayList<>();
        this.options = PlotOptions.defaults();
        for (int r = 0; r < rows; r++) {
            cellSpecs.add(new ArrayList<>(List.copyOf(List.of())));
            cellPlots.add(new ArrayList<>(List.copyOf(List.of())));
            for (int c = 0; c < cols; c++) {
                cellSpecs.get(r).add(null);
                cellPlots.get(r).add(null);
            }
        }
    }

    static <DF> PlotMatrix<DF> create(int rows, int cols) {
        return new PlotMatrix<>(rows, cols);
    }

    // ─── Cell configuration ───────────────────────────────────────────

    /**
     * Reserves proportional column widths across the grid, one weight per
     * column. Fewer than {@code cols} weights are left-padded with 1.0 so the
     * first columns simply copy their explicit weight.
     *
     * @param weights the column weights
     * @return {@code this}
     */
    public PlotMatrix<DF> widths(double... weights) {
        colWeights = weights.clone();
        return this;
    }

    /**
     * Reserves proportional row heights across the grid, one weight per row.
     * Fewer than {@code rows} weights are left-padded with 1.0 so the first
     * rows simply copy their explicit weight.
     *
     * @param weights the row weights
     * @return {@code this}
     */
    public PlotMatrix<DF> heights(double... weights) {
        rowWeights = weights.clone();
        return this;
    }

    /**
     * Returns the column weights, left-padded to the grid's column count with
     * ones so unset slots count as uniform columns.
     *
     * @return the effective column weights, one per column
     */
    public double[] widths() {
        return GridMath.padded(colWeights, cols);
    }

    /**
     * Returns the row weights, left-padded to the grid's row count with ones
     * so unset slots count as uniform rows.
     *
     * @return the effective row weights, one per row
     */
    public double[] heights() {
        return GridMath.padded(rowWeights, rows);
    }

    /**
     * Names the x and y outer axes ({@code columnLabels} replaces the shared
     * variable-name titles on both outer axes; {@code xlab}/{@code ylab} draw
     * the centered single-axis titles instead).
     *
     * @param labels the outer axis names
     * @return {@code this}
     */
    public PlotMatrix<DF> columnLabels(String... labels) {
        columnLabels = labels.clone();
        return this;
    }

    /**
     * Returns the outer axis labels, or {@code null} when unset (the matrix
     * then falls back to {@link #headers}).
     *
     * @return the axis labels, or {@code null}
     */
    public String[] columnLabels() {
        return columnLabels;
    }

    /**
     * Sets a single x-axis title drawn centered under the grid (phase-A
     * convenience mirroring the reference's {@code xlab}).
     *
     * @param label the x-axis title ({@code null} clears it)
     * @return {@code this}
     */
    public PlotMatrix<DF> xlab(String label) {
        xLabel = label;
        return this;
    }

    /**
     * Returns the single x-axis title, or {@code null} when unset.
     *
     * @return the x-axis title, or {@code null}
     */
    public String xlab() {
        return xLabel;
    }

    /**
     * Sets a single y-axis title drawn rotated beside the left column
     * (phase-A convenience mirroring the reference's {@code ylab}).
     *
     * @param label the y-axis title ({@code null} clears it)
     * @return {@code this}
     */
    public PlotMatrix<DF> ylab(String label) {
        yLabel = label;
        return this;
    }

    /**
     * Returns the single y-axis title, or {@code null} when unset.
     *
     * @return the y-axis title, or {@code null}
     */
    public String ylab() {
        return yLabel;
    }

    /**
     * Sets the axis-label policy for the whole matrix:
     * {@code AxisLabels.SHOW} forces every panel to draw its inner x/y tick
     * labels, {@code AxisLabels.NONE} suppresses them all, and
     * {@code AxisLabels.INTERNAL} (the default) keeps the shared
     * margins-style panels with labels only on the outer columns and rows.
     *
     * @param labels the policy
     * @return {@code this}
     */
    public PlotMatrix<DF> axisLabels(AxisLabels labels) {
        axisLabels = labels;
        return this;
    }

    /**
     * Controls how the per-panel tick labels are revealed across the matrix.
     * <ul>
     *   <li>{@link #SHOW} forces every panel to draw its own x and y tick
     *       labels, even when shared axis labels would otherwise be lifted to
     *       the margins ({@code AxisLabels.SHOW})</li>
     *   <li>{@link #INTERNAL} (the default) labels only the outermost panels,
     *       so the shared margins act as the x/y axes</li>
     *   <li>{@link #NONE} suppresses every inner tick label, leaving only the
     *       shared outer titles</li>
     * </ul>
     */
    public enum AxisLabels {
        /** Every panel draws its own x and y tick labels. */
        SHOW,
        /** Labels only on the outer edges (the generalized-pairs default). */
        INTERNAL,
        /** No tick labels anywhere; only the shared outer titles remain. */
        NONE
    }

    /**
     * Returns the current axis-label policy.
     *
     * @return the current {@link AxisLabels}
     */
    public AxisLabels axisLabels() {
        return axisLabels;
    }

    /**
     * Sets the matrix-level options governing tick labels and axis titles.
     * The default is {@link PlotOptions#defaults()} ({@code MARGINS}).
     *
     * @param options the matrix options
     * @return {@code this}
     */
    public PlotMatrix<DF> options(PlotOptions options) {
        this.options = options;
        return this;
    }

    /**
     * The matrix-level options.
     *
     * @return the current {@link PlotOptions}
     */
    public PlotOptions options() {
        return options;
    }

    /**
     * Sets the quality-vs-speed tradeoff propagated to every cell plot of this
     * matrix. When a cell renders to a vector backend ({@code SvgDrawSurface})
     * the effective mode is always {@link RenderMode#FULL}.
     *
     * @param renderMode the {@link RenderMode} to apply to the cells
     * @return {@code this}
     */
    public PlotMatrix<DF> renderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        return this;
    }

    /**
     * The quality-vs-speed tradeoff propagated to every cell plot.
     *
     * @return the configured {@link RenderMode}
     */
    public RenderMode renderMode() {
        return renderMode;
    }

    /**
     * The number of rows.
     *
     * @return the row count
     */
    public int numRows() {
        return rows;
    }

    /**
     * The number of columns.
     *
     * @return the column count
     */
    public int numCols() {
        return cols;
    }

    /**
     * Populates the matrix cells row-major with the given descriptors.
     * Exactly {@code rows × cols} descriptors must be provided.
     *
     * @param descriptors the cell descriptors, row-major
     * @return this matrix for fluid chaining
     * @throws IllegalArgumentException if the size doesn't match {@code rows × cols}
     */
    @SafeVarargs
    public final PlotMatrix<DF> cells(PlotDescriptor<DF>... descriptors) {
        if (descriptors.length != rows * cols) {
            throw new IllegalArgumentException(
                    "Expected " + (rows * cols) + " cells, got " + descriptors.length);
        }
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cellSpecs.get(r).set(c, descriptors[idx++]);
                cellPlots.get(r).set(c, null);
            }
        }
        return this;
    }

    /**
     * Sets the base descriptor from which cell copies are derived.
     * The consumer form of {@link #cells(CellConfigurator...)} copies this
     * base and passes each copy to the consumer. Must be called before the
     * consumer form of {@code cells}.
     *
     * @param base the base descriptor (carries data, extractor, aes)
     * @return this matrix for fluid chaining
     */
    public PlotMatrix<DF> base(PlotDescriptor<DF> base) {
        this.base = base;
        return this;
    }

    /**
     * Populates the matrix cells row-major by copying the
     * {@linkplain #base(PlotDescriptor) base descriptor} and invoking the
     * given consumers. The consumers must supply exactly {@code rows × cols}
     * entries. The base descriptor's {@link PlotOptions} are resolved for
     * each cell's grid position before the consumer runs.
     *
     * @param configurators the cell configurators, row-major
     * @return this matrix for fluid chaining
     */
    @SafeVarargs
    public final PlotMatrix<DF> cells(CellConfigurator<DF, ?>... configurators) {
        if (configurators.length != rows * cols) {
            throw new IllegalArgumentException(
                    "Expected " + (rows * cols) + " cells, got " + configurators.length);
        }
        if (base == null) {
            throw new IllegalStateException("Call base(PlotDescriptor) before cells(CellConfigurator...)");
        }
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var copy = base.copy();
                copy.options(resolvedOptions(r, c));
                configurators[idx++].configure(copy, r, c);
                cellSpecs.get(r).set(c, copy);
                cellPlots.get(r).set(c, null);
            }
        }
        return this;
    }

    // ─── Pair-sugar configuration ─────────────────────────────────────

    /**
     * Names the row and column variables, mirroring a generalized pairs plot
     * headers. Used by {@link #diag(VarConfigurator)}, {@link #lower(PairConfigurator)}
     * and {@link #upper(PairConfigurator)} to pass the variable name(s) into
     * per-cell configurators. Must cover every row and column index (≥
     * {@code max(rows, cols)} entries).
     *
     * @param headers the variable names, one per row and one per column
     * @return this matrix for fluid chaining
     * @throws IllegalArgumentException if fewer than {@code max(rows, cols)}
     *         headers are given
     */
    public PlotMatrix<DF> headers(String... headers) {
        if (headers.length < Math.max(rows, cols)) {
            throw new IllegalArgumentException("Expected at least " + Math.max(rows, cols)
                    + " headers, got " + headers.length);
        }
        this.headers = headers;
        return this;
    }

    /**
     * Configures the diagonal cells, passing each the name of its variable.
     *
     * @param config the configurator invoked for every {@code (i, i)} cell
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when {@link #headers(String...)} was not set
     */
    public PlotMatrix<DF> diag(VarConfigurator<DF> config) {
        requireHeaders();
        for (int i = 0; i < rows && i < cols; i++) {
            config.configure(cellOrThrow(i, i), headers[i]);
        }
        return this;
    }

    /**
     * Replaces the geometry of every diagonal cell with the given layers,
     * discarding the defaults a {@code pairs()} factory installed (the
     * per-variable histogram). A concise shorthand for
     * {@code diag((d, var) -> d.replaceGeoms(...))} — the override is expressed
     * by the layer value itself, so the descriptor isn't part of the call.
     *
     * @param replacementGeoms the replacement layers
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when a diagonal cell is not populated
     */
    @SafeVarargs
    public final PlotMatrix<DF> diag(LayerConfigurator<? super DF>... replacementGeoms) {
        for (int i = 0; i < rows && i < cols; i++) {
            replaceCellGeoms(cellOrThrow(i, i), replacementGeoms);
        }
        return this;
    }

    /**
     * Configures the cells below the diagonal, passing each the names of its
     * x (column) and y (row) variables.
     *
     * @param config the configurator invoked for every {@code (r, c)} with {@code r > c}
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when {@link #headers(String...)} was not set
     */
    public PlotMatrix<DF> lower(PairConfigurator<DF> config) {
        requireHeaders();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols && c < r; c++) {
                config.configure(cellOrThrow(r, c), headers[c], headers[r]);
            }
        }
        return this;
    }

    /**
     * Replaces the geometry of every lower-triangle cell with the given layers,
     * discarding the defaults a {@code pairs()} factory installed (scatter +
     * correlation annotation). A concise shorthand for
     * {@code lower((d, x, y) -> d.replaceGeoms(...))}.
     *
     * @param replacementGeoms the replacement layers
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when a lower-triangle cell is not populated
     */
    @SafeVarargs
    public final PlotMatrix<DF> lower(LayerConfigurator<? super DF>... replacementGeoms) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols && c < r; c++) {
                replaceCellGeoms(cellOrThrow(r, c), replacementGeoms);
            }
        }
        return this;
    }

    /**
     * Configures the cells above the diagonal, passing each the names of its
     * x (column) and y (row) variables.
     *
     * @param config the configurator invoked for every {@code (r, c)} with {@code r < c}
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when {@link #headers(String...)} was not set
     */
    public PlotMatrix<DF> upper(PairConfigurator<DF> config) {
        requireHeaders();
        for (int r = 0; r < rows; r++) {
            for (int c = r + 1; c < cols; c++) {
                config.configure(cellOrThrow(r, c), headers[c], headers[r]);
            }
        }
        return this;
    }

    /**
     * Replaces the geometry of every upper-triangle cell with the given layers,
     * discarding the defaults a {@code pairs()} factory installed (correlation
     * annotation alone). A concise shorthand for
     * {@code upper((d, x, y) -> d.replaceGeoms(...))}.
     *
     * @param replacementGeoms the replacement layers
     * @return this matrix for fluid chaining
     * @throws IllegalStateException when an upper-triangle cell is not populated
     */
    @SafeVarargs
    public final PlotMatrix<DF> upper(LayerConfigurator<? super DF>... replacementGeoms) {
        for (int r = 0; r < rows; r++) {
            for (int c = r + 1; c < cols; c++) {
                replaceCellGeoms(cellOrThrow(r, c), replacementGeoms);
            }
        }
        return this;
    }

    /**
     * Swaps a cell's geometry pipeline for the given layers: the previously
     * installed layers are dropped before reconfiguring, so the vararg array is
     * consumed element-wise rather than forwarded (keeps {@code -Xlint:varargs}
     * green on the strict build).
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    private final void replaceCellGeoms(PlotDescriptor<DF> cell,
            LayerConfigurator<? super DF>... replacementGeoms) {
        cell.geoms().clear();
        for (var geom : replacementGeoms) {
            ((LayerConfigurator<DF>) geom).configure(cell);
        }
    }

    private PlotDescriptor<DF> cellOrThrow(int row, int col) {
        var desc = cellSpecs.get(row).get(col);
        if (desc == null) {
            throw new IllegalStateException("Cell (" + row + ", " + col
                    + ") has no descriptor yet — populate cells before diag/lower/upper");
        }
        return desc;
    }

    /**
     * Collapses the whole matrix onto a <em>single shared legend</em>: every
     * cell's own guides are suppressed, and the designated cell becomes the
     * legend source, rendered as a standalone band to the right of the grid.
     * The legend source carries the shared aesthetic mapping (e.g. the hue of
     * a {@code pairs()} matrix); the band is styled from the source cell's
     * theme.
     *
     * @param row the 0-based row of the legend source cell
     * @param col the 0-based column of the legend source cell
     * @return this matrix for fluid chaining
     * @throws IndexOutOfBoundsException when the cell is outside the grid
     */
    public PlotMatrix<DF> legendCell(int row, int col) {
        Objects.checkIndex(row, rows);
        Objects.checkIndex(col, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var desc = cellSpecs.get(r).get(c);
                if (desc != null) {
                    desc.guides(Guides.none());
                }
            }
        }
        var source = cellSpecs.get(row).get(col);
        legendSource = source == null ? null : source.copy().guides(Guides.empty());
        return this;
    }

    /**
     * The descriptor powering the shared legend band, or {@code null} when no
     * shared legend has been requested via {@link #legendCell(int, int)}.
     *
     * @return the legend source descriptor, or {@code null}
     */
    public PlotDescriptor<DF> legendSource() {
        return legendSource;
    }

    private void requireHeaders() {
        if (headers == null) {
            throw new IllegalStateException("Call headers(String...) before diag/lower/upper");
        }
    }

    /**
     * Configures a diagonal cell. Receives the cell's descriptor and the name
     * of the variable it shows.
     *
     * @param <DF> the DataFrame type represented by each cell
     */
    @FunctionalInterface
    public interface VarConfigurator<DF> {
        /**
         * Mutates the diagonal cell's descriptor.
         *
         * @param descriptor the cell's descriptor to mutate
         * @param variable the header name
         */
        void configure(PlotDescriptor<DF> descriptor, String variable);
    }

    /**
     * Configures an off-diagonal cell. Receives the cell's descriptor and the
     * names of its x (column) and y (row) variables.
     *
     * @param <DF> the DataFrame type represented by each cell
     */
    @FunctionalInterface
    public interface PairConfigurator<DF> {
        /**
         * Mutates the off-diagonal cell's descriptor.
         *
         * @param descriptor the cell's descriptor to mutate
         * @param xVariable the column name
         * @param yVariable the row name
         */
        void configure(PlotDescriptor<DF> descriptor, String xVariable, String yVariable);
    }

    // ─── Cell accessors ───────────────────────────────────────────────

    /**
     * The cell descriptor at the given position.
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @return the cell's {@link PlotDescriptor}
     */
    public PlotDescriptor<DF> cell(int row, int col) {
        return cellSpecs.get(row).get(col);
    }

    /**
     * Replaces the cell at the given position with an independent descriptor,
     * invalidating any cached {@link Plot} for that cell. Useful for blanking a
     * single cell of an otherwise factory-built matrix (e.g. with
     * {@code Geoms.none()}).
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @param descriptor the descriptor to install, or {@code null} to blank it
     * @return this matrix for fluid chaining
     * @throws IndexOutOfBoundsException when the cell is outside the grid
     */
    public PlotMatrix<DF> cell(int row, int col, PlotDescriptor<DF> descriptor) {
        Objects.checkIndex(row, rows);
        Objects.checkIndex(col, cols);
        cellSpecs.get(row).set(col, descriptor);
        cellPlots.get(row).set(col, null);
        return this;
    }

    /**
     * Resolves the cell's options for its grid position under the matrix policy.
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @return a fully-resolved {@link PlotOptions}
     */
    public PlotOptions resolvedOptions(int row, int col) {
        var resolved = options.resolvedBy(options.outerLabels(), row == rows - 1, col == 0);
        if (axisLabels == AxisLabels.SHOW) {
            resolved = resolved.xLabels(true).yLabels(true);
        } else if (axisLabels == AxisLabels.NONE) {
            resolved = resolved.xLabels(false).yLabels(false);
        }
        return resolved;
    }

    // ─── GgFigure / Canvas / redraw ───────────────────────────────────

    /**
     * Whether a cell's descriptor is considered blank and should be
     * skipped entirely — either unpopulated, or populated with only
     * {@link org.jtaccuino.gog.layer.GeomNone} layers.
     */
    private static boolean isBlank(PlotDescriptor<?> desc) {
        if (desc == null) {
            return true;
        }
        var layers = desc.geoms();
        return layers.isEmpty() || layers.stream().allMatch(l -> l instanceof GeomNone);
    }

    /**
     * Applies the given theme to the whole matrix: every cell descriptor (so
     * not-yet-built cells pick it up), every already-built cell plot, the legend
     * source and any cached legend plot, then re-resolves and repaints this
     * figure itself.
     *
     * @param theme the theme to apply
     */
    @Override
    public void applyTheme(Theme theme) {
        for (var row : cellSpecs) {
            for (var desc : row) {
                if (desc != null) {
                    desc.theme(theme);
                }
            }
        }
        for (var row : cellPlots) {
            for (var plot : row) {
                if (plot != null) {
                    plot.applyTheme(theme);
                }
            }
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
     * Pre-builds and prepares all cell {@code Plot}s and the legend plot
     * on the calling thread (typically the sampler's loader pool) so the
     * first {@link #layoutChildren() → redraw()} on the JavaFX thread
     * performs only canvas painting with no scale/stat work.
     */
    @Override
    public void prepareAsync() {
        setThemeIfAbsent(cellTheme());
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var desc = cellSpecs.get(r).get(c);
                if (!isBlank(desc)) {
                    buildPlot(r, c, desc).prepareAsync();
                }
            }
        }
        if (legendSource != null && cachedLegendPlot == null) {
            cachedLegendPlot = buildLegendPlot();
            cachedLegendPlot.prepareAsync();
        }
    }

    @Override
    public void renderTo(DrawSurface surface, double width, double height) {
        if (width <= 0 || height <= 0 || rows <= 0 || cols <= 0) {
            return;
        }
        var renderStart = System.nanoTime();
        var renderEvt = new org.jtaccuino.gog.jfr.MatrixRenderEvent();
        boolean enabled = renderEvt.isEnabled();
        if (enabled) {
            renderEvt.rows = rows;
            renderEvt.cols = cols;
            renderEvt.panelCount = rows * cols;
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
        var theme = cellTheme();
        setThemeIfAbsent(theme);
        double titleH = reservedTitleBand(theme);
        double legendW = legendSource != null ? PlotDescriptor.legendBandWidth(legendSource) : 0.0;
        double rightMargin = legendW > 0 ? 0.0 : theme.marginRight();
        double hGap = explicitGaps ? this.hGap : theme.facetHGap();
        double vGap = explicitGaps ? this.vGap : theme.facetVGap();
        if (options.outerLabels() == PlotOptions.OuterLabels.MARGINS) {
            renderUniformFrame(surface, width, height, titleH, legendW, rightMargin, hGap, vGap, theme);
        } else {
            renderLabeledFrame(surface, width, height, titleH, legendW, rightMargin, hGap, vGap);
        }
        if (title != null || subtitle != null) {
            drawMainTitle(surface, width, titleH, theme);
        }
        if (legendSource != null) {
            var legendPlot = cachedLegendPlot != null ? cachedLegendPlot : buildLegendPlot();
            surface.save();
            // The legend band hugs the grid top rather than the title band, so
            // the guide reads as anchored to the figure's top edge.
            surface.translate(width - legendW, 0);
            try {
                legendPlot.renderTo(surface, legendW, height);
            } finally {
                surface.restore();
            }
        }
        if (caption != null) {
            drawCaption(surface, width, height, legendW, theme);
        }
    }

    /**
     * The {@code MARGINS} label frame: every cell reserves the same bands on
     * its outer edges ({@link Theme#matrixLabelBandY()} left of the first
     * column, {@link Theme#matrixLabelBandX()} below the bottom row) and shares
     * an identical panel geometry via
     * {@link PlotDescriptor#panelInsets(double, double, double, double)}.
     * Panels in a column align and the shared x-axis tick labels line up,
     * mirroring facet grid / separable-axes semantics. The variable names from
     * {@link #headers} render afterwards as standard axis titles.
     */
    private void renderUniformFrame(DrawSurface surface, double width, double height,
            double titleH, double legendW, double rightMargin, double hGap, double vGap, Theme theme) {
        double yBand = theme.matrixLabelBandY();
        double xBand = theme.matrixLabelBandX();
        double gridW = Math.floor(width - legendW - rightMargin - yBand - (cols - 1) * hGap);
        double gridH = Math.floor(height - titleH - xBand - (rows - 1) * vGap);
        double[] colW = GridMath.proportionalIntegral(widths(), gridW);
        double[] rowH = GridMath.proportionalIntegral(heights(), gridH);
        double[] colX = new double[cols];
        double[] rowY = new double[rows];
        // colX: first column starts at 0 (includes the y label band); subsequent
        // columns sit right of it.
        double accX = 0.0;
        for (int c = 0; c < cols; c++) {
            colX[c] = (c == 0 ? 0.0 : yBand) + accX;
            accX += colW[c] + hGap;
        }
        double accY = titleH;
        for (int r = 0; r < rows; r++) {
            rowY[r] = accY;
            accY += rowH[r] + vGap;
        }
        commitMatrixLayout(colX, rowY, colW, rowH, cols, rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var desc = cellSpecs.get(r).get(c);
                if (isBlank(desc)) {
                    continue;
                }
                double cellW = (c == 0 ? yBand : 0.0) + colW[c];
                double cellH = (r == rows - 1 ? xBand : 0.0) + rowH[r];
                renderCell(surface, r, c, cellW, cellH, colX[c], rowY[r]);
            }
        }
        var axisNames = columnLabels != null ? columnLabels : headers;
        if (xLabel != null || yLabel != null || axisNames != null) {
            drawOuterAxisTitles(surface, colW, rowH, colX, rowY, theme);
        }
    }

    /** The general {@code ALL} label frame: uniform cells, per-cell label zones. */
    private void renderLabeledFrame(DrawSurface surface, double width, double height,
            double titleH, double legendW, double rightMargin, double hGap, double vGap) {
        double gridW = Math.floor(width - legendW - rightMargin - (cols - 1) * hGap);
        double gridH = Math.floor(height - titleH - (rows - 1) * vGap);
        double[] colW = GridMath.proportionalIntegral(widths(), gridW);
        double[] rowH = GridMath.proportionalIntegral(heights(), gridH);
        double[] colX = GridMath.leadingOffsets(colW, hGap);
        double[] rowY = new double[rows];
        double accY = titleH;
        for (int r = 0; r < rows; r++) {
            rowY[r] = accY;
            accY += rowH[r] + vGap;
        }
        commitMatrixLayout(colX, rowY, colW, rowH, cols, rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                var desc = cellSpecs.get(r).get(c);
                if (isBlank(desc)) {
                    continue;
                }
                renderCell(surface, r, c, colW[c], rowH[r], colX[c], rowY[r]);
            }
        }
    }

    private void renderCell(DrawSurface surface, int row, int col, double cellW, double cellH,
            double cx, double cy) {
        var plot = buildPlot(row, col, cellSpecs.get(row).get(col));
        surface.save();
        surface.translate(cx, cy);
        try {
            plot.renderTo(surface, cellW, cellH);
        } finally {
            surface.restore();
        }
    }

    /**
     * Records the resolved matrix cell geometry so a JFR report can prove
     * whether the proportional cell division left the cell origins on
     * subpixel coordinates. A fractional cell width/height (or a fractional
     * origin from the cumulative offsets) pushes every cell primitive onto
     * subpixels and costs ~10x in the raster paths.
     */
    private void commitMatrixLayout(double[] colX, double[] rowY, double[] colW, double[] rowH,
            int cols, int rows) {
        var layoutEvt = new MatrixLayoutEvent();
        layoutEvt.numCols = cols;
        layoutEvt.numRows = rows;
        layoutEvt.cellWidth = colW[0];
        layoutEvt.cellHeight = rowH[0];
        layoutEvt.originsIntegral = matrixOriginsIntegral(colX, rowY, cols, rows);
        layoutEvt.begin();
        layoutEvt.end();
        layoutEvt.commit();
    }

    /** Whether every cell origin computed from the given grid geometry falls on a whole pixel. */
    private static boolean matrixOriginsIntegral(double[] colX, double[] rowY, int cols, int rows) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (colX[c] != Math.floor(colX[c]) || rowY[r] != Math.floor(rowY[r])) {
                    return false;
                }
            }
        }
        return true;
    }

    private Plot<DF> buildLegendPlot() {
        var legend = legendSource.copy().guidesOnly();
        legend.renderMode(renderMode);
        // A standalone guide needs no axis label zone; dropping it lets the
        // legend keys start right below the figure's top edge instead of a
        // large band further down.
        legend.theme(t -> t.axisLabelZone(0));
        return Ggplot.<DF>ggplot(legend);
    }

    /**
     * Draws the variable names as axis titles. The precedence is:
     * <ul>
     *   <li>{@link #xlab}/{@link #ylab} — a single title spanning the
     *       whole grid, drawn once.</li>
     *   <li>{@link #columnLabels} — overrides {@link #headers} on both axes
     *       (per-column/per-row names drawn at their respective panel
     *       centres).</li>
     *   <li>{@link #headers} — the default per-variable names.</li>
     * </ul>
     * Panel sizes are passed in explicitly so the centre of each
     * non-uniform panel can be computed correctly.
     */
    private void drawOuterAxisTitles(DrawSurface surface, double[] colW, double[] rowH,
            double[] colX, double[] rowY, Theme theme) {
        var axisStyle = AxisStyle.from(theme);
        var perCell = columnLabels != null ? columnLabels : headers;

        surface.save();
        surface.setFont(axisStyle.titleFont());
        surface.setFill(axisStyle.titleColor());
        surface.setTextBaseline(VPos.BASELINE);

        // ── X-axis titles ──────────────────────────────────────────────
        double xTitleY = rowY[rows - 1] + rowH[rows - 1] + theme.axisTitleOffset();
        if (xLabel != null) {
            // Single centered title spanning the whole grid.
            double gridLeft  = colX[0];
            double gridRight = colX[cols - 1] + colW[cols - 1];
            surface.setTextAlign(TextAlignment.CENTER);
            surface.fillText(xLabel, (gridLeft + gridRight) / 2.0, xTitleY);
        } else if (perCell != null) {
            surface.setTextAlign(TextAlignment.CENTER);
            for (int c = 0; c < cols; c++) {
                if (c >= perCell.length) {
                    break;
                }
                double panelCenter = colX[c] + colW[c] / 2.0;
                surface.fillText(perCell[c], panelCenter, xTitleY);
            }
        }

        // ── Y-axis titles ──────────────────────────────────────────────
        if (yLabel != null) {
            double gridTop = rowY[0];
            double gridBot = rowY[rows - 1] + rowH[rows - 1];
            surface.save();
            surface.translate(theme.matrixYTitlePivot(), (gridTop + gridBot) / 2.0);
            surface.rotate(-90.0);
            surface.setTextAlign(TextAlignment.CENTER);
            surface.fillText(yLabel, 0.0, 0.0);
            surface.restore();
        } else if (perCell != null) {
            for (int r = 0; r < rows; r++) {
                if (r >= perCell.length) {
                    break;
                }
                double panelMidY = rowY[r] + rowH[r] / 2.0;
                surface.save();
                surface.translate(theme.matrixYTitlePivot(), panelMidY);
                surface.rotate(-90.0);
                surface.setTextAlign(TextAlignment.CENTER);
                surface.fillText(perCell[r], 0.0, 0.0);
                surface.restore();
            }
        }
        surface.restore();
    }

    private Theme cellTheme() {
        var first = cellSpecs.get(0).get(0);
        return first != null ? first.theme() : Theme.theme_gray();
    }

    // ─── Internal ─────────────────────────────────────────────────────

    private Plot<DF> buildPlot(int row, int col, PlotDescriptor<DF> desc) {
        var existing = cellPlots.get(row).get(col);
        if (existing != null) {
            return existing;
        }
        var resolved = desc.options();
        var copy = desc.copy();
        copy.options(resolved);
        copy.renderMode(renderMode);
        if (options.outerLabels() == PlotOptions.OuterLabels.MARGINS && copy.panelInsets() == null) {
            var theme = desc.theme();
            copy.panelInsets(
                    col == 0 ? theme.matrixLabelBandY() : 0.0,
                    0.0,
                    0.0,
                    row == rows - 1 ? theme.matrixLabelBandX() : 0.0);
        }
        var plot = Ggplot.<DF>ggplot(copy);
        cellPlots.get(row).set(col, plot);
        return plot;
    }

    /**
     * Functional interface for cell configuration with position context.
     *
     * @param <DF> the DataFrame type
     * @param <SELF> the concrete type (for chaining)
     */
    @FunctionalInterface
    public interface CellConfigurator<DF, SELF> {
        /**
         * Configures a cell descriptor. The descriptor's {@link PlotOptions}
         * have already been resolved for the cell's grid position.
         *
         * @param descriptor the cell's mutable descriptor
         * @param row the row index (0-based)
         * @param col the column index (0-based)
         */
        void configure(PlotDescriptor<DF> descriptor, int row, int col);
    }
}
