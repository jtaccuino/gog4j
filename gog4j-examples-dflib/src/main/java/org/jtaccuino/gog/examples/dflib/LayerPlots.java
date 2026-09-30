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
import static org.jtaccuino.gog.Aes.aesOf;
import static org.jtaccuino.gog.AesValue.ComputedVariable.COUNT;
import static org.jtaccuino.gog.AesValue.ComputedVariable.DENSITY;
import static org.jtaccuino.gog.AesValue.afterStat;
import static org.jtaccuino.gog.Geoms.bar;
import static org.jtaccuino.gog.Geoms.barGeom;
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.layer.Positions.identity;
import static org.jtaccuino.gog.stat.Stats.count;

import java.util.Map;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * {@code Plot.layer(...)}, {@code aesOf(map)}, and computed {@code afterStat()}
 * aesthetics — the core-infrastructure additions of the feature-parity roadmap.
 * <p>
 * The first figure builds a {@code Geoms.bar()} with the factory sugar: since
 * {@code Geoms.bar()} runs {@code Stats.count()} by default, {@code bar()} alone
 * counts the rows per class. The second colours the very same bars by their
 * count through {@code afterStat(count)}, a variable that exists only in the
 * stat's output, via per-geom {@code mapping(...)}. The third maps the whole
 * plot with {@code aesOf(Map)} and re-maps one layer's fill to the bin
 * density. The fourth assembles {@code Geoms.bar + Stats.count} from parts with
 * the all-in-one {@code Plot#layer(...)} call,
 * using the {@code Stats.count()} and {@code Positions.identity()} factories.
 *
 * @see org.jtaccuino.gog.dflib.data.MpgDatasets
 */
public class LayerPlots {

    /** Utility class; not meant to be instantiated. */
    private LayerPlots() {
    }

    /**
     * The factory-sugar {@code Geoms.bar()}: {@code bar()} inherits
     * {@code Stats.count()}, so it counts the observations per vehicle class —
     * the {@code aes(x = class)} mapping is all that is needed.
     *
     * @return the count bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bar chart counting observations per vehicle class via the default Stats.count().",
            title = "Geoms.bar via bar() — Stats.count() is the default",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLayerBarCount() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class"))
                .geoms(bar().fill(Color.web("#3182bd")))
                .labs(labs("Geoms.bar via bar() — Stats.count() is the default",
                        "Vehicle Class", "Count"));
    }

    /**
     * The computed-aesthetic highlight: {@code afterStat(count)} in the
     * layer's own {@code mapping(...)} colours each bar by its count through
     * the continuous colour scale, exactly as
     * {@code afterStat(count)} in a fill mapping does in the reference implementation.
     *
     * @return the afterStat bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bars coloured by their count through the afterStat(count) computed aesthetic.",
            title = "Geoms.bar fill = afterStat(count) via bar().mapping(...)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLayerBarAfterStat() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class"))
                .geoms(bar().mapping(aes().fill(afterStat(COUNT))))
                .labs(labs("Geoms.bar fill = afterStat(count) via bar().mapping(...)",
                        "Vehicle Class", "Count"));
    }

    /**
     * The {@code aesOf(Map)} and per-geom {@code mapping(...)} paths on a plain
     * {@code Geoms.histogram()}: the plot-wide mapping is built from a string
     * map ({@code {x: hwy}}), and the layer's local mapping fills every bin by
     * the bin density computed by {@code Stats.bin()}.
     *
     * @return the aesOf histogram {@link Plot}
     */
    @SamplePlot(description = "Histogram mapped from a string map with bins coloured by bin density.",
            title = "aesOf(map) + histogram fill = afterStat(density)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.HISTOGRAM},
            stats = {SampleStat.BIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAesOfHistogram() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aesOf(Map.of("x", "hwy")))
                .geoms(histogram().mapping(aes().fill(afterStat(DENSITY))))
                .labs(labs("aesOf(map) + histogram fill = afterStat(density)",
                        "Highway MPG", "Density"));
    }

    /**
     * Assembling {@code Geoms.bar + Stats.count} from parts — the reference
     * {@code Plot#layer(...)} counterpart, using
     * only factories: the bare geometry from {@link org.jtaccuino.gog.Geoms#barGeom()},
     * the statistic from {@link org.jtaccuino.gog.stat.Stats#count()}, and the
     * position from {@link org.jtaccuino.gog.layer.Positions#identity()}. The
     * layer inherits the plot-wide {@code aes(x = class)} mapping and runs
     * {@code Stats.count()} to produce one bar per vehicle class with a height of
     * its observation count.
     *
     * @return the assembled bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bar chart assembled from explicit geom, stat, position, and parameter factories.",
            title = "Geoms.bar from parts via Plot#layer(...)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            positions = {SamplePosition.IDENTITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLayerFromParts() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class"))
                .layer(barGeom(), count(), identity(), aes(),
                        LayerParams.builder().fill(Color.web("#3182bd")).build())
                .labs(labs("Geoms.bar from parts via Plot#layer(...)",
                        "Vehicle Class", "Count"));
    }
}
