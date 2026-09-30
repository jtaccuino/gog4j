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
package org.jtaccuino.gog.render;

import javafx.geometry.VPos;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * The drawing surface a plot renders itself onto.
 * <p>
 * The method set mirrors the subset of {@link javafx.scene.canvas.GraphicsContext}
 * the framework uses, so that geometry layers are written once and can be
 * targeted at different backends: {@link FxDrawSurface} paints onto a JavaFX
 * canvas for interactive display and PNG export, {@link SvgDrawSurface} writes
 * scalable vector output for publication.
 * <p>
 * Coordinates are in pixels of the plot area, with y growing downwards, exactly
 * as on a JavaFX canvas.
 */
public interface DrawSurface {

    // --- Paint state ---------------------------------------------------------

    /**
     * Sets the paint used for filled shapes and text.
     *
     * @param paint the fill paint
     */
    void setFill(Paint paint);

    /**
     * Sets the paint used for outlines.
     *
     * @param paint the stroke paint
     */
    void setStroke(Paint paint);

    /**
     * Sets the outline width.
     *
     * @param width the stroke width in pixels
     */
    void setLineWidth(double width);

    /**
     * Sets the dash pattern for subsequent outlines.
     *
     * @param dashes alternating on/off segment lengths, or {@code null} for solid
     */
    void setLineDashes(double... dashes);

    /**
     * Sets the font used for text.
     *
     * @param font the font
     */
    void setFont(Font font);

    /**
     * Returns the font currently set.
     *
     * @return the active font
     */
    Font getFont();

    /**
     * Sets the horizontal anchoring of text drawn at a point.
     *
     * @param alignment the text alignment
     */
    void setTextAlign(TextAlignment alignment);

    /**
     * Sets the vertical anchoring of text drawn at a point.
     *
     * @param baseline the vertical position
     */
    void setTextBaseline(VPos baseline);

    // --- State stack ---------------------------------------------------------

    /** Pushes the current paint, font, transform, and clip onto the state stack. */
    void save();

    /** Restores the state most recently pushed by {@link #save()}. */
    void restore();

    /**
     * Translates the coordinate system.
     *
     * @param x horizontal offset in pixels
     * @param y vertical offset in pixels
     */
    void translate(double x, double y);

    /**
     * Rotates the coordinate system about its origin.
     *
     * @param degrees the rotation angle in degrees
     */
    void rotate(double degrees);

    /**
     * Scales the coordinate system about its origin, shrinking or enlarging
     * subsequent shapes and text alike. The scale transform composes with any
     * surrounding {@link #translate} and {@link #rotate}, exactly as in a
     * nested SVG group.
     *
     * @param sx the horizontal scale factor, 1.0 for no change
     * @param sy the vertical scale factor, 1.0 for no change
     */
    void scale(double sx, double sy);

    // --- Shapes --------------------------------------------------------------

    /**
     * Fills a rectangle.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    void fillRect(double x, double y, double w, double h);

    /**
     * Outlines a rectangle.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    void strokeRect(double x, double y, double w, double h);

    /**
     * Fills an ellipse inscribed in the given rectangle.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    void fillOval(double x, double y, double w, double h);

    /**
     * Outlines an ellipse inscribed in the given rectangle.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    void strokeOval(double x, double y, double w, double h);

    /**
     * Fills an elliptical pie slice: the sector of the ellipse inscribed in
     * the bounding box, closed to its centre.
     * <p>
     * Angles follow the canvas convention — 0 at three o'clock, increasing
     * clockwise (since y grows downwards), measured in degrees. A sweep beyond
     * {@code ±360°} is clamped to a full ellipse.
     *
     * @param x          left edge of the bounding box
     * @param y          top edge of the bounding box
     * @param w          width of the bounding box
     * @param h          height of the bounding box
     * @param startAngle the start angle in degrees
     * @param arcExtent  the clockwise sweep in degrees, may be negative
     */
    void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent);

    /**
     * Outlines an open elliptical arc — the sector edge without the closing
     * radii to the centre. Angle convention as in {@link #fillArc(double, double, double, double, double, double)}.
     *
     * @param x          left edge of the bounding box
     * @param y          top edge of the bounding box
     * @param w          width of the bounding box
     * @param h          height of the bounding box
     * @param startAngle the start angle in degrees
     * @param arcExtent  the clockwise sweep in degrees, may be negative
     */
    void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent);

    /**
     * Appends an elliptical arc to the current path without closing it — the
     * path-level counterpart of {@link #strokeArc(double, double, double, double, double, double)}.
     * Allows a wedge to be composed from an outer arc, a connecting radius, an
     * inner arc, and a close, exactly as a vector backend would draw it.
     *
     * @param x          left edge of the bounding box
     * @param y          top edge of the bounding box
     * @param w          width of the bounding box
     * @param h          height of the bounding box
     * @param startAngle the start angle in degrees
     * @param arcExtent  the clockwise sweep in degrees, may be negative
     */
    void arc(double x, double y, double w, double h, double startAngle, double arcExtent);

    /**
     * Draws a line segment.
     *
     * @param x1 start x
     * @param y1 start y
     * @param x2 end x
     * @param y2 end y
     */
    void strokeLine(double x1, double y1, double x2, double y2);

    /**
     * Fills a polygon.
     *
     * @param xPoints vertex x coordinates
     * @param yPoints vertex y coordinates
     * @param n       number of vertices to use
     */
    void fillPolygon(double[] xPoints, double[] yPoints, int n);

    /**
     * Outlines a polygon.
     *
     * @param xPoints vertex x coordinates
     * @param yPoints vertex y coordinates
     * @param n       number of vertices to use
     */
    void strokePolygon(double[] xPoints, double[] yPoints, int n);

    // --- Paths ---------------------------------------------------------------

    /** Starts a new path, discarding any path under construction. */
    void beginPath();

    /**
     * Moves the path cursor without drawing.
     *
     * @param x target x
     * @param y target y
     */
    void moveTo(double x, double y);

    /**
     * Extends the current path with a straight segment.
     *
     * @param x target x
     * @param y target y
     */
    void lineTo(double x, double y);

    /**
     * Appends a closed rectangle to the current path.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    void rect(double x, double y, double w, double h);

    /** Closes the current subpath. */
    void closePath();

    /** Strokes the current path. */
    void stroke();

    /** Fills the current path. */
    void fill();

    /** Intersects the clip region with the current path. */
    void clip();

    // --- Text ----------------------------------------------------------------

    /**
     * Fills text at a point, anchored per {@link #setTextAlign(TextAlignment)}.
     *
     * @param text the string to draw
     * @param x    anchor x
     * @param y    anchor y
     */
    void fillText(String text, double x, double y);

    /**
     * Outlines text at a point — used to give a caption a halo so it stays
     * readable over dense data.
     *
     * @param text the string to draw
     * @param x    anchor x
     * @param y    anchor y
     */
    void strokeText(String text, double x, double y);

    // --- Backend hooks -------------------------------------------------------

    /**
     * Announces a layer that is about to be drawn, letting a backend choose a
     * different strategy for it. A vector backend may, for instance, divert a
     * scatter layer of half a million points into an embedded raster image
     * rather than emit half a million elements.
     * <p>
     * The returned surface is the one the layer must draw onto; the default is
     * this surface itself. Every call must be paired with {@link #endLayer()}.
     *
     * @param estimatedPrimitives rough number of shapes the layer will draw
     * @param x                   left edge of the panel the layer draws into
     * @param y                   top edge of the panel
     * @param w                   panel width
     * @param h                   panel height
     * @return the surface to draw the layer onto
     */
    default DrawSurface beginLayer(int estimatedPrimitives, double x, double y, double w, double h) {
        return this;
    }

    /** Closes the layer opened by {@link #beginLayer(int, double, double, double, double)}. */
    default void endLayer() {
    }
}
