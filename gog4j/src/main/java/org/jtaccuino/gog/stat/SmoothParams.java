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

/**
 * The typed, immutable parameters of a {@link StatSmooth} computation —
 * the flow-friendly alternative to the generic {@link StatParams} bag.
 * <p>
 * Every field has a sensible default, so a bare
 * {@link #defaults()} describes {@code Stats.smooth()} with LOESS smoothing,
 * a span of {@code 0.75}, and the confidence envelope enabled. Callers change
 * only what they need via {@code with*} overrides and chain fluently:
 * <pre>{@code
 * SmoothParams.defaults()
 *     .method(SmoothMethod.LM)
 *     .se(false)
 *     .fullrange(true)
 *     .globalRange(0.0, 6.5);
 * }</pre>
 *
 * @param method    the smoothing model, {@link SmoothMethod#LOESS} by default
 * @param span      the LOESS bandwidth, {@code 0.75} by default
 * @param se        whether to emit the {@code ymin}/{@code ymax} confidence
 *                  envelope, {@code true} by default
 * @param fullrange whether to fit over a uniform grid spanning
 *                  {@code [globalMin, globalMax]} instead of the data range,
 *                  {@code false} by default
 * @param globalMin the left edge of the {@code fullrange} grid, only read when
 *                  {@code fullrange} is enabled
 * @param globalMax the right edge of the {@code fullrange} grid, only read when
 *                  {@code fullrange} is enabled
 * @param steps     the number of points on the fitted curve, {@code 80} by default
 */
public record SmoothParams(SmoothMethod method, double span, boolean se, boolean fullrange,
                           double globalMin, double globalMax, int steps) {

    /**
     * Default {@code Stats.smooth()} parameters: LOESS with span {@code 0.75},
     * the confidence envelope enabled, and a curve of {@code 80} points.
     *
     * @return an all-default parameter set
     */
    public static SmoothParams defaults() {
        return new SmoothParams(SmoothMethod.LOESS, 0.75, true, false,
                Double.NaN, Double.NaN, 80);
    }

    /**
     * Returns a copy with a different smoothing model.
     *
     * @param m the model to use
     * @return an updated parameter set
     */
    public SmoothParams method(SmoothMethod m) {
        return new SmoothParams(m, span, se, fullrange, globalMin, globalMax, steps);
    }

    /**
     * Returns a copy with a different LOESS bandwidth.
     *
     * @param s the new bandwidth
     * @return an updated parameter set
     */
    public SmoothParams span(double s) {
        return new SmoothParams(method, s, se, fullrange, globalMin, globalMax, steps);
    }

    /**
     * Returns a copy with the confidence envelope toggled.
     *
     * @param b {@code true} to emit {@code ymin}/{@code ymax}
     * @return an updated parameter set
     */
    public SmoothParams se(boolean b) {
        return new SmoothParams(method, span, b, fullrange, globalMin, globalMax, steps);
    }

    /**
     * Returns a copy with full-range extrapolation toggled.
     *
     * @param b {@code true} to fit over the global grid
     * @return an updated parameter set
     */
    public SmoothParams fullrange(boolean b) {
        return new SmoothParams(method, span, se, b, globalMin, globalMax, steps);
    }

    /**
     * Returns a copy fitted over the given global grid — the domain the curve
     * is extrapolated across when {@link #fullrange(boolean)} is enabled.
     *
     * @param min the left edge of the grid
     * @param max the right edge of the grid
     * @return an updated parameter set
     */
    public SmoothParams globalRange(double min, double max) {
        return new SmoothParams(method, span, se, fullrange, min, max, steps);
    }

    /**
     * Returns a copy that fits a different number of points on the curve.
     *
     * @param n the new curve resolution
     * @return an updated parameter set
     */
    public SmoothParams steps(int n) {
        return new SmoothParams(method, span, se, fullrange, globalMin, globalMax, n);
    }
}
