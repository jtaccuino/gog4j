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
import javafx.scene.paint.Color;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Fluent builder for {@code scaleColorManual()}: assigns explicit colours to
 * the categories of the {@code color}/{@code fill} aesthetic. Each
 * {@link #color(String, Color)} call adds one category colour, mirroring
 * {@code scaleColorManual(values = c(a = "grey", b = "skyblue"))}.
 * <p>
 * The builder is itself a {@link ScaleConfigurator}, so it is handed straight
 * to {@link Plot#scales(ScaleConfigurator)} (or chained) without a terminal
 * build step.
 */
public final class ColorManualScale implements ScaleConfigurator {

    private final Map<String, Color> colors = new LinkedHashMap<>();

    /** Creates an empty colour scale; add entries with {@link #color}. */
    public ColorManualScale() {
    }

    /**
     * Assigns a colour to a category, overriding the automatic palette.
     *
     * @param category the category name (its string form)
     * @param color    the colour to assign
     * @return this builder for fluid chaining
     */
    public ColorManualScale color(String category, Color color) {
        colors.put(category, color);
        return this;
    }

    @Override
    public void configure(ConfigTarget<?> plot) {
        plot.getScaleSpec().setManualColors(colors);
    }
}
