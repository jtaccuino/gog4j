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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.guide.Guide.GuideMetrics;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.scale.Scale;

/**
 * Shared layout and rendering utilities for bar-style continuous guides
 * ({@link GuideColorbar} and {@link GuideAlpha}). Centralises tick
 * computation, tick/label drawing, title placement, and measurement so
 * both guide types stay in sync without duplicating layout logic.
 */
final class BarGuideHelper {

    /** A tick value paired with its resolved label. */
    record Tick(double value, String label) {}

    /** Measurer used to vertically centre tick labels on their tick marks. */
    private static final ThreadLocal<Text> LABEL_MEASURER = ThreadLocal.withInitial(Text::new);

    private BarGuideHelper() {}

    /**
     * Collects the labelled break ticks to draw, filtered to the bar's domain
     * and sorted ascending. Matching {@code Guides.guideColorbar}, the
     * tick labels are the "nice" breaks only. When {@code includeLimits} is
     * set, the raw domain minimum and maximum are also labelled (by default
     * does for a colourbar); when {@code includeLimits} is unset — as for an
     * alpha bar — the raw off-grid limits are omitted so no min/max label
     * collides with or hugs a neighbouring break. {@code drawLlim}/
     * {@code drawUlim} toggle the lowest/highest tick (the raw limits when
     * included, otherwise the first/last break).
     *
     * @param domain        the value range the bar spans
     * @param breaks        the break positions, or {@code null}
     * @param labels        the user-supplied labels parallel to breaks, or {@code null}
     * @param drawLlim      {@code true} to draw the lowest tick
     * @param drawUlim      {@code true} to draw the highest tick
     * @param includeLimits {@code true} to label the raw domain limits
     * @return sorted ticks
     */
    static List<Tick> ticksAndLabels(MinMax domain, List<Double> breaks,
            List<String> labels, boolean drawLlim, boolean drawUlim, boolean includeLimits) {
        double min = domain.min();
        double max = domain.max();
        double tol = (max - min) * 1e-6;
        var byValue = new LinkedHashMap<Double, String>();
        if (breaks != null) {
            for (int i = 0; i < breaks.size(); i++) {
                double b = breaks.get(i);
                if (b >= min - tol && b <= max + tol) {
                    byValue.put(b, labels != null && i < labels.size()
                            ? labels.get(i) : Scale.formatTick(b));
                }
            }
        }
        if (includeLimits) {
            if (drawLlim && !nearAny(min, byValue.keySet(), max - min)) {
                byValue.put(min, Scale.formatTick(min));
            }
            if (drawUlim && !nearAny(max, byValue.keySet(), max - min)) {
                byValue.put(max, Scale.formatTick(max));
            }
        }
        var ticks = byValue.entrySet().stream()
                .sorted(Comparator.comparingDouble(java.util.Map.Entry::getKey))
                .map(e -> new Tick(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        if (!includeLimits && !drawLlim && !ticks.isEmpty()) {
            ticks.remove(0);
        }
        if (!includeLimits && !drawUlim && !ticks.isEmpty()) {
            ticks.remove(ticks.size() - 1);
        }
        return ticks;
    }

    /** Returns whether {@code v} lies within a tiny relative tolerance of any key in {@code keys}. */
    private static boolean nearAny(double v, Collection<Double> keys, double span) {
        double tol = Math.max(Math.abs(span) * 1e-6, 1e-9);
        for (double k : keys) {
            if (Math.abs(k - v) <= tol) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws tick marks and labels at the positions computed by
     * {@link #ticksAndLabels}.
     *
     * @param gc             the drawing surface
     * @param ticks          the resolved ticks
     * @param min            the domain minimum (for t-mapping)
     * @param max            the domain maximum
     * @param reverse        {@code true} to invert the tick positions
     * @param drawTicks      {@code true} to stroke the tick marks
     * @param ticksColor     the tick colour, or {@code null} for the style default
     * @param ticksLineWidth the tick stroke width
     * @param horizontal     the bar orientation
     * @param barX           the bar's left edge
     * @param barY           the bar's top edge
     * @param barLen         the bar's length along its flow axis
     * @param barThickness   the bar's cross-axis thickness
     * @param style          the theme-derived drawing style
     */
    static void renderTicksAndLabels(DrawSurface gc, List<Tick> ticks,
            double min, double max, boolean reverse, boolean drawTicks,
            Color ticksColor, double ticksLineWidth, boolean horizontal,
            double barX, double barY, double barLen, double barThickness,
            GuideStyle style, GuideMetrics m) {
        double tickLen = m.tickLength();
        for (var tick : ticks) {
            double t = (tick.value() - min) / (max - min);
            if (reverse) {
                t = 1.0 - t;
            }
            t = Math.max(0.0, Math.min(1.0, t));

            double px;
            double py;
            if (horizontal) {
                px = barX + t * barLen;
                py = barY + barThickness;
            } else {
                px = barX + barThickness;
                py = barY + barLen - t * barLen;
            }

            if (drawTicks) {
                gc.setStroke(ticksColor != null ? ticksColor : style.tickColor());
                gc.setLineWidth(ticksLineWidth);
                if (horizontal) {
                    gc.strokeLine(px, py, px, py + tickLen);
                } else {
                    gc.strokeLine(px, py, px + tickLen, py);
                }
            }

            gc.setFill(style.keyColor());
            gc.setFont(style.keyFont());
            drawLabel(gc, tick.label(), px, py, horizontal, style.keyFont());
        }
    }

    /**
     * Draws a single tick label at the given position. Along a horizontal bar
     * the label hangs below the bar; beside a vertical bar it is centred
     * vertically on the tick mark ({@code py}) using the text's real bounds.
     *
     * @param gc       the drawing surface
     * @param label    the tick label text
     * @param px       the anchor x position
     * @param py       the tick position to centre a vertical label on
     * @param horizontal whether the bar flows horizontally
     * @param keyFont  the font used to measure and draw the label
     */
    static void drawLabel(DrawSurface gc, String label, double px, double py,
            boolean horizontal, Font keyFont) {
        if (horizontal) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(label, px, py + 16);
        } else {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(label, px + 8, verticallyCenteredBaseline(label, keyFont, py));
        }
    }

    /**
     * The baseline Y at which to draw {@code text} so that its vertical centre
     * (its layout-bounds midpoint, in the given font) sits exactly on {@code py}.
     */
    private static double verticallyCenteredBaseline(String text, Font font, double py) {
        var t = LABEL_MEASURER.get();
        t.setFont(font);
        t.setText(text);
        var bounds = t.getLayoutBounds();
        return py - bounds.getHeight() / 2.0 + t.getBaselineOffset();
    }

    /**
     * Renders the guide title: above the bar for a vertical orientation,
     * beside the bar for a horizontal one. Advances the content cursor
     * accordingly.
     *
     * @return the horizontal offset the content area should shift by
     *         (non-zero only for a horizontal title)
     */
    static double renderTitle(DrawSurface gc, String title, GuideStyle style,
            boolean horizontal, double x, double y, double barThickness, GuideMetrics m) {
        if (title == null || title.isEmpty()) {
            return 0.0;
        }
        gc.setFill(style.titleColor());
        gc.setFont(style.titleFont());
        if (horizontal) {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(title, x, y + barThickness / 2.0 + 4);
            return sideTitleWidth(title, style.titleFont(), m.contentPad());
        } else {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(title, x, y + textBaselineOffset(title, style.titleFont()));
            return verticalTitleAdvance(title, style.titleFont(), style.keyFont(), m);
        }
    }

    /**
     * The vertical advance (in pixels) a title above a vertical bar must push
     * the bar down so the title never touches the first (top) tick label. The
     * title is anchored with its ascender-top at the content origin (its
     * baseline is drawn at the title's own baseline offset) and its layout box
     * extends downward; the advance clears that box and leaves a gap above the
     * top tick label, which extends above the bar because it is vertically
     * centred on the bar's top-edge tick. The label's contribution is its real
     * {@linkplain #textInkHeight(String, Font) ink height} (the tight glyph
     * band) rather than the leading-inclusive line box, so the reserved gap is
     * the visual whitespace actually seen between the title and the label.
     *
     * @param title     the guide title, or {@code null} for none
     * @param titleFont the font the title is drawn with
     * @param keyFont   the font tick labels are drawn with
     * @param m         the guide spacing metrics
     * @return the vertical advance, or {@code 0} when there is no title
     */
    static double verticalTitleAdvance(String title, Font titleFont, Font keyFont, GuideMetrics m) {
        if (title == null || title.isEmpty()) {
            return 0.0;
        }
        double titleBottom = textLayoutHeight(title, titleFont);
        double firstTickLabelHalf = textInkHeight("0.00", keyFont) / 2.0;
        return titleBottom + firstTickLabelHalf + m.titleTickGap();
    }

    /**
     * The line-box height of {@code text} in the given font.
     */
    private static double textLayoutHeight(String text, Font font) {
        var t = LABEL_MEASURER.get();
        t.setFont(font);
        t.setText(text);
        return t.getLayoutBounds().getHeight();
    }

    /**
     * The ink (glyph) height of {@code text} in the given font. Unlike
     * {@link #textLayoutHeight(String, Font)}, this measures the tight visual
     * bounds of the glyphs, so it excludes the font's leading and the empty
     * ascender band above digits, which the line box would otherwise reserve.
     */
    private static double textInkHeight(String text, Font font) {
        var t = LABEL_MEASURER.get();
        t.setFont(font);
        t.setBoundsType(TextBoundsType.VISUAL);
        t.setText(text);
        double h = t.getLayoutBounds().getHeight();
        t.setBoundsType(TextBoundsType.LOGICAL);
        return h;
    }

    /**
     * The distance from the top of {@code text}'s layout box to its baseline in
     * the given font.
     */
    private static double textBaselineOffset(String text, Font font) {
        var t = LABEL_MEASURER.get();
        t.setFont(font);
        t.setText(text);
        return t.getBaselineOffset();
    }

    /**
     * The width the title occupies beside a horizontal bar, measured in the
     * given font, else the default font when {@code null}. Measurement is
     * always real, never estimated per character.
     */
    static double sideTitleWidth(String title, Font font, double contentPad) {
        if (title == null || title.isEmpty()) {
            return 0.0;
        }
        return TextMeasurer.width(title, resolved(font)) + contentPad;
    }

    /**
     * The height this guide occupies: title (vertical only) + bar extent +
     * tick/label space.
     */
    static double measureHeight(String title, double barExtentY,
            boolean horizontal, boolean hasTitle, GuideMetrics m) {
        double height = 0.0;
        if (hasTitle && !horizontal) {
            height += m.barTitleHeight();
        }
        height += barExtentY + (horizontal
                ? m.barTickSpaceHorizontal()
                : m.barTickSpace());
        return height;
    }

    /**
     * The natural width: for a vertical bar, whatever fits the bar plus its
     * right-hand tick labels and title; for a horizontal one, the content
     * width (widest label plus padding, floored) plus its title. Including the
     * label zone keeps right-side labels from overflowing the reserved strip
     * width.
     *
     * @param horizontal  the bar orientation
     * @param barWidth    the bar thickness
     * @param title       the guide title, or {@code null} for none
     * @param titleFont   the font the title and labels are drawn in, or
     *                    {@code null} for the default font
     * @param ticks       the resolved ticks, whose labels sit right of a vertical bar
     * @param widestLabel the width of the widest tick label, or {@code 0} if none
     * @param m           the guide spacing metrics
     */
    static double preferredWidth(boolean horizontal, double barWidth,
            String title, Font titleFont, List<Tick> ticks, double widestLabel, GuideMetrics m) {
        if (horizontal) {
            double naturalWidth = Math.max(widestLabel + m.contentPad(), m.minNaturalWidth());
            return naturalWidth + sideTitleWidth(title, titleFont, m.contentPad());
        }
        double extent = barWidth;
        double ticksExtent = ticks.stream()
                .map(Tick::label)
                .filter(l -> l != null && !l.isEmpty())
                .mapToDouble(l -> barWidth + m.contentPad() + TextMeasurer.width(l, resolved(titleFont)))
                .max()
                .orElse(0.0);
        extent = Math.max(extent, ticksExtent);
        if (title != null && !title.isEmpty()) {
            extent = Math.max(extent, sideTitleWidth(title, titleFont, m.contentPad()));
        }
        return extent;
    }

    /** The font to measure in, defaulting when none was supplied. */
    private static Font resolved(Font font) {
        return font != null ? font : Font.getDefault();
    }
}
