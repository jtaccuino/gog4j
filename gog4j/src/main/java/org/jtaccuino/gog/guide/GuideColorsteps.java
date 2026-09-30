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
import javafx.scene.text.Font;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * The binned continuous guide: a colour bar divided into discrete steps, each
 * filled with one step of the ramp — the guide behind {@code scale_*_binned}
 * and the {@code Guides.guideColorsteps()}. Unlike the smooth-coloured
 * {@link GuideColorbar}, binned steps render as a sequence of solid swatches.
 * <p>
 * Instances are immutable; each {@code with}-style setter returns a new guide
 * with that single property changed.
 */
public final class GuideColorsteps extends Guide<GuideColorsteps.Data> {

    /**
     * The per-plot data a colorsteps guide renders against.
     *
     * @param columnName the column mapped to the colour aesthetic
     * @param domain     the value range the bar spans
     * @param breaks     the tick positions, or {@code null} for none
     * @param labels     the tick labels, or {@code null} to format from the breaks
     * @param scale      the colour scale used to sample the step colours
     * @param steps      the number of discrete colour steps
     */
    public record Data(String columnName, MinMax domain, List<Double> breaks,
                       List<String> labels, ContinuousColorScale scale, int steps) implements GuideData {}

    private final double barWidth;
    private final double barHeight;
    private final int nbin;
    private final boolean ticks;
    private final Color ticksColor;
    private final double ticksLineWidth;
    private final Color frameColor;
    private final double frameLineWidth;
    private final GuideColorbar.LabelPosition labelPosition;
    private final boolean drawUlim;
    private final boolean drawLlim;

    /**
     * Constructs a colorsteps guide with the given properties.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param ticks           whether ticks are drawn at the step boundaries
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit, or {@code null} to auto-select
     * @param drawUlim        whether to draw the highest break tick
     * @param drawLlim        whether to draw the lowest break tick
     */
    public GuideColorsteps(String title, Guide.Direction direction, boolean reverse, int order,
                  double barWidth, double barHeight, int nbin, boolean ticks,
                  Color ticksColor, double ticksLineWidth, Color frameColor,
                  double frameLineWidth, GuideColorbar.LabelPosition labelPosition,
                  boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, null, barWidth, barHeight, nbin, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs a colorsteps guide with the given properties and an explicit
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
     * @param ticks           whether ticks are drawn at the step boundaries
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideColorsteps(String title, Guide.Direction direction, boolean reverse, int order, GuidePosition position,
                  double barWidth, double barHeight, int nbin, boolean ticks,
                  Color ticksColor, double ticksLineWidth, Color frameColor,
                  double frameLineWidth, GuideColorbar.LabelPosition labelPosition,
                  boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, null, barWidth, barHeight, nbin, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs a colorsteps guide with the given properties and per-guide
     * theme overrides.
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
     * @param ticks           whether ticks are drawn at the step boundaries
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideColorsteps(String title, Guide.Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, double barWidth, double barHeight, int nbin, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, GuideColorbar.LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        super(title, direction, reverse, order, position, guideTheme);
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        this.nbin = Math.max(1, nbin);
        this.ticks = ticks;
        this.ticksColor = ticksColor;
        this.ticksLineWidth = ticksLineWidth;
        this.frameColor = frameColor;
        this.frameLineWidth = frameLineWidth;
        this.labelPosition = labelPosition;
        this.drawUlim = drawUlim;
        this.drawLlim = drawLlim;
    }

    /** {@return the thickness of the color bar in pixels} */
    public double barWidth() { return barWidth; }

    /** {@return the height of a vertical color bar in pixels} */
    public double barHeight() { return barHeight; }

    /** {@return the number of gradient segments used to draw the bar} */
    public int nbin() { return nbin; }

    /** {@return whether tick marks are drawn at the step boundaries} */
    public boolean ticks() { return ticks; }

    /** {@return the color of the tick marks, or {@code null} for the theme's} */
    public Color ticksColor() { return ticksColor; }

    /** {@return the width of the tick marks} */
    public double ticksLineWidth() { return ticksLineWidth; }

    /** {@return the color of the bar's frame, or {@code null} for the theme's} */
    public Color frameColor() { return frameColor; }

    /** {@return the width of the bar's frame} */
    public double frameLineWidth() { return frameLineWidth; }

    /** {@return whether the highest break tick is drawn} */
    public boolean drawUlim() { return drawUlim; }

    /** {@return whether the lowest break tick is drawn} */
    public boolean drawLlim() { return drawLlim; }

    /** {@return the resolved label position, right of a vertical bar or top of a horizontal one} */
    public GuideColorbar.LabelPosition labelPosition() {
        return labelPosition != null ? labelPosition
                : (effectiveDirection() == Direction.HORIZONTAL ? GuideColorbar.LabelPosition.TOP : GuideColorbar.LabelPosition.RIGHT);
    }

    /** {@return the resolved label font, defaulting when unset} */
    private Font resolvedLabelFont() {
        var font = labelFont();
        return font != null ? font : Font.getDefault();
    }

    @Override
    public GuideColorsteps title(String title) {
        return copyWith(title, direction(), reverse(), order(), position(), guideTheme());
    }

    @Override
    public GuideColorsteps position(GuidePosition position) {
        return copyWith(title(), direction(), reverse(), order(), position, guideTheme());
    }

    private GuideColorsteps copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme) {
        return new GuideColorsteps(title, direction, reverse, order, position, guideTheme, barWidth, barHeight, nbin,
                ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition, drawUlim, drawLlim);
    }

    @Override
    public double measure(Data data, double width) {
        return BarGuideHelper.measureHeight(title(), barExtentY(),
                effectiveDirection() == Direction.HORIZONTAL,
                title() != null && !title().isEmpty(), metrics());
    }

    /** The bar's extent along the Y axis. */
    private double barExtentY() {
        return effectiveDirection() == Direction.HORIZONTAL ? barWidth : barHeight;
    }

    @Override
    public double preferredWidth(Data data) {
        var ticks = BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(), data.labels(),
                drawLlim(), drawUlim(), true);
        double widestLabel = ticks.stream()
                .map(BarGuideHelper.Tick::label)
                .filter(l -> l != null)
                .mapToDouble(l -> TextMeasurer.width(l, resolvedLabelFont()))
                .max()
                .orElse(0.0);
        return BarGuideHelper.preferredWidth(effectiveDirection() == Direction.HORIZONTAL, barWidth, title(), labelFont(),
                ticks, widestLabel, metrics());
    }

    @Override
    public void render(DrawSurface gc, Data data, double x, double y, double width, GuideStyle style) {
        double min = data.domain().min();
        double max = data.domain().max();
        double ly = y;
        var horizontal = effectiveDirection() == Direction.HORIZONTAL;
        double contentX = x;

        double sideTitle = BarGuideHelper.renderTitle(gc, title(), style, horizontal, contentX, ly, barWidth, metrics());
        if (horizontal) {
            contentX += sideTitle;
        } else {
            ly += sideTitle;
        }

        var steps = Math.max(1, data.steps());
        var colors = data.scale().sampleSteps(steps);

        double barLen;
        if (horizontal) {
            barLen = width - sideTitle;
        } else {
            barLen = barHeight;
        }
        double stepLen = barLen / steps;

        for (int i = 0; i < steps; i++) {
            gc.setFill(colors.get(i));
            if (horizontal) {
                gc.fillRect(contentX + i * stepLen, ly, stepLen, barWidth);
            } else {
                gc.fillRect(x, ly + i * stepLen, barWidth, stepLen);
            }
        }

        gc.setStroke(frameColor != null ? frameColor : style.frameColor());
        gc.setLineWidth(frameLineWidth);
        if (horizontal) {
            gc.strokeRect(contentX, ly, barLen, barWidth);
        } else {
            gc.strokeRect(x, ly, barWidth, barHeight);
        }

        if (horizontal) {
            BarGuideHelper.renderTicksAndLabels(gc,
                    BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, true),
                    min, max, reverse(), ticks, ticksColor, ticksLineWidth, true,
                    contentX, ly, barLen, barWidth, style, metrics());
        } else {
            BarGuideHelper.renderTicksAndLabels(gc,
                    BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, true),
                    min, max, reverse(), ticks, ticksColor, ticksLineWidth, false,
                    x, ly, barHeight, barWidth, style, metrics());
        }
    }
}
