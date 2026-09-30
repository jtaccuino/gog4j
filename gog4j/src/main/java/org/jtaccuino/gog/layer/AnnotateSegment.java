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
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Data-free segment annotation — a straight line drawn from a fixed (x, y) to
 * a fixed (xend, yend) in data space.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotateSegment<DF> implements Layer<DF> {

    private static final Color DEFAULT_COLOR = Color.BLACK;

    private final double x, y, xend, yend;
    private final Color color;
    private final double width;
    private final double[] dashes;

    /**
     * Creates a segment annotation.
     *
     * @param x     the data-space start x
     * @param y     the data-space start y
     * @param xend  the data-space end x
     * @param yend  the data-space end y
     * @param color the stroke colour, or {@code null} for the default
     * @param width the stroke width in pixels
     * @param dashes the dash pattern, or {@code null} for a solid line
     */
    public AnnotateSegment(double x, double y, double xend, double yend,
                           Color color, double width, double[] dashes) {
        this.x = x;
        this.y = y;
        this.xend = xend;
        this.yend = yend;
        this.color = color;
        this.width = width > 0 ? width : 1.0;
        this.dashes = dashes;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(),
                sy.minPixel() - sy.maxPixel());
        gc.clip();

        gc.setStroke(color != null ? color : DEFAULT_COLOR);
        gc.setLineWidth(width);
        if (dashes != null && dashes.length > 0) gc.setLineDashes(dashes);

        var px1 = coord.xPixel(sx, sy, x, y);
        var py1 = coord.yPixel(sx, sy, x, y);
        var px2 = coord.xPixel(sx, sy, xend, yend);
        var py2 = coord.yPixel(sx, sy, xend, yend);
        gc.strokeLine(px1, py1, px2, py2);

        gc.setLineDashes(null);
        gc.restore();
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        if (xDiscrete || yDiscrete) return bounds;
        return new Bounds(
                Math.min(bounds.xMin(), Math.min(x, xend)),
                Math.max(bounds.xMax(), Math.max(x, xend)),
                Math.min(bounds.yMin(), Math.min(y, yend)),
                Math.max(bounds.yMax(), Math.max(y, yend)));
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var px1 = coord.xPixel(sx, sy, x, y);
        var py1 = coord.yPixel(sx, sy, x, y);
        var px2 = coord.xPixel(sx, sy, xend, yend);
        var py2 = coord.yPixel(sx, sy, xend, yend);
        double dx = px2 - px1, dy = py2 - py1;
        if (dx * dx + dy * dy == 0) return null;
        double distSq = PolygonMath.distanceSqToSegment(mx, my, px1, py1, px2, py2);
        return distSq <= 25.0 ? Scale.formatTick(x) + "→" + Scale.formatTick(xend) : null;
    }
}
