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
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;

/**
 * Data-free 3D segment annotation — a straight line from a fixed (x, y, z) to
 * a fixed (xend, yend, zend) in data space. Created via
 * {@link Annotations3d#segment(double, double, double, double, double, double)}.
 *
 * @param <DF> the DataFrame type (unused by this data-free layer)
 */
public class AnnotateSegment3d<DF> implements Layer<DF> {

    private static final Color DEFAULT_COLOR = Color.BLACK;

    private final double x, y, z, xend, yend, zend;
    private final Color color;
    private final double width;
    private final double[] dashes;

    /**
     * Creates a 3D segment annotation.
     *
     * @param x     the data-space start x
     * @param y     the data-space start y
     * @param z     the data-space start z
     * @param xend  the data-space end x
     * @param yend  the data-space end y
     * @param zend  the data-space end z
     * @param color the stroke colour, or {@code null} for black
     * @param width the stroke width in pixels
     * @param dashes the dash pattern, or {@code null} for a solid line
     */
    public AnnotateSegment3d(double x, double y, double z, double xend, double yend, double zend,
                             Color color, double width, double[] dashes) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xend = xend;
        this.yend = yend;
        this.zend = zend;
        this.color = color;
        this.width = width > 0 ? width : 1.0;
        this.dashes = dashes;
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        if (!(ctx.plot().coord() instanceof Coord3D coord3d)) return;
        gc.setStroke(color != null ? color : DEFAULT_COLOR);
        gc.setLineWidth(width);
        if (dashes != null && dashes.length > 0) gc.setLineDashes(dashes);

        var p1 = coord3d.projectData(x, y, z);
        var p2 = coord3d.projectData(xend, yend, zend);
        gc.strokeLine(p1.sx(), p1.sy(), p2.sx(), p2.sy());

        gc.setLineDashes(null);
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
        if (!(ctx.plot().coord() instanceof Coord3D coord3d)) return null;
        var p1 = coord3d.projectData(x, y, z);
        var p2 = coord3d.projectData(xend, yend, zend);
        double dx = p2.sx() - p1.sx(), dy = p2.sy() - p1.sy();
        if (dx * dx + dy * dy == 0) return null;
        double distSq = PolygonMath.distanceSqToSegment(mx, my, p1.sx(), p1.sy(), p2.sx(), p2.sy());
        return distSq <= 25.0 ? Scale.formatTick(x) + "→" + Scale.formatTick(xend) : null;
    }
}
