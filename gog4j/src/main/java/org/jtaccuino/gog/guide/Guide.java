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

import javafx.scene.text.Font;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.Anchor;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * A guide is the annotation that connects a scale's values to their visual
 * encoding, the "legend" of the default. Every scale can emit a
 * guide; the concrete kinds are {@link GuideLegend} (discrete, one key per
 * category), {@link GuideColorbar} (continuous, a gradient bar with ticks), and
 * {@link GuideColorsteps} (binned continuous, a stepped colour bar).
 * <p>
 * The type parameter ties each concrete guide to its own {@link GuideData}
 * subtype, so a legend only ever measures or renders against legend data and a
 * colorbar against colorbar data &mdash; no downcasts. Heterogeneous contexts
 * (the per-aesthetic registry) hold guides as {@code Guide<?>}.
 * <p>
 * A guide is configured once and then {@linkplain #render(DrawSurface, GuideData,
 * double, double, double, GuideStyle) rendered} against the {@link GuideData}
 * built for a specific plot: categories for a legend, domain and breaks for a
 * colorbar. {@link #NONE} is the suppression marker that renders nothing.
 *
 * @param <D> the {@link GuideData} subtype this guide renders against
 */
public abstract class Guide<D extends GuideData> {

    /** Which way the guide's keys or bar flow inside its legend slot. */
    public enum Direction {
        /** Vertical flow, one key or bar segment below the next. */
        VERTICAL,
        /** Horizontal flow, keys or bar segments laid side by side. */
        HORIZONTAL
    }

    /**
     * The resolved guide-spacing metrics, derived from the active theme, that
     * guide layout and rendering share so measured size and drawn spacing stay
     * consistent.
     *
     * @param barTitleHeight        the height the bar guide title row reserves
     * @param keyTitleHeight        the height the legend title row reserves
     * @param sizedKeyPad           extra vertical padding per size-mapped key row
     * @param tickLength            the length of the tick marks on bar guides
     * @param barRowGap             the gap between stacked rows of a bar guide
     * @param barTickSpace          the tick/label zone cleared under a vertical bar
     * @param barTickSpaceHorizontal the tick/label zone cleared beside a horizontal bar
     * @param barLabelPad           extra horizontal padding for bar tick labels
     * @param titleTickGap          the gap between a bar guide title and its first tick label
     * @param barGap                the gap between adjacent side-by-side horizontal bar segments
     * @param contentPad            the padding between a bar/label and its measured boundary
     * @param minNaturalWidth       the floor for a horizontal bar's natural content width
     * @param maxTitleLineWidth     the minimum wrap width for a legend title line
     * @param titleTextPad          extra width padding around legend title text
     * @param titleSidePad          extra width padding beside a horizontal legend's title
     * @param titleOffsetY          vertical centring offset for legend title/key label text
     */
    public record GuideMetrics(double barTitleHeight, double keyTitleHeight,
                               double sizedKeyPad, double tickLength,
                               double barRowGap, double barTickSpace,
                               double barTickSpaceHorizontal, double barLabelPad,
                               double titleTickGap, double barGap, double contentPad,
                               double minNaturalWidth, double maxTitleLineWidth,
                               double titleTextPad, double titleSidePad,
                               double titleOffsetY) {}

    /**
     * Suppression marker: a guide that renders nothing, used to turn a scale's
     * guide off while keeping it registered (mirrors {@code guides(aes = "none")}).
     * Accepts any guide data, since it never looks at it.
     */
    public static final Guide<GuideData> NONE = new Guide<>(null, Direction.VERTICAL, false, 0) {
        @Override
        public double measure(GuideData data, double width) {
            return 0.0;
        }

        @Override
        public double preferredWidth(GuideData data) {
            return 0.0;
        }

        @Override
        public void render(DrawSurface gc, GuideData data, double x, double y, double width, GuideStyle style) {
            // intentionally empty
        }

        @Override
        public Guide<GuideData> title(String title) {
            return this;
        }

        @Override
        public Guide<GuideData> position(GuidePosition position) {
            return this;
        }
    };

    private final String title;
    private final Direction direction;
    private final boolean reverse;
    private final int order;
    private final GuidePosition position;
    private final GuideTheme guideTheme;
    private final Anchor insideAnchor;

    /**
     * The font guide labels are measured and laid out in, or {@code null} while
     * unset. This is render-time configuration resolved from the plot's theme
     * ({@code guideKeyFont()}) just before layout and rendering; it does not
     * participate in the guide's semantic identity, so it is not part of the
     * immutable copy chain.
     */
    private Font labelFont;

    /**
     * The font guide titles are measured and laid out in, or {@code null} while
     * unset. Like {@link #labelFont}, this is render-time configuration resolved
     * from the plot's theme ({@code guideTitleFont()}) just before layout and
     * rendering; it does not participate in the guide's semantic identity, so it
     * is not part of the immutable copy chain.
     */
    private Font titleFont;

    /**
     * The spacing metrics guide layout and rendering use, or {@code null}
     * until set by the plot's theming pipeline. Like {@link #labelFont}, this
     * is render-time configuration resolved from the theme and does not
     * participate in the guide's immutable copy chain.
     */
    private GuideMetrics metrics;

    /**
     * Constructs a guide with the given title, direction, reverse flag, and
     * stacking order, inheriting the theme's guide position.
     *
     * @param title     the guide title, or {@code null} for none
     * @param direction the flow direction; {@code null} means {@link Direction#VERTICAL}
     * @param reverse   {@code true} to reverse the key order or bar direction
     * @param order     the stacking order among multiple guides
     */
    protected Guide(String title, Direction direction, boolean reverse, int order) {
        this(title, direction, reverse, order, null);
    }

    /**
     * Constructs a guide with the given title, direction, reverse flag, stacking
     * order, and strip-side position.
     *
     * @param title     the guide title, or {@code null} for none
     * @param direction the flow direction; {@code null} means {@link Direction#VERTICAL}
     * @param reverse   {@code true} to reverse the key order or bar direction
     * @param order     the stacking order among multiple guides
     * @param position  the strip side this guide is placed on, or {@code null} to
     *                  inherit the theme's guide position
     */
    protected Guide(String title, Direction direction, boolean reverse, int order, GuidePosition position) {
        this(title, direction, reverse, order, position, null);
    }

    /**
     * Creates a guide with the given display properties.
     *
     * @param title      the guide title, or {@code null} for none
     * @param direction  the flow direction; {@code null} means {@link Direction#VERTICAL}
     * @param reverse    {@code true} to reverse the key order or bar direction
     * @param order      the stacking order among multiple guides
     * @param position   the strip side this guide is placed on, or {@code null} to
     *                   inherit the theme's guide position
     * @param guideTheme per-guide style overrides over the theme, or {@code null} to
     *                   inherit everything from the theme
     */
    protected Guide(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme) {
        this(title, direction, reverse, order, position, guideTheme, null);
    }

    /**
     * Creates a guide with the given display properties and inside anchor.
     *
     * @param title        the guide title, or {@code null} for none
     * @param direction    the flow direction; {@code null} means {@link Direction#VERTICAL}
     * @param reverse      {@code true} to reverse the key order or bar direction
     * @param order        the stacking order among multiple guides
     * @param position     the strip side this guide is placed on, or {@code null} to
     *                     inherit the theme's guide position
     * @param guideTheme   per-guide style overrides over the theme, or {@code null} to
     *                     inherit everything from the theme
     * @param insideAnchor the panel-relative anchor for {@link GuidePosition#INSIDE}
     *                     placement, or {@code null} to inherit the theme's anchor
     */
    protected Guide(String title, Direction direction, boolean reverse, int order, GuidePosition position,
            GuideTheme guideTheme, Anchor insideAnchor) {
        this.title = title;
        this.direction = direction == null ? Direction.VERTICAL : direction;
        this.reverse = reverse;
        this.order = order;
        this.position = position;
        this.guideTheme = guideTheme;
        this.insideAnchor = insideAnchor;
    }

    /** {@return the guide title, or {@code null} when no title is drawn} */
    public String title() { return title; }

    /**
     * Returns a copy of this guide with the given title. Concrete guides apply
     * immutable copy semantics; {@link Guide#NONE} returns itself, as it has no
     * state and renders nothing.
     *
     * @param title the guide title, or {@code null} for none
     * @return the titled guide
     */
    public abstract Guide<D> title(String title);

    /** {@return the flow direction of the guide} */
    public Direction direction() { return direction; }

    /**
     * {@return the direction the guide renders with}
     * <p>
     * A vertically-flowing guide placed on a horizontal strip
     * ({@code TOP}/{@code BOTTOM}, by override or theme) flows horizontally
     * instead, following the position-derived legend layout. An explicit
     * direction on any other side is kept as configured.
     */
    public Direction effectiveDirection() {
        return direction == Direction.VERTICAL
                && (position == GuidePosition.TOP || position == GuidePosition.BOTTOM)
                ? Direction.HORIZONTAL
                : direction;
    }

    /** {@return whether the keys or the colorbar direction are reversed} */
    public boolean reverse() { return reverse; }

    /** {@return the stacking order among multiple guides} */
    public int order() { return order; }

    /**
     * {@return the strip side this guide is placed on, or {@code null} to}
     * inherit the theme's guide position.
     */
    public GuidePosition position() { return position; }

    /**
     * Returns a copy of this guide placed on the given strip side. Concrete
     * guides apply immutable copy semantics; {@link Guide#NONE} returns itself,
     * as it has no state and renders nothing.
     *
     * @param position the strip side, or {@code null} to inherit the theme's
     * @return the repositioned guide
     */
    public abstract Guide<D> position(GuidePosition position);

    /**
     * {@return the per-guide style overrides over the theme, or {@code null}}
     * when everything is inherited
     */
    public GuideTheme guideTheme() { return guideTheme; }

    /**
     * {@return the panel-relative anchor for {@link GuidePosition#INSIDE}}
     * placement, or {@code null} to inherit the theme's anchor
     */
    public Anchor insideAnchor() { return insideAnchor; }

    /**
     * {@return whether this continuous bar guide should stretch to fill the
     * full horizontal space of a top/bottom strip}
     * <p>
     * Bar guides (a colorbar, or an alpha guide with several colour classes)
     * divide whatever width they are given, so when a top/bottom strip has
     * room to spare they expand to fill it, sharing the leftover width with
     * other bar guides. Discrete legends keep their natural width.
     */
    public boolean stretchesToFillStrip() {
        return false;
    }

    /**
     * Sets the font used to measure and lay out this guide's labels.
     * <p>
     * Called by the plot's theming pipeline with the theme's guide key font
     * before layout; {@code measure} and {@code preferredWidth} use it to size
     * labels precisely instead of estimating from character counts. A
     * {@code null} font (the initial state) leaves the legacy per-character
     * estimate in place.
     *
     * @param font the label font, or {@code null} to fall back to the estimate
     * @return this guide
     */
    public Guide<D> withLabelFont(Font font) {
        this.labelFont = font;
        return this;
    }

    /**
     * {@return the font guide labels are laid out in, or {@code null} when}
     * the legacy per-character estimate is used
     */
    public Font labelFont() { return labelFont; }

    /**
     * Sets the font used to measure and lay out this guide's title.
     * <p>
     * Called by the plot's theming pipeline with the theme's guide title font
     * before layout; title measurement uses it so wrapped titles are sized to
     * the text actually rendered, matching the theme.
     *
     * @param font the title font, or {@code null} when no title is measured
     * @return this guide
     */
    public Guide<D> withTitleFont(Font font) {
        this.titleFont = font;
        return this;
    }

    /**
     * {@return the font guide titles are laid out in, or {@code null} when}
     * no title font was pushed by the theming pipeline
     */
    public Font titleFont() { return titleFont; }

    /**
     * The spacing metrics this guide lays itself out and renders with, or
     * {@code null} until the plot's theming pipeline pushes them.
     *
     * @return the guide's spacing metrics, or {@code null} when unset
     */
    public GuideMetrics metrics() { return metrics; }

    /**
     * Sets the spacing metrics this guide uses for layout and rendering.
     * <p>
     * Called by the plot's theming pipeline with the theme's guide spacing
     * before layout; both {@code measure}/{@code preferredWidth} and the render
     * paths read the same metrics, so layout and drawing stay consistent.
     *
     * @param m the guide spacing metrics, or {@code null} to use the built-in
     *          defaults
     * @return this guide
     */
    public Guide<D> withMetrics(GuideMetrics m) {
        this.metrics = m;
        return this;
    }

    /**
     * Computes the height in pixels this guide occupies in a legend slot of the
     * given width, given the built data.
     *
     * @param data  this guide's built {@link GuideData} subtype
     * @param width the width available to the guide
     * @return the height the guide needs
     */
    public abstract double measure(D data, double width);

    /**
     * Computes the width this guide needs when it is not constrained, e.g.
     * when it flows horizontally in a top or bottom legend slot.
     *
     * @param data this guide's built {@link GuideData} subtype
     * @return the natural width of the guide in pixels
     */
    public abstract double preferredWidth(D data);

    /**
     * Draws this guide into the legend slot starting at the given top-left
     * corner.
     *
     * @param gc    the drawing surface
     * @param data  this guide's built {@link GuideData} subtype
     * @param x     the left edge of the slot
     * @param y     the top edge of the slot
     * @param width the width available to the guide
     * @param style the theme-derived drawing style
     */
    public abstract void render(DrawSurface gc, D data, double x, double y, double width, GuideStyle style);
}
