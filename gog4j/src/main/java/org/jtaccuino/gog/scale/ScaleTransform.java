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
package org.jtaccuino.gog.scale;

import static org.jtaccuino.gog.scale.ScaleTransform.roundToNiceNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * A monotone transformation applied to a continuous axis before values are
 * mapped to pixels — the equivalent of the {@code log10Scale} and
 * {@code scale_*_sqrt()}.
 * <p>
 * Transforming the <em>axis</em> keeps every observation in the plot at its true
 * value; only the spacing changes. That is the honest alternative to clamping
 * data into a display range, which silently rewrites the values themselves.
 * <p>
 * Tick positions are chosen in data space, so axis labels always read in the
 * original units.
 */
public interface ScaleTransform {

    /** The untransformed axis: values map to pixels by linear interpolation. */
    ScaleTransform IDENTITY = new ScaleTransform() {
        @Override public double forward(double value) { return value; }
        @Override public double inverse(double value) { return value; }
        @Override public String name() { return "identity"; }
    };

    /**
     * Base-10 logarithm. Defined for strictly positive values; anything at or
     * below zero is pulled to the smallest positive double so that a stray
     * value cannot produce {@code NaN} and blank the panel.
     */
    ScaleTransform LOG10 = new ScaleTransform() {
        @Override public double forward(double value) { return Math.log10(Math.max(value, Double.MIN_NORMAL)); }
        @Override public double inverse(double value) { return Math.pow(10, value); }
        @Override public double clampDomain(double value) { return Math.max(value, Double.MIN_NORMAL); }
        @Override public String name() { return "log10"; }

        @Override
        public List<Double> breaks(double dataMin, double dataMax, int target) {
            var ticks = new ArrayList<Double>();
            var from = (int) Math.floor(Math.log10(Math.max(dataMin, Double.MIN_NORMAL)));
            var to = (int) Math.ceil(Math.log10(Math.max(dataMax, Double.MIN_NORMAL)));
            for (var e = from; e <= to; e++) {
                var decade = Math.pow(10, e);
                if (decade >= dataMin && decade <= dataMax) ticks.add(decade);
            }
            return ticks;
        }
    };

    /**
     * Square root. Compresses a long upper tail while leaving the lower range
     * legible, which suits quantities such as {@code -log10(P)} that span
     * several orders of magnitude but must still show their values near zero.
     */
    ScaleTransform SQRT = new ScaleTransform() {
        @Override public double forward(double value) { return Math.sqrt(Math.max(value, 0)); }
        @Override public double inverse(double value) { return value * value; }
        @Override public double clampDomain(double value) { return Math.max(value, 0); }
        @Override public String name() { return "sqrt"; }

        @Override
        public List<Double> breaks(double dataMin, double dataMax, int target) {
            // Space the ticks evenly in transformed space, then round each back
            // to a readable number in data space.
            var lo = Math.sqrt(Math.max(dataMin, 0));
            var hi = Math.sqrt(Math.max(dataMax, 0));
            if (hi <= lo) return List.of(dataMin);

            var ticks = new ArrayList<Double>();
            var steps = Math.max(2, target);
            for (var i = 0; i <= steps; i++) {
                var candidate = roundToNiceNumber(Math.pow(lo + (hi - lo) * i / steps, 2));
                if (candidate >= dataMin && candidate <= dataMax && !ticks.contains(candidate)) {
                    ticks.add(candidate);
                }
            }
            return ticks;
        }
    };

    /**
     * Maps a data value into transformed space.
     *
     * @param value the value in data units
     * @return the transformed value
     */
    double forward(double value);

    /**
     * Maps a transformed value back to data units.
     *
     * @param value the transformed value
     * @return the value in data units
     */
    double inverse(double value);

    /**
     * A short identifier, used in diagnostics.
     *
     * @return the transform name
     */
    String name();

    /**
     * Constrains an axis bound to the transform's valid domain. Padding applied
     * to the data range can push a bound below what the transform accepts —
     * a log axis cannot start at zero, a square-root axis cannot start below it.
     *
     * @param value the proposed axis bound
     * @return the bound, moved inside the valid domain if needed
     */
    default double clampDomain(double value) {
        return value;
    }

    /**
     * Chooses tick positions in data space appropriate for this transform.
     * Returning an empty list defers to the scale's default linear breaks.
     *
     * @param dataMin the lower axis bound in data units
     * @param dataMax the upper axis bound in data units
     * @param target  the desired approximate number of ticks
     * @return the tick positions in data units
     */
    default List<Double> breaks(double dataMin, double dataMax, int target) {
        return List.of();
    }

    /**
     * Rounds a value to a readable magnitude — 1, 2, 5 times a power of ten.
     *
     * @param value the value to round
     * @return the nearest readable number
     */
    static double roundToNiceNumber(double value) {
        if (value <= 0) return 0;
        var magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        var normalized = value / magnitude;
        double nice;
        if (normalized < 1.5) nice = 1;
        else if (normalized < 3.5) nice = 2;
        else if (normalized < 7.5) nice = 5;
        else nice = 10;
        return nice * magnitude;
    }
}
