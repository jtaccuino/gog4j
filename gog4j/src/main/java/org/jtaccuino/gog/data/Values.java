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
package org.jtaccuino.gog.data;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Shared coercion of raw column values, so the discrimination between numeric,
 * temporal, and string-typed values lives in exactly one place instead of being
 * re-implemented inside every geometry.
 * <p>
 * {@link #toDouble(Object)} converts a value to its numeric data position:
 * {@link Number} keeps its double value, {@link LocalDate} becomes its epoch
 * day, and anything else is parsed as a {@code double}. {@link #label(Object)}
 * produces a display string for tooltips and legends.
 */
public final class Values {

    private Values() {
    }

    /**
     * Converts a raw column value to its numeric data position.
     *
     * @param value the raw column value, may be {@code null}
     * @return the numeric position, {@code 0.0} when the value is {@code null}
     *         or cannot be parsed as a number
     */
    public static double toDouble(Object value) {
        return toDouble(value, 0.0);
    }

    /**
     * Converts a raw column value to its numeric data position, returning a
     * caller-chosen value when a string value cannot be parsed as a number.
     *
     * @param value             the raw column value, may be {@code null}
     * @param onNumberFormatError the value returned when parsing fails
     * @return the numeric position; {@code 0.0} for {@code null}, otherwise the
     *         converted value or {@code onNumberFormatError}
     */
    public static double toDouble(Object value, double onNumberFormatError) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof LocalDate ld) return ld.toEpochDay();
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException e) {
                return onNumberFormatError;
            }
        }
        return 0.0;
    }

    /**
     * Converts a raw column value to its numeric data position, or returns
     * {@code null} when the value does not carry a number. Unlike
     * {@link #toDouble(Object)} this fails open: non-numeric and {@code null}
     * values yield {@code null} instead of a {@code 0.0} default, so callers
     * can drop the affected rows.
     *
     * @param value the raw column value, may be {@code null}
     * @return the numeric position, or {@code null} for null / non-numeric values
     */
    public static Double toDoubleOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof LocalDate ld) return (double) ld.toEpochDay();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Produces a display string for a raw column value: dates print as ISO text,
     * whole numbers without a decimal point, and other numbers with up to two
     * decimals.
     *
     * @param value the raw column value, may be {@code null}
     * @return the display string
     */
    public static String label(Object value) {
        if (value == null) return "null";
        if (value instanceof LocalDate ld) return ld.toString();
        if (value instanceof Number n) {
            var v = n.doubleValue();
            if (v == Math.rint(v) && Math.abs(v) < 1e15) {
                return String.valueOf((long) v);
            }
            return String.format(Locale.US, "%.2f", v);
        }
        return value.toString();
    }
}
