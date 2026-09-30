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
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.matrixPlot;

import org.dflib.DataFrame;
import org.jtaccuino.gog.PlotMatrix;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.dflib.data.PenguinsDatasets;
import org.jtaccuino.gog.dflib.data.TipsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * {@code matrixPlot()} examples, mirroring the gesture of a generalized pairs plot:
 * an <em>n</em>×<em>n</em> matrix over several variables. Every cell is
 * dispatched from the variables' column types: numeric pairs get densities,
 * scatters and correlations, mixed pairs get box plots and faceted histograms,
 * and categorical pairs get count bars.
 * <p>
 * When the base {@code aes} maps a legend-bearing aesthetic (e.g.
 * {@code hue = "species"}) the whole matrix renders a single shared legend to
 * the right of the grid — the default
 * {@link org.jtaccuino.gog.PlotMatrix#legendCell legend} placement.
 *
 * @see org.jtaccuino.gog.PlotMatrix
 * @see org.jtaccuino.gog.Ggplot#matrixPlot(Object, String...)
 */
public class MatrixPlots {

    /** Utility class; not meant to be instantiated. */
    private MatrixPlots() {
    }

    /** The four Palmer Penguin body measurements spanned by the matrix. */
    private static final String[] COLUMNS = {
            "bill_length_mm", "bill_depth_mm", "flipper_length_mm", "body_mass_g"
    };

    /** {@return a penguins scatter-plot matrix for the four body measurements} */
    @SamplePlot(description = "Scatter-plot matrix over the four penguin body measurements.",
            title = "Penguins Pair Plot",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createPenguinsMatrix() {
        var penguins = PenguinsDatasets.loadPenguins();
        return matrixPlot(penguins, COLUMNS).title("Penguins Pair Plot");
    }

    /**
     * {@return a penguins scatter-plot matrix coloured by species, after the
     *          a scatter-plot matrix a scatter-plot matrix example}
     */
    @SamplePlot(description = "Matrix coloured by species with scatter upper cells and a shared legend.",
            title = "Penguins Pair Plot by Species",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createPenguinsMatrixBySpecies() {
        var penguins = PenguinsDatasets.loadPenguins();
        return matrixPlot(penguins, aes().color("species"), COLUMNS)
                .title("Penguins Pair Plot by Species")
                .upper(point());
    }

    /** {@return a tips scatter-plot matrix over the numeric columns} */
    @SamplePlot(description = "Scatter-plot matrix over the numeric tips columns.",
            title = "Tips Pair Plot",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createTipsMatrix() {
        var tips = TipsDatasets.loadTips();
        return matrixPlot(tips, "total_bill", "tip", "size")
                .title("Tips Pair Plot");
    }

    /** {@return a tips scatter-plot matrix coloured by smoker} */
    @SamplePlot(description = "Matrix coloured by smoker status across bill, tip, and size.",
            title = "Tips Pair Plot by Smoker",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createTipsMatrixBySmoker() {
        var tips = TipsDatasets.loadTips();
        return matrixPlot(tips, aes().color("smoker"), "total_bill", "tip", "size")
                .title("Tips Pair Plot by Smoker");
    }

    /** {@return a tips matrix with the type dispatch over mixed columns} */
    @SamplePlot(description = "Matrix dispatching cell types over mixed numeric and categorical columns.",
            title = "Tips Mixed-Type Matrix (type dispatch)",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY, SampleGeom.BAR},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createTipsMixed() {
        var tips = TipsDatasets.loadTips();
        return matrixPlot(tips, aes(), "total_bill", "day", "sex")
                .title("Tips Mixed-Type Matrix (type dispatch)");
    }

    /** {@return a penguins matrix with the type dispatch over mixed columns} */
    @SamplePlot(description = "Matrix dispatching cell types over mixed body and species columns.",
            title = "Penguins Mixed-Type Matrix (type dispatch)",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY, SampleGeom.BAR},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createPenguinsMixed() {
        var penguins = PenguinsDatasets.loadPenguins();
        return matrixPlot(penguins, aes(), "bill_length_mm", "species", "island")
                .title("Penguins Mixed-Type Matrix (type dispatch)");
    }

    /** {@return a diamonds scatter-plot matrix over the full 53k-row dataset} */
    @SamplePlot(description = "Scatter-plot matrix over the full 53k-row diamonds dataset.",
            title = "Diamonds Pair Plot (full dataset)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createDiamondsMatrix() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        return matrixPlot(diamonds, "carat", "depth", "price", "table")
                .title("Diamonds Pair Plot (full dataset)");
    }

    /** {@return a diamonds scatter-plot matrix coloured by cut (full dataset)} */
    @SamplePlot(description = "Full-dataset matrix coloured by diamond cut.",
            title = "Diamonds Pair Plot by Cut (full dataset)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createDiamondsMatrixByCut() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        return matrixPlot(diamonds, aes().color("cut"), "carat", "depth", "price")
                .title("Diamonds Pair Plot by Cut (full dataset)");
    }

    /** {@return a diamonds matrix with the type dispatch over mixed columns} */
    @SamplePlot(description = "Matrix dispatching cell types over carat, cut, and clarity columns.",
            title = "Diamonds Mixed-Type Matrix (type dispatch)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY, SampleGeom.BAR},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createDiamondsMixed() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        return matrixPlot(diamonds, aes(), "carat", "cut", "clarity")
                .title("Diamonds Mixed-Type Matrix (type dispatch)");
    }

    /** {@return a numeric mtcars scatter-plot matrix} */
    @SamplePlot(description = "Scatter-plot matrix over the numeric mtcars columns.",
            title = "Mtcars Pair Plot",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createMtcarsMatrix() {
        var mtcars = MtcarsDatasets.loadNumericMtcars();
        return matrixPlot(mtcars, "mpg", "disp", "hp", "wt")
                .title("Mtcars Pair Plot");
    }

    /** {@return a numeric mtcars scatter-plot matrix coloured by cylinder count} */
    @SamplePlot(description = "Matrix coloured by cylinder count across mpg, hp, and weight.",
            title = "Mtcars Pair Plot by Cylinders",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createMtcarsMatrixByCyl() {
        var mtcars = MtcarsDatasets.loadNumericMtcars();
        return matrixPlot(mtcars, aes().color("cyl"), "mpg", "hp", "wt")
                .title("Mtcars Pair Plot by Cylinders");
    }

    /** {@return an mpg scatter-plot matrix over the fuel-economy columns} */
    @SamplePlot(description = "Scatter-plot matrix over the fuel-economy columns.",
            title = "MPG Pair Plot",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createMpgMatrix() {
        var mpg = MpgDatasets.loadMpg();
        return matrixPlot(mpg, "displ", "cty", "hwy")
                .title("MPG Pair Plot");
    }

    /** {@return an mpg scatter-plot matrix coloured by vehicle class} */
    @SamplePlot(description = "Matrix coloured by vehicle class across displacement and mileage.",
            title = "MPG Pair Plot by Class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY},
            stats = {SampleStat.COR},
            features = {SampleFeature.TWO_D})
    public static PlotMatrix<DataFrame> createMpgMatrixByClass() {
        var mpg = MpgDatasets.loadMpg();
        return matrixPlot(mpg, aes().color("class"), "displ", "cty", "hwy")
                .title("MPG Pair Plot by Class");
    }
}
