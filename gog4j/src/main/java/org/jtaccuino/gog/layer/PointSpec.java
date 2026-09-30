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
package org.jtaccuino.gog.layer;

import javafx.scene.paint.Color;

/**
 * Configuration specification holder for scatter plot points ({@link GeomPoint}).
 * Holds properties for point diameter size, constant fill/stroke color, point shape symbol,
 * and opacity.
 */
public class PointSpec {
    private double size = 9.0; // diameter (default is ~9.0 in pixel space)
    private boolean sizeExplicit;
    private Color color = null; // when null, falls back to default red or categorical master palette
    private PointShape shape = PointShape.CIRCLE;
    private double opacity = 1.0;

    /**
     * Creates a point specification with default size, shape, and opacity.
     */
    public PointSpec() {
    }

    /**
     * Returns the point diameter size in pixels. The value is only meaningful
     * when {@link #hasExplicitSize()} is {@code true}; an unset size falls back
     * to the theme's {@code defaultPointSize()} at render time.
     *
     * @return diameter in pixels
     */
    public double getSize() { return size; }

    /**
     * Whether the point size was explicitly configured via {@link #size(double)}.
     *
     * @return {@code true} when the user set a size
     */
    public boolean hasExplicitSize() { return sizeExplicit; }

    /**
     * Sets the point diameter size in pixels.
     *
     * @param size diameter in pixels
     * @return this {@code PointSpec} instance
     */
    public PointSpec size(double size) { this.size = size; this.sizeExplicit = true; return this; }

    /**
     * Returns the custom point color, if configured.
     *
     * @return custom {@link Color}, or {@code null} if default palette is used
     */
    public Color getColor() { return color; }

    /**
     * Sets a custom constant color for points.
     *
     * @param color JavaFX {@link Color}
     * @return this {@code PointSpec} instance
     */
    public PointSpec color(Color color) { this.color = color; return this; }

    /**
     * Returns the point symbol shape.
     *
     * @return active {@link PointShape}
     */
    public PointShape getShape() { return shape; }

    /**
     * Sets the point symbol shape.
     *
     * @param shape the {@link PointShape} symbol
     * @return this {@code PointSpec} instance
     */
    public PointSpec shape(PointShape shape) { this.shape = shape; return this; }

    /**
     * Returns the point opacity value.
     *
     * @return opacity between 0.0 (fully transparent) and 1.0 (fully opaque)
     */
    public double getOpacity() { return opacity; }

    /**
     * Sets the point opacity value.
     *
     * @param opacity between 0.0 (fully transparent) and 1.0 (fully opaque)
     * @return this {@code PointSpec} instance
     */
    public PointSpec opacity(double opacity) { this.opacity = opacity; return this; }
}
