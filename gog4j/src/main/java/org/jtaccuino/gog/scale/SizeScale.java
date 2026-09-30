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

import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * The continuous size scale: maps values to point radii with area
 * proportions, following the {@code sizeScale()} — the radius grows
 * with the square root of the value between {@link #RADIUS_MIN} and
 * {@link #RADIUS_MAX} pixels across the data range.
 */
public final class SizeScale {

    /** The smallest point radius a size mapping produces, in pixels. */
    public static final double RADIUS_MIN = 1.0;

    /** The largest point radius a size mapping produces, in pixels. */
    public static final double RADIUS_MAX = 7.0;

    private SizeScale() {
    }

    /**
     * The global [min, max] of a size column over the whole data frame — every
     * facet and the legend must share one range for comparable sizes.
     *
     * @param <DF>       the DataFrame type
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the size aesthetic
     * @return the range, or {@code null} when no numeric value exists
     */
    public static <DF> MinMax range(DF df, DataExtractor<DF> ext, String columnName) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (var v : ext.getColumn(df, columnName)) {
            if (v == null) {
                continue;
            }
            // Unparseable values are skipped via NaN sentinel instead of
            // dragging the range towards 0.
            double d = Values.toDouble(v, Double.NaN);
            if (Double.isNaN(d)) {
                continue;
            }
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        return min <= max ? new MinMax(min, max) : null;
    }

    /**
     * Maps a value to a point radius: square-root scaling between the range's
     * endpoints so the point area grows linearly with the value.
     *
     * @param value the value in data units
     * @param range the scale's data range
     * @return the point radius in pixels
     */
    public static double radiusFor(double value, MinMax range) {
        double span = range.max() - range.min();
        if (!(span > 0)) {
            return (RADIUS_MIN + RADIUS_MAX) / 2.0;
        }
        double t = Math.max(0.0, Math.min(1.0, (value - range.min()) / span));
        return RADIUS_MIN + (RADIUS_MAX - RADIUS_MIN) * Math.sqrt(t);
    }
}
