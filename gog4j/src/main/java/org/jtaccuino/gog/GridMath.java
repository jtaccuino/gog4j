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

import java.util.Arrays;

/**
 * Shared grid-track arithmetic for the composite figure types. The three
 * helpers turn a sparse list of per-track weights into concrete pixel
 * geometry: {@link #padded(double[], int)} interprets unset slots as uniform
 * tracks, {@link #proportional(double[], double)} splits a total extent by the
 * weights, and {@link #leadingOffsets(double[], double)} precomputes the
 * leading edge of every track given a gap.
 */
final class GridMath {

    private GridMath() {
    }

    /**
     * Left-pads the given explicit weights to length {@code n} with ones, so
     * unset slots behave as uniform tracks.
     *
     * @param weights the explicit weights, may be {@code null} or shorter
     * @param n       the number of tracks
     * @return the effective weights, one per track
     */
    static double[] padded(double[] weights, int n) {
        double[] out = new double[n];
        Arrays.fill(out, 1.0);
        if (weights != null) {
            System.arraycopy(weights, 0, out, 0, Math.min(weights.length, n));
        }
        return out;
    }

    /**
     * Splits a total extent proportionally to the given weights, falling back
     * to uniform tracks when the weights sum to zero or less.
     *
     * @param weights the per-track weights
     * @param total   the total extent to split, in pixels
     * @return one size per track, summing to {@code total}
     */
    static double[] proportional(double[] weights, double total) {
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
        }
        double[] out = new double[weights.length];
        if (sum <= 0.0) {
            Arrays.fill(out, total / weights.length);
            return out;
        }
        for (int i = 0; i < weights.length; i++) {
            out[i] = total * weights[i] / sum;
        }
        return out;
    }

    /**
     * Returns the cumulative leading edge of each track, separated by a gap.
     *
     * @param sizes the per-track sizes
     * @param gap   the gap between tracks, in pixels
     * @return the leading edge of each track
     */
    static double[] leadingOffsets(double[] sizes, double gap) {
        double[] out = new double[sizes.length];
        double acc = 0.0;
        for (int i = 0; i < sizes.length; i++) {
            out[i] = acc;
            acc += sizes[i] + gap;
        }
        return out;
    }

    /**
     * Splits a total extent proportionally to the weights like
     * {@link #proportional(double[], double)}, but floors every track to a
     * whole pixel and absorbs the sub-pixel remainder into the last track, so
     * the sizes still sum to {@code total} while every track stays integral.
     * An integral {@code total} therefore yields integral tracks and, via
     * {@link #leadingOffsets(double[], double)} or a cumulative sum, integral
     * track origins — keeping panel primitives on the device-pixel grid.
     *
     * @param weights the per-track weights
     * @param total   the total extent to split, in pixels
     * @return one integral size per track, summing to {@code total}
     */
    static double[] proportionalIntegral(double[] weights, double total) {
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
        }
        double[] out = new double[weights.length];
        if (sum <= 0.0) {
            double base = Math.floor(total / weights.length);
            Arrays.fill(out, base);
            out[weights.length - 1] = total - base * (weights.length - 1);
            return out;
        }
        double used = 0.0;
        for (int i = 0; i < weights.length - 1; i++) {
            out[i] = Math.floor(total * weights[i] / sum);
            used += out[i];
        }
        out[weights.length - 1] = total - used;
        return out;
    }
}
