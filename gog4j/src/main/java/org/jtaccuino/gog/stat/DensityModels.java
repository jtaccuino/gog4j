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
import java.util.Collections;
import java.util.List;

/**
 * One-dimensional kernel density estimation helpers for the 3D distribution
 * statistic — the Java counterpart of {@code stats::density()} (bandwidth
 * rules, kernels and grid sampling), used by {@link StatDistributions3d} for
 * the per-slice 1-D densities of its 3-D ridgelines.
 */
public final class DensityModels {

    private DensityModels() {
    }

    /**
     * A sampled 1-D density estimate.
     *
     * @param values the x coordinates the density is sampled at
     * @param y      the density per sample
     * @param bw     the bandwidth actually used
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only carrier
    public record Estimate(double[] values, double[] y, double bw) {
    }

    /**
     * Computes a kernel density estimate over {@code eval} points.
     *
     * @param data   the observations
     * @param eval   the evaluation points
     * @param bw     the bandwidth
     * @param kernel the kernel name ({@code "gaussian"}, {@code "rectangular"},
     *               {@code "triangular"}, {@code "epanechnikov"},
     *               {@code "biweight"}, {@code "cosine"}, {@code "optcosine"})
     * @return the density at each evaluation point
     */
    public static double[] densityAt(List<Double> data, double[] eval, double bw, String kernel) {
        int n = data.size();
        double[] out = new double[eval.length];
        for (int i = 0; i < eval.length; i++) {
            double sum = 0;
            for (double d : data) {
                sum += kernelWeight((eval[i] - d) / bw, kernel);
            }
            out[i] = sum / (n * bw);
        }
        return out;
    }

    /**
     * The normalization-1 kernel weight for a scaled argument u.
     *
     * @param u the scaled distance from the evaluation point
     * @param kernel the kernel name, or {@code null} for gaussian
     * @return the kernel weight at {@code u}
     */
    public static double kernelWeight(double u, String kernel) {
        return switch (kernel == null ? "gaussian" : kernel) {
            case "rectangular" -> Math.abs(u) <= 1 ? 0.5 : 0;
            case "triangular" -> Math.abs(u) <= 1 ? 1 - Math.abs(u) : 0;
            case "epanechnikov" -> Math.abs(u) <= 1 ? 0.75 * (1 - u * u) : 0;
            case "biweight" -> {
                double v = 1 - u * u;
                yield Math.abs(u) <= 1 ? 15.0 / 16.0 * v * v : 0;
            }
            case "cosine" -> Math.abs(u) <= 1 ? 0.25 * Math.PI * Math.cos(0.5 * Math.PI * u) : 0;
            case "optcosine" -> {
                if (Math.abs(u) >= 1) {
                    yield 0;
                }
                yield 0.25 * Math.PI * Math.PI / 8.0 * Math.cos(Math.PI * u);
            }
            default -> Math.exp(-0.5 * u * u) / Math.sqrt(2 * Math.PI);
        };
    }

    /**
     * Silverman's rule-of-thumb bandwidth {@code Silverman's rule of thumb}.
     *
     * @param sorted the data values
     * @return the bandwidth
     */
    public static double bandwidthNrd0(List<Double> sorted) {
        return ruleOfThumb(sorted, false);
    }

    /**
     * Robust rule-of-thumb bandwidth {@code Silverman's rule of thumb}.
     *
     * @param sorted the data values
     * @return the bandwidth
     */
    public static double bandwidthNrd(List<Double> sorted) {
        return ruleOfThumb(sorted, true);
    }

    private static double ruleOfThumb(List<Double> data, boolean robust) {
        int k = data.size();
        double mean = 0;
        for (double v : data) {
            mean += v;
        }
        mean /= k;
        double varSum = 0;
        for (double v : data) {
            varSum += (v - mean) * (v - mean);
        }
        double sd = k > 1 ? Math.sqrt(varSum / (k - 1)) : 0;
        double dispers;
        if (robust) {
            var sorted = new ArrayList<>(data);
            Collections.sort(sorted);
            double median = quantile(sorted, 0.5);
            var abs = new ArrayList<Double>(k);
            for (double v : data) {
                abs.add(Math.abs(v - median));
            }
            Collections.sort(abs);
            dispers = quantile(abs, 0.5) / 0.6745;
        } else {
            var sorted = new ArrayList<>(data);
            Collections.sort(sorted);
            dispers = (quantile(sorted, 0.75) - quantile(sorted, 0.25)) / 1.34;
        }
        double bw = 0.9 * Math.min(sd, dispers) * Math.pow(k, -0.2);
        if (!(bw > 0) || Double.isNaN(bw)) {
            bw = 0.9 * sd;
        }
        if (!(bw > 0) || Double.isNaN(bw)) {
            bw = 1.0;
        }
        return bw;
    }

    /**
     * Linear-interpolation quantile (the type-7 default).
     *
     * @param sorted the data values
     * @param p the probability in {@code [0, 1]}
     * @return the interpolated quantile
     */
    public static double quantile(List<Double> sorted, double p) {
        double[] a = sorted.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int k = a.length;
        if (k == 1) {
            return a[0];
        }
        double h = (k - 1) * p;
        int lo = (int) Math.floor(h);
        int hi = (int) Math.ceil(h);
        return a[lo] + (h - lo) * (a[hi] - a[lo]);
    }
}
