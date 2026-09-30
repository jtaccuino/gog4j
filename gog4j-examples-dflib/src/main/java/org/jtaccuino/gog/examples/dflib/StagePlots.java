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
import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.AesValue.ComputedVariable.COUNT;
import static org.jtaccuino.gog.AesValue.ComputedVariable.PROP;
import static org.jtaccuino.gog.AesValue.afterScale;
import static org.jtaccuino.gog.AesValue.afterStat;
import static org.jtaccuino.gog.AesValue.stage;
import static org.jtaccuino.gog.Geoms.bar;
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.ArrayList;
import java.util.List;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleStat;

/**
 * Recomputable aesthetics on top of {@code Plot.layer(...)}: the
 * {@code afterStat} expression form, {@code afterScale} references, and the
 * {@code stage(start, afterStat, afterScale)} pipeline that feeds one into
 * the next.
 * <p>
 * The first figure runs a Java function over the stat's output — the
 * {@code afterStat(count / max(count))} of the default — shading each histogram bin
 * by its share of the largest bin. The second binds a bar's fill to its own
 * stroke through {@code fill = afterScale("color")}, so every bar is filled
 * with the colour the colour scale mapped its count to. The third chains both
 * stages: {@code stage(afterStat(prop), afterScale = color)} maps each
 * cylinder class's share of total through the colour scale. The fourth places
 * the points with {@code y = afterScale("x")}, echoing the transformed x value
 * onto the y axis to trace the identity diagonal.
 *
 * @see org.jtaccuino.gog.dflib.data.MpgDatasets
 */
public class StagePlots {

    /** Utility class; not meant to be instantiated. */
    private StagePlots() {
    }

    /**
     * The {@code afterStat} function form: {@code afterStat(sd -> ...)} runs
     * a lambda over the computed {@code StatData}, so a {@code count} column
     * saved by {@code Stats.count()} can be re-expressed as its share of the
     * maximum, exactly like the {@code afterStat(count / max(count))}.
     *
     * @return the afterStat-expression histogram {@link Plot}
     */
    @SamplePlot(description = "Histogram with bins shaded by each count's share of the maximum.",
            title = "Geoms.histogram fill = afterStat(count / max(count))",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.HISTOGRAM},
            stats = {SampleStat.BIN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAfterStatNCount() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("hwy"))
                .geoms(histogram().mapping(aes().fill(afterStat(
                        sd -> {
                            var out = new ArrayList<Double>();
                            double max = 0.0;
                            for (var v : sd.columnAsDoubles("count")) {
                                max = Math.max(max, v);
                            }
                            for (var v : sd.columnAsDoubles("count")) {
                                out.add(v / max);
                            }
                            return List.copyOf(out);
                        },
                        "count"))))
                .labs(labs("Geoms.histogram fill = afterStat(count / max(count))",
                        "Highway MPG", "Count"));
    }

    /**
     * {@code afterScale("color")} on a fill: the bar's colour aesthetic is
     * {@code afterStat(count)}, and the fill reads the very colour the colour
     * scale assigned to that count — the stroke and the fill of every bar agree
     * because they both answer to the same scale output.
     *
     * @return the afterScale fill bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bars filled with the colour their count received from the colour scale.",
            title = "Geoms.bar fill = afterScale(\"color\") echoes color = afterStat(count)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAfterScaleFill() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cyl").color(afterStat(COUNT)).fill(afterScale("color")))
                .geoms(bar())
                .labs(labs("Geoms.bar fill = afterScale(\"color\") echoes color = afterStat(count)",
                        "Cylinders", "Count"));
    }

    /**
     * The full {@code stage(...)} pipeline: the aesthetic starts at
     * {@code afterStat(prop)} — each cylinder class's share of the total
     * observations — and is then pushed through the colour scale by the
     * {@code afterScale = "color"} stage. One value follows the other exactly
     * the default stages its aesthetics.
     *
     * @return the staged bar-plot {@link Plot}
     */
    @SamplePlot(description = "Bars coloured by cylinder-class share staged through the colour scale.",
            title = "Geoms.bar fill = stage(afterStat(prop), afterScale = color)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BAR},
            stats = {SampleStat.COUNT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStage() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("cyl").fill(stage(afterStat(PROP)).afterScale("color").build()))
                .geoms(bar())
                .labs(labs("Geoms.bar fill = stage(afterStat(prop), afterScale = color)",
                        "Cylinders", "Proportion"));
    }

    /**
     * {@code afterScale("x")} on a position: each point's y coordinate is the
     * x value mapped through the x scale's transform, so plotting
     * {@code y = afterScale("x")} against the raw x traces the identity
     * diagonal — every point lands exactly on the line {@code y = x}. The
     * cylinder count is re-cast to a categorical text column so the colour
     * scale shows the four/six/eight classes as discrete legend keys (by default
     * would infer the same discrete scale from the counted integer codes).
     *
     * @return the afterScale-position scatter {@link Plot}
     */
    @SamplePlot(description = "Points traced along the identity diagonal with y echoing scaled x.",
            title = "Geoms.point y = afterScale(\"x\") — the identity diagonal",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAfterScalePosition() {
        var df = MpgDatasets.loadMpg().cols("cyl").merge($col("cyl").castAsStr());
        return ggplot(df, aes().x("hwy").y(afterScale("x")).color("cyl"))
                .geoms(point().opacity(0.6))
                .labs(labs("Geoms.point y = afterScale(\"x\") — the identity diagonal",
                        "Highway MPG", "Highway MPG (afterScale x)"));
    }
}
