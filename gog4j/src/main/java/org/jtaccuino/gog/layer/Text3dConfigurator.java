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
import org.jtaccuino.gog.coord.CubeFace;

/**
 * Configurator (builder) for the billboard {@link GeomText3d} geometry layer.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class Text3dConfigurator<DF> extends BasePrimitive3dConfigurator<DF, GeomText3d<DF>, Text3dConfigurator<DF>> {

    /**
     * Creates a configurator around the given 3D text geometry.
     *
     * @param geom the text geometry to configure
     */
    public Text3dConfigurator(GeomText3d<DF> geom) {
        super(geom);
    }

    /**
     * {@return this} Sets the font size in points.
     *
     * @param fontSize the font size
     */
    public Text3dConfigurator<DF> size(double fontSize) { geom().size(fontSize); return this; }

    /**
     * {@return this} Sets the text colour.
     *
     * @param color the fill colour
     */
    public Text3dConfigurator<DF> color(Color color) { geom().color(color); return this; }

    /**
     * {@return this} Renders the labels in a bold face.
     */
    public Text3dConfigurator<DF> bold() { geom().bold(); return this; }

    /**
     * {@return this} Sets a constant transparency applied to every label.
     *
     * @param alpha opacity between 0.0 and 1.0
     */
    public Text3dConfigurator<DF> alpha(double alpha) { geom().alpha(alpha); return this; }

    /**
     * {@return this} Sets the horizontal justification of the labels.
     *
     * @param hjust 0 left, 0.5 centred, 1 right
     */
    public Text3dConfigurator<DF> hjust(double hjust) { geom().hjust(hjust); return this; }

    /**
     * {@return this} Sets the vertical justification of the labels.
     *
     * @param vjust 0 bottom, 0.5 centred, 1 top
     */
    public Text3dConfigurator<DF> vjust(double vjust) { geom().vjust(vjust); return this; }

    /**
     * {@return this} Sets the rotation of the labels in degrees clockwise.
     *
     * @param angle the clockwise rotation in degrees
     */
    public Text3dConfigurator<DF> angle(double angle) { geom().angle(angle); return this; }

    /**
     * {@return this} Enables (default) or disables the perspective size cue.
     *
     * @param scaleDepth {@code true} to scale with depth, {@code false} for constant size
     */
    public Text3dConfigurator<DF> scaleDepth(boolean scaleDepth) { geom().scaleDepth(scaleDepth); return this; }

    /**
     * {@return this} Enables or disables the contrasting halo used as a
     * stand-in for a polygon-outline text renderer.
     *
     * @param textOutlines {@code true} to render the halo
     */
    public Text3dConfigurator<DF> textOutlines(boolean textOutlines) { geom().textOutlines(textOutlines); return this; }

    /**
     * {@return this} Sets the halo colour used with outline text.
     *
     * @param haloColor the halo stroke colour
     */
    public Text3dConfigurator<DF> haloColor(Color haloColor) { geom().haloColor(haloColor); return this; }

    /**
     * {@return this} Selects the rendering method; only {@code "billboard"}
     * is supported.
     *
     * @param method {@code "billboard"} (default)
     */
    public Text3dConfigurator<DF> method(String method) { geom().method(method); return this; }

    /**
     * {@return this} Sets the facing direction for the labels; accepted for
     * source compatibility and ignored by billboard rendering.
     *
     * @param facing a cube face, accepted and ignored
     */
    public Text3dConfigurator<DF> facing(CubeFace facing) { geom().facing(facing); return this; }

    /**
     * {@return this} Accepts a camera-facing specification; billboard text
     * already faces the camera.
     *
     * @param facing a camera-facing specification, accepted and ignored
     */
    public Text3dConfigurator<DF> cameraFacing(Object facing) { geom().cameraFacing(facing); return this; }
}
