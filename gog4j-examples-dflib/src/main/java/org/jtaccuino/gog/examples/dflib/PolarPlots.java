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
import static org.jtaccuino.gog.Coords.coordPolar;
import static org.jtaccuino.gog.Coords.coordRadial;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleXContinuous;
import static org.jtaccuino.gog.scale.Scales.scaleYContinuous;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.dflib.data.PolarDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.Expansion;

/**
 * {@code coordPolar()} figures, following the reference: the x
 * aesthetic maps to the angle by default, turning bars into the wedges of a
 * coxcomb; {@code theta("y")} swaps the roles so the y values become the
 * angles of a pie.
 *
 * @see org.jtaccuino.gog.dflib.data.PolarDatasets
 */
public class PolarPlots {

    /** Utility class; not meant to be instantiated. */
    private PolarPlots() {
    }

    /** {@return a basic coxcomb (rose) plot of cylinder counts} */
    @SamplePlot(description = "Bars of cylinder counts wrapped as coxcomb wedges with x mapped to angle.",
            title = "Coxcomb (coordPolar)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCoxcomb() {
        var df = PolarDatasets.loadCylCounts();
        return ggplot(df, aes().x("cyl").y("count"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0).fill(Color.web("#9ecae1")))
                .coord(coordPolar())
                .labs(labs("1. Coxcomb (coordPolar)", "Cylinders", "Count"));
    }

    /** {@return a coxcomb plot with wedges coloured by cylinder count} */
    @SamplePlot(description = "Coxcomb wedges filled by cylinder count with a relabelled legend.",
            title = "Coxcomb Coloured by Cylinder",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColouredCoxcomb() {
        var df = PolarDatasets.loadCylCounts();
        return ggplot(df, aes().x("cyl").y("count").fill("cyl"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordPolar())
                .labs(labs("2. Coxcomb Coloured by Cylinder", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return a pie chart of cylinder counts (coordPolar(theta = 'y'))} */
    @SamplePlot(description = "Pie chart of cylinder counts with y values mapped to angles.",
            title = "Pie (coordPolar(theta = 'y'))",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createPie() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordPolar().theta("y"))
                .labs(labs("3. Pie (coordPolar(theta = 'y'))", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return a bulls-eye plot of stacked cylinder counts} */
    @SamplePlot(description = "Stacked cylinder counts as concentric bulls-eye rings around the centre.",
            title = "Bulls-eye (coordPolar)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBullseye() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordPolar())
                .labs(labs("4. Bulls-eye (coordPolar)", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return a donut chart of cylinder counts with a hollow centre} */
    @SamplePlot(description = "Pie chart of cylinder counts hollowed out by an inner radius.",
            title = "Donut (inner radius)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDonut() {
        var df = PolarDatasets.loadPieFrame();
        return ggplot(df, aes().x("label").y("count").fill("cyl"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordPolar().theta("y").innerRadius(0.5))
                .labs(labs("5. Donut (inner radius)", "Cylinders", "Count")
                        .legendTitle("Cylinders")
                        .map("4", "4 cylinders")
                        .map("6", "6 cylinders")
                        .map("8", "8 cylinders"));
    }

    /** {@return a stacked rose of diamond cuts by clarity} */
    @SamplePlot(description = "Stacked rose of diamond clarity counts with wedges filled by cut.",
            title = "Stacked Rose (diamonds: cut by clarity)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStackedRose() {
        var df = PolarDatasets.loadClarityCutCounts();
        return ggplot(df, aes().x("clarity").y("count").fill("cut"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .coord(coordPolar())
                .labs(labs("6. Stacked Rose (diamonds: cut by clarity)", "Clarity", "Count")
                        .legendTitle("Cut"));
    }

    /** {@return a radial scatter fan of displacement vs fuel economy} */
    @SamplePlot(description = "Scatter of displacement vs fuel economy on a partial radial fan.",
            title = "Radial Scatter (coordRadial)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.RADIAL},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRadialScatter() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return ggplot(df, aes().x("disp").y("mpg"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(point().size(3.5))
                .coord(coordRadial().start(-0.4 * Math.PI).end(0.4 * Math.PI).innerRadius(0.3))
                .labs(labs("7. Radial Scatter (coordRadial)", "Disp", "MPG"));
    }
}
