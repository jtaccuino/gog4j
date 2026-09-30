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
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Data-free text annotation — a label rendered at a fixed (x, y) in data space.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotateText<DF> implements Layer<DF> {

    private static final double DEFAULT_SIZE = 11.0;

    private final double x;
    private final double y;
    private final String label;
    private final double size;
    private final Color color;
    private final double angle;

    /**
     * Creates a text annotation.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param label the text to render
     * @param size  the font size in points ({@code 0} for default)
     * @param color the text colour, or {@code null} for default
     * @param angle the rotation angle in degrees (0 for horizontal)
     */
    public AnnotateText(double x, double y, String label, double size, Color color, double angle) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.size = size > 0 ? size : DEFAULT_SIZE;
        this.color = color;
        this.angle = angle;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        if (label == null || label.isBlank()) return;
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var fill = color != null ? color : ctx.plot().theme().fallbackColor();

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(),
                sy.minPixel() - sy.maxPixel());
        gc.clip();

        var cx = coord.xPixel(sx, sy, x, y);
        var cy = coord.yPixel(sx, sy, x, y);
        gc.setFont(Font.font("System", size));
        gc.setFill(fill);

        if (angle == 0.0) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(label, cx, cy);
        } else {
            gc.save();
            gc.translate(cx, cy);
            gc.rotate(angle);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(label, 0, 0);
            gc.restore();
        }
        gc.setTextBaseline(VPos.BASELINE);
        gc.restore();
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
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var cx = coord.xPixel(sx, sy, x, y);
        var cy = coord.yPixel(sx, sy, x, y);
        double threshold = 12.0;
        return (Math.abs(mx - cx) <= threshold && Math.abs(my - cy) <= threshold) ? label : null;
    }
}
