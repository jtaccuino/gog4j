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
import static org.jtaccuino.gog.Geoms.curve;
import static org.jtaccuino.gog.Geoms.path;
import static org.jtaccuino.gog.Geoms.segment;
import static org.jtaccuino.gog.Geoms.step;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Example plots for the line-family geoms — {@code Geoms.step()},
 * {@code Geoms.path()}, {@code Geoms.segment()} and {@code Geoms.curve()} — added
 * in Sprint 1 of the feature-parity plan.
 * <p>
 * Each figures the geometry the others iterate on: dependents stepping over an
 * independent axis, an arbitrary trajectory in row order, and one directed
 * segment/curve per row between an {@code (x, y)} start and {@code (xend, yend)}
 * end point.
 */
public class WirePlots {

    /** Utility class; not meant to be instantiated. */
    private WirePlots() {
    }

    /** {@return a tiny synthetic frame of dependent values and endpoint columns} */
    private static DataFrame stepData() {
        var x = new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        var y = new double[]{1, 2, 2, 4, 4, 3, 3, 5, 5, 6, 6};
        return DataFrame.byColumn("x", "y").of(Series.ofDouble(x), Series.ofDouble(y));
    }

    /** {@return a coarse walk used as a trajectory for Geoms.path} */
    private static DataFrame walkData() {
        var x = new double[]{0, 1, 1, 2, 3, 3, 4, 4, 5};
        var y = new double[]{0, 0, 2, 2, 1, 3, 3, 4, 4};
        return DataFrame.byColumn("x", "y").of(Series.ofDouble(x), Series.ofDouble(y));
    }

    /** {@return a fan of directed start→end pairs for segment/curve} */
    private static DataFrame fanData() {
        var x = new double[]{0, 1, 2, 3, 4, 5};
        var y = new double[]{0, 1, 2, 3, 4, 5};
        var xe = new double[]{1, 2, 3, 4, 5, 6};
        var ye = new double[]{3, 4, 1, 2, 5, 6};
        return DataFrame.byColumn("x", "y", "xend", "yend")
                .of(Series.ofDouble(x), Series.ofDouble(y), Series.ofDouble(xe), Series.ofDouble(ye));
    }

    /** {@return a Geoms.step staircase plot} */
    @SamplePlot(description = "Staircase line stepping between dependent values across the x-axis.",
            title = "Geoms.step()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.STEP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStep() {
        return ggplot(stepData(), aes().x("x").y("y"))
                .geoms(step(Color.web("#e31a1c"), 2.0))
                .labs(labs("Geoms.step()", "x", "y"));
    }

    /** {@return a Geoms.path trajectory plot} */
    @SamplePlot(description = "Trajectory connecting points in row order across a synthetic walk.",
            title = "Geoms.path()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.PATH},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createPath() {
        return ggplot(walkData(), aes().x("x").y("y"))
                .geoms(path(Color.web("#3182bd"), 2.0))
                .labs(labs("Geoms.path()", "x", "y"));
    }

    /** {@return a Geoms.segment fan of directed lines} */
    @SamplePlot(description = "Fan of directed line segments from each start point to its endpoint.",
            title = "Geoms.segment()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SEGMENT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSegment() {
        return ggplot(fanData(), aes().x("x").y("y").xend("xend").yend("yend"))
                .geoms(segment(Color.web("#756bb1"), 2.0))
                .labs(labs("Geoms.segment()", "x", "y"));
    }

    /** {@return a Geoms.curve fan with bowed arcs} */
    @SamplePlot(description = "Fan of curved segments bowing between paired start and end points.",
            title = "Geoms.curve()",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.CURVE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCurve() {
        return ggplot(fanData(), aes().x("x").y("y").xend("xend").yend("yend"))
                .geoms(curve(Color.web("#31a354"), 2.0, 0.6))
                .labs(labs("Geoms.curve()", "x", "y"));
    }
}
