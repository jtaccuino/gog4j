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
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.AnscombeDatasets;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleTheme;
import org.jtaccuino.gog.stat.SmoothMethod;
import org.jtaccuino.gog.theme.Theme;

/**
 * Example plot definitions for Anscombe's quartet.
 * <p>
 * Demonstrates four-data-set faceting and LOESS/LM smoothing. The quartet's
 * classic fixed axis limits are set per plot through
 * {@link Coord2D#xlim(double, double)}/{@link Coord2D#ylim(double, double)}.
 */
public class AnscombePlots {

    /** Utility class; not meant to be instantiated. */
    private AnscombePlots() {
    }

    /** {@return the raw scatter plot of the first quartet} */
    @SamplePlot(description = "Raw scatter of the first quartet with fixed axis limits and square points.",
            title = "Anscombe I: Raw Scatter Plot",
            dataset = SampleDataset.ANSCOMBE,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createRawScatterPlot() {
        var df = AnscombeDatasets.getQuartetData();
        return ggplot(df, aes().x("x").y("y"))
                .coord(Coord2D.cartesian().xlim(2, 20).ylim(2, 14))
                .geoms(point(Color.web("#3182bd"), 6.5).shape(PointShape.SQUARE))
                .labs(labs("Anscombe I: Raw Scatter Plot", "X", "Y"));
    }

    /** {@return the four quartets on one axis, color-coded via color aesthetic} */
    @SamplePlot(description = "All four quartets on one axis, color-coded by quartet on a dark theme.",
            title = "Anscombe II: Color-Mapped Quartets",
            dataset = SampleDataset.ANSCOMBE,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.CARTESIAN},
            themes = {SampleTheme.DARK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColorMappedPlot() {
        var df = AnscombeDatasets.getQuartetData();
        return ggplot(df, aes().x("x").y("y").color("quartet"))
                .coord(Coord2D.cartesian().xlim(2, 20).ylim(2, 14))
                .geoms(point().size(5.0))
                .theme(Theme.theme_dark())
                .labs(labs("Anscombe II: Color-Mapped Quartets", "X", "Y"));
    }

    /** {@return the faceted quartet plot with LOESS/LM smoothing} */
    @SamplePlot(description = "Quartets faceted in a wrap grid with linear smooth fits and red points.",
            title = "Anscombe III: The final conventional Quartet",
            dataset = SampleDataset.ANSCOMBE,
            geoms = {SampleGeom.SMOOTH, SampleGeom.POINT},
            coords = {SampleCoord.CARTESIAN},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createFinalFacettedPlot() {
        var df = AnscombeDatasets.getQuartetData();
        return ggplot(df, aes().x("x").y("y"))
                .coord(Coord2D.cartesian().xlim(2, 20).ylim(2, 14))
                .geoms(
                    smooth(SmoothMethod.LM, true).fullrange(true),
                    point(Color.web("#e31a1c"), 7.0)
                )
                .facets(wrap("quartet", 2))
                .labs(labs("Anscombe III: The final conventional Quartet", "Predictor (X)", "Response (Y)"))
                .theme(t -> t.gridLineColor(Color.web("#cccccc")).gridLineWidth(1.2));
    }
}
