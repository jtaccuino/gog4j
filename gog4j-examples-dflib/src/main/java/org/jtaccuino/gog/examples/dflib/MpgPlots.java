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
import static org.jtaccuino.gog.Geoms.boxplot;
import static org.jtaccuino.gog.Geoms.jitter;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.labs.Labs.title;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Example plot definitions using the fuel-economy (mpg) dataset.
 * <p>
 * Demonstrates 3D scatter plots, faceted smooth regressions, categorical
 * colour/shape mappings, and the use of {@link MpgDatasets}.
 */
public class MpgPlots {

    /** Utility class; not meant to be instantiated. */
    private MpgPlots() {
    }

    /** {@return a LOESS-smoothed scatter plot of displacement vs highway mpg} */
    @SamplePlot(description = "Points overlaid with a LOESS smoothing fit of displacement versus highway mpg.",
            title = "Smoothed Conditional Means (LOESS)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStandardSmooth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(
                    point(),
                    smooth()
                )
                .labs(labs("1. Smoothed Conditional Means (LOESS)", "displacement", "horsepower"));
    }

    /** {@return a scatter plot with a wigglier LOESS smooth (span = 0.3)} */
    @SamplePlot(description = "Points with a wigglier LOESS smooth using a span of 0.3.",
            title = "Wigglier Curve (span=0.3)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createWiggleySmooth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(
                    point(),
                    smooth(0.3)
                )
                .labs(labs("2. Wigglier Curve (span=0.3)", "displacement", "horsepower"));
    }

    /** {@return a scatter plot with a trend line and no confidence band} */
    @SamplePlot(description = "Scatter points with a linear trend line and no confidence band.",
            title = "Trendline Without Confidence Interval",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNoSeSmooth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(
                    point(),
                    smooth().method(SmoothMethod.LM).se(false)
                )
                .labs(labs("3. Trendline Without Confidence Interval", "displacement", "horsepower"));
    }

    /** {@return a scatter plot with per-drive-type linear models} */
    @SamplePlot(description = "Points and linear model fits colored and shaped by drive type.",
            title = "Points & Linear Models per Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLinearModelGrouped() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").shape("drv"))
                .geoms(
                    point(),
                    smooth(SmoothMethod.LM, true).fill(Color.web("#999999"))
                )
                .labs(labs("4. Points & Linear Models per Drive Type", "displacement", "horsepower"));
    }

    /** {@return a scatter plot with per-drive-type local smooth mappings} */
    @SamplePlot(description = "Points with a per-geom linear smooth colored by drive type.",
            title = "Global Points with Local Smooth Mappings",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLocalAesSmooth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(
                    point().size(3.0),
                    smooth(aes().color("drv")).method(SmoothMethod.LM)
                )
                .labs(labs("5. Global Points with Local Smooth Mappings", "displacement", "highway efficiency"));
    }

    /**
     * {@return a plot whose two {@code smooth()} layers each carry their own
     * per-geom aesthetic, so both colour mappings coexist on one shared global
     * {@code aes}}
     * <p>
     * The global mapping only declares {@code x}/{@code y}; each smooth layer
     * overrides {@code color} with a different grouping column. This is only
     * possible because per-geom aesthetics are honoured: both group-coloured
     * regressions render against the same plot on their own mappings.
     */
    @SamplePlot(description = "Points with two smooth layers, each carrying its own color mapping.",
            title = "Two Local Smooth Mappings",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createTwoLocalSmoothMappings() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(
                    point().size(2.0),
                    smooth(aes().color("drv")).method(SmoothMethod.LM),
                    smooth(aes().color("cyl")).se(false).span(0.35)
                )
                .labs(labs("20. Two Local Smooth Mappings", "displacement", "highway efficiency"));
    }

    /** {@return a scatter plot mapping both colour and shape to drive type} */
    @SamplePlot(description = "Points mapping both color and shape to drive type.",
            title = "Dual Aesthetic Mapping (Color & Shape via Aes)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDualMappingPlot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").shape("drv"))
                .geoms(
                    point().size(10.0)
                )
                .labs(title("6. Dual Aesthetic Mapping (Color & Shape via Aes)")
                        .xLabel("Engine Displacement (L)")
                        .yLabel("Highway MPG")
                        .map("f", "Front")
                        .map("r", "Rear")
                        .map("4", "4WD"));
    }

    /** {@return a 3D scatter plot of displacement vs highway mpg, z = drive type} */
    @SamplePlot(description = "Three-dimensional scatter of displacement, highway mpg, and drive type.",
            title = "3D Scatterplot",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> create3dScatterPlot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").z("drv").color("class"))
                .geoms(point3d().size(6.0))
                .coord(Coords.coord3d())
                .labs(labs("7. 3D Scatterplot", "displacement", "highway"));
    }

    /** {@return a boxplot of highway mpg per vehicle class} */
    @SamplePlot(description = "Boxplot of highway mpg summarized per vehicle class.",
            title = "Highway MPG by Vehicle Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(boxplot().fill(Color.web("#4c78a8")))
                .labs(labs("8. Highway MPG by Vehicle Class", "vehicle class", "highway mpg"));
    }

    /** {@return a horizontal boxplot of displacement per drive type} */
    @SamplePlot(description = "Horizontal boxplot of displacement per drive type with flipped coordinates.",
            title = "Displacement by Drive Type (horizontal)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            coords = {SampleCoord.FLIP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("drv").y("displ"))
                .geoms(boxplot().fill(Color.web("#f58518")))
                .coord(Coord2D.flip())
                .labs(labs("9. Displacement by Drive Type (horizontal)", "drive type", "displacement"));
    }

    /** {@return a notched boxplot of highway mpg per vehicle class} */
    @SamplePlot(description = "Notched boxplot of highway mpg with confidence-interval notches.",
            title = "Notched Highway MPG by Vehicle Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNotchedBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(boxplot().fill(Color.web("#8c6daf")).notch(true))
                .labs(labs("10. Notched Highway MPG by Vehicle Class", "vehicle class", "highway mpg"));
    }

    /** {@return a variable-width boxplot of highway mpg per vehicle class} */
    @SamplePlot(description = "Boxplot with box widths proportional to vehicle class size.",
            title = "Variable-Width Highway MPG by Vehicle Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createVarWidthBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(boxplot().fill(Color.web("#74c476")).varwidth(true))
                .labs(labs("11. Variable-Width Highway MPG by Vehicle Class", "vehicle class", "highway mpg"));
    }

    /** {@return a boxplot with diamond outlier markers} */
    @SamplePlot(description = "Boxplot with diamond-shaped outlier markers.",
            title = "Highway MPG with Diamond Outliers",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createOutlierShapeBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(boxplot().fill(Color.web("#d4a017"))
                        .outlierShape(PointShape.DIAMOND).outlierSize(3.5))
                .labs(labs("12. Highway MPG with Diamond Outliers", "vehicle class", "highway mpg"));
    }

    /** {@return a boxplot with outliers suppressed} */
    @SamplePlot(description = "Boxplot with outliers suppressed to show only the whisker summary.",
            title = "Highway MPG without Outliers",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNoOutlierBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(boxplot().fill(Color.web("#6baed6")).drawOutliers(false))
                .labs(labs("13. Highway MPG without Outliers", "vehicle class", "highway mpg"));
    }

    /** {@return a boxplot overlaid with jittered raw observations} */
    @SamplePlot(description = "Boxplot overlaid with jittered raw observations.",
            title = "Highway MPG with Jittered Points",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT, SampleGeom.JITTER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBoxplotJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(
                    boxplot().fill(Color.web("#9ecae1")).drawOutliers(false),
                    jitter().size(5.0).opacity(0.6)
                )
                .labs(labs("14. Highway MPG with Jittered Points", "vehicle class", "highway mpg"));
    }

    /** {@return the default jitter plot of cylinders vs highway mpg} */
    @SamplePlot(description = "Jittered points showing the distribution of cylinders versus highway mpg.",
            title = "Default Jitter (cyl vs hwy)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDefaultJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cyl").y("hwy"))
                .geoms(jitter().size(4.0).opacity(0.6))
                .labs(labs("15. Default Jitter (cyl vs hwy)", "cylinders", "highway mpg"));
    }

    /** {@return a jitter plot of cylinders vs highway mpg coloured by class} */
    @SamplePlot(description = "Jittered points with color mapped to vehicle class.",
            title = "Jitter Coloured by Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColoredJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cyl").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .labs(labs("16. Jitter Coloured by Class", "cylinders", "highway mpg"));
    }

    /** {@return a jitter plot with a narrow width (0.25)} */
    @SamplePlot(description = "Jittered points with a narrow horizontal spread of 0.25.",
            title = "Narrow Jitter (width=0.25)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createNarrowJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cyl").y("hwy"))
                .geoms(jitter().size(4.0).width(0.25).opacity(0.6))
                .labs(labs("17. Narrow Jitter (width=0.25)", "cylinders", "highway mpg"));
    }

    /** {@return a jitter plot with a wide width and height (0.5)} */
    @SamplePlot(description = "Jittered city mpg versus highway mpg with equal width and height spreads.",
            title = "Wide Jitter (width=0.5, height=0.5)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createWideJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cty").y("hwy"))
                .geoms(jitter().size(4.0).width(0.5).height(0.5).opacity(0.6))
                .labs(labs("18. Wide Jitter (width=0.5, height=0.5)", "city mpg", "highway mpg"));
    }

    /** {@return a boxplot filled by drive type} */
    @SamplePlot(description = "Boxplots filled by drive type and dodged within vehicle class.",
            title = "Boxplot Filled by Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFillMappedBoxplot() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy").fill("drv"))
                .geoms(boxplot().position(Position.DODGE).dodgeWidth(0.9))
                .labs(labs("19. Boxplot Filled by Drive Type", "vehicle class", "highway mpg")
                        .map("4", "4-Wheel Drive")
                        .map("f", "Front-Wheel Drive")
                        .map("r", "Rear-Wheel Drive"));
    }
}
