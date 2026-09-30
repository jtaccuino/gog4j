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
package org.jtaccuino.gog.examples.dflib;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Geoms;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Reference examples for the theme's tick and grid configuration surface,
 * following the {@code showMajorGrid()}/{@code showMinorGrid()} and {@code showMajorTicks()}
 * elements.
 * <p>
 * By default both the major and the minor grid are drawn, the
 * minor grid using the same color as the major grid at half the width. Major
 * tick marks appear on the axes; minor tick marks are off, as in the
 * default theme. {@link #createDefaults()} shows that default.
 * {@link #createNoMinor()} blanks the minor grid and minor ticks.
 * {@link #createCustomMinor()} overrides the minor grid color and width and
 * the minor tick color, which is how the minor grid is made to stand out on a
 * light theme.
 */
public class TickGridPlots {

    /** Utility class; not meant to be instantiated. */
    private TickGridPlots() {
    }

    /** {@return the default tick and grid configuration: major + minor grid, major ticks, minor tick marks off by default} */
    @SamplePlot(description = "Scatter with default major and minor grid and major axis ticks.",
            title = "ticks & grid: defaults",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDefaults() {
        return scatter().labs(labs("ticks & grid: defaults",
                "Displacement (L)", "Highway MPG"));
    }

    /** {@return the same scatter with the minor grid and the minor ticks turned off} */
    @SamplePlot(description = "Scatter with the minor grid and minor ticks turned off.",
            title = "ticks & grid: major only",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNoMinor() {
        return scatter()
                .theme(t -> t.showMinorGrid(false).showMinorTicks(false))
                .labs(labs("ticks & grid: major only",
                        "Displacement (L)", "Highway MPG"));
    }

    /** {@return a scatter with a custom minor grid color/width and minor tick color} */
    @SamplePlot(description = "Scatter with restyled minor grid color and width and minor ticks.",
            title = "ticks & grid: custom minor grid",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.MINOR_GRID})
    public static Plot<DataFrame> createCustomMinor() {
        return scatter()
                .theme(t -> t.minorGridLineColor(Color.web("#a6a6a6"))
                        .minorGridLineWidth(1.2)
                        .minorTickColor(Color.web("#a6a6a6"))
                        .minorTickLength(2.5))
                .labs(labs("ticks & grid: custom minor grid",
                        "Displacement (L)", "Highway MPG"));
    }

    /** {@return the same scatter with the X-axis tick marks removed; the Y-axis keeps major and minor ticks} */
    @SamplePlot(description = "Scatter with X-axis tick marks removed but the Y axis intact.",
            title = "ticks & grid: no X-axis ticks",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNoXAxisTicks() {
        return scatter()
                .theme(t -> t.showXAxisTicks(false).showMinorTicks(false))
                .labs(labs("ticks & grid: no X-axis ticks",
                        "Displacement (L)", "Highway MPG"));
    }

    private static Plot<DataFrame> scatter() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(2.0));
    }
}
