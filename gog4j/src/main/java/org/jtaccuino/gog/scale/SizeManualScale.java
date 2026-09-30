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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Fluent builder for {@code scaleSizeManual()}: assigns explicit point radii
 * to the categories of the {@code size} aesthetic. Each
 * {@link #size(String, double)} call adds one category-to-size entry,
 * mirroring {@code Scales.scaleSizeManual(values = ...)}.
 * <p>
 * The builder is itself a {@link ScaleConfigurator}, so it is handed straight
 * to {@link Plot#scales(ScaleConfigurator)} (or chained) without a terminal
 * build step.
 */
public final class SizeManualScale implements ScaleConfigurator {

    private final Map<String, Double> sizes = new LinkedHashMap<>();

    /** Creates an empty size scale; add entries with {@link #size}. */
    public SizeManualScale() {
    }

    /**
     * Assigns a point radius to a category, overriding the continuous size
     * scaling for that category.
     *
     * @param category the category name (its string form)
     * @param radius   the point radius in pixels
     * @return this builder for fluid chaining
     */
    public SizeManualScale size(String category, double radius) {
        sizes.put(category, radius);
        return this;
    }

    @Override
    public void configure(ConfigTarget<?> plot) {
        plot.getScaleSpec().setManualSizes(sizes);
    }
}
