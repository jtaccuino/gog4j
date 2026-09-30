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
package org.jtaccuino.gog;

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.guide.GuideColorbar;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Guards the hover-tooltip colour invariant for the Sprint-8 stage examples:
 * the tooltip glyph resolved from a geometry's tooltip text must equal the
 * colour the geometry was actually painted with.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class StageTooltipColorTest {

    private static final double TOL = 1e-6;
    private Method tooltipColorMethod;
    private Method resolvedColorForMethod;
    private Method resolveTooltipColorMethod;

    @Test
    void stage04PointColorAgreesWithTooltipColor() throws Exception {
        // stage-04: scatter y = afterScale("x"), colour = cyl
        var plot = StagePlotsRef.create("createAfterScalePosition");
        var df = (DataFrame) plotDf(plot);
        renderPlot(plot);

        var scales = scalesOf(plot);
        var self = this;

        // The continuous viridis scale used for "cyl" (4..8) must agree whether
        // resolved through the per-point render path or the tooltip path.
        for (int i = 0; i < df.height(); i++) {
            var cyl = df.getColumn("cyl").get(i);
            var pointColor = (Color) self.resolvedColorFor(scales, "cyl", cyl);
            var tooltipText = "cyl: " + cyl + "\nhwy: 24.0\nY: 24.0";
            var glyph = (Color) self.tooltipColor(scales, "cyl", tooltipText);
            if (cyl != null) {
                assertTrue(same(pointColor, glyph),
                        "row " + i + " cyl=" + cyl + " point=" + pointColor + " tooltip=" + glyph);
            }
        }

        // Full end-to-end: the private Plot.resolveTooltipColor on the tooltip
        // text a hover produces must also agree with the point's colour.
        var row4 = df.height() - 1; // a cyl=4 row near the end
        var cyl = df.getColumn("cyl").get(row4);
        var pointColor = (Color) self.resolvedColorFor(scales, "cyl", cyl);
        if (resolveTooltipColorMethod == null) {
            resolveTooltipColorMethod = Plot.class.getDeclaredMethod("resolveTooltipColor", String.class);
            resolveTooltipColorMethod.setAccessible(true);
        }
        var glyphColor = (Color) resolveTooltipColorMethod.invoke(plot,
                "cyl: " + cyl + "\nhwy: 24.0\nY: 24.0");
        assertEquals(true, same(pointColor, glyphColor),
                "end-to-end cyl=" + cyl + " point=" + pointColor + " tooltip=" + glyphColor);
    }

    @Test
    void stage01AfterStatExprTooltipMatchesBarFill() throws Exception {
        // stage-01: histogram fill = afterStat(count / max(count)); the
        // expression's own output must drive the scale so bars shade.
        var plot = StagePlotsRef.create("createAfterStatNCount");
        renderPlot(plot);

        // Distinct ratios resolve to distinct colours (the ramp spans the
        // expression's own output domain, not the raw count column's).
        var glyphLow = resolveTooltip(plot, "count: 0.2\n-4\nValue: 12.0");
        var glyphHigh = resolveTooltip(plot, "count: 1.0\n-4\nValue: 12.0");
        assertTrue(!same(glyphLow, glyphHigh),
                "afterStat(count/max(count)) fill must span the ramp; low=" + glyphLow + " high=" + glyphHigh);

        // And the tooltip glyph equals the colour the per-row render path uses.
        var scales = scalesOf(plot);
        for (double r : new double[] {0.2, 0.5, 0.8, 1.0}) {
            var barFill = (Color) resolvedColorFor(scales, "count", r);
            var glyph = resolveTooltip(plot, "count: " + r + "\n-4\nValue: 12.0");
            assertTrue(same(barFill, glyph), "ratio " + r + " bar=" + barFill + " tooltip=" + glyph);
        }
    }

    @Test
    void stage01EveryBinPaintMatchesTooltipGlyph() throws Exception {
        // Regression: the tooltip hint used to round afterStat counts to two
        // decimals ("0.22"), so the glyph resolved off a slightly different
        // value than the exact ratio the bar was painted with. The geometry's
        // own hint line must now round-trip so paint == glyph for every bin.
        var plot = StagePlotsRef.create("createAfterStatNCount");
        renderPlot(plot);
        var scales = scalesOf(plot);

        var prepared = Plot.class.getDeclaredField("preparedData");
        prepared.setAccessible(true);
        var ld = ((Map<?, ?>) prepared.get(plot)).values().iterator().next();
        var statData = (StatData) ld.getClass().getMethod("statData").invoke(ld);
        var aes = effectiveAes(plot, firstLayer(plot));

        var renderCls = Class.forName("org.jtaccuino.gog.layer.AesRendering");
        var fillColorM = renderCls.getDeclaredMethod("fillColor",
                StatData.class, scales.getClass(),
                Class.forName("org.jtaccuino.gog.AesValue$AesResolution"),
                AesValue.class, int.class);
        fillColorM.setAccessible(true);
        var resolutionM = renderCls.getDeclaredMethod("resolution",
                Class.forName("org.jtaccuino.gog.layer.PanelContext"),
                Aes.class, StatData.class, List.class);
        resolutionM.setAccessible(true);
        var res = resolutionM.invoke(null, null, aes, statData, null);
        var hintM = renderCls.getDeclaredMethod("statColourHint",
                StatData.class, Aes.class, int.class);
        hintM.setAccessible(true);

        var counts = statData.columnAsDoubles("count");
        for (int i = 0; i < counts.size(); i++) {
            if (counts.get(i) == 0.0) {
                continue;
            }
            var paint = (Color) fillColorM.invoke(null, statData, scales, res, aes.fillValue(), i);
            var hint = (String) hintM.invoke(null, statData, aes, i);
            var glyph = resolveTooltip(plot, hint + "\n-4\nValue: 10");
            assertTrue(same(paint, glyph),
                    "bin " + i + " count=" + counts.get(i) + " hint=\"" + hint
                            + "\" bar=" + paint + " tooltip=" + glyph);
        }
    }

    @Test
    void stage04EveryPointPaintMatchesTooltipGlyph() throws Exception {
        var plot = StagePlotsRef.create("createAfterScalePosition");
        renderPlot(plot);
        var scales = scalesOf(plot);
        var df = (DataFrame) plotDf(plot);
        var cyl = df.getColumn("cyl");
        for (int i = 0; i < cyl.size(); i++) {
            var value = cyl.get(i);
            if (value == null) {
                continue;
            }
            var paint = (Color) resolvedColorFor(scales, "cyl", value);
            var glyph = resolveTooltip(plot, "cyl: " + value + "\nhwy: 24\nY: 24.00");
            assertTrue(same(paint, glyph),
                    "row " + i + " cyl=" + value + " point=" + paint + " tooltip=" + glyph);
        }
    }

    @Test
    void stage02AfterScaleFillTooltipMatchesBarColor() throws Exception {
        // stage-02: color=afterStat(COUNT), fill=afterScale("color"); the
        // fill echoes the colour scale assigned to the count.
        assertStatColumnColourAgrees("createAfterScaleFill", "count", 14.0, 50.0, 81.0);
    }

    @Test
    void stage03StagedFillTooltipMatchesBarColor() throws Exception {
        // stage-03: fill = stage(afterStat(prop), afterScale = color); the
        // tooltip reports the prop and must resolve to the staged bar colour.
        assertStatColumnColourAgrees("createStage", "prop", 0.1, 0.25, 0.46);
    }

    @Test
    void layerConstantBarFillGlyphMatchesPaint() throws Exception {
        // Bars with a constant fill and no mapped aesthetic (the layer /
        // afterStat examples' identity-colour bars) must report the exact
        // fill the bars were painted with, not a scale/default fallback.
        for (String method : new String[] {"createLayerBarCount", "createLayerFromParts"}) {
            var plot = reflectPlot("org.jtaccuino.gog.examples.dflib.LayerPlots", method);
            renderPlot(plot);
            var glyph = resolveTooltip(plot, "Vehicle Class: suv\nValue: 62.0");
            assertTrue(same(Color.web("#3182bd"), glyph),
                    method + " constant-bar tooltip glyph=" + glyph);
        }
    }

    @Test
    void layerAfterStatBarsPaintMatchesTooltipGlyph() throws Exception {
        // The layer / afterStat bar examples (fill = afterStat(count) bars
        // and afterStat(density) histogram) must resolve every bar's tooltip
        // glyph to the exact colour the bar was painted with.
        var cases = new String[][] {
                {"org.jtaccuino.gog.examples.dflib.LayerPlots", "createLayerBarAfterStat", "count"},
                {"org.jtaccuino.gog.examples.dflib.LayerPlots", "createAesOfHistogram", "density"},
                {"org.jtaccuino.gog.examples.dflib.BarColStatsPlots", "createCylCount", "count"},
                {"org.jtaccuino.gog.examples.dflib.BarColStatsPlots", "createAfterStatProp", "prop"},
        };
        for (var c : cases) {
            var plot = reflectPlot(c[0], c[1]);
            renderPlot(plot);
            var scales = scalesOf(plot);

            var prepared = Plot.class.getDeclaredField("preparedData");
            prepared.setAccessible(true);
            var ld = ((Map<?, ?>) prepared.get(plot)).values().iterator().next();
            var statData = (StatData) ld.getClass().getMethod("statData").invoke(ld);
            var aes = effectiveAes(plot, firstLayer(plot));

            var renderCls = Class.forName("org.jtaccuino.gog.layer.AesRendering");
            var fillColorM = renderCls.getDeclaredMethod("fillColor",
                    StatData.class, scales.getClass(),
                    Class.forName("org.jtaccuino.gog.AesValue$AesResolution"),
                    AesValue.class, int.class);
            fillColorM.setAccessible(true);
            var resolutionM = renderCls.getDeclaredMethod("resolution",
                    Class.forName("org.jtaccuino.gog.layer.PanelContext"),
                    Aes.class, StatData.class, List.class);
            resolutionM.setAccessible(true);
            var res = resolutionM.invoke(null, null, aes, statData, null);
            var hintM = renderCls.getDeclaredMethod("statColourHint",
                    StatData.class, Aes.class, int.class);
            hintM.setAccessible(true);

            var values = statData.columnAsDoubles(c[2]);
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) == 0.0) {
                    continue;
                }
                var paint = (Color) fillColorM.invoke(null, statData, scales, res, aes.fillValue(), i);
                var hint = (String) hintM.invoke(null, statData, aes, i);
                var glyph = resolveTooltip(plot, hint + "\nVehicle Class: x\nValue: 10.0");
                assertTrue(same(paint, glyph),
                        c[1] + " row " + i + " " + c[2] + "=" + values.get(i)
                                + " hint=\"" + hint + "\" bar=" + paint + " tooltip=" + glyph);
            }
        }
    }

    @Test
    void identityBarFillMatchesTooltipGlyph() throws Exception {
        // Geoms.col / bar(stat = identity) with no mapped fill must paint with
        // the same constant colour the tooltip glyph reports. They used to
        // diverge: the raw render path painted the theme fallback red while the
        // glyph returned the geometry default #3182bd.
        for (String method : new String[] {"createCylPlainCount", "createMeanMpgCol",
                "createBarIdentityOverride", "createColCountOverride"}) {
            var plot = reflectPlot("org.jtaccuino.gog.examples.dflib.BarColStatsPlots", method);
            var svg = renderSvg(plot);
            assertTrue(svg.contains("#3182bd"), method + " rendered without the #3182bd bar fill");
            assertTrue(!svg.contains("#e31a1c"), method + " still painted the theme fallback red");
            var glyph = resolveTooltip(plot, "Cylinders: 4\nValue: 11.0");
            assertTrue(same(Color.web("#3182bd"), glyph), method + " tooltip glyph=" + glyph);
        }
    }

    private String renderSvg(Plot<?> plot) throws Exception {
        var surface = new SvgDrawSurface(800, 500, Color.WHITE);
        onFxThread(() -> {
            plot.renderTo(surface, 800, 500);
            return null;
        });
        return surface.toSvg();
    }

    private void assertStatColumnColourAgrees(String plotName, String column,
                                              double v0, double v1, double v2) throws Exception {
        var plot = StagePlotsRef.create(plotName);
        renderPlot(plot);
        var scales = scalesOf(plot);
        for (double v : new double[] {v0, v1, v2}) {
            var barFill = (Color) resolvedColorFor(scales, column, v);
            var glyph = resolveTooltip(plot, column + ": " + v + "\n-4\nValue: 3.0");
            assertTrue(same(barFill, glyph),
                    plotName + " " + column + "=" + v + " bar=" + barFill + " tooltip=" + glyph);
        }
    }

    @Test
    void afterStatFillsCarryColourbarGuides() throws Exception {
        // the default draws a fill colourbar for afterStat(...) mappings even when
        // they live on a layer; assert every stage fill plot now collects one.
        for (String name : new String[] {"createAfterStatNCount", "createAfterScaleFill", "createStage"}) {
            var plot = StagePlotsRef.create(name);
            renderPlot(plot);
            var m = Plot.class.getDeclaredMethod("collectGuideInlays", Guides.class);
            m.setAccessible(true);
            var inlays = (List<?>) m.invoke(plot, Guides.empty());
            boolean colourbar = false;
            for (var inlay : inlays) {
                var guide = inlay.getClass().getMethod("guide").invoke(inlay);
                if (guide instanceof GuideColorbar) {
                    colourbar = true;
                    break;
                }
            }
            assertTrue(colourbar, name + " should collect a fill colourbar guide; got " + inlays);
        }
    }

    private Color resolveTooltip(Plot<?> plot, String tooltip) throws Exception {
        if (resolveTooltipColorMethod == null) {
            resolveTooltipColorMethod = Plot.class.getDeclaredMethod("resolveTooltipColor", String.class);
            resolveTooltipColorMethod.setAccessible(true);
        }
        return (Color) resolveTooltipColorMethod.invoke(plot, tooltip);
    }

    private static boolean same(Color a, Color b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return Math.abs(a.getRed() - b.getRed()) < TOL
                && Math.abs(a.getGreen() - b.getGreen()) < TOL
                && Math.abs(a.getBlue() - b.getBlue()) < TOL;
    }

    private Object plotDf(Plot<?> plot) throws Exception {
        var f = Plot.class.getDeclaredField("descriptor");
        f.setAccessible(true);
        var desc = f.get(plot);
        var m = desc.getClass().getMethod("data");
        return m.invoke(desc);
    }

    private void renderPlot(Plot<?> plot) throws Exception {
        var surface = new SvgDrawSurface(800, 500, Color.WHITE);
        onFxThread(() -> {
            plot.renderTo(surface, 800, 500);
            return null;
        });
    }

    private void onFxThread(Callable<Void> work) throws Exception {
        var task = new FutureTask<>(work);
        Platform.runLater(task);
        task.get(60, TimeUnit.SECONDS);
    }

    private Object scalesOf(Plot<?> plot) throws Exception {
        var m = Plot.class.getDeclaredMethod("scales");
        m.setAccessible(true);
        return m.invoke(plot);
    }

    private Object resolvedColorFor(Object scales, String column, Object value) throws Exception {
        if (resolvedColorForMethod == null) {
            resolvedColorForMethod = scales.getClass().getMethod("resolvedColorFor", String.class, Object.class);
        }
        return resolvedColorForMethod.invoke(scales, column, value);
    }

    private Object firstLayer(Plot<?> plot) throws Exception {
        var f = Plot.class.getDeclaredField("descriptor");
        f.setAccessible(true);
        var desc = f.get(plot);
        var m = desc.getClass().getMethod("geoms");
        return ((List<?>) m.invoke(desc)).get(0);
    }

    private Aes effectiveAes(Plot<?> plot, Object layer) throws Exception {
        var m = Plot.class.getDeclaredMethod("effectiveAes", Layer.class);
        m.setAccessible(true);
        return (Aes) m.invoke(plot, layer);
    }

    private Object tooltipColor(Object scales, String column, String tooltip) throws Exception {
        if (tooltipColorMethod == null) {
            tooltipColorMethod = scales.getClass().getMethod("tooltipColor", String.class, String.class);
        }
        return tooltipColorMethod.invoke(scales, column, tooltip);
    }

    /** Loaded reflectively so the test does not hard-bind to the example class. */
    private static final class StagePlotsRef {
        static Plot<?> create(String methodName) {
            return reflectPlot("org.jtaccuino.gog.examples.dflib.StagePlots", methodName);
        }
    }

    private static Plot<?> reflectPlot(String className, String methodName) {
        try {
            var cls = Class.forName(className);
            return (Plot<?>) cls.getMethod(methodName).invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
