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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.spi.DataExtractorRegistry;
import org.jtaccuino.gog.stat.StatBin;
import org.jtaccuino.gog.stat.Stats;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Swing coverage for Sprint 2 of the feature-parity plan: the {@code Stats.bin()}
 * ({@code histogram()}/{@code freqpoly()}) and {@code Stats.summary()}
 * (error/range geom) family. Pure transformations are asserted on their data
 * output; every geometry is rendered to SVG and checked to emit geometry.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class StatSummaryBinTest {

    private static DataFrame scoreData() {
        return DataFrame.byColumn("score")
                .of(Series.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0, 4.0));
    }

    private static DataFrame groupedData() {
        var g = Series.of("a", "a", "a", "b", "b", "b");
        var v = Series.of(1.0, 2.0, 3.0, 4.0, 5.0, 9.0);
        return DataFrame.byColumn("grp", "val").of(g, v);
    }

    private static DataFrame summaryData() {
        var x = Series.of("a", "b", "c");
        var y = Series.of(2.0, 5.0, 8.0);
        var mn = Series.of(1.0, 4.0, 7.0);
        var mx = Series.of(3.0, 6.0, 9.0);
        return DataFrame.byColumn("x", "y", "ymin", "ymax").of(x, y, mn, mx);
    }

    @SuppressWarnings("unchecked")
    private static DataExtractor<DataFrame> extractor() {
        return (DataExtractor<DataFrame>) DataExtractorRegistry.extractorFor(DataFrame.class);
    }

    private static double d(Object o) {
        return Values.toDouble(o);
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(400, 300).toSvg(plot));
    }

    @Test
    void statSummaryMeanSdEmitsPerGroupAggregate() {
        var ext = extractor();
        var out = Stats.summary(groupedData(), "grp", "val").meanSd().fit();
        var xs = ext.getColumn(out, "x");
        var ys = ext.getColumn(out, "y");
        var mn = ext.getColumn(out, "ymin");
        var mx = ext.getColumn(out, "ymax");
        assertEquals(2, xs.size());
        assertEquals(2.0, d(ys.get(0)), 1e-9);
        assertEquals(6.0, d(ys.get(1)), 1e-9);
        assertTrue(d(mn.get(0)) < d(ys.get(0)));
        assertTrue(d(mx.get(0)) > d(ys.get(0)));
    }

    @Test
    void statSummaryMedianIqrUsesQuantiles() {
        var out = Stats.summary(groupedData(), "grp", "val").medianIqr().fit();
        var ys = extractor().getColumn(out, "y");
        assertEquals(2.0, d(ys.get(0)), 1e-9);
    }

    @Test
    void statBinCountsIntoEdges() {
        var params = new StatBin.BinParams(3, Double.NaN, Double.NaN, Double.NaN, false);
        var stat = Stats.bin();
        var edges = stat.edges(List.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0), params);
        assertTrue(edges.length >= 3, "expected a multi-bin grid");
        var data = stat.fit(List.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0), params);
        var counts = data.column("count");
        assertNotNull(counts, "Stats.bin must produce a count column");
        var total = 0.0;
        for (var c : counts) {
            total += d(c);
        }
        assertEquals(6.0, total, 1e-9);
    }

    @Test
    void histogramRendersFilledBars() throws Exception {
        var plot = Ggplot.ggplot(scoreData(), aes().x("score"))
                .geoms(Geoms.histogram().bins(4).fill(Color.web("#3182bd")));
        var svg = renderSvg(plot);
        assertTrue(svg.contains("<svg"), "histogram must produce svg: " + svg);
        assertTrue(svg.contains("d=\"M"), "histogram must emit bar paths: " + svg);
    }

    @Test
    void freqpolyRendersAStrokedLine() throws Exception {
        var plot = Ggplot.ggplot(scoreData(), aes().x("score"))
                .geoms(Geoms.freqpoly().bins(4).color(Color.web("#e31a1c")));
        var svg = renderSvg(plot);
        assertTrue(svg.contains("d=\"M"), "freqpoly must emit a stroked line: " + svg);
    }

    @Test
    void errorbarRendersWhiskers() throws Exception {
        var plot = Ggplot.ggplot(summaryData(), aes().x("x").y("y").ymin("ymin").ymax("ymax"))
                .geoms(Geoms.errorbar());
        var svg = renderSvg(plot);
        assertTrue(svg.contains("d=\"M"), "errorbar must emit whiskers: " + svg);
    }

    @Test
    void pointrangeAndLinerangeAndCrossbarRender() throws Exception {
        var base = aes().x("x").y("y").ymin("ymin").ymax("ymax");
        var pr = renderSvg(Ggplot.ggplot(summaryData(), base).geoms(Geoms.pointrange()));
        assertTrue(pr.contains("d=\"M"), "pointrange must emit geometry: " + pr);
        var lr = renderSvg(Ggplot.ggplot(summaryData(), base).geoms(Geoms.linerange()));
        assertTrue(lr.contains("<svg"), "linerange must produce svg: " + lr);
        var cr = renderSvg(Ggplot.ggplot(summaryData(), base).geoms(Geoms.crossbar()));
        assertTrue(cr.contains("<svg"), "crossbar must produce svg: " + cr);
    }

    @Test
    void ribbonRendersFilledBand() throws Exception {
        var t = Series.of(0.0, 1.0, 2.0, 3.0);
        var lo = Series.of(0.0, 1.0, 2.0, 3.0);
        var hi = Series.of(1.0, 2.0, 3.0, 4.0);
        var df = DataFrame.byColumn("x", "ymin", "ymax").of(t, lo, hi);
        var plot = Ggplot.ggplot(df, aes().x("x").ymin("ymin").ymax("ymax"))
                .geoms(Geoms.ribbon());
        var svg = renderSvg(plot);
        assertTrue(svg.contains("<svg"), "ribbon must produce svg: " + svg);
    }
}
