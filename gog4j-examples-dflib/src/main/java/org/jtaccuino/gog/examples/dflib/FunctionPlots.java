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
import static org.jtaccuino.gog.Geoms.function;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import javafx.scene.paint.Color;
import org.apache.commons.math4.legacy.special.BesselJ;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.data.EmptyDataFrame;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Mathematical-curve examples following the {@code Geoms.function()} reference
 * of the reference: overlaying a known function on a density, drawing on a data-less
 * plot whose x axis is set by the plot-level coordinate limits, shifting a
 * Gaussian, comparing two distributions, a bespoke anonymous function, and
 * restricting or widening the range a superimposed curve is evaluated over.
 */
public class FunctionPlots {

    /** Utility class; not meant to be instantiated. */
    private FunctionPlots() {
    }

    /** A dense red curve for the overlaid function. */
    private static final Color CURVE = Color.web("#e31a1c");

    private static final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);

    /**
     * The standard normal density {@code normalDensity(x)}, the Java analogue of
     * {@code normalDensity(x)} used throughout the reference examples.
     *
     * @param x the value to evaluate
     * @return the probability density of the standard normal at {@code x}
     */
    public static double dnorm(double x) {
        return dnorm(x, 0.0, 1.0);
    }

    /**
     * The normal density {@code normalDensity(x, mean, sd)}.
     *
     * @param x     the value to evaluate
     * @param mean  the distribution mean
     * @param sd    the standard deviation
     * @return the probability density of the normal at {@code x}
     */
    public static double dnorm(double x, double mean, double sd) {
        var z = (x - mean) / sd;
        return Math.exp(-0.5 * z * z) / (sd * SQRT_2PI);
    }

    /**
     * A Student-t density {@code dt(x, df)} for the reference example's
     * {@code fun = dt, args = list(df = 1)} — at one degree of freedom the t
     * reduces to the Cauchy distribution, so no gamma function is needed.
     *
     * @param x  the value to evaluate
     * @param df the degrees of freedom (always 1 in the reference example)
     * @return the probability density of the t distribution at {@code x}
     */
    public static double dt(double x, int df) {
        if (df == 1) {
            return 1.0 / (Math.PI * (1.0 + x * x));
        }
        throw new IllegalArgumentException("dt is only implemented for df = 1");
    }

    /**
     * A DataFrame of {@code n} standard-normal draws, the Java analogue of
     * {@code data.frame(x = rnorm(100))}.
     *
     * @param n    the number of draws
     * @param seed the RNG seed for reproducible figures
     * @return a single-column {@code x} DataFrame
     */
    public static DataFrame standardNormal(int n, long seed) {
        var rng = new Random(seed);
        var values = new double[n];
        for (var i = 0; i < n; i++) {
            values[i] = rng.nextGaussian();
        }
        return DataFrame.byColumn("x").of(Series.ofDouble(values));
    }

    /** {@return a plot of the standard normal density with {@code normalDensity} overlaid} */
    @SamplePlot(description = "Standard normal density with the analytic normalDensity curve overlaid in red.",
            title = "normalDensity Over a Density",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.DENSITY, SampleGeom.FUNCTION},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createOverlayOnDensity() {
        return ggplot(standardNormal(100, 1492L), aes().x("x"))
                .geoms(density())
                .geoms(function(FunctionPlots::dnorm).color(CURVE).width(2.0))
                .labs(labs("1. normalDensity Over a Density", "x", "density"));
    }

    /**
     * {@return the standard normal density on a data-less plot whose x axis is
     * set to {@code [-5, 5]} by the plot-level coordinate limits}
     */
    @SamplePlot(description = "The standard normal curve drawn on a data-less plot with a fixed xlim(-5, 5) axis.",
            title = "normalDensity Alone, xlim(-5, 5)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<EmptyDataFrame> createFunctionAlone() {
        return ggplot(aes())
                .coord(Coord2D.cartesian().xlim(-5, 5))
                .geoms(function(FunctionPlots::dnorm))
                .labs(labs("2. normalDensity Alone, xlim(-5, 5)", "x", "density"));
    }

    /** {@return the same density shifted and narrowed via function's parameters} */
    @SamplePlot(description = "Normal curve shifted and narrowed by the mean = 2 and sd = .5 parameters.",
            title = "normalDensity(mean = 2, sd = .5)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<EmptyDataFrame> createShiftedGaussian() {
        return ggplot(aes())
                .coord(Coord2D.cartesian().xlim(-5, 5))
                .geoms(function(x -> dnorm(x, 2.0, 0.5)).color(Color.web("#3182bd")))
                .labs(labs("3. normalDensity(mean = 2, sd = .5)", "x", "density"));
    }

    /** {@return two distributions on the same plot, normal and t with df = 1} */
    @SamplePlot(description = "Two overlaid curves comparing the normal density and the t distribution with one degree of freedom.",
            title = "Normal vs t, df = 1",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<EmptyDataFrame> createTwoFunctions() {
        return ggplot(aes())
                .coord(Coord2D.cartesian().xlim(-5, 5))
                .geoms(
                        function(FunctionPlots::dnorm).color(Color.web("#e31a1c")),
                        function(x -> dt(x, 1)).color(Color.web("#3182bd")))
                .labs(labs("4. Normal vs t, df = 1", "x", "density"));
    }

    /** {@return a bespoke anonymous function, {@code \\(x) 0.5 * exp(-|x|)}} */
    @SamplePlot(description = "A custom anonymous function drawing the curve 0.5 times exp(-abs(x)).",
            title = "Custom Anonymous Function",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<EmptyDataFrame> createAnonymousFunction() {
        return ggplot(aes())
                .coord(Coord2D.cartesian().xlim(-5, 5))
                .geoms(function((DoubleUnaryOperator) x -> 0.5 * Math.exp(-Math.abs(x))))
                .labs(labs("5. Custom Anonymous Function", "x", "density"));
    }

    /** {@return the overlaid curve restricted to xlim(-1, 1)} */
    @SamplePlot(description = "Density histogram with the normalDensity curve restricted to a narrow xlim(-1, 1) range.",
            title = "normalDensity Restricted to xlim(-1, 1)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.DENSITY, SampleGeom.FUNCTION},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRestrictedXlim() {
        return ggplot(standardNormal(100, 1492L), aes().x("x"))
                .geoms(density())
                .geoms(function(FunctionPlots::dnorm).xlim(-1, 1).color(CURVE).width(2.0))
                .labs(labs("6. normalDensity Restricted to xlim(-1, 1)", "x", "density"));
    }

    /** {@return the overlaid curve widened to xlim(-7, 7)} */
    @SamplePlot(description = "Density histogram with the normalDensity curve widened to an xlim(-7, 7) range.",
            title = "normalDensity Widened to xlim(-7, 7)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.DENSITY, SampleGeom.FUNCTION},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createWidenedXlim() {
        return ggplot(standardNormal(100, 1492L), aes().x("x"))
                .geoms(density())
                .geoms(function(FunctionPlots::dnorm).xlim(-7, 7).color(CURVE).width(2.0))
                .labs(labs("7. normalDensity Widened to xlim(-7, 7)", "x", "density"));
    }

    /**
     * {@return the Bessel functions of the first kind, orders 0 and 2, over
     * {@code [0, 20]}, each curve coloured via its own per-geom label}
     * <p>
     * Because a function carries no data column, each {@code aes().color(...)}
     * is a constant group label: the label auto-assigns a palette colour to the
     * curve and, together with the second labelled curve, produces a matching
     * colour legend whose keys are the curves' labels.
     */
    @SamplePlot(description = "Bessel functions of orders 0 and 2 over [0, 20], coloured via per-geom labels.",
            title = "Bessel Functions J0 and J2",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FUNCTION},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<EmptyDataFrame> createBesselFunctions() {
        return ggplot(aes())
                .coord(Coord2D.cartesian().xlim(0, 20))
                .geoms(
                        function(x -> new BesselJ(0).value(x)).n(400)
                                .aes(aes().color("BJ 0")).width(2.0),
                        function(x -> new BesselJ(2).value(x)).n(400)
                                .aes(aes().color("BJ 2")).width(2.0))
                .guides(Guides.guide(Aesthetic.COLOR, Guides.guideLegend().title("")))
                .labs(labs("8. Bessel Functions J0 and J2", "x", "y"));
    }
}
