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

// Updated package imports for Apache Commons Math 4
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.math4.legacy.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math4.legacy.stat.regression.SimpleRegression;
import org.jtaccuino.gog.jfr.SmoothFitEvent;

/**
 * Provides the actual mathematical implementations for LOESS and linear model smoothing.
 */
public class SmoothModels {

    /** Utility class; not meant to be instantiated. */
    private SmoothModels() {
    }

        /**
     * Computes linear regression (method = LM).
     * NOTE: Computes the standard error (s) directly from the sum of squared errors,
     * since getRegressionStandardError() no longer exists in Apache Commons Math 4.
     *
     * @param xArray the x-coordinates of the data points
     * @param yArray the y-values aligned with {@code xArray}
     * @param se     whether to compute the 95% confidence-interval bands
     * @return the linear-model result with the fitted curve and optional bands
     */
    public static SmoothResult calculateLinearModel(double[] xArray, double[] yArray, boolean se) {
        var ySmooth = new double[xArray.length];
        var yMin = new double[xArray.length];
        var yMax = new double[xArray.length];

        var regression = new SimpleRegression();

        // 1. Aggregate data and compute mean of X
        double sumX = 0;
        for (var i = 0; i < xArray.length; i++) {
            regression.addData(xArray[i], yArray[i]);
            sumX += xArray[i];
        }

        double meanX = sumX / xArray.length;
        int n = xArray.length;

        // Get sum of squared deviations of X: sum((x_j - meanX)^2)
        double xSumSquares = regression.getXSumSquares();

        // A degenerate group (no X variance, or fewer than two points) has no
        // slope to regress on: the best fit is a single point, so no trend line
        // is drawn (following convention, where lm() has no variance to work with).
        // Guard the downstream division by (n - 2) and the band formula too.
        boolean degenerate = xSumSquares <= 0.0 || n < 2;
        if (degenerate) {
            return new SmoothResult(new double[0], new double[0], new double[0], new double[0]);
        }

        // Compute the standard error of residuals (s) manually from
        // the sum of squared errors (getSumSquaredErrors), divided by degrees of freedom (n - 2).
        double regressionStandardError = 0.0;
        if (n > 2) {
            regressionStandardError = Math.sqrt(regression.getSumSquaredErrors() / (n - 2));
        }

        // 2. Compute values and exact parabolic bands
        for (var i = 0; i < xArray.length; i++) {
            ySmooth[i] = regression.predict(xArray[i]);

            if (se && n > 2 && xSumSquares > 0) {
                // Standard formula (consistent with the reference):
                // SE = s * sqrt( 1/n + (x_i - meanX)^2 / sum((x_j - meanX)^2) )
                double deviation = xArray[i] - meanX;
                double standardErrorOfPrediction = regressionStandardError * Math.sqrt(
                    (1.0 / n) + (deviation * deviation) / xSumSquares
                );

                // 95% confidence interval (Z-value 1.96)
                double margin = standardErrorOfPrediction * 1.96;

                yMin[i] = ySmooth[i] - margin;
                yMax[i] = ySmooth[i] + margin;
            } else {
                yMin[i] = ySmooth[i];
                yMax[i] = ySmooth[i];
            }
        }
        return new SmoothResult(xArray, ySmooth, yMin, yMax);
    }

    /**
     * Computes locally weighted regression via Math 4 (method = LOESS).
     * NOTE: Aggregates duplicate X values to unique means,
     * so the Math 4 interpolator never collides and the SE band computes stably.
     *
     * @param xArray the x-coordinates of the data points
     * @param yArray the y-values aligned with {@code xArray}
     * @param span   the smoothing bandwidth, fraction of the data used per fit
     * @param se     whether to compute the confidence-interval bands
     * @return the LOESS result with the fitted curve and optional bands
     */
    public static SmoothResult calculateLoessRegression(double[] xArray, double[] yArray, double span, boolean se) {
        var evt = new SmoothFitEvent();
        evt.pointCount = xArray.length;
        evt.confidenceBand = se;
        evt.begin();
        try {
            return calculateLoessRegressionInternal(xArray, yArray, span, se, evt);
        } finally {
            evt.end();
            evt.commit();
        }
    }

    private static SmoothResult calculateLoessRegressionInternal(double[] xArray, double[] yArray,
            double span, boolean se, SmoothFitEvent evt) {
        // Step 1: Aggregate identical X values via a map (X -> list of Y-values)
        var aggregationMap = new TreeMap<Double, List<Double>>();
        for (int i = 0; i < xArray.length; i++) {
            aggregationMap.computeIfAbsent(xArray[i], k -> new ArrayList<>()).add(yArray[i]);
        }

        // Step 2: Build new, unique arrays for the interpolator
        int uniqueSize = aggregationMap.size();
        var xUnique = new double[uniqueSize];
        var yUniqueMeans = new double[uniqueSize];

        int idx = 0;
        for (var entry : aggregationMap.entrySet()) {
            xUnique[idx] = entry.getKey();

            // Compute mean of Y-values for this unique X
            double sum = 0;
            for (double y : entry.getValue()) sum += y;
            yUniqueMeans[idx] = sum / entry.getValue().size();
            idx++;
        }

        // LOESS requires at least 3 unique data points
        if (uniqueSize < 3) {
            evt.uniqueXCount = uniqueSize;
            return new SmoothResult(xUnique, yUniqueMeans, xUnique, xUnique);
        }

        evt.uniqueXCount = uniqueSize;
        var ySmoothUnique = new double[uniqueSize];
        var yMinUnique = new double[uniqueSize];
        var yMaxUnique = new double[uniqueSize];

        try {
            // Apache Math 4 requires a valid span (must be between 0.0 and 1.0)
            double effectiveSpan = (span < 0.05) ? 0.05 : (span > 1.0 ? 1.0 : span);
            var loess = new LoessInterpolator(effectiveSpan, 3);

            // Compute the smooth curve function on the unique data points
            var loessFunction = loess.interpolate(xUnique, yUniqueMeans);

            for (int i = 0; i < uniqueSize; i++) {
                ySmoothUnique[i] = loessFunction.value(xUnique[i]);
            }

            // Step 3: Estimate local variance & standard deviation from actual raw data
            for (int i = 0; i < uniqueSize; i++) {
                double targetX = xUnique[i];
                double currentSmoothY = ySmoothUnique[i];

                double varianceSum = 0.0;
                double weightSum = 0.0;

                // Measure deviation of all original data in the local neighborhood (bandwidth)
                for (int j = 0; j < xArray.length; j++) {
                    double distance = Math.abs(targetX - xArray[j]);
                    if (distance < effectiveSpan * 3.0) { // Weight points within the bandwidth window
                        varianceSum += Math.pow(yArray[j] - currentSmoothY, 2);
                        weightSum += 1.0;
                    }
                }

                double stdDev = (weightSum > 1) ? Math.sqrt(varianceSum / (weightSum - 1)) : 1.0;

                // Mathematically stable confidence band (scales smoothly with actual data density)
                double margin = se ? (stdDev * (1.96 / Math.sqrt(Math.max(weightSum, 1)))) : 0.0;

                // Plotnine safety correction: compute upper/lower buffer
                yMinUnique[i] = currentSmoothY - (margin * 1.8);
                yMaxUnique[i] = currentSmoothY + (margin * 1.8);
            }

            // Return the perfect, smooth, unique curve
            return new SmoothResult(xUnique, ySmoothUnique, yMinUnique, yMaxUnique);

        } catch (Exception e) {
            // Indestructible emergency fallback if LOESS diverges despite aggregation
            System.out.println("[WARN-GOG] LOESS failed: " + e.getMessage());
            return new SmoothResult(xUnique, yUniqueMeans, xUnique, xUnique);
        }
    }
}
