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
import static org.jtaccuino.gog.Facets.wrap;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Facets;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Example plot definitions using the diamonds dataset.
 * <p>
 * Demonstrates scatter plots, faceted distributions, smooth regression lines,
 * categorical color/shape mappings, and the use of {@link DiamondsDatasets}.
 */
public class DiamondsPlots {

    /** Utility class; not meant to be instantiated. */
    private DiamondsPlots() {
    }

    /** {@return a scatter plot of diamond price vs carat} */
    @SamplePlot(description = "Scatter of diamond price against carat weight.",
            title = "Diamond Price vs Carat",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createScatterPlot() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point())
                .labs(labs("1. Diamond Price vs Carat", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat coloured by clarity} */
    @SamplePlot(description = "Scatter coloured by clarity to reveal the price spread.",
            title = "Colored by Clarity",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColorByClarity() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("clarity"))
                .geoms(point().size(2.0))
                .labs(labs("2. Colored by Clarity", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat faceted by clarity} */
    @SamplePlot(description = "Scatter with a smooth fit wrapped by clarity.",
            title = "Faceted by Clarity",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetByClarity() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(1.5),
                        smooth())
                .facets(wrap("clarity", 2))
                .labs(labs("3. Faceted by Clarity", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat faceted by color} */
    @SamplePlot(description = "Scatter faceted by diamond color in a wrap grid.",
            title = "Faceted by Color",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetByColor() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(1.5))
                .facets(wrap("color", 4))
                .labs(labs("4. Faceted by Color", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat with a LOESS smooth} */
    @SamplePlot(description = "Scatter with a LOESS smoothing fit of price on carat.",
            title = "LOESS Smooth",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSmoothLoess() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(1.5).color(Color.web("#333333", 0.3)), smooth())
                .labs(labs("5. LOESS Smooth", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat with a linear-model smooth} */
    @SamplePlot(description = "Scatter with a linear-model smoothing fit of price on carat.",
            title = "Linear Model Smooth",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSmoothLinear() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(1.5).color(Color.web("#333333", 0.3)), smooth(SmoothMethod.LM, true))
                .labs(labs("6. Linear Model Smooth", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat with alpha transparency} */
    @SamplePlot(description = "Scatter with fixed low alpha to show point density.",
            title = "Alpha=0.15 (Opacity)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createAlphaPlot() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().color(Color.web("#333333", 0.15)).size(2.0))
                .labs(labs("7. Alpha=0.15 (Opacity)", "Carat", "Price"));
    }

    /** {@return a scatter plot of price vs carat faceted by clarity and coloured by cut} */
    @SamplePlot(description = "Scatter faceted by clarity and coloured by cut.",
            title = "Facet: Clarity, Color: Cut",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetClarityColorCut() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut"))
                .geoms(point().size(1.5))
                .facets(wrap("clarity", 4))
                .labs(labs("8. Facet: Clarity, Color: Cut", "Carat", "Price"));
    }

    /** {@return a large free-scale grid of price vs carat across cut × clarity} */
    @SamplePlot(description = "Scatter in a cut-by-clarity grid with free scales.",
            title = "Facet grid: Cut × Clarity, free scales",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.FREE_SCALES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetCutClarityGrid() {
        var df = DiamondsDatasets.loadDiamonds();
        var opts = GridOptions.defaults().withScale(GridOptions.Scale.FREE);
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(1.5))
                .facets(Facets.grid("cut", "clarity", opts))
                .labs(labs("9. Facet grid: Cut (rows) × Clarity (cols), free scales", "Carat", "Price"));
    }

    /** {@return a scatter with one LOESS smooth per cut group, trained on global data (Case A)} */
    @SamplePlot(description = "Scatter with one LOESS smooth per cut group.",
            title = "Grouped LOESS by Cut",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createGroupedSmoothByCut() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut"))
                .geoms(point().size(1.5), smooth())
                .labs(labs("10. Grouped LOESS by Cut", "Carat", "Price"));
    }

    /** {@return a scatter of price vs carat coloured by clarity with a fixed sub-1 opacity} */
    @SamplePlot(description = "Scatter coloured by clarity at half opacity.",
            title = "Opacity 0.5, Color by Clarity",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createOpacityColorByClarity() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("clarity"))
                .geoms(point().size(2.0).opacity(0.5))
                .labs(labs("11. Opacity 0.5, Color by Clarity", "Carat", "Price"));
    }

    /** {@return a scatter of price vs carat with a fixed constant color and fixed sub-1 opacity} */
    @SamplePlot(description = "Scatter in a constant colour at half opacity.",
            title = "Opacity 0.5, Constant Color",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createOpacityConstant() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price"))
                .geoms(point().size(2.0).color(Color.web("#3182bd")).opacity(0.5))
                .labs(labs("12. Opacity 0.5, Constant Color", "Carat", "Price"));
    }
}
