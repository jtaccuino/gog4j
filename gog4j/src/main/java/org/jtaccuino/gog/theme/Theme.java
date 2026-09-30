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
package org.jtaccuino.gog.theme;

import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Theme interface defining the complete visual style of an {@link org.jtaccuino.gog.Plot}.
 * <p>
 * Implementations control colors, fonts, line widths, and spacing for all
 * chart elements including panels, axes, grid lines, labels, and facet strips.
 */
public interface Theme {

    /**
     * The background color of the facet plot panels (data area).
     *
     * @return the plot panel fill color
     */
    Color plotBackground();

    /**
     * The background color of the outer Plot pane (margins, labels area).
     *
     * @return the outer pane fill color
     */
    Color paneBackground();

    /**
     * The color of the background grid lines.
     *
     * @return the grid line color
     */
    Color gridLineColor();

    /**
     * The color of the minor background grid lines, drawn at the sub-intervals
     * between the major grid lines (for example the power-of-ten sub-decades of
     * a log axis, or the sub-divided intervals of a linear axis).
     * <p>
     * Mirrors the default: defaults to the same color as the major
     * {@link #gridLineColor()}, the minor grid distinguishing itself only by a
     * thinner {@link #minorGridLineWidth()}.
     *
     * @return the minor grid line color
     */
    default Color minorGridLineColor() { return gridLineColor(); }

    /**
     * The thickness of the minor background grid lines. The default sets the
     * minor grid to {@code rel(0.5)} of the major grid line width.
     *
     * @return the minor grid line width
     */
    default double minorGridLineWidth() { return gridLineWidth() * 0.5; }

    /**
     * The length of the axis tick marks, in pixels.
     *
     * @return the major tick mark length
     */
    default double tickLength() { return 5.0; }

    /**
     * The color of the minor axis tick marks. Defaults to the same color as
     * the major {@link #axisTickColor()}, following the
     * {@code showMinorTicks()} default inherited from {@code showMajorTicks()}.
     *
     * @return the minor tick mark color
     */
    default Color minorTickColor() { return axisTickColor(); }

    /**
     * The length of the minor axis tick marks, in pixels. The default sets
     * this to {@code rel(0.75)} of the major tick length.
     *
     * @return the minor tick mark length
     */
    default double minorTickLength() { return tickLength() * 0.75; }

    /**
     * The color of the axis lines (spines).
     *
     * @return the axis line color
     */
    Color axisLineColor();

    /**
     * The color of the axis tick marks. Defaults to the theme's
     * {@link #axisLineColor()}, matching the historical rendering, but may be
     * themed independently from the axis lines.
     *
     * @return the axis tick mark color
     */
    default Color axisTickColor() { return axisLineColor(); }

    /**
     * The default text color used for labels and annotations.
     *
     * @return the text color
     */
    Color textColor();

    /**
     * The default fill color for geometry elements (bars, areas, points).
     *
     * @return the default geom fill color
     */
    Color defaultGeomFill();

    /**
     * The default stroke (outline) color for geometry elements.
     *
     * @return the default geom stroke color
     */
    Color defaultGeomStroke();

    /**
     * The default point diameter in pixels for point layers that do not set
     * their own size, e.g. a bare {@code Geoms.point()} scatter. Matches the
     * the conventional approach of a fixed visual weight rather than a
     * geometry-proportional size; composite builders (such as the pair-plot
     * matrix) pick a compact explicit size for their dense cells instead.
     *
     * @return the default point diameter in pixels
     */
    default double defaultPointSize() { return 9.0; }

    /**
     * The conventional Tableau-10 style categorical palette, shared by the
     * default discrete colour scales of all themes.
     */
    List<Color> CATEGORICAL_PALETTE = List.of(
            Color.web("#1f77b4"),
            Color.web("#ff7f0e"),
            Color.web("#2ca02c"),
            Color.web("#d62728"),
            Color.web("#9467bd"),
            Color.web("#8c564b"),
            Color.web("#e377c2"),
            Color.web("#7f7f7f"),
            Color.web("#bcbd22"),
            Color.web("#17becf"));

    /**
     * The categorical palette used by the default discrete colour scale, i.e.
     * the colours assigned to groups when no manual palette
     * ({@code Scales.scaleColorManual}) is configured. Defaults to the conventional
     * Tableau-10 style colours.
     *
     * @return the default categorical palette colours, in assignment order
     */
    default List<Color> categoricalPalette() {
        return CATEGORICAL_PALETTE;
    }

    /**
     * The name of the built-in colour ramp used by the default continuous
     * colour scale (e.g. {@code viridis}, {@code plasma}, {@code blues}).
     * Ignored when a layer requests an explicit ramp.
     *
     * @return the default continuous ramp name
     */
    default String defaultContinuousRamp() {
        return "viridis";
    }

    /**
     * The colour of neutral guide keys that encode no colour mapping (e.g.
     * shape- and size-only legends).
     *
     * @return the neutral guide key color
     */
    default Color neutralKeyColor() { return Color.web("#666666"); }

    /**
     * The last-resort colour for lookups that have no mapping at all: unmapped
     * geom defaults, degenerate scale lookups, and ungrouped single-series
     * drawing. Kept stable across themes so unknown values never collide with a
     * themed colour; a future comprehensive dark adoption may theme it.
     *
     * @return the absolute fallback colour
     */
    default Color fallbackColor() { return Color.web("#e31a1c"); }

    /**
     * The last-resort stroke colour paired with {@link #fallbackColor()},
     * used for outlines of ungrouped single-series geometry.
     *
     * @return the absolute fallback stroke colour
     */
    default Color fallbackStroke() { return Color.web("#bd0026"); }

    /**
     * The base font size (in points) from which all other font sizes in the
     * theme are derived. Changing this single value rescales every text
     * element of the chart proportionally while preserving their relative
     * hierarchy. Individual font sizes can still be overridden via their own
     * dedicated hooks (e.g. {@link #titleFont()}).
     *
     * @return the base font size in points
     */
    default double baseFontSize() { return 12.0; }

    /**
     * A multiplicative scale factor applied on top of {@link #baseFontSize()}
     * for responsive rescaling of the entire chart, e.g. when exporting at a
     * different resolution. Defaults to 1.0 (no scaling).
     *
     * @return the font scale factor
     */
    default double fontScale() { return 1.0; }

    /**
     * Builds a font derived from the theme's {@link #baseFontSize()} and
     * {@link #fontScale()}, scaled by the given relative factor.
     *
     * @param family the font family name
     * @param weight the font weight
     * @param factor multiplies the base size to reach this element's size
     * @return the derived font
     */
    private Font derivedFont(String family, FontWeight weight, double factor) {
        return Font.font(family, weight == null ? FontWeight.NORMAL : weight,
                baseFontSize() * factor * fontScale());
    }

    /**
     * The font used for the plot title.
     * Derived from {@link #baseFontSize()}.
     *
     * @return the title font
     */
    default Font titleFont() { return derivedFont("System", FontWeight.BOLD, 7.0 / 6.0); }

    /**
     * The font used for the plot subtitle, drawn directly beneath the title.
     * Defaults to the regular-weight {@link #axisTitleFont()} — the classic
     * the conventional pairing of a bold title over a plain subtitle.
     *
     * @return the subtitle font
     */
    default Font subtitleFont() { return derivedFont("System", FontWeight.NORMAL, 1.0); }

    /**
     * The font used for the plot caption, drawn at the bottom of the figure.
     * Defaults to the regular {@link #tickLabelFont()} size, mirroring
     * the muted caption treatment.
     *
     * @return the caption font
     */
    default Font captionFont() { return derivedFont("System", FontWeight.NORMAL, 3.0 / 4.0); }

    /**
     * The font used for axis titles (X and Y labels).
     * Derived from {@link #baseFontSize()}.
     *
     * @return the axis title font
     */
    default Font axisTitleFont() { return derivedFont("System", FontWeight.NORMAL, 1.0); }

    /**
     * The font used for axis tick labels.
     * Derived from {@link #baseFontSize()}.
     *
     * @return the tick label font
     */
    default Font tickLabelFont() { return derivedFont("System", FontWeight.NORMAL, 3.0 / 4.0); }

    /**
     * The stroke width of the axis lines.
     *
     * @return the axis line width in pixels
     */
    double axisLineWidth();

    /**
     * The stroke width of the background grid lines.
     *
     * @return the grid line width in pixels
     */
    double gridLineWidth();

    /**
     * The base line spacing for multi-line text elements, derived from
     * {@link #baseFontSize()}.
     *
     * @return the font spacing in pixels
     */
    default double fontSpacing() { return baseFontSize() + 2.0; }

    /**
     * The rotation angle for X-axis tick labels (degrees).
     * A value of 0 means horizontal labels; negative values tilt
     * clockwise.
     *
     * @return the rotation angle in degrees
     */
    double xLabelRotationAngle();

    /**
     * The color of the axis title text.
     *
     * @return the axis title color
     */
    Color axisTitleColor();

    /**
     * The color of the plot title text.
     *
     * @return the title text color
     */
    Color titleColor();

    /**
     * The color of the subtitle text. Defaults to the theme's
     * {@link #titleColor()}, matching the historical rendering, but may be
     * themed independently from the title.
     *
     * @return the subtitle text color
     */
    default Color subtitleColor() { return titleColor(); }

    /**
     * The color of the caption text. Defaults to the theme's
     * {@link #tickLabelColor()}, so a caption reads as secondary text.
     *
     * @return the caption text color
     */
    default Color captionColor() { return tickLabelColor(); }

    /**
     * The border color of the plot panel. May return {@code null}
     * for no border.
     *
     * @return the panel border color, or {@code null}
     */
    Color panelBorderColor();

    /**
     * The stroke width of the panel border.
     *
     * @return the panel border width in pixels
     */
    double panelBorderWidth();

    /**
     * The background color of facet strip headers.
     *
     * @return the strip background color
     */
    Color stripBackground();

    /**
     * The text color of facet strip headers.
     *
     * @return the strip text color
     */
    Color stripTextColor();

    /**
     * The font used for facet strip header text.
     * Derived from {@link #baseFontSize()}.
     *
     * @return the strip font
     */
    default Font stripFont() { return derivedFont("System", FontWeight.BOLD, 5.0 / 6.0); }

    /**
     * The horizontal gap (in pixels) between facet panels.
     *
     * @return the horizontal gap in pixels
     */
    double facetHGap();

    /**
     * The vertical gap (in pixels) between facet panels.
     *
     * @return the vertical gap in pixels
     */
    double facetVGap();

    /**
     * The reserved margin around the data panel that holds the axis tick
     * labels, tick marks, and axis titles. The actual left inset can grow
     * beyond this when a wide Y tick label needs it.
     *
     * @return the base axis label zone in pixels
     */
    default double axisLabelZone() { return 50.0; }

    /**
     * The outer x-label band a {@code PlotMatrix} reserves below its bottom row
     * for the shared x tick labels and x axis titles. Matches the standard
     * {@link #axisLabelZone()}.
     *
     * @return the matrix x-label band in pixels
     */
    default double matrixLabelBandX() { return 50.0; }

    /**
     * The outer y-label band a {@code PlotMatrix} reserves left of its first
     * column for the shared y tick labels and rotated y axis titles. Wider than
     * {@link #matrixLabelBandX()} because it also holds the rotated titles.
     *
     * @return the matrix y-label band in pixels
     */
    default double matrixLabelBandY() { return 66.0; }

    /**
     * The pivot (canvas x) for the rotated y axis titles a {@code PlotMatrix}
     * draws beside its first column, clear of the tick labels.
     *
     * @return the matrix y-title pivot in pixels
     */
    default double matrixYTitlePivot() { return 14.0; }

    /**
     * The compact per-cell label zone {@code matrixPlot()} stamps on its cells
     * so panels fill the grid, overriding {@link #axisLabelZone()} which the
     * outer bands already provide room for.
     *
     * @return the matrix cell label zone in pixels
     */
    default double matrixCellLabelZone() { return 24.0; }

    /**
     * The band a composite figure ({@code PlotMatrix}, {@code ComposedPlot})
     * reserves on top for its own title and subtitle. Roughly the single-plot
     * {@link #titleOffset()} plus the title's cap height.
     *
     * @return the composite title band in pixels
     */
    default double titleBand() { return 40.0; }

    /**
     * The offset from a composed cell's corner where a panel tag letter is
     * drawn.
     *
     * @return the panel tag offset in pixels
     */
    default double panelTagOffset() { return 6.0; }

    /**
     * The breathing padding between a guide strip and the panel edge or the
     * canvas edge, and around the guides inside a strip.
     *
     * @return the guide strip padding in pixels
     */
    default double guideStripPadding() { return 10.0; }

    /**
     * The gap between the data panel and the guide strip.
     *
     * @return the panel-to-guide gap in pixels
     */
    default double panelGuideGap() { return 20.0; }

    /**
     * The margin reserved on the right of a composite figure
     * ({@code PlotMatrix}, {@code ComposedPlot}) when no guide occupies that
     * edge, so the last panel does not run flush against the figure's right
     * side. Defaults to {@link #panelGuideGap()} — the same spacing a guide
     * would claim — but is themed independently so a composite can tighten or
     * widen its right margin without affecting the panel-to-guide gap of
     * ordinary plots.
     *
     * @return the composite right margin in pixels
     */
    default double marginRight() { return panelGuideGap(); }

    /**
     * The distance of the main plot title below the top edge of the canvas.
     *
     * @return the title offset in pixels
     */
    default double titleOffset() { return 25.0; }

    /**
     * The distance of the X-axis title below the bottom edge of the panel.
     *
     * @return the X-axis title offset in pixels
     */
    default double axisTitleOffset() { return 35.0; }

    /**
     * The height of a facet strip header above the panel.
     *
     * @return the facet strip height in pixels
     */
    default double facetStripHeight() { return 18.0; }

    /**
     * The minimum width of an {@link GuidePosition#INSIDE} guide strip.
     *
     * @return the minimum inside strip width in pixels
     */
    default double guideMinInsideWidth() { return 130.0; }

    /**
     * The color of the tick labels (axis value labels).
     *
     * @return the tick label color
     */
    Color tickLabelColor();

    /**
     * The font used for the guide title (legend or colorbar).
     * Derived from {@link #baseFontSize()}.
     *
     * @return the guide title font
     */
    default Font guideTitleFont() { return derivedFont("System", FontWeight.BOLD, 11.0 / 12.0); }

    /**
     * The color of the guide title (legend or colorbar).
     * Defaults to the theme's {@link #textColor()}, matching the historical
     * legend rendering.
     *
     * @return the guide title color
     */
    default Color guideTitleColor() { return textColor(); }

    /**
     * The font used for guide keys and colorbar tick labels.
     * Derived from {@link #baseFontSize()}.
     *
     * @return the guide key font
     */
    default Font guideKeyFont() { return derivedFont("System", FontWeight.NORMAL, 5.0 / 6.0); }

    /**
     * The color of the colorbar's frame border.
     * Defaults to the conventional light gray.
     *
     * @return the guide frame color
     */
    default Color guideFrameColor() { return Color.web("#cccccc"); }

    /**
     * The color of the tick marks and rule lines drawn on bar-style guides
     * (colorbar and alpha). Defaults to the theme's {@link #textColor()},
     * matching the historical rendering, but may be themed independently.
     *
     * @return the guide tick mark color
     */
    default Color guideTickColor() { return textColor(); }

    /**
     * The color of the border drawn around the guide stack's background box.
     * Defaults to the theme's {@link #guideFrameColor()}, matching the
     * historical rendering, but may be themed independently from the
     * colorbar frame.
     *
     * @return the guide box border color
     */
    default Color guideBoxBorderColor() { return guideFrameColor(); }

    /**
     * The side of the plot panel the guides are placed against.
     * Defaults to the right, matching the historical legend rendering.
     *
     * @return the guide position
     */
    default GuidePosition guidePosition() { return GuidePosition.RIGHT; }

    /**
     * How a side guide strip that is taller than the panel is handled.
     * Defaults to {@link GuideOverflow#FLOW_TO_BOTTOM}, which relocates the
     * overflow to a horizontal bottom strip.
     *
     * @return the guide overflow mode
     */
    default GuideOverflow guideOverflow() { return GuideOverflow.FLOW_TO_BOTTOM; }

    /**
     * The anchor for guides placed at {@link GuidePosition#INSIDE}: the
     * relative point (x, y) in [0, 1] of the panel that the guide strip is
     * centered on, following the {@code GuidePosition.INSIDE}.
     * Defaults to the panel center.
     *
     * @return the relative inside anchor
     */
    default Anchor legendInsideAnchor() { return new Anchor(0.5, 0.5); }

    /**
     * The gap between guides stacked in the same position.
     *
     * @return the guide spacing in pixels
     */
    default double guideSpacing() { return 10.0; }

    /**
     * The background color of the box drawn behind the guide stack, or
     * {@code null} to draw no box. The box border uses {@link #guideFrameColor()}.
     *
     * @return the guide box background, or {@code null}
     */
    default Color guideBoxColor() { return null; }

    /**
     * The padding between the guide box edge and its guides.
     *
     * @return the box margin in pixels
     */
    default double guideBoxMargin() { return 4.0; }

    /**
     * The length of the tick marks drawn on bar-style guides (colorbar and
     * alpha), extending outward from the bar.
     *
     * @return the guide tick mark length in pixels
     */
    default double guideTickLength() { return 4.0; }

    /**
     * The height of the title row on a bar-style guide (colorbar and alpha)
     * drawn vertically.
     *
     * @return the bar guide title row height in pixels
     */
    default double guideTitleHeight() { return 16.0; }

    /**
     * The height of the title row on a legend guide drawn vertically.
     *
     * @return the legend title row height in pixels
     */
    default double guideKeyTitleHeight() { return 22.0; }

    /**
     * The extra vertical padding per key row of a sized (size-mapped) legend,
     * keeping neighbouring circles from touching.
     *
     * @return the sized key padding in pixels
     */
    default double guideSizedKeyPad() { return 6.0; }

    /**
     * The gap between stacked rows of a bar-style guide (colorbar and alpha)
     * when multiple colour classes are drawn one below the other.
     *
     * @return the bar guide row gap in pixels
     */
    default double guideBarRowGap() { return 4.0; }

    /**
     * The tick/label space cleared under a vertically-oriented bar guide,
     * below the bar, so labels never overlap the next row or panel edge.
     *
     * @return the vertical bar guide tick/label space in pixels
     */
    default double guideBarTickSpace() { return 12.0; }

    /**
     * The tick/label space cleared beside a horizontally-oriented bar guide,
     * right of the bar, so its labels fit without clipping.
     *
     * @return the horizontal bar guide tick/label space in pixels
     */
    default double guideBarTickSpaceHorizontal() { return 22.0; }

    /**
     * The extra horizontal padding beside bar tick labels, keeping them from
     * crowding the edge of a side-by-side horizontal bar.
     *
     * @return the bar guide label padding in pixels
     */
    default double guideBarLabelPad() { return 16.0; }

    /**
     * The gap between a bar guide's title and its first tick label, so the
     * title never touches the label above the bar.
     *
     * @return the bar guide title-to-tick gap in pixels
     */
    default double guideTitleTickGap() { return 10.0; }

    /**
     * The gap between adjacent side-by-side horizontal bar segments.
     *
     * @return the bar guide segment gap in pixels
     */
    default double guideBarGap() { return 20.0; }

    /**
     * The padding between a bar or label and the reserved boundary when
     * measuring guide width.
     *
     * @return the bar guide content padding in pixels
     */
    default double guideContentPad() { return 8.0; }

    /**
     * The floor for a horizontal bar's content-driven natural width, keeping
     * a bar with few or no labels from collapsing to a degenerate width.
     *
     * @return the minimum horizontal bar natural width in pixels
     */
    default double guideMinNaturalWidth() { return 40.0; }

    /**
     * The minimum wrap width for a legend title line, so a short title doesn't
     * wrap into an excessively narrow column.
     *
     * @return the minimum legend title line width in pixels
     */
    default double legendMaxTitleLineWidth() { return 80.0; }

    /**
     * Extra width padding around legend title text when measuring.
     *
     * @return the legend title text padding in pixels
     */
    default double legendTitleTextPad() { return 4.0; }

    /**
     * Extra width padding beside a horizontal legend's side title.
     *
     * @return the legend side title padding in pixels
     */
    default double legendTitleSidePad() { return 8.0; }

    /**
     * Vertical centring offset applied to legend titles and key labels.
     *
     * @return the legend title offset in pixels
     */
    default double legendTitleOffsetY() { return 3.0; }

    /**
     * Whether vertical grid lines (at the X-axis breaks) are drawn.
     * Manhattan-style plots switch these off, since the X position carries
     * no meaning beyond chromosome order.
     *
     * @return {@code true} to draw vertical grid lines
     */
    default boolean showXGrid() { return true; }

    /**
     * Whether horizontal grid lines (at the Y-axis breaks) are drawn.
     *
     * @return {@code true} to draw horizontal grid lines
     */
    default boolean showYGrid() { return true; }

    /**
     * Whether a minor grid is drawn between the major grid lines: the
     * power-of-ten sub-decades on a log axis, or sub-divided intervals on a
     * linear axis. Drawn by default, which shows the minor
     * grid unless it is blanked.
     *
     * @return {@code true} to draw minor grid lines
     */
    default boolean showMinorGrid() { return true; }

    /**
     * Whether the major axis tick marks are drawn on either axis. Mirror of
     * the {@code showMajorTicks()} element — a theme like {@code theme_minimal}
     * blanks it, while our default theme keeps it on.
     *
     * @return {@code true} to draw major tick marks
     */
    default boolean showMajorTicks() { return true; }

    /**
     * Whether tick marks are drawn along the X axis (major by
     * {@link #showMajorTicks()}, minor by {@link #showMinorTicks()}). Mirror
     * of the per-axis {@code showXAxisTicks()} / {@code showMinorTicks()}.
     *
     * @return {@code true} to draw ticks on the X axis
     */
    default boolean showXAxisTicks() { return true; }

    /**
     * Whether tick marks are drawn along the Y axis (major by
     * {@link #showMajorTicks()}, minor by {@link #showMinorTicks()}). Mirror
     * of the per-axis {@code showYAxisTicks()} / {@code showMinorTicks()}.
     *
     * @return {@code true} to draw ticks on the Y axis
     */
    default boolean showYAxisTicks() { return true; }

    /**
     * Whether the minor axis tick marks (the short marks at the minor breaks,
     * between the major ticks) are drawn. Drawn by default.
     *
     * @return {@code true} to draw minor axis tick marks
     */
    default boolean showMinorTicks() { return false; }

    /**
     * Factory method for the default gray grammar-of-graphics theme.
     *
     * @return a new gray theme instance
     */
    static Theme theme_gray() { return new GrayTheme(); }

    /**
     * Factory method for the white {@code theme_bw()} look: a white
     * panel with light grid lines and a thin grey panel border.
     *
     * @return a new black-and-white theme instance
     */
    static Theme theme_bw() { return new BwTheme(); }

    /**
     * Factory method for the dark theme.
     *
     * @return a new dark theme instance
     */
    static Theme theme_dark() { return new DarkTheme(); }

    /**
     * Factory method for the light {@code theme_light()} look: a white
     * panel with light grey grid lines, a thin grey panel border, and dark
     * axis lines.
     *
     * @return a new light theme instance
     */
    static Theme theme_light() { return new LightTheme(); }

    /**
     * Factory method for the minimal {@code theme_minimal()} look: a
     * white background, no panel border, and minimal grid lines.
     *
     * @return a new minimal theme instance
     */
    static Theme theme_minimal() { return new MinimalTheme(); }

    /**
     * Creates a mutable derived theme that copies all properties from
     * the given base theme. The derived theme can be safely mutated
     * without affecting the base.
     *
     * @param base the base {@link Theme} to copy from
     * @return a new {@link DerivedTheme} initialized from {@code base}
     */
    static DerivedTheme derive(Theme base) {
        return new DerivedTheme(base);
    }
}
