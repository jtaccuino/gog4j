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
package org.jtaccuino.gog.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.theme.Anchor;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * The discrete guide: one key per category, each showing the layer's visual
 * encoding (color swatch, midline, point shape) next to its label. Mirrors
 * the {@code Guides.guideLegend()}: keys flow vertically in a single column,
 * or in a {@code nrow}/{@code ncol} grid, or horizontally for a top or bottom
 * legend slot.
 * <p>
 * Instances are immutable; each {@code with}-style setter returns a new guide
 * with that single property changed. Create them via {@link org.jtaccuino.gog.Guides#guideLegend()}.
 */
public final class GuideLegend extends Guide<GuideLegend.Data> {

    /**
     * One legend key: a category with its label, color, optional shape,
     * optional point radius for size mappings, and optional line dash pattern
     * for line-type mappings.
     *
     * @param value  the category value the key represents
     * @param label  the label shown next to the key
     * @param color  the color the key is drawn with
     * @param shape  the point shape, or {@code null} when no shape is drawn
     * @param radius the point radius, or {@code null} when not size-mapped
     * @param dashes the dash pattern for the key's line, or {@code null} for a
     *               solid line
     */
    public record Key(Object value, String label, Color color, PointShape shape, Double radius, List<Double> dashes) {

        /**
         * Constructs a key without a dash pattern.
         *
         * @param value  the category value the key represents
         * @param label  the label shown next to the key
         * @param color  the color the key is drawn with
         * @param shape  the point shape, or {@code null} when no shape is drawn
         * @param radius the point radius, or {@code null} when not size-mapped
         */
        public Key(Object value, String label, Color color, PointShape shape, Double radius) {
            this(value, label, color, shape, radius, null);
        }

        /**
         * Constructs a key without an explicit point radius or dash pattern.
         *
         * @param value the category value the key represents
         * @param label the label shown next to the key
         * @param color the color the key is drawn with
         * @param shape the point shape, or {@code null} when no shape is drawn
         */
        public Key(Object value, String label, Color color, PointShape shape) {
            this(value, label, color, shape, null);
        }

        /**
         * Returns the dash pattern as a {@code double[]} suitable for a draw
         * surface, or {@code null} for a solid line.
         *
         * @return the dash array, or {@code null} when the key is drawn solid
         */
        public double[] dashArray() {
            if (dashes == null) return null;
            var arr = new double[dashes.size()];
            for (int i = 0; i < dashes.size(); i++) arr[i] = dashes.get(i);
            return arr;
        }
    }

    /**
     * The per-plot data a legend guide renders against.
     *
     * @param columnName the column mapped to the colour aesthetic
     * @param keys       the legend keys, one per category
     * @param smooth     whether a smooth line is drawn for each key
     * @param drawBand   whether a confidence band is drawn for each key
     * @param bandColor  the band fill color, or {@code null} for the key's color
     * @param points     whether point glyphs are drawn for each key
     * @param lines      whether a line is drawn for each key
     * @param sized      whether the legend lays out scaled-size keys
     */
    public record Data(String columnName, List<Key> keys, boolean smooth, boolean drawBand,
                       Color bandColor, boolean points, boolean lines,
                       boolean sized) implements GuideData {

        /**
         * Creates legend data without a scaled-size mapping; {@code sized}
         * defaults to {@code false}.
         *
         * @param columnName the column mapped to the colour aesthetic
         * @param keys       the legend keys, one per category
         * @param smooth     whether a smooth line is drawn for each key
         * @param drawBand   whether a confidence band is drawn for each key
         * @param bandColor  the band fill color, or {@code null} for the key's color
         * @param points     whether point glyphs are drawn for each key
         * @param lines      whether a line is drawn for each key
         */
        public Data(String columnName, List<Key> keys, boolean smooth, boolean drawBand,
                    Color bandColor, boolean points, boolean lines) {
            this(columnName, keys, smooth, drawBand, bandColor, points, lines, false);
        }
    }

    private final double keyWidth;
    private final double rowHeight;
    private final int nrow;
    private final int ncol;
    private final boolean byrow;
    private final double keySpacingX;
    private final SizeLegendStyle sizeStyle;
    private final GuideShading shade;

    /**
     * Constructs a legend guide, inheriting the theme's guide position.
     *
     * @param title        the guide title, or {@code null} for none
     * @param direction    the flow direction of the keys
     * @param reverse      whether to draw the keys in reverse order
     * @param order        the stacking order among multiple guides
     * @param keyWidth     the width of each key square in pixels
     * @param rowHeight    the vertical space per key row in pixels
     * @param nrow         the desired number of key rows, or {@code 0} to infer
     * @param ncol         the desired number of key columns, or {@code 0} to infer
     * @param byrow        whether to fill keys row by row instead of column by column
     * @param keySpacingX  the horizontal gap between a key and its label
     */
    public GuideLegend(String title, Direction direction, boolean reverse, int order,
                double keyWidth, double rowHeight, int nrow, int ncol,
                boolean byrow, double keySpacingX) {
        this(title, direction, reverse, order, null, keyWidth, rowHeight, nrow, ncol,
                byrow, keySpacingX);
    }

    /**
     * Constructs a legend guide with the given properties and an explicit
     * strip-side position.
     *
     * @param position    the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param title       the guide title, or {@code null} for none
     * @param direction   the flow direction of the keys
     * @param reverse     {@code true} to draw the keys in reverse order
     * @param order       the stacking order among multiple guides
     * @param keyWidth    the width of each key square in pixels
     * @param rowHeight   the vertical space per key row in pixels
     * @param nrow        the desired number of key rows, or {@code 0} to infer
     * @param ncol        the desired number of key columns, or {@code 0} to infer
     * @param byrow       {@code true} to fill keys row by row instead of column by column
     * @param keySpacingX the horizontal gap between a key and its label
     */
    public GuideLegend(String title, Direction direction, boolean reverse, int order, GuidePosition position,
                double keyWidth, double rowHeight, int nrow, int ncol,
                boolean byrow, double keySpacingX) {
        this(title, direction, reverse, order, position, null, keyWidth, rowHeight, nrow, ncol,
                byrow, keySpacingX);
    }

    /**
     * Constructs a legend guide with the given properties and per-guide theme
     * overrides.
     *
     * @param position    the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param title       the guide title, or {@code null} for none
     * @param direction   the flow direction of the keys
     * @param reverse     {@code true} to draw the keys in reverse order
     * @param order       the stacking order among multiple guides
     * @param guideTheme  per-guide style overrides over the theme, or {@code null} to inherit
     * @param keyWidth    the width of each key square in pixels
     * @param rowHeight   the vertical space per key row in pixels
     * @param nrow        the desired number of key rows, or {@code 0} to infer
     * @param ncol        the desired number of key columns, or {@code 0} to infer
     * @param byrow       {@code true} to fill keys row by row instead of column by column
     * @param keySpacingX the horizontal gap between a key and its label
     */
    public GuideLegend(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme,
            double keyWidth, double rowHeight, int nrow, int ncol,
            boolean byrow, double keySpacingX) {
        this(title, direction, reverse, order, position, guideTheme, null,
                keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX);
    }

    /**
     * Constructs a legend guide with the given properties, per-guide theme
     * overrides, and inside-panel anchor.
     *
     * @param position     the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param title        the guide title, or {@code null} for none
     * @param direction    the flow direction of the keys
     * @param reverse      {@code true} to draw the keys in reverse order
     * @param order        the stacking order among multiple guides
     * @param guideTheme   per-guide style overrides over the theme, or {@code null} to inherit
     * @param insideAnchor the panel-relative anchor for INSIDE placement, or {@code null} to inherit
     * @param keyWidth     the width of each key square in pixels
     * @param rowHeight    the vertical space per key row in pixels
     * @param nrow         the desired number of key rows, or {@code 0} to infer
     * @param ncol         the desired number of key columns, or {@code 0} to infer
     * @param byrow        {@code true} to fill keys row by row instead of column by column
     * @param keySpacingX  the horizontal gap between a key and its label
     */
    public GuideLegend(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor,
            double keyWidth, double rowHeight, int nrow, int ncol,
            boolean byrow, double keySpacingX) {
        this(title, direction, reverse, order, position, guideTheme, insideAnchor, SizeLegendStyle.NESTED,
                keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, null);
    }

    /**
     * Creates a legend with the given key-layout configuration.
     *
     * @param title        the legend title, or {@code null}
     * @param direction    how to lay out the keys
     * @param reverse      whether to reverse the key order
     * @param order        the priority for resolving ties in guide placement
     * @param position     the strip side this legend is placed on, or {@code null}
     *                     to inherit the theme
     * @param guideTheme   the theme used to style this legend
     * @param insideAnchor the anchor used when the legend is placed inside the panel
     * @param sizeStyle    how a size legend lays out its circles
     * @param keyWidth     the width of each key square in pixels
     * @param rowHeight    the vertical space per key row in pixels
     * @param nrow         the desired number of key rows, or {@code 0} when unset
     * @param ncol         the desired number of key columns, or {@code 0} when unset
     * @param byrow        whether keys fill row-first rather than column-first
     * @param keySpacingX  the horizontal gap between a key and its label
     * @param shade        the 3-D shading applied to each key, or {@code null} for none
     */
    public GuideLegend(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor, SizeLegendStyle sizeStyle,
            double keyWidth, double rowHeight, int nrow, int ncol,
            boolean byrow, double keySpacingX, GuideShading shade) {
        super(title, direction, reverse, order, position, guideTheme, insideAnchor);
        this.keyWidth = keyWidth;
        this.rowHeight = rowHeight;
        this.nrow = nrow;
        this.ncol = ncol;
        this.byrow = byrow;
        this.keySpacingX = keySpacingX;
        this.sizeStyle = sizeStyle == null ? SizeLegendStyle.NESTED : sizeStyle;
        this.shade = shade;
    }

    /** {@return the width of each key square in pixels} */
    public double keyWidth() { return keyWidth; }

    /** {@return the vertical space per key row in pixels} */
    public double rowHeight() { return rowHeight; }

    /** {@return the desired number of key rows, or {@code 0} when unset} */
    public int nrow() { return nrow; }

    /** {@return the desired number of key columns, or {@code 0} when unset} */
    public int ncol() { return ncol; }

    /** {@return whether keys fill rows before columns} */
    public boolean byrow() { return byrow; }

    /** {@return the horizontal gap between a key and its label} */
    public double keySpacingX() { return keySpacingX; }

    /**
     * {@return a copy of this legend with the title set}
     *
     * @param title the guide title, or {@code null} for none
     */
    @Override
    public GuideLegend title(String title) {
        return new GuideLegend(title, direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the flow direction set}
     *
     * @param direction the flow direction of the keys
     */
    public GuideLegend direction(Direction direction) {
        return new GuideLegend(title(), direction, reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the reverse flag set}
     *
     * @param reverse {@code true} to draw the keys in reverse order
     */
    public GuideLegend reverse(boolean reverse) {
        return new GuideLegend(title(), direction(), reverse, order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the stacking order set}
     *
     * @param order the stacking order among multiple guides
     */
    public GuideLegend order(int order) {
        return new GuideLegend(title(), direction(), reverse(), order, position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the strip side set}
     *
     * @param position the strip side this guide is placed on, overriding the
     *                 theme's guide position; {@code null} inherits the theme
     */
    @Override
    public GuideLegend position(GuidePosition position) {
        return new GuideLegend(title(), direction(), reverse(), order(), position, guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * Returns a copy of this legend with per-guide style overrides.
     *
     * @param guideTheme per-guide style overrides over the theme; {@code null}
     *                   inherits everything from the theme
     * @return a copy of this legend carrying the given overrides
     */
    public GuideLegend theme(GuideTheme guideTheme) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme, insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * Returns a copy of this legend anchored inside the panel.
     *
     * @param x relative horizontal anchor in [0, 1] of the panel
     * @param y relative vertical anchor in [0, 1] of the panel
     * @return a copy of this guide anchored at the given panel point when
     *         placed {@link org.jtaccuino.gog.theme.GuidePosition#INSIDE}
     */
    public GuideLegend inside(double x, double y) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(),
                new Anchor(x, y), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the key width set}
     *
     * @param keyWidth the width of each key square in pixels
     */
    public GuideLegend keyWidth(double keyWidth) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the row height set}
     *
     * @param rowHeight the vertical space per key row in pixels
     */
    public GuideLegend rowHeight(double rowHeight) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the row count set}
     *
     * @param nrow the desired number of key rows, or {@code 0} to infer
     */
    public GuideLegend nrow(int nrow) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the column count set}
     *
     * @param ncol the desired number of key columns, or {@code 0} to infer
     */
    public GuideLegend ncol(int ncol) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the fill order set}
     *
     * @param byrow {@code true} to fill keys row by row instead of column by column
     */
    public GuideLegend byrow(boolean byrow) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * {@return a copy of this legend with the key-to-label gap set}
     *
     * @param keySpacingX the horizontal gap between a key and its label
     */
    public GuideLegend keySpacingX(double keySpacingX) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /**
     * Sets how a size legend lays out its circles.
     *
     * @param sizeStyle how a size legend lays out its circles; only affects
     *                  legends whose data is a size mapping
     * @return a copy of this legend with the size-layout style set
     */
    public GuideLegend sizeStyle(SizeLegendStyle sizeStyle) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    /** {@return how a size legend lays out its circles} */
    public SizeLegendStyle sizeStyle() { return sizeStyle; }

    /**
     * {@return the 3-D shading applied to each key, or {@code null} for a
     * plain 2-D legend}
     */
    public GuideShading shading() { return shade; }

    /**
     * {@return a copy of this legend shaded as a 3-D legend, giving every key}
     * a uniformly lit-tile look.
     *
     * @param shade the 3-D shading to apply, or {@code null} to clear it
     */
    public GuideLegend shade(GuideShading shade) {
        return new GuideLegend(title(), direction(), reverse(), order(), position(), guideTheme(),
                insideAnchor(), sizeStyle, keyWidth, rowHeight, nrow, ncol, byrow, keySpacingX, shade);
    }

    @Override
    public double measure(Data data, double width) {
        var keys = orderedKeys(data);
        if (keys.isEmpty()) {
            return 0.0;
        }
        if (data.sized()) {
            return sizedMeasure(keys, data);
        }
        double height = grid(keys, width).rows() * rowHeight;
        // A horizontal legend carries its title beside the keys, so it adds
        // no height; a vertical one stacks the (possibly wrapped) title rows
        // on top.
        if (effectiveDirection() != Direction.HORIZONTAL) {
            height += verticalTitleHeight(verticalContentWidth(data, keys));
        }
        return height;
    }

    @Override
    public double preferredWidth(Data data) {
        var keys = orderedKeys(data);
        if (keys.isEmpty()) {
            return 0.0;
        }
        if (data.sized()) {
            return sizedPreferredWidth(keys);
        }
        if (effectiveDirection() == Direction.HORIZONTAL) {
            double total = sideTitleWidth();
            for (var key : keys) {
                total += keyWidth + keySpacingX + labelWidth(key.label());
            }
            return total;
        }
        int cols = columnCount(keys);
        double[] columnWidths = new double[cols];
        int rows = rowsFor(keys, cols);
        for (int i = 0; i < keys.size(); i++) {
            columnWidths[columnOf(i, cols, rows)] = Math.max(columnWidths[columnOf(i, cols, rows)],
                    keyWidth + keySpacingX + labelWidth(keys.get(i).label()));
        }
        double total = 0.0;
        for (double w : columnWidths) {
            total += w;
        }
        // The title is drawn left-anchored above the first column and extends
        // right of the legend's edge, so the legend must be at least as wide
        // as its widest title line to keep it inside the plot. Long titles are
        // wrapped so they sit close to the narrow keys instead of inflating
        // the strip with empty right margin.
        double titleMax = 0.0;
        for (String line : titleLines(total)) {
            titleMax = Math.max(titleMax, titleTextWidth(line));
        }
        return Math.max(total, titleMax);
    }

    @Override
    public void render(DrawSurface gc, Data data, double x, double y, double width, GuideStyle style) {
        var keys = orderedKeys(data);
        if (keys.isEmpty()) {
            return;
        }
        if (data.sized()) {
            renderSized(gc, data, keys, x, y, style);
            return;
        }
        double ly = y;
        double contentX = x;

        if (title() != null && !title().isEmpty()) {
            gc.setFill(style.titleColor());
            gc.setFont(style.titleFont());
            // the per-direction defaults: a vertical legend reads its
            // title above the keys, a horizontal one beside them.
            if (effectiveDirection() == Direction.HORIZONTAL) {
                int cols = grid(keys, width).cols();
                int rows = rowsFor(keys, cols);
                gc.setTextAlign(TextAlignment.LEFT);
                // Share the key labels' baseline line: centered across the
                // rows, using the same offset the labels use within a row.
                double midRow = (rows - 1) * rowHeight / 2.0;
                gc.fillText(title(), contentX, ly + midRow + keyWidth / 2.0 + metrics().titleOffsetY());
                contentX += sideTitleWidth();
            } else {
                gc.setTextAlign(TextAlignment.LEFT);
                for (String line : titleLines(verticalContentWidth(data, keys))) {
                    gc.fillText(line, contentX, ly + 12);
                    ly += keyTitleHeight();
                }
            }
        }

        int cols = grid(keys, width).cols();
        int rows = rowsFor(keys, cols);
        double[] columnX = new double[cols + 1];
        double[] columnWidths = new double[cols];
        for (int c = 0; c < cols; c++) {
            columnWidths[c] = columnWidth(keys, rows, cols, c);
            columnX[c + 1] = columnX[c] + columnWidths[c];
        }

        for (int i = 0; i < keys.size(); i++) {
            int c = columnOf(i, cols, rows);
            int r = (effectiveDirection() == Direction.HORIZONTAL || byrow) ? i / cols : i % rows;
            double kx = contentX + columnX[c];
            double ky = ly + r * rowHeight;
            drawKey(gc, data, keys.get(i), kx, ky, style);
        }
    }

    /** The width the title occupies beside a horizontal legend's keys. */
    private double sideTitleWidth() {
        if (title() == null || title().isEmpty()) {
            return 0.0;
        }
        return labelWidth(title()) + metrics().titleSidePad();
    }

    /**
     * The extra vertical padding per key row of a sized legend, keeping
     * neighbouring circles from touching.
     */
    private double sizedKeyPad() {
        return metrics().sizedKeyPad();
    }

    /** The height of a vertical legend's title row. */
    private double keyTitleHeight() {
        return metrics().keyTitleHeight();
    }

    /** The compositing variant treats several consecutive sized keys as one. */
    private boolean sizedComposited() {
        return sizeStyle == SizeLegendStyle.NESTED;
    }

    /** The diameter of a sized key's cell: twice its radius, with a floor. */
    private static double sizedDiameter(Key key) {
        double r = key.radius() != null ? key.radius() : 2.5;
        return 2.0 * r;
    }

    /** The vertical space a sized key's row occupies. */
    private double sizedRowHeight(Key key) {
        return sizedDiameter(key) + sizedKeyPad();
    }

    /** The width of a sized key's cell including its label. */
    private double sizedCellWidth(Key key) {
        return sizedDiameter(key) + keySpacingX + labelWidth(key.label());
    }

    /** The vertical extent of a sized legend: per-key rows plus the title. */
    private double sizedMeasure(List<Key> keys, @SuppressWarnings("unused") Data data) {
        if (effectiveDirection() == Direction.HORIZONTAL) {
            double maxRow = 0.0;
            for (var key : keys) {
                maxRow = Math.max(maxRow, sizedRowHeight(key));
            }
            return maxRow;
        }
        double height = 0.0;
        for (var key : keys) {
            height += sizedRowHeight(key);
        }
        return height + titleHeight();
    }

    /** The horizontal extent of a sized legend. */
    private double sizedPreferredWidth(List<Key> keys) {
        if (effectiveDirection() == Direction.HORIZONTAL) {
            double total = sideTitleWidth();
            for (var key : keys) {
                total += sizedCellWidth(key);
            }
            return total;
        }
        double maxCell = 0.0;
        for (var key : keys) {
            maxCell = Math.max(maxCell, sizedCellWidth(key));
        }
        // A vertical sized legend stacks its title above the tiny key circles,
        // so the strip must stretch to fit the title even when every key cell is
        // narrower than it.
        return Math.max(maxCell, sideTitleWidth());
    }

    /**
     * Renders a size legend. Each key gets a cell sized to its own diameter;
     * in the {@linkplain SizeLegendStyle#NESTED nested} style the larger keys
     * composite their smaller successors concentrically behind them, and in the
     * {@linkplain SizeLegendStyle#SEPARATED} style every circle sits in its own
     * non-overlapping row.
     */
    private void renderSized(DrawSurface gc, @SuppressWarnings("unused") Data data, List<Key> keys, double x, double y, GuideStyle style) {
        double ly = y;
        double contentX = x;

        if (title() != null && !title().isEmpty()) {
            gc.setFill(style.titleColor());
            gc.setFont(style.titleFont());
            gc.setTextAlign(TextAlignment.LEFT);
            if (effectiveDirection() == Direction.HORIZONTAL) {
                double maxRow = 0.0;
                for (var key : keys) {
                    maxRow = Math.max(maxRow, sizedRowHeight(key));
                }
                gc.fillText(title(), contentX, ly + maxRow / 2.0 + metrics().titleOffsetY());
                contentX += sideTitleWidth();
            } else {
                gc.fillText(title(), contentX, ly + 12);
                ly += keyTitleHeight();
            }
        }

        if (effectiveDirection() == Direction.HORIZONTAL) {
            for (int i = 0; i < keys.size(); i++) {
                drawSizedKey(gc, keys, i, contentX, y, style);
                contentX += sizedCellWidth(keys.get(i));
            }
            return;
        }

        // The widest key's diameter anchors one fixed column: every circle is
        // centred on it (so shapes line up) and every label starts just past it
        // (so labels are left-aligned), regardless of each key's own size.
        double maxDiameter = 0.0;
        for (var key : keys) {
            maxDiameter = Math.max(maxDiameter, sizedDiameter(key));
        }
        double centerX = x + maxDiameter / 2.0;
        double labelX = x + maxDiameter + keySpacingX;

        for (int i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            double rowHeight = sizedRowHeight(key);
            double centerY = ly + rowHeight / 2.0;
            if (sizedComposited()) {
                // Concentric circles of every key up to and including this one
                // sit in this cell, largest first, so the stack grows down the
                // legend and each row's visible size is that row's own key.
                for (int j = i; j >= 0; j--) {
                    var inner = keys.get(j);
                    drawCircle(gc, style, centerX, centerY, inner.radius() != null ? inner.radius() : 2.5,
                            inner.color());
                }
            } else {
                drawCircle(gc, style, centerX, centerY,
                        key.radius() != null ? key.radius() : 2.5, key.color());
            }
            gc.setFill(style.keyColor());
            gc.setFont(style.keyFont());
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(key.label(), labelX, centerY + metrics().titleOffsetY());
            ly += rowHeight;
        }
    }

    /** Outlines a sized key circle in the legend's neutral grey. */
    private static void drawCircle(DrawSurface gc, @SuppressWarnings("unused") GuideStyle style, double cx, double cy, double r, Color color) {
        gc.setFill(color);
        gc.setStroke(color.darker());
        gc.setLineWidth(0.5);
        gc.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        gc.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    /** Draws a single sized key in a horizontal sized legend. */
    private void drawSizedKey(DrawSurface gc, List<Key> keys, int i, double x, double y, GuideStyle style) {
        var key = keys.get(i);
        double rowHeight = sizedRowHeight(key);
        double centerY = y + rowHeight / 2.0;
        double centerX = x + sizedDiameter(key) / 2.0;
        if (sizedComposited()) {
            for (int j = i; j >= 0; j--) {
                var inner = keys.get(j);
                drawCircle(gc, style, centerX, centerY, inner.radius() != null ? inner.radius() : 2.5,
                        inner.color());
            }
        } else {
            drawCircle(gc, style, centerX, centerY,
                    key.radius() != null ? key.radius() : 2.5, key.color());
        }
        gc.setFill(style.keyColor());
        gc.setFont(style.keyFont());
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(key.label(), x + sizedDiameter(key) + keySpacingX, centerY + metrics().titleOffsetY());
    }

    /** The keys in draw order, honoring {@link #reverse()}. */
    private List<Key> orderedKeys(Data d) {
        var keys = new ArrayList<>(d.keys());
        if (reverse()) {
            Collections.reverse(keys);
        }
        return keys;
    }

    /** The number of key columns for the given key count and width. */
    private int columnCount(List<Key> keys) {
        if (ncol > 0) {
            return Math.min(ncol, keys.size());
        }
        if (nrow > 0) {
            return Math.max(1, (int) Math.ceil((double) keys.size() / nrow));
        }
        if (effectiveDirection() == Direction.HORIZONTAL) {
            return keys.size();
        }
        return 1;
    }

    /** The number of rows given a column count. */
    private int rowsFor(List<Key> keys, int cols) {
        return (int) Math.ceil((double) keys.size() / cols);
    }

    /** The grid geometry: columns and rows derived from the key count. */
    private Grid grid(List<Key> keys, double width) {
        int cols;
        if (ncol > 0) {
            cols = Math.min(ncol, keys.size());
        } else if (nrow > 0) {
            cols = Math.max(1, (int) Math.ceil((double) keys.size() / nrow));
        } else if (effectiveDirection() == Direction.HORIZONTAL) {
            cols = Math.max(1, columnsThatFit(keys, width));
        } else {
            cols = 1;
        }
        return new Grid(cols, rowsFor(keys, cols));
    }

    /** The column of key {@code i} in a grid of the given geometry. */
    private int columnOf(int i, int cols, int rows) {
        return (effectiveDirection() == Direction.HORIZONTAL || byrow) ? i % cols : i / rows;
    }

    /** The width of a column: the widest key cell it holds. */
    private double columnWidth(List<Key> keys, int rows, int cols, int col) {
        double max = 0.0;
        for (int i = 0; i < keys.size(); i++) {
            if (columnOf(i, cols, rows) == col) {
                max = Math.max(max, keyWidth + keySpacingX + labelWidth(keys.get(i).label()));
            }
        }
        return max;
    }

    /** How many keys fit in the first row of a width-constrained legend. */
    private int columnsThatFit(List<Key> keys, double width) {
        double used = 0.0;
        int cols = 0;
        for (var key : keys) {
            double cell = keyWidth + keySpacingX + labelWidth(key.label());
            if (used > 0 && used + cell > width) {
                break;
            }
            used += cell;
            cols++;
        }
        return Math.max(1, cols);
    }

    private void drawKey(DrawSurface gc, Data d, Key key, double x, double y, GuideStyle style) {
        var color = key.color();
        if (shade != null && color != null) {
            color = Colors.shade(color, shade.keyShade(), shade.hsl());
        }
        boolean drawLine = d.smooth() || d.lines();

        if (d.drawBand()) {
            // The box is the confidence band; a line is drawn through it below.
            var band = d.bandColor() != null
                    ? new Color(d.bandColor().getRed(), d.bandColor().getGreen(), d.bandColor().getBlue(), 0.15)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.12);
            gc.setFill(band);
            gc.fillRect(x, y, keyWidth, keyWidth);
            gc.setStroke(style.frameColor());
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, keyWidth, keyWidth);
        } else if (!drawLine && d.points()) {
            // A point-only key: a subtle backdrop for the shape drawn on top.
            gc.setStroke(style.frameColor());
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, keyWidth, keyWidth);
        } else if (!drawLine) {
            // A plain fill swatch for an unmapped-line category.
            gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.85));
            gc.fillRect(x, y, keyWidth, keyWidth);
            gc.setStroke(color.darker());
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, keyWidth, keyWidth);
        }

        if (drawLine) {
            gc.setStroke(color);
            gc.setLineWidth(1.8);
            double[] dashes = key.dashArray();
            gc.setLineDashes(dashes);
            double lineY = y + keyWidth / 2.0;
            gc.strokeLine(x + 1, lineY, x + keyWidth - 1, lineY);
            gc.setLineDashes(null);
        }

        if (d.points()) {
            gc.setFill(color);
            gc.setStroke(color.darker());
            // A size mapping scales the glyph; the row still clips it, so the
            // largest keys stay within the legend's rhythm.
            double radius = key.radius() != null ? Math.min(key.radius(), keyWidth / 2.0) : 2.5;
            double cx = x + keyWidth / 2.0;
            double cy = y + keyWidth / 2.0;
            var shape = key.shape() == null ? PointShape.CIRCLE : key.shape();
            gc.setLineWidth(shape == PointShape.CROSS ? 1.2 : 0.5);
            shape.draw(gc, cx, cy, radius);
        }

        gc.setFill(style.keyColor());
        gc.setFont(style.keyFont());
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(key.label(), x + keyWidth + keySpacingX, y + keyWidth / 2.0 + metrics().titleOffsetY());
    }

    /** The space reserved for the title row. */
    private double titleHeight() {
        return title() != null && !title().isEmpty() ? keyTitleHeight() : 0.0;
    }

    /**
     * The width of the key grid of a vertical legend, used as the floor for
     * its title wrapping so an overly long title does not inflate the strip.
     */
    private double verticalContentWidth(Data data, List<Key> keys) {
        if (data.sized()) {
            double maxCell = 0.0;
            for (var key : keys) {
                maxCell = Math.max(maxCell, sizedCellWidth(key));
            }
            return maxCell;
        }
        int cols = columnCount(keys);
        double[] columnWidths = new double[cols];
        int rows = rowsFor(keys, cols);
        for (int i = 0; i < keys.size(); i++) {
            columnWidths[columnOf(i, cols, rows)] = Math.max(columnWidths[columnOf(i, cols, rows)],
                    keyWidth + keySpacingX + labelWidth(keys.get(i).label()));
        }
        double total = 0.0;
        for (double w : columnWidths) {
            total += w;
        }
        return total;
    }

    /** The vertical space the (possibly wrapped) title rows reserve. */
    private double verticalTitleHeight(double contentWidth) {
        String t = title();
        if (t == null || t.isEmpty() || effectiveDirection() == Direction.HORIZONTAL) {
            return titleHeight();
        }
        return titleLines(contentWidth).size() * keyTitleHeight();
    }

    /**
     * Wraps the title into lines that each fit within a target width, keeping a
     * long title from stretching a vertical legend far past its narrow keys.
     * Lines never break a word unless a word alone exceeds the target.
     */
    private List<String> titleLines(double contentWidth) {
        String t = title();
        if (t == null || t.isEmpty() || effectiveDirection() == Direction.HORIZONTAL) {
            return t == null || t.isEmpty() ? List.of() : List.of(t);
        }
        double target = Math.max(contentWidth, metrics().maxTitleLineWidth());
        List<String> lines = new ArrayList<>();
        String current = "";
        int start = 0;
        for (int i = 0; i <= t.length(); i++) {
            if (i < t.length() && t.charAt(i) != ' ') {
                continue;
            }
            String word = t.substring(start, i);
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (current.isEmpty() || titleTextWidth(candidate) <= target) {
                current = candidate;
            } else {
                lines.add(current);
                current = word;
            }
            start = i + 1;
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines;
    }

    /**
     * The width a title line occupies when rendered in the guide title font.
     * The guide title font is proportional, so widths are measured for real in
     * the theme-supplied title font rather than estimated per character. When
     * no title font has been pushed (e.g. headless layout unit tests), the
     * measurement still happens in a real font so layout never degrades to a
     * character-count guess.
     */
    private double titleTextWidth(String s) {
        return TextMeasurer.width(s, resolvedTitleFont()) + metrics().titleTextPad();
    }

    /** The font wrapped guide titles are measured in, defaulting when unset. */
    private Font resolvedTitleFont() {
        var font = titleFont();
        return font != null ? font : Font.getDefault();
    }

    /** The width of a guide label, measured precisely in the label font. */
    private double labelWidth(String label) {
        return TextMeasurer.width(label, resolvedLabelFont());
    }

    /** The font guide labels are measured in, defaulting when unset. */
    private Font resolvedLabelFont() {
        var font = labelFont();
        return font != null ? font : Font.getDefault();
    }

    private record Grid(int cols, int rows) {}
}
