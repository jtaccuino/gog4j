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

import org.jtaccuino.gog.render.DrawSurface;

/**
 * Enumeration of supported point symbol shapes for scatter plots.
 * <p>
 * Supported symbols:
 * <ul>
 *   <li>{@link #CIRCLE} (standard filled circle)</li>
 *   <li>{@link #TRIANGLE} (filled triangle)</li>
 *   <li>{@link #SQUARE} (filled square)</li>
 *   <li>{@link #PLUS} (crossline plus sign)</li>
 *   <li>{@link #CROSS} (diagonal cross sign)</li>
 *   <li>{@link #DIAMOND} (filled diamond/rhombus)</li>
 * </ul>
 * <p>
 * Filled shapes support an optional stroke-suppression mode used by the
 * interactive speed path: a small point's outline is a 1px ring around a
 * 2-4px fill that reads as noise, and skipping the {@code strokeXxx} primitive
 * halves the recorded display-list calls per point. Outline-only shapes
 * ({@link #PLUS}, {@link #CROSS}) always draw their strokes.
 */
public enum PointShape {
    /** A standard filled circle. */
    CIRCLE {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            double size = radius * 2.0;
            gc.fillOval(cx - radius, cy - radius, size, size);
            if (!suppressStroke) {
                gc.strokeOval(cx - radius, cy - radius, size, size);
            }
        }
    },
    /** A filled triangle. */
    TRIANGLE {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            gc.fillPolygon(
                new double[]{cx, cx + radius, cx - radius},
                new double[]{cy - radius, cy + radius, cy + radius},
                3
            );
            if (!suppressStroke) {
                gc.strokePolygon(
                    new double[]{cx, cx + radius, cx - radius},
                    new double[]{cy - radius, cy + radius, cy + radius},
                    3
                );
            }
        }
    },
    /** A filled square. */
    SQUARE {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            double size = radius * 2.0;
            gc.fillRect(cx - radius, cy - radius, size, size);
            if (!suppressStroke) {
                gc.strokeRect(cx - radius, cy - radius, size, size);
            }
        }
    },
    /** A crossline plus sign. */
    PLUS {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            gc.strokeLine(cx - radius, cy, cx + radius, cy);
            gc.strokeLine(cx, cy - radius, cx, cy + radius);
        }
    },
    /** A diagonal cross sign. */
    CROSS {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            gc.strokeLine(cx - radius, cy - radius, cx + radius, cy + radius);
            gc.strokeLine(cx - radius, cy + radius, cx + radius, cy - radius);
        }
    },
    /** A filled diamond / rhombus. */
    DIAMOND {
        @Override
        public void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke) {
            double[] xPoints = {cx, cx + radius, cx, cx - radius};
            double[] yPoints = {cy - radius, cy, cy + radius, cy};
            gc.fillPolygon(xPoints, yPoints, 4);
            if (!suppressStroke) {
                gc.strokePolygon(xPoints, yPoints, 4);
            }
        }
    };

    /**
     * Draws the symbol shape onto the specified canvas context centered at {@code (cx, cy)}.
     *
     * @param gc target JavaFX canvas graphics context
     * @param cx center X pixel coordinate
     * @param cy center Y pixel coordinate
     * @param radius symbol radius in pixels
     */
    public final void draw(DrawSurface gc, double cx, double cy, double radius) {
        draw(gc, cx, cy, radius, false);
    }

    /**
     * Draws the symbol shape onto the specified canvas context centered at
     * {@code (cx, cy)}, optionally omitting the outline primitive.
     *
     * @param gc target JavaFX canvas graphics context
     * @param cx center X pixel coordinate
     * @param cy center Y pixel coordinate
     * @param radius symbol radius in pixels
     * @param suppressStroke whether to skip the outline for filled shapes
     */
    public abstract void draw(DrawSurface gc, double cx, double cy, double radius, boolean suppressStroke);
}
