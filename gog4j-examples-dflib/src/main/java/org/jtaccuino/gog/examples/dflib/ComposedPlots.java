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
import static org.jtaccuino.gog.Geoms.bar;
import static org.jtaccuino.gog.Geoms.boxplot;
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.composedPlot;
import static org.jtaccuino.gog.Ggplot.ggplot;

import org.dflib.DataFrame;
import org.jtaccuino.gog.ComposedPlot;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.dflib.data.PenguinsDatasets;
import org.jtaccuino.gog.dflib.data.TipsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Gallery factories for {@code composedPlot()} — multi-plot compositions
 * arranging several figures into one aligned grid with a shared title, panel
 * tags, and (optionally) a collected legend.
 */
public class ComposedPlots {

    /** Utility class; not meant to be instantiated. */
    private ComposedPlots() {
    }

    /** {@return a penguins scatter and density side by side} */
    @SamplePlot(description = "Scatter of bill measurements composed beside a flipper-length density curve.",
            title = "Penguins: Flipper Length vs Bill",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createPenguinsScatterDensity() {
        var penguins = PenguinsDatasets.loadPenguins();
        return composedPlot(
                ggplot(penguins, aes().x("bill_length_mm").y("bill_depth_mm"))
                        .geoms(point()),
                ggplot(penguins, aes().x("flipper_length_mm"))
                        .geoms(density()))
                .title("Penguins: Flipper Length vs Bill");
    }

    /** {@return a penguins matrix beside a single density} */
    @SamplePlot(description = "Pairwise penguins matrix composed beside a species-coloured body-mass density.",
            title = "Penguins Matrix + Body Mass Density",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createPenguinsMatrixAndDensity() {
        var penguins = PenguinsDatasets.loadPenguins();
        return composedPlot(
                Ggplot.matrixPlot(penguins, "bill_length_mm", "bill_depth_mm", "flipper_length_mm"),
                ggplot(penguins, aes().x("body_mass_g").color("species"))
                        .geoms(density().alpha(0.4)))
                .title("Penguins Matrix + Body Mass Density");
    }

    /** {@return tips bill histogram above a box plot split by day} */
    @SamplePlot(description = "Bill and tip histograms stacked above day and smoker box plots.",
            title = "Tips: Bill and Tip Distributions",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.HISTOGRAM, SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createTipsBillByDay() {
        var tips = TipsDatasets.loadTips();
        return composedPlot(
                ggplot(tips, aes().x("total_bill")).geoms(histogram()),
                ggplot(tips, aes().x("day").y("total_bill")).geoms(boxplot()))
                .above(composedPlot(
                        ggplot(tips, aes().x("tip")).geoms(histogram()),
                        ggplot(tips, aes().x("smoker").y("tip")).geoms(boxplot())))
                .title("Tips: Bill and Tip Distributions");
    }

    /** {@return a 2×2 tips grid: bill/tip histograms and day/smoker boxes} */
    @SamplePlot(description = "2×2 composition of bill and tip histograms above day and smoker box plots.",
            title = "Tips: 2×2 Summary Grid",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.HISTOGRAM, SampleGeom.BOXPLOT},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createTipsGridBySmoker() {
        var tips = TipsDatasets.loadTips();
        return composedPlot(
                ggplot(tips, aes().x("total_bill")).geoms(histogram()),
                ggplot(tips, aes().x("tip")).geoms(histogram()),
                ggplot(tips, aes().x("day").y("total_bill")).geoms(boxplot()),
                ggplot(tips, aes().x("smoker").y("tip")).geoms(boxplot()))
                .rows(2).cols(2)
                .title("Tips: 2×2 Summary Grid");
    }

    /** {@return a 2×2 mtcars grid} */
    @SamplePlot(description = "2×2 composition of pairwise engine scatters and an mpg histogram.",
            title = "Mtcars: 2×2 Pairwise Grid",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.HISTOGRAM},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createMtcarsGrid() {
        var mtcars = MtcarsDatasets.loadNumericMtcars();
        return composedPlot(
                ggplot(mtcars, aes().x("wt").y("mpg")).geoms(point()),
                ggplot(mtcars, aes().x("hp").y("mpg")).geoms(point()),
                ggplot(mtcars, aes().x("wt").y("hp")).geoms(point()),
                ggplot(mtcars, aes().x("mpg")).geoms(histogram()))
                .rows(2).cols(2)
                .title("Mtcars: 2×2 Pairwise Grid");
    }

    /** {@return a 2×2 mtcars grid with a collected hue legend} */
    @SamplePlot(description = "2×2 composition of cylinder-coloured scatters with a collected legend.",
            title = "Mtcars by Cylinder",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createMtcarsGridLegend() {
        var mtcars = MtcarsDatasets.loadNumericMtcars();
        var legend = Ggplot.plot(mtcars, aes().x("wt").y("mpg").color("cyl"))
                .geoms(point());
        return composedPlot(
                ggplot(mtcars, aes().x("wt").y("mpg").color("cyl")).geoms(point()),
                ggplot(mtcars, aes().x("hp").y("mpg").color("cyl")).geoms(point()))
                .rows(1)
                .title("Mtcars by Cylinder")
                .legend(legend)
                .collectLegends(true);
    }

    /** {@return a 2×2 diamonds grid over the full dataset} */
    @SamplePlot(description = "2×2 composition of carat and price histograms with price scatters.",
            title = "Diamonds: 2×2 Grid (full dataset)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.HISTOGRAM, SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createDiamondsGrid() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        return composedPlot(
                ggplot(diamonds, aes().x("carat")).geoms(histogram()),
                ggplot(diamonds, aes().x("price")).geoms(histogram()),
                ggplot(diamonds, aes().x("carat").y("price")).geoms(point()),
                ggplot(diamonds, aes().x("depth").y("price")).geoms(point()))
                .rows(2).cols(2)
                .title("Diamonds: 2×2 Grid (full dataset)");
    }

    /** {@return a diamonds scatter and density with a collected hue legend} */
    @SamplePlot(description = "Cut-coloured carat-price scatter and density with a collected legend.",
            title = "Diamonds by Cut",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createDiamondsByCutLegend() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        var legend = Ggplot.plot(diamonds, aes().x("carat").y("price").color("cut"))
                .geoms(point());
        return composedPlot(
                ggplot(diamonds, aes().x("carat").y("price").color("cut")).geoms(point()),
                ggplot(diamonds, aes().x("carat").color("cut")).geoms(density().alpha(0.4)))
                .rows(1)
                .title("Diamonds by Cut")
                .legend(legend)
                .collectLegends(true);
    }

    /** {@return a 2×2 mpg grid: scatters and distributions} */
    @SamplePlot(description = "2×2 composition mixing mpg scatters with a displacement histogram and class bars.",
            title = "MPG: 2×2 Grid",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.HISTOGRAM, SampleGeom.BAR},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createMpgClassGrid() {
        var mpg = MpgDatasets.loadMpg();
        return composedPlot(
                ggplot(mpg, aes().x("displ").y("hwy")).geoms(point()),
                ggplot(mpg, aes().x("cty").y("hwy")).geoms(point()),
                ggplot(mpg, aes().x("displ")).geoms(histogram()),
                ggplot(mpg, aes().x("class")).geoms(bar()))
                .rows(2).cols(2)
                .title("MPG: 2×2 Grid");
    }

    /** {@return a 2×2 penguins grid coloured by species with panel tags a–d} */
    @SamplePlot(description = "2×2 species-coloured grid of penguin measurements with panel tags.",
            title = "Penguins by Species",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY, SampleGeom.BAR},
            features = {SampleFeature.TWO_D, SampleFeature.COMPOSITION})
    public static ComposedPlot createPenguinsTaggedGrid() {
        var penguins = PenguinsDatasets.loadPenguins();
        return composedPlot(
                ggplot(penguins, aes().x("bill_length_mm").y("bill_depth_mm").color("species"))
                        .geoms(point()),
                ggplot(penguins, aes().x("flipper_length_mm").y("body_mass_g").color("species"))
                        .geoms(point()),
                ggplot(penguins, aes().x("bill_length_mm").color("species"))
                        .geoms(density().alpha(0.4)),
                ggplot(penguins, aes().x("species")).geoms(bar()))
                .rows(2).cols(2)
                .title("Penguins by Species")
                .subtitle("Palmer Archipelago, 2007–2009")
                .tags(true);
    }
}
