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

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Facets;
import org.jtaccuino.gog.Geoms;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Example plots exercising the {@code Facets.grid()} layout: a matrix of
 * panels spanned by row and column faceting variables, with free scales,
 * free space, margins, strip switching, and data-driven reference lines.
 */
public class FacetGridPlots {

    private static final GridOptions OPT = GridOptions.defaults();

    private FacetGridPlots() {
    }

    /**
     * 1. {@code Facets.grid(rows = drv, cols = cyl)}: the canonical two-way grid,
     * rows of drive type against columns of cylinder count.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points of engine displacement versus highway mpg in a two-way drv-by-cyl grid.",
            title = "Facets.grid(rows = drv, cols = cyl)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDrvCylGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl"))
                .labs(labsFor("1. Facets.grid(rows = drv, cols = cyl)"));
    }

    /**
     * 2. {@code Facets.grid(rows = cyl, cols = drv)}: the same grid with the row
     * and column variables swapped.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Point grid with row and column faceting variables swapped.",
            title = "Facets.grid(rows = cyl, cols = drv)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCylDrvGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("cyl", "drv"))
                .labs(labsFor("2. Facets.grid(rows = cyl, cols = drv)"));
    }

    /**
     * 3. {@code Facets.grid(rows = drv, cols = cyl, scales = "free")}: each panel
     * owns its own axis range instead of sharing a common one.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points faceted by drv and cyl with independent axis ranges per panel.",
            title = "Facets.grid(rows = drv, cols = cyl, scales = \"free\")",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.FREE_SCALES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFreeScalesGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl", OPT.withScale(GridOptions.Scale.FREE)))
                .labs(labsFor("3. Facets.grid(rows = drv, cols = cyl, scales = \"free\")"));
    }

    /**
     * 4. {@code Facets.grid(rows = drv, cols = cyl, scales = "free", space = "free")}:
     * both the axis ranges and the panel widths are free.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points with free axis ranges and panel widths sized to each cell.",
            title = "Facets.grid(rows = drv, cols = cyl, scales = \"free\", space = \"free\")",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.FREE_SCALES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFreeScalesFreeSpaceGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl", OPT.withScale(GridOptions.Scale.FREE)
                        .withSpace(GridOptions.Space.FREE)))
                .labs(labsFor("4. Facets.grid(rows = drv, cols = cyl, scales = \"free\", space = \"free\")"));
    }

    /**
     * 5. {@code Facets.grid(rows = vars(vs, am), cols = vars(gear))}: multiple
     * row variables produce one nested dimension per variable.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points nested across two row variables and one column variable.",
            title = "Facets.grid(rows = vars(vs, am), cols = vars(gear))",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createVsAmGearGrid() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return Ggplot.ggplot(df, Aes.aes().x("disp").y("mpg"))
                .geoms(Geoms.point().size(2.0))
                .facets(Facets.grid(List.of("vs", "am"), List.of("gear"), OPT))
                .labs(labsForMtcars("5. Facets.grid(rows = vars(vs, am), cols = vars(gear))"));
    }

    /**
     * 6. {@code Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = TRUE)}:
     * aggregate row and column panels summarise across each margin.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with aggregate row, column, and grand-total margin panels.",
            title = "Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = TRUE)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMarginsTrueGrid() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return Ggplot.ggplot(df, Aes.aes().x("disp").y("mpg"))
                .geoms(Geoms.point().size(2.0))
                .facets(Facets.grid(List.of("vs", "am"), List.of("gear"), OPT.withMargins()))
                .labs(labsForMtcars("6. Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = TRUE)"));
    }

    /**
     * 7. {@code Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = "am")}:
     * a single named margin variable, so only the {@code am} dimension aggregates.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with margins computed over the am dimension only.",
            title = "Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = \"am\")",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMarginsAmGrid() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return Ggplot.ggplot(df, Aes.aes().x("disp").y("mpg"))
                .geoms(Geoms.point().size(2.0))
                .facets(Facets.grid(List.of("vs", "am"), List.of("gear"), OPT.withMargins("am")))
                .labs(labsForMtcars("7. Facets.grid(rows = vars(vs, am), cols = vars(gear), margins = \"am\")"));
    }

    /**
     * 8. {@code Facets.grid(rows = cyl, cols = drv, as.table = FALSE)}: row
     * levels run bottom-up instead of top-down.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with row levels ordered bottom-up instead of top-down.",
            title = "Facets.grid(rows = cyl, cols = drv, as.table = FALSE)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.AS_TABLE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAsTableFalseGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("cyl", "drv", OPT.withAsTable(false)))
                .labs(labsFor("8. Facets.grid(rows = cyl, cols = drv, as.table = FALSE)"));
    }

    /**
     * 9. {@code Facets.grid(rows = drv, cols = cyl, switch = "both")}: strip
     * labels move to the outside edges of the grid.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with strip labels relocated to the outer panel edges.",
            title = "Facets.grid(rows = drv, cols = cyl, switch = \"both\")",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.SWITCH_AXES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSwitchBothGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl", OPT.withSwitch(GridOptions.GridSwitch.BOTH)))
                .labs(labsFor("9. Facets.grid(rows = drv, cols = cyl, switch = \"both\")"));
    }

    /**
     * 10. {@code Facets.grid(rows = year, cols = cyl)} with a data-driven
     * {@code Geoms.hline} per cylinder panel.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points faceted by year and cylinder with a data-driven mean hline per panel.",
            title = "Facets.grid(rows = year, cols = cyl) + per-cylinder hline",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE},
            facets = {SampleFacet.GRID},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createGridHline() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5),
                        Geoms.hline().data(meanHwyByCyl()).yintercept("hwy").color(Color.web("#e5552e")))
                .facets(Facets.grid("year", "cyl"))
                .labs(labsFor("10. Facets.grid(rows = year, cols = cyl) + per-cylinder hline"));
    }

    /**
     * 11. {@code Facets.grid(rows = drv, cols = cyl, switch = "x")}: the column
     * strips move to the bottom and the x-axis stands on the top row, with tick
     * labels and the axis title above the panels.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with column strips moved to the bottom and the x-axis on top.",
            title = "Facets.grid(rows = drv, cols = cyl, switch = \"x\")",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.SWITCH_AXES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSwitchXGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl", OPT.withSwitch(GridOptions.GridSwitch.X)))
                .labs(labsFor("11. Facets.grid(rows = drv, cols = cyl, switch = \"x\")"));
    }

    /**
     * 12. {@code Facets.grid(rows = drv, cols = cyl, switch = "y")}: the row
     * strips move to the left and the y-axis stands on the right column, with
     * tick labels and the axis title to the right of the panels.
     *
     * @return the grid plot
     */
    @SamplePlot(description = "Points grid with row strips moved to the left and the y-axis on the right.",
            title = "Facets.grid(rows = drv, cols = cyl, switch = \"y\")",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            facets = {SampleFacet.GRID, SampleFacet.SWITCH_AXES},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSwitchYGrid() {
        var df = MpgDatasets.loadMpg();
        return Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("drv", "cyl", OPT.withSwitch(GridOptions.GridSwitch.Y)))
                .labs(labsFor("12. Facets.grid(rows = drv, cols = cyl, switch = \"y\")"));
    }

    /**
     * The mean highway mileage per cylinder count, used by the per-cylinder
     * hline example to draw one horizontal line in each cylinder panel.
     *
     * @return a two-column frame of cylinder count and mean hwy
     */
    public static DataFrame meanHwyByCyl() {
        return DataFrame.byColumn("cyl", "hwy").of(
                Series.ofDouble(4, 5, 6, 8),
                Series.ofDouble(28.8025, 28.75, 22.8228, 17.6286));
    }

    private static LabsSpec labsFor(String title) {
        return Labs.labs(title, "engine displacement (litres)", "highway mpg");
    }

    private static LabsSpec labsForMtcars(String title) {
        return Labs.labs(title, "engine displacement (cu in)", "miles per gallon");
    }
}
