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
package org.jtaccuino.gog.stat;

import org.jtaccuino.gog.stat.StatSmooth.SmoothBuilder;
import org.jtaccuino.gog.stat.StatSummary.SummaryBuilder;

/**
 * Static facade over the stat transformations — {@code Stats.bin()},
 * {@code Stats.smooth()}, {@code Stats.summary()}, and {@code Stats.cor()} —
 * mirroring how
 * {@code Geoms} exposes the geometric layers. It is the gog4j counterpart of
 * the {@code transform} constructors: the one place through which a raw
 * stat is obtained, either for the rendering pipeline
 * ({@link #bin()}, {@link #smooth()}, {@link #summary()}, {@link #cor()}) or as a
 * ready-to-bind stat for {@code Plot.layer(...)} /
 * {@code geom.stat(...)}, and through which the dataframe-agnostic fluent
 * entry points ({@link #smooth(Object, String, String)},
 * {@link #summary(Object, String, String)}) are started.
 * <p>
 * {@link #identity()} expresses the {@code Stats.identity()} default of
 * {@code Geoms.col()}: it carries no transformation, so a geometry bound to it
 * draws the raw columns directly (the identity behaviour).
 * <pre>{@code
 * var edges = Stats.bin().edges(xValues, BinParams.defaults());
 *
 * Stats.smooth(cars, "wt", "mpg")
 *     .lm().se(false).fit();
 * Stats.summary(df, "grp", "val")
 *     .meanSd().fit();
 * }</pre>
 *
 * @see org.jtaccuino.gog.Plot#layer
 */
public final class Stats {

    /** Utility class; not meant to be instantiated. */
    private Stats() {
    }

    /**
     * The identity statistic ({@code Stats.identity()} in the reference implementation): the "no
     * transformation" marker that makes a stat-consuming geometry fall back to
     * drawing the raw input columns directly. Returns {@code null} because
     * identity is expressed as the absence of a stat to run — bind it via
     * {@code geom.stat(Stats.identity())} to restore raw rendering.
     *
     * @param <DF> the DataFrame type
     * @return {@code null}, meaning "run no statistic"
     */
    public static <DF> Stat<DF> identity() {
        return null;
    }

    /**
     * {@code Stats.identity3d()} — the pass-through statistic of the 3D
     * geometry family ({@code StatIdentity3d}): it turns discrete
     * {@code x}/{@code y}/{@code z} columns into numeric positions and emits
     * the hierarchical {@code group} column that the depth sorter consumes,
     * without aggregating any rows.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatIdentity3d} transformation
     */
    public static <DF> Stat<DF> identity3d() {
        return new StatIdentity3d<>();
    }

    /**
     * {@code Stats.surface3d()} — the point-row pass-through for 3D surfaces,
     * adding per-point gradients when the data form a regular grid.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatSurface3d} transformation
     */
    public static <DF> Stat<DF> surface3d() {
        return new StatSurface3d<>();
    }

    /**
     * {@code Stats.function3d()} — evaluates a {@code f(x, y) = z} function
     * over a regular grid, emitting point rows with gradients. The function
     * travels in the layer's stat params under the key {@code "fun"}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatFunction3d} transformation
     */
    public static <DF> Stat<DF> function3d() {
        return new StatFunction3d<>();
    }

    /**
     * {@code Stats.density3d()} — a 2-D Gaussian kernel density surface over a
     * point grid, with {@code row}/{@code col} retained for sparse
     * rectangular tiling after {@code min_ndensity} filtering.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatDensity3d} transformation
     */
    public static <DF> Stat<DF> density3d() {
        return new StatDensity3d<>();
    }

    /**
     * {@code Stats.smooth3d()} — fits a loess/lm surface and emits polygon
     * rows (optionally clipped to the data hull, with confidence surfaces).
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatSmooth3d} transformation
     */
    public static <DF> Stat<DF> smooth3d() {
        return new StatSmooth3d<>();
    }

    /**
     * {@code Stats.distributions3d()} — one 1-D kernel density per position
     * slice, laid out as 3-D ridgelines.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatDistributions3d} transformation
     */
    public static <DF> Stat<DF> distributions3d() {
        return new StatDistributions3d<>();
    }

    /**
     * {@code Stats.col3d()} — turns grid points into rectangular 3-D columns,
     * from a {@code zmin} base up to each row's {@code z}.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatCol3d} transformation
     */
    public static <DF> Stat<DF> col3d() {
        return new StatCol3d<>();
    }

    /**
     * {@code Stats.bar3d()} — counts or bins a 2-D layout into 3-D bars, with
     * the computed {@code count}/{@code proportion}/{@code ncount}/
     * {@code density}/{@code ndensity} variables.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatBar3d} transformation
     */
    public static <DF> Stat<DF> bar3d() {
        return new StatBar3d<>();
    }

    /**
     * {@code Stats.voxel3d()} — turns sparse 3-D points into fixed-size cubes
     * centered on each coordinate.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatVoxel3d} transformation
     */
    public static <DF> Stat<DF> voxel3d() {
        return new StatVoxel3d<>();
    }

    /**
     * {@code Stats.hull3d()} — turns a 3-D point cloud into a convex or alpha
     * surface hull of triangular polygons.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a {@link StatHull3d} transformation
     */
    public static <DF> Stat<DF> hull3d() {
        return new StatHull3d<>();
    }

    /**
     * {@code Stats.count()} — counts the observations for each value of
     * {@code aes(x)}. The inherited statistic of {@code Geoms.bar()}.
     *
     * @param <DF> the DataFrame type
     * @return a {@link StatCount} transformation
     */
    public static <DF> Stat<DF> count() {
        return new StatCount<>();
    }

    /**
     * Creates a {@code Stats.bin()} transformation for the histogram and
     * freqpoly families, binning a numeric {@code x} into a count-per-edge grid.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a fresh {@code StatBin}
     */
    public static <DF> StatBin<DF> bin() {
        return new StatBin<>();
    }

    /**
     * Creates a {@code Stats.smooth()} transformation for fitting layered trend
     * lines.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a fresh {@code StatSmooth}
     */
    public static <DF> StatSmooth<DF> smooth() {
        return new StatSmooth<>();
    }

    /**
     * Creates a {@code Stats.summary()} transformation for per-group
     * aggregation.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a fresh {@code StatSummary}
     */
    public static <DF> StatSummary<DF> summary() {
        return new StatSummary<>();
    }

    /**
     * Creates a {@code Stats.cor()} transformation computing the Pearson
     * correlation of an {@code (x, y)} column pair and exposing it as a
     * {@code corr} computed variable for {@code afterStat} label references.
     *
     * @param <DF> the DataFrame type representing the underlying dataset
     * @return a fresh {@code StatCor}
     */
    public static <DF> StatCor<DF> cor() {
        return new StatCor<>();
    }

    /**
     * Starts a fluent {@code Stats.smooth()} over a {@code (x, y)} column pair of
     * any DataFrame type. The concrete extractor is resolved through
     * {@link org.jtaccuino.gog.spi.DataExtractorRegistry} — exactly like the
     * {@code DataExtractor} SPI — so this entry point is dataframe-agnostic and
     * returns the stat's own {@link StatData} model; no concrete frame type
     * leaks out. Configure the fit with the {@code span}/{@code se}/
     * {@code fullrange}… overrides and finish with {@link SmoothBuilder#fit()}:
     * <pre>{@code
     * Stats.smooth(cars, "wt", "mpg")
     *     .lm().se(false).fit();
     * }</pre>
     *
     * @param df       the frame (of any library) holding the columns
     * @param xColumn  the column to fit along
     * @param yColumn  the column to smooth
     * @param <DF>     the DataFrame type underlying the dataset
     * @return a fluent builder
     * @see SmoothBuilder
     */
    public static <DF> SmoothBuilder<DF> smooth(DF df, String xColumn, String yColumn) {
        return new SmoothBuilder<>(df, xColumn, yColumn);
    }

    /**
     * Starts a fluent {@code Stats.summary()} over a {@code (group, value)}
     * column pair of any DataFrame type. The concrete extractor is resolved
     * through {@link org.jtaccuino.gog.spi.DataExtractorRegistry}, so this
     * entry point is dataframe-agnostic and returns the aggregated DataFrame of
     * the source frame's concrete type:
     * <pre>{@code
     * Stats.summary(df, "grp", "val")
     *     .meanSd().fit();
     * }</pre>
     *
     * @param df            the frame (of any library) holding the columns
     * @param groupColumn   the discrete column that groups the rows
     * @param valueColumn   the continuous column to aggregate
     * @param <DF>          the DataFrame type underlying the dataset
     * @return a fluent builder
     * @see SummaryBuilder
     */
    public static <DF> SummaryBuilder<DF> summary(DF df, String groupColumn, String valueColumn) {
        return new SummaryBuilder<>(df, groupColumn, valueColumn);
    }
}
