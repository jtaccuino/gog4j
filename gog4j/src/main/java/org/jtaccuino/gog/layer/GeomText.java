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
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatParams;

/**
 * Text annotation geometry ({@code Geoms.text}). Draws the value of the
 * {@link Aes#label(String) label} aesthetic next to each data point; rows whose
 * label is {@code null} or blank are skipped, so a single sparse column can mark
 * just the handful of observations worth naming.
 * <p>
 * The geometry is stat-capable in the same way as {@code GeomBar} or
 * {@code GeomHistogram}: bound to a {@link Stat} it runs the stat over the panel
 * data and resolves each row's x, y, and label from the {@code StatData} output
 * instead of the raw columns. That is how the {@link org.jtaccuino.gog.Geoms#corrText()
 * correlation annotation} — a text layer hosting {@code Stats.cor()} with a label
 * mapped to {@link org.jtaccuino.gog.AesValue#afterStat(org.jtaccuino.gog.AesValue.ComputedVariable)
 * afterStat(CORR)} — draws "r = 0.93" on the single anchor row of each panel.
 * Without a bound stat the geometry reads the raw columns, the default identity
 * behaviour.
 * <p>
 * Labels inherit the theme's {@link org.jtaccuino.gog.theme.Theme#textColor()
 * text color} unless an explicit {@link #color(Color)} override is given.
 * <p>
 * Labels that would collide with an already placed label are pushed further away
 * from the point in steps, a lightweight stand-in for {@code ggrepel}'s force
 * layout that keeps rendering deterministic.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomText<DF> implements Layer<DF>, StatHost<DF> {

    /** Horizontal breathing room between neighbouring labels, in pixels. */
    private static final double LABEL_PADDING = 3.0;

    /** How many line heights a label may be displaced to escape a collision. */
    private static final int MAX_STAGGER = 12;

    /** Safety margin kept between a label and the panel edge, in pixels. */
    private static final double EDGE_MARGIN = 4.0;

    private Stat<DF> stat;
    private StatParams statParams = StatParams.empty();

    private double fontSize = 9.0;

    /** Text colour; {@code null} inherits the theme's {@link org.jtaccuino.gog.theme.Theme#textColor()}. */
    private Color color;
    private boolean bold = false;
    private double dx = 0.0;
    private double dy = -8.0;
    private boolean avoidOverlap = true;
    private int maxLabels = Integer.MAX_VALUE;

    /** The text rotation in degrees clockwise, 0 for horizontal reading. */
    private double angle = 0.0;

    /** The horizontal justification: 0 left, 0.5 centred, 1 right. */
    private double hjust = 0.5;

    /** The vertical justification: 0 bottom, 0.5 centred, 1 top. */
    private double vjust = 0.5;

    /**
     * A constant data-space y anchoring every label, the {@code aes(y = 32)}
     * of the wind-rose example — a whole column of labels shares one radius
     * instead of riding along their points' own y values.
     */
    private Double fixedY = null;

    /**
     * Creates a text annotation geometry with default label styling.
     */
    public GeomText() {
    }

    /**
     * Creates a text annotation geometry bound to a stat: rendering runs the
     * stat over the panel data and draws the resolved label, x and y from its
     * output instead of the raw columns.
     *
     * @param stat the stat to run before drawing, or {@code null} for identity
     */
    public GeomText(Stat<DF> stat) {
        this.stat = stat;
    }

    /**
     * Sets the font size in points.
     *
     * @param fontSize the font size
     * @return this instance for fluid chaining
     */
    public GeomText<DF> size(double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    /**
     * Sets the text colour, overriding the theme's
     * {@link org.jtaccuino.gog.theme.Theme#textColor() text color} that an
     * unset colour inherits.
     *
     * @param color the fill colour
     * @return this instance for fluid chaining
     */
    public GeomText<DF> color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Renders the labels in a bold face.
     *
     * @return this instance for fluid chaining
     */
    public GeomText<DF> bold() {
        this.bold = true;
        return this;
    }

    /**
     * Sets the pixel offset of the label relative to its data point.
     * Negative {@code dy} places the label above the point.
     *
     * @param dx horizontal offset in pixels
     * @param dy vertical offset in pixels
     * @return this instance for fluid chaining
     */
    public GeomText<DF> nudge(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
        return this;
    }

    /**
     * Enables or disables the collision avoidance that staggers overlapping labels.
     *
     * @param avoid {@code true} to stagger colliding labels
     * @return this instance for fluid chaining
     */
    public GeomText<DF> avoidOverlap(boolean avoid) {
        this.avoidOverlap = avoid;
        return this;
    }

    /**
     * Caps how many labels are drawn, in row order. Useful when a label column
     * is denser than the panel can legibly carry.
     *
     * @param maxLabels the maximum number of labels to render
     * @return this instance for fluid chaining
     */
    public GeomText<DF> maxLabels(int maxLabels) {
        this.maxLabels = maxLabels;
        return this;
    }

    /**
     * Sets the rotation of the text in degrees clockwise from horizontal —
     * 90 turns it to read bottom-up, like the vertical y-axis labels of the
     * classic wind rose. Inside a {@link org.jtaccuino.gog.coord.CoordPolar} with
     * {@code rotateAngle} enabled this is the user angle that the coordinate
     * system realigns with each label's position.
     *
     * @param angle the clockwise rotation in degrees
     * @return this instance for fluid chaining
     */
    public GeomText<DF> angle(double angle) {
        this.angle = angle;
        return this;
    }

    /**
     * Sets the horizontal justification of the label against its data point,
     * from 0 (left edge at the point) through 0.5 (centred) to 1 (right edge
     * at the point).
     *
     * @param hjust the horizontal justification
     * @return this instance for fluid chaining
     */
    public GeomText<DF> hjust(double hjust) {
        this.hjust = hjust;
        return this;
    }

    /**
     * Sets the vertical justification of the label against its data point,
     * from 0 (baseline at the point) through 0.5 (centred) to 1 (top edge at
     * the point).
     *
     * @param vjust the vertical justification
     * @return this instance for fluid chaining
     */
    public GeomText<DF> vjust(double vjust) {
        this.vjust = vjust;
        return this;
    }

    /**
     * Anchors every label at a constant data-space y position, regardless of
     * each label's own y value — the {@code aes(y = 32)} of the wind rose,
     * where all car names hang on one radius near the rim.
     *
     * @param y the fixed data value of the label's y coordinate
     * @return this instance for fluid chaining
     */
    public GeomText<DF> y(double y) {
        this.fixedY = y;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the stat this geometry was constructed or bound with, or
     * {@code null} for plain identity text.
     */
    @Override
    public Stat<DF> defaultStat() {
        return stat;
    }

    @Override
    public void attach(Stat<DF> stat, PositionAdjust position, LayerParams params) {
        this.stat = stat;
        this.statParams = params.stat();
    }

    /**
     * The per-row drawing data: x, y, and raw label for each row index. Built
     * either from the raw panel columns (identity mode) or from the bound stat's
     * output (stat mode, resolving the label against the {@code StatData}).
     *
     * @return the row source, or {@code null} when there is nothing to draw
     */
    private Rows rows(PanelContext<DF> ctx, DataExtractor<DF> ext, Aes aes) {
        var df = ctx.partitionDf();
        if (stat == null) {
            if (aes.label() == null) {
                return null;
            }
            var rawX = ext.getColumn(df, aes.x());
            var rawY = ext.getColumn(df, aes.y());
            var rawLabel = ext.getColumn(df, aes.label());
            var n = Math.min(ext.getRowCount(df), rawLabel.size());
            if (n == 0) {
                return null;
            }
            return new Rows() {
                @Override
                public int size() {
                    return n;
                }

                @Override
                public Object x(int i) {
                    return rawX.get(i);
                }

                @Override
                public Object y(int i) {
                    return rawY.get(i);
                }

                @Override
                public Object label(int i) {
                    return rawLabel.get(i);
                }
            };
        }
        if (aes.labelValue() == null || aes.x() == null || aes.y() == null) {
            return null;
        }
        var statData = stat.computeLayer(df, ext, aes, statParams);
        if (statData.isEmpty()) {
            return null;
        }
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        if (xCol == null || yCol == null) {
            return null;
        }
        var res = AesRendering.resolution(ctx, aes, statData, null);
        var labelValue = aes.labelValue();
        int n = statData.rowCount();
        return new Rows() {
            @Override
            public int size() {
                return n;
            }

            @Override
            public Object x(int i) {
                return xCol.get(i);
            }

            @Override
            public Object y(int i) {
                return yCol.get(i);
            }

            @Override
            public Object label(int i) {
                return labelValue.resolve(res, i);
            }
        };
    }

    @Override
    public void render(DrawSurface gc, PanelContext<DF> ctx, LayerData data) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var rows = rows(ctx, ext, aes);
        if (rows == null) return;

        var font = Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize);
        var measurer = new Text();
        measurer.setFont(font);

        gc.save();
        gc.beginPath();
        gc.rect(sx.minPixel(), sy.maxPixel(), sx.maxPixel() - sx.minPixel(), sy.minPixel() - sy.maxPixel());
        gc.clip();
        gc.setFont(font);
        gc.setFill(color != null ? color : ctx.plot().theme().textColor());
        gc.setTextAlign(TextAlignment.CENTER);

        var placed = new ArrayList<double[]>();
        var lineHeight = fontSize + 2.0;
        var drawn = 0;
        var rotated = stat == null
                && (Math.abs(angle) > 1e-9
                        || (coord instanceof CoordPolar polar && polar.isRotateAngle()));

        for (var i = 0; i < rows.size() && drawn < maxLabels; i++) {
            var rl = rows.label(i);
            if (rl == null) continue;
            var text = rl.toString().trim();
            if (text.isEmpty()) continue;

            var rx = rows.x(i);
            var ry = rows.y(i);
            if (rx == null || (ry == null && (stat != null || fixedY == null))) continue;

            var xData = sx.toData(rx);
            var yData = stat != null || fixedY == null ? sy.toData(ry) : fixedY;
            var cx = coord.xPixel(sx, sy, xData, yData) + dx;
            var cy = coord.yPixel(sx, sy, xData, yData) + dy;

            if (rotated) {
                drawRotated(gc, coord, xData, yData, text, cx, cy);
                drawn++;
                continue;
            }

            measurer.setText(text);
            var w = measurer.getLayoutBounds().getWidth();

            // Keep the label inside the panel; the clip would otherwise silently
            // cut characters off labels near an edge, or behead those at the top.
            // The margin absorbs the metric differences of other text engines —
            // an SVG renderer lays the same string out slightly wider or narrower.
            var half = w / 2.0 + EDGE_MARGIN;
            if (sx.maxPixel() - sx.minPixel() > 2 * half) {
                cx = Math.max(sx.minPixel() + half, Math.min(sx.maxPixel() - half, cx));
            }
            // maxPixel is the top of the panel, minPixel the bottom: y grows downwards.
            cy = Math.max(sy.maxPixel() + lineHeight, Math.min(sy.minPixel(), cy));

            // Pad horizontally so neighbouring labels separate rather than abut
            var left = cx - half - LABEL_PADDING;
            var right = cx + half + LABEL_PADDING;

            var placedY = avoidOverlap
                    ? findFreeRow(left, right, cy, lineHeight, placed, sy)
                    : Double.valueOf(cy);
            if (placedY == null) continue;

            gc.fillText(text, cx, placedY);
            placed.add(new double[]{left, placedY - lineHeight, right, placedY});
            drawn++;
        }

        gc.restore();
    }

    /**
     * Draws a label rotated at its data point. The rotation comes from the
     * geom's own {@link #angle(double) angle} — 90 for the vertical labels of
     * the classic wind rose — and, inside a polar coordinate system with
     * {@code rotateAngle}, is realigned with the label's angular position so
     * the text tangents or parallels the ring it sits on. The hjust/vjust
     * anchoring is applied in the rotated frame, and a realigned label that
     * would read upside down is turned right-side up with its anchors flipped,
     * exactly the {@code textAngleAdjustment} dance of the default.
     */
    private void drawRotated(DrawSurface gc, Coord coord,
                             double xData, double yData, String text, double cx, double cy) {
        var effective = angle;
        var flip = false;
        if (coord instanceof CoordPolar polar && polar.isRotateAngle()) {
            var thetaData = polar.isThetaX() ? xData : yData;
            var rotation = polar.textRotation(thetaData, angle);
            effective = rotation.degrees();
            flip = rotation.flipped();
        }
        var h = flip ? 1.0 - hjust : hjust;
        var v = flip ? 1.0 - vjust : vjust;
        gc.setTextAlign(alignOf(h));
        gc.setTextBaseline(baselineOf(v));
        if (Math.abs(effective) < 1e-9) {
            gc.fillText(text, cx, cy);
            return;
        }
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(effective);
        gc.fillText(text, 0, 0);
        gc.restore();
    }

    /** Maps a horizontal justification to its text alignment. */
    static TextAlignment alignOf(double hjust) {
        if (hjust > 0.75) return TextAlignment.RIGHT;
        if (hjust < 0.25) return TextAlignment.LEFT;
        return TextAlignment.CENTER;
    }

    /** Maps a vertical justification to its text baseline. */
    static VPos baselineOf(double vjust) {
        if (vjust > 0.75) return VPos.TOP;
        if (vjust < 0.25) return VPos.BOTTOM;
        return VPos.CENTER;
    }

    /**
     * Looks for a free row for a label, stepping away from the data point in the
     * nudge direction first and then the other way. Searching both directions
     * matters where several labels share a height near a panel edge and cannot
     * all move outwards.
     *
     * @return the baseline y to draw at, or {@code null} if no free row was found
     */
    private static Double findFreeRow(double left, double right, double preferredY,
                                      double lineHeight, List<double[]> placed, Scale sy) {
        var top = sy.maxPixel() + lineHeight;
        var bottom = sy.minPixel();
        for (var attempt = 0; attempt <= 2 * MAX_STAGGER; attempt++) {
            // 0, -1, +1, -2, +2, … in units of one line height
            var k = (attempt + 1) / 2;
            var candidate = preferredY + (attempt % 2 == 1 ? -k : k) * lineHeight;
            if (candidate < top || candidate > bottom) continue;
            var box = new double[]{left, candidate - lineHeight, right, candidate};
            if (!intersectsAny(box, placed)) return candidate;
        }
        return null;
    }

    private static boolean intersectsAny(double[] box, List<double[]> placed) {
        for (var other : placed) {
            var separate = box[2] <= other[0] || box[0] >= other[2]
                        || box[3] <= other[1] || box[1] >= other[3];
            if (!separate) return true;
        }
        return false;
    }

    @Override
    public String locate(PanelContext<DF> ctx, LayerData data, double mx, double my) {
        var ext = ctx.plot().extractor();
        var aes = ctx.plot().aes();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var coord = ctx.plot().coord();
        var rows = rows(ctx, ext, aes);
        if (rows == null) {
            return null;
        }
        var rotated = stat == null
                && (Math.abs(angle) > 1e-9
                        || (coord instanceof CoordPolar polar && polar.isRotateAngle()));
        if (rotated) {
            return null;
        }

        var font = Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize);
        var measurer = new Text();
        measurer.setFont(font);
        var lineHeight = fontSize + 2.0;
        for (var i = 0; i < rows.size() && i < maxLabels; i++) {
            var rl = rows.label(i);
            if (rl == null) {
                continue;
            }
            var text = rl.toString().trim();
            if (text.isEmpty()) {
                continue;
            }
            var rx = rows.x(i);
            var ry = rows.y(i);
            if (rx == null || (ry == null && (stat != null || fixedY == null))) {
                continue;
            }
            var xData = sx.toData(rx);
            var yData = stat != null || fixedY == null ? sy.toData(ry) : fixedY;
            var cx = coord.xPixel(sx, sy, xData, yData) + dx;
            var cy = coord.yPixel(sx, sy, xData, yData) + dy;

            measurer.setText(text);
            var half = measurer.getLayoutBounds().getWidth() / 2.0 + EDGE_MARGIN;
            if (sx.maxPixel() - sx.minPixel() > 2 * half) {
                cx = Math.max(sx.minPixel() + half, Math.min(sx.maxPixel() - half, cx));
            }
            cy = Math.max(sy.maxPixel() + lineHeight, Math.min(sy.minPixel(), cy));

            var left = cx - half;
            var right = cx + half;
            var top = cy - lineHeight;
            var bottom = cy;
            if (mx >= left && mx <= right && my >= top && my <= bottom) {
                return text;
            }
        }
        return null;
    }

    /** Per-row x/y/label access, fed from raw columns or a stat's output. */
    private interface Rows {
        int size();

        Object x(int i);

        Object y(int i);

        Object label(int i);
    }
}
