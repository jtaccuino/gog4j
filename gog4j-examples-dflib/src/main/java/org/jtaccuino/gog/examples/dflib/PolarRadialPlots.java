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
import static org.jtaccuino.gog.Coords.coordRadial;
import static org.jtaccuino.gog.Geoms.boxplot;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.text;
import static org.jtaccuino.gog.Geoms.violin;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Mode;
import org.jtaccuino.gog.coord.ScaleExpansion;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.dflib.data.PolarDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;

/**
 * The {@code coordRadial()} figures of the release notes
 * ("Introducing: coordRadial()", the reference documentation, 2024-03): the partial polar
 * fans, the donut with a hollow centre, the expanded pie variants, and the
 * rotated-text wind rose that the new coordinate system makes possible.
 *
 * @see org.jtaccuino.gog.examples.dflib.PolarPlots
 * @see org.jtaccuino.gog.dflib.data.PolarDatasets
 */
public class PolarRadialPlots {

    /** Utility class; not meant to be instantiated. */
    private PolarRadialPlots() {
    }

    /** {@return a radial scatter on a half-circle fan} */
    @SamplePlot(description = "Radial scatter constrained to a half-circle fan.",
            title = "Half Circle (coordRadial −0.5π to +0.5π)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.RADIAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHalfCircle() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(3.0))
                .coord(coordRadial().start(-0.5 * Math.PI).end(0.5 * Math.PI))
                .labs(labs("8. Half Circle (coordRadial −0.5π to +0.5π)", "Displ", "Hwy"));
    }

    /** {@return a radial scatter on a quarter-circle fan} */
    @SamplePlot(description = "Radial scatter constrained to a quarter-circle fan.",
            title = "Quarter Circle (coordRadial 0 to +0.5π)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.RADIAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createQuarterCircle() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(3.0))
                .coord(coordRadial().start(0.0).end(0.5 * Math.PI))
                .labs(labs("9. Quarter Circle (coordRadial 0 to +0.5π)", "Displ", "Hwy"));
    }

    /** {@return a donut scatter with a hollow centre and inside axis labels} */
    @SamplePlot(description = "Radial scatter with a hollow centre and inside axis labels.",
            title = "Donut (inner.radius 0.3, radialAxisInside)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.RADIAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDonutScatter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(3.0))
                .coord(coordRadial().innerRadius(0.3).rAxisInside(Mode.YES))
                .labs(labs("10. Donut (inner.radius 0.3, radialAxisInside)", "Displ", "Hwy"));
    }

    /** {@return the radial pie with default scale expansion} */
    @SamplePlot(description = "Stacked radial bar pie with default scale expansion.",
            title = "Radial Pie (default expansion)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.RADIAL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialPieDefault() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordRadial().theta("y"))
                .labs(labs("11. Radial Pie (default expansion)", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return the radial pie filling the disc with no expansion} */
    @SamplePlot(description = "Stacked radial pie filling the disc with no expansion.",
            title = "Radial Pie (expand = FALSE)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.RADIAL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialPieNoExpand() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordRadial().theta("y").expand(ScaleExpansion.OFF))
                .labs(labs("12. Radial Pie (expand = FALSE)", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return the clean radial pie with both axes hidden} */
    @SamplePlot(description = "Clean radial pie with both axes hidden.",
            title = "Radial Pie (axes hidden)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.RADIAL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialPieClean() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordRadial().theta("y").expand(ScaleExpansion.OFF).showThetaAxis(false).showRadialAxis(false))
                .labs(labs("13. Radial Pie (axes hidden)", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return a wind rose of the mtcars cars with rotated rim labels} */
    @SamplePlot(description = "Wind rose with stacked bars and rotated rim labels.",
            title = "Wind Rose (rotateAngle = TRUE)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL, SampleGeom.TEXT},
            coords = {SampleCoord.RADIAL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createWindRose() {
        var df = MtcarsDatasets.loadNamedMtcars();
        return ggplot(df,
                        aes().x("car").y("mpg").label("car"))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .geoms(text().size(9.0).angle(90).hjust(1).y(32.0).nudge(0, 0).avoidOverlap(false))
                .coord(coordRadial().rotateAngle(true).expand(ScaleExpansion.OFF).showThetaAxis(false))
                .labs(labs("14. Wind Rose (rotateAngle = TRUE)", "Car", "MPG"));
    }

    /** {@return a radial boxplot of displacement per car class} */
    @SamplePlot(description = "Radial boxplot of displacement per car class.",
            title = "Radial Boxplot (theta labels tangent)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            coords = {SampleCoord.RADIAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("displ"))
                .geoms(boxplot())
                .coord(coordRadial().start(0.25 * Math.PI).end(1.75 * Math.PI).thetaLabelAngle(0))
                .labs(labs("15. Radial Boxplot (theta labels tangent)", "Class", "Displ"));
    }

    /** {@return a radial violin of displacement per car class} */
    @SamplePlot(description = "Radial violin of displacement per car class.",
            title = "Radial Violin (theta labels tangent)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            coords = {SampleCoord.RADIAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialViolin() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("displ"))
                .geoms(violin())
                .coord(coordRadial().start(0.25 * Math.PI).end(1.75 * Math.PI).thetaLabelAngle(0))
                .labs(labs("16. Radial Violin (theta labels tangent)", "Class", "Displ"));
    }
}
