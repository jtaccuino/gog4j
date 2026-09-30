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
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.coord.CoordFixed;
import org.jtaccuino.gog.examples.dflib.CoordPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the fixed-aspect coordinates added in Sprint 7. Geometrically the
 * crop must make equal data spans map to equal pixel spans on both continuous
 * axes, and a discrete axis must be rejected with {@link IllegalArgumentException}.
 * All figures render through the SVG backend, guarded on a live JavaFX toolkit.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class CoordEqualTest {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;

    @Test
    void coordFixedEqualizesThePixelWindow() {
        var coord = new CoordFixed(1.0);
        // x span of 2 units over 200 px => 100 px/unit; y span of 4 units over
        // 200 px => 50 px/unit. The y axis has the slack, so it is cropped to
        // 100 px/unit * 4 = 400 px... but it only has 200, so actually the x
        // axis is the one with the wrong per-unit ratio and gets cropped.
        // With ratio 1 the per-unit rates must be equal after the crop.
        var w = coord.equalizeWindow(0, 200, 50, 250, 2, 4);
        double xUnit = (w[1] - w[0]) / 2.0;
        double yUnit = (w[3] - w[2]) / 4.0;
        assertEquals(xUnit, yUnit, 1e-9, "pixels per unit must be equal on both axes");
    }

    @Test
    void coordFixedYieldsToTheRatio() {
        var coord = new CoordFixed(2.0);
        var w = coord.equalizeWindow(0, 200, 50, 250, 2, 4);
        double xPerUnit = (w[1] - w[0]) / 2.0;
        double yPerUnit = (w[3] - w[2]) / 4.0;
        assertEquals(xPerUnit / yPerUnit, 2.0, 1e-9,
                "the x per-unit pixel rate divided by the y rate must honour the ratio");
    }

    @Test
    void coordEqualFigureRenders() throws Exception {
        var svg = renderSvg(CoordPlots.createCoordEqual());
        assertTrue(svg.contains("coordEqual"), "the SVG must carry the coordEqual figure");
    }

    @Test
    void coordTransFigureRenders() throws Exception {
        var svg = renderSvg(CoordPlots.createCoordTrans());
        assertTrue(svg.contains("coordTrans"), "the SVG must carry the coordTrans figure");
    }

    @Test
    void discreteAxisRejectedByCoordEqual() throws Exception {
        var df = DataFrame.byColumn("cat", "v")
                .of(Series.of("a", "b", "c"), Series.ofDouble(1.0, 2.0, 3.0));
        var plot = Ggplot.ggplot(df, aes().x("cat").y("v"))
                .geoms(Geoms.point())
                .coord(Coords.coordEqual());
        assertThrows(IllegalArgumentException.class, () -> renderSvg(plot),
                "coordEqual on a discrete axis must throw IllegalArgumentException");
    }

    @Test
    void discreteAxisRejectedByCoordTrans() throws Exception {
        var df = DataFrame.byColumn("cat", "v")
                .of(Series.of("a", "b", "c"), Series.ofDouble(1.0, 10.0, 100.0));
        var plot = Ggplot.ggplot(df, aes().x("cat").y("v"))
                .geoms(Geoms.point())
                .coord(Coords.coordTrans(
                        ScaleTransform.LOG10, null));
        assertThrows(IllegalArgumentException.class, () -> renderSvg(plot),
                "coordTrans on a discrete axis must throw IllegalArgumentException");
    }

    @Test
    void invalidRatioRejected() {
        assertThrows(IllegalArgumentException.class, () -> Coords.coordEqual(0),
                "a non-positive ratio must be rejected");
        assertThrows(IllegalArgumentException.class, () -> Coords.coordEqual(-2),
                "a negative ratio must be rejected");
        assertThrows(IllegalArgumentException.class, () -> Coords.coordEqual(Double.NaN),
                "a NaN ratio must be rejected");
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }
}
