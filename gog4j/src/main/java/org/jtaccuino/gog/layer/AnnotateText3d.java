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

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * Data-free 3D text annotation — a label fixed at (x, y, z) in data space,
 * always facing the camera (billboard), with depth-scaled font size.
 * Created via {@link Annotations3d#text(double, double, double, String)}.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotateText3d<DF> implements Layer<DF> {

    private final double x, y, z;
    private final String label;
    private final double size;
    private final Color color;
    private final double alpha;
    private final double angle;
    private final boolean bold;

    /**
     * Creates a 3D text annotation.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param z     the data-space z position
     * @param label the text to render
     * @param size  the font size in points ({@code 0} for default 11.0)
     * @param color the label colour, or {@code null} for the theme default
     * @param alpha the opacity between 0.0 and 1.0
     * @param hjust the horizontal justification (accepted, see {@link GeomText3d})
     * @param vjust the vertical justification (accepted, see {@link GeomText3d})
     * @param angle the clockwise rotation in degrees
     * @param bold  whether to render in a bold face
     */
    public AnnotateText3d(double x, double y, double z, String label, double size,
                          Color color, double alpha, double hjust, double vjust,
                          double angle, boolean bold) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label;
        this.size = size > 0 ? size : 11.0;
        this.color = color;
        this.alpha = alpha > 0 ? alpha : 1.0;
        this.angle = angle;
        this.bold = bold;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        if (!(ctx.plot().coord() instanceof Coord3D coord3d)) return;
        if (label == null || label.isBlank()) return;
        var fill = color != null ? color : ctx.plot().theme().fallbackColor();
        if (alpha < 1.0) {
            fill = Colors.withAlpha(fill, alpha);
        }
        var proj = coord3d.projectData(x, y, z);
        var cube = CubeStyle.from(ctx.plot().theme());
        double depthFactor = CubeStyle.depthFactor(proj.depthScale(), cube.depthScaleStrength());
        double fontSize = Math.max(4.0, size * depthFactor);

        gc.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize));
        gc.setFill(fill);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (angle == 0.0) {
            gc.fillText(label, proj.sx(), proj.sy());
        } else {
            gc.save();
            gc.translate(proj.sx(), proj.sy());
            gc.rotate(angle);
            gc.fillText(label, 0, 0);
            gc.restore();
        }
        gc.setTextBaseline(VPos.BASELINE);
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        if (xDiscrete || yDiscrete) return bounds;
        return new Bounds(
                Math.min(bounds.xMin(), x), Math.max(bounds.xMax(), x),
                Math.min(bounds.yMin(), y), Math.max(bounds.yMax(), y));
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        if (!(ctx.plot().coord() instanceof Coord3D coord3d)) return null;
        var proj = coord3d.projectData(x, y, z);
        double threshold = 12.0;
        return (Math.abs(mx - proj.sx()) <= threshold && Math.abs(my - proj.sy()) <= threshold) ? label : null;
    }
}
