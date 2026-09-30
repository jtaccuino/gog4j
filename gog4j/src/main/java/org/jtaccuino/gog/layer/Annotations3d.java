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
 * Factory for creating 3D annotation layers — data-free geometry layers that
 * carry their own scalar (x, y, z) coordinates and draw directly onto the
 * panel through {@link org.jtaccuino.gog.coord.Coord3D}. Three families of
 * factories are provided, one per annotation kind: {@link #point},
 * {@link #text} and {@link #segment}.
 * <p>
 * Every returned layer implements {@link Layer} and requires a plot whose
 * coordinate system is a {@code Coord3D} — no data call is needed. Register it
 * with {@link org.jtaccuino.gog.Plot#annotate(Layer[])}. Since these layers are
 * data-free the z range must cover the annotations; set it explicitly via the
 * {@code Coord3D} bounds when an annotation lies outside the data extent.
 */
public final class Annotations3d {

    /** Utility class; not meant to be instantiated. */
    private Annotations3d() {}

    /**
     * A 3D point annotation.
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint3d} with default styling
     */
    public static <DF> AnnotatePoint3d<DF> point(double x, double y, double z) {
        return new AnnotatePoint3d<>(x, y, z, 4.0, null, null, 1.0);
    }

    /**
     * A 3D point annotation with a specific size.
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param size the point diameter in pixels
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint3d} with default colour and shape
     */
    public static <DF> AnnotatePoint3d<DF> point(double x, double y, double z, double size) {
        return new AnnotatePoint3d<>(x, y, z, size, null, null, 1.0);
    }

    /**
     * A 3D point annotation with size and colour.
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param size the point diameter in pixels
     * @param color the point colour, or {@code null} for the default
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotatePoint3d} with default shape
     */
    public static <DF> AnnotatePoint3d<DF> point(double x, double y, double z, double size, Color color) {
        return new AnnotatePoint3d<>(x, y, z, size, color, null, 1.0);
    }

    /**
     * A 3D text annotation (billboard — always faces the camera).
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param label the text to render
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText3d} with default styling
     */
    public static <DF> AnnotateText3d<DF> text(double x, double y, double z, String label) {
        return new AnnotateText3d<>(x, y, z, label, 0, null, 1.0, 0.5, 0.5, 0, false);
    }

    /**
     * A 3D text annotation with size and colour.
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param label the text to render
     * @param size  the font size in points ({@code 0} for default)
     * @param color the text colour, or {@code null} for the default
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText3d}
     */
    public static <DF> AnnotateText3d<DF> text(double x, double y, double z, String label,
                                               double size, Color color) {
        return new AnnotateText3d<>(x, y, z, label, size, color, 1.0, 0.5, 0.5, 0, false);
    }

    /**
     * A 3D text annotation with size, colour, rotation, and bold face.
     *
     * @param x   the data-space x position
     * @param y   the data-space y position
     * @param z   the data-space z position
     * @param label the text to render
     * @param size  the font size in points ({@code 0} for default)
     * @param color the text colour, or {@code null} for the default
     * @param angle the clockwise rotation in degrees
     * @param bold  whether to render in a bold face
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateText3d}
     */
    public static <DF> AnnotateText3d<DF> text(double x, double y, double z, String label,
                                               double size, Color color, double angle, boolean bold) {
        return new AnnotateText3d<>(x, y, z, label, size, color, 1.0, 0.5, 0.5, angle, bold);
    }

    /**
     * A 3D segment annotation between two data points.
     *
     * @param x    the data-space start x
     * @param y    the data-space start y
     * @param z    the data-space start z
     * @param xend the data-space end x
     * @param yend the data-space end y
     * @param zend the data-space end z
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateSegment3d} with default styling
     */
    public static <DF> AnnotateSegment3d<DF> segment(double x, double y, double z,
                                                     double xend, double yend, double zend) {
        return new AnnotateSegment3d<>(x, y, z, xend, yend, zend, null, 1.0, null);
    }

    /**
     * A 3D segment annotation with colour and width.
     *
     * @param x     the data-space start x
     * @param y     the data-space start y
     * @param z     the data-space start z
     * @param xend  the data-space end x
     * @param yend  the data-space end y
     * @param zend  the data-space end z
     * @param color the stroke colour, or {@code null} for black
     * @param width the stroke width in pixels
     * @param <DF> the DataFrame type (unused by data-free layers)
     * @return a new {@link AnnotateSegment3d}
     */
    public static <DF> AnnotateSegment3d<DF> segment(double x, double y, double z,
                                                     double xend, double yend, double zend,
                                                     Color color, double width) {
        return new AnnotateSegment3d<>(x, y, z, xend, yend, zend, color, width, null);
    }
}
