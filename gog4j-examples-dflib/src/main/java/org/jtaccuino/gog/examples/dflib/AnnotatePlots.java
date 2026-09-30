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
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Geoms;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.layer.Annotations;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.scale.ScaleTransform;

/**
 * {@code annotate()} examples, mirroring the {@code annotate()}
 * documentation: data-free layers that carry their own scalar coordinates and
 * draw on top of the mapped geometry, plus a log-axis figure showing the
 * automatic sub-decade minor ticks.
 * <p>
 * {@link #createAnnotations()} layers a highlighted rectangle, a dashed
 * reference segment, a labelled point, and straight reference lines atop a
 * scatter. {@link #createLogticks()} shows the sub-decade axis tick marks of a
 * {@code coordTrans(…, LOG10)} axis, whose diminishing spacing reads like
 * the {@code log-axis tick annotation()}.
 *
 * @see org.jtaccuino.gog.Plot#annotate(org.jtaccuino.gog.layer.Layer[])
 */
public class AnnotatePlots {

    /** Utility class; not meant to be instantiated. */
    private AnnotatePlots() {
    }

    /** {@return a scatter annotated with rect, segment, point, text, and hline layers} */
    @SamplePlot(description = "Scatter overlaid with rectangle, segment, point, text, and hline annotations.",
            title = "annotate(): rect, segment, point, text, hline",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE})
    public static Plot<DataFrame> createAnnotations() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(2.0).color(Color.web("#4d4d4d")))
                .annotate(
                        Annotations.rect(2.0, 20.0, 5.0, 35.0, Color.web("#fb8072", 0.3)),
                        Annotations.segment(2.0, 20.0, 5.0, 35.0, Color.web("#d62728"), 1.2),
                        Annotations.point(5.0, 40.0, 5.0, Color.web("#2ca02c")),
                        Annotations.text(5.2, 40.0, "annotate() point"),
                        Annotations.hline(30.0).color(Color.web("#9467bd")).width(1.0).dashed())
                .labs(labs("annotate(): rect, segment, point, text, hline",
                        "Displacement (L)", "Highway MPG"));
    }

    /** {@return an identity line on a linear x log10 y axis showing the log axis minor ticks} */
    @SamplePlot(description = "Identity line on a log10 y-axis showing sub-decade minor tick marks.",
            title = "logticks: sub-decade tick marks on a log10 axis",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.LINE},
            coords = {SampleCoord.TRANS},
            features = {SampleFeature.TWO_D, SampleFeature.LOG_TICKS})
    public static Plot<DataFrame> createLogticks() {
        int n = 10_000;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i + 1;
            y[i] = i + 1;
        }
        var df = DataFrame.byColumn("x", "y")
                .of(Series.ofDouble(x), Series.ofDouble(y));
        return ggplot(df, aes().x("x").y("y"))
                .geoms(Geoms.line())
                .coord(Coords.coordTrans(null, ScaleTransform.LOG10))
                .theme(t -> t.showMinorTicks(true))
                .labs(labs("logticks: sub-decade tick marks on a log10 axis",
                        "x", "log10 y"));
    }

    /** {@return a linear-axis scatter with the theme-level minor grid turned on} */
    @SamplePlot(description = "Scatter with the theme-level minor grid enabled on a linear axis.",
            title = "minor grid on a linear axis",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.MINOR_GRID})
    public static Plot<DataFrame> createMinorGridLinear() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(Geoms.point().size(2.0))
                .theme(t -> t.showMinorGrid(true))
                .labs(labs("minor grid on a linear axis",
                        "Displacement (L)", "Highway MPG"));
    }
}
