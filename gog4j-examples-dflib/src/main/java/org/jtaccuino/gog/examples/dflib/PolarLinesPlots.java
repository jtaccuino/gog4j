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
import static org.jtaccuino.gog.Coords.coordPolar;
import static org.jtaccuino.gog.Coords.coordRadial;
import static org.jtaccuino.gog.Geoms.abline;
import static org.jtaccuino.gog.Geoms.area;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.density2d;
import static org.jtaccuino.gog.Geoms.hline;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Geoms.vline;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleXContinuous;
import static org.jtaccuino.gog.scale.Scales.scaleYContinuous;

import java.time.LocalDate;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.ScaleExpansion;
import org.jtaccuino.gog.dflib.data.FaithfulDatasets;
import org.jtaccuino.gog.dflib.data.MeatDatasets;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.dflib.data.PolarDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.Expansion;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * The polar figures that round out the {@code coordPolar()} / {@code
 * coordRadial()} coverage beyond wedges and pies: reference lines, lines and
 * smooths wrapped around the disc, heatmaps, density roses, and stacked-area
 * ribbons. Each geometry either routes its points through the coordinate
 * system or draws an explicit polar shape, so the same layer works in
 * Cartesian and polar layouts.
 *
 * @see org.jtaccuino.gog.examples.dflib.PolarPlots
 * @see org.jtaccuino.gog.examples.dflib.PolarRadialPlots
 * @see org.jtaccuino.gog.dflib.data.PolarDatasets
 */
public class PolarLinesPlots {

    /** Utility class; not meant to be instantiated. */
    private PolarLinesPlots() {
    }

    /** Beef production, one series per row, the spiral of the figures below. */
    private static DataFrame beefProduction() {
        return MeatDatasets.getProductionData().rows(row ->
                "beef".equals(String.valueOf(row.get("animal")))).select();
    }

    /** {@return a spiral of beef production (Geoms.line + coordPolar)} */
    @SamplePlot(description = "Line of beef production over time wrapped around the disc as a spiral.",
            title = "Spiral (Geoms.line + coordPolar)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.LINE},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSpiral() {
        var df = beefProduction();
        return ggplot(df, aes().x("date").y("weight"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(line(Color.web("#2c7bb6"), 1.4))
                .coord(coordPolar())
                .labs(labs("17. Spiral (Geoms.line + coordPolar)", "Date", "Beef production"));
    }

    /** {@return a radial scatter with a LOESS smooth and confidence band} */
    @SamplePlot(description = "Radial fan with points, a LOESS smooth and its confidence band.",
            title = "Radial Smooth (LOESS on the fan)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            coords = {SampleCoord.RADIAL},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadialSmooth() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return ggplot(df, aes().x("disp").y("mpg"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(
                        point().size(3.5),
                        smooth(SmoothMethod.LOESS, true).span(0.6)
                )
                .coord(coordRadial().start(-0.4 * Math.PI).end(0.4 * Math.PI).innerRadius(0.3))
                .labs(labs("18. Radial Smooth (LOESS on the fan)", "Disp", "MPG"));
    }

    /** {@return a polar rose with a dashed significance ring} */
    @SamplePlot(description = "Stacked rose of diamond counts with a dashed reference ring.",
            title = "Significance Ring (Geoms.hline on the rose)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.COL, SampleGeom.HLINE},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createSignificanceRing() {
        var df = PolarDatasets.loadClarityCutCounts();
        return ggplot(df, aes().x("clarity").y("count").fill("cut"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(col(Position.STACK).widthFactor(1.0))
                .geoms(hline(1000).color(Color.web("#d62728")).dashed().width(1.3).annotate("1,000 diamonds"))
                .coord(coordPolar())
                .labs(labs("19. Significance Ring (Geoms.hline on the rose)", "Clarity", "Count")
                        .legendTitle("Cut"));
    }

    /** {@return a beef-production spiral with a cutoff spoke at 1970} */
    @SamplePlot(description = "Beef spiral with a dashed spoke marking the 1970 cutoff.",
            title = "Cut-off Spoke (Geoms.vline on the spiral)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.LINE, SampleGeom.VLINE},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createCutoffSpoke() {
        var df = beefProduction();
        var targetYear = (double) LocalDate.of(1970, 1, 1).toEpochDay();
        return ggplot(df, aes().x("date").y("weight"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(line(Color.web("#2c7bb6"), 1.4))
                .geoms(vline(targetYear).color(Color.web("#d62728")).dashed().width(1.3).annotate("1970"))
                .coord(coordPolar())
                .labs(labs("20. Cut-off Spoke (Geoms.vline on the spiral)", "Date", "Beef production"));
    }

    /** {@return a radial fan with an identity trendline through it} */
    @SamplePlot(description = "Radial fan with points and a dashed trendline through them.",
            title = "Radial Trendline (Geoms.abline through the fan)",
            dataset = SampleDataset.MTCARS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            coords = {SampleCoord.RADIAL},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createIdentityLine() {
        var df = MtcarsDatasets.loadNumericMtcars();
        return ggplot(df, aes().x("disp").y("mpg"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(
                        point().size(3.5),
                        abline(0.05, 10).color(Color.web("#d62728")).dashed().width(1.3)
                )
                .coord(coordRadial().start(-0.4 * Math.PI).end(0.4 * Math.PI).innerRadius(0.3))
                .labs(labs("21. Radial Trendline (Geoms.abline through the fan)", "Disp", "MPG"));
    }

    /** {@return a circular heatmap of diamond cut by clarity} */
    @SamplePlot(description = "Heatmap of clarity by cut tiled around the disc with viridis fill.",
            title = "Circular Heatmap (Geoms.tile + coordPolar)",
            dataset = SampleDataset.POLAR,
            geoms = {SampleGeom.TILE},
            coords = {SampleCoord.POLAR},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCircularHeatmap() {
        var df = PolarDatasets.loadClarityCutCounts();
        return ggplot(df, aes().x("clarity").y("cut").fill("count"))
                .geoms(tile("viridis", 1.0))
                .coord(coordPolar())
                .labs(labs("22. Circular Heatmap (Geoms.tile + coordPolar)", "Clarity", "Cut"));
    }

    /** {@return a density rose of the Old Faithful eruption lengths} */
    @SamplePlot(description = "Density curve of eruption lengths drawn on a half radial fan.",
            title = "Density Rose (Geoms.density on a half fan)",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.DENSITY},
            coords = {SampleCoord.RADIAL},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDensityRose() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(density().alpha(0.7))
                .coord(coordRadial().start(-0.5 * Math.PI).end(0.5 * Math.PI).expand(ScaleExpansion.OFF))
                .labs(labs("23. Density Rose (Geoms.density on a half fan)", "Eruptions", "Density"));
    }

    /** {@return stacked spiral ribbons of meat production over time} */
    @SamplePlot(description = "Stacked-area ribbons of meat production wrapped around a polar spiral.",
            title = "Spiral Ribbons (Geoms.area + coordPolar)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.AREA},
            coords = {SampleCoord.POLAR},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSpiralRibbons() {
        var df = MeatDatasets.getProductionData().rows(row -> {
            var animal = String.valueOf(row.get("animal"));
            return "beef".equals(animal) || "pork".equals(animal) || "broilers".equals(animal);
        }).select();
        return ggplot(df, aes().x("date").y("weight").fill("animal"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(area())
                .coord(coordPolar())
                .labs(labs("24. Spiral Ribbons (Geoms.area + coordPolar)", "Date", "Production"));
    }

    /** {@return radial density-2d contours on the faithful data} */
    @SamplePlot(description = "Radial fan with faint points and density-2d contour lines.",
            title = "Radial Contours (Geoms.density2d on a fan)",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.POINT, SampleGeom.DENSITY2D},
            coords = {SampleCoord.RADIAL},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadialContours() {
        var df = FaithfulDatasets.loadFaithful();
        return ggplot(df, aes().x("eruptions").y("waiting"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(
                        point().size(1.2).opacity(0.25),
                        density2d()
                )
                .coord(coordRadial().start(-0.4 * Math.PI).end(0.4 * Math.PI).innerRadius(0.3))
                .labs(labs("25. Radial Contours (Geoms.density2d on a fan)", "Eruptions", "Waiting"));
    }
}
