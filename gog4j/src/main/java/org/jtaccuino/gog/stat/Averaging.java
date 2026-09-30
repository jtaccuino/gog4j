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
import java.util.List;

/**
 * Provides rolling-window average filtering for time-series data.
 * <p>
 * Used by {@link org.jtaccuino.gog.layer.GeomLine} to smooth sequential data points
 * via a simple moving-average filter.
 */
public class Averaging {
    private final int windowSize;
    private final String mode; // for future extensions (e.g. "EXPONENTIAL", "MEDIAN")

    private Averaging(int windowSize, String mode) {
        this.windowSize = windowSize;
        this.mode = mode;
    }

    /**
     * {@return a new rolling/moving-average filter with the given window size}
     *
     * @param size the number of data points in the rolling window
     */
    public static Averaging rollingWindow(int size) {
        return new Averaging(size, "ROLLING");
    }

    /**
     * Fluent API to clarify the time unit in code.
     *
     * @return this {@link Averaging}
     */
    public Averaging months() { return this; }

    /** {@return this {@link Averaging} (fluent API time-unit clarifier)} */
    public Averaging days() { return this; }

    /** {@return the rolling-window size} */
    public int getWindowSize() { return windowSize; }

    /** {@return whether the window is larger than one point} */
    public boolean isActive() { return windowSize > 1; }

    /**
     * Filters the given list of Y-values in-memory according to the selected strategy.
     *
     * @param indices the indices into {@code yData} to filter
     * @param yData   the raw Y-values, aligned with {@code indices}
     * @return the filtered Y-values
     */
    public List<Double> apply(List<Integer> indices, List<?> yData) {
        var filteredY = new ArrayList<Double>(indices.size());
        var buffer = new ArrayList<Double>();

        for (var idx : indices) {
            var rawY = yData.get(idx);
            if (rawY == null) {
                filteredY.add(null);
                continue;
            }

            var yVal = ((Number) rawY).doubleValue();

            if (isActive() && "ROLLING".equals(mode)) {
                buffer.add(yVal);
                if (buffer.size() > windowSize) {
                    buffer.removeFirst(); // shift oldest element out of the window
                }
                var sum = 0.0;
                for (var val : buffer) sum += val;
                filteredY.add(sum / buffer.size());
            } else {
                filteredY.add(yVal);
            }
        }
        return filteredY;
    }
}
