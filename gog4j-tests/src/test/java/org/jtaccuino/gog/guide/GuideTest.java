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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.DiscreteColorScale;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.jtaccuino.gog.theme.Anchor;
import org.jtaccuino.gog.theme.GuidePosition;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JavaFxToolkitExtension.class)
class GuideTest {

    @Test
    void colorbarFactoryDefaultsMatchGgplot2() {
        var bar = Guides.guideColorbar();
        assertEquals(Guide.Direction.VERTICAL, bar.direction());
        assertFalse(bar.reverse());
        assertEquals(20.0, bar.barWidth());
        assertEquals(180.0, bar.barHeight());
        assertEquals(100, bar.nbin());
        assertTrue(bar.ticks());
        assertNull(bar.ticksColor());
        assertEquals(0.5, bar.ticksLineWidth());
        assertNull(bar.frameColor());
        assertTrue(bar.drawUlim());
        assertTrue(bar.drawLlim());
        assertEquals(GuideColorbar.LabelPosition.RIGHT, bar.labelPosition());
        assertNull(bar.title());
    }

    @Test
    void horizontalColorbarDefaultsLabelsToTop() {
        var bar = Guides.guideColorbar().direction(Guide.Direction.HORIZONTAL);
        assertEquals(GuideColorbar.LabelPosition.TOP, bar.labelPosition());
    }

    @Test
    void colorbarWithMethodOverrides() {
        var bar = Guides.guideColorbar()
                .title("density")
                .reverse(true)
                .barWidth(12.0)
                .barHeight(90.0)
                .nbin(50)
                .ticks(false)
                .drawUlim(false)
                .drawLlim(false)
                .labelPosition(GuideColorbar.LabelPosition.LEFT);
        assertEquals("density", bar.title());
        assertTrue(bar.reverse());
        assertEquals(12.0, bar.barWidth());
        assertEquals(90.0, bar.barHeight());
        assertEquals(50, bar.nbin());
        assertFalse(bar.ticks());
        assertFalse(bar.drawUlim());
        assertFalse(bar.drawLlim());
        assertEquals(GuideColorbar.LabelPosition.LEFT, bar.labelPosition());
    }

    @Test
    void legendFactoryDefaults() {
        var legend = Guides.guideLegend();
        assertEquals(Guide.Direction.VERTICAL, legend.direction());
        assertFalse(legend.reverse());
        assertEquals(14.0, legend.keyWidth());
        assertEquals(20.0, legend.rowHeight());
        assertEquals(0, legend.nrow());
        assertEquals(0, legend.ncol());
        assertFalse(legend.byrow());
        assertEquals(8.0, legend.keySpacingX());
        assertNull(legend.title());
    }

    @Test
    void legendGridRespectsNcol() throws Exception {
        var legend = Guides.guideLegend().ncol(2).title("cyl").withMetrics(defaultMetrics());
        var data = fourKeys();
        assertEquals(22.0 + 2 * 20.0, onFxThread(() -> legend.measure(data, 130.0)));
    }

    @Test
    void legendGridRespectsNrow() throws Exception {
        var legend = Guides.guideLegend().nrow(2).title("cyl").withMetrics(defaultMetrics());
        var data = new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null),
                        new GuideLegend.Key("6", "6", Color.BLUE, null),
                        new GuideLegend.Key("8", "8", Color.GREEN, null),
                        new GuideLegend.Key("10", "10", Color.YELLOW, null),
                        new GuideLegend.Key("12", "12", Color.ORANGE, null)),
                false, false, null, false, false);
        assertEquals(22.0 + 2 * 20.0, onFxThread(() -> legend.measure(data, 130.0)));
    }

    @Test
    void horizontalLegendLaysOutSingleRow() throws Exception {
        var labelFont = Font.font("System", 12.0);
        var legend = Guides.guideLegend().direction(Guide.Direction.HORIZONTAL).title("cyl")
                .withLabelFont(labelFont).withTitleFont(labelFont).withMetrics(defaultMetrics());
        var data = fourKeys();
        // The side title adds no height and its width leads the key row
        // (the left-of-keys default for horizontal legends).
        assertEquals(20.0, onFxThread(() -> legend.measure(data, 1000.0)));
        double sideTitle = onFxThread(() -> TextMeasurer.width("cyl", labelFont) + 8.0);
        double keys = 0.0;
        for (String label : List.of("4", "6", "8", "G")) {
            keys += 14.0 + 8.0 + onFxThread(() -> TextMeasurer.width(label, labelFont));
        }
        assertEquals(sideTitle + keys, onFxThread(() -> legend.preferredWidth(data)));
    }

    @Test
    void verticalPreferredWidthIsWidestColumn() throws Exception {
        var labelFont = Font.font("System", 12.0);
        var legend = Guides.guideLegend().withLabelFont(labelFont).withMetrics(defaultMetrics());
        var data = fourKeys();
        double widest = 0.0;
        for (String l : List.of("4", "6", "8", "G")) {
            widest = Math.max(widest, onFxThread(() -> TextMeasurer.width(l, labelFont)));
        }
        assertEquals(14.0 + 8.0 + widest, onFxThread(() -> legend.preferredWidth(data)));
    }

    @Test
    void ncolGridPreferredWidthIsSumOfColumns() throws Exception {
        var labelFont = Font.font("System", 12.0);
        var legend = Guides.guideLegend().ncol(2).withLabelFont(labelFont).withMetrics(defaultMetrics());
        var data = new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null),
                        new GuideLegend.Key("4", "4", Color.BLUE, null),
                        new GuideLegend.Key("4", "4", Color.GREEN, null),
                        new GuideLegend.Key("4", "4", Color.YELLOW, null)),
                false, false, null, false, false);
        double label = onFxThread(() -> TextMeasurer.width("4", labelFont));
        assertEquals(2.0 * (14.0 + 8.0 + label), onFxThread(() -> legend.preferredWidth(data)));
    }

    @Test
    void reverseKeepsLayoutMetrics() throws Exception {
        var base = Guides.guideLegend().title("cyl");
        var data = fourKeys();
        var legend = base.withMetrics(defaultMetrics());
        var reversed = base.reverse(true).withMetrics(defaultMetrics());
        assertEquals(onFxThread(() -> legend.measure(data, 130.0)),
                onFxThread(() -> reversed.measure(data, 130.0)));
        assertEquals(onFxThread(() -> legend.preferredWidth(data)),
                onFxThread(() -> reversed.preferredWidth(data)));
    }

    private static GuideLegend.Data fourKeys() {
        return new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null),
                        new GuideLegend.Key("6", "6", Color.BLUE, null),
                        new GuideLegend.Key("8", "8", Color.GREEN, null),
                        new GuideLegend.Key("G", "G", Color.YELLOW, null)),
                false, false, null, false, false);
    }

    /** The theme-default guide metrics, matching the historical layout constants. */
    private static Guide.GuideMetrics defaultMetrics() {
        return new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                4.0, 12.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 80.0, 4.0, 8.0, 3.0);
    }

    @Test
    void legendMeasureCountsTitleAndRows() throws Exception {
        var legend = Guides.guideLegend().title("cyl").withMetrics(defaultMetrics());
        var data = new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null),
                        new GuideLegend.Key("6", "6", Color.BLUE, null)),
                false, false, null, false, false);
        assertEquals(22.0 + 2 * 20.0, onFxThread(() -> legend.measure(data, 130.0)));
    }

    @Test
    void legendMeasureWithoutTitleIsRowsOnly() throws Exception {
        var legend = Guides.guideLegend().withMetrics(defaultMetrics());
        var data = new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("8", "8", Color.GREEN, null)),
                false, false, null, false, false);
        assertEquals(20.0, onFxThread(() -> legend.measure(data, 130.0)));
    }

    @Test
    void legendTitleWrapMeasuresInThemedTitleFont() throws Exception {
        // A larger themed title font wraps a long, space-separated title into
        // more, shorter lines. Wrap is measured in the supplied title font, so
        // the measured heights differ by font size (regression: formerly
        // measured in a fixed 11px font regardless of the theme).
        var big = Font.font("System", FontWeight.BOLD, 30.0);
        var small = Font.font("System", FontWeight.BOLD, 11.0);
        var title = String.join(" ", Collections.nCopies(40, "abc"));
        var data = new GuideLegend.Data("A",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null)),
                false, false, null, false, false);
        var withBig = Guides.guideLegend().title(title).withTitleFont(big).withMetrics(defaultMetrics());
        var withSmall = Guides.guideLegend().title(title).withTitleFont(small).withMetrics(defaultMetrics());
        assertTrue(onFxThread(() -> withBig.measure(data, 130.0)) > onFxThread(() -> withSmall.measure(data, 130.0)));
    }

    @Test
    void colorbarMeasureCountsTitleAndBar() {
        var bar = Guides.guideColorbar().title("wt").barHeight(120.0).withMetrics(defaultMetrics());
        assertEquals(16.0 + 120.0 + 12.0, bar.measure(null, 130.0));
    }

    @Test
    void noneMeasuresNothing() {
        assertEquals(0.0, Guide.NONE.measure(null, 130.0));
    }

    @Test
    void titleOnBaseTypeReturnsTitledGuide() {
        Guide<?> guide = Guides.guideLegend();
        var titled = guide.title("cyl");
        assertEquals("cyl", titled.title());
    }

    @Test
    void noneIgnoresTitle() {
        assertSame(Guide.NONE, Guide.NONE.title("ignored"));
    }

    @Test
    void defaultForContinuousIsColorbar() {
        assertInstanceOf(GuideColorbar.class, Guides.defaultFor(true));
    }

    @Test
    void defaultForDiscreteIsLegend() {
        assertInstanceOf(GuideLegend.class, Guides.defaultFor(false));
    }

    @Test
    void colorbar3dFactoryShadesTheBar() {
        var bar = Guides.guideColorbar3d();
        assertEquals(GuideShading.DEFAULT, bar.shading());
        assertEquals(20.0, bar.barWidth());
        assertTrue(bar.ticks());

        var mid = Guides.guideColorbar3d(new MinMax(-0.5, 0.5));
        assertEquals(new MinMax(-0.5, 0.5), mid.shading().range());
        assertFalse(mid.shading().reverse());

        var reversed = Guides.guideColorbar3d(new MinMax(-1.0, 1.0), true);
        assertTrue(reversed.shading().reverse());
    }

    @Test
    void legend3dFactoryShadesEveryKey() {
        var legend = Guides.guideLegend3d();
        assertEquals(GuideShading.DEFAULT, legend.shading());
        assertEquals(GuideShading.DEFAULT.keyShade(), GuideShading.DEFAULT.keyShade());
        assertTrue(legend.keyWidth() > 0);
    }

    @Test
    void shadingClearsAndCopiesThroughSetters() {
        var bar = Guides.guideColorbar3d();
        assertNull(bar.shade(null).shading());
        var kept = bar.barWidth(30.0);
        assertEquals(GuideShading.DEFAULT, kept.shading(), "copies preserve the shading");

        var legend = Guides.guideLegend3d();
        assertNull(legend.shade(null).shading());
        assertEquals(GuideShading.DEFAULT, legend.title("cyl").shading());
    }

    @Test
    void guideShadingMapsBarPositionToShadeFactor() {
        var shading = GuideShading.DEFAULT;
        assertEquals(-1.0, shading.shadeAt(0.0), 1e-9);
        assertEquals(0.0, shading.shadeAt(0.5), 1e-9);
        assertEquals(1.0, shading.shadeAt(1.0), 1e-9);

        var reversed = new GuideShading(new MinMax(-1.0, 1.0), true, false);
        assertEquals(1.0, reversed.shadeAt(0.0), 1e-9);
        assertEquals(-1.0, reversed.shadeAt(1.0), 1e-9);

        assertEquals(new MinMax(-1.0, 1.0), new GuideShading(null, false, false).range(),
                "a null range falls back to the default [-1, 1]");
        assertEquals(1.0, GuideShading.DEFAULT.keyShade(), 1e-9);
    }

    @Test
    void styleFromThemeUsesThemedConstants() {
        var theme = Theme.theme_gray();
        var style = GuideStyle.from(theme);
        assertEquals(theme.textColor(), style.textColor());
        assertEquals(theme.guideTitleColor(), style.titleColor());
        assertEquals(theme.guideTitleFont(), style.titleFont());
        assertEquals(theme.guideKeyFont(), style.keyFont());
        assertEquals(theme.guideFrameColor(), style.frameColor());
    }

    @Test
    void themeGuideDefaultsMatchHistoricalRendering() {
        var theme = Theme.theme_gray();
        assertEquals(GuidePosition.RIGHT, theme.guidePosition());
        assertEquals(10.0, theme.guideSpacing());
        assertNull(theme.guideBoxColor());
        assertEquals(4.0, theme.guideBoxMargin());
    }

    @Test
    void derivedThemeCanRepositionGuides() {
        var theme = Theme.derive(Theme.theme_gray())
                .guidePosition(GuidePosition.TOP)
                .guideSpacing(6.0)
                .guideBoxColor(Color.WHITE)
                .guideBoxMargin(8.0);
        assertEquals(GuidePosition.TOP, theme.guidePosition());
        assertEquals(6.0, theme.guideSpacing());
        assertEquals(Color.WHITE, theme.guideBoxColor());
        assertEquals(8.0, theme.guideBoxMargin());
    }

    @Test
    void derivedThemeCanThemeGuideSpacing() {
        var theme = Theme.derive(Theme.theme_gray())
                .guideBarRowGap(2.0)
                .guideBarTickSpace(8.0)
                .guideBarTickSpaceHorizontal(18.0)
                .guideBarLabelPad(12.0)
                .guideTitleTickGap(6.0);
        assertEquals(2.0, theme.guideBarRowGap());
        assertEquals(8.0, theme.guideBarTickSpace());
        assertEquals(18.0, theme.guideBarTickSpaceHorizontal());
        assertEquals(12.0, theme.guideBarLabelPad());
        assertEquals(6.0, theme.guideTitleTickGap());
        assertEquals(4.0, Theme.theme_gray().guideBarRowGap());
    }

    @Test
    void guideSpacingMetricsDriveBarMeasurement() {
        // Thematic bar spacing is folded into GuideMetrics; a vertical colourbar
        // reserves the themed title row plus the themed tick space below the bar.
        var defaultMetrics = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                4.0, 12.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 80.0, 4.0, 8.0, 3.0);
        var tightMetrics = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                2.0, 8.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 80.0, 4.0, 8.0, 3.0);
        var bar = Guides.guideColorbar().title("wt").barHeight(120.0);
        assertEquals(16.0 + 120.0 + 12.0,
                bar.withMetrics(defaultMetrics).measure(null, 130.0));
        assertEquals(16.0 + 120.0 + 8.0,
                bar.withMetrics(tightMetrics).measure(null, 130.0));
    }

    @Test
    void stackedAlphaRowsCountThemedRowGapAndTickSpace() {
        var metrics = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                2.0, 8.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 80.0, 4.0, 8.0, 3.0);
        var alpha = Guides.guideAlpha();
        var data = new GuideAlpha.Data("alpha", List.of(
                new GuideAlpha.AlphaEntry(Color.RED, new MinMax(0.0, 1.0)),
                new GuideAlpha.AlphaEntry(Color.BLUE, new MinMax(0.0, 1.0))),
                List.of(0.0, 1.0), List.of("0", "1"));
        double measured = alpha.withMetrics(metrics).measure(data, 130.0);
        // Two rows: each barExtentY (== barHeight for a vertical bar) + tickSpace,
        // plus one row gap between them.
        assertEquals(2 * (alpha.barHeight() + 8.0) + 2.0, measured);
    }

    @Test
    void derivedThemeCanThemeGuideTypographyAndColors() {
        var titleFont = Font.font("System", 15.0);
        var keyFont = Font.font("System", 13.0);
        var theme = Theme.derive(Theme.theme_gray())
                .guideTitleFont(titleFont)
                .guideKeyFont(keyFont)
                .guideTitleColor(Color.RED)
                .guideFrameColor(Color.GREEN);
        assertEquals(titleFont, theme.guideTitleFont());
        assertEquals(keyFont, theme.guideKeyFont());
        assertEquals(Color.RED, theme.guideTitleColor());
        assertEquals(Color.GREEN, theme.guideFrameColor());
        var style = GuideStyle.from(theme);
        assertEquals(Color.RED, style.titleColor());
        assertEquals(titleFont, style.titleFont());
        assertEquals(keyFont, style.keyFont());
        assertEquals(Color.GREEN, style.frameColor());
    }

    @Test
    void derivedThemeCanSetInsideAnchor() {
        var theme = Theme.derive(Theme.theme_gray())
                .legendInsideAnchor(new Anchor(0.25, 0.75));
        assertEquals(new Anchor(0.25, 0.75), theme.legendInsideAnchor());
    }

    @Test
    void derivedThemeBaseFontSizeRescalesDerivedFonts() {
        var theme = Theme.derive(Theme.theme_gray()).baseFontSize(18.0);
        assertEquals(18.0, theme.baseFontSize());
        assertEquals(18.0 + 2.0, theme.fontSpacing());
        assertEquals(18.0 * (7.0 / 6.0), theme.titleFont().getSize(), 1e-9);
        assertEquals(18.0, theme.axisTitleFont().getSize(), 1e-9);
        assertEquals(18.0 * (3.0 / 4.0), theme.tickLabelFont().getSize(), 1e-9);
        assertEquals(18.0 * (5.0 / 6.0), theme.stripFont().getSize(), 1e-9);
        assertEquals(18.0 * (11.0 / 12.0), theme.guideTitleFont().getSize(), 1e-9);
        assertEquals(18.0 * (5.0 / 6.0), theme.guideKeyFont().getSize(), 1e-9);
    }

    @Test
    void derivedThemeFontScaleRescalesDerivedFonts() {
        var theme = Theme.derive(Theme.theme_gray()).fontScale(2.0);
        assertEquals(2.0, theme.fontScale());
        assertEquals(12.0 * 2.0 * (7.0 / 6.0), theme.titleFont().getSize(), 1e-9);
        assertEquals(12.0 * 2.0, theme.axisTitleFont().getSize(), 1e-9);
        assertEquals(12.0 + 2.0, theme.fontSpacing());
    }

    @Test
    void derivedThemeBaseFontSizeRecomputesIndividualFontOverrides() {
        var customTitle = Font.font("System", FontWeight.BOLD, 30.0);
        var theme = Theme.derive(Theme.theme_gray()).titleFont(customTitle).baseFontSize(18.0);
        assertNotEquals(customTitle, theme.titleFont());
        assertEquals(18.0 * (7.0 / 6.0), theme.titleFont().getSize(), 1e-9);
    }

    @Test
    void guidesRegistryResolvesConfiguredAndSuppressedAesthetics() {
        var legend = Guides.guideLegend();
        var guides = Guides.empty()
                .set(Aesthetic.COLOR, legend)
                .suppress(Aesthetic.FILL);

        assertTrue(guides.isConfigured(Aesthetic.COLOR));
        assertSame(legend, guides.forAesthetic(Aesthetic.COLOR));
        assertTrue(guides.isSuppressed(Aesthetic.FILL));
        assertSame(Guide.NONE, guides.forAesthetic(Aesthetic.FILL));
        assertSame(Guide.NONE, guides.forAesthetic(Aesthetic.SHAPE));
        assertFalse(guides.isConfigured(Aesthetic.SHAPE));
        assertEquals(2, guides.configured().size());
    }

    @Test
    void mappingsMergeIntoRegistryWithLaterOnesWinning() {
        var first = Guides.guideLegend();
        var second = Guides.guideLegend();
        var colorbar = Guides.guideColorbar();
        var guides = Guides.empty().set(
                Guides.guide(Aesthetic.COLOR, first),
                Guides.guide(Aesthetic.SIZE, second),
                Guides.guide(Aesthetic.COLOR, colorbar));

        assertSame(colorbar, guides.forAesthetic(Aesthetic.COLOR));
        assertSame(second, guides.forAesthetic(Aesthetic.SIZE));
        assertEquals(2, guides.configured().size());
    }

    @Test
    void guideMappingRejectsNullParts() {
        assertThrows(NullPointerException.class, () -> Guides.guide(Aesthetic.COLOR, null));
        assertThrows(NullPointerException.class, () -> Guides.guide(null, Guides.guideLegend()));
    }

    @Test
    void noneSuppressesEveryAesthetic() {
        var guides = Guides.none();

        assertTrue(guides.isAllSuppressed());
        assertTrue(guides.isSuppressed(Aesthetic.COLOR));
        assertTrue(guides.isSuppressed(Aesthetic.FILL));
        assertSame(Guide.NONE, guides.forAesthetic(Aesthetic.COLOR));
        assertTrue(guides.configured().isEmpty());
    }

    @Test
    void suppressAllKeepsMappingsButHidesThem() {
        var legend = Guides.guideLegend();
        var guides = Guides.empty().set(Aesthetic.COLOR, legend).suppressAll();

        assertTrue(guides.isAllSuppressed());
        assertTrue(guides.isSuppressed(Aesthetic.COLOR));
        assertSame(Guide.NONE, guides.forAesthetic(Aesthetic.COLOR));
        assertEquals(1, guides.configured().size());
        assertSame(legend, guides.configured().get(Aesthetic.COLOR));
    }

    @Test
    void validateRejectsColorbarOnDiscreteScale() {
        var df = DataFrame.byColumn("g").of(Series.of("a", "b"));
        var discrete = DiscreteColorScale.forColumn(df, new DflibDataExtractor(), "g", null, null,
                Theme.theme_gray().categoricalPalette(), Theme.theme_gray().fallbackColor());
        assertThrows(IllegalArgumentException.class,
                () -> Guides.validate(Guides.guideColorbar(), discrete, Aesthetic.COLOR));
    }

    @Test
    void validateRejectsLegendOnContinuousScale() {
        var continuous = ContinuousColorScale.forRange("wt", List.of(1.0, 2.0), null);
        assertThrows(IllegalArgumentException.class,
                () -> Guides.validate(Guides.guideLegend(), continuous, Aesthetic.COLOR));
    }

    @Test
    void validateAcceptsMatchingGuides() {
        var continuous = ContinuousColorScale.forRange("wt", List.of(1.0, 2.0), null);
        var df = DataFrame.byColumn("g").of(Series.of("a", "b"));
        var discrete = DiscreteColorScale.forColumn(df, new DflibDataExtractor(), "g", null, null,
                Theme.theme_gray().categoricalPalette(), Theme.theme_gray().fallbackColor());
        Guides.validate(Guides.guideColorbar(), continuous, Aesthetic.COLOR);
        Guides.validate(Guides.guideLegend(), discrete, Aesthetic.COLOR);
    }

    @Test
    void positionOverrideIsImmutableAndDefaultsToNull() {
        var legend = Guides.guideLegend();
        assertNull(legend.position());

        var positioned = legend.position(GuidePosition.BOTTOM);
        assertSame(GuidePosition.BOTTOM, positioned.position());
        assertNull(legend.position(), "the original guide keeps its theme inheritance");
        assertEquals("cyl", positioned.title("cyl").title());
        assertSame(GuidePosition.BOTTOM, positioned.title("cyl").position(),
                "copies preserve the position override");

        var bar = Guides.guideColorbar().position(GuidePosition.INSIDE);
        assertSame(GuidePosition.INSIDE, bar.position());
    }

    @Test
    void positionOnHorizontalStripFlowsHorizontally() {
        var legend = Guides.guideLegend();
        assertEquals(Guide.Direction.VERTICAL, legend.direction());
        assertEquals(Guide.Direction.VERTICAL, legend.effectiveDirection());

        var bottom = legend.position(GuidePosition.BOTTOM);
        assertEquals(Guide.Direction.VERTICAL, bottom.direction(), "the configured direction is kept");
        assertEquals(Guide.Direction.HORIZONTAL, bottom.effectiveDirection(),
                "a vertical guide on a horizontal strip flows horizontally");

        var topBar = Guides.guideColorbar().position(GuidePosition.TOP);
        assertEquals(Guide.Direction.HORIZONTAL, topBar.effectiveDirection());

        var left = Guides.guideLegend().direction(Guide.Direction.HORIZONTAL).position(GuidePosition.LEFT);
        assertEquals(Guide.Direction.HORIZONTAL, left.effectiveDirection(),
                "an explicit direction on a vertical strip is kept");
    }

    @Test
    void labsPerAestheticTitlesFallBackToGlobal() {
        var labs = new LabsSpec()
                .legendTitle("Global")
                .legendTitle(Aesthetic.COLOR, "Drive type");

        assertEquals("Drive type", labs.legendTitle(Aesthetic.COLOR));
        assertNull(labs.legendTitle(Aesthetic.FILL));
        assertEquals("Global", labs.legendTitle());
    }

    @Test
    void alphaNbinAndBarStepArePerGeomAndImmutable() {
        var alpha = Guides.guideAlpha();
        assertEquals(100, alpha.nbin());
        assertEquals(64.0, alpha.barStep(), 1e-9);

        var resampled = alpha.nbin(64).barStep(24.0);
        assertEquals(64, resampled.nbin());
        assertEquals(24.0, resampled.barStep(), 1e-9);
        assertEquals(100, alpha.nbin(), "the original guide keeps its per-geom config");
        assertEquals(64.0, alpha.barStep(), 1e-9);

        var copied = resampled.title("alpha");
        assertEquals(64, copied.nbin(), "copies preserve the per-geom config");
        assertEquals(24.0, copied.barStep(), 1e-9);
    }

    @Test
    void alphaBarStepAndThemedBarGapDriveHorizontalPreferredWidth() throws Exception {
        var data = new GuideAlpha.Data("alpha",
                List.of(new GuideAlpha.AlphaEntry(Color.RED, new MinMax(0.0, 1.0)),
                        new GuideAlpha.AlphaEntry(Color.BLUE, new MinMax(0.0, 1.0))),
                List.of(0.0, 1.0), List.of("0", "1"));
        var base = Guides.guideAlpha().direction(Guide.Direction.HORIZONTAL).barStep(100.0);
        var wideGap = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                4.0, 12.0, 22.0, 16.0, 10.0, 30.0, 8.0, 40.0, 80.0, 4.0, 8.0, 3.0);

        assertEquals(2 * 100.0 + 20.0, onFxThread(() -> base.withMetrics(defaultMetrics()).preferredWidth(data)), 1e-9);
        assertEquals(2 * 100.0 + 30.0, onFxThread(() -> base.withMetrics(wideGap).preferredWidth(data)), 1e-9);
        assertEquals(2 * 50.0 + 20.0,
                onFxThread(() -> base.barStep(50.0).withMetrics(defaultMetrics()).preferredWidth(data)), 1e-9);
    }

    @Test
    void themedLegendWrapMetricsAffectTitleWrap() throws Exception {
        var title = "A fairly long legend title";
        var data = new GuideLegend.Data("cyl",
                List.of(new GuideLegend.Key("4", "4", Color.RED, null)),
                false, false, null, false, false);
        var narrow = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                4.0, 12.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 40.0, 4.0, 8.0, 3.0);
        var wide = new Guide.GuideMetrics(16.0, 22.0, 6.0, 4.0,
                4.0, 12.0, 22.0, 16.0, 10.0, 20.0, 8.0, 40.0, 200.0, 4.0, 8.0, 3.0);

        var narrowLegend = Guides.guideLegend().title(title).withMetrics(narrow);
        var wideLegend = Guides.guideLegend().title(title).withMetrics(wide);
        assertTrue(onFxThread(() -> narrowLegend.measure(data, 130.0)) > onFxThread(() -> wideLegend.measure(data, 130.0)),
                "a narrower max title line width wraps the title into more rows");
    }
}
