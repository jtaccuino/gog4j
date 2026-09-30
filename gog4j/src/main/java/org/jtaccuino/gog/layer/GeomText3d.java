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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * {@link org.jtaccuino.gog.Geoms#text3d()} geometry, billboard method: renders
 * each label as a flat text run at its projected 3D anchor, always facing the
 * camera — no orientation computation is needed because the label is drawn in
 * the view plane after projection.
 * <p>
 * Rows with a blank label are skipped. Font size is scaled by the anchor's
 * depth so farther labels shrink (unless {@link #scaleDepth(boolean)} is
 * disabled); labels are depth-sorted back-to-front so nearer text always sits
 * on top of farther text. {@code hjust}/{@code vjust} anchor the label against
 * its point and {@code angle} rotates it in the view plane.
 * <p>
 * A polygon-outline text renderer is out of scope for this geometry;
 * {@link #textOutlines(boolean)} provides a lighter stand-in
 * that strokes each glyph with a contrasting halo. The
 * {@link #facing(CubeFace)} and {@link #cameraFacing(Object)} options are
 * accepted for source compatibility but have no effect on billboard rendering.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomText3d<DF> implements Layer<DF> {

    /** Halo radius in fraction of the font size. */
    private static final double HALO_FRACTION = 0.09;

    private double fontSize = 11.0;
    private Color color = Color.web("#222222");
    private boolean bold = false;
    private Double constantAlpha;
    private double hjust = 0.5;
    private double vjust = 0.5;
    private double angle = 0.0;
    private boolean scaleDepth = true;
    private boolean textOutlines = false;
    private Color haloColor = Color.web("#ffffff");
    private CubeFace facing = CubeFace.ZMAX; // accepted for source compatibility; unused by billboard

    /**
     * Creates a billboard text geometry with default label styling.
     */
    public GeomText3d() {
    }

    /**
     * Sets the font size in points (before depth scaling).
     *
     * @param fontSize the font size in points
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> size(double fontSize) { this.fontSize = fontSize; return this; }

    /**
     * Sets the text colour.
     *
     * @param color the fill colour
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> color(Color color) { this.color = color; return this; }

    /**
     * Renders the labels in a bold face.
     *
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> bold() { this.bold = true; return this; }

    /**
     * Sets a constant transparency applied to every label.
     *
     * @param alpha opacity between 0.0 and 1.0
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> alpha(double alpha) { this.constantAlpha = alpha; return this; }

    /**
     * Sets the horizontal justification of the label against its anchor, from
     * 0 (left edge at the point) through 0.5 (centred) to 1 (right edge at the
     * point).
     *
     * @param hjust the horizontal justification
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> hjust(double hjust) { this.hjust = hjust; return this; }

    /**
     * Sets the vertical justification of the label against its anchor, from 0
     * (baseline at the point) through 0.5 (centred) to 1 (top edge at the
     * point).
     *
     * @param vjust the vertical justification
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> vjust(double vjust) { this.vjust = vjust; return this; }

    /**
     * Sets the rotation of the labels in degrees clockwise in the view plane.
     *
     * @param angle the clockwise rotation in degrees
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> angle(double angle) { this.angle = angle; return this; }

    /**
     * Enables (default) or disables the perspective size cue that shrinks
     * labels farther from the camera.
     *
     * @param scaleDepth {@code true} to scale with depth, {@code false} for constant size
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> scaleDepth(boolean scaleDepth) { this.scaleDepth = scaleDepth; return this; }

    /**
     * Enables the contrasting halo that strokes each glyph in
     * {@link #haloColor(Color)} — a lighter stand-in for a polygon-outline
     * text renderer. Disabled by default.
     *
     * @param textOutlines {@code true} to render the halo
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> textOutlines(boolean textOutlines) { this.textOutlines = textOutlines; return this; }

    /**
     * Sets the halo colour used when {@link #textOutlines(boolean) outlines}
     * are enabled (default white).
     *
     * @param haloColor the halo stroke colour
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> haloColor(Color haloColor) { this.haloColor = haloColor; return this; }

    /**
     * Selects the rendering method; only {@code "billboard"} is supported.
     *
     * @param method {@code "billboard"} (default) — the only implemented method
     * @return this geometry for fluid chaining
     * @throws IllegalArgumentException for any method other than {@code "billboard"}
     */
    public GeomText3d<DF> method(String method) {
        if (!"billboard".equals(method)) {
            throw new IllegalArgumentException(
                    "Geoms.text3d() method '" + method + "' is not supported; only the billboard method is implemented");
        }
        return this;
    }

    /**
     * Sets the facing direction the labels should point toward. Kept for
     * source compatibility; billboard text always faces the camera so this
     * has no effect.
     *
     * @param facing a cube face, accepted and ignored
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> facing(CubeFace facing) { this.facing = facing; return this; }

    /**
     * The current facing direction (accepted for source compatibility;
     * unused by billboard rendering).
     *
     * @return the configured facing direction, or {@link CubeFace#ZMAX} if unset
     */
    public CubeFace facing() { return facing; }

    /**
     * Camera-facing billboard text. Billboard labels already face the camera,
     * so this accepts the specification and has no further effect.
     *
     * @param facing a camera-facing specification, accepted and ignored
     * @return this geometry for fluid chaining
     */
    public GeomText3d<DF> cameraFacing(Object facing) { return this; }

    private record Label(String text, double sx, double sy, double depthScale,
                         Color color, double fontPx) {}

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var coord = ctx.plot().coord();
        var df = ctx.partitionDf();
        if (!(coord instanceof Coord3D) || aes.z() == null || aes.label() == null) return;

        var rawLabel = ext.getColumn(df, aes.label());

        var cube = CubeStyle.from(ctx.plot().theme());
        double strength = scaleDepth ? cube.depthScaleStrength() : 0.0;
        var mapper = Primitive3dMapper.forPrimitives(ctx, color, constantAlpha != null ? constantAlpha : 1.0, strength);

        var labels = new ArrayList<Label>(mapper.rowCount());
        for (var i = 0; i < mapper.rowCount(); i++) {
            if (i >= rawLabel.size()) break;
            var rl = rawLabel.get(i);
            if (rl == null) continue;
            var text = rl.toString().trim();
            if (text.isEmpty()) continue;
            var pri = mapper.resolve(i);
            if (pri == null) continue;

            labels.add(new Label(text, pri.sx(), pri.sy(), pri.depthScale(),
                    Colors.withAlpha(pri.color(), pri.alpha()), fontSize * pri.depthFactor()));
        }

        if (labels.isEmpty()) return;

        var nearness = new double[labels.size()];
        for (var k = 0; k < labels.size(); k++) {
            nearness[k] = labels.get(k).depthScale();
        }

        gc.setTextAlign(GeomText.alignOf(hjust));
        gc.setTextBaseline(GeomText.baselineOf(vjust));
        boolean rotated = Math.abs(angle) > 1e-9;
        for (var oi : DepthSorter.order(nearness)) {
            var lb = labels.get(oi);
            gc.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, lb.fontPx()));
            // drawLabel() draws at the origin with only the halo offsets; the
            // projected label position must always be applied here, not just for
            // the rotated case, or every label lands on top of the origin.
            gc.save();
            gc.translate(lb.sx(), lb.sy());
            if (rotated) {
                gc.rotate(angle);
            }
            drawLabel(gc, lb);
            gc.restore();
        }
    }

    private void drawLabel(DrawSurface gc, Label lb) {
        if (textOutlines) {
            double r = Math.max(1.0, lb.fontPx() * HALO_FRACTION);
            gc.setStroke(Colors.withAlpha(haloColor, lb.color().getOpacity()));
            gc.setLineWidth(Math.max(1.0, r / 2.0));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    gc.strokeText(lb.text(), dx * r, dy * r);
                }
            }
        }
        gc.setFill(lb.color());
        gc.fillText(lb.text(), 0, 0);
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        if (!(ctx.plot().coord() instanceof Coord3D) || aes.x() == null || aes.y() == null
                || aes.z() == null || aes.label() == null) {
            return null;
        }
        var df = ctx.partitionDf();
        var rawLabel = ext.getColumn(df, aes.label());
        if (rawLabel == null) return null;

        var mapper = Primitive3dMapper.forPrimitives(ctx, color, 1.0, 0.0);
        double threshold = Math.max(10.0, fontSize * 1.2);
        double bestDistSq = Double.POSITIVE_INFINITY;
        String best = null;
        for (int i = 0; i < mapper.rowCount(); i++) {
            if (i >= rawLabel.size()) break;
            var rl = rawLabel.get(i);
            if (rl == null) continue;
            var text = rl.toString().trim();
            if (text.isEmpty()) continue;
            var proj = mapper.project(i);
            if (proj == null) continue;
            double dx = mx - proj.sx();
            double dy = my - proj.sy();
            double distSq = dx * dx + dy * dy;
            if (distSq <= threshold * threshold && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = text;
            }
        }
        return best;
    }

    @Override
    public int estimatedPrimitiveCount(PanelContext<DF> ctx) {
        return ctx.plot().extractor().getRowCount(ctx.partitionDf());
    }
}
