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
import static org.jtaccuino.gog.scale.Scales.scaleColorBinned;
import static org.jtaccuino.gog.scale.Scales.scaleColorBrewer;
import static org.jtaccuino.gog.scale.Scales.scaleColorGradient;
import static org.jtaccuino.gog.scale.Scales.scaleColorViridisC;
import static org.jtaccuino.gog.scale.Scales.scaleColorViridisD;
import static org.jtaccuino.gog.scale.Scales.scaleShapeManual;
import static org.jtaccuino.gog.scale.Scales.scaleSizeManual;
import static org.jtaccuino.gog.scale.Scales.scaleXPosition;
import static org.jtaccuino.gog.scale.Scales.scaleXReverse;
import static org.jtaccuino.gog.scale.Scales.scaleYLimits;
import static org.jtaccuino.gog.scale.Scales.scaleYPosition;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.AxisPosition;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Demonstrates the extra scale types added in the scales sprint: reversed axes,
 * named colour ramps (viridis/brewer) and gradients, manual shape/linetype/size
 * maps, axis limits, and binned continuous colour scales.
 */
public class ScalePlots {

    /** Utility class; not meant to be instantiated. */
    private ScalePlots() {
    }

    /**
     * {@return a scatter plot with the X axis running from high to low,
     * mirroring {@code Scales.scaleXReverse()}}
     */
    @SamplePlot(description = "Scatter with the X axis reversed to run from high to low.",
            title = "Reversed X Axis",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.X_REVERSE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createReverseX() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("hwy").y("displ"))
                .geoms(point().size(3.0))
                .scales(scaleXReverse())
                .labs(labs("Reversed X Axis", "highway mpg (descending)", "displacement"));
    }

    /**
     * {@return a scatter plot whose continuous {@code color} scale uses the
     * viridis ramp, {@code Scales.scaleColorViridisC()}}
     */
    @SamplePlot(description = "Scatter coloured by a continuous viridis ramp.",
            title = "Continuous Colour Scale (viridis)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.COLOR_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createViridisC() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("cty"))
                .geoms(point().size(3.0))
                .scales(scaleColorViridisC())
                .labs(labs("Continuous Colour Scale (viridis)", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose continuous {@code color} scale interpolates
     * a low-to-high gradient, {@code Scales.scaleColorGradient(low, high)}}
     */
    @SamplePlot(description = "Scatter coloured by a two-stop low-to-high gradient.",
            title = "Two-Stop Colour Gradient",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.COLOR_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createGradient() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("cty"))
                .geoms(point().size(3.0))
                .scales(scaleColorGradient(Color.web("#fee8c8"), Color.web("#e34a33")))
                .labs(labs("Two-Stop Colour Gradient", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose continuous {@code color} scale is binned
     * into discrete steps with a colorsteps guide, {@code Scales.scaleColorBinned(6)}}
     */
    @SamplePlot(description = "Scatter coloured by six discrete bins with a coloursteps guide.",
            title = "Binned Colour Scale (color steps)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.COLOR_BINNED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBinned() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("cty"))
                .geoms(point().size(3.0))
                .scales(scaleColorBinned(6))
                .labs(labs("Binned Colour Scale (color steps)", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose discrete {@code color} scale uses the
     * viridis palette, {@code Scales.scaleColorViridisD()}}
     */
    @SamplePlot(description = "Scatter coloured by a discrete viridis palette.",
            title = "Discrete Colour Scale (viridis)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.COLOR_DISCRETE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createViridisD() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("cyl"))
                .geoms(point().size(3.0))
                .scales(scaleColorViridisD())
                .labs(labs("Discrete Colour Scale (viridis)", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose discrete {@code color} scale uses a named
     * ColorBrewer palette, {@code Scales.scaleColorBrewer("Dark2")}}
     */
    @SamplePlot(description = "Scatter coloured by the named ColorBrewer Dark2 palette.",
            title = "Discrete Colour Scale (brewer)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.COLOR_BREWER},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBrewer() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("cyl"))
                .geoms(point().size(3.0))
                .scales(scaleColorBrewer("Dark2"))
                .labs(labs("Discrete Colour Scale (brewer)", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose Y axis is clamped to explicit limits,
     * {@code Scales.scaleYLimits(min, max)}}
     */
    @SamplePlot(description = "Scatter with the Y axis clamped to explicit limits.",
            title = "Explicit Y Limits",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.Y_LIMITS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLimits() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(3.0))
                .scales(scaleYLimits(15.0, 45.0))
                .labs(labs("Explicit Y Limits", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose point shapes are assigned explicitly per
     * cylinder count, {@code Scales.scaleShapeManual(values = ...)}}
     */
    @SamplePlot(description = "Scatter with point shapes assigned manually per cylinder count.",
            title = "Manual Shape Scale",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.SHAPE_MANUAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createManualShape() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").shape("cyl"))
                .geoms(point().size(3.0))
                .scales(scaleShapeManual()
                        .shape("4", PointShape.CIRCLE)
                        .shape("5", PointShape.TRIANGLE)
                        .shape("6", PointShape.SQUARE)
                        .shape("8", PointShape.DIAMOND))
                .labs(labs("Manual Shape Scale", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose point radii are assigned explicitly per
     * cylinder count, {@code Scales.scaleSizeManual(values = ...)}}
     */
    @SamplePlot(description = "Scatter with point radii assigned manually per cylinder count.",
            title = "Manual Size Scale",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.SIZE_MANUAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createManualSize() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").size("cyl"))
                .geoms(point())
                .scales(scaleSizeManual()
                        .size("4", 2.5)
                        .size("5", 3.0)
                        .size("6", 4.0)
                        .size("8", 6.0))
                .labs(labs("Manual Size Scale", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose X axis rides along the top edge instead of
     * the bottom, {@code Scales.scaleXContinuous(position = "top")}}
     */
    @SamplePlot(description = "Scatter with the X axis drawn along the top edge.",
            title = "Axis Positions: X on Top",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.X_POSITION},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAxisPositionXTop() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(1.5))
                .scales(scaleXPosition(AxisPosition.TOP))
                .labs(labs("Axis Positions: X on Top", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot whose Y axis rides along the right edge instead
     * of the left, {@code Scales.scaleYContinuous(position = "right")}}
     */
    @SamplePlot(description = "Scatter with the Y axis drawn along the right edge.",
            title = "Axis Positions: Y on the Right",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.Y_POSITION},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAxisPositionYRight() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(1.5))
                .scales(scaleYPosition(AxisPosition.RIGHT))
                .labs(labs("Axis Positions: Y on the Right", "displacement", "highway mpg"));
    }

    /**
     * {@return a scatter plot with the X axis on top and the Y axis on the
     * right, combining {@code Scales.scaleXContinuous(position = "top")} and
     * {@code Scales.scaleYContinuous(position = "right")}}
     */
    @SamplePlot(description = "Scatter with the X axis on top and the Y axis on the right.",
            title = "Axis Positions: X on Top, Y on the Right",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            scales = {SampleScale.X_POSITION, SampleScale.Y_POSITION},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAxisPositionBoth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy"))
                .geoms(point().size(1.5))
                .scales(scaleXPosition(AxisPosition.TOP))
                .scales(scaleYPosition(AxisPosition.RIGHT))
                .labs(labs("Axis Positions: X on Top, Y on the Right", "displacement", "highway mpg"));
    }
}
