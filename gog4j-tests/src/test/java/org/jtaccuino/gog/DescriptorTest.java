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
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies the {@link PlotDescriptor} extraction is behavior-transparent: the
 * fluent spec built through {@link Ggplot#plot(Object, Aes)} renders to the
 * exact same SVG as the equivalent {@link Plot} chain, {@link PlotDescriptor#copy()}
 * yields a fully independent clone, and {@link Ggplot#ggplot(PlotDescriptor)}
 * builds a {@link Plot} indistinguishable from one configured interactively.
 * Guarded on a live JavaFX toolkit like the other render tests.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class DescriptorTest {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 420;

    private static DataFrame mpg() {
        return DataFrame.byColumn("displ", "hwy", "drv")
                .of(
                        Series.of(1.8, 2.0, 2.8, 3.5, 4.6, 5.3),
                        Series.of(29, 31, 26, 25, 21, 18),
                        Series.of("f", "f", "4", "r", "r", "4"));
    }

    @Test
    void descriptorChainRendersByteIdenticalToPlotChain() throws Exception {
        var df = mpg();
        var plotSvg = renderSvg(ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point(), smooth())
                .labs("Descriptor parity", "displ", "hwy")
                .theme(t -> t.showMinorGrid(false)));

        var descriptor = Ggplot.plot(df, aes().x("displ").y("hwy"))
                .geoms(point(), smooth())
                .labs("Descriptor parity", "displ", "hwy")
                .theme(t -> t.showMinorGrid(false));
        var descriptorSvg = renderSvg(ggplot(descriptor));

        assertEquals(plotSvg, descriptorSvg);
    }

    @Test
    void copyIsIndependent() {
        var df = mpg();
        var original = Ggplot.plot(df, aes().x("displ").y("hwy"))
                .geoms(point())
                .labs("Original");

        var copy = original.copy();

        assertNotSame(original, copy);
        assertNotSame(original.geoms(), copy.geoms());
        assertNotSame(original.scaleSpec(), copy.scaleSpec());
        assertSame(copy.data(), original.data());
        assertEquals(original.geoms().size(), copy.geoms().size());

        copy.geoms().clear();
        copy.scaleSpec().setYReverse(!original.scaleSpec().isYReverse());
        copy.labs("Mutated");
        assertTrue(original.geoms().size() > 0, "mutating the copy must not disturb the original");
        assertTrue(original.labs().title().length() > 0, "labels stay intact on the original");
        assertEquals("Mutated", copy.labs().title(), "the copy carries its own labels");
    }

    @Test
    void plotOverDescriptorEqualsHandBuiltPlot() throws Exception {
        var df = mpg();
        var handBuilt = ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point())
                .labs("Hand built");

        var descriptor = Ggplot.plot(df, aes().x("displ").y("hwy"))
                .geoms(point())
                .labs("Hand built");

        var overDescriptor = ggplot(descriptor);

        assertEquals(renderSvg(handBuilt), renderSvg(overDescriptor));
    }

    @Test
    void optionsDefaultResolvesBothAxesOnStandalone() throws Exception {
        var df = mpg();
        var plain = renderSvg(ggplot(df, aes().x("displ").y("hwy")).geoms(point()));
        var withDefaults = renderSvg(ggplot(Ggplot.plot(df, aes().x("displ").y("hwy"))
                .options(PlotOptions.defaults())
                .geoms(point())));
        assertEquals(plain, withDefaults);
    }

    @Test
    void nullFacetAndCoordDefaultMatchPlot() {
        var df = mpg();
        var descriptor = Ggplot.plot(df, aes().x("displ").y("hwy"));
        assertNull(descriptor.facet(), "unfaceted descriptors carry a null facet spec");
        assertEquals(new Coord2D().getClass(), descriptor.coord().getClass(),
                "the default coordinate system is a Coord2D");
    }

    private static String renderSvg(GgFigure figure) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(figure));
    }
}
