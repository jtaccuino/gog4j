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
 * Configurator (builder) for the {@link GeomSegment3d} and {@link GeomPath3d}
 * geometry layers.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class Segment3dConfigurator<DF> extends BasePrimitive3dConfigurator<DF, GeomSegment3d<DF>, Segment3dConfigurator<DF>> {

    /**
     * Creates a configurator around the given segment geometry.
     *
     * @param geom the segment or path geometry to configure
     */
    public Segment3dConfigurator(GeomSegment3d<DF> geom) {
        super(geom);
    }

    /**
     * {@return this} Sets the stroke color for the segments.
     *
     * @param c line color
     */
    public Segment3dConfigurator<DF> color(Color c) { geom().color(c); return this; }

    /**
     * {@return this} Sets the segment stroke width in pixels.
     *
     * @param w line width in pixels
     */
    public Segment3dConfigurator<DF> linewidth(double w) { geom().linewidth(w); return this; }

    /**
     * {@return this} Sets the dash pattern (linetype) of the segments.
     *
     * @param dashes alternating on/off lengths, or {@code null} for solid
     */
    public Segment3dConfigurator<DF> linetype(double... dashes) { geom().linetype(dashes); return this; }

    /**
     * {@return this} Sets a constant transparency applied to every segment.
     *
     * @param alpha opacity between 0.0 and 1.0
     */
    public Segment3dConfigurator<DF> alpha(double alpha) { geom().alpha(alpha); return this; }

    /**
     * {@return this} Enables (default) or disables the perspective line-width
     * cue.
     *
     * @param scaleDepth {@code true} to deepen far segments, {@code false} for constant width
     */
    public Segment3dConfigurator<DF> scaleDepth(boolean scaleDepth) { geom().scaleDepth(scaleDepth); return this; }
}
