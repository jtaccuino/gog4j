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
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.theme.Anchor;
import org.jtaccuino.gog.theme.Colors;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * The continuous guide: a gradient color bar with tick marks and labels at
 * break positions. Mirrors the default's {@code Guides.guideColorbar()}; the ticks are
 * drawn by default, with the two extreme break ticks controlled by
 * {@code draw.ulim}/{@code draw.llim}.
 * <p>
 * Instances are immutable; each {@code with}-style setter returns a new guide
 * with that single property changed. Create them via {@link org.jtaccuino.gog.Guides#guideColorbar()}.
 */
public final class GuideColorbar extends Guide<GuideColorbar.Data> {

    /** Where the tick labels sit relative to the bar. */
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
     * The per-plot data a colorbar guide renders against.
     *
     * @param columnName the column mapped to the colour aesthetic
     * @param domain     the value range the bar spans
     * @param breaks     the tick positions, or {@code null} for none
     * @param labels     the tick labels, or {@code null} to format from the breaks
     * @param scale      the color scale used to build the gradient
     */
    public record Data(String columnName, MinMax domain, List<Double> breaks,
                       List<String> labels, ContinuousColorScale scale) implements GuideData {}

    private final double barWidth;
    private final double barHeight;
    private final int nbin;
    private final boolean ticks;
    private final Color ticksColor;
    private final double ticksLineWidth;
    private final Color frameColor;
    private final double frameLineWidth;
    private final LabelPosition labelPosition;
    private final boolean drawUlim;
    private final boolean drawLlim;
    private final GuideShading shade;

    /**
     * Constructs a colorbar guide, inheriting the theme's guide position.
     *
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param ticks           whether to draw tick marks at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit, or {@code null} to auto-select
     * @param drawUlim        whether to draw the highest break tick
     * @param drawLlim        whether to draw the lowest break tick
     */
    public GuideColorbar(String title, Direction direction, boolean reverse, int order,
                  double barWidth, double barHeight, int nbin, boolean ticks,
                  Color ticksColor, double ticksLineWidth, Color frameColor,
                  double frameLineWidth, LabelPosition labelPosition,
                  boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, null, barWidth, barHeight, nbin, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs a colorbar guide with the given properties and an explicit
     * strip-side position.
     *
     * @param position        the strip side this guide is placed on, or {@code null} to inherit the theme's
     * @param title           the guide title, or {@code null} for none
     * @param direction       the bar orientation
     * @param reverse         {@code true} to reverse the bar's value direction
     * @param order           the stacking order among multiple guides
     * @param barWidth        the thickness of the color bar in pixels
     * @param barHeight       the height of a vertical color bar in pixels
     * @param nbin            the number of gradient segments used to draw the bar
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideColorbar(String title, Direction direction, boolean reverse, int order, GuidePosition position,
                  double barWidth, double barHeight, int nbin, boolean ticks,
                  Color ticksColor, double ticksLineWidth, Color frameColor,
                  double frameLineWidth, LabelPosition labelPosition,
                  boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, null, barWidth, barHeight, nbin, ticks,
                ticksColor, ticksLineWidth, frameColor, frameLineWidth, labelPosition,
                drawUlim, drawLlim);
    }

    /**
     * Constructs a colorbar guide with the given properties and per-guide theme
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
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideColorbar(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, double barWidth, double barHeight, int nbin, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, guideTheme, null,
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor,
                frameLineWidth, labelPosition, drawUlim, drawLlim, null);
    }

    /**
     * Constructs a colorbar guide with the given properties, per-guide theme
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
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     */
    public GuideColorbar(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor,
            double barWidth, double barHeight, int nbin, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim) {
        this(title, direction, reverse, order, position, guideTheme, insideAnchor,
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor,
                frameLineWidth, labelPosition, drawUlim, drawLlim, null);
    }

    /**
     * Constructs a colorbar guide with the given properties, per-guide theme
     * overrides, inside-panel anchor, and optional 3-D shading.
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
     * @param ticks           whether tick marks are drawn at the breaks
     * @param ticksColor      the color of the tick marks, or {@code null} for the theme's text color
     * @param ticksLineWidth  the width of the tick marks
     * @param frameColor      the color of the bar's frame, or {@code null} for the theme's guide frame color
     * @param frameLineWidth  the width of the bar's frame
     * @param labelPosition   where the tick labels sit
     * @param drawUlim        whether the highest break tick is drawn
     * @param drawLlim        whether the lowest break tick is drawn
     * @param shade           the 3-D shading applied along the bar, or {@code null} for none
     */
    public GuideColorbar(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor,
            double barWidth, double barHeight, int nbin, boolean ticks,
            Color ticksColor, double ticksLineWidth, Color frameColor,
            double frameLineWidth, LabelPosition labelPosition,
            boolean drawUlim, boolean drawLlim, GuideShading shade) {
        super(title, direction, reverse, order, position, guideTheme, insideAnchor);
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
        this.shade = shade;
    }

    /** {@return the thickness of the color bar in pixels, used as its width when vertical and its height when horizontal} */
    public double barWidth() { return barWidth; }

    /** {@return the height of a vertical color bar in pixels; horizontal bars span the guide strip instead} */
    public double barHeight() { return barHeight; }

    /** {@return the number of gradient segments used to draw the bar} */
    public int nbin() { return nbin; }

    /** {@return whether tick marks are drawn at the breaks} */
    public boolean ticks() { return ticks; }

    /**
     * {@return the color of the tick marks, or {@code null} to fall back to}
     * the theme's text color.
     */
    public Color ticksColor() { return ticksColor; }

    /** {@return the width of the tick marks} */
    public double ticksLineWidth() { return ticksLineWidth; }

    /**
     * {@return the color of the bar's frame, or {@code null} to fall back to}
     * the theme's guide frame color.
     */
    public Color frameColor() { return frameColor; }

    /** {@return the width of the bar's frame} */
    public double frameLineWidth() { return frameLineWidth; }

    /** {@return whether the topmost break tick is drawn} */
    public boolean drawUlim() { return drawUlim; }

    /** {@return whether the lowest break tick is drawn} */
    public boolean drawLlim() { return drawLlim; }

    /**
     * {@return the 3-D shading applied along the bar, or {@code null} for a
     * plain 2-D colorbar}
     */
    public GuideShading shading() { return shade; }

    /**
     * {@return a copy of this colorbar shaded as a 3-D bar, modulating the}
     * gradient across the given spec.
     *
     * @param shade the 3-D shading to apply, or {@code null} to clear it
     */
    public GuideColorbar shade(GuideShading shade) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return the resolved label position: right of a vertical bar or top of}
     * a horizontal one, unless a label position was explicitly configured.
     */
    public LabelPosition labelPosition() {
        return labelPosition != null ? labelPosition
                : (effectiveDirection() == Direction.HORIZONTAL ? LabelPosition.TOP : LabelPosition.RIGHT);
    }

    /**
     * {@return a copy of this colorbar with the title set}
     *
     * @param title the guide title, or {@code null} for none
     */
    @Override
    public GuideColorbar title(String title) {
        return copyWith(title, direction(), reverse(), order());
    }

    /**
     * {@return a copy of this colorbar with the bar orientation set}
     *
     * @param direction the bar orientation
     */
    public GuideColorbar direction(Direction direction) {
        return copyWith(title(), direction, reverse(), order());
    }

    /**
     * {@return a copy of this colorbar with the reversal flag set}
     *
     * @param reverse {@code true} to put the lowest value at the top
     */
    public GuideColorbar reverse(boolean reverse) {
        return copyWith(title(), direction(), reverse, order());
    }

    /**
     * {@return a copy of this colorbar with the stacking order set}
     *
     * @param order the stacking order among multiple guides
     */
    public GuideColorbar order(int order) {
        return copyWith(title(), direction(), reverse(), order);
    }

    /**
     * {@return a copy of this colorbar with the strip side set}
     *
     * @param position the strip side this guide is placed on, overriding the
     *                 theme's guide position; {@code null} inherits the theme
     */
    @Override
    public GuideColorbar position(GuidePosition position) {
        return copyWith(title(), direction(), reverse(), order(), position);
    }

    /**
     * Returns a copy of this colorbar with per-guide style overrides.
     *
     * @param guideTheme per-guide style overrides over the theme; {@code null}
     *                   inherits everything from the theme
     * @return a copy of this colorbar carrying the given overrides
     */
    public GuideColorbar theme(GuideTheme guideTheme) {
        return copyWith(title(), direction(), reverse(), order(), position(), guideTheme);
    }

    /**
     * Returns a copy of this colorbar anchored inside the panel.
     *
     * @param x relative horizontal anchor in [0, 1] of the panel
     * @param y relative vertical anchor in [0, 1] of the panel
     * @return a copy of this guide anchored at the given panel point when
     *         placed {@link GuidePosition#INSIDE}
     */
    public GuideColorbar inside(double x, double y) {
        return copyWith(title(), direction(), reverse(), order(), position(), guideTheme(),
                new Anchor(x, y));
    }

    /**
     * {@return {@code true}; a continuous colorbar divides the width it is
     * given, so it stretches to fill a top/bottom strip}
     */
    @Override
    public boolean stretchesToFillStrip() {
        return true;
    }

    /**
     * {@return a copy of this colorbar with the bar thickness set}
     *
     * @param barWidth the thickness of the color bar in pixels
     */
    public GuideColorbar barWidth(double barWidth) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the bar height set}
     *
     * @param barHeight the height of a vertical color bar in pixels
     */
    public GuideColorbar barHeight(double barHeight) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the segment count set}
     *
     * @param nbin the number of gradient segments used to draw the bar
     */
    public GuideColorbar nbin(int nbin) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the tick visibility set}
     *
     * @param ticks {@code true} to draw tick marks at the breaks
     */
    public GuideColorbar ticks(boolean ticks) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the tick color set}
     *
     * @param ticksColor the color of the tick marks, or {@code null} for the theme's text color
     */
    public GuideColorbar ticksColor(Color ticksColor) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the tick line width set}
     *
     * @param ticksLineWidth the width of the tick marks
     */
    public GuideColorbar ticksLineWidth(double ticksLineWidth) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the frame color set}
     *
     * @param frameColor the color of the bar's frame, or {@code null} for the theme's guide frame color
     */
    public GuideColorbar frameColor(Color frameColor) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the frame line width set}
     *
     * @param frameLineWidth the width of the bar's frame
     */
    public GuideColorbar frameLineWidth(double frameLineWidth) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the label position set}
     *
     * @param labelPosition where the tick labels sit; defaults to right of a vertical bar
     */
    public GuideColorbar labelPosition(LabelPosition labelPosition) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the highest break tick visibility set}
     *
     * @param drawUlim {@code true} to draw the highest break tick
     */
    public GuideColorbar drawUlim(boolean drawUlim) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    /**
     * {@return a copy of this colorbar with the lowest break tick visibility set}
     *
     * @param drawLlim {@code true} to draw the lowest break tick
     */
    public GuideColorbar drawLlim(boolean drawLlim) {
        return new GuideColorbar(title(), direction(), reverse(), order(), position(), guideTheme(), insideAnchor(),
                barWidth, barHeight, nbin, ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    private GuideColorbar copyWith(String title, Direction direction, boolean reverse, int order) {
        return copyWith(title, direction, reverse, order, position(), guideTheme());
    }

    private GuideColorbar copyWith(String title, Direction direction, boolean reverse, int order, GuidePosition position) {
        return copyWith(title, direction, reverse, order, position, guideTheme());
    }

    private GuideColorbar copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme) {
        return copyWith(title, direction, reverse, order, position, guideTheme, insideAnchor());
    }

    private GuideColorbar copyWith(String title, Direction direction, boolean reverse, int order,
            GuidePosition position, GuideTheme guideTheme, Anchor insideAnchor) {
        return new GuideColorbar(title, direction, reverse, order, position, guideTheme, insideAnchor, barWidth, barHeight, nbin,
                ticks, ticksColor, ticksLineWidth, frameColor, frameLineWidth,
                labelPosition, drawUlim, drawLlim, shade);
    }

    @Override
    public double measure(Data data, double width) {
        return BarGuideHelper.measureHeight(title(), barExtentY(),
                effectiveDirection() == Direction.HORIZONTAL,
                title() != null && !title().isEmpty(), metrics());
    }

    @Override
    public double preferredWidth(Data data) {
        var ticks = BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(),
                data.labels(), drawLlim(), drawUlim(), true);
        double widestLabel = ticks.stream()
                .map(BarGuideHelper.Tick::label)
                .filter(l -> l != null)
                .mapToDouble(l -> TextMeasurer.width(l, resolvedLabelFont()))
                .max()
                .orElse(0.0);
        return BarGuideHelper.preferredWidth(
                effectiveDirection() == Direction.HORIZONTAL, barWidth, title(), labelFont(),
                ticks, widestLabel, metrics());
    }

    /** The bar's extent along the Y axis: its length when vertical, its thickness when horizontal. */
    private double barExtentY() {
        return effectiveDirection() == Direction.HORIZONTAL ? barWidth : barHeight;
    }

    /** The font bar tick labels are measured in, defaulting when unset. */
    private Font resolvedLabelFont() {
        var font = labelFont();
        return font != null ? font : Font.getDefault();
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

        var stops = new ArrayList<Stop>(nbin + 1);
        for (int i = 0; i <= nbin; i++) {
            double t = (double) i / nbin;
            double value = min + t * (max - min);
            var color = data.scale().colorFor(value, min, max);
            if (shade != null) {
                color = Colors.shade(color, shade.shadeAt(t), shade.hsl());
            }
            stops.add(new Stop(t, color));
        }
        var gradient = new LinearGradient(
                0.0,
                horizontal ? 0.0 : 1.0,
                horizontal ? 1.0 : 0.0,
                0.0,
                true, CycleMethod.NO_CYCLE, stops);
        gc.setFill(gradient);
        if (horizontal) {
            double barLen = width - sideTitle;
            gc.fillRect(contentX, ly, barLen, barWidth);
            gc.setStroke(frameColor != null ? frameColor : style.frameColor());
            gc.setLineWidth(frameLineWidth);
            gc.strokeRect(contentX, ly, barLen, barWidth);
            BarGuideHelper.renderTicksAndLabels(gc,
                    BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, true),
                    min, max, reverse(), ticks, ticksColor, ticksLineWidth, true,
                    contentX, ly, barLen, barWidth, style, metrics());
        } else {
            gc.fillRect(x, ly, barWidth, barHeight);
            gc.setStroke(frameColor != null ? frameColor : style.frameColor());
            gc.setLineWidth(frameLineWidth);
            gc.strokeRect(x, ly, barWidth, barHeight);
            BarGuideHelper.renderTicksAndLabels(gc,
                    BarGuideHelper.ticksAndLabels(data.domain(), data.breaks(), data.labels(), drawLlim, drawUlim, true),
                    min, max, reverse(), ticks, ticksColor, ticksLineWidth, false,
                    x, ly, barHeight, barWidth, style, metrics());
        }
    }
}
