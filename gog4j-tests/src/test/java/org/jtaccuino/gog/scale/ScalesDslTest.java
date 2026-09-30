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
package org.jtaccuino.gog.scale;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the scale DSL added in the scales sprint: reversed axes, axis
 * limits, named colour ramps (viridis/brewer) and gradients, manual
 * shape/linetype/size maps, and binned continuous colour scales. Each test
 * configures a minimal plot and asserts on the resulting {@link ScaleSpec}.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class ScalesDslTest {

    private static DataFrame data() {
        return DataFrame.byColumn("x", "y")
                .of(Series.ofInt(1, 2, 3), Series.ofInt(4, 5, 6));
    }

    private static Plot<DataFrame> plot() {
        return ggplot(data(), aes().x("x").y("y")).geoms(point());
    }

    @Test
    void reverseFlipsTheAxisDirection() {
        assertTrue(plot().scales(Scales.scaleXReverse()).getScaleSpec().isXReverse());
        assertTrue(plot().scales(Scales.scaleYReverse()).getScaleSpec().isYReverse());
        assertFalse(plot().getScaleSpec().isXReverse());
    }

    @Test
    void limitsClampTheAxisDomain() {
        var xSpec = plot().scales(Scales.scaleXLimits(0.0, 10.0)).getScaleSpec();
        assertEquals(new MinMax(0.0, 10.0), xSpec.getXLimits());
        var ySpec = plot().scales(Scales.scaleYLimits(5.0, 50.0)).getScaleSpec();
        assertEquals(new MinMax(5.0, 50.0), ySpec.getYLimits());
        assertNull(plot().getScaleSpec().getXLimits());
    }

    @Test
    void zLimitsClampTheZDomain() {
        var spec = plot().scales(Scales.zlim(0.0, 10.0)).getScaleSpec();
        assertEquals(new MinMax(0.0, 10.0), spec.getZLimits());
        assertNull(plot().getScaleSpec().getZLimits());
    }

    @Test
    void zContinuousRecordsBreaksLabelsAndExpansion() {
        var breaks = plot().scales(Scales.scaleZContinuous(List.of(0.0, 5.0, 10.0))).getScaleSpec();
        assertTrue(breaks.hasZBreaks());
        assertEquals(List.of(0.0, 5.0, 10.0), breaks.getZBreaks());

        var labelled = plot().scales(Scales.scaleZContinuous(
                List.of(0.0, 5.0), List.of("low", "high"))).getScaleSpec();
        assertEquals("low", labelled.zLabelAt(0));
        assertEquals("high", labelled.zLabelAt(1));

        var expand = plot().scales(Scales.scaleZContinuous(Expansion.none())).getScaleSpec();
        assertEquals(Expansion.none(), expand.getZExpand());
    }

    @Test
    void zDiscreteRecordsCategoryBreaks() {
        var spec = plot().scales(Scales.scaleZDiscrete(List.of(0.0, 1.0, 2.0))).getScaleSpec();
        assertTrue(spec.hasZBreaks());
        assertEquals(List.of(0.0, 1.0, 2.0), spec.getZBreaks());
        assertNull(spec.getZLabels());
    }

    @Test
    void mismatchedZBreaksAndLabelsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Scales.scaleZContinuous(List.of(0.0, 5.0), List.of("only-one")));
    }

    @Test
    void viridisContinuousSelectsRampAndClearsGradient() {
        var spec = plot().scales(Scales.scaleColorViridisC()).getScaleSpec();
        assertEquals("viridis", spec.getContinuousColorMap());
        assertNull(spec.getContinuousGradient());
    }

    @Test
    void gradientConfiguresColourStops() {
        var spec = plot().scales(Scales.scaleColorGradient(Color.WHITE, Color.BLACK)).getScaleSpec();
        assertEquals(2, spec.getContinuousGradient().size());
        var three = plot().scales(Scales.scaleColorGradient2(Color.RED, Color.WHITE, Color.BLUE)).getScaleSpec();
        assertEquals(3, three.getContinuousGradient().size());
    }

    @Test
    void brewerSelectsNamedPalette() {
        var spec = plot().scales(Scales.scaleColorBrewer("Dark2")).getScaleSpec();
        assertEquals("Dark2", spec.getContinuousColorMap());
        assertEquals("viridis", plot().scales(Scales.scaleColorViridisD()).getScaleSpec().getContinuousColorMap());
    }

    @Test
    void binnedRecordsStepCount() {
        var spec = plot().scales(Scales.scaleColorBinned(6)).getScaleSpec();
        assertTrue(spec.isBinned());
        assertEquals(6, spec.getColorSteps());
        var unbinned = plot().getScaleSpec();
        assertFalse(unbinned.isBinned());
        assertNull(unbinned.getColorSteps());
    }

    @Test
    void manualShapeMapResolvesCategories() {
        var spec = plot().scales(Scales.scaleShapeManual()
                .shape("a", PointShape.CIRCLE)
                .shape("b", PointShape.TRIANGLE)).getScaleSpec();
        assertEquals(PointShape.CIRCLE, spec.manualShapeFor("a"));
        assertEquals(PointShape.TRIANGLE, spec.manualShapeFor("b"));
        assertNull(spec.manualShapeFor("z"));
    }

    @Test
    void manualSizeMapResolvesCategories() {
        var spec = plot().scales(Scales.scaleSizeManual()
                .size("a", 2.5)
                .size("b", 6.0)).getScaleSpec();
        assertEquals(2.5, spec.manualSizeFor("a"));
        assertEquals(6.0, spec.manualSizeFor("b"));
        assertNull(spec.manualSizeFor("z"));
    }

    @Test
    void manualMapsResolveIntegralNumericCategories() {
        var spec = plot()
                .scales(Scales.scaleShapeManual().shape("6", PointShape.TRIANGLE))
                .scales(Scales.scaleSizeManual().size("6", 3.5))
                .getScaleSpec();
        assertEquals(PointShape.TRIANGLE, spec.manualShapeFor(6.0));
        assertEquals(3.5, spec.manualSizeFor(6.0));
        assertEquals(PointShape.TRIANGLE, spec.manualShapeFor(6));
    }

    @Test
    void fluentManualBuildersMatchLegacyMapEntries() {
        var shape = plot().scales(Scales.scaleShapeManual()
                .shape("a", PointShape.CIRCLE)
                .shape("b", PointShape.TRIANGLE)).getScaleSpec();
        assertEquals(PointShape.CIRCLE, shape.manualShapeFor("a"));
        assertEquals(PointShape.TRIANGLE, shape.manualShapeFor("b"));

        var size = plot().scales(Scales.scaleSizeManual()
                .size("a", 2.5)
                .size("b", 6.0)).getScaleSpec();
        assertEquals(2.5, size.manualSizeFor("a"));
        assertEquals(6.0, size.manualSizeFor("b"));

        var color = plot().scales(Scales.scaleColorManual()
                .color("a", Color.RED)
                .color("b", Color.BLUE)).getScaleSpec();
        assertEquals(Color.RED, color.manualColorFor("a"));
        assertEquals(Color.BLUE, color.manualColorFor("b"));
    }

    @Test
    void sampleStepsBinsTheRampIntoDistinctColours() {
        var scale = ContinuousColorScale.forRange("x", List.of(0.0, 10.0), "viridis");
        var steps = scale.sampleSteps(5);
        assertEquals(5, steps.size());
        assertEquals(steps.get(0), scale.colorFor(0.0));
        assertEquals(steps.get(4), scale.colorFor(10.0));
        assertEquals(1, scale.sampleSteps(1).size());
    }

    private static DataFrame colorData() {
        return DataFrame.byColumn("cty").of(
                Series.ofDouble(10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0, 28.0, 30.0));
    }

    private static ResolvedScales<DataFrame> resolvedScales(ScaleConfigurator configurator) {
        var spec = plot().scales(configurator).getScaleSpec();
        return ResolvedScales.forData(colorData(), new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
    }

    @Test
    void continuousColorInterpolatesNumericValues() {
        var scales = resolvedScales(Scales.scaleColorGradient(Color.WHITE, Color.BLACK));
        assertTrue(scales.isContinuousColor("cty"));
        var low = scales.resolvedColorFor("cty", 10.0);
        var high = scales.resolvedColorFor("cty", 30.0);
        var mid = scales.resolvedColorFor("cty", 20.0);
        assertEquals(Color.WHITE, low);
        assertEquals(Color.BLACK, high);
        assertNotEquals(low, mid);
        assertNotEquals(high, mid);
    }

    @Test
    void binnedColorMapsValueToBin() {
        var scales = resolvedScales(Scales.scaleColorBinned(4));
        assertTrue(scales.isContinuousColor("cty"));
        var steps = scales.continuousColorScale("cty").sampleSteps(4);
        assertEquals(steps.get(0), scales.resolvedColorFor("cty", 10.0));
        assertEquals(steps.get(1), scales.resolvedColorFor("cty", 15.0));
        assertEquals(steps.get(2), scales.resolvedColorFor("cty", 20.0));
        assertEquals(steps.get(3), scales.resolvedColorFor("cty", 25.0));
        assertEquals(steps.get(3), scales.resolvedColorFor("cty", 30.0));
    }

    @Test
    void textualColumnUsesDiscretePalette() {
        var df = DataFrame.byColumn("grp").of(Series.of("a", "b", "c"));
        var spec = plot().getScaleSpec();
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
        assertFalse(scales.isContinuousColor("grp"));
        assertEquals(scales.colorScale("grp").colorFor("a"), scales.resolvedColorFor("grp", "a"));
        assertEquals(scales.colorScale("grp").colorFor("c"), scales.resolvedColorFor("grp", "c"));
    }

    @Test
    void manualPaletteDisablesContinuousResolution() {
        var scales = resolvedScales(Scales.scaleColorManual()
                .color("10", Color.RED)
                .color("30", Color.BLUE));
        assertFalse(scales.isContinuousColor("cty"));
        assertEquals(Color.RED, scales.resolvedColorFor("cty", 10.0));
        assertEquals(Color.BLUE, scales.resolvedColorFor("cty", 30.0));
    }

    @Test
    void discreteRampStaysDiscreteOnNumericColumn() {
        var d = resolvedScales(Scales.scaleColorViridisD());
        assertFalse(d.isContinuousColor("cty"));
        assertEquals(d.colorScale("cty").colorFor(10.0), d.resolvedColorFor("cty", 10.0));
        assertEquals(d.colorScale("cty").colorFor(25.0), d.resolvedColorFor("cty", 25.0));

        var b = resolvedScales(Scales.scaleColorBrewer("Dark2"));
        assertFalse(b.isContinuousColor("cty"));
        assertEquals(b.colorScale("cty").colorFor(10.0), b.resolvedColorFor("cty", 10.0));
        assertEquals(b.colorScale("cty").colorFor(30.0), b.resolvedColorFor("cty", 30.0));

        var c = resolvedScales(Scales.scaleColorViridisC());
        assertTrue(c.isContinuousColor("cty"));
    }

    @Test
    void tooltipDotInterpolatesContinuousColumnValue() {
        var scales = resolvedScales(Scales.scaleColorGradient(Color.WHITE, Color.BLACK));
        assertEquals(Color.WHITE, scales.tooltipColor("cty", "Cylinders: 10.00"));
        assertEquals(Color.BLACK, scales.tooltipColor("cty", "Cylinders: 30.00"));
        var mid = scales.tooltipColor("cty", "Cylinders: 20.00");
        assertEquals(scales.continuousColorScale("cty").colorFor(20.0), mid);
        assertNotEquals(scales.resolvedColorFor("cty", 10.0), mid);
        assertNotEquals(scales.resolvedColorFor("cty", 30.0), mid);
    }

    @Test
    void tooltipDotFollowsNamedContinuousRamp() {
        var scales = resolvedScales(Scales.scaleColorViridisC());
        assertEquals(scales.continuousColorScale("cty").colorFor(15.0),
                scales.tooltipColor("cty", "Cylinders: 15.00"));
        assertEquals(scales.continuousColorScale("cty").colorFor(29.0),
                scales.tooltipColor("cty", "cty: 29.00"));
    }

    @Test
    void tooltipDotParsesBareFirstLineValue() {
        var scales = resolvedScales(Scales.scaleColorViridisC());
        assertEquals(scales.continuousColorScale("cty").colorFor(15.0),
                scales.tooltipColor("cty", "15.00\nn = 5\nbandwidth = 1.2"));
    }

    @Test
    void tooltipDotUsesBinnedStepColour() {
        var scales = resolvedScales(Scales.scaleColorBinned(4));
        assertEquals(scales.resolvedColorFor("cty", 20.0),
                scales.tooltipColor("cty", "Cylinders: 20.00"));
        assertEquals(scales.continuousColorScale("cty").sampleSteps(4).get(3),
                scales.tooltipColor("cty", "Cylinders: 28.00"));
    }

    @Test
    void tooltipDotMatchesDiscreteCategory() {
        var df = DataFrame.byColumn("grp").of(Series.of("a", "b", "c"));
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), plot().getScaleSpec(), new LabsSpec(), Theme.theme_gray());
        assertEquals(scales.colorScale("grp").colorFor("a"), scales.tooltipColor("grp", "grp: a"));
        assertEquals(scales.colorScale("grp").colorFor("c"), scales.tooltipColor("grp", "grp: c"));
        assertNull(scales.tooltipColor("grp", "grp: z"));
    }

    @Test
    void tooltipDotMatchesIntegralNumericColourCategory() {
        var df = DataFrame.byColumn("cyl").of(Series.ofDouble(4.0, 5.0, 6.0, 8.0));
        var spec = plot().scales(Scales.scaleColorManual()
                .color("4", Color.RED)
                .color("5", Color.GREEN)
                .color("6", Color.BLUE)
                .color("8", Color.YELLOW)).getScaleSpec();
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
        assertEquals(scales.colorScale("cyl").colorFor(6.0), scales.tooltipColor("cyl", "cyl: 6\nX: 2.5"));
        assertEquals(scales.colorScale("cyl").colorFor(8.0), scales.tooltipColor("cyl", "cyl: 8\nX: 2.5"));
        assertNull(scales.tooltipColor("cyl", "cyl: 9\nX: 2.5"));
    }

    @Test
    void tooltipDotMatchesManualShapeMap() {
        var df = DataFrame.byColumn("cyl").of(Series.ofDouble(4.0, 5.0, 6.0, 8.0));
        var spec = plot().scales(Scales.scaleShapeManual()
                .shape("4", PointShape.CIRCLE)
                .shape("5", PointShape.TRIANGLE)
                .shape("6", PointShape.SQUARE)
                .shape("8", PointShape.DIAMOND)).getScaleSpec();
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
        assertEquals(PointShape.CIRCLE, scales.tooltipShape("cyl", "cyl: 4\nX: 2.5"));
        assertEquals(PointShape.TRIANGLE, scales.tooltipShape("cyl", "cyl: 5\nX: 2.5"));
        assertEquals(PointShape.SQUARE, scales.tooltipShape("cyl", "cyl: 6\nX: 2.5"));
        assertEquals(PointShape.DIAMOND, scales.tooltipShape("cyl", "cyl: 8\nX: 2.5"));
    }

    @Test
    void tooltipDotMatchesShapeOnLaterLine() {
        var df = DataFrame.byColumn("cyl").of(Series.ofDouble(4.0, 5.0, 6.0, 8.0));
        var spec = plot().scales(Scales.scaleShapeManual()
                .shape("4", PointShape.CIRCLE)
                .shape("8", PointShape.DIAMOND)).getScaleSpec();
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
        assertEquals(PointShape.DIAMOND, scales.tooltipShape("cyl", "displacement: 2.35\ncyl: 8\nY: 30.00"));
    }

    @Test
    void tooltipDotMatchesTextShapeCategory() {
        var df = DataFrame.byColumn("drv").of(Series.of("f", "r", "4"));
        var spec = plot().scales(Scales.scaleShapeManual()
                .shape("f", PointShape.TRIANGLE)
                .shape("r", PointShape.SQUARE)).getScaleSpec();
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), spec, new LabsSpec(), Theme.theme_gray());
        assertEquals(PointShape.TRIANGLE, scales.tooltipShape("drv", "drv: f"));
        assertEquals(PointShape.SQUARE, scales.tooltipShape("drv", "drv: r"));
        assertNull(scales.tooltipShape("drv", "drv: z"));
    }

    @Test
    void tooltipDotFallsBackToNullForUnknownShapeValue() {
        var df = DataFrame.byColumn("cyl").of(Series.ofDouble(4.0, 5.0, 6.0, 8.0));
        var scales = ResolvedScales.forData(df, new DflibDataExtractor(), plot().getScaleSpec(), new LabsSpec(), Theme.theme_gray());
        assertNull(scales.tooltipShape("cyl", "cyl: 9\nX: 2.5"));
        assertNull(scales.tooltipShape(null, "cyl: 9"));
        assertNull(scales.tooltipShape("cyl", null));
    }
}
