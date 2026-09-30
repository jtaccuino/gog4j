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
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math4.legacy.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math4.legacy.stat.regression.SimpleRegression;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;

/**
 * {@code Stats.smooth()} statistical transformation: fits a smoothed conditional
 * mean (LOESS or linear model) over a numeric {@code x}×{@code y} pair and emits
 * the fitted curve together with an optional confidence envelope.
 * <p>
 * Like every {@link Stat}, it is a pure data transformation driven through
 * {@link #computeLayer} (or the list-based {@link #fit} convenience for callers
 * that already hold the values). Parameters are the typed
 * {@link SmoothParams}, built from {@link SmoothParams#defaults()} with fluent
 * overrides; the generic {@link StatParams} bag is reserved for interface-driven
 * callers. The heavy lifting lives in {@link SmoothModels}. Obtain instances
 * through the {@link Stats} facade — {@code Stats.smooth()} for the rendering
 * pipeline, {@code Stats.smooth(df, xColumn, yColumn)} for a fluent fit.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatSmooth<DF> implements Stat<DF> {

    /**
     * Constructs a Stats.smooth transformation.
     */
    StatSmooth() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var yCol = aes.y() == null ? null : ext.getColumn(df, aes.y());
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }
        var n = Math.min(xCol.size(), yCol.size());
        var xData = new ArrayList<Double>(n);
        var yData = new ArrayList<Double>(n);
        for (var i = 0; i < n; i++) {
            var rx = xCol.get(i);
            var ry = yCol.get(i);
            if (rx != null && ry != null) {
                xData.add(Values.toDouble(rx));
                yData.add(Values.toDouble(ry));
            }
        }
        return fit(xData, yData, fromBag(params));
    }

    @Override
    public Aes requiredAes() {
        return Aes.aes();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("y", "ymin", "ymax");
    }

    /**
     * The fluent {@code Stats.smooth()} builder, started through
     * {@link Stats#smooth(Object, String, String)}. The {@code (x, y)} columns are
     * read off the frame through the {@link DataExtractorRegistry}-resolved
     * extractor, and each override accumulates a {@link SmoothParams} protected
     * from the caller; {@link #fit()} runs the fit and returns an agnostic
     * {@link StatData}.
     *
     * @param <DF> the DataFrame type underlying the dataset
     */
    public static final class SmoothBuilder<DF> {

        private final DF df;
        private final String xColumn;
        private final String yColumn;
        private SmoothParams params = SmoothParams.defaults();

        SmoothBuilder(DF df, String xColumn, String yColumn) {
            this.df = df;
            this.xColumn = xColumn;
            this.yColumn = yColumn;
        }

        /**
         * Fits a LOESS curve (the default model).
         *
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> loess() {
            return method(SmoothMethod.LOESS);
        }

        /**
         * Fits a straight least-squares line.
         *
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> lm() {
            return method(SmoothMethod.LM);
        }

        /**
         * Sets the smoothing model explicitly.
         *
         * @param method the {@link SmoothMethod} to use
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> method(SmoothMethod method) {
            this.params = params.method(method);
            return this;
        }

        /**
         * Sets the LOESS bandwidth (0.0 to 1.0); smaller values are wigglier.
         *
         * @param span the bandwidth, {@code 0.75} by default
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> span(double span) {
            this.params = params.span(span);
            return this;
        }

        /**
         * Toggles the {@code ymin}/{@code ymax} confidence envelope.
         *
         * @param se {@code true} to emit the envelope, {@code true} by default
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> se(boolean se) {
            this.params = params.se(se);
            return this;
        }

        /**
         * Toggles extrapolation across the {@link #globalRange(double, double)}
         * grid instead of the data range.
         *
         * @param fullrange {@code true} to fit over the global grid
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> fullrange(boolean fullrange) {
            this.params = params.fullrange(fullrange);
            return this;
        }

        /**
         * Sets the {@code [min, max]} grid the curve extrapolates across when
         * {@link #fullrange(boolean)} is enabled.
         *
         * @param min the left edge of the grid
         * @param max the right edge of the grid
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> globalRange(double min, double max) {
            this.params = params.globalRange(min, max);
            return this;
        }

        /**
         * Sets the number of points on the fitted curve.
         *
         * @param n the curve resolution, {@code 80} by default
         * @return this builder for fluent chaining
         */
        public SmoothBuilder<DF> steps(int n) {
            this.params = params.steps(n);
            return this;
        }

        /**
         * Runs the fit and returns the fitted curve — {@code x}/{@code y}/
         * {@code ymin}/{@code ymax} columns — reconstructed into a DataFrame of
         * the source frame's concrete type via resolved
         * {@link DataExtractor}. The caller simply hands this straight to
         * {@code ggplot(…)}; no concrete frame type leaks out of the stat API.
         *
         * @return the fitted, renderable frame
         */
        public DF fit() {
            @SuppressWarnings("unchecked")
            var ext = (DataExtractor<DF>) DataExtractorRegistry.extractorFor(df.getClass());
            if (ext == null) {
                throw new IllegalStateException("No DataExtractor registered for " + df.getClass().getName());
            }
            var xCol = ext.getColumn(df, xColumn);
            var yCol = ext.getColumn(df, yColumn);
            var n = Math.min(xCol.size(), yCol.size());
            var xData = new ArrayList<Double>(n);
            var yData = new ArrayList<Double>(n);
            for (var i = 0; i < n; i++) {
                var rx = xCol.get(i);
                var ry = yCol.get(i);
                if (rx != null && ry != null) {
                    xData.add(Values.toDouble(rx));
                    yData.add(Values.toDouble(ry));
                }
            }
            var result = new StatSmooth<DF>().fit(xData, yData, params);
            return ext.fromStatData(result);
        }
    }

    /**
     * Fits the smooth over already-collected numeric values, with default
     * parameters. Convenience for layers that partition (e.g. per group) before
     * smoothing rather than letting the stat read the frame directly.
     *
     * @param xData raw x-coordinates
     * @param yData raw y-values aligned with {@code xData}
     * @return the fitted curve and envelope as a {@link StatData}
     */
    public StatData fit(double[] xData, double[] yData) {
        return fit(xData, yData, SmoothParams.defaults());
    }

    /**
     * Fits the smooth over already-collected numeric values. Convenience for
     * layers that partition (e.g. per group) before smoothing rather than
     * letting the stat read the frame directly.
     *
     * @param xData  the raw x-coordinates
     * @param yData  the raw y-values aligned with {@code xData}
     * @param params the typed stat parameters
     * @return the fitted curve and envelope as a {@link StatData}
     */
    public StatData fit(double[] xData, double[] yData, SmoothParams params) {
        var res = params.fullrange()
                ? calculateFullRange(xData, yData, params)
                : calculate(xData, yData, params);

        var builder = StatData.builder();
        for (var i = 0; i < res.xPoints().length; i++) {
            builder.add("x", res.xPoints()[i])
                    .add("y", res.yPoints()[i])
                    .add("ymin", res.yMin()[i])
                    .add("ymax", res.yMax()[i]);
        }
        return builder.build();
    }

    /**
     * Fits the smooth over collected values held as boxed lists, with default
     * parameters. Delegates to the primitive-array {@link #fit(double[], double[])}.
     *
     * @param xData raw x-coordinates
     * @param yData raw y-values aligned with {@code xData}
     * @return the fitted curve and envelope as a {@link StatData}
     */
    public StatData fit(List<Double> xData, List<Double> yData) {
        return fit(toArray(xData), toArray(yData));
    }

    /**
     * Fits the smooth over collected values held as boxed lists. Delegates to
     * the primitive-array {@link #fit(double[], double[], SmoothParams)}.
     *
     * @param xData  the raw x-coordinates
     * @param yData  the raw y-values aligned with {@code xData}
     * @param params the typed stat parameters
     * @return the fitted curve and envelope as a {@link StatData}
     */
    public StatData fit(List<Double> xData, List<Double> yData, SmoothParams params) {
        return fit(toArray(xData), toArray(yData), params);
    }

    private static double[] toArray(List<Double> list) {
        var out = new double[list.size()];
        for (var i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static SmoothParams fromBag(StatParams p) {
        return new SmoothParams(
                p.get("method", SmoothMethod.LOESS) instanceof SmoothMethod m ? m : SmoothMethod.LOESS,
                p.getDouble("span", 0.75),
                p.getBoolean("se", true),
                p.getBoolean("fullrange", false),
                p.getDouble("globalMin", Double.NaN),
                p.getDouble("globalMax", Double.NaN),
                p.getInt("steps", 80));
    }

    private static SmoothResult calculate(double[] xData, double[] yData, SmoothParams params) {
        // Sort the data along the X-axis (required for interpolation). Primitive
        // arrays carry no nulls, so only ordering is needed.
        var indices = new Integer[xData.length];
        for (var i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, (a, b) -> Double.compare(xData[a], xData[b]));

        var xArray = new double[xData.length];
        var yArray = new double[yData.length];
        for (var i = 0; i < indices.length; i++) {
            xArray[i] = xData[indices[i]];
            yArray[i] = yData[indices[i]];
        }

        return switch (params.method()) {
            case LM -> SmoothModels.calculateLinearModel(xArray, yArray, params.se());
            case LOESS -> SmoothModels.calculateLoessRegression(xArray, yArray, params.span(), params.se());
        };
    }

    private static SmoothResult calculateFullRange(double[] xData, double[] yData, SmoothParams params) {
        // Train the model on the actual group data only.
        var baseRes = calculate(xData, yData, params);
        var method = params.method();
        var se = params.se();
        var span = params.span();
        var globalMin = params.globalMin();
        var globalMax = params.globalMax();
        var steps = params.steps();

        // Build a uniformly distributed X-grid over the entire plot width.
        var xExtended = new double[steps];
        double stepSize = (globalMax - globalMin) / (steps - 1);
        for (int i = 0; i < steps; i++) {
            xExtended[i] = globalMin + (i * stepSize);
        }

        var yExtrapolated = new double[steps];
        var yMinExtrapolated = new double[steps];
        var yMaxExtrapolated = new double[steps];

        if (method == SmoothMethod.LM) {
            var regression = new SimpleRegression();
            double sumX = 0;
            double sumY = 0;
            for (int i = 0; i < xData.length; i++) {
                regression.addData(xData[i], yData[i]);
                sumX += xData[i];
                sumY += yData[i];
            }
            double xSumSquares = regression.getXSumSquares();
            // A degenerate group (every X equal) has no variance to regress on:
            // instead of a NaN slope, treat it as a flat line through the mean Y.
            boolean degenerate = xSumSquares <= 0.0 || xData.length < 2;
            double meanX = sumX / xData.length;
            double meanY = sumY / xData.length;
            double regressionStandardError = !degenerate && xData.length > 2
                    ? Math.sqrt(regression.getSumSquaredErrors() / (xData.length - 2))
                    : 0.0;

            for (int i = 0; i < steps; i++) {
                yExtrapolated[i] = degenerate ? meanY : regression.predict(xExtended[i]);
                if (se && !degenerate && xData.length > 2 && xSumSquares > 0) {
                    double deviation = xExtended[i] - meanX;
                    double sePrediction = regressionStandardError * Math.sqrt((1.0 / xData.length) + (deviation * deviation) / xSumSquares);
                    yMinExtrapolated[i] = yExtrapolated[i] - (sePrediction * 1.96);
                    yMaxExtrapolated[i] = yExtrapolated[i] + (sePrediction * 1.96);
                } else {
                    yMinExtrapolated[i] = yExtrapolated[i];
                    yMaxExtrapolated[i] = yExtrapolated[i];
                }
            }
        } else {
            // Mathematically reactive LOESS extrapolation: interpolate the base
            // fit, then extend linearly past each edge using the edge slope.
            var loess = new LoessInterpolator(span, 3);
            try {
                var uniqueX = baseRes.xPoints();
                var uniqueY = baseRes.yPoints();
                var loessFn = loess.interpolate(uniqueX, uniqueY);

                double coreMin = uniqueX[0];
                double coreMax = uniqueX[uniqueX.length - 1];

                double leftSlope = 0.0;
                if (uniqueX.length > 1) {
                    leftSlope = (uniqueY[1] - uniqueY[0]) / (uniqueX[1] - uniqueX[0]);
                }
                double rightSlope = 0.0;
                if (uniqueX.length > 1) {
                    int last = uniqueX.length - 1;
                    rightSlope = (uniqueY[last] - uniqueY[last - 1]) / (uniqueX[last] - uniqueX[last - 1]);
                }

                for (int i = 0; i < steps; i++) {
                    double targetX = xExtended[i];

                    if (targetX >= coreMin && targetX <= coreMax) {
                        yExtrapolated[i] = loessFn.value(targetX);
                    } else if (targetX < coreMin) {
                        double deltaX = coreMin - targetX;
                        yExtrapolated[i] = uniqueY[0] - (leftSlope * deltaX);
                    } else {
                        double deltaX = targetX - coreMax;
                        yExtrapolated[i] = uniqueY[uniqueY.length - 1] + (rightSlope * deltaX);
                    }

                    double distToCore = (targetX < coreMin) ? (coreMin - targetX) : (targetX > coreMax ? targetX - coreMax : 0.0);
                    double edgeExpansion = 1.0 + (distToCore * 0.4);
                    yMinExtrapolated[i] = yExtrapolated[i] - (2.5 * edgeExpansion);
                    yMaxExtrapolated[i] = yExtrapolated[i] + (2.5 * edgeExpansion);
                }
            } catch (Exception e) {
                return baseRes;
            }
        }

        return new SmoothResult(xExtended, yExtrapolated, yMinExtrapolated, yMaxExtrapolated);
    }
}
