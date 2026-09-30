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
import java.util.Locale;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;

/**
 * Shared parameter and range helpers for the 3D statistics, factored out of the
 * individual {@code Stat*3d} implementations that used to carry byte-identical
 * private copies.
 */
final class Stat3dSupport {

    /** The default grid resolution used when a stat does not set {@code n}. */
    private static final int[] DEFAULT_N = {40};

    private Stat3dSupport() {
    }

    /**
     * Resolves an enum stat parameter, accepting the enum constant directly or a
     * case-insensitive {@link String} name.
     *
     * @param <E>   the enum type
     * @param type  the enum class
     * @param value the raw parameter value, or {@code null} to use the default
     * @param def   the default constant
     * @param name  the parameter name, used in error messages
     * @return the resolved enum constant
     */
    static <E extends Enum<E>> E enumParam(Class<E> type, Object value, E def, String name) {
        if (value == null) {
            return def;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof String s) {
            try {
                return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid " + name + ": " + s
                        + ". Valid values are: " + Arrays.toString(type.getEnumConstants()), e);
            }
        }
        throw new IllegalArgumentException("Invalid " + name + " value: " + value);
    }

    /**
     * {@return the grid resolution for a stat, defaulting to {@code {40}}}
     *
     * @param value the raw {@code n} value: an {@code int[]}, a number, or
     *              {@code null}
     */
    static int[] nParam(Object value) {
        if (value instanceof int[] ia) {
            return ia;
        }
        if (value instanceof Number num) {
            return new int[] {num.intValue()};
        }
        return DEFAULT_N;
    }

    /**
     * Resolves a two-element range parameter ({@code xlim}/{@code ylim}),
     * accepting it only when it is an ordered pair.
     *
     * @param params   the stat parameters
     * @param key      the parameter key
     * @param fallback the array to return when the parameter is absent or invalid
     * @return the ordered range, or {@code fallback}
     */
    static double[] rangeParam(StatParams params, String key, double[] fallback) {
        var v = params.get(key, null);
        if (v instanceof double[] d && d.length == 2 && d[0] < d[1]) {
            return d;
        }
        return fallback;
    }

    /**
     * {@return the {@code {min, max}} bounds of the data, or {@code null} when
     * all values are equal (a degenerate range)}
     *
     * @param data the values to bound
     */
    static double[] minMax(double[] data) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : data) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (!(min < max)) {
            return null;
        }
        return new double[] {min, max};
    }

    /**
     * {@return the {@code {min, max}} bounds of the data, expanded by
     * {@code pad} times the span on each side}
     *
     * @param data the values to bound
     * @param pad  the fraction of the span added on each side
     */
    static double[] paddedRange(double[] data, double pad) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : data) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double ext = (max - min) * pad;
        return new double[] {min - ext, max + ext};
    }

    /**
     * Resolves the {@code grid} stat parameter, accepting the
     * {@link GridGeometry} directly or a legacy {@code "rectangle"}/{@code
     * "right1"}/… string.
     *
     * @param params the stat parameters
     * @param def    the default geometry
     * @return the resolved grid geometry
     */
    static GridGeometry gridParam(StatParams params, GridGeometry def) {
        return enumParam(GridGeometry.class, params.get("grid", def), def, "grid");
    }

    /**
     * Resolves the {@code direction} stat parameter, accepting the
     * {@link GridDirection} directly or a legacy {@code "x"}/{@code "y"} string.
     *
     * @param params the stat parameters
     * @param def    the default direction
     * @return the resolved grid direction
     */
    static GridDirection directionParam(StatParams params, GridDirection def) {
        return enumParam(GridDirection.class, params.get("direction", def), def, "direction");
    }
}
