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
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot3d;
import static org.jtaccuino.gog.Ggplot.plot3d;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the discoverable 3D entry points {@link Ggplot#plot3d(Object, Aes)}
 * / {@link Ggplot#ggplot3d(Object, Aes)} and the {@link PlotDescriptor3D} cube
 * DSL, plus an end-to-end SVG render guarded on a live JavaFX toolkit.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Plot3dApiTest {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;

    private static DataFrame data() {
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(1, 2, 3, 4),
                        Series.ofDouble(2, 4, 6, 8),
                        Series.ofDouble(1, 3, 5, 7));
    }

    @Test
    void plot3dInstallsA3dDescriptorAndCoord() {
        var descriptor = plot3d(data(), aes().x("x").y("y").z("z"));
        assertInstanceOf(PlotDescriptor3D.class, descriptor);
        assertInstanceOf(Coord3D.class, descriptor.coord());
    }

    @Test
    void viewPanelsAndLightChainWithoutLosingThe3dType() {
        var descriptor = plot3d(data(), aes().x("x").y("y").z("z"))
                .view(35, -75, -55)
                .panels(CubePanel.NONE)
                .light(Light3d.defaultLight());
        assertInstanceOf(PlotDescriptor3D.class, descriptor);
        assertInstanceOf(Coord3D.class, descriptor.coord());
        assertNotNull(descriptor.light());
    }

    @Test
    void coordRejectsNon3dCoordinateSystems() {
        var descriptor = plot3d(data(), aes().x("x").y("y").z("z"));
        assertThrows(IllegalArgumentException.class,
                () -> descriptor.coord(Coords.coordCartesian()));
    }

    @Test
    void ggplot3dRendersTheCube() throws Exception {
        var plot = ggplot3d(data(), aes().x("x").y("y").z("z"))
                .geoms(point3d().size(6.0))
                .labs(labs("plot3d smoke", "x", "y"));
        var svg = onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
        assertNotNull(svg);
        assertTrue(svg.contains("plot3d smoke"), "the SVG must carry the chart title");
    }
}
