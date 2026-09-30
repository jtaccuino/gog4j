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
import org.jtaccuino.gog.layer.GeomHline;
import org.jtaccuino.gog.layer.GeomVline;

/**
 * Factory for creating annotation layers — data-free geometry layers that
 * carry their own scalar coordinates and draw directly onto the panel via
 * {@link org.jtaccuino.gog.coord.Coord}. The gog4j counterpart of the
 * {@code annotate()} family.
 * <p>
 * Every returned layer implements {@link Layer}, expands the axis domain to
 * include its annotation coordinates, and can be registered with
 * {@link org.jtaccuino.gog.Plot#annotate(Layer[])}. Coordinates are
 * translated through the active coordinate system (Cartesian, flipped, polar,
 * coordTrans), so annotations behave consistently with data layers.
 */
public final class Annotations {

    /** Utility class; not meant to be instantiated. */
    private Annotations() {}

    /**
     * A point annotation.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint} with default styling
     */
    public static <DF> AnnotatePoint<DF> point(double x, double y) {
        return new AnnotatePoint<>(x, y, 4.0, null, null);
    }

    /**
     * A point annotation with explicit size.
     *
     * @param x    the data-space x position
     * @param y    the data-space y position
     * @param size the point diameter in pixels
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint} with default colour and shape
     */
    public static <DF> AnnotatePoint<DF> point(double x, double y, double size) {
        return new AnnotatePoint<>(x, y, size, null, null);
    }

    /**
     * A point annotation with colour.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param size  the point diameter in pixels
     * @param color the point colour, or {@code null} for the default
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint} with default shape
     */
    public static <DF> AnnotatePoint<DF> point(double x, double y, double size, Color color) {
        return new AnnotatePoint<>(x, y, size, color, null);
    }

    /**
     * A point annotation with colour and shape.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param size  the point diameter in pixels
     * @param color the point colour, or {@code null} for the default
     * @param shape the point shape symbol
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint}
     */
    public static <DF> AnnotatePoint<DF> point(double x, double y, double size,
                                               Color color, PointShape shape) {
        return new AnnotatePoint<>(x, y, size, color, shape);
    }

    /**
     * A text annotation at a given data position.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param label the text to render
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText} with default styling
     */
    public static <DF> AnnotateText<DF> text(double x, double y, String label) {
        return new AnnotateText<>(x, y, label, 0, null, 0);
    }

    /**
     * A text annotation with size and colour.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param label the text to render
     * @param size  the font size in points
     * @param color the text colour, or {@code null} for the default
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText}
     */
    public static <DF> AnnotateText<DF> text(double x, double y, String label,
                                             double size, Color color) {
        return new AnnotateText<>(x, y, label, size, color, 0);
    }

    /**
     * A text annotation with size, colour, and rotation.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param label the text to render
     * @param size  the font size in points
     * @param color the text colour, or {@code null} for the default
     * @param angle the rotation angle in degrees (0 for horizontal)
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText}
     */
    public static <DF> AnnotateText<DF> text(double x, double y, String label,
                                             double size, Color color, double angle) {
        return new AnnotateText<>(x, y, label, size, color, angle);
    }

    /**
     * A segment annotation (a straight line) between two data points.
     *
     * @param x    the data-space start x
     * @param y    the data-space start y
     * @param xend the data-space end x
     * @param yend the data-space end y
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateSegment} with default styling
     */
    public static <DF> AnnotateSegment<DF> segment(double x, double y, double xend, double yend) {
        return new AnnotateSegment<>(x, y, xend, yend, null, 1.0, null);
    }

    /**
     * A segment annotation with colour and width.
     *
     * @param x     the data-space start x
     * @param y     the data-space start y
     * @param xend  the data-space end x
     * @param yend  the data-space end y
     * @param color the stroke colour, or {@code null} for the default
     * @param width the stroke width in pixels
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateSegment}
     */
    public static <DF> AnnotateSegment<DF> segment(double x, double y, double xend, double yend,
                                                   Color color, double width) {
        return new AnnotateSegment<>(x, y, xend, yend, color, width, null);
    }

    /**
     * A segment annotation with colour, width, and dash pattern.
     *
     * @param x     the data-space start x
     * @param y     the data-space start y
     * @param xend  the data-space end x
     * @param yend  the data-space end y
     * @param color the stroke colour, or {@code null} for the default
     * @param width the stroke width in pixels
     * @param dashes the dash pattern, or {@code null} for a solid line
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateSegment}
     */
    public static <DF> AnnotateSegment<DF> segment(double x, double y, double xend, double yend,
                                                   Color color, double width, double[] dashes) {
        return new AnnotateSegment<>(x, y, xend, yend, color, width, dashes);
    }

    /**
     * A rectangle annotation between two data-space corners.
     *
     * @param xmin the left x
     * @param ymin the bottom y
     * @param xmax the right x
     * @param ymax the top y
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateRect} with default fill
     */
    public static <DF> AnnotateRect<DF> rect(double xmin, double ymin, double xmax, double ymax) {
        return new AnnotateRect<>(xmin, ymin, xmax, ymax, null, null, 1.0);
    }

    /**
     * A rectangle annotation with a specific fill colour.
     *
     * @param xmin the left x
     * @param ymin the bottom y
     * @param xmax the right x
     * @param ymax the top y
     * @param fill the fill colour, or {@code null} for default
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateRect} with no stroke
     */
    public static <DF> AnnotateRect<DF> rect(double xmin, double ymin, double xmax, double ymax,
                                             Color fill) {
        return new AnnotateRect<>(xmin, ymin, xmax, ymax, fill, null, 1.0);
    }

    /**
     * A horizontal reference line at a constant y value.
     *
     * @param yIntercept the data-space y value
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link GeomHline} drawing the line at the given y
     */
    public static <DF> GeomHline<DF> hline(double yIntercept) {
        return new GeomHline<>(yIntercept);
    }

    /**
     * A vertical reference line at a constant x value.
     *
     * @param xIntercept the data-space x value
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link GeomVline} drawing the line at the given x
     */
    public static <DF> GeomVline<DF> vline(double xIntercept) {
        return new GeomVline<>(xIntercept);
    }
}
