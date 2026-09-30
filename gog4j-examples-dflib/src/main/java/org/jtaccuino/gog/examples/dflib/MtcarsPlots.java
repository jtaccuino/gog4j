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
import static org.jtaccuino.gog.Geoms.abline;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.hline;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Geoms.vline;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.layer.Position.STACK;

import java.util.ArrayList;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Reference-line examples on the Motor Trend car-road-tests (mtcars) dataset,
 * following the {@code Geoms.abline()} documentation of the reference: fixed-value
 * vertical and horizontal rules, slope/intercept diagonals, and the least
 * squares fit drawn directly or through {@code Geoms.smooth(method = "lm")}.
 *
 * @see MtcarsDatasets
 */
public class MtcarsPlots {

    /** Utility class; not meant to be instantiated. */
    private MtcarsPlots() {
    }

    private static final Color VLINE = Color.web("#2e86c1");
    private static final Color HLINE = Color.web("#c0392b");
    private static final Color ABLINE = Color.web("#7f8c8d");

    /**
     * The scatter plot all reference-line figures are built on:
     * weight against fuel economy, the mtcars analogue of the
     * {@code ggplot(mtcars, aes(wt, mpg)) + Geoms.point()}.
     */
    private static Plot<DataFrame> base(String title) {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("wt").y("mpg"))
                .geoms(point().size(3.0))
                .labs(labs(title, "weight (1,000 lb)", "miles per gallon"));
    }

    /**
     * 1. A vertical reference line at a single x value —
     * {@code p + Geoms.vline(xintercept = 5)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Vertical reference line at 5,000 lb with an annotation label.",
            title = "Vertical reference line (xintercept = 5)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.VLINE},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE})
    public static Plot<DataFrame> createFixedVline() {
        return base("1. Vertical reference line — vline(xintercept = 5)")
                .geoms(vline(5.0).color(VLINE).annotate("5,000 lb"));
    }

    /**
     * 2. Several vertical rules at once, {@code p + Geoms.vline(xintercept = 1:5)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Vertical reference lines at every integer from one through five.",
            title = "Vertical lines at every integer (1:5)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.VLINE},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createMultipleVlines() {
        var layers = new ArrayList<LayerConfigurator<? super DataFrame>>();
        layers.add(point().size(3.0));
        for (var x = 1; x <= 5; x++) {
            layers.add(vline(x).color(VLINE));
        }
        return base("2. Vertical lines at every integer — vline(xintercept = 1:5)")
                .geoms(layers);
    }

    /**
     * 3. A horizontal reference line at a single y value —
     * {@code p + Geoms.hline(yintercept = 20)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Dashed horizontal reference line at 20 mpg with an annotation label.",
            title = "Horizontal reference line (yintercept = 20)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE})
    public static Plot<DataFrame> createFixedHline() {
        return base("3. Horizontal reference line — hline(yintercept = 20)")
                .geoms(hline(20.0).color(HLINE).dashed().annotate("20 mpg"));
    }

    /**
     * 4. A diagonal with only the intercept supplied, slope defaulting to one —
     * {@code p + Geoms.abline(intercept = 20)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Diagonal reference line with intercept 20 and slope one.",
            title = "Diagonal from an intercept (intercept = 20)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAblineIntercept() {
        return base("4. Diagonal from an intercept — abline(intercept = 20)")
                .geoms(abline(1.0, 20.0).color(ABLINE).dashed());
    }

    /**
     * 5. The least-squares line from {@code coef(lm(mpg ~ wt))}, rounded as in
     * the reference example — {@code p + Geoms.abline(intercept = 37, slope = -5)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Least-squares diagonal fitted to weight against fuel economy.",
            title = "Least-squares fit (intercept = 37, slope = -5)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAblineBestFit() {
        return base("5. Least-squares fit — abline(intercept = 37, slope = -5)")
                .geoms(abline(-5.0, 37.0).color(ABLINE).width(1.4));
    }

    /**
     * 6. The bare diagonal {@code p + Geoms.abline()}, slope one through the
     * origin — below the visible y range, so nothing appears.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Default slope-one diagonal, hidden below the visible data range.",
            title = "Default diagonal, outside the data range",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAblineOutsideRange() {
        return base("6. Default diagonal, outside the data range")
                .geoms(abline(1.0, 0.0).color(ABLINE));
    }

    /**
     * 7. The recommended alternative to hand-computed coefficients:
     * {@code p + Geoms.smooth(method = "lm", se = FALSE)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Linear-model smoothing fit as an alternative to a hand-computed diagonal.",
            title = "Same fit via Geoms.smooth(method = 'lm')",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLmSmoothAlternative() {
        return base("7. The same fit via Geoms.smooth(method = 'lm')")
                .geoms(smooth(SmoothMethod.LM, false).color(ABLINE));
    }

    /**
     * The faceted scatter plot the per-cylinder reference-line figures are
     * built on: {@code ggplot(mtcars, aes(mpg, wt)) + Geoms.point() +
     * Facets.wrap(~cyl)}, the mtcars analogue of the reference example.
     */
    private static Plot<DataFrame> facetedBase(String title) {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("mpg").y("wt"))
                .geoms(point().size(3.0))
                .facets(wrap("cyl", 3))
                .labs(labs(title, "miles per gallon", "weight (1,000 lb)"));
    }

    /**
     * 8. A horizontal reference line per panel, drawn from a summary frame that
     * shares the facet column — {@code p + Geoms.hline(aes(yintercept = wt), mean_wt)}
     * inside {@code Facets.wrap(~cyl)}. Each panel shows the mean weight of its
     * own cylinder count.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Faceted horizontal lines showing mean weight per cylinder count.",
            title = "Faceted hline from summary frame",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetedHline() {
        return facetedBase("8. Faceted hline from summary frame")
                .geoms(hline().data(MtcarsDatasets.meanWeightByCyl()).yintercept("wt").color(HLINE));
    }

    /**
     * 9. A vertical reference line per panel, the mirror of example 8 —
     * {@code p + Geoms.vline(aes(xintercept = mpg), mean_mpg)} inside
     * {@code Facets.wrap(~cyl)}. Each panel shows the mean fuel economy of its
     * own cylinder count.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Faceted vertical lines showing mean fuel economy per cylinder count.",
            title = "Faceted vline from summary frame",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.VLINE},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetedVline() {
        return facetedBase("9. Faceted vline from summary frame")
                .geoms(vline().data(MtcarsDatasets.meanMpgByCyl()).xintercept("mpg").color(VLINE));
    }

    /**
     * 10. The same per-panel line with colour mapped to the intercept column —
     * {@code p + Geoms.hline(aes(yintercept = wt, colour = wt), mean_wt)}. The
     * continuous ramp is scaled across the whole summary frame, so the colour of
     * a panel's line stays meaningful.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Faceted horizontal lines with color scaled by each panel's mean weight.",
            title = "Faceted hline coloured by weight",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColourMappedHline() {
        return facetedBase("10. Faceted hline coloured by weight")
                .geoms(hline().data(MtcarsDatasets.meanWeightByCyl()).yintercept("wt").color("wt"));
    }

    /**
     * 11. A separate regression line per panel, from per-cylinder least-squares
     * coefficients — the faceted analogue of the single
     * {@code abline(intercept = 37, slope = -5)} in example 5. The fit is
     * {@code mpg ~ wt}, so this figure keeps {@code wt} on the X axis and
     * {@code mpg} on the Y axis, unlike examples 8–10.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Faceted least-squares regression lines fitted per cylinder count.",
            title = "Faceted abline per cylinder",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetedAbline() {
        var df = MtcarsDatasets.loadMtcars();
        return ggplot(df, aes().x("wt").y("mpg"))
                .geoms(point().size(3.0))
                .facets(wrap("cyl", 3))
                .labs(labs("11. Faceted abline per cylinder", "weight (1,000 lb)", "miles per gallon"))
                .geoms(abline().data(MtcarsDatasets.lmFitByCyl()).slope("slope").intercept("intercept").color(ABLINE));
    }

    /**
     * 12. A faceted, stacked bar chart — the count of cars per number of forward
     * gears, stacked by transmission (automatic / manual), one panel per
     * cylinder count. The reference analogue is
     * {@code ggplot(mtcars, aes(gear, n, fill = am)) + Geoms.col() +
     * Facets.wrap(~cyl)}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Stacked bars of gear counts stacked by transmission, faceted by cylinders.",
            title = "Faceted stacked bars (gears by transmission)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            facets = {SampleFacet.WRAP},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFacetedStackedBar() {
        var df = MtcarsDatasets.countsByCylGearAm();
        return ggplot(df, aes().x("gear").y("n").fill("am"))
                .geoms(col(STACK).widthFactor(0.75))
                .facets(wrap("cyl", 3))
                .labs(labs("12. Faceted stacked bars — cars by gears, stacked by transmission",
                                "number of forward gears", "number of cars")
                        .map("0", "automatic")
                        .map("1", "manual"));
    }
}
