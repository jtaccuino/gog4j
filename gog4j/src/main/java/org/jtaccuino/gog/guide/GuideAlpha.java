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
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.theme.Anchor;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * A continuous guide for the alpha (transparency) aesthetic: a gradient bar
 * that ramps from fully transparent to fully opaque in a base colour, with
 * tick marks and labels at break positions. The base colour is the resolved
 * fill colour of the data; when colour is also mapped, one bar per colour
 * class is rendered, each tinted in that class's colour.
 * <p>
 * Instances are immutable; each {@code with}-style setter returns a new guide
 * with that single property changed. Create them via
 * {@link org.jtaccuino.gog.Guides#guideAlpha()}.
 */
public final class GuideAlpha extends Guide<GuideAlpha.Data> {

    /**
     * Where the tick labels sit relative to the bar.
     */
    public enum LabelPosition {
        /** Labels sit above the bar. */
        TOP,
        /** Labels sit to the right of the bar. */
        RIGHT,
        /** Labels sit below the bar. */
        BOTTOM,
        /** Labels sit to the left of the bar. */
        LEFT
    }

    /**
     * One coloured alpha bar entry.
     *
     * @param baseColor the base colour the alpha gradient is tinted with
     * @param domain    the value range the bar maps onto
     */
    public record AlphaEntry(Color baseColor, MinMax domain) {}

    /**
     * The per-plot data an alpha guide renders against.
     *
     * @param columnName the column the alpha encoding maps
     * @param entries    the per-colour alpha entries to draw
     * @param breaks     the tick break positions
     * @param labels     the tick labels
     */
    public record Data(String columnName, List<AlphaEntry> entries,
                       List<Double> breaks, List<String> labels) implements GuideData {
    }

    private final double barWidth;
    private final double barHeight;
    private final int nbin;
    private final double barStep;
    private final boolean ticks;
    private final Color ticksColor;
    private final double ticksLineWidth;
    private final Color frameColor;
    private final double frameLineWidth;
    private final LabelPosition labelPosition;
    private final boolean drawUlim;
    private final boolean drawLlim;

    /**
     * Constructs an alpha guide with the given properties, using the theme's
     * guide position.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param barStep         the minimum width per bar in pixels
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideAlpha(String title, Direction direction, boolean reverse, int order,
            double barWidth, double barHeight, int nbin, double barStep, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, null, barWidth, barHeight, nbin, barStep, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs an alpha guide with the given properties and an explicit
     * strip-side position.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param position        the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param barStep         the minimum width per bar in pixels
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideAlpha(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            double barWidth, double barHeight, int nbin, double barStep, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, null, barWidth, barHeight, nbin, barStep, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs an alpha guide with the given properties and per-guide theme
     * overrides.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param position        the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param guideTheme      per-guide style overrides over the theme, or {@code null} to inherit
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param barStep         the minimum width per bar in pixels
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideAlpha(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, double barWidth, double barHeight, int nbin, double barStep, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, guideTheme, null,
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor,
                frameLineWidth, labelPosition, drawUlim, drawLlim);
    }

    /**
     * Constructs an alpha guide with the given properties, per-guide theme
     * overrides, and inside-panel anchor.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param position        the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param guideTheme      per-guide style overrides over the theme, or {@code null} to inherit
     * @param insideAnchor    the panel-relative anchor for INSIDE placement, or {@code null} to inherit
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param barStep         the minimum width per bar in pixels
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideAlpha(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor,
            double barWidth, double barHeight, int nbin, double barStep, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        super(title, direction, reverse, order, position, guideTheme, insideAnchor);
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        this.nbin = nbin;
        this.barStep = barStep;
        this.ticks = ticks;
        this.ticksColor = ticksColor;
        this.ticksLineWidth = ticksLineWidth;
        this.frameColor = frameColor;
        this.frameLineWidth = frameLineWidth;
        this.labelPosition = labelPosition;
        this.drawUlim = drawUlim;
        this.drawLlim = drawLlim;
    }

    /** {@return the bar thickness in pixels} */
    public double barWidth() { return barWidth; }

    /** {@return the bar length in pixels for a vertical bar} */
    public double barHeight() { return barHeight; }

    /** {@return the number of gradient segments used to draw the alpha bar} */
    public int nbin() { return nbin; }

    /** {@return the minimum width reserved for each bar laid out side by side} */
    public double barStep() { return barStep; }

    /** {@return whether tick marks are drawn at the breaks} */
    public boolean ticks() { return ticks; }

    /** {@return the tick colour, or {@code null} for the theme default} */
    public Color ticksColor() { return ticksColor; }

    /** {@return the tick stroke width} */
    public double ticksLineWidth() { return ticksLineWidth; }

    /** {@return the frame colour, or {@code null} for the theme default} */
    public Color frameColor() { return frameColor; }

    /** {@return the frame stroke width} */
    public double frameLineWidth() { return frameLineWidth; }

    /** {@return whether the highest break tick is drawn} */
    public boolean drawUlim() { return drawUlim; }

    /** {@return whether the lowest break tick is drawn} */
    public boolean drawLlim() { return drawLlim; }

    /** {@return the resolved label position} */
    public LabelPosition labelPosition() {
        return labelPosition != null ? labelPosition
                : (effectiveDirection() == Direction.HORIZONTAL ? LabelPosition.TOP : LabelPosition.RIGHT);
    }

    /** {@return a copy of this guide with the title set} */
    @Override
    public GuideAlpha title(String title) {
        return copyWith(title, direction(), reverse(), order());
    }

    /**
     * {@return a copy of this guide with the bar orientation set}
     *
     * @param direction the bar orientation
     */
    public GuideAlpha direction(Direction direction) {
        return copyWith(title(), direction, reverse(), order());
    }

    /**
     * {@return a copy of this guide with the reversal flag set}
     *
     * @param reverse {@code true} to reverse the bar's value direction
     */
    public GuideAlpha reverse(boolean reverse) {
        return copyWith(title(), direction(), reverse, order());
    }

    /**
     * {@return a copy of this guide with the stacking order set}
     *
     * @param order the stacking order among multiple guides
     */
    public GuideAlpha order(int order) {
        return copyWith(title(), direction(), reverse(), order);
    }

    /**
     * {@return a copy of this guide with the strip side set}
     *
     * @param position the strip side, or {@code null} to inherit
     */
    @Override
    public GuideAlpha position(GuidePosition position) {
        return copyWith(title(), direction(), reverse(), order(), position);
    }

    /**
     * {@return a copy of this guide with per-guide style overrides}
     *
     * @param guideTheme per-guide style overrides over the theme
     */
    public GuideAlpha theme(GuideTheme guideTheme) {
        return copyWith(title(), direction(), reverse(), order(), position(), guideTheme);
    }

    /**
     * {@return a copy of this guide anchored inside the panel}
     *
     * @param x relative horizontal anchor in [0, 1] of the panel
     * @param y relative vertical anchor in [0, 1] of the panel
     */
    public GuideAlpha inside(double x, double y) {
        return copyWith(title(), direction(), reverse(), order(), position(), guideTheme(),
                new Anchor(x, y));
    }

    /**
     * {@return {@code true}; an alpha guide divides the width it is given
     * between its colour-class bars, so it stretches to fill a top/bottom
     * strip}
     */
    @Override
    public boolean stretchesToFillStrip() {
        return true;
    }

    /**
     * {@return a copy of this guide with the bar thickness set}
     *
     * @param barWidth the bar thickness in pixels
     */
    public GuideAlpha barWidth(double barWidth) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the bar length set}
     *
     * @param barHeight the bar length in pixels for a vertical bar
     */
    public GuideAlpha barHeight(double barHeight) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the tick visibility set}
     *
     * @param ticks {@code true} to draw tick marks
     */
    public GuideAlpha ticks(boolean ticks) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the tick colour set}
     *
     * @param ticksColor the tick colour
     */
    public GuideAlpha ticksColor(Color ticksColor) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the tick stroke width set}
     *
     * @param ticksLineWidth the tick stroke width
     */
    public GuideAlpha ticksLineWidth(double ticksLineWidth) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the frame colour set}
     *
     * @param frameColor the frame colour
     */
    public GuideAlpha frameColor(Color frameColor) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the frame stroke width set}
     *
     * @param frameLineWidth the frame stroke width
     */
    public GuideAlpha frameLineWidth(double frameLineWidth) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the label position set}
     *
     * @param labelPosition where the tick labels sit
     */
    public GuideAlpha labelPosition(LabelPosition labelPosition) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the highest break tick visibility set}
     *
     * @param drawUlim {@code true} to draw the highest break tick
     */
    public GuideAlpha drawUlim(boolean drawUlim) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the lowest break tick visibility set}
     *
     * @param drawLlim {@code true} to draw the lowest break tick
     */
    public GuideAlpha drawLlim(boolean drawLlim) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the alpha-gradient resolution set}
     *
     * @param nbin the number of gradient segments used to draw the bar
     */
    public GuideAlpha nbin(int nbin) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    /**
     * {@return a copy of this guide with the minimum side-by-side bar width set}
     *
     * @param barStep the minimum width per bar in pixels
     */
    public GuideAlpha barStep(double barStep) {
        return new GuideAlpha(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    private GuideAlpha copyWith(String title, Direction direction, boolean reverse, int order) {
        return copyWith(title, direction, reverse, order, position(), guideTheme());
    }

    private GuideAlpha copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position) {
        return copyWith(title, direction, reverse, order, position, guideTheme());
    }

    private GuideAlpha copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme) {
        return copyWith(title, direction, reverse, order, position, guideTheme, insideAnchor());
    }

    private GuideAlpha copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme, Anchor insideAnchor) {
        return new GuideAlpha(title, direction, reverse, order, position, guideTheme, insideAnchor,
                barWidth, barHeight, nbin, barStep, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim);
    }

    private double barExtentY() {
        return effectiveDirection() == Direction.HORIZONTAL ? barWidth : barHeight;
    }

    @Override
    public double measure(Data data, double width) {
        boolean hasTitle = title() != null && !title().isEmpty();
        boolean horizontal = effectiveDirection() == Direction.HORIZONTAL;
        int entryCount = data.entries().size();
        if (entryCount <= 1 || (horizontal && entryCount > 1)) {
            return BarGuideHelper.measureHeight(title(), barExtentY(), horizontal, hasTitle, metrics());
        }
        double height = 0.0;
        if (hasTitle && !horizontal) {
            height += metrics().barTitleHeight();
        }
        for (int i = 0; i < entryCount; i++) {
            height += barExtentY() + barTickSpace();
            if (i < entryCount - 1) {
                height += barRowGap();
            }
        }
        return height;
    }

    @Override
    public double preferredWidth(Data data) {
        boolean horizontal = effectiveDirection() == Direction.HORIZONTAL;
        int entryCount = data.entries().size();
        if (horizontal && entryCount > 1) {
            double sideTitle = BarGuideHelper.sideTitleWidth(title(), labelFont(), metrics().contentPad());
            double widestLabel = data.entries().stream()
                    .flatMap(entry -> BarGuideHelper.ticksAndLabels(entry.domain(), data.breaks(),
                            data.labels(), drawLlim(), drawUlim(), false).stream())
                    .map(BarGuideHelper.Tick::label)
                    .filter(l -> l != null)
                    .mapToDouble(l -> TextMeasurer.width(l, resolvedLabelFont()))
                    .max()
                    .orElse(0.0);
            double perBar = Math.max(barStep(), widestLabel + barLabelPad());
            return sideTitle + entryCount * perBar + metrics().barGap() * (entryCount - 1);
        }
        List<BarGuideHelper.Tick> ticks = List.of();
        if (!data.entries().isEmpty()) {
            var entry = data.entries().get(0);
            ticks = BarGuideHelper.ticksAndLabels(entry.domain(), data.breaks(),
                    data.labels(), drawLlim(), drawUlim(), false);
        }
        double widestLabel = ticks.stream()
                .map(BarGuideHelper.Tick::label)
                .filter(l -> l != null)
                .mapToDouble(l -> TextMeasurer.width(l, resolvedLabelFont()))
                .max()
                .orElse(0.0);
        return BarGuideHelper.preferredWidth(horizontal, barWidth, title(), labelFont(), ticks,
                widestLabel, metrics());
    }

    @Override
    public void render(DrawSurface gc, Data data, double x, double y, double width, GuideStyle style) {
        var entries = data.entries();
        boolean horizontal = effectiveDirection() == Direction.HORIZONTAL;
        double ly = y;
        double contentX = x;

        double sideTitle = BarGuideHelper.renderTitle(gc, title(), style, horizontal, contentX, ly, barWidth, metrics());
        if (horizontal) {
            contentX += sideTitle;
        } else {
            ly += sideTitle;
        }

        boolean sideBySide = horizontal && entries.size() > 1;
        double gaps = sideBySide ? metrics().barGap() * (entries.size() - 1) : 0.0;
        double barLen = horizontal
                ? (width - sideTitle - gaps) / (sideBySide ? entries.size() : 1.0)
                : 0.0;

        for (int e = 0; e < entries.size(); e++) {
            var entry = entries.get(e);
            double min = entry.domain().min();
            double max = entry.domain().max();
            Color base = entry.baseColor();

            double bx = horizontal ? contentX : x;
            double by = ly;

            var stops = new ArrayList<Stop>(nbin + 1);
            for (int i = 0; i <= nbin; i++) {
                double t = (double) i / nbin;
                stops.add(new Stop(t, base.deriveColor(0, 1, 1, t)));
            }
            var gradient = new LinearGradient(
                    0.0, horizontal ? 0.0 : 1.0,
                    horizontal ? 1.0 : 0.0, 0.0,
                    true, CycleMethod.NO_CYCLE, stops);
            gc.setFill(gradient);

            if (horizontal) {
                gc.fillRect(bx, by, barLen, barWidth);
                gc.setStroke(frameColor != null ? frameColor : style.frameColor());
                gc.setLineWidth(frameLineWidth);
                gc.strokeRect(bx, by, barLen, barWidth);
                BarGuideHelper.renderTicksAndLabels(gc,
                        BarGuideHelper.ticksAndLabels(entry.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, false),
                        min, max, reverse(), ticks, ticksColor, ticksLineWidth, true,
                        bx, by, barLen, barWidth, style, metrics());
            } else {
                gc.fillRect(bx, by, barWidth, barHeight);
                gc.setStroke(frameColor != null ? frameColor : style.frameColor());
                gc.setLineWidth(frameLineWidth);
                gc.strokeRect(bx, by, barWidth, barHeight);
                BarGuideHelper.renderTicksAndLabels(gc,
                        BarGuideHelper.ticksAndLabels(entry.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, false),
                        min, max, reverse(), ticks, ticksColor, ticksLineWidth, false,
                        bx, by, barHeight, barWidth, style, metrics());
            }

            if (horizontal) {
                contentX += sideBySide ? barLen + metrics().barGap() : 0;
                if (!sideBySide) {
                    ly += barWidth + (e < entries.size() - 1 ? barTickSpaceHorizontal() + barRowGap() : 0);
                }
            } else {
                ly += barHeight + barTickSpace() + (e < entries.size() - 1 ? barRowGap() : 0);
            }
        }
    }

    /** The font bar tick labels are measured in, defaulting when unset. */
    private Font resolvedLabelFont() {
        var font = labelFont();
        return font != null ? font : Font.getDefault();
    }

    /** The gap between stacked bar rows, from the theme metrics. */
    private double barRowGap() {
        return metrics().barRowGap();
    }

    /** The tick/label space cleared under a vertical bar, from the theme metrics. */
    private double barTickSpace() {
        return metrics().barTickSpace();
    }

    /** The tick/label space cleared beside a horizontal bar, from the theme metrics. */
    private double barTickSpaceHorizontal() {
        return metrics().barTickSpaceHorizontal();
    }

    /** The extra horizontal padding beside bar tick labels, from the theme metrics. */
    private double barLabelPad() {
        return metrics().barLabelPad();
    }
}
