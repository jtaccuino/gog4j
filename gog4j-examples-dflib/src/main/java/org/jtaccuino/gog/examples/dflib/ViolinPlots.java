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
import static org.jtaccuino.gog.Geoms.jitter;
import static org.jtaccuino.gog.Geoms.violin;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.labs.Labs.title;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;

/**
 * Violin plot examples on the fuel-economy (mpg) dataset, following the
 * {@code Geoms.violin()} documentation of the reference: the basic mirrored-density
 * view, flipped orientation, an overlaid jitter strip, area/count/width
 * scaling, tail trimming, bandwidth adjustment, mapped and fixed aesthetics,
 * and quartile marks.
 *
 * @see org.jtaccuino.gog.dflib.data.MpgDatasets
 */
public class ViolinPlots {

    /** Utility class; not meant to be instantiated. */
    private ViolinPlots() {
    }

    /** {@return a basic violin of highway mpg per vehicle class} */
    @SamplePlot(description = "Mirrored density violin of highway MPG for each vehicle class.",
            title = "Highway MPG by Vehicle Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBasicViolin() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin())
                .labs(labs("1. Highway MPG by Vehicle Class", "vehicle class", "highway mpg"));
    }

    /** {@return a horizontal violin of displacement per drive type} */
    @SamplePlot(description = "Horizontal violin of engine displacement per drive type via flipped coordinates.",
            title = "Displacement by Drive Type (horizontal)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            coords = {SampleCoord.FLIP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalViolin() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("drv").y("displ"))
                .geoms(violin().fill(Color.web("#f58518")))
                .coord(Coord2D.flip())
                .labs(labs("2. Displacement by Drive Type (horizontal)", "displacement", "drive type"));
    }

    /** {@return a violin overlaid with a jitter strip of the raw observations} */
    @SamplePlot(description = "Violin plot overlaid with a jitter strip of the raw observations.",
            title = "Highway MPG with Jittered Points",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN, SampleGeom.JITTER},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createViolinJitter() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(
                    violin(),
                    jitter().size(4.0).opacity(0.6)
                )
                .labs(labs("3. Highway MPG with Jittered Points", "vehicle class", "highway mpg"));
    }

    /** {@return a violin with area proportional to sample size (scale = 'count')} */
    @SamplePlot(description = "Violin widths proportional to per-class sample size via scale = 'count'.",
            title = "Violin Width Proportional to Sample Size (scale = 'count')",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createScaleCount() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().scale("count").fill(Color.web("#74c476")))
                .labs(labs("4. Violin Width Proportional to Sample Size (scale = 'count')",
                        "vehicle class", "highway mpg"));
    }

    /** {@return a violin with uniform maximum width (scale = 'width')} */
    @SamplePlot(description = "Violins with a uniform maximum width via scale = 'width'.",
            title = "Uniform Maximum Width (scale = 'width')",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createScaleWidth() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().scale("width").fill(Color.web("#8c6daf")))
                .labs(labs("5. Uniform Maximum Width (scale = 'width')",
                        "vehicle class", "highway mpg"));
    }

    /** {@return a violin with untrimmed kernel tails (trim = false)} */
    @SamplePlot(description = "Violins with untrimmed kernel tails extending beyond the data range.",
            title = "Untrimmed Tails (trim = false)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createTrimFalse() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().trim(false).fill(Color.web("#d4a017")))
                .labs(labs("6. Untrimmed Tails (trim = false)", "vehicle class", "highway mpg"));
    }

    /** {@return a violin with a closer density fit (adjust = 0.5)} */
    @SamplePlot(description = "Violins with a tighter density fit from halved bandwidth smoothing.",
            title = "Closer Density Fit (adjust = 0.5)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAdjustHalf() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().adjust(0.5).fill(Color.web("#6baed6")))
                .labs(labs("7. Closer Density Fit (adjust = 0.5)", "vehicle class", "highway mpg"));
    }

    /** {@return a violin split by drive type, dodged per vehicle class} */
    @SamplePlot(description = "Violins dodged per vehicle class with fill mapped to drive type.",
            title = "Fill Mapped to Drive Type",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFillMapped() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy").fill("drv"))
                .geoms(violin().position(Position.DODGE))
                .labs(labs("8. Fill Mapped to Drive Type", "vehicle class", "highway mpg"));
    }

    /** {@return a violin with fixed grey fill and blue outline} */
    @SamplePlot(description = "Violin with a fixed grey fill and a blue outline.",
            title = "Fixed Fill and Outline",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFixedStyle() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().fill(Color.web("#cccccc")).color(Color.web("#3366FF")))
                .labs(title("9. Fixed Fill and Outline")
                        .xLabel("vehicle class")
                        .yLabel("highway mpg"));
    }

    /** {@return a violin with quartile marks at the 25th, 50th, and 75th percentiles} */
    @SamplePlot(description = "Violin with horizontal marks at the 25th, 50th, and 75th percentiles.",
            title = "Quartile Marks (quantileDrawing = c(0.25, 0.5, 0.75))",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.VIOLIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDrawQuantiles() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy"))
                .geoms(violin().drawQuantiles(0.25, 0.5, 0.75).fill(Color.web("#9ecae1"))
                        .color(Color.web("#3182bd")))
                .labs(labs("10. Quartile Marks (quantileDrawing = c(0.25, 0.5, 0.75))",
                        "vehicle class", "highway mpg"));
    }
}
