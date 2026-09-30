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
import static org.jtaccuino.gog.AesValue.ComputedVariable.COUNT;
import static org.jtaccuino.gog.AesValue.ComputedVariable.PROP;
import static org.jtaccuino.gog.AesValue.afterStat;
import static org.jtaccuino.gog.Geoms.bar;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.stat.Stats.count;
import static org.jtaccuino.gog.stat.Stats.identity;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * Contrasts the {@code Geoms.bar()} and {@code Geoms.col()} statistics, mirroring
 * the {@code Stats.count()} vs {@code Stats.identity()} split.
 * <p>
 * {@code Geoms.bar()} counts the observations per {@code x} category from the
 * raw rows; {@code Geoms.col()} draws heights that are already in the data. Each
 * can be overridden to the other's statistic, and computed {@code afterStat()}
 * variables colour the counted bars.
 */
public class BarColStatsPlots {

    /** Utility class; not meant to be instantiated. */
    private BarColStatsPlots() {
    }

    /**
     * A genuine {@code Geoms.bar()}: the raw mtcars rows are counted per
     * cylinder count, so the bar height is the number of cars — no {@code y}
     * mapping needed.
     *
     * @return the count bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bar heights counting raw mpg rows per vehicle class.",
            title = "Geoms.bar() — Stats.count counts the rows per class",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createClassCount() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class"))
                .geoms(bar())
                .labs(labs("Geoms.bar() — Stats.count counts the rows per class",
                        "Vehicle Class", "Count"));
    }

    /**
     * The same counting on the mtcars cylinders, with bars coloured by their
     * count through {@code afterStat(count)}.
     *
     * @return the afterStat count bar-plot {@link Plot}
     */
    @SamplePlot(description = "Count bars coloured by the afterStat(count) computed variable.",
            title = "Geoms.bar() fill = afterStat(count)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCylCount() {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("cyl"))
                .geoms(bar().mapping(aes().fill(afterStat(COUNT))))
                .labs(labs("Geoms.bar() fill = afterStat(count)",
                        "Cylinders", "Count"));
    }

    /**
     * The plain {@code Geoms.bar()} count on the mtcars cylinders: only
     * {@code aes(x)} is mapped, the bar height is the row count per cylinder.
     *
     * @return the count bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bar heights counting raw mtcars rows per cylinder count.",
            title = "Geoms.bar() — Stats.count counts the rows per cylinder",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCylPlainCount() {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("cyl"))
                .geoms(bar())
                .labs(labs("Geoms.bar() — Stats.count counts the rows per cylinder",
                        "Cylinders", "Count"));
    }

    /**
     * A genuine {@code Geoms.col()}: the mean-mileage values are already in the
     * data, so the column height is that value (identity statistic), not a
     * count.
     *
     * @return the value column-plot {@link Plot}
     */
    @SamplePlot(description = "Column heights taken from data values via the identity statistic.",
            title = "Geoms.col() — the y values are the heights (identity)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.COL},
            stats = {SampleStat.IDENTITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMeanMpgCol() {
        var df = MtcarsDatasets.meanMpgByCyl();
        return ggplot(df, aes().x("cyl").y("mpg"))
                .geoms(col())
                .labs(labs("Geoms.col() — the y values are the heights (identity)",
                        "Cylinders", "Mean MPG"));
    }

    /**
     * {@code Geoms.bar(stat = "identity")}: the explicit
     * {@link org.jtaccuino.gog.stat.Stats#identity()} override makes
     * {@code Geoms.bar()} draw the raw y values, producing the very figure
     * {@code Geoms.col()} draws.
     *
     * @return the identity-override bar-plot {@link Plot}
     */
    @SamplePlot(description = "Geoms.bar overridden with the identity statistic to draw raw y values.",
            title = "Geoms.bar(stat = \"identity\") ≡ Geoms.col()",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.IDENTITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBarIdentityOverride() {
        var df = MtcarsDatasets.meanMpgByCyl();
        return ggplot(df, aes().x("cyl").y("mpg"))
                .geoms(bar().stat(identity()))
                .labs(labs("Geoms.bar(stat = \"identity\") ≡ Geoms.col()",
                        "Cylinders", "Mean MPG"));
    }

    /**
     * {@code Geoms.col(stat = "count")}: the explicit
     * {@link org.jtaccuino.gog.stat.Stats#count()} override makes
     * {@code Geoms.col()} count rows per {@code x}, producing the very figure
     * {@code Geoms.bar()} draws.
     *
     * @return the count-override column-plot {@link Plot}
     */
    @SamplePlot(description = "Geoms.col overridden with the count statistic to tally rows per category.",
            title = "Geoms.col(stat = \"count\") ≡ Geoms.bar()",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.COL},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColCountOverride() {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("cyl"))
                .geoms(col().stat(count()))
                .labs(labs("Geoms.col(stat = \"count\") ≡ Geoms.bar()",
                        "Cylinders", "Count"));
    }

    /**
     * Bars coloured by their share of the total: {@code afterStat(prop)} is a
     * computed variable of {@code Stats.count()}.
     *
     * @return the afterStat prop bar-plot {@link Plot}
     */
    @SamplePlot(description = "Count bars coloured by each category's proportion via afterStat(prop).",
            title = "Geoms.bar() fill = afterStat(prop)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAfterStatProp() {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("cyl"))
                .geoms(bar().mapping(aes().fill(afterStat(PROP))))
                .labs(labs("Geoms.bar() fill = afterStat(prop)",
                        "Cylinders", "Count"));
    }
}
