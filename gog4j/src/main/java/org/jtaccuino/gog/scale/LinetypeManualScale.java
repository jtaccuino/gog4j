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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Fluent builder for {@code scaleLinetypeManual()}: assigns explicit dash
 * patterns to the categories of the {@code linetype} aesthetic. Each
 * {@link #linetype(String, double...)} or {@link #linetype(String, LineType)}
 * call adds one category-to-dash entry, mirroring
 * {@code Scales.scaleLinetypeManual(values = ...)}.
 * <p>
 * The builder is itself a {@link ScaleConfigurator}, so it is handed straight
 * to {@link Plot#scales(ScaleConfigurator)} (or chained) without a terminal
 * build step.
 */
public final class LinetypeManualScale implements ScaleConfigurator {

    private final Map<String, List<Double>> linetypes = new LinkedHashMap<>();

    /** Creates an empty line-type scale; add entries with {@link #linetype}. */
    public LinetypeManualScale() {
    }

    /**
     * Assigns a custom dash pattern to a category, overriding the automatic
     * cycle.
     *
     * @param category the category name (its string form)
     * @param dashes   the alternating on/off lengths; empty means solid
     * @return this builder for fluid chaining
     */
    public LinetypeManualScale linetype(String category, double... dashes) {
        return linetype(category, LineType.of(dashes));
    }

    /**
     * Assigns a {@link LineType} (a predefined {@link LineTypes} constant or a
     * custom {@link LineType#of(double...)} pattern) to a category.
     *
     * @param category the category name (its string form)
     * @param type     the line type to assign
     * @return this builder for fluid chaining
     */
    public LinetypeManualScale linetype(String category, LineType type) {
        var d = type.dashes();
        var pattern = new ArrayList<Double>(d.length);
        for (var v : d) {
            pattern.add(v);
        }
        linetypes.put(category, pattern);
        return this;
    }

    @Override
    public void configure(ConfigTarget<?> plot) {
        plot.getScaleSpec().setManualLinetypes(linetypes);
    }
}
