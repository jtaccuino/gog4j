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

import java.util.List;
import org.jtaccuino.gog.data.EmptyDataFrame;
import org.jtaccuino.gog.data.EmptyDataFrameExtractor;
import org.jtaccuino.gog.facet.FacetSpec;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;
import org.jtaccuino.gog.stat.StatCor;

/**
 * Central entry point and primary factory for the <b>gog4j</b> Grammar of Graphics plotting library.
 * <p>
 * {@code Ggplot} provides a static builder method ({@link #ggplot(Object, Aes)})
 * to instantiate a new declarative {@link Plot} bound to a dataset and an
 * aesthetic mapping ({@link Aes}). The matching {@link DataExtractor} for the
 * dataset type is resolved automatically through the {@link DataExtractorRegistry}.
 * <p>
 * Example usage with DFLib:
 * <pre>{@code
 * Plot<DataFrame> plot = ggplot(df, aes().x("displ").y("hwy").color("drv"))
 *     .geoms(point(), smooth())
 *     .labs(labs("MPG vs Displacement", "Displacement (L)", "Highway MPG"))
 *     .theme(new DarkTheme());
 * }</pre>
 * <p>
 * For three-dimensional plots use the 3D entry points {@link #plot3d(Object, Aes)}
 * (the {@link PlotDescriptor3D} spec half) and {@link #ggplot3d(Object, Aes)}
 * (the {@link Plot} node half), which install a {@code Coord3D} cube and expose
 * its view controls as first-class methods.
 *
 * @see Plot
 * @see PlotDescriptor3D
 * @see Aes
 * @see DataExtractor
 */
public class Ggplot {

    /** Utility class; not meant to be instantiated. */
    private Ggplot() {}

    /**
     * Creates a new declarative {@link Plot} bound to the specified dataset, resolving the
     * {@link DataExtractor} for the DataFrame type through the {@link DataExtractorRegistry}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings defining variable bindings (e.g., x, y, color, fill, shape, group)
     * @return a new, reactive {@link Plot} instance for fluid geometry, scale, facet, and theme configuration
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     */
    @SuppressWarnings("unchecked")
    public static <DF> Plot<DF> ggplot(DF df, Aes aes) {
        var extractor = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "No DataExtractor registered for DataFrame type " + df.getClass().getName());
        }
        return new Plot<>(new PlotDescriptor<>(df, extractor, aes));
    }

    /**
     * Creates a new declarative {@link Plot} bound to no data, backed by the
     * library-internal, always-empty {@link EmptyDataFrame}.
     * <p>
     * This is the entry point for data-less plots such as a standalone
     * {@code Geoms.function()}: the x axis is then sized by the plot's
     * coordinate limits (e.g. {@code Coord2D.cartesian().xlim(...)}) rather
     * than by any data.
     *
     * @param aes the global aesthetic mappings defining variable bindings (e.g., x, y, color, fill, shape, group)
     * @return a new, reactive {@code Plot<EmptyDataFrame>} instance for fluid geometry, scale, facet, and theme configuration
     */
    public static Plot<EmptyDataFrame> ggplot(Aes aes) {
        return new Plot<>(new PlotDescriptor<>(EmptyDataFrame.INSTANCE, new EmptyDataFrameExtractor(), aes));
    }

    /**
     * Creates a new {@link PlotDescriptor} bound to the specified dataset,
     * resolving the {@link DataExtractor} for the DataFrame type through the
     * {@link DataExtractorRegistry}.
     * <p>
     * A descriptor is the spec half of the gog4j model: it carries every
     * configuration (data, aesthetics, geoms, scales, facets, labs, theme,
     * coord, guides, options) but no rendering node, so it can be shown
     * standalone via {@link #ggplot(PlotDescriptor)} or cloned into the cells
     * of a composite {@code PlotMatrix}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings defining variable bindings (e.g., x, y, color, fill, shape, group)
     * @return a new, configurable {@link PlotDescriptor} for fluid spec construction
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     */
    @SuppressWarnings("unchecked")
    public static <DF> PlotDescriptor<DF> plot(DF df, Aes aes) {
        var extractor = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "No DataExtractor registered for DataFrame type " + df.getClass().getName());
        }
        return new PlotDescriptor<>(df, extractor, aes);
    }

    /**
     * Creates a new {@link PlotDescriptor} bound to the specified dataset with
     * presentation options applied, resolving the {@link DataExtractor} for the
     * DataFrame type through the {@link DataExtractorRegistry}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings defining variable bindings (e.g., x, y, color, fill, shape, group)
     * @param options the presentation options (e.g., {@link PlotOptions#defaults()})
     * @return a new, configurable {@link PlotDescriptor} for fluid spec construction
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     */
    @SuppressWarnings("unchecked")
    public static <DF> PlotDescriptor<DF> plot(DF df, Aes aes, PlotOptions options) {
        var descriptor = plot(df, aes);
        descriptor.options(options);
        return descriptor;
    }

    /**
     * Creates a new, reactive {@link Plot} node rendering the given descriptor.
     *
     * @param <DF> the DataFrame type represented by the descriptor
     * @param descriptor the spec to render (typically from {@link #plot(Object, Aes)})
     * @return a new {@code Plot<DF>} node bound to the descriptor's spec
     */
    public static <DF> Plot<DF> ggplot(PlotDescriptor<DF> descriptor) {
        return new Plot<>(descriptor);
    }

    // ─── 3D entry points ──────────────────────────────────────────────

    /**
     * Creates a new 3D {@link PlotDescriptor3D} bound to the specified dataset,
     * resolving the {@link DataExtractor} for the DataFrame type through the
     * {@link DataExtractorRegistry}.
     * <p>
     * This is the discoverable entry point for 3D plots: the returned
     * descriptor already carries a {@code Coord3D} and exposes the cube's view
     * controls as first-class methods, so the canonical recipe reads:
     * <pre>{@code
     * plot3d(df, aes().x("displ").y("hwy").z("drv"))
     *     .view(35, -75, -55)
     *     .panels(CubePanel.NONE)
     *     .light(Light3d.defaultLight())
     *     .geoms(col3d());
     * }</pre>
     * Map the {@code z} aesthetic to the third data dimension; without it the
     * cube has no depth to render.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings (x, y, z, color, fill, shape, group)
     * @return a new, configurable {@link PlotDescriptor3D} for fluid spec construction
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     * @see PlotDescriptor3D
     * @see #ggplot3d(Object, Aes)
     */
    @SuppressWarnings("unchecked")
    public static <DF> PlotDescriptor3D<DF> plot3d(DF df, Aes aes) {
        var extractor = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "No DataExtractor registered for DataFrame type " + df.getClass().getName());
        }
        return new PlotDescriptor3D<>(df, extractor, aes);
    }

    /**
     * Creates a new 3D {@link PlotDescriptor3D} bound to the specified dataset
     * with presentation options applied, resolving the {@link DataExtractor}
     * for the DataFrame type through the {@link DataExtractorRegistry}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings (x, y, z, color, fill, shape, group)
     * @param options the presentation options (e.g., {@link PlotOptions#defaults()})
     * @return a new, configurable {@link PlotDescriptor3D} for fluid spec construction
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     * @see #plot3d(Object, Aes)
     */
    public static <DF> PlotDescriptor3D<DF> plot3d(DF df, Aes aes, PlotOptions options) {
        var descriptor = plot3d(df, aes);
        descriptor.options(options);
        return descriptor;
    }

    /**
     * Creates a new, reactive 3D {@link Plot} node bound to the specified
     * dataset, the node shortcut of {@link #plot3d(Object, Aes)}.
     * <p>
     * The returned node renders a {@link org.jtaccuino.gog.coord.Coord3D} cube. Configure the view
     * through the descriptor form ({@code ggplot3d(plot3d(...).view(...))}) if
     * you need typed cube controls while chaining.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object (e.g., DFLib {@code DataFrame})
     * @param aes the global aesthetic mappings (x, y, z, color, fill, shape, group)
     * @return a new {@code Plot<DF>} node rendering the 3D spec
     * @throws IllegalArgumentException if no registered extractor supports the DataFrame type
     * @see #plot3d(Object, Aes)
     */
    public static <DF> Plot<DF> ggplot3d(DF df, Aes aes) {
        return new Plot<>(plot3d(df, aes));
    }

    /**
     * Creates a new, reactive {@link Plot} node rendering the given 3D
     * descriptor.
     *
     * @param <DF> the DataFrame type represented by the descriptor
     * @param descriptor the 3D spec to render (typically from {@link #plot3d(Object, Aes)})
     * @return a new {@code Plot<DF>} node bound to the descriptor's spec
     */
    public static <DF> Plot<DF> ggplot3d(PlotDescriptor3D<DF> descriptor) {
        return new Plot<>(descriptor);
    }

    /**
     * Creates an {@code m×n} {@link PlotMatrix} with a base descriptor shared
     * across all cells. Each cell receives a {@link PlotDescriptor#copy()} of
     * the base and can be individually overridden via
     * {@link PlotMatrix#cells(PlotMatrix.CellConfigurator...)}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df the input dataset object
     * @param aes the global aesthetic mappings
     * @param rows the number of rows (positive)
     * @param cols the number of columns (positive)
     * @return a new {@link PlotMatrix} ready for cell configuration
     * @throws IllegalArgumentException if rows or cols are not positive
     */
    @SuppressWarnings("unchecked")
    public static <DF> PlotMatrix<DF> ggplot(DF df, Aes aes, int rows, int cols) {
        var descriptor = plot(df, aes);
        return PlotMatrix.<DF>create(rows, cols).base(descriptor);
    }

    /**
     * Creates an empty {@code m×n} {@link PlotMatrix} for mixed datasets.
     * Use {@link PlotMatrix#cells(PlotDescriptor[])} to populate it with
     * independent descriptors per cell.
     *
     * @param rows the number of rows (positive)
     * @param cols the number of columns (positive)
     * @return a new {@link PlotMatrix}
     * @throws IllegalArgumentException if rows or cols are not positive
     */
    public static PlotMatrix<?> ggplot(int rows, int cols) {
        return PlotMatrix.create(rows, cols);
    }

    /**
     * Creates a multi-plot {@link ComposedPlot} arranging the given
     * figures into a single row (the default grid). Use
     * {@link ComposedPlot#rows(int)}/{@link ComposedPlot#cols(int)} or
     * {@link ComposedPlot#beside(GgFigure)}/{@link ComposedPlot#above(GgFigure)}
     * to shape the grid and {@link ComposedPlot#title(String)} etc. to
     * annotate it.
     *
     * @param figures the figures (plots, matrices, or compositions) to compose
     * @return a new {@link ComposedPlot} ready for configuration
     */
    public static ComposedPlot composedPlot(GgFigure... figures) {
        return new ComposedPlot(figures);
    }

    // ─── matrixPlot ───────────────────────────────────────────────────

    /**
     * Builds an <em>n</em>×<em>n</em> {@link PlotMatrix} in the style of
     * a generalized pairs plot. Every cell is dispatched from its
     * variables' {@link DataExtractor.ColumnType}: continuous×continuous pairs
     * render a density diagonal, a plain scatter lower triangle and a
     * correlation-only upper triangle; mixed pairs render a box plot and a
     * faceted histogram; and categorical pairs render count bars.  The base
     * {@code aes} carries any shared visual mappings (colour, shape, facet,
     * &hellip;) that propagate to every cell.
     * <p>
     * <strong>Matrix-mode defaults</strong> (applied automatically; standalone
     * plots are unaffected):
     * <ul>
     *   <li>The variable names render as <em>header strips</em> — a column
     *       strip above each column and a rotated row strip on the left —
     *       instead of per-cell titles or axis titles.</li>
     *   <li>Axis tick labels are rendered on the <em>outer edges</em> only
     *       (bottom row and left column), mirroring facet-wrap / grid
     *       semantics.</li>
     *   <li>Cells use a compact label zone so the panels fill the grid.</li>
     *   <li>When the base {@code aes} maps a legend-bearing aesthetic
     *       (colour, shape, &hellip;) the top-right cell becomes the legend
     *       source and a <em>single shared legend</em> renders to the right
     *       of the whole matrix via
     *       {@link PlotMatrix#legendCell(int, int)}.</li>
     * </ul>
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df   the input dataset
     * @param aes  the global aesthetic mappings (colour, shape, &hellip;)
     * @param columns the variable names that span the rows and columns (≥ 1)
     * @return a fully-wired {@link PlotMatrix} ready for rendering
     * @throws IllegalArgumentException if {@code columns} is empty or
     *         no {@link DataExtractor} supports the DataFrame type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <DF> PlotMatrix<DF> matrixPlot(DF df, Aes aes, String... columns) {
        return matrixPlotCore(df, aes, PlotOptions.defaults(), columns);
    }

    /**
     * Builds an <em>n</em>×<em>n</em> {@link PlotMatrix} in the style of
     * a generalized pairs plot with default {@link PlotOptions}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df   the input dataset
     * @param columns the variable names that span the rows and columns (≥ 1)
     * @return a fully-wired {@link PlotMatrix} ready for rendering
     * @throws IllegalArgumentException if {@code columns} is empty or
     *         no {@link DataExtractor} supports the DataFrame type
     * @see #matrixPlot(Object, Aes, String...)
     */
    public static <DF> PlotMatrix<DF> matrixPlot(DF df, String... columns) {
        return matrixPlot(df, Aes.aes(), columns);
    }

    /**
     * Builds an <em>n</em>×<em>n</em> {@link PlotMatrix} in the style of
     * a generalized pairs plot with presentation options applied.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @param df   the input dataset
     * @param aes  the global aesthetic mappings (colour, shape, &hellip;)
     * @param options the presentation options (e.g. outer-label policy)
     * @param columns the variable names that span the rows and columns (≥ 1)
     * @return a fully-wired {@link PlotMatrix} ready for rendering
     * @throws IllegalArgumentException if {@code columns} is empty or
     *         no {@link DataExtractor} supports the DataFrame type
     * @see #matrixPlot(Object, Aes, String...)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <DF> PlotMatrix<DF> matrixPlot(DF df, Aes aes, PlotOptions options, String... columns) {
        return matrixPlotCore(df, aes, options, columns);
    }

    /** Shared cell-wiring core of every {@code matrixPlot(...)} overload. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <DF> PlotMatrix<DF> matrixPlotCore(DF df, Aes aes,
            PlotOptions options, String... columns) {
        if (columns.length == 0) {
            throw new IllegalArgumentException("matrixPlot requires at least one column name");
        }
        var extractor = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "No DataExtractor registered for DataFrame type " + df.getClass().getName());
        }
        int n = columns.length;
        var columnTypes = new DataExtractor.ColumnType[n];
        for (int i = 0; i < n; i++) {
            columnTypes[i] = extractor.columnType(df, columns[i]);
        }
        var descriptors = new PlotDescriptor[n * n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (r == c) {
                    descriptors[r * n + c] = matrixDiagonalCell(df, aes, columns[r], columnTypes[r]);
                } else {
                    var kind = CellKind.of(columnTypes[r], columnTypes[c]);
                    descriptors[r * n + c] = matrixOffDiagonalCell(
                            df, aes, columns[c], columnTypes[c],
                            columns[r], kind, r < c);
                }
            }
        }
        PlotMatrix<DF> matrix = PlotMatrix.<DF>create(n, n).options(options)
                .headers(columns);
        matrix.cells(descriptors);
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                matrix.cell(r, c).options(matrix.resolvedOptions(r, c).xTitle(false).yTitle(false));
                // Cells compact their label zone so panels fill the grid; the
                // matrix's outer bands already reserve room for the labels.
                matrix.cell(r, c).theme(t -> t.axisLabelZone(t.matrixCellLabelZone()));
            }
        }
        if (hasLegendMapping(aes)) {
            matrix.legendCell(0, n - 1);
        }
        return matrix;
    }

    /** Diagonal cell: a count bar for a categorical variable, a density otherwise. */
    private static <DF> PlotDescriptor<DF> matrixDiagonalCell(
            DF df, Aes base, String variable, DataExtractor.ColumnType type) {
        var diagAes = hueAes(base, Aes.aes().x(variable));
        if (type == DataExtractor.ColumnType.TEXT) {
            return plot(df, diagAes).geoms(Geoms.bar());
        }
        return plot(df, diagAes).geoms(Geoms.density());
    }

    /** Off-diagonal cell dispatched by its pair's {@link CellKind}. */
    private static <DF> PlotDescriptor<DF> matrixOffDiagonalCell(
            DF df, Aes base,
            String xVar, DataExtractor.ColumnType xType,
            String yVar, CellKind kind, boolean upper) {
        return switch (kind) {
            case TT -> ttCell(df, base, xVar, yVar, upper);
            case COMBO -> comboCell(df, base, xVar, xType, yVar, upper);
            case DD -> ddCell(df, base, xVar, yVar, upper);
        };
    }

    /** Continuous×continuous: correlation-only upper, plain scatter lower. */
    private static <DF> PlotDescriptor<DF> ttCell(DF df, Aes base,
            String xVar, String yVar, boolean upper) {
        if (upper) {
            return matrixCell(df, base, xVar, yVar, true);
        }
        // A pair-plot cell packs 54k points into a ~300px panel: a compact dot
        // keeps the cluster readable instead of a 9px blob per point (the
        // pair-plot convention, cf. generalized pairs plots and scatter-plot matrices).
        return plot(df, cellAes(base, xVar, yVar)).geoms(Geoms.point().size(2.5));
    }

    /** Continuous×categorical (either orientation). */
    private static <DF> PlotDescriptor<DF> comboCell(DF df, Aes base,
            String xVar, DataExtractor.ColumnType xType, String yVar, boolean upper) {
        boolean xCont = CellKind.isContinuous(xType);
        String discrete = xCont ? yVar : xVar;
        String continuous = xCont ? xVar : yVar;
        if (upper) {
            // box_no_facet: box of the continuous variable per discrete level
            return plot(df, cellAes(base, discrete, continuous))
                    .geoms(Geoms.boxplot());
        }
        // facethist: histogram of the continuous variable per discrete level
        return plot(df, cellAes(base, continuous, null))
                .facets(FacetSpec.grid(List.of(discrete), List.of(), GridOptions.defaults()))
                .geoms(Geoms.histogram());
    }

    /** Categorical×categorical: grouped count bars upper, faceted bars lower. */
    private static <DF> PlotDescriptor<DF> ddCell(DF df, Aes base,
            String xVar, String yVar, boolean upper) {
        if (upper) {
            return plot(df, cellAes(base, xVar, null).fill(yVar))
                    .geoms(Geoms.bar());
        }
        return plot(df, cellAes(base, xVar, null))
                .facets(FacetSpec.grid(List.of(yVar), List.of(), GridOptions.defaults()))
                .geoms(Geoms.bar());
    }

    /** A fresh aes carrying the x/y bindings plus the base's colour/shape/alpha. */
    private static Aes cellAes(Aes base, String x, String y) {
        Aes a = Aes.aes().x(x).y(y);
        if (base.color() != null) {
            a.color(base.color());
        }
        if (base.shape() != null) {
            a.shape(base.shape());
        }
        if (base.alpha() != null) {
            a.alpha(base.alpha());
        }
        return a;
    }

    /** Overlays the base fill/colour mapping on a diagonal aes. */
    private static Aes hueAes(Aes base, Aes a) {
        if (base.fill() != null) {
            a.fill(base.fill());
        } else if (base.color() != null) {
            a.color(base.color());
        }
        return a;
    }

    /**
     * Off-diagonal continuous×continuous cell.  When {@code upper} is
     * {@code true} the cell shows only the {@code Stats.cor()} correlation
     * annotation; otherwise it carries a plain scatter layer.
     */
    static <DF> PlotDescriptor<DF> matrixCell(
            DF df, Aes base,
            String xCol, String yCol, boolean upper) {
        var aes = upper
                ? corrCellAes(base, xCol, yCol)
                : cellAes(base, xCol, yCol);
        if (upper) {
            return plot(df, aes).geoms(Geoms.corrText());
        }
        return plot(df, aes).geoms(Geoms.point());
    }

    /**
     * Builds a new {@link Aes} copying the base and overriding x, y and the
     * after-stat label reference.
     */
    private static Aes corrCellAes(Aes base, String xCol, String yCol) {
        return base.overrideWith(Aes.aes()
                .x(xCol)
                .y(yCol)
                .label(AesValue.afterStat(AesValue.ComputedVariable.CORR)));
    }

    /**
     * Pearson sample correlation between two parallel columns. Rows where
     * either value is null or non-numeric are skipped. Delegates to
     * {@link StatCor}, which also hosts the computation for the matrixPlot
     * correlation cells.
     */
    static double pearson(List<?> a, List<?> b) {
        return StatCor.pearson(a, b);
    }

    private static boolean hasLegendMapping(Aes aes) {
        return aes.color() != null
                || aes.fill() != null
                || aes.shape() != null
                || aes.linetype() != null
                || aes.size() != null
                || aes.alpha() != null;
    }
}
