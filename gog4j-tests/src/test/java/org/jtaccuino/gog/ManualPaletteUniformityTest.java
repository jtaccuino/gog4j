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
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.scale.Scales;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that {@code scaleColorManual} is honored uniformly by every
 * geometry that resolves a mapped categorical colour at render time — the
 * shared {@code ResolvedScales} instance must deliver the same manual palette
 * to scatter, jitter, boxplot, violin, and smooth layers that it does to the
 * palette-driven bar/line/area/polygon layers. The rendered vector output is
 * checked, so the assertion covers both the geometry strokes/fills and the
 * legend swatches (both are driven by the shared scales).
 */
@ExtendWith(JavaFxToolkitExtension.class)
class ManualPaletteUniformityTest {

    private static final Color RED = Color.web("#ff0000");
    private static final Color BLUE = Color.web("#0000ff");

    /** Two evenly filled x groups of eight rows: a solid per-box sample. */
    private static DataFrame spreadData() {
        var x = new int[16];
        var g = new String[16];
        var y = new double[16];
        for (var i = 0; i < 16; i++) {
            x[i] = i < 8 ? 0 : 1;
            g[i] = i < 8 ? "a" : "b";
            y[i] = i * 1.3 + (i % 3);
        }
        return DataFrame.byColumn("x", "g", "y")
                .of(Series.ofInt(x), Series.of(g), Series.ofDouble(y));
    }

    /** Twelve rows with alternating groups across the x range: a per-group fit. */
    private static DataFrame trendData() {
        var x = new int[12];
        var g = new String[12];
        var y = new double[12];
        var z = new double[12];
        for (var i = 0; i < 12; i++) {
            x[i] = i + 1;
            g[i] = i % 2 == 0 ? "a" : "b";
            y[i] = i + (i % 4);
            z[i] = 12 - i;
        }
        return DataFrame.byColumn("x", "g", "y", "z")
                .of(Series.ofInt(x), Series.of(g), Series.ofDouble(y), Series.ofDouble(z));
    }

    /** Renders a plot with the manual palette and asserts both colours appear. */
    private static void assertHonorsManualPalette(Plot<DataFrame> plot) throws Exception {
        var svg = onFxThread(() -> new SvgExporter().size(600, 400).toSvg(plot));
        assertTrue(svg.contains("#ff0000"),
                "manual colour RED must reach the rendered geometry and/or legend");
        assertTrue(svg.contains("#0000ff"),
                "manual colour BLUE must reach the rendered geometry and/or legend");
    }

    private static Plot<DataFrame> withPalette(Plot<DataFrame> plot) {
        return plot.scales(Scales.scaleColorManual().color("a", RED).color("b", BLUE));
    }

    @Test
    void pointHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(trendData(), aes().x("x").y("y").color("g"))
                .geoms(Geoms.point());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void jitterHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(trendData(), aes().x("x").y("y").color("g"))
                .geoms(Geoms.jitter());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void point3dHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(trendData(), aes().x("x").y("y").z("z").color("g"))
                .geoms(Geoms.point3d())
                .coord(new Coord3D());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void boxplotHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(spreadData(), aes().x("x").y("y").fill("g"))
                .geoms(Geoms.boxplot());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void violinHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(spreadData(), aes().x("x").y("y").fill("g"))
                .geoms(Geoms.violin());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void smoothHonorsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(trendData(), aes().x("x").y("y").color("g"))
                .geoms(Geoms.smooth());
        assertHonorsManualPalette(withPalette(plot));
    }

    @Test
    void legendShowsManualPalette() throws Exception {
        var plot = Ggplot.ggplot(trendData(), aes().x("x").y("y").color("g"))
                .geoms(Geoms.point());
        assertHonorsManualPalette(withPalette(plot));
    }
}
