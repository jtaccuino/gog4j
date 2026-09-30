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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.bin()} statistical transformation: groups a continuous numeric
 * variable into contiguous, non-overlapping bins and emits one row per bin with
 * the bin edges, the bin centre, and the count of observations that fall into
 * it.
 * <p>
 * The binned output ({@code xmin}/{@code xmax}/{@code x}/{@code count}/
 * {@code ncount}/{@code density}/{@code ndensity}/{@code width}) is shared by
 * {@code Geoms.histogram()} (bars of height {@code count}) and
 * {@code Geoms.freqpoly()} (a line through the bin centres) — the same split as
 * by default, so both geometries are pure consumers of one transformation.
 * <p>
 * Bin edges are chosen from the data range with {@link Scale#niceBreaks}, giving
 * "nice" rounding boundaries; an explicit {@code binwidth}, {@code boundary},
 * or {@code center} overrides the default. The computation is exposed as
 * {@link #fit(List, BinParams)} over already-collected values, and through
 * {@link #computeLayer} for interface-driven callers.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatBin<DF> implements Stat<DF> {

    /**
     * Constructs a Stats.bin transformation.
     */
    StatBin() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        if (xCol == null) {
            return StatData.builder().build();
        }
        var xData = xCol.stream()
                .filter(Objects::nonNull)
                .map(Values::toDouble)
                .collect(Collectors.toList());
        return fit(xData, fromBag(params));
    }

    @Override
    public Aes requiredAes() {
        return Aes.aes();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("xmin", "xmax", "x", "count", "ncount", "density", "ndensity", "width");
    }

    /**
     * {@code Stats.bin()} parameters with conventional defaults. Either the
     * requested {@code bins} or an explicit {@code binwidth} fixes the bin size;
     * {@code boundary} and {@code center} shift the edge grid.
     *
     * @param bins     the approximate number of bins over the data range, or
     *                 {@code -1} to derive {@code binwidth} instead
     * @param binwidth the fixed bin width, or {@code NaN} to derive from
     *                 {@code bins}
     * @param boundary the edge alignment point; bins are anchored so an edge
     *                 falls on this value, or {@code NaN} to use the data min
     * @param center   the bin centre alignment point, or {@code NaN} to ignore
     * @param pad      reserve an empty bin on either side of the data range so
     *                 the tallest group sits clear of the panel edge
     */
    public record BinParams(int bins, double binwidth, double boundary, double center, boolean pad) {

        /** {@return the default {@code Stats.bin()} parameters} */
        public static BinParams defaults() {
            return new BinParams(30, Double.NaN, Double.NaN, Double.NaN, false);
        }

        /**
         * {@return a copy with a different bin count}
         *
         * @param b the new approximate bin count
         */
        public BinParams bins(int b) {
            return new BinParams(b, binwidth, boundary, center, pad);
        }

        /**
         * {@return a copy with a fixed bin width}
         *
         * @param w the new fixed bin width, or {@code NaN} to derive from {@code bins}
         */
        public BinParams binwidth(double w) {
            return new BinParams(bins, w, boundary, center, pad);
        }

        /**
         * {@return a copy anchored at a different boundary edge}
         *
         * @param b the new edge alignment point, or {@code NaN} to use the data min
         */
        public BinParams boundary(double b) {
            return new BinParams(bins, binwidth, b, center, pad);
        }

        /**
         * {@return a copy anchored at a different bin centre}
         *
         * @param c the new centre alignment point, or {@code NaN} to ignore
         */
        public BinParams center(double c) {
            return new BinParams(bins, binwidth, boundary, c, pad);
        }

        /**
         * {@return a copy with the padding flag set}
         *
         * @param p {@code true} to reserve an empty bin on either side of the data range
         */
        public BinParams pad(boolean p) {
            return new BinParams(bins, binwidth, boundary, center, p);
        }
    }

    /**
     * Computes the bin-edge grid for a column of x-values under the given
     * parameters. Exposed separately so layers that bin several groups over the
     * same grid (stacked histograms, frequency polygons) can align their edges
     * to one shared set rather than re-deriving them per group.
     *
     * @param xData  the continuous values whose range defines the grid
     * @param params the binning parameters
     * @return the bin edges, {@code [x0, x1, …, xn]} with {@code n} bins
     */
    public double[] edges(List<Double> xData, BinParams params) {
        var stats = xData.stream()
                .mapToDouble(Double::doubleValue)
                .filter(d -> !Double.isNaN(d))
                .summaryStatistics();
        if (stats.getCount() == 0) {
            return new double[0];
        }
        double min = stats.getMin();
        double max = stats.getMax();

        var width = params.binwidth();
        if (Double.isNaN(width)) {
            var nice = Scale.niceBreaks(min, max, Math.max(1, params.bins()));
            width = nice.size() > 1 ? nice.get(1) - nice.get(0) : 1.0;
            if (width <= 0 || !Double.isFinite(width)) {
                width = (max - min) / Math.max(1, params.bins());
                if (width <= 0) width = 1.0;
            }
        }

        double lo = min;
        double hi = max;
        if (params.pad()) {
            lo = Math.floor(min / width) * width - width;
            hi = Math.ceil(max / width) * width + width;
        }

        // Anchor edges so a boundary (or centre) lands where the caller wants.
        double origin;
        if (!Double.isNaN(params.boundary)) {
            origin = params.boundary;
        } else if (!Double.isNaN(params.center)) {
            origin = params.center + width / 2.0;
        } else {
            origin = Math.floor(lo / width) * width;
        }
        var start = origin + Math.floor((lo - origin) / width) * width;
        double w = width;

        int binCount = (int) Math.max(1, Math.ceil((hi - start) / width - 1e-9) + 1);
        return IntStream.rangeClosed(0, binCount)
                .mapToDouble(i -> start + i * w)
                .toArray();
    }

    /**
     * Bins a column of numeric x-values with default parameters.
     *
     * @param xData continuous values to bin
     * @return per-bin rows, or an empty {@link StatData} when {@code xData} is
     *         empty
     */
    public StatData fit(List<Double> xData) {
        return fit(xData, BinParams.defaults());
    }

    /**
     * Bins a column of numeric x-values with the given parameters.
     *
     * @param xData  the continuous values to bin
     * @param params the binning parameters
     * @return per-bin rows, or an empty {@link StatData} when {@code xData} is
     *         empty
     */
    public StatData fit(List<Double> xData, BinParams params) {
        return fitInto(xData, edges(xData, params));
    }

    /**
     * Counts x-values into an externally supplied bin-edge grid (see
     * {@link #edges(List, BinParams)}), so several groups can be tallied over
     * one shared set of bins.
     *
     * @param xData continuous values to bin
     * @param edges the bin edges, {@code [x0, …, xn]} with {@code n} bins
     * @return per-bin rows with {@code count}/{@code density} computed against
     *         {@code xData}'s own total
     */
    public StatData fitInto(List<Double> xData, double[] edges) {
        if (edges.length < 2) {
            return StatData.builder().build();
        }
        var width = edges[1] - edges[0];
        int binCount = edges.length - 1;
        var counts = new double[binCount];

        for (double v : xData) {
            if (Double.isNaN(v)) {
                continue;
            }
            int idx = (int) Math.floor((v - edges[0]) / width);
            if (idx < 0 || idx >= binCount) {
                continue;
            }
            counts[idx] += 1.0;
        }

        double maxCount = Arrays.stream(counts).max().orElse(0.0);
        double total = xData.size();
        var densities = IntStream.range(0, binCount)
                .mapToDouble(i -> total > 0 ? counts[i] / (width * total) : 0.0)
                .toArray();
        double maxDensity = Arrays.stream(densities).max().orElse(0.0);

        var builder = StatData.builder();
        for (var i = 0; i < binCount; i++) {
            double xmin = edges[i];
            double xmax = edges[i + 1];
            builder.add("xmin", xmin)
                    .add("xmax", xmax)
                    .add("x", (xmin + xmax) / 2.0)
                    .add("count", counts[i])
                    .add("ncount", maxCount > 0 ? counts[i] / maxCount : 0.0)
                    .add("density", densities[i])
                    .add("ndensity", maxDensity > 0 ? densities[i] / maxDensity : 0.0)
                    .add("width", width);
        }
        return builder.build();
    }

    private static BinParams fromBag(StatParams p) {
        var width = p.contains("binwidth") ? p.getDouble("binwidth", Double.NaN) : Double.NaN;
        return new BinParams(
                p.getInt("bins", 30),
                width,
                p.contains("boundary") ? p.getDouble("boundary", Double.NaN) : Double.NaN,
                p.contains("center") ? p.getDouble("center", Double.NaN) : Double.NaN,
                p.getBoolean("pad", false));
    }
}
