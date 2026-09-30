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
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Data-free point annotation — a single dot drawn at a fixed (x, y) in data
 * space, styled with constant colour, size, and shape.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotatePoint<DF> implements Layer<DF> {

    private static final double DEFAULT_SIZE = 4.0;

    private final double x;
    private final double y;
    private final double size;
    private final Color color;
    private final PointShape shape;

    /**
     * Creates a point annotation at a fixed data position.
     *
     * @param x     the data-space x position
     * @param y     the data-space y position
     * @param size  the point diameter in pixels ({@code 0}/{@code <=0} for the default)
     * @param color the point fill/stroke colour, or {@code null} for the theme default
     * @param shape the point shape symbol, or {@code null} for a filled circle
     */
    public AnnotatePoint(double x, double y, double size, Color color, PointShape shape) {
        this.x = x;
        this.y = y;
        this.size = size > 0 ? size : DEFAULT_SIZE;
        this.color = color;
        this.shape = shape != null ? shape : PointShape.CIRCLE;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var theme = ctx.plot().theme();

        var fill = color != null ? color : theme.fallbackColor();
        var r = size / 2.0;

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(),
                sy.minPixel() - sy.maxPixel());
        gc.clip();

        var cx = coord.xPixel(sx, sy, x, y);
        var cy = coord.yPixel(sx, sy, x, y);
        gc.setFill(fill);
        gc.setStroke(fill);
        shape.draw(gc, cx, cy, r);

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
        double dx = mx - cx, dy = my - cy;
        double threshold = Math.max(4.0, size / 2.0 + 2.0);
        return (dx * dx + dy * dy) <= threshold * threshold
                ? "(" + Scale.formatTick(x) + ", " + Scale.formatTick(y) + ")"
                : null;
    }
}
