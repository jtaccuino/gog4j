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

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$double;
import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Coords.coordFlip;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.dflib.data.MeatDatasets;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;

/**
 * {@code Geoms.col()} examples, following the {@code Geoms.bar()} documentation
 * of the reference: column heights represent the values already in the data — the
 * identity statistic — rather than a count of cases.
 * <p>
 * The first two figures mirror the reference example almost literally:
 * the same values drawn first as columns, then as points, which need no
 * zero baseline. The remaining figures exercise the stacked and dodged
 * positions, a horizontal orientation, and a time axis.
 *
 * @see org.jtaccuino.gog.dflib.data.MpgDatasets
 * @see org.jtaccuino.gog.dflib.data.DiamondsDatasets
 * @see org.jtaccuino.gog.dflib.data.MeatDatasets
 */
public class ColPlots {

    /** Utility class; not meant to be instantiated. */
    private ColPlots() {
    }

    /**
     * Mean highway fuel economy per vehicle class, in order of first
     * appearance in the mpg dataset.
     *
     * @return a {@link DataFrame} with columns {@code class} and {@code mean_hwy}
     */
    private static DataFrame meanHwyByClass() {
        var mpg = MpgDatasets.loadMpg().cols("hwy").merge($col("hwy").castAsDouble());
        return mpg.group("class").agg($col("class"), $double("hwy").avg().as("mean_hwy"));
    }

    /**
     * Mean highway fuel economy per vehicle class and drive type, the fields a
     * stacked or dodged column chart groups by.
     *
     * @return a {@link DataFrame} with columns {@code class}, {@code drv}, and {@code mean_hwy}
     */
    private static DataFrame meanHwyByClassAndDrv() {
        var mpg = MpgDatasets.loadMpg().cols("hwy").merge($col("hwy").castAsDouble());
        return mpg.group("class", "drv").agg(
                $col("class"), $col("drv"), $double("hwy").avg().as("mean_hwy"));
    }

    /**
     * Mean diamond price per cut, in order of first appearance.
     *
     * @return a {@link DataFrame} with columns {@code cut} and {@code mean_price}
     */
    private static DataFrame meanPriceByCut() {
        var diamonds = DiamondsDatasets.loadDiamonds();
        return diamonds.group("cut").agg($col("cut"), $double("price").avg().as("mean_price"));
    }

    /**
     * Total meat production per year, summed across the three main animal
     * types.
     *
     * @return a {@link DataFrame} with columns {@code year} and {@code total}
     */
    private static DataFrame totalProductionByYear() {
        var consumption = MeatDatasets.getConsumptionPerYear();
        return consumption.group("year").agg($col("year"), $double("total_consumption").sum().as("total"));
    }

    /** {@return the basic column plot of mean highway MPG by class} */
    @SamplePlot(description = "Column heights for mean highway MPG per vehicle class, filled blue.",
            title = "Mean Highway MPG by Class (Geoms.col)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBasicCol() {
        var df = meanHwyByClass();
        return ggplot(df, aes().x("class").y("mean_hwy"))
                .geoms(col().fill(Color.web("#3182bd")))
                .labs(labs("1. Mean Highway MPG by Class (Geoms.col)", "Vehicle Class", "Mean Highway MPG"));
    }

    /** {@return the same values drawn as points instead of columns} */
    @SamplePlot(description = "The same class-mean values drawn as points instead of columns.",
            title = "The Same Values as Points (Geoms.point)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColVsPoint() {
        var df = meanHwyByClass();
        return ggplot(df, aes().x("class").y("mean_hwy"))
                .geoms(point().size(6.0).color(Color.web("#3182B4")))
                .labs(labs("2. The Same Values as Points (Geoms.point)", "Vehicle Class", "Mean Highway MPG"));
    }

    /** {@return the stacked column plot by vehicle class and drive type} */
    @SamplePlot(description = "Columns stacked by drive type using the default stack position.",
            title = "Stacked Columns (position = 'stack')",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStackedCol() {
        var df = meanHwyByClassAndDrv();
        return ggplot(df, aes().x("class").y("mean_hwy").fill("drv"))
                .geoms(col())
                .labs(labs("3. Stacked Columns (position = 'stack')", "Vehicle Class", "Mean Highway MPG"));
    }

    /** {@return the dodged column plot with drive-type segments side by side} */
    @SamplePlot(description = "Columns dodged side by side by drive type via the dodge position.",
            title = "Dodged Columns (position = 'dodge')",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDodgedCol() {
        var df = meanHwyByClassAndDrv();
        return ggplot(df, aes().x("class").y("mean_hwy").fill("drv"))
                .geoms(col(Position.DODGE))
                .labs(labs("4. Dodged Columns (position = 'dodge')", "Vehicle Class", "Mean Highway MPG"));
    }

    /** {@return the dodged column plot with an explicit narrow dodge width} */
    @SamplePlot(description = "Dodged columns with an explicit narrow dodge width of 0.5.",
            title = "Dodged Columns (Positions.dodge(width = 0.5))",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDodgedColWidth() {
        var df = meanHwyByClassAndDrv();
        return ggplot(df, aes().x("class").y("mean_hwy").fill("drv"))
                .geoms(col().dodge(0.5))
                .labs(labs("4b. Dodged Columns (Positions.dodge(width = 0.5))", "Vehicle Class", "Mean Highway MPG"));
    }

    /** {@return the horizontal column plot of mean diamond price per cut} */
    @SamplePlot(description = "Horizontal columns of mean price per cut via the flipped coordinate system.",
            title = "Horizontal Columns (coordFlip)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.FLIP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalCol() {
        var df = meanPriceByCut();
        return ggplot(df, aes().x("cut").y("mean_price"))
                .geoms(col().fill(Color.web("#6baed6")))
                .coord(coordFlip())
                .labs(labs("5. Horizontal Columns (coordFlip)", "Mean Price", "Cut"));
    }

    /** {@return the column plot of total annual meat production over a time axis} */
    @SamplePlot(description = "Annual meat production totals drawn as columns over a time axis.",
            title = "Total Annual Production (col over a Time Axis)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createTimeAxisCol() {
        var df = totalProductionByYear();
        return ggplot(df, aes().x("year").y("total"))
                .geoms(col().widthFactor(0.8))
                .labs(labs("6. Total Annual Production (col over a Time Axis)", "Year", "Production (Million Pounds)"));
    }
}
