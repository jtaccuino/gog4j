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

/**
 * Data-free rectangle annotation — a filled (and optionally stroked) rectangle
 * drawn from (xmin, ymin) to (xmax, ymax) in data space.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotateRect<DF> implements Layer<DF> {

    private static final Color DEFAULT_FILL = Color.web("#3182bd", 0.3);

    private final double xmin, ymin, xmax, ymax;
    private final Color fill;
    private final Color stroke;
    private final double strokeWidth;

    /**
     * Creates a rectangle annotation.
     *
     * @param xmin      the left data-space x
     * @param ymin      the bottom data-space y
     * @param xmax      the right data-space x
     * @param ymax      the top data-space y
     * @param fill      the fill colour, or {@code null} for default
     * @param stroke    the stroke colour, or {@code null} for no stroke
     * @param strokeWidth the stroke width in pixels
     */
    public AnnotateRect(double xmin, double ymin, double xmax, double ymax,
                        Color fill, Color stroke, double strokeWidth) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
        this.fill = fill;
        this.stroke = stroke;
        this.strokeWidth = strokeWidth > 0 ? strokeWidth : 1.0;
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

        var x1 = coord.xPixel(sx, sy, xmin, ymin);
        var y1 = coord.yPixel(sx, sy, xmin, ymin);
        var x2 = coord.xPixel(sx, sy, xmax, ymax);
        var y2 = coord.yPixel(sx, sy, xmax, ymax);

        var rx = Math.min(x1, x2);
        var ry = Math.min(y1, y2);
        var rw = Math.abs(x2 - x1);
        var rh = Math.abs(y2 - y1);

        gc.setFill(fill != null ? fill : DEFAULT_FILL);
        gc.fillRect(rx, ry, rw, rh);

        if (stroke != null) {
            gc.setStroke(stroke);
            gc.setLineWidth(strokeWidth);
            gc.strokeRect(rx, ry, rw, rh);
        }

        gc.restore();
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        if (xDiscrete || yDiscrete) return bounds;
        return new Bounds(
                Math.min(bounds.xMin(), Math.min(xmin, xmax)),
                Math.max(bounds.xMax(), Math.max(xmin, xmax)),
                Math.min(bounds.yMin(), Math.min(ymin, ymax)),
                Math.max(bounds.yMax(), Math.max(ymin, ymax)));
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();

        var x1 = coord.xPixel(sx, sy, xmin, ymin);
        var y1 = coord.yPixel(sx, sy, xmin, ymin);
        var x2 = coord.xPixel(sx, sy, xmax, ymax);
        var y2 = coord.yPixel(sx, sy, xmax, ymax);

        var rx = Math.min(x1, x2);
        var ry = Math.min(y1, y2);
        var rw = Math.abs(x2 - x1);
        var rh = Math.abs(y2 - y1);

        return (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh)
                ? "rect[" + rx + "," + ry + "," + rw + "," + rh + "]"
                : null;
    }
}
