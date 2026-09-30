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
import static org.jtaccuino.gog.Geoms.crossbar;
import static org.jtaccuino.gog.Geoms.errorbar;
import static org.jtaccuino.gog.Geoms.freqpoly;
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Geoms.pointrange;
import static org.jtaccuino.gog.Geoms.ribbon;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.stat.Stats.summary;

import java.util.Random;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * Example plots for Sprint 2 of the feature-parity plan: {@code Geoms.histogram()}
 * /{@code Geoms.freqpoly()} (the {@code Stats.bin()} family) and the
 * {@code Stats.summary()} error/range family — {@code Geoms.errorbar()},
 * {@code Geoms.crossbar()}, {@code Geoms.pointrange()} and {@code Geoms.ribbon()}.
 * <p>
 * The summary figures build their frame with
 * {@code Stats.summary(df, group, value)} and hand the
 * {@code x}/{@code y}/{@code ymin}/{@code ymax} columns straight to a range
 * geom, exactly the workflow the DSL is designed around.
 */
public class SummaryStatPlots {

    /** Utility class; not meant to be instantiated. */
    private SummaryStatPlots() {
    }

    /** A deterministic bell-shaped sample, ~500 points, for binning. */
    private static DataFrame bellData() {
        var rng = new Random(42);
        var n = 500;
        var x = new double[n];
        for (var i = 0; i < n; i++) {
            x[i] = rng.nextGaussian() * 1.2;
        }
        return DataFrame.byColumn("score").of(Series.ofDouble(x));
    }

    /** Two overlapping bell-shaped groups, one per colour. */
    private static DataFrame twoGroupData() {
        var rng = new Random(7);
        var n = 200;
        var score = new double[2 * n];
        var grp = new String[2 * n];
        for (var i = 0; i < n; i++) {
            score[i] = rng.nextGaussian() * 1.0 - 0.8;
            score[n + i] = rng.nextGaussian() * 1.0 + 0.8;
            grp[i] = "A";
            grp[n + i] = "B";
        }
        return DataFrame.byColumn("score", "grp")
                .of(Series.ofDouble(score), Series.of(grp));
    }

    /** A per-category value column whose range a summary collapses. */
    private static DataFrame summarySource() {
        var rng = new Random(11);
        var groups = new String[]{"Very low", "Low", "Medium", "High", "Very high"};
        var size = 40;
        var allG = new String[groups.length * size];
        var allV = new double[groups.length * size];
        for (var g = 0; g < groups.length; g++) {
            for (var i = 0; i < size; i++) {
                allG[g * size + i] = groups[g];
                allV[g * size + i] = (g + 1) * 2.0 + rng.nextGaussian() * 0.8;
            }
        }
        return DataFrame.byColumn("grp", "val")
                .of(Series.of(allG), Series.ofDouble(allV));
    }

    /** A smooth-ish trend with a symmetric band for the ribbon figure. */
    private static DataFrame bandData() {
        var n = 40;
        var t = new double[n];
        var lo = new double[n];
        var hi = new double[n];
        for (var i = 0; i < n; i++) {
            double x = i / (double) (n - 1) * 4.0;
            double mid = Math.sin(x) * 2.0 + x;
            t[i] = x;
            lo[i] = mid - 0.6;
            hi[i] = mid + 0.6;
        }
        return DataFrame.byColumn("x", "ymin", "ymax").of(Series.ofDouble(t), Series.ofDouble(lo), Series.ofDouble(hi));
    }

    /** {@return a {@code Geoms.histogram()} of a bell-shaped score} */
    @SamplePlot(description = "Histogram of a bell-shaped score distribution with 30 bins.",
            title = "Geoms.histogram()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.HISTOGRAM},
            stats = {SampleStat.BIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHistogram() {
        return ggplot(bellData(), aes().x("score"))
                .geoms(histogram().bins(30).fill(Color.web("#3182bd")).color(Color.web("#1f4e79")))
                .labs(labs("Geoms.histogram()", "score", "count"));
    }

    /** {@return a {@code Geoms.freqpoly()} overlaying two distributions} */
    @SamplePlot(description = "Frequency polygons overlaid with one line per colour group.",
            title = "Geoms.freqpoly() — one line per group",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.FREQPOLY},
            stats = {SampleStat.BIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFreqpoly() {
        return ggplot(twoGroupData(), aes().x("score").color("grp"))
                .geoms(freqpoly().bins(20).width(2.0))
                .labs(labs("Geoms.freqpoly() — one line per group", "score", "count"));
    }

    /** {@return a {@code Geoms.pointrange()} of a per-group mean ± sd} */
    @SamplePlot(description = "Pointrange of per-group mean with standard-deviation whiskers.",
            title = "Geoms.pointrange() — Stats.summary mean ± sd",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINTRANGE},
            stats = {SampleStat.SUMMARY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createPointrange() {
        var summary = summary(summarySource(), "grp", "val").meanSd().fit();
        return ggplot(summary, aes().x("x").y("y").ymin("ymin").ymax("ymax"))
                .geoms(pointrange().color(Color.web("#756bb1")).width(1.6).pointRadius(3.0))
                .labs(labs("Geoms.pointrange() — Stats.summary mean ± sd", "group", "value"));
    }

    /** {@return a {@code Geoms.errorbar()} + {@code Geoms.crossbar()} combo} */
    @SamplePlot(description = "Errorbars layered over crossbars summarizing mean ± sd per group.",
            title = "Geoms.errorbar() over Geoms.crossbar()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.ERRORBAR, SampleGeom.CROSSBAR},
            stats = {SampleStat.SUMMARY},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createErrorbarCrossbar() {
        var summary = summary(summarySource(), "grp", "val").meanSd().fit();
        return ggplot(summary, aes().x("x").y("y").ymin("ymin").ymax("ymax"))
                .geoms(errorbar().color(Color.web("#e31a1c")).width(1.4))
                .geoms(crossbar().fill(Color.web("#3182bd", 0.35)).color(Color.web("#1f4e79")))
                .labs(labs("Geoms.errorbar() over Geoms.crossbar()", "group", "value"));
    }

    /** {@return a standalone {@code Geoms.ribbon()} uncertainty band} */
    @SamplePlot(description = "Ribbon band spanning ymin to ymax as an uncertainty envelope.",
            title = "Geoms.ribbon() — an arbitrary uncertainty band",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.RIBBON},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRibbon() {
        return ggplot(bandData(), aes().x("x").ymin("ymin").ymax("ymax"))
                .geoms(ribbon().fill(Color.web("#3182bd")).alpha(0.35).outline(true))
                .labs(labs("Geoms.ribbon() — an arbitrary uncertainty band", "x", "value"));
    }
}
