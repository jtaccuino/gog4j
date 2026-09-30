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
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleLinetypeManual;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.LineType;
import org.jtaccuino.gog.scale.LineTypes;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Demonstrates the {@code linetype} aesthetic: discrete line types mapped from
 * a column, alone or alongside {@code colour}, for both {@code line()} and
 * {@code smooth()} geometries.
 */
public class LineTypePlots {

    /** Utility class; not meant to be instantiated. */
    private LineTypePlots() {
    }

    /**
     * {@return grouped lines where the {@code linetype} aesthetic alone both
     * splits the data into groups and assigns each group a distinct dash
     * pattern}
     */
    @SamplePlot(description = "Grouped lines where the linetype aesthetic alone assigns distinct dash patterns per drive type.",
            title = "Linetype Maps the Lines per Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.LINE},
            features = {SampleFeature.TWO_D, SampleFeature.LINETYPE})
    public static Plot<DataFrame> createLineGroupDashes() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").linetype("drv"))
                .geoms(
                    line()
                )
                .labs(labs("Linetype Maps the Lines per Drive Type", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter-and-line plot mapping both {@code colour} and
     * {@code linetype} to the same column, so each group is a coloured dashed
     * line and the legend keys carry the matching dash}
     */
    @SamplePlot(description = "Points and lines mapping both colour and linetype to drive type.",
            title = "Colour + Linetype per Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.LINE},
            features = {SampleFeature.TWO_D, SampleFeature.LINETYPE, SampleFeature.LAYERS})
    public static Plot<DataFrame> createColorAndLineType() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").linetype("drv"))
                .geoms(
                    point().size(3.0),
                    line()
                )
                .labs(labs("Colour + Linetype per Drive Type", "displacement", "highway mpg"));
    }

    /**
     * {@return a plot whose per-drive-type linear trend lines are both coloured
     * and dashed, with the legend showing the dashed line for each group}
     */
    @SamplePlot(description = "Linear smooth trends coloured and dashed per drive type with a matching legend.",
            title = "Dashed Linear Trends per Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D, SampleFeature.LINETYPE, SampleFeature.LAYERS})
    public static Plot<DataFrame> createSmoothLineType() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").linetype("drv"))
                .geoms(
                    point().size(2.0),
                    smooth(SmoothMethod.LM, false)
                )
                .labs(labs("Dashed Linear Trends per Drive Type", "displacement", "highway mpg"));
    }

    /**
     * {@return grouped lines where the {@code linetype} aesthetic is assigned
     * explicit dash patterns via the manual linetype DSL
     * ({@code scaleLinetypeManual()})}
     */
    @SamplePlot(description = "Lines with explicit dash patterns assigned via the manual linetype scale.",
            title = "Manual Linetype per Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.LINE},
            scales = {SampleScale.LINETYPE_MANUAL},
            features = {SampleFeature.TWO_D, SampleFeature.LINETYPE})
    public static Plot<DataFrame> createManualLineType() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").linetype("drv"))
                .scales(
                    scaleLinetypeManual()
                        .linetype("4", LineType.of(4.0, 2.0))
                        .linetype("f", LineType.of(8.0, 3.0, 2.0, 3.0))
                        .linetype("r", LineTypes.DASHED)
                )
                .geoms(line())
                .labs(labs("Manual Linetype per Drive Type", "displacement", "highway mpg"));
    }
}
