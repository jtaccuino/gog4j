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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jtaccuino.gog.layer.PanelContext;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.stat.StatData;

/**
 * The typed value bound to an aesthetic — the stored form behind {@link Aes}'s
 * lightweight string setters. An {@code AesValue} is one of:
 * <ul>
 *   <li>{@code raw(...)} — a literal column name or value in the model data
 *       (or a wire string),</li>
 *   <li>{@code afterStat(...)} — a reference to a {@link ComputedVariable} the
 *       attached {@code stat} computes, or to any named stat output column,
 *       following the {@code afterStat(...)},</li>
 *   <li>{@code afterStat(...)} with an expression — an inline computation over
 *       the {@link StatData} the stat produced,</li>
 *   <li>{@code afterScale(...)} — a reference to another aesthetic's
 *       scale-mapped value, following the {@code afterScale(...)},</li>
 *   <li>{@code stage(...)} — a start value threaded through an optional
 *       {@code afterStat} and an optional {@code afterScale} stage, mirroring
 *       the {@code stage(...)}.</li>
 * </ul>
 * <p>
 * Values render to the same wire form {@link Aes} always produced: a bare value
 * for {@code raw}, {@code ::name} for a stat reference, {@code ::expr} for an
 * inline expression, and {@code @name} for an after-scale reference. The {@code
 * resolve} seam carries the panel context needed to unwind stat, scale and
 * staged references into a per-row value.
 *
 * @see Aes
 */
public sealed interface AesValue permits AesValue.Raw, AesValue.AfterStat,
        AesValue.AfterStatExpr, AesValue.AfterScale, AesValue.Stage {

    /** Prefix marking an aesthetic value as a stat-computed reference ({@code ::}). */
    String STAT_PREFIX = "::";

    /** Prefix marking an aesthetic value as an after-scale reference ({@code @}). */
    String SCALE_PREFIX = "@";

    /**
     * Wraps a literal column name or a constant value. This is the type of every
     * simple {@code aes().x("col")} style setter; a value that already begins
     * with {@value #STAT_PREFIX} or {@value #SCALE_PREFIX} is parsed into the
     * corresponding reference so wire values round-trip.
     *
     * @param value the column name or literal value
     * @return the raw aesthetic value
     */
    static AesValue raw(String value) {
        if (value != null && value.startsWith(STAT_PREFIX)) {
            return new AfterStat(value.substring(STAT_PREFIX.length()));
        }
        if (value != null && value.startsWith(SCALE_PREFIX)) {
            return new AfterScale(Aesthetic.of(value.substring(SCALE_PREFIX.length())));
        }
        return new Raw(value);
    }

    /**
     * References a stat-computed variable ({@code afterStat(COUNT)}), mirroring
     * the {@code afterStat(...)}. The value resolves against the output
     * of the stat attached to the layer's geometry.
     *
     * @param variable the computed stat output variable
     * @return the after-stat reference
     */
    static AesValue afterStat(ComputedVariable variable) {
        return new AfterStat(variable.column());
    }

    /**
     * References a stat output column by name — the escaping hatch for extension
     * stats that compute variables beyond the built-in {@link ComputedVariable}s.
     *
     * @param column the stat output column name (without the {@value #STAT_PREFIX} prefix)
     * @return the after-stat reference
     */
    static AesValue afterStat(String column) {
        return new AfterStat(column);
    }

    /**
     * Computes an aesthetic value inline from the stat output, following the
     * {@code afterStat(expr)}. The expression receives the {@link StatData} the
     * attached stat produced — its columns are the model columns mapped by the
     * layer plus the computed variables ({@code count}, {@code density}, ...) —
     * and returns one value per row. The {@code requires} columns are validated
     * against {@link StatData#columnNames()} when the expression is evaluated.
     *
     * @param compute  the expression, mapping stat output to a value column
     * @param requires the stat output columns the expression reads, used for
     *                 early validation
     * @return the after-stat expression
     */
    static AesValue afterStat(Function<StatData, List<?>> compute, String... requires) {
        return new AfterStatExpr(compute, requires == null ? List.of() : List.of(requires));
    }

    /**
     * References another aesthetic's value after its scale was applied, mirroring
     * the {@code afterScale(...)}. For a colour aesthetic the resolved
     * value is the mapped {@code Color}; for a position aesthetic the value in
     * the referenced scale's transformed space.
     *
     * @param aesthetic the referenced aesthetic name ({@code color},
     *                  {@code fill}, {@code shape}, {@code linetype}, {@code x},
     *                  {@code y}, ...)
     * @return the after-scale reference
     */
    static AesValue afterScale(Aesthetic aesthetic) {
        return new AfterScale(aesthetic);
    }

    /**
     * References another aesthetic's value after its scale was applied.
     *
     * @param aesthetic the referenced aesthetic name (e.g. {@code "color"},
     *                  {@code "fill"}, {@code "x"}, {@code "y"})
     * @return the after-scale reference
     * @throws IllegalArgumentException when {@code aesthetic} is not a known aesthetic
     */
    static AesValue afterScale(String aesthetic) {
        return new AfterScale(Aesthetic.of(aesthetic));
    }

    /**
     * Begins a staged aesthetic: {@code stage(start)} may pin down an
     * {@code afterStat} and an {@code afterScale} stage, following the
     * {@code stage(start, afterStat = ..., afterScale = ...)}. The start value
     * feeds the after-stat stage, whose output feeds the after-scale stage.
     *
     * @param start the value the pipeline starts from (a model column, a
     *              stat reference, or a constant)
     * @return the stage builder for chaining {@code afterStat} and {@code afterScale}
     */
    static StageBuilder stage(AesValue start) {
        return new StageBuilder(start);
    }

    /**
     * Begins a staged aesthetic from a string wire value (a {@code raw} value or
     * a {@code ::}/{@code @}-prefixed reference).
     *
     * @param start the wire value the pipeline starts from
     * @return the stage builder for chaining {@code afterStat} and {@code afterScale}
     */
    static StageBuilder stage(String start) {
        return new StageBuilder(raw(start));
    }

    /**
     * The bare column name an aesthetic reads from the model frame, when it
     * reads one at all: the value for {@link Raw}, and {@code null} for every
     * referential form ({@code afterStat}/{@code afterScale}/{@code stage}),
     * which resolve against stat output or scales instead of raw columns.
     *
     * @param value the aesthetic value, or {@code null}
     * @return the raw model column name, or {@code null}
     */
    static String rawColumn(AesValue value) {
        return value != null && value.isRaw() ? value.render() : null;
    }

    /**
     * Renders this value in the wire form {@link Aes} exposes through its string
     * getters: the bare value, {@code ::name}, {@code ::expr}, or {@code @name}.
     *
     * @return the wire representation
     */
    String render();

    /**
     * Whether this value is a plain model column or literal, resolved directly
     * against the layer's raw data.
     *
     * @return {@code true} for a {@link Raw} value
     */
    default boolean isRaw() {
        return false;
    }

    /**
     * Whether this value depends on the attached {@code stat}'s output.
     *
     * @return {@code true} for {@link AfterStat}, {@link AfterStatExpr}, and
     *         {@link Stage} values carrying an after-stat stage
     */
    default boolean referencesStat() {
        return false;
    }

    /**
     * Whether this value depends on another aesthetic's scale-mapped value.
     *
     * @return {@code true} for {@link AfterScale} and {@link Stage} values
     *         carrying an after-scale stage
     */
    default boolean referencesScale() {
        return false;
    }

    /**
     * The bare column this value reads, when it reads a single column: the raw
     * value for {@link Raw}, the output column for {@link AfterStat}. Returns
     * {@code null} when the value is not a single-column read.
     *
     * @return the column name, or {@code null}
     */
    default String column() {
        return null;
    }

    /**
     * Resolves this value to its per-row value in the context of a panel render.
     * {@link Raw} returns the raw model value, {@link AfterStat} the stat output
     * cell, {@link AfterStatExpr} the expression result, {@link AfterScale} the
     * referenced aesthetic's scale-mapped value, and {@link Stage} the value
     * threaded through its stages. The returned object is a column value (a
     * {@code Number}, {@code String}, date, ...) for data-space references and a
     * scale-mapped value (e.g. a {@code Color}) for after-scale references. The
     * default implementation returns {@code null}.
     *
     * @param ctx the resolution context for this row
     * @param row the zero-based row index
     * @return the resolved value, or {@code null}
     */
    default Object resolve(AesResolution<?> ctx, int row) {
        return null;
    }

    /**
     * Resolution context handed to {@link AesValue#resolve}: the panel being
     * rendered, the effective {@link Aes} mapping, the attached stat's output
     * (when the layer consumes one), the extracted raw column for the aesthetic
     * being resolved, and a scratch list tracking the references currently being
     * unwound (used to detect {@code afterScale} reference cycles).
     *
     * @param panel      the panel-scoped context with scales, coordinate system
     *                   and partition data
     * @param aes        the effective aesthetic mapping for the layer
     * @param statData   the attached stat's output for this panel, or {@code null}
     * @param rawValues  the raw model values of the aesthetic being resolved
     * @param inFlight   the in-progress reference chain (never {@code null}; a
     *                   freshly allocated list per render pass)
     * @param <DF>       the DataFrame type
     */
    record AesResolution<DF>(PanelContext<DF> panel, Aes aes, StatData statData,
                             List<?> rawValues, List<Aesthetic> inFlight) {

        /**
         * The panel's resolved scales.
         *
         * @return the resolved scales
         */
        public ResolvedScales<DF> scales() {
            return panel.plot().scales();
        }
    }

    /**
     * The named variables a {@code stat} can compute and expose as aesthetic
     * targets via {@link #afterStat(ComputedVariable)}. Mirrors the default's
     * "computed variables" list per stat: {@code count} and {@code ncount}
     * come from {@code Stats.count()}/{@code Stats.bin()}, {@code density} and
     * {@code ndensity} from binning/density estimation, {@code prop} from
     * {@code Stats.count()}, and {@code n} from {@code Stats.summary()}, and
     * {@code corr} from {@code Stats.cor()}.
     */
    enum ComputedVariable {
        /** Number of observations in a bin or category ({@code Stats.count()}, {@code Stats.bin()}). */
        COUNT("count"),
        /** {@code count} normalised to the largest bin ({@code Stats.bin()}). */
        NCOUNT("ncount"),
        /** Probability density per bin ({@code Stats.bin()}). */
        DENSITY("density"),
        /** {@code density} normalised to the largest bin ({@code Stats.bin()}). */
        NDENSITY("ndensity"),
        /** Bin width in data units ({@code Stats.bin()}). */
        WIDTH("width"),
        /** Proportion of a category within its group ({@code Stats.count()}). */
        PROP("prop"),
        /** Number of observations aggregated per group ({@code Stats.summary()}). */
        N("n"),
        /** Pearson correlation of the mapped (x, y) pair ({@code Stats.cor()}). */
        CORR("corr");

        private final String column;

        ComputedVariable(String column) {
            this.column = column;
        }

        /**
         * The column name this variable is exposed as in the stat output.
         *
         * @return the output column name
         */
        public String column() {
            return column;
        }
    }

    /**
     * A plain model column or literal. Rendered as the bare value.
     *
     * @param value the column name or literal
     */
    record Raw(String value) implements AesValue {

        @Override
        public String render() {
            return value;
        }

        @Override
        public boolean isRaw() {
            return true;
        }

        @Override
        public String column() {
            return value;
        }

        @Override
        public Object resolve(AesResolution<?> ctx, int row) {
            return rowInRange(ctx.rawValues(), row) ? ctx.rawValues().get(row) : null;
        }
    }

    /**
     * A reference to a stat output column. Rendered as {@code ::column}.
     *
     * @param column the stat output column name
     */
    record AfterStat(String column) implements AesValue {

        @Override
        public String render() {
            return STAT_PREFIX + column;
        }

        @Override
        public boolean referencesStat() {
            return true;
        }

        @Override
        public String column() {
            return column;
        }

        @Override
        public Object resolve(AesResolution<?> ctx, int row) {
            if (ctx.statData() == null) {
                return null;
            }
            var col = ctx.statData().column(column);
            return col != null && row >= 0 && row < col.size() ? col.get(row) : null;
        }
    }

    /**
     * An inline expression computed over the stat output. Rendered as
     * {@code ::expr}.
     *
     * @param compute  the expression, mapping {@link StatData} to a value column
     * @param requires the stat output columns the expression reads
     */
    record AfterStatExpr(Function<StatData, List<?>> compute, List<String> requires)
            implements AesValue {

        @Override
        public String render() {
            return STAT_PREFIX + "expr";
        }

        @Override
        public boolean referencesStat() {
            return true;
        }

        @Override
        public String column() {
            return requires().isEmpty() ? null : requires().get(0);
        }

        @Override
        public Object resolve(AesResolution<?> ctx, int row) {
            if (ctx.statData() == null) {
                return null;
            }
            var computed = evaluate(ctx.statData());
            return rowInRange(computed, row) ? computed.get(row) : null;
        }

        /**
         * Computes the full output column of this expression against the given
         * stat output, validating that every {@code requires} column is present.
         *
         * @param statData stat output
         * @return the computed value column
         * @throws IllegalArgumentException when a required column is missing
         */
        public List<?> evaluate(StatData statData) {
            for (var requirement : requires()) {
                if (!statData.columnNames().contains(requirement)) {
                    throw new IllegalArgumentException(
                            "afterStat expression requires column '" + requirement
                                    + "' but the stat output only provides "
                                    + statData.columnNames());
                }
            }
            return compute().apply(statData);
        }
    }

    /**
     * A reference to another aesthetic's value after its scale was applied.
     * Rendered as {@code @aesthetic}.
     *
     * @param aesthetic the referenced aesthetic
     */
    record AfterScale(Aesthetic aesthetic) implements AesValue {

        @Override
        public String render() {
            return SCALE_PREFIX + aesthetic.key();
        }

        @Override
        public boolean referencesScale() {
            return true;
        }

        @Override
        public Object resolve(AesResolution<?> ctx, int row) {
            return AesValueSupport.resolveReference(ctx, aesthetic, null, row);
        }
    }

    /**
     * A start value threaded through optional after-stat and after-scale stages.
     *
     * @param start      the value the pipeline starts from
     * @param afterStat  the after-stat stage, or {@code null}
     * @param afterScale the after-scale stage, or {@code null}
     */
    record Stage(AesValue start, AesValue afterStat, AesValue afterScale) implements AesValue {

        @Override
        public String render() {
            if (afterStat != null) {
                return afterStat.render();
            }
            if (afterScale != null) {
                return afterScale.render();
            }
            return start.render();
        }

        @Override
        public boolean referencesStat() {
            return afterStat != null;
        }

        @Override
        public boolean referencesScale() {
            return afterScale != null;
        }

        @Override
        public String column() {
            return afterStat != null ? afterStat.column() : start.column();
        }

        @Override
        public Object resolve(AesResolution<?> ctx, int row) {
            var value = start.resolve(ctx, row);
            if (afterStat != null) {
                var statValue = afterStat.resolve(ctx, row);
                if (statValue != null) {
                    value = statValue;
                }
            }
            if (afterScale instanceof AfterScale scale) {
                return AesValueSupport.mapThroughScale(ctx, scale.aesthetic(), value,
                        afterStat != null ? afterStat : start);
            }
            return value;
        }
    }

    /**
     * Builds a {@link Stage} value with an orderly (builder-style) API:
     * {@code stage(start).afterStat(...).afterScale(...).build()}.
     */
    final class StageBuilder {

        private final AesValue start;
        private AesValue afterStat;
        private AesValue afterScale;

        StageBuilder(AesValue start) {
            if (start == null) {
                throw new IllegalArgumentException("stage() requires a start value");
            }
            this.start = start;
        }

        /**
         * Pins the after-stat stage to a computed variable.
         *
         * @param variable the computed stat output variable
         * @return this builder
         */
        public StageBuilder afterStat(ComputedVariable variable) {
            return afterStat(variable.column());
        }

        /**
         * Pins the after-stat stage to a named stat output column.
         *
         * @param column the stat output column name
         * @return this builder
         */
        public StageBuilder afterStat(String column) {
            checkAfterStat();
            this.afterStat = AesValue.afterStat(column);
            return this;
        }

        /**
         * Pins the after-stat stage to an inline expression.
         *
         * @param compute  the expression
         * @param requires the stat output columns the expression reads
         * @return this builder
         */
        public StageBuilder afterStat(Function<StatData, List<?>> compute, String... requires) {
            checkAfterStat();
            this.afterStat = AesValue.afterStat(compute, requires);
            return this;
        }

        /**
         * Pins the after-scale stage to another aesthetic's scale-mapped value.
         *
         * @param aesthetic the referenced aesthetic
         * @return this builder
         */
        public StageBuilder afterScale(Aesthetic aesthetic) {
            checkAfterScale();
            this.afterScale = AesValue.afterScale(aesthetic);
            return this;
        }

        /**
         * Pins the after-scale stage to another aesthetic's scale-mapped value.
         *
         * @param aesthetic the referenced aesthetic name
         * @return this builder
         * @throws IllegalArgumentException when {@code aesthetic} is not a known aesthetic
         */
        public StageBuilder afterScale(String aesthetic) {
            checkAfterScale();
            this.afterScale = AesValue.afterScale(aesthetic);
            return this;
        }

        /**
         * Completes the staged value.
         *
         * @return the built {@link Stage}
         */
        public AesValue build() {
            return new Stage(start, afterStat, afterScale);
        }

        private void checkAfterStat() {
            if (afterStat != null) {
                throw new IllegalStateException("stage() afterStat already set");
            }
        }

        private void checkAfterScale() {
            if (afterScale != null) {
                throw new IllegalStateException("stage() afterScale already set");
            }
        }
    }

    /**
     * Whether an index lies within a list, guarding against empty or short
     * columns.
     */
    private static boolean rowInRange(List<?> values, int row) {
        return values != null && row >= 0 && row < values.size();
    }
}
