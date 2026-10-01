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
package org.jtaccuino.gog.coord;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Temporals;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.theme.AxisStyle;
import org.jtaccuino.gog.theme.Theme;

/**
 * A 2D Cartesian coordinate system for rendering panel backgrounds, grid lines,
 * axis lines, and tick labels.
 * <p>
 * Supports flipped coordinates (for {@code coordFlip}), date-aware axis formatting,
 * auto-rotation of overlapping x-axis labels, and delegate-to-scale label generation.
 */
public class Coord2D implements Coord {

    /** The number of breaks an axis aims for, and so the span a temporal axis sizes its granularity to. */
    private static final int DEFAULT_BREAKS = 5;

    private boolean flipped;
    private MinMax xLim, yLim;
    private ScaleTransform transX, transY;

    private double panelX, panelY, panelW, panelH;
    private Scale scaleX, scaleY;
    private Theme theme;
    private ScaleSpec scaleSpec;
    private DataExtractor<?> extractor;
    private Object df;
    private Aes aes;
    private boolean drawXLabels, drawYLabels;
    private boolean xLabelsTop, yLabelsRight;

    /** Widest y-tick label of the last background render, in pixels. */
    private double widestYLabelWidth;

    /** Creates a non-flipped 2D coordinate system. */
    public Coord2D() { this(false); }

    /** Creates a 2D coordinate system with the axes optionally swapped.
     * @param flipped whether the x and y axes are swapped */
    public Coord2D(boolean flipped) { this.flipped = flipped; }

    /** {@return a non-flipped 2D coordinate system} */
    public static Coord2D cartesian() { return new Coord2D(false); }

    /** {@return a flipped 2D coordinate system (swap x/y)} */
    public static Coord2D flip() { return new Coord2D(true); }

    /** Constrains the x-axis to the given data-range bounds.
     * @param min the lower x bound
     * @param max the upper x bound
     * @return this coordinate system for fluid chaining */
    public Coord2D xlim(double min, double max) { xLim = new MinMax(min, max); return this; }

    /** Constrains the y-axis to the given data-range bounds.
     * @param min the lower y bound
     * @param max the upper y bound
     * @return this coordinate system for fluid chaining */
    public Coord2D ylim(double min, double max) { yLim = new MinMax(min, max); return this; }

    /**
     * Overrides the x and y axis scaling with the given transforms, the
     * {@code Coords.coordTrans()} counterpart: every data unit is run through the
     * transform before mapping to pixels, so a {@link ScaleTransform#LOG10}
     * y-axis places {1, 10, 100, 1000} at equidistant pixels.
     *
     * @param xTransform the transform for the x data aesthetic, or {@code null} to keep the spec's
     * @param yTransform the transform for the y data aesthetic, or {@code null} to keep the spec's
     * @return this coordinate system for fluid chaining */
    public Coord2D trans(ScaleTransform xTransform, ScaleTransform yTransform) {
        this.transX = xTransform;
        this.transY = yTransform;
        return this;
    }

    /** {@return the transform overriding the x data aesthetic, or {@code null} when the spec's applies} */
    public ScaleTransform transX() { return transX; }

    /** {@return the transform overriding the y data aesthetic, or {@code null} when the spec's applies} */
    public ScaleTransform transY() { return transY; }

    @Override public boolean isFlipped() { return flipped; }
    @Override public MinMax adjustXBounds(MinMax b) { return xLim != null ? xLim : b; }
    @Override public MinMax adjustYBounds(MinMax b) { return yLim != null ? yLim : b; }

    /**
     * Sets the pixel bounds of the panel area on the canvas.
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    public void setPanelBounds(double x, double y, double w, double h) {
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;
    }

    /**
     * Prepares the coordinate system with scale, theme, extractor, and data references
     * for rendering the current facet.
     * @param sx the x-axis scale
     * @param sy the y-axis scale
     * @param t the theme
     * @param spec the scale specification
     * @param ext the data extractor
     * @param df the data frame
     * @param aes the aesthetic mappings
     * @param drawX whether x-axis labels are drawn
     * @param drawY whether y-axis labels are drawn
     * @param xLabelsTop whether x tick labels sit above the panel (top) instead of below (bottom)
     * @param yLabelsRight whether y tick labels sit to the right of the panel instead of the left
     */
    public void prepare(Scale sx, Scale sy, Theme t, ScaleSpec spec,
                        DataExtractor<?> ext, Object df, Aes aes,
                        boolean drawX, boolean drawY, boolean xLabelsTop, boolean yLabelsRight) {
        scaleX = sx;
        scaleY = sy;
        theme = t;
        scaleSpec = spec;
        extractor = ext;
        this.df = df;
        this.aes = aes;
        drawXLabels = drawX;
        drawYLabels = drawY;
        this.xLabelsTop = xLabelsTop;
        this.yLabelsRight = yLabelsRight;
    }

    /**
     * Reports whether the given data column is date-typed, bridging the raw
     * {@link DataExtractor} SPI retained for the loosely-typed data frame.
     * @param extractor the data extractor
     * @param df the data frame
     * @param column the column name
     * @return {@code true} if the column is date-typed
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static DataExtractor.ColumnType columnTypeOf(DataExtractor<?> extractor, Object df, String column) {
        if (column == null) return DataExtractor.ColumnType.TEXT;
        return ((DataExtractor) extractor).columnType(df, column);
    }

    /** Renders the panel background fill, grid lines, axis lines, and tick labels.
     * @param gc the surface to draw onto */
    public void renderBackground(DrawSurface gc) {
        double minXPix = scaleX.minPixel();
        double maxXPix = scaleX.maxPixel();
        double minYPix = scaleY.minPixel();
        double maxYPix = scaleY.maxPixel();
        widestYLabelWidth = 0.0;

        var xType = columnTypeOf(extractor, df, AesValue.rawColumn(aes.xValue()));
        var yType = columnTypeOf(extractor, df, AesValue.rawColumn(aes.yValue()));

        if (flipped) {
            var tmp = xType;
            xType = yType;
            yType = tmp;
        }

        List<Double> xTicks;
        if (scaleSpec.hasXBreaks()) {
            xTicks = scaleSpec.getXBreaks();
        } else {
            xTicks = generateTemporalTicks(scaleX, xType, 5);
        }

        List<Double> yTicks;
        if (scaleSpec.hasYBreaks()) {
            yTicks = scaleSpec.getYBreaks();
        } else {
            yTicks = generateTemporalTicks(scaleY, yType, 5);
        }

        // --- Panel background fill (masks grid lines outside panel) ---
        gc.setFill(theme.plotBackground());
        gc.fillRect(panelX, panelY, panelW, panelH);

        // --- Grid lines inside panel ---
        gc.setStroke(theme.gridLineColor());
        gc.setLineWidth(theme.gridLineWidth());
        if (theme.showXGrid()) {
            for (var tick : xTicks) {
                double px = scaleX.toPixel(tick);
                if (px >= panelX && px <= panelX + panelW)
                    gc.strokeLine(px, panelY, px, panelY + panelH);
            }
        }
        if (theme.showYGrid()) {
            for (var tick : yTicks) {
                double py = scaleY.toPixel(tick);
                if (py >= panelY && py <= panelY + panelH && py != panelY && py != panelY + panelH)
                    gc.strokeLine(panelX, py, panelX + panelW, py);
            }
        }

        // --- Minor grid lines between the major grid lines ---
        if (theme.showMinorGrid()) {
            gc.setStroke(theme.minorGridLineColor());
            gc.setLineWidth(theme.minorGridLineWidth());
            drawMinorGrid(gc, scaleX, xTicks, yTicks, scaleY);
        }

        // --- Panel border (theme_bw style), drawn before the axis spines ---
        if (theme.panelBorderColor() != null && theme.panelBorderWidth() > 0) {
            gc.setStroke(theme.panelBorderColor());
            gc.setLineWidth(theme.panelBorderWidth());
            gc.strokeRect(panelX, panelY, panelW, panelH);
        }

        // --- Axis lines ---
        var axisStyle = AxisStyle.from(theme);
        gc.setStroke(axisStyle.axisLineColor());
        gc.setLineWidth(axisStyle.axisLineWidth());
        // X spine on the bottom (default) or top (switched); Y spine on the
        // left (default) or right (switched). The other two edges stay bare.
        double xAxisPix = xLabelsTop ? maxYPix : minYPix;
        double yAxisPix = yLabelsRight ? maxXPix : minXPix;
        gc.strokeLine(minXPix, xAxisPix, maxXPix, xAxisPix);
        gc.strokeLine(yAxisPix, maxYPix, yAxisPix, minYPix);

        // --- X-axis labels (bottom by default, top when switched) ---
        gc.setFill(axisStyle.tickLabelColor());
        var labelFont = axisStyle.tickLabelFont();
        gc.setFont(labelFont);

        var availableAxisWidth = maxXPix - minXPix;
        var totalLabelsWidthNeeded = 0.0;
        var helperText = new Text();
        helperText.setFont(labelFont);
        for (var i = 0; i < xTicks.size(); i++) {
            helperText.setText(xLabel(xTicks.get(i), i, xType));
            totalLabelsWidthNeeded += helperText.getLayoutBounds().getWidth() + 12.0;
        }

        var autoAngle = theme.xLabelRotationAngle();
        if (autoAngle == 0.0 && totalLabelsWidthNeeded > availableAxisWidth)
            autoAngle = -45.0;

        var drawXTicks = theme.showMajorTicks() && theme.showXAxisTicks();
        for (var i = 0; i < xTicks.size(); i++) {
            var tick = xTicks.get(i);
            var px = scaleX.toPixel(tick);
            if (drawXTicks) {
                gc.setStroke(axisStyle.tickColor());
                gc.setLineWidth(axisStyle.axisLineWidth());
                gc.strokeLine(px, xAxisPix, px, xAxisPix + (xLabelsTop ? -5 : 5));
            }
            if (drawXLabels) {
                String label = xLabel(tick, i, xType);
                gc.save();
                if (autoAngle == 0.0) {
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(label, px, xAxisPix + (xLabelsTop ? -14 : 14));
                } else {
                    // Rotated labels hang from the axis; flip the anchor for the top edge.
                    gc.translate(px - 4, xAxisPix + (xLabelsTop ? -14 : 14));
                    gc.rotate(autoAngle);
                    gc.setTextAlign(TextAlignment.RIGHT);
                    gc.fillText(label, 0, 0);
                }
                gc.restore();
            }
        }

        // --- Y-axis labels (left by default, right when switched) ---
        var yHelper = new Text();
        yHelper.setFont(labelFont);
        var drawYTicks = theme.showMajorTicks() && theme.showYAxisTicks();
        for (var i = 0; i < yTicks.size(); i++) {
            var tick = yTicks.get(i);
            var py = scaleY.toPixel(tick);
            if (drawYTicks) {
                gc.setStroke(axisStyle.tickColor());
                gc.setLineWidth(axisStyle.axisLineWidth());
                gc.strokeLine(yAxisPix, py, yAxisPix + (yLabelsRight ? 5 : -5), py);
            }
            if (drawYLabels) {
                String label = yLabel(tick, i, yType);
                // Right-hand labels read left-to-right away from the spine.
                gc.setTextAlign(yLabelsRight ? TextAlignment.LEFT : TextAlignment.RIGHT);
                gc.fillText(label, yAxisPix + (yLabelsRight ? 10 : -10), py + 4);
                yHelper.setText(label);
                widestYLabelWidth = Math.max(widestYLabelWidth, yHelper.getLayoutBounds().getWidth());
            }
        }

        // --- Minor axis tick marks, between the major ticks ---
        if (theme.showMinorTicks()) {
            gc.setStroke(theme.minorTickColor());
            gc.setLineWidth(axisStyle.axisLineWidth());
            if (theme.showXAxisTicks() && !scaleX.isDiscrete() && !scaleX.isLog() && xTicks.size() >= 2) {
                for (var v : minorPositions(scaleX, xTicks)) {
                    double px = scaleX.toPixel(v);
                    gc.strokeLine(px, xAxisPix, px, xAxisPix + (xLabelsTop ? -theme.minorTickLength() : theme.minorTickLength()));
                }
            }
            if (theme.showYAxisTicks() && !scaleY.isDiscrete() && !scaleY.isLog() && yTicks.size() >= 2) {
                for (var v : minorPositions(scaleY, yTicks)) {
                    double py = scaleY.toPixel(v);
                    gc.strokeLine(yAxisPix, py, yAxisPix + (yLabelsRight ? theme.minorTickLength() : -theme.minorTickLength()), py);
                }
            }
            // Log-axis sub-decade ticks ({2..9}·10ⁿ), with a longer mid tick
            // at 5·10ⁿ, drawn outward from the log-axis edge.
            if (theme.showXAxisTicks() && scaleX.isLog()) {
                double shortLen = theme.minorTickLength();
                double midLen = shortLen * 2.0;
                for (var px : positionLogTicks(scaleX)) {
                    double x = scaleX.toPixel(px);
                    double len = isHalfDecade(px) ? midLen : shortLen;
                    gc.strokeLine(x, xAxisPix, x, xAxisPix + (xLabelsTop ? -len : len));
                }
            }
            if (theme.showYAxisTicks() && scaleY.isLog()) {
                double shortLen = theme.minorTickLength();
                double midLen = shortLen * 2.0;
                for (var py : positionLogTicks(scaleY)) {
                    double y = scaleY.toPixel(py);
                    double len = isHalfDecade(py) ? midLen : shortLen;
                    gc.strokeLine(yAxisPix, y, yAxisPix + (yLabelsRight ? len : -len), y);
                }
            }
        }
    }

    /** {@return the widest y-axis tick label rendered by the last {@link #renderBackground(DrawSurface)} call, in pixels} */
    public double widestYLabelWidth() {
        return widestYLabelWidth;
    }

    /**
     * Draws the linear minor grid: each interval between consecutive major
     * ticks is divided into four equal sub-intervals and the interior lines
     * are drawn, along an axis only when its major grid is shown. Log axes
     * skip the linear minor grid — their sub-decades are drawn as tick marks.
     */
    private void drawMinorGrid(DrawSurface gc, Scale sx, List<Double> xTicks,
            List<Double> yTicks, Scale sy) {
        if (theme.showXGrid() && !sx.isLog() && xTicks.size() >= 2) {
            drawLinearMinor(gc, sx, xTicks, panelY, panelY + panelH, true);
        }
        if (theme.showYGrid() && !sy.isLog() && yTicks.size() >= 2) {
            drawLinearMinor(gc, sy, yTicks, panelX, panelX + panelW, false);
        }
    }

    /**
     * The data positions of the minor grid lines along a linear axis,
     * following the {@code prettyBreaks}: the major interval is divided into
     * four equal sub-intervals and the quarter-step is extended continuously
     * across the whole axis range, so partial intervals between the axis ends
     * and the first/last major tick still get minor lines. Major positions
     * themselves (every fourth quarter-step) are excluded. Log scales skip
     * this — their sub-decades are drawn as log tick marks.
     */
    private List<Double> minorPositions(Scale scale, List<Double> ticks) {
        List<Double> minor = new ArrayList<>();
        if (ticks.size() < 2) {
            return minor;
        }
        double quarter = (ticks.get(1) - ticks.get(0)) / 4.0;
        double lo = scale.minData();
        double hi = scale.maxData();
        for (long k = (long) Math.floor(lo / quarter); k <= (long) Math.ceil(hi / quarter); k++) {
            double v = k * quarter;
            if (v >= lo && v <= hi && k % 4 != 0) {
                minor.add(v);
            }
        }
        return minor;
    }

    private void drawLinearMinor(DrawSurface gc, Scale scale, List<Double> ticks,
            double lineStart, double lineEnd, boolean vertical) {
        for (var v : minorPositions(scale, ticks)) {
            double p = scale.toPixel(v);
            if (vertical) {
                gc.strokeLine(p, lineStart, p, lineEnd);
            } else {
                gc.strokeLine(lineStart, p, lineEnd, p);
            }
        }
    }

    /**
     * The sub-decade data positions ({2, 3, …, 9}·10ⁿ) that fall inside the
     * scale's current data range.
     */
    private static List<Double> positionLogTicks(Scale scale) {
        List<Double> ticks = new ArrayList<>();
        double lo = scale.minData();
        double hi = scale.maxData();
        if (!(lo > 0 && hi > 0)) {
            return ticks;
        }
        double baseLog = Math.log(10);
        int first = (int) Math.floor(Math.log(lo) / baseLog);
        int last = (int) Math.ceil(Math.log(hi) / baseLog);
        for (int decade = first; decade <= last; decade++) {
            double power = Math.pow(10, decade);
            for (int m = 2; m < 10; m++) {
                double v = power * m;
                if (v > lo && v < hi) {
                    ticks.add(v);
                }
            }
        }
        return ticks;
    }

    /** Whether the value is a 5·10ⁿ half-decade position. */
    private static boolean isHalfDecade(double value) {
        var mantissa = value / Math.pow(10, Math.floor(Math.log10(value)));
        return Math.abs(mantissa - 5.0) < 1e-9;
    }

    /**
     * Resolves the x-tick label: an explicit label supplied alongside custom
     * breaks wins, then the scale's own labels (discrete scales), then a label
     * the column type dictates — a year, a date-time, or a plain number.
     */
    private String xLabel(double tick, int breakIndex, DataExtractor.ColumnType type) {
        if (scaleSpec != null && scaleSpec.hasXBreaks()) {
            var explicit = scaleSpec.xLabelAt(breakIndex);
            if (explicit != null) return explicit;
        }
        if (scaleX != null && scaleX.getTickLabels() != null) {
            return scaleX.getLabel(tick);
        }
        return temporalLabel(tick, type, scaleX);
    }

    /**
     * Resolves the y-tick label, following the same precedence as
     * {@link #xLabel(double, int, DataExtractor.ColumnType)}.
     */
    private String yLabel(double tick, int breakIndex, DataExtractor.ColumnType type) {
        if (scaleSpec != null && scaleSpec.hasYBreaks()) {
            var explicit = scaleSpec.yLabelAt(breakIndex);
            if (explicit != null) return explicit;
        }
        if (scaleY != null && scaleY.getTickLabels() != null) {
            return scaleY.getLabel(tick);
        }
        return temporalLabel(tick, type, scaleY);
    }

    /**
     * Generates tick positions for an axis whose column type decides the ladder:
     * a date axis breaks by year, a timestamp axis by whatever
     * calendar granularity the span affords, and anything else falls back to
     * the scale's own "nice" breaks.
     *
     * @param scale  the axis scale
     * @param type   the column type mapped to the axis
     * @param target the wanted number of breaks
     * @return the break positions
     */
    private static List<Double> generateTemporalTicks(Scale scale, DataExtractor.ColumnType type, int target) {
        if (type == DataExtractor.ColumnType.DATE) {
            return Temporals.dateTicks(scale.minData(), scale.maxData());
        }
        if (type == DataExtractor.ColumnType.TIMESTAMP) {
            return Temporals.timestampTicks(scale.minData(), scale.maxData(), target);
        }
        return scale.calculateTicks(target, null);
    }

    /**
     * Resolves the label of a break for an axis whose column type decides the
     * format: a date break reads as a year, a timestamp break at the granularity
     * its span affords, and anything else as a plain number.
     *
     * @param tick  the break position in data units
     * @param type  the column type mapped to the axis
     * @param scale the axis scale, consulted only for a timestamp axis
     * @return the formatted label
     */
    private static String temporalLabel(double tick, DataExtractor.ColumnType type, Scale scale) {
        if (type == DataExtractor.ColumnType.DATE) {
            return Temporals.dateLabel(tick);
        }
        if (type == DataExtractor.ColumnType.TIMESTAMP) {
            var granularity = Temporals.granularityOf(scale.minData(), scale.maxData(), DEFAULT_BREAKS);
            return Temporals.timestampLabel(tick, granularity);
        }
        return Scale.formatTick(tick);
    }
}
