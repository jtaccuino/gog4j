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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.BarColStatsPlots;
import org.jtaccuino.gog.examples.dflib.LayerPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the Sprint-6 all-in-one-layer figures through the SVG backend and
 * checks the {@code afterStat} machinery end to end: a {@code layer(geom,
 * stat, position, mapping, params)} call must colour each bar by its computed
 * count (many distinct fills) where the constant-fill twin has only a handful.
 * Guarded on a live JavaFX toolkit, like the other render tests.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class LayerRenderTest {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;

    private static final Pattern RECT_WITH_FILL =
            Pattern.compile("<rect ([^>]*?)fill=\"(#[0-9a-f]{6})\"");

    @Test
    void layerBarCountRendersStatCountBars() throws Exception {
        var svg = renderSvg(LayerPlots.createLayerBarCount());
        assertNotNull(svg);
        assertTrue(svg.contains("Geoms.bar via bar()"), "the SVG must carry the bar() figure");
        assertTrue(distinctRectFills(svg).size() >= 3, "bars, their strokes, and the panel");
    }

    @Test
    void layerFromPartsWithFactoryStatsRenders() throws Exception {
        var svg = renderSvg(LayerPlots.createLayerFromParts());
        assertNotNull(svg);
        assertTrue(svg.contains("Plot#layer(...)"),
                "the SVG must carry the from-parts layer() figure");
    }

    @Test
    void afterStatFillColoursEachBarByItsCount() throws Exception {
        var afterStat = renderSvg(LayerPlots.createLayerBarAfterStat());
        var constant = renderSvg(LayerPlots.createLayerBarCount());

        var afterStatFills = distinctRectFills(afterStat);
        var constantFills = distinctRectFills(constant);
        assertTrue(afterStatFills.size() > constantFills.size(),
                "afterStat(count) must introduce a per-count gradient: "
                        + afterStatFills.size() + " vs " + constantFills.size());
    }

    @Test
    void aesOfHistogramRenders() throws Exception {
        var svg = renderSvg(LayerPlots.createAesOfHistogram());
        assertNotNull(svg);
        assertTrue(svg.contains("aesOf(map)"), "the SVG must carry the aesOf figure");
    }

    @Test
    void colIdentityOverrideRendersIdenticallyToCol() throws Exception {
        var viaOverride = renderSvg(BarColStatsPlots.createBarIdentityOverride());
        var plainCol = renderSvg(BarColStatsPlots.createMeanMpgCol());
        assertEquals(rectGeometries(plainCol), rectGeometries(viaOverride),
                "Geoms.bar(stat = identity) must draw exactly what Geoms.col() draws");
    }

    @Test
    void colCountOverrideRendersIdenticallyToBar() throws Exception {
        var viaOverride = renderSvg(BarColStatsPlots.createColCountOverride());
        var plainBar = renderSvg(BarColStatsPlots.createCylPlainCount());
        assertEquals(rectGeometries(plainBar), rectGeometries(viaOverride),
                "Geoms.col(stat = count) must draw exactly what Geoms.bar() draws");
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }

    /** The geometry of every filled rect (x, y, width, height, fill, stroke), background excluded. */
    private static Set<String> rectGeometries(String svg) {
        Set<String> geoms = new HashSet<>();
        Matcher m = RECT_WITH_FILL.matcher(svg);
        while (m.find()) {
            if (!m.group(1).contains("width=\"100%\"")) {
                geoms.add(m.group(1));
            }
        }
        return geoms;
    }

    private static Set<String> distinctRectFills(String svg) {
        Set<String> fills = new HashSet<>();
        Matcher m = RECT_WITH_FILL.matcher(svg);
        while (m.find()) {
            if (!m.group(1).contains("width=\"100%\"")) {
                fills.add(m.group(2));
            }
        }
        return fills;
    }
}
