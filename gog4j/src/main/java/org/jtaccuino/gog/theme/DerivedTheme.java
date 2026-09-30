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
 * A mutable, derived theme that copies all properties from a base {@link Theme}.
 * Supports fluent mutation via builder-style setters, allowing safe
 * per-plot theme overrides without modifying the original theme instance.
 */
public class DerivedTheme implements Theme3d {
    private List<Color> categoricalPalette;
    private String defaultContinuousRamp;
    private Color neutralKeyColor;
    private Color fallbackColor;
    private Color fallbackStroke;
    private Color plotBackground; private Color paneBackground;
    private Color gridLineColor; private Color minorGridLineColor; private Color axisLineColor;
    private Color axisTickColor; private Color minorTickColor;
    private Color textColor; private Color defaultGeomFill; private Color defaultGeomStroke; private Color tickLabelColor;
    private double defaultPointSize;
    private Font titleFont; private Font axisTitleFont; private Font tickLabelFont;
    private double axisLineWidth; private double gridLineWidth; private double minorGridLineWidth; private double fontSpacing;
    private double tickLength; private double minorTickLength;
    private double xLabelRotationAngle;
    private double facetHGap; private double facetVGap;

    // New fields
    private Color axisTitleColor; private Color titleColor;
    private Color panelBorderColor; private double panelBorderWidth;
    private Color stripBackground; private Color stripTextColor; private Font stripFont;
    private boolean showXGrid; private boolean showYGrid; private boolean showMinorGrid; private boolean showMinorTicks;
    private boolean showMajorTicks; private boolean showXAxisTicks; private boolean showYAxisTicks;
    private GuidePosition guidePosition; private double guideSpacing;
    private Color guideBoxColor; private double guideBoxMargin;
    private GuideOverflow guideOverflow;
    private Color guideTickColor;
    private Color guideBoxBorderColor;
    private double axisLabelZone; private double guideStripPadding; private double panelGuideGap;
    private double marginRight;
    private double matrixLabelBandX; private double matrixLabelBandY; private double matrixYTitlePivot;
    private double matrixCellLabelZone; private double titleBand; private double panelTagOffset;
    private double titleOffset; private double axisTitleOffset; private double facetStripHeight;
    private double guideMinInsideWidth;
    private double guideTickLength; private double guideTitleHeight;
    private double guideKeyTitleHeight; private double guideSizedKeyPad;
    private double guideBarRowGap; private double guideBarTickSpace;
    private double guideBarTickSpaceHorizontal; private double guideBarLabelPad;
    private double guideTitleTickGap;
    private double guideBarGap; private double guideContentPad;
    private double guideMinNaturalWidth; private double legendMaxTitleLineWidth;
    private double legendTitleTextPad; private double legendTitleSidePad;
    private double legendTitleOffsetY;
    private double baseFontSize; private double fontScale;
    private Font guideTitleFont; private Font guideKeyFont;
    private Color guideTitleColor; private Color guideFrameColor;
    private Anchor legendInsideAnchor;
    private Font subtitleFont; private Font captionFont;
    private Color subtitleColor; private Color captionColor;

    /**
     * Constructs a derived theme by copying all properties from the given base theme.
     *
     * @param base the base {@link Theme} to copy
     */
    public DerivedTheme(Theme base) {
        this.categoricalPalette = base.categoricalPalette();
        this.defaultContinuousRamp = base.defaultContinuousRamp();
        this.neutralKeyColor = base.neutralKeyColor();
        this.fallbackColor = base.fallbackColor();
        this.fallbackStroke = base.fallbackStroke();
        this.plotBackground = base.plotBackground();
        this.paneBackground = base.paneBackground();
        this.gridLineColor = base.gridLineColor();
        this.minorGridLineColor = base.minorGridLineColor();
        this.axisLineColor = base.axisLineColor();
        this.axisTickColor = base.axisTickColor();
        this.minorTickColor = base.minorTickColor();
        this.textColor = base.textColor();
        this.defaultGeomFill = base.defaultGeomFill();
        this.defaultGeomStroke = base.defaultGeomStroke();
        this.defaultPointSize = base.defaultPointSize();
        this.tickLabelColor = base.tickLabelColor();
        this.titleFont = base.titleFont();
        this.axisTitleFont = base.axisTitleFont();
        this.tickLabelFont = base.tickLabelFont();
        this.axisLineWidth = base.axisLineWidth();
        this.gridLineWidth = base.gridLineWidth();
        this.minorGridLineWidth = base.minorGridLineWidth();
        this.tickLength = base.tickLength();
        this.minorTickLength = base.minorTickLength();
        this.fontSpacing = base.fontSpacing();
        this.xLabelRotationAngle = base.xLabelRotationAngle();
        this.facetHGap = base.facetHGap();
        this.facetVGap = base.facetVGap();

        // Deep copy of new attributes
        this.axisTitleColor = base.axisTitleColor();
        this.titleColor = base.titleColor();
        this.panelBorderColor = base.panelBorderColor();
        this.panelBorderWidth = base.panelBorderWidth();
        this.stripBackground = base.stripBackground();
        this.stripTextColor = base.stripTextColor();
        this.stripFont = base.stripFont();
        this.showXGrid = base.showXGrid();
        this.showYGrid = base.showYGrid();
        this.showMinorGrid = base.showMinorGrid();
        this.showMinorTicks = base.showMinorTicks();
        this.showMajorTicks = base.showMajorTicks();
        this.showXAxisTicks = base.showXAxisTicks();
        this.showYAxisTicks = base.showYAxisTicks();
        this.guidePosition = base.guidePosition();
        this.guideSpacing = base.guideSpacing();
        this.guideBoxColor = base.guideBoxColor();
        this.guideBoxMargin = base.guideBoxMargin();
        this.guideOverflow = base.guideOverflow();
        this.guideTickColor = base.guideTickColor();
        this.guideBoxBorderColor = base.guideBoxBorderColor();
        this.axisLabelZone = base.axisLabelZone();
        this.matrixLabelBandX = base.matrixLabelBandX();
        this.matrixLabelBandY = base.matrixLabelBandY();
        this.matrixYTitlePivot = base.matrixYTitlePivot();
        this.matrixCellLabelZone = base.matrixCellLabelZone();
        this.titleBand = base.titleBand();
        this.panelTagOffset = base.panelTagOffset();
        this.guideStripPadding = base.guideStripPadding();
        this.panelGuideGap = base.panelGuideGap();
        this.marginRight = base.marginRight();
        this.titleOffset = base.titleOffset();
        this.axisTitleOffset = base.axisTitleOffset();
        this.facetStripHeight = base.facetStripHeight();
        this.guideMinInsideWidth = base.guideMinInsideWidth();
        this.guideTickLength = base.guideTickLength();
        this.guideTitleHeight = base.guideTitleHeight();
        this.guideKeyTitleHeight = base.guideKeyTitleHeight();
        this.guideSizedKeyPad = base.guideSizedKeyPad();
        this.guideBarRowGap = base.guideBarRowGap();
        this.guideBarTickSpace = base.guideBarTickSpace();
        this.guideBarTickSpaceHorizontal = base.guideBarTickSpaceHorizontal();
        this.guideBarLabelPad = base.guideBarLabelPad();
        this.guideTitleTickGap = base.guideTitleTickGap();
        this.guideBarGap = base.guideBarGap();
        this.guideContentPad = base.guideContentPad();
        this.guideMinNaturalWidth = base.guideMinNaturalWidth();
        this.legendMaxTitleLineWidth = base.legendMaxTitleLineWidth();
        this.legendTitleTextPad = base.legendTitleTextPad();
        this.legendTitleSidePad = base.legendTitleSidePad();
        this.legendTitleOffsetY = base.legendTitleOffsetY();
        this.baseFontSize = base.baseFontSize();
        this.fontScale = base.fontScale();
        this.guideTitleFont = base.guideTitleFont();
        this.guideKeyFont = base.guideKeyFont();
        this.guideTitleColor = base.guideTitleColor();
        this.guideFrameColor = base.guideFrameColor();
        this.legendInsideAnchor = base.legendInsideAnchor();
        this.subtitleFont = base.subtitleFont();
        this.captionFont = base.captionFont();
        this.subtitleColor = base.subtitleColor();
        this.captionColor = base.captionColor();
    }

    // --- Interface Getters ---
    @Override public List<Color> categoricalPalette() { return categoricalPalette; }
    @Override public String defaultContinuousRamp() { return defaultContinuousRamp; }
    @Override public Color neutralKeyColor() { return neutralKeyColor; }
    @Override public Color fallbackColor() { return fallbackColor; }
    @Override public Color fallbackStroke() { return fallbackStroke; }
    @Override public Color plotBackground() { return plotBackground; }
    @Override public Color paneBackground() { return paneBackground; }
    @Override public Color gridLineColor() { return gridLineColor; }
    @Override public Color minorGridLineColor() { return minorGridLineColor; }
    @Override public Color axisLineColor() { return axisLineColor; }
    @Override public Color axisTickColor() { return axisTickColor; }
    @Override public Color minorTickColor() { return minorTickColor; }
    @Override public Color textColor() { return textColor; }
    @Override public Color defaultGeomFill() { return defaultGeomFill; }
    @Override public Color defaultGeomStroke() { return defaultGeomStroke; }
    @Override public double defaultPointSize() { return defaultPointSize; }
    @Override public Color tickLabelColor() { return tickLabelColor; }
    @Override public Font titleFont() { return titleFont; }
    @Override public Font axisTitleFont() { return axisTitleFont; }
    @Override public Font tickLabelFont() { return tickLabelFont; }
    @Override public double axisLineWidth() { return axisLineWidth; }
    @Override public double gridLineWidth() { return gridLineWidth; }
    @Override public double minorGridLineWidth() { return minorGridLineWidth; }
    @Override public double tickLength() { return tickLength; }
    @Override public double minorTickLength() { return minorTickLength; }
    @Override public double fontSpacing() { return fontSpacing; }
    @Override public double xLabelRotationAngle() { return xLabelRotationAngle; }
    @Override public double facetHGap() { return facetHGap; }
    @Override public double facetVGap() { return facetVGap; }
    @Override public Color axisTitleColor() { return axisTitleColor; }
    @Override public Color titleColor() { return titleColor; }
    @Override public Color panelBorderColor() { return panelBorderColor; }
    @Override public double panelBorderWidth() { return panelBorderWidth; }
    @Override public Color stripBackground() { return stripBackground; }
    @Override public Color stripTextColor() { return stripTextColor; }
    @Override public Font stripFont() { return stripFont; }
    @Override public boolean showXGrid() { return showXGrid; }
    @Override public boolean showYGrid() { return showYGrid; }
    @Override public boolean showMinorGrid() { return showMinorGrid; }
    @Override public boolean showMinorTicks() { return showMinorTicks; }
    @Override public boolean showMajorTicks() { return showMajorTicks; }
    @Override public boolean showXAxisTicks() { return showXAxisTicks; }
    @Override public boolean showYAxisTicks() { return showYAxisTicks; }
    @Override public GuidePosition guidePosition() { return guidePosition; }
    @Override public double guideSpacing() { return guideSpacing; }
    @Override public Color guideBoxColor() { return guideBoxColor; }
    @Override public double guideBoxMargin() { return guideBoxMargin; }
    @Override public GuideOverflow guideOverflow() { return guideOverflow; }
    @Override public Color guideTickColor() { return guideTickColor; }
    @Override public Color guideBoxBorderColor() { return guideBoxBorderColor; }
    @Override public double axisLabelZone() { return axisLabelZone; }
    @Override public double matrixLabelBandX() { return matrixLabelBandX; }
    @Override public double matrixLabelBandY() { return matrixLabelBandY; }
    @Override public double matrixYTitlePivot() { return matrixYTitlePivot; }
    @Override public double matrixCellLabelZone() { return matrixCellLabelZone; }
    @Override public double titleBand() { return titleBand; }
    @Override public double panelTagOffset() { return panelTagOffset; }
    @Override public double guideStripPadding() { return guideStripPadding; }
    @Override public double panelGuideGap() { return panelGuideGap; }
    @Override public double marginRight() { return marginRight; }
    @Override public double titleOffset() { return titleOffset; }
    @Override public double axisTitleOffset() { return axisTitleOffset; }
    @Override public double facetStripHeight() { return facetStripHeight; }
    @Override public double guideMinInsideWidth() { return guideMinInsideWidth; }
    @Override public double guideTickLength() { return guideTickLength; }
    @Override public double guideTitleHeight() { return guideTitleHeight; }
    @Override public double guideKeyTitleHeight() { return guideKeyTitleHeight; }
    @Override public double guideSizedKeyPad() { return guideSizedKeyPad; }
    @Override public double guideBarRowGap() { return guideBarRowGap; }
    @Override public double guideBarTickSpace() { return guideBarTickSpace; }
    @Override public double guideBarTickSpaceHorizontal() { return guideBarTickSpaceHorizontal; }
    @Override public double guideBarLabelPad() { return guideBarLabelPad; }
    @Override public double guideTitleTickGap() { return guideTitleTickGap; }
    @Override public double guideBarGap() { return guideBarGap; }
    @Override public double guideContentPad() { return guideContentPad; }
    @Override public double guideMinNaturalWidth() { return guideMinNaturalWidth; }
    @Override public double legendMaxTitleLineWidth() { return legendMaxTitleLineWidth; }
    @Override public double legendTitleTextPad() { return legendTitleTextPad; }
    @Override public double legendTitleSidePad() { return legendTitleSidePad; }
    @Override public double legendTitleOffsetY() { return legendTitleOffsetY; }
    @Override public double baseFontSize() { return baseFontSize; }
    @Override public double fontScale() { return fontScale; }
    @Override public Font guideTitleFont() { return guideTitleFont; }
    @Override public Font guideKeyFont() { return guideKeyFont; }
    @Override public Color guideTitleColor() { return guideTitleColor; }
    @Override public Color guideFrameColor() { return guideFrameColor; }
    @Override public Anchor legendInsideAnchor() { return legendInsideAnchor; }
    @Override public Font subtitleFont() { return subtitleFont; }
    @Override public Font captionFont() { return captionFont; }
    @Override public Color subtitleColor() { return subtitleColor; }
    @Override public Color captionColor() { return captionColor; }

    // --- Fluent mutation API ---

    /**
     * Overrides the plot panel background color.
     *
     * @param c the new plot panel background color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme plotBackground(Color c) { this.plotBackground = c; return this; }
    /**
     * Overrides the outer pane background color.
     *
     * @param c the new outer pane background color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme paneBackground(Color c) { this.paneBackground = c; return this; }
    /**
     * Overrides the grid line color.
     *
     * @param c the new grid line color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme gridLineColor(Color c) { this.gridLineColor = c; return this; }
    /**
     * Overrides the minor grid line color.
     *
     * @param c the new minor grid line color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme minorGridLineColor(Color c) { this.minorGridLineColor = c; return this; }
    /**
     * Overrides the axis line color.
     *
     * @param c the new axis line color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisLineColor(Color c) { this.axisLineColor = c; return this; }

    /**
     * Overrides the axis tick mark color.
     *
     * @param c the new axis tick mark color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisTickColor(Color c) { this.axisTickColor = c; return this; }

    /**
     * Overrides the minor axis tick color.
     *
     * @param c the new minor tick color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme minorTickColor(Color c) { this.minorTickColor = c; return this; }

    /**
     * Overrides the default text color.
     *
     * @param c the new default text color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme textColor(Color c) { this.textColor = c; return this; }
    /**
     * Overrides the default geom fill color.
     *
     * @param c the new default geom fill color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme defaultGeomFill(Color c) { this.defaultGeomFill = c; return this; }
    /**
     * Overrides the default geom stroke color.
     *
     * @param c the new default geom stroke color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme defaultGeomStroke(Color c) { this.defaultGeomStroke = c; return this; }
    /**
     * Overrides the default point diameter in pixels for point layers that do
     * not set their own size.
     *
     * @param size the new default point diameter in pixels
     * @return this theme, for fluent chaining
     */
    public DerivedTheme defaultPointSize(double size) { this.defaultPointSize = size; return this; }

    /**
     * Overrides the categorical palette used by the default discrete colour
     * scale. Manual palettes configured via {@code Scales.scaleColorManual} still
     * take precedence over this palette.
     *
     * @param palette the new palette colours, in assignment order
     * @return this theme, for fluent chaining
     */
    public DerivedTheme categoricalPalette(List<Color> palette) { this.categoricalPalette = palette; return this; }

    /**
     * Overrides the name of the ramp used by the default continuous colour scale.
     *
     * @param name the built-in ramp name (e.g. {@code viridis})
     * @return this theme, for fluent chaining
     */
    public DerivedTheme defaultContinuousRamp(String name) { this.defaultContinuousRamp = name; return this; }

    /**
     * Overrides the colour of neutral guide keys (shape- and size-only legends).
     *
     * @param c the new neutral key color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme neutralKeyColor(Color c) { this.neutralKeyColor = c; return this; }

    /**
     * Overrides the absolute last-resort fallback colour.
     *
     * @param c the new fallback color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme fallbackColor(Color c) { this.fallbackColor = c; return this; }

    /**
     * Overrides the absolute last-resort fallback stroke colour.
     *
     * @param c the new fallback stroke color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme fallbackStroke(Color c) { this.fallbackStroke = c; return this; }
    /**
     * Overrides the axis tick label color.
     *
     * @param c the new axis tick label color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme tickLabelColor(Color c) { this.tickLabelColor = c; return this; }
    /**
     * Overrides the title font.
     *
     * @param f the new title font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme titleFont(Font f) { this.titleFont = f; return this; }
    /**
     * Overrides the subtitle font. Defaults to a normal-weight version of
     * {@link #titleFont()} derived from the current {@link #baseFontSize()}.
     *
     * @param f the new subtitle font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme subtitleFont(Font f) { this.subtitleFont = f; return this; }
    /**
     * Overrides the caption font.
     *
     * @param f the new caption font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme captionFont(Font f) { this.captionFont = f; return this; }
    /**
     * Overrides the subtitle color.
     *
     * @param c the new subtitle text color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme subtitleColor(Color c) { this.subtitleColor = c; return this; }
    /**
     * Overrides the caption color.
     *
     * @param c the new caption text color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme captionColor(Color c) { this.captionColor = c; return this; }
    /**
     * Overrides the axis title font.
     *
     * @param f the new axis title font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisTitleFont(Font f) { this.axisTitleFont = f; return this; }
    /**
     * Overrides the tick label font.
     *
     * @param f the new tick label font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme tickLabelFont(Font f) { this.tickLabelFont = f; return this; }
    /**
     * Overrides the axis line width.
     *
     * @param w the new axis line width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisLineWidth(double w) { this.axisLineWidth = w; return this; }
    /**
     * Overrides the grid line width.
     *
     * @param w the new grid line width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme gridLineWidth(double w) { this.gridLineWidth = w; return this; }
    /**
     * Overrides the minor grid line width.
     *
     * @param w the new minor grid line width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme minorGridLineWidth(double w) { this.minorGridLineWidth = w; return this; }
    /**
     * Overrides the axis tick mark length.
     *
     * @param len the new tick mark length
     * @return this theme, for fluent chaining
     */
    public DerivedTheme tickLength(double len) { this.tickLength = len; return this; }
    /**
     * Overrides the minor axis tick mark length.
     *
     * @param len the new minor tick mark length
     * @return this theme, for fluent chaining
     */
    public DerivedTheme minorTickLength(double len) { this.minorTickLength = len; return this; }
    /**
     * Overrides the font line spacing.
     *
     * @param s the new font line spacing
     * @return this theme, for fluent chaining
     */
    public DerivedTheme fontSpacing(double s) { this.fontSpacing = s; return this; }
    /**
     * Overrides the X tick label rotation angle.
     *
     * @param angle the new rotation angle in degrees
     * @return this theme, for fluent chaining
     */
    public DerivedTheme xLabelRotationAngle(double angle) { this.xLabelRotationAngle = angle; return this; }
    /**
     * Overrides the horizontal gap between facet panels.
     *
     * @param gap the new horizontal facet gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme facetHGap(double gap) { this.facetHGap = gap; return this; }
    /**
     * Overrides the vertical gap between facet panels.
     *
     * @param gap the new vertical facet gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme facetVGap(double gap) { this.facetVGap = gap; return this; }

    /**
     * Overrides the axis title color.
     *
     * @param c the new axis title color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisTitleColor(Color c) { this.axisTitleColor = c; return this; }
    /**
     * Overrides the plot title color.
     *
     * @param c the new plot title color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme titleColor(Color c) { this.titleColor = c; return this; }
    /**
     * Overrides the panel border color.
     *
     * @param c the new panel border color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme panelBorderColor(Color c) { this.panelBorderColor = c; return this; }
    /**
     * Overrides the panel border width.
     *
     * @param w the new panel border width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme panelBorderWidth(double w) { this.panelBorderWidth = w; return this; }
    /**
     * Overrides the facet strip background color.
     *
     * @param c the new facet strip background color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme stripBackground(Color c) { this.stripBackground = c; return this; }
    /**
     * Overrides the facet strip text color.
     *
     * @param c the new facet strip text color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme stripTextColor(Color c) { this.stripTextColor = c; return this; }
    /**
     * Overrides the facet strip font.
     *
     * @param f the new facet strip font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme stripFont(Font f) { this.stripFont = f; return this; }

    /**
     * Toggles the vertical grid lines drawn at the X-axis breaks.
     *
     * @param show {@code true} to draw the vertical grid lines
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showXGrid(boolean show) { this.showXGrid = show; return this; }

    /**
     * Toggles the horizontal grid lines drawn at the Y-axis breaks.
     *
     * @param show {@code true} to draw the horizontal grid lines
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showYGrid(boolean show) { this.showYGrid = show; return this; }

    /**
     * Toggles the minor grid lines drawn between the major grid lines.
     *
     * @param show {@code true} to draw the minor grid lines
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showMinorGrid(boolean show) { this.showMinorGrid = show; return this; }

    /**
     * Toggles the minor axis tick marks drawn at the minor breaks.
     *
     * @param show {@code true} to draw the minor tick marks
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showMinorTicks(boolean show) { this.showMinorTicks = show; return this; }

    /**
     * Toggles the major axis tick marks drawn on either axis.
     *
     * @param show {@code true} to draw the major tick marks
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showMajorTicks(boolean show) { this.showMajorTicks = show; return this; }

    /**
     * Toggles tick marks along the X axis (both major and minor).
     *
     * @param show {@code true} to draw ticks on the X axis
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showXAxisTicks(boolean show) { this.showXAxisTicks = show; return this; }

    /**
     * Toggles tick marks along the Y axis (both major and minor).
     *
     * @param show {@code true} to draw ticks on the Y axis
     * @return this theme, for fluent chaining
     */
    public DerivedTheme showYAxisTicks(boolean show) { this.showYAxisTicks = show; return this; }

    /**
     * Places the guides against the given side of the panel.
     *
     * @param position the side of the panel the guides move to
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guidePosition(GuidePosition position) { this.guidePosition = position; return this; }

    /**
     * Sets the gap between guides stacked in the same position.
     *
     * @param spacing the new gap between stacked guides
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideSpacing(double spacing) { this.guideSpacing = spacing; return this; }

    /**
     * Sets the guide box background color, or {@code null} for no box.
     *
     * @param color the new guide box background color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBoxColor(Color color) { this.guideBoxColor = color; return this; }

    /**
     * Sets the padding between the guide box edge and its guides.
     *
     * @param margin the new guide box padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBoxMargin(double margin) { this.guideBoxMargin = margin; return this; }

    /**
     * Sets how a side guide strip that is taller than the panel is handled.
     *
     * @param overflow the overflow policy to apply
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideOverflow(GuideOverflow overflow) { this.guideOverflow = overflow; return this; }

    /**
     * Sets the color of tick marks and rule lines drawn on bar-style guides.
     *
     * @param color the new guide tick mark color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTickColor(Color color) { this.guideTickColor = color; return this; }

    /**
     * Sets the color of the border drawn around the guide stack's background box.
     *
     * @param color the new guide box border color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBoxBorderColor(Color color) { this.guideBoxBorderColor = color; return this; }

    /**
     * Sets the base margin around the data panel that holds axis labels, tick
     * marks, and axis titles.
     *
     * @param zone the new base axis label zone
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisLabelZone(double zone) { this.axisLabelZone = zone; return this; }

    /**
     * Sets the outer x-label band a {@code PlotMatrix} reserves below its
     * bottom row.
     *
     * @param band the new matrix x-label band
     * @return this theme, for fluent chaining
     */
    public DerivedTheme matrixLabelBandX(double band) { this.matrixLabelBandX = band; return this; }

    /**
     * Sets the outer y-label band a {@code PlotMatrix} reserves left of its
     * first column.
     *
     * @param band the new matrix y-label band
     * @return this theme, for fluent chaining
     */
    public DerivedTheme matrixLabelBandY(double band) { this.matrixLabelBandY = band; return this; }

    /**
     * Sets the pivot for the rotated y axis titles a {@code PlotMatrix} draws
     * beside its first column.
     *
     * @param pivot the new matrix y-title pivot
     * @return this theme, for fluent chaining
     */
    public DerivedTheme matrixYTitlePivot(double pivot) { this.matrixYTitlePivot = pivot; return this; }

    /**
     * Sets the compact per-cell label zone {@code matrixPlot()} stamps on its
     * cells.
     *
     * @param zone the new matrix cell label zone
     * @return this theme, for fluent chaining
     */
    public DerivedTheme matrixCellLabelZone(double zone) { this.matrixCellLabelZone = zone; return this; }

    /**
     * Sets the band a composite figure reserves on top for its title and
     * subtitle.
     *
     * @param band the new composite title band
     * @return this theme, for fluent chaining
     */
    public DerivedTheme titleBand(double band) { this.titleBand = band; return this; }

    /**
     * Sets the corner offset for composed panel tag letters.
     *
     * @param offset the new panel tag offset
     * @return this theme, for fluent chaining
     */
    public DerivedTheme panelTagOffset(double offset) { this.panelTagOffset = offset; return this; }

    /**
     * Sets the breathing padding around guide strips and guides.
     *
     * @param padding the new guide strip padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideStripPadding(double padding) { this.guideStripPadding = padding; return this; }

    /**
     * Sets the gap between the data panel and the guide strip.
     *
     * @param gap the new panel-to-guide gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme panelGuideGap(double gap) { this.panelGuideGap = gap; return this; }

    /**
     * Sets the margin reserved on the right of a composite figure when no
     * guide occupies that edge.
     *
     * @param margin the new composite right margin
     * @return this theme, for fluent chaining
     */
    public DerivedTheme marginRight(double margin) { this.marginRight = margin; return this; }

    /**
     * Sets the distance of the main title below the top canvas edge.
     *
     * @param offset the new title offset
     * @return this theme, for fluent chaining
     */
    public DerivedTheme titleOffset(double offset) { this.titleOffset = offset; return this; }

    /**
     * Sets the distance of the X-axis title below the panel bottom edge.
     *
     * @param offset the new X-axis title offset
     * @return this theme, for fluent chaining
     */
    public DerivedTheme axisTitleOffset(double offset) { this.axisTitleOffset = offset; return this; }

    /**
     * Sets the height of facet strip headers.
     *
     * @param height the new facet strip height
     * @return this theme, for fluent chaining
     */
    public DerivedTheme facetStripHeight(double height) { this.facetStripHeight = height; return this; }

    /**
     * Sets the minimum width of an inside guide strip.
     *
     * @param width the new minimum inside strip width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideMinInsideWidth(double width) { this.guideMinInsideWidth = width; return this; }

    /**
     * Sets the length of tick marks drawn on bar-style guides.
     *
     * @param length the new guide tick mark length
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTickLength(double length) { this.guideTickLength = length; return this; }

    /**
     * Sets the height of the title row on vertical bar-style guides.
     *
     * @param height the new bar guide title row height
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTitleHeight(double height) { this.guideTitleHeight = height; return this; }

    /**
     * Sets the height of the title row on a vertical legend guide.
     *
     * @param height the new legend title row height
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideKeyTitleHeight(double height) { this.guideKeyTitleHeight = height; return this; }

    /**
     * Sets the vertical padding per key row of a sized legend.
     *
     * @param pad the new sized key padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideSizedKeyPad(double pad) { this.guideSizedKeyPad = pad; return this; }

    /**
     * Sets the gap between stacked rows of a bar-style guide.
     *
     * @param gap the new bar row gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBarRowGap(double gap) { this.guideBarRowGap = gap; return this; }

    /**
     * Sets the tick/label space cleared under a vertical bar guide.
     *
     * @param space the new vertical bar tick/label space
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBarTickSpace(double space) { this.guideBarTickSpace = space; return this; }

    /**
     * Sets the tick/label space cleared beside a horizontal bar guide.
     *
     * @param space the new horizontal bar tick/label space
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBarTickSpaceHorizontal(double space) { this.guideBarTickSpaceHorizontal = space; return this; }

    /**
     * Sets the extra horizontal padding beside bar tick labels.
     *
     * @param pad the new bar label padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBarLabelPad(double pad) { this.guideBarLabelPad = pad; return this; }

    /**
     * Sets the gap between a bar guide's title and its first tick label.
     *
     * @param gap the new title-to-tick gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTitleTickGap(double gap) { this.guideTitleTickGap = gap; return this; }

    /**
     * Sets the gap between adjacent side-by-side horizontal bar segments.
     *
     * @param gap the new bar segment gap
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideBarGap(double gap) { this.guideBarGap = gap; return this; }

    /**
     * Sets the padding between a bar or label and the reserved boundary when
     * measuring guide width.
     *
     * @param pad the new content padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideContentPad(double pad) { this.guideContentPad = pad; return this; }

    /**
     * Sets the floor for a horizontal bar's content-driven natural width.
     *
     * @param width the new minimum natural width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideMinNaturalWidth(double width) { this.guideMinNaturalWidth = width; return this; }

    /**
     * Sets the minimum wrap width for a legend title line.
     *
     * @param width the new minimum title line width
     * @return this theme, for fluent chaining
     */
    public DerivedTheme legendMaxTitleLineWidth(double width) { this.legendMaxTitleLineWidth = width; return this; }

    /**
     * Sets the extra width padding around legend title text when measuring.
     *
     * @param pad the new title text padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme legendTitleTextPad(double pad) { this.legendTitleTextPad = pad; return this; }

    /**
     * Sets the extra width padding beside a horizontal legend's side title.
     *
     * @param pad the new side title padding
     * @return this theme, for fluent chaining
     */
    public DerivedTheme legendTitleSidePad(double pad) { this.legendTitleSidePad = pad; return this; }

    /**
     * Sets the vertical centring offset applied to legend titles and key labels.
     *
     * @param offset the new title offset
     * @return this theme, for fluent chaining
     */
    public DerivedTheme legendTitleOffsetY(double offset) { this.legendTitleOffsetY = offset; return this; }

    /**
     * Sets the base font size in points, rescaling every text element of the
     * chart proportionally while preserving their relative hierarchy. Fonts
     * that were individually overridden via their dedicated hooks (e.g.
     * {@link #titleFont(Font)}) are recomputed from the new base size, so
     * re-apply any per-element overrides afterwards.
     *
     * @param size the new base font size in points
     * @return this theme, for fluent chaining
     */
    public DerivedTheme baseFontSize(double size) {
        this.baseFontSize = size;
        recomputeDerivedFonts();
        return this;
    }

    /**
     * Sets the multiplicative font scale factor applied on top of
     * {@link #baseFontSize()}, rescaling the entire chart. As with
     * {@link #baseFontSize(double)}, derived fonts are recomputed from the
     * current base size and scale.
     *
     * @param scale the new font scale factor
     * @return this theme, for fluent chaining
     */
    public DerivedTheme fontScale(double scale) {
        this.fontScale = scale;
        recomputeDerivedFonts();
        return this;
    }

    /**
     * Overrides the guide title font (legend or colorbar).
     *
     * @param f the new guide title font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTitleFont(Font f) { this.guideTitleFont = f; return this; }

    /**
     * Overrides the guide key font (legend keys and colorbar tick labels).
     *
     * @param f the new guide key font
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideKeyFont(Font f) { this.guideKeyFont = f; return this; }

    /**
     * Overrides the guide title color (legend or colorbar).
     *
     * @param c the new guide title color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideTitleColor(Color c) { this.guideTitleColor = c; return this; }

    /**
     * Overrides the guide frame border color (colorbar and alpha bars).
     *
     * @param c the new guide frame color
     * @return this theme, for fluent chaining
     */
    public DerivedTheme guideFrameColor(Color c) { this.guideFrameColor = c; return this; }

    /**
     * Overrides the relative inside anchor for guides placed at
     * {@link GuidePosition#INSIDE}: the point (x, y) in [0, 1] of the panel
     * the guide strip is centered on.
     *
     * @param anchor the new relative inside anchor
     * @return this theme, for fluent chaining
     */
    public DerivedTheme legendInsideAnchor(Anchor anchor) { this.legendInsideAnchor = anchor; return this; }

    /**
     * Rebuilds the fonts and spacing derived from {@link #baseFontSize} and
     * {@link #fontScale}, mirroring the derivation the {@link Theme} interface
     * performs. Called whenever either of the two base values changes.
     */
    private void recomputeDerivedFonts() {
        double size = baseFontSize * fontScale;
        titleFont = Font.font("System", FontWeight.BOLD, size * (7.0 / 6.0));
        subtitleFont = Font.font("System", FontWeight.NORMAL, size * 1.0);
        captionFont = Font.font("System", FontWeight.NORMAL, size * (3.0 / 4.0));
        axisTitleFont = Font.font("System", FontWeight.NORMAL, size * 1.0);
        tickLabelFont = Font.font("System", FontWeight.NORMAL, size * (3.0 / 4.0));
        stripFont = Font.font("System", FontWeight.BOLD, size * (5.0 / 6.0));
        guideTitleFont = Font.font("System", FontWeight.BOLD, size * (11.0 / 12.0));
        guideKeyFont = Font.font("System", FontWeight.NORMAL, size * (5.0 / 6.0));
        fontSpacing = baseFontSize + 2.0;
    }
}
