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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;

/**
 * {@code Stats.summary()} statistical transformation: collapses a numeric
 * {@code y} column grouped by a discrete {@code x} (or {@code group}) into one
 * aggregated row per group holding a central value ({@code y}) and a range
 * ({@code ymin}/{@code ymax}) — the engine behind error bars, crossbars,
 * lineranges, pointranges and ribbons.
 * <p>
 * The output columns ({@code x}, {@code y}, {@code ymin}, {@code ymax} and
 * optionally {@code n}) are consumed by the range/error geoms, which draw what
 * they need (whisker, band, line, point) around those three values. The
 * aggregation is driven by a {@link SummaryKind} chosen via fluent
 * {@code Stats.summary(df, group, value)} builder, mirroring the
 * {@code Stats.smooth(df, x, y)} pattern so the result is a DataFrame of the
 * source frame's concrete type.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatSummary<DF> implements Stat<DF> {

    /**
     * Constructs a Stats.summary transformation.
     */
    StatSummary() {
    }

    /**
     * The aggregate reported for each {@code y} group.
     */
    public enum SummaryKind {
        /** Central {@code mean}, range {@code mean ± sd}. */
        MEAN_SD,
        /** Central {@code mean}, range {@code mean ± se} (sd / √n). */
        MEAN_SE,
        /** Central {@code mean}, range {@code mean ± 1.96·se} (95% CI). */
        MEAN_CI,
        /** Central {@code median}, range {@code Q1..Q3} (IQR). */
        MEDIAN_IQR,
        /** Central {@code mean}, range {@code min..max}. */
        MIN_MAX
    }

    /**
     * The typed, immutable parameters of a {@link StatSummary} computation.
     *
     * @param kind the aggregate reported per group, {@link SummaryKind#MEAN_SD}
     *             by default
     */
    public record SummaryParams(SummaryKind kind) {

        /** {@return the default {@code Stats.summary()} parameters} */
        public static SummaryParams defaults() {
            return new SummaryParams(SummaryKind.MEAN_SD);
        }

        /**
         * {@return a copy with a different aggregation kind}
         *
         * @param k the new aggregation kind
         */
        public SummaryParams kind(SummaryKind k) {
            return new SummaryParams(k);
        }
    }

    /**
     * The fluent {@code Stats.summary()} builder, started through
     * {@link Stats#summary(Object, String, String)}. The {@code (group, value)}
     * columns are read off the frame through the resolved extractor, aggregated
     * by the chosen {@link SummaryKind}, and {@link #fit()} returns a frame of
     * the source type with columns {@code x}{@code y}{@code ymin}{@code ymax}
     * (and {@code n}).
     *
     * @param <DF> the DataFrame type underlying the dataset
     */
    public static final class SummaryBuilder<DF> {

        private final DF df;
        private final String groupColumn;
        private final String valueColumn;
        private SummaryParams params = SummaryParams.defaults();

        SummaryBuilder(DF df, String groupColumn, String valueColumn) {
            this.df = df;
            this.groupColumn = groupColumn;
            this.valueColumn = valueColumn;
        }

        /**
         * Uses {@code mean ± sd} as the aggregate.
         *
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> meanSd() {
            return kind(SummaryKind.MEAN_SD);
        }

        /**
         * Uses {@code mean ± se} as the aggregate.
         *
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> meanSe() {
            return kind(SummaryKind.MEAN_SE);
        }

        /**
         * Uses {@code mean} with a 95% confidence interval.
         *
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> meanCi() {
            return kind(SummaryKind.MEAN_CI);
        }

        /**
         * Uses {@code median} with the interquartile range.
         *
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> medianIqr() {
            return kind(SummaryKind.MEDIAN_IQR);
        }

        /**
         * Uses {@code mean} with the observed {@code min..max} range.
         *
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> minMax() {
            return kind(SummaryKind.MIN_MAX);
        }

        /**
         * Sets the aggregation kind explicitly.
         *
         * @param kind the aggregation to apply
         * @return this builder for fluid chaining
         */
        public SummaryBuilder<DF> kind(SummaryKind kind) {
            this.params = params.kind(kind);
            return this;
        }

        /**
         * Runs the aggregation and returns the resulting per-group frame —
         * {@code x}/{@code y}/{@code ymin}/{@code ymax} — reconstructed into
         * the source frame's concrete type.
         *
         * @return the aggregated, renderable frame
         */
        public DF fit() {
            @SuppressWarnings("unchecked")
            var ext = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
            if (ext == null) {
                throw new IllegalStateException("No DataExtractor registered for " + df.getClass().getName());
            }
            var groupCol = ext.getColumn(df, groupColumn);
            var valueCol = ext.getColumn(df, valueColumn);
            var groups = new ArrayList<Object>(groupCol.size());
            var values = new ArrayList<Double>(groupCol.size());
            for (var i = 0; i < groupCol.size(); i++) {
                var rg = groupCol.get(i);
                var rv = valueCol.get(i);
                if (rg != null && rv != null) {
                    groups.add(rg);
                    values.add(Values.toDouble(rv));
                }
            }
            var result = new StatSummary<DF>().fit(groups, values, params);
            return ext.fromStatData(result);
        }
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var yCol = aes.y() == null ? null : ext.getColumn(df, aes.y());
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }
        var groups = new ArrayList<Object>(xCol.size());
        var values = new ArrayList<Double>(xCol.size());
        for (var i = 0; i < xCol.size(); i++) {
            var rx = xCol.get(i);
            var ry = yCol.get(i);
            if (rx != null && ry != null) {
                groups.add(rx);
                values.add(Values.toDouble(ry));
            }
        }
        return fit(groups, values, fromBag(params));
    }

    @Override
    public Aes requiredAes() {
        return Aes.aes();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("y", "ymin", "ymax", "n");
    }

    /**
     * Aggregates {@code y} values grouped by equal {@code x} category, emitting
     * one row per group with the central value and range of the configured
     * {@link SummaryKind}.
     *
     * @param groups the group (x) label for each observation, aligned with
     *               {@code values}
     * @param values the continuous values to aggregate
     * @param params the aggregation parameters
     * @return per-group rows, or an empty {@link StatData} when {@code values}
     *         is empty
     */
    public StatData fit(List<Object> groups, List<Double> values, SummaryParams params) {
        var byGroup = new LinkedHashMap<Object, List<Double>>();
        for (var i = 0; i < values.size(); i++) {
            var v = values.get(i);
            if (Double.isNaN(v)) {
                continue;
            }
            byGroup.computeIfAbsent(groups.get(i), k -> new ArrayList<>()).add(v);
        }
        if (byGroup.isEmpty()) {
            return StatData.builder().build();
        }

        var order = byGroup.keySet().stream()
                .sorted((a, b) -> a.toString().compareTo(b.toString()))
                .toList();

        var builder = StatData.builder();
        for (var group : order) {
            var vals = byGroup.get(group);
            double y;
            double ymin;
            double ymax;
            int n = vals.size();
            double mean = mean(vals);
            double sd = n > 1 ? stdDev(vals, mean) : 0.0;
            double se = n > 0 ? sd / Math.sqrt(n) : 0.0;

            switch (params.kind()) {
                case MEAN_SD -> {
                    y = mean;
                    ymin = mean - sd;
                    ymax = mean + sd;
                }
                case MEAN_SE -> {
                    y = mean;
                    ymin = mean - se;
                    ymax = mean + se;
                }
                case MEAN_CI -> {
                    y = mean;
                    ymin = mean - 1.96 * se;
                    ymax = mean + 1.96 * se;
                }
                case MEDIAN_IQR -> {
                    y = median(vals);
                    ymin = quantile(vals, 0.25);
                    ymax = quantile(vals, 0.75);
                }
                case MIN_MAX -> {
                    y = mean;
                    ymin = min(vals);
                    ymax = max(vals);
                }
                default -> throw new IllegalStateException("Unhandled summary kind: " + params.kind());
            }

            builder.add("x", group)
                    .add("y", y)
                    .add("ymin", Math.min(ymin, ymax))
                    .add("ymax", Math.max(ymin, ymax))
                    .add("n", n);
        }
        return builder.build();
    }

    private static double mean(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static double stdDev(List<Double> v, double mean) {
        double sumSq = v.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum();
        return Math.sqrt(sumSq / (v.size() - 1));
    }

    private static double min(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);
    }

    private static double max(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).max().orElse(-Double.MAX_VALUE);
    }

    private static double median(List<Double> v) {
        return quantile(v, 0.5);
    }

    private static double quantile(List<Double> v, double q) {
        var sorted = new ArrayList<>(v);
        sorted.sort(Double::compareTo);
        return quantileSorted(sorted, q);
    }

    private static double quantileSorted(List<Double> sorted, double q) {
        int n = sorted.size();
        double pos = q * (n + 1) - 1;
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo < 0) return sorted.get(0);
        if (hi >= n) return sorted.get(n - 1);
        double frac = pos - lo;
        return sorted.get(lo) * (1 - frac) + sorted.get(hi) * frac;
    }

    private static SummaryParams fromBag(StatParams p) {
        var v = p.get("kind", SummaryKind.MEAN_SD);
        return new SummaryParams(v instanceof SummaryKind k ? k : SummaryKind.MEAN_SD);
    }
}
