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
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.stat.Stats.smooth;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * Example plots that use the {@link org.jtaccuino.gog.stat.StatSmooth}
 * statistical transformation <em>directly</em>, bypassing the convenience
 * {@code Geoms.smooth()} layer: each figure reads a {@code (x, y)} pair out of
 * the Motor Trend car-road-tests (mtcars) data and draws the returned fit with
 * {@code Geoms.line()}.
 * <p>
 * This is the stat-as-data-transform story from the feature-parity plan: a stat is
 * pure computation over {@code (x, y)} with no drawing of its own, and its
 * output columns are all a geometry needs to render. The {@code smooth(...)}
 * builder resolves the {@link org.jtaccuino.gog.spi.DataExtractor} internally
 * and {@code .fit()} hands back a frame ready for {@code Geoms.line()} — the
 * concrete frame type never leaks into the stat API.
 *
 * @see org.jtaccuino.gog.stat.Stats
 * @see org.jtaccuino.gog.stat.StatSmooth
 * @see MtcarsDatasets
 */
public class StatPlots {

    /** Utility class; not meant to be instantiated. */
    private StatPlots() {
    }

    private static final Color LOESS = Color.web("#e31a1c");
    private static final Color LM = Color.web("#3182bd");
    private static final Color WIGGLY = Color.web("#756bb1");
    private static final Color EXTRAP = Color.web("#31a354");

    /**
     * 1. A LOESS fit with a moderate span — {@code Stats.smooth(method =
     * "loess", span = 0.6)} — drawn with {@code Geoms.line()}.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "LOESS fit with a moderate span drawn as a line over mtcars.",
            title = "StatSmooth: LOESS fit (span = 0.6)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.LINE},
            stats = {SampleStat.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLoess() {
        return ggplot(smooth(MtcarsDatasets.loadMtcars(), "wt", "mpg").span(0.6).fit(),
                aes().x("x").y("y"))
                .geoms(line(LOESS, 2.5))
                .labs(labs("StatSmooth: LOESS fit (span = 0.6)", "weight (1,000 lb)", "miles per gallon"));
    }

    /**
     * 2. A linear-model fit — {@code Stats.smooth(method = "lm", se = FALSE)} —
     * a straight least-squares line, again through the smoothing stat.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Straight least-squares line from the linear-model smoothing stat.",
            title = "StatSmooth: linear-model fit",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.LINE},
            stats = {SampleStat.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLm() {
        return ggplot(smooth(MtcarsDatasets.loadMtcars(), "wt", "mpg").lm().se(false).fit(),
                aes().x("x").y("y"))
                .geoms(line(LM, 2.5))
                .labs(labs("StatSmooth: linear-model fit", "weight (1,000 lb)", "miles per gallon"));
    }

    /**
     * 3. The {@code span} parameter — a small span (0.3) tracks the local wobble
     * of the data more closely than the default, so the same LOESS stat yields a
     * visibly wigglier curve.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "LOESS fit with a small span tracing the local wobble closely.",
            title = "StatSmooth: LOESS span = 0.3 (wiggly)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.LINE},
            stats = {SampleStat.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSpan() {
        return ggplot(smooth(MtcarsDatasets.loadMtcars(), "wt", "mpg").span(0.3).se(false).fit(),
                aes().x("x").y("y"))
                .geoms(line(WIGGLY, 2.5))
                .labs(labs("StatSmooth: LOESS span = 0.3 (wiggly)", "weight (1,000 lb)", "miles per gallon"));
    }

    /**
     * 4. {@code fullrange} extrapolation — the LOESS fit is evaluated over a
     * {@code [globalMin, globalMax]} grid wider than the mtcars data, so the
     * returned curve extends left and right beyond the observed {@code wt} range.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "LOESS fit extrapolated beyond the observed range over a wider grid.",
            title = "StatSmooth: fullrange extrapolation to [0, 6.5]",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.LINE},
            stats = {SampleStat.SMOOTH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFullrange() {
        return ggplot(smooth(MtcarsDatasets.loadMtcars(), "wt", "mpg")
                        .span(0.6).se(false).fullrange(true).globalRange(0.0, 6.5).fit(),
                aes().x("x").y("y"))
                .geoms(line(EXTRAP, 2.5))
                .labs(labs("StatSmooth: fullrange extrapolation to [0, 6.5]", "weight (1,000 lb)", "miles per gallon"));
    }
}
