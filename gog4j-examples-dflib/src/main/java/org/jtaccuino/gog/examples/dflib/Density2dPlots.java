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
import static org.jtaccuino.gog.Geoms.density2d;
import static org.jtaccuino.gog.Geoms.density2dFilled;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleXContinuous;
import static org.jtaccuino.gog.scale.Scales.scaleYContinuous;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.dflib.data.FaithfulDatasets;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.Expansion;

/**
 * {@code Geoms.density2d()} examples, following the reference documentation of
 * by default: a two-dimensional Gaussian kernel-density estimate over the points
 * mapped to {@code aes(x)} and {@code aes(y)}, drawn as contour lines — the
 * remedy against over-plotted scatter — or as filled contour bands.
 * <p>
 * The canonical Old Faithful example pairs the contours with the raw points;
 * the reference examples zoom the view to the interesting range, adjust the
 * bandwidth, and map an aesthetic to a categorical variable to get one set of
 * contours per category.
 *
 * @see org.jtaccuino.gog.dflib.data.FaithfulDatasets
 * @see org.jtaccuino.gog.dflib.data.DiamondsDatasets
 */
public class Density2dPlots {

    /** Utility class; not meant to be instantiated. */
    private Density2dPlots() {
    }

    /** {@return the basic Old Faithful density-2d contour plot} */
    @SamplePlot(description = "Two-dimensional density contours over Old Faithful eruption scatter points.",
            title = "Contours of Eruption Density (Geoms.density2d)",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createBasic() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(point().size(2.0))
                .geoms(density2d())
                .labs(labs("1. Contours of Eruption Density (Geoms.density2d)", "Eruptions (min)", "Waiting Time (min)"));
    }

    /** {@return the zoomed Old Faithful density-2d plot (xlim(0.5, 6), ylim(40, 110))} */
    @SamplePlot(description = "Density contours zoomed to reveal two eruption-time clusters.",
            title = "Zoomed to Two Clusters (xlim(0.5, 6), ylim(40, 110))",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createZoomed() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .coord(Coord2D.cartesian().xlim(0.5, 6).ylim(40, 110))
                .geoms(point().size(2.0))
                .geoms(density2d().bins(12))
                .labs(labs("2. Zoomed to Two Clusters (xlim(0.5, 6), ylim(40, 110))", "Eruptions (min)", "Waiting Time (min)"));
    }

    /** {@return the density-2d plot with a wider smoothing bandwidth (adjust = 2)} */
    @SamplePlot(description = "Contours of eruption density smoothed with a doubled bandwidth.",
            title = "Smoother Contours (adjust = 2)",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createAdjust() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(point().size(2.0))
                .geoms(density2d().adjust(2.0))
                .labs(labs("3. Smoother Contours (adjust = 2)", "Eruptions (min)", "Waiting Time (min)"));
    }

    /** {@return the density-2d plot with a contour set per diamond cut} */
    @SamplePlot(description = "Contour sets per diamond cut, colour-mapped with sparse bins.",
            title = "One Contour Set per Cut (colourMapping = cut))",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY2D},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColourMapped() {
        var diamonds = DiamondsDatasets.loadDiamonds().head(1000);
        return ggplot(diamonds, aes().x("x").y("y").color("cut"))
                .geoms(density2d().bins(5))
                .labs(labs("4. One Contour Set per Cut (colourMapping = cut))", "Diamond Width x (mm)", "Diamond Width y (mm)"));
    }

    /** {@return the density-2d plot with filled contour bands} */
    @SamplePlot(description = "Filled contour bands of eruption density over the raw points.",
            title = "Filled Contour Bands (Geoms.density2dFilled)",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D_FILLED},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA, SampleFeature.LAYERS})
    public static Plot<DataFrame> createFilled() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(point().size(2.0))
                .geoms(density2dFilled().alpha(0.5))
                .labs(labs("5. Filled Contour Bands (Geoms.density2dFilled)", "Eruptions (min)", "Waiting Time (min)"));
    }

    /** {@return the density-2d plot with filled bands and black contour lines} */
    @SamplePlot(description = "Filled density bands outlined with black contour lines.",
            title = "Filled Bands with Black Contour Lines",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D_FILLED, SampleGeom.DENSITY2D},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA, SampleFeature.LAYERS})
    public static Plot<DataFrame> createFilledLines() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(point().size(2.0))
                .geoms(density2dFilled().alpha(0.4))
                .geoms(density2d().lineWidth(0.75).color(Color.web("#000000")))
                .labs(labs("6. Filled Bands with Black Contour Lines", "Eruptions (min)", "Waiting Time (min)"));
    }

    /** {@return the density-2d plot of fuel economy with dense contours (bins = 15)} */
    @SamplePlot(description = "Dense density contours over fuel-economy scatter with 15 bins.",
            title = "Dense Contours on Fuel Economy (bins = 15)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createMpgBins() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(point().size(1.5))
                .geoms(density2d().bins(15))
                .labs(labs("7. Dense Contours on Fuel Economy (bins = 15)", "Engine Displacement (L)", "Highway MPG"));
    }
}
