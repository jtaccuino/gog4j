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
import org.jtaccuino.gog.layer.PointShape;

/**
 * Fluent builder for {@code scaleShapeManual()}: assigns explicit point shapes
 * to the categories of the {@code shape} aesthetic. Each
 * {@link #shape(String, PointShape)} call adds one category-to-shape entry,
 * mirroring {@code Scales.scaleShapeManual(values = ...)}.
 * <p>
 * The builder is itself a {@link ScaleConfigurator}, so it is handed straight
 * to {@link Plot#scales(ScaleConfigurator)} (or chained) without a terminal
 * build step.
 */
public final class ShapeManualScale implements ScaleConfigurator {

    private final Map<String, PointShape> shapes = new LinkedHashMap<>();

    /** Creates an empty shape scale; add entries with {@link #shape}. */
    public ShapeManualScale() {
    }

    /**
     * Assigns a point shape to a category, overriding the automatic cycle.
     *
     * @param category the category name (its string form)
     * @param shape    the point shape to assign
     * @return this builder for fluid chaining
     */
    public ShapeManualScale shape(String category, PointShape shape) {
        shapes.put(category, shape);
        return this;
    }

    @Override
    public void configure(ConfigTarget<?> plot) {
        plot.getScaleSpec().setManualShapes(shapes);
    }
}
