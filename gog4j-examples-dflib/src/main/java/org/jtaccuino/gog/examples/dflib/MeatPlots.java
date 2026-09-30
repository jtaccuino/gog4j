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
import static org.jtaccuino.gog.Coords.coordFlip;
import static org.jtaccuino.gog.Facets.wrap;
import static org.jtaccuino.gog.Geoms.area;
import static org.jtaccuino.gog.Geoms.col;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.smooth;
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.labs.Labs.title;
import static org.jtaccuino.gog.scale.Scales.scaleXContinuous;
import static org.jtaccuino.gog.scale.Scales.scaleYContinuous;
import static org.jtaccuino.gog.stat.Averaging.rollingWindow;

import java.time.LocalDate;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MeatDatasets;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.scale.Expansion;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Example plot definitions using the USDA meat production dataset.
 * <p>
 * Demonstrates stacked area charts, time-series lines, small multiples,
 * heatmaps, and faceted bar charts with dodge/stack layouts.
 */
public class MeatPlots {

    /** Utility class; not meant to be instantiated. */
    private MeatPlots() {
    }

    /** {@return a trend plot of total meat production over time} */
    @SamplePlot(description = "Square points with a tight linear smooth over total meat production.",
            title = "Globaler Fleischtrend",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.POINT, SampleGeom.SMOOTH},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createSingleTrendLine() {
        return ggplot(MeatDatasets.getProductionData(), aes().x("date").y("weight"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(
                        point().shape(PointShape.SQUARE).size(5.0).color(Color.BLUE),
                        smooth(aes().fill("animal")).method(SmoothMethod.LM).span(0.15)
                )
                .labs(labs("1. Globaler Fleischtrend", "Jahr", "Produktion (Mio. Pfund)"));
    }

    /** {@return a multi-line trend plot of meat production coloured by animal} */
    @SamplePlot(description = "Twelve-month rolling mean lines colored by animal with linear trends.",
            title = "Trends by Animal Type (Color Mapped)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.LINE, SampleGeom.POINT, SampleGeom.SMOOTH},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createMultipleTrendLines() {
        return ggplot(MeatDatasets.getProductionData(), aes().x("date").y("weight").color("animal"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(line(rollingWindow(12).months(), 1.8),
                        // Optional tiny raw points in the background (for contrast)
                        point(Color.web("#555555", 0.15), 1.5).shape(PointShape.TRIANGLE),
                        smooth().se(false) // Linear trend per facet
                )
                .labs(labs("2. Trends by Animal Type (Color Mapped)", "Year", "Production (Million Pounds)"));
    }

    /** {@return a faceted time-series plot of meat production per animal} */
    @SamplePlot(description = "Time-series lines faceted by animal with per-group smoothing.",
            title = "Pecking Orders and Prime Cuts: True Grammar",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.LINE, SampleGeom.POINT, SampleGeom.SMOOTH},
            scales = {SampleScale.X_CONTINUOUS, SampleScale.Y_CONTINUOUS},
            facets = {SampleFacet.WRAP},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createFinalFacettedPlot() {
        // Map only the axes globally
        return ggplot(MeatDatasets.getProductionData(), aes().x("date").y("weight"))
                .scales(scaleXContinuous(Expansion.none())).scales(scaleYContinuous(Expansion.none()))
                .geoms(
                        line(rollingWindow(12).months(), 1.8),
                        point(Color.web("#e31a1c", 0.2), 2.5),
                        // Per-group smoothing identical to the conventional grammar
                        smooth(aes().fill("animal")).span(0.15)
                )
                .facets(wrap("animal", -1))
                .labs(labs("Pecking Orders and Prime Cuts: True Grammar", "Date", "Weight (million pounds)"))
                .theme(t -> t.gridLineColor(Color.web("#cccccc")).gridLineWidth(1.0));
    }

    /** {@return a dodged bar chart comparing meat production by animal} */
    @SamplePlot(description = "Dodged bar chart comparing January production by animal.",
            title = "Production Comparison (GeomBar Dodge)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.COL},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBarChartComparison() {
        // Use the original CSV dataset
        var rawDf = MeatDatasets.getProductionData();

        var modernDf = rawDf.rows(row -> {
            var dateObj = row.get("date");
            if (dateObj instanceof LocalDate date) {
                // Filter January snapshots for years 2005 onward
                return date.getYear() >= 2005 && date.getMonthValue() == 1;
            }
            return false;
        }).select();

        return ggplot(modernDf, aes().x("date").y("weight").fill("animal"))
                .geoms(
                        // Exactly as in the reference implementation: cluster bars side-by-side with 90% width utilization
                        col(Position.DODGE).widthFactor(0.9)
                )
                .labs(labs("4. Production Comparison (GeomBar Dodge)", "Year Snapshot", "Million Pounds"))
                .theme(t -> t.gridLineColor(Color.web("#dddddd")).gridLineWidth(1.0));
    }

    /**
     * Stage 5: Horizontal bar chart (coordFlip) showing annual total consumption by animal.
     *
     * @return the configured {@link Plot} instance
     */
    @SamplePlot(description = "Horizontal dodged bars of annual consumption with flipped coordinates.",
            title = "Horizontal Meat Production (Coord Flip)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.FLIP},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalBarChart() {
        var rawDf = MeatDatasets.getConsumptionPerYear();

        return ggplot(rawDf, aes().x("year").y("total_consumption").fill("animal"))
                .geoms(
                        col(Position.DODGE).widthFactor(0.8)
                )
                .coord(Coords.coordFlip())
                .labs(labs("Horizontal Meat Production (Coord Flip)", "Production (Million Pounds)", "Year"));
    }

    /** {@return a horizontal dodged bar chart of recent meat production} */
    @SamplePlot(description = "Horizontal dodged bars of recent beef, pork, and broiler production.",
            title = "Horizontal Dodge Bar Chart",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.FLIP},
            positions = {SamplePosition.DODGE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalDodgeBarChart() {
        var rawDf = MeatDatasets.getProductionData();

        // Filter beef, pork, and broilers for years 2021 to 2023 (January only)
        var clusterDf = rawDf.rows(row -> {
            var animal = row.get("animal") != null ? row.get("animal").toString() : "";
            var dateObj = row.get("date");

            if (dateObj instanceof LocalDate date) {
                var isTargetAnimal = "beef".equals(animal) || "pork".equals(animal) || "broilers".equals(animal);
                var isModernPeriod = date.getYear() >= 2000;
                var isJanuary = date.getMonthValue() == 1;

                return isTargetAnimal && isModernPeriod && isJanuary;
            }
            return false;
        }).select();

        return ggplot(clusterDf, aes().x("date").y("weight").fill("animal"))
                .geoms(
                        // Unbound factory uses the elastic, type-masked interface in DODGE mode
                        col(Position.DODGE).widthFactor(0.8)
                )
                .coord(coordFlip())
                .theme(t -> t.gridLineColor(Color.web("#dddddd")).gridLineWidth(1.0));
    }

    /** {@return a stacked area chart of meat production over time} */
    @SamplePlot(description = "Stacked area chart of meat production over time.",
            title = "Production Density Over Time (GeomArea)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.AREA},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAreaChart() {
        var rawDf = MeatDatasets.getProductionData();

        // Use beef, pork, and poultry data from 2015 onward for optimal visualization
        var modernDf = rawDf.rows(row -> {
            var animal = row.get("animal") != null ? row.get("animal").toString() : "";
            var dateObj = row.get("date");
            if (dateObj instanceof LocalDate date) {
                return ("beef".equals(animal) || "pork".equals(animal) || "broilers".equals(animal))
                        && date.getYear() >= 2000 && date.getMonthValue() == 1;
            }
            return false;
        }).select();

        return ggplot(modernDf, aes().x("date").y("weight").fill("animal"))
                .geoms(
                        area().position(Position.STACK)
                )
                .labs(labs("7. Production Density Over Time (GeomArea)", "Year", "Million Pounds"))
                .theme(t -> t.gridLineColor(Color.web("#e5e5e5")).gridLineWidth(0.8));
    }

    /** {@return the stacked bar chart of meat production over time} */
    @SamplePlot(description = "Stacked bars of cumulative meat production over time.",
            title = "Total Accumulated Production (Stacked Bars)",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.COL},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStackedBarChart() {
        var rawDf = MeatDatasets.getProductionData();
        var modernDf = rawDf.rows(row -> {
            var animal = row.get("animal") != null ? row.get("animal").toString() : "";
            var dateObj = row.get("date");
            if (dateObj instanceof LocalDate date) {
                return ("beef".equals(animal) || "pork".equals(animal) || "broilers".equals(animal))
                        && date.getYear() >= 2018 && date.getMonthValue() == 1;
            }
            return false;
        }).select();

        return ggplot(modernDf, aes().x("date").y("weight").fill("animal"))
                .geoms(
                        // Load bars in STACK mode
                        col(Position.STACK).widthFactor(0.75)
                )
                .labs(labs("8. Total Accumulated Production (Stacked Bars)", "Year", "Million Pounds"));
    }

    /** {@return a heatmap of meat consumption by animal and year} */
    @SamplePlot(description = "Heatmap of meat consumption by animal and year.",
            title = "Meat Consumption Heatmap",
            dataset = SampleDataset.MEAT,
            geoms = {SampleGeom.TILE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHeatmap() {
        var df = MeatDatasets.getHeatmapData();
        return ggplot(df, aes().x("animal").y("year").fill("consumption"))
                .geoms(tile("plasma", 0.5))
                .labs(title("Meat Consumption Heatmap"));
    }
}
