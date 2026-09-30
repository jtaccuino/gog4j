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
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Smoke tests for the line-family geoms added in Sprint 1 of the reference gap
 * plan: {@code path()}, {@code step()}, {@code segment()} and {@code curve()}.
 * Each is rendered to SVG and asserted to emit at least one stroked path, so
 * regressions that silently drop geometry are caught.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class GeomWireTest {

    private static DataFrame wireData() {
        var x = new double[]{0, 1, 2, 3, 4, 5};
        var y = new double[]{0, 1, 0, 1, 0, 1};
        var xe = new double[]{1, 2, 3, 4, 5, 6};
        var ye = new double[]{1, 2, 3, 4, 5, 6};
        return DataFrame.byColumn("x", "y", "xend", "yend")
                .of(Series.ofDouble(x), Series.ofDouble(y), Series.ofDouble(xe), Series.ofDouble(ye));
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(400, 300).toSvg(plot));
    }

    @Test
    void pathDrawsAStrokedPolyline() throws Exception {
        var plot = Ggplot.ggplot(wireData(), aes().x("x").y("y"))
                .geoms(Geoms.path());
        var svg = renderSvg(plot);
        assertTrue(svg.contains("stroke"), "path must emit stroked geometry: " + svg);
        assertTrue(svg.contains("d=\"M"), "expected a path: " + svg);
    }

    @Test
    void stepDrawsAStaircasePolyline() throws Exception {
        var plot = Ggplot.ggplot(wireData(), aes().x("x").y("y"))
                .geoms(Geoms.step());
        var svg = renderSvg(plot);
        assertTrue(svg.contains("d=\"M"), "step must emit a path: " + svg);
    }

    @Test
    void segmentDrawsDirectedLinesUsingEndAesthetics() throws Exception {
        var plot = Ggplot.ggplot(wireData(), aes().x("x").y("y").xend("xend").yend("yend"))
                .geoms(Geoms.segment());
        var svg = renderSvg(plot);
        assertTrue(svg.contains("d=\"M"), "segment must emit stroked lines: " + svg);
    }

    @Test
    void curveDrawsSampledBezierPaths() throws Exception {
        var plot = Ggplot.ggplot(wireData(), aes().x("x").y("y").xend("xend").yend("yend"))
                .geoms(Geoms.curve(Color.BLUE, 1.0, 0.5));
        var svg = renderSvg(plot);
        assertTrue(svg.contains("d=\"M"), "curve must emit stroked paths: " + svg);
    }
}
