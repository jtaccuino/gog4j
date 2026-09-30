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

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies {@link ComposedPlot}: the multi-plot grid composition, bottom-
 * left panel alignment via {@link Plot#insetsFor(double, double)} + re-stamped
 * {@link PlotDescriptor#panelInsets(double, double, double, double)}, shared
 * legend band, and end-to-end SVG rendering.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class ComposedPlotTest {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;

    private static DataFrame mpg() {
        return DataFrame.byColumn("displ", "hwy", "drv")
                .of(
                        Series.of(1.8, 2.0, 2.8, 3.5, 4.6, 5.3),
                        Series.of(29, 31, 26, 25, 21, 18),
                        Series.of("f", "f", "4", "r", "r", "4"));
    }

    private static Plot<DataFrame> displPlot() {
        return Ggplot.ggplot(mpg(), aes().x("displ").y("hwy")).geoms(Geoms.point());
    }

    private static Plot<DataFrame> hwyPlot() {
        return Ggplot.ggplot(mpg(), aes().x("hwy").y("displ")).geoms(Geoms.smooth());
    }

    private static String renderSvg(GgFigure figure) throws Exception {
        return onFxThread(() -> new SvgExporter()
                .size(WIDTH, HEIGHT).toSvg(figure));
    }

    @Test
    void composedPlotRendersAllFigures() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot());
        var svg = renderSvg(composed);
        assertNotNull(svg, "composed plot must render");
        assertTrue(svg.contains("<svg"), "output must be an SVG document");
        assertTrue(svg.contains(">displ<"), "first figure's x variable must render");
        assertTrue(svg.contains(">hwy<"), "second figure's x variable must render");
    }

    @Test
    void taglessComposedReclaimsTopLabelZone() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot());
        var svg = renderSvg(composed);
        // Without tags (and with no per-leaf title) the panels must start flush
        // at the top of their cell instead of leaving an empty label zone.
        assertEquals(0.0, firstPanelTop(svg), 0.001,
                "no tag/no title: the panel top label zone must be reclaimed");
        // The right label zone is empty for these plots too, so the panel must
        // reach the right edge of its cell (filling the grid, no gap to a
        // legend).
        assertEquals(firstCellWidth(svg), firstPanelRight(svg), 0.001,
                "no right y axis: the panel right label zone must be reclaimed");
    }

    @Test
    void taggedComposedKeepsPanelTagZone() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot()).tags(true);
        var svg = renderSvg(composed);
        // With tags the natural top label zone stays so each letter has room.
        assertTrue(firstPanelTop(svg) > 0.0,
                "with tags the top label zone must remain for the tag letters");
    }

    @Test
    void composedPlotGridRowsAndCols() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot(), displPlot(), hwyPlot())
                .rows(2).cols(2);
        var svg = renderSvg(composed);
        assertNotNull(svg, "2×2 composition must render");
        assertTrue(svg.length() > 1000, "grid must contain non-trivial geometry");
    }

    @Test
    void composedPlotBesideAndAbove() throws Exception {
        var beside = Ggplot.composedPlot(displPlot()).beside(hwyPlot());
        assertNotNull(renderSvg(beside), "beside must render a single row");

        var above = Ggplot.composedPlot(displPlot()).above(hwyPlot());
        assertNotNull(renderSvg(above), "above must render a single column");
    }

    @Test
    void composedPlotByrowColumnMajor() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot(), displPlot())
                .cols(2).byrow(false);
        assertNotNull(renderSvg(composed), "column-major fill must render");
    }

    @Test
    void composedPlotColumnRowWeightsProportional() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot())
                .columnWeights(2, 1).rowWeights(1);
        var svg = renderSvg(composed);
        assertNotNull(svg, "proportional composition must render");
        assertTrue(svg.contains("<svg"), "proportional composition produces valid SVG");
    }

    @Test
    void composedPlotTitleSubtitleCaptionAndTags() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot())
                .title("Composed").subtitle("Sub").caption("Cap")
                .tags(true);
        var svg = renderSvg(composed);
        assertNotNull(svg);
        assertTrue(svg.contains(">Composed<"), "title must render");
        assertTrue(svg.contains(">Sub<"), "subtitle must render");
        assertTrue(svg.contains(">Cap<"), "caption must render");
        assertTrue(svg.contains(">a<"), "first tag must render");
        assertTrue(svg.contains(">b<"), "second tag must render");
    }

    @Test
    void composedPlotExplicitTags() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot()).tags("A", "B");
        var svg = renderSvg(composed);
        assertNotNull(svg);
        assertTrue(svg.contains(">A<"), "explicit first tag must render");
        assertTrue(svg.contains(">B<"), "explicit second tag must render");
    }

    @Test
    void tagsRenderInEachPanelCorner() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot(), displPlot(), hwyPlot())
                .rows(2).cols(2).tags(true);
        var svg = renderSvg(composed);
        assertNotNull(svg);
        // Every tag must sit inside a translated cell frame (not stacked at the
        // figure origin), and each tag must land in a distinct cell.
        var tagCells = tagEnclosingCells(svg);
        assertEquals(4, tagCells.size(), "all four tags must render");
        assertFalse(tagCells.containsValue("ROOT"), "no tag may be drawn outside a cell frame");
        assertEquals(4, new HashSet<>(tagCells.values()).size(),
                "each tag must be in a distinct cell corner");
    }

    /** The y offset of the first panel rect, relative to its enclosing cell. */
    private static double firstPanelTop(String svg) {
        var matcher = Pattern.compile(
                "<rect x=\"[^\"]*\" y=\"([0-9.]+)\"[^>]*fill=\"#ebebeb\"").matcher(svg);
        assertTrue(matcher.find(), "a panel rect must exist");
        return Double.parseDouble(matcher.group(1));
    }

    /** The width of the first cell, from its background rect. */
    private static double firstCellWidth(String svg) {
        var matcher = Pattern.compile(
                "<rect x=\"0\" y=\"0\" width=\"([0-9.]+)\"").matcher(svg);
        assertTrue(matcher.find(), "a cell background rect must exist");
        return Double.parseDouble(matcher.group(1));
    }

    /** The right edge of the first panel rect, relative to its enclosing cell. */
    private static double firstPanelRight(String svg) {
        var matcher = Pattern.compile(
                "<rect x=\"([0-9.]+)\" y=\"([0-9.]+)\" width=\"([0-9.]+)\"[^>]*fill=\"#ebebeb\"").matcher(svg);
        assertTrue(matcher.find(), "a panel rect must exist");
        return Double.parseDouble(matcher.group(1)) + Double.parseDouble(matcher.group(3));
    }

    /**
     * Maps each tag letter to the innermost {@code translate(...)} cell frame
     * it is drawn inside, or {@code ROOT} when drawn without any translation.
     */
    private static Map<String, String> tagEnclosingCells(String svg) {
        var out = new LinkedHashMap<String, String>();
        var stack = new ArrayDeque<String>();
        var group = Pattern.compile("<g[^>]*>|</g>|>([a-d])</text>");
        var translate = Pattern.compile("translate\\(([^)]*)\\)");
        var matcher = group.matcher(svg);
        while (matcher.find()) {
            var token = matcher.group(0);
            if (token.equals("</g>")) {
                stack.pop();
            } else if (token.startsWith("<g")) {
                stack.push(token);
            } else {
                var cell = "ROOT";
                for (var frame : stack) {
                    var t = translate.matcher(frame);
                    if (t.find()) {
                        cell = t.group(1);
                    }
                }
                out.put(matcher.group(1), cell);
            }
        }
        return out;
    }

    @Test
    void plotInsetsForMeasuresNaturalMargins() throws Exception {
        onFxThread(() -> {
            var insets = displPlot().insetsFor(400, 300);
            assertTrue(insets.left() > 0, "left inset must reserve the y-label zone");
            assertTrue(insets.bottom() > 0, "bottom inset must reserve the x-label zone");
            assertTrue(insets.top() >= 0 && insets.right() >= 0,
                    "top and right insets must be non-negative");
            return null;
        });
    }

    @Test
    void composedPlotLegendBandRenders() throws Exception {
        var source = Ggplot.plot(mpg(), aes().x("displ").y("hwy").color("drv"))
                .geoms(Geoms.point());
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot())
                .legend(source).collectLegends(true);
        var svg = renderSvg(composed);
        assertNotNull(svg, "shared legend composition must render");
        assertTrue(svg.contains(">drv<"), "shared legend title must render");
    }

    @Test
    void composedPlotAlignsPanelsByDefault() throws Exception {
        var composed = Ggplot.composedPlot(displPlot(), hwyPlot());
        var svg = renderSvg(composed);
        assertNotNull(svg, "aligned composition must render without error");
        assertTrue(svg.contains("<svg"), "aligned composition produces valid SVG");
    }

    @Test
    void composedPlotLayoutChildrenAttachesCanvas() throws Exception {
        onFxThread(() -> {
            var composed = Ggplot.composedPlot(displPlot(), hwyPlot());
            var parent = new Pane(composed);
            assertNotNull(parent);
            composed.resize(WIDTH, HEIGHT);
            composed.layoutChildren();
            var canvases = composed.getChildren().stream()
                    .filter(Canvas.class::isInstance)
                    .map(Canvas.class::cast)
                    .toList();
            assertEquals(1, canvases.size(),
                    "layoutChildren must attach exactly one canvas");
            return null;
        });
    }
}
