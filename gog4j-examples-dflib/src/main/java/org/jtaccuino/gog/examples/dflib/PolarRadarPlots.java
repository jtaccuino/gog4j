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
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.polygon;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MtcarsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Radar (spider / web) charts on a polar disc: a categorical metric column
 * maps to the angular spokes and a value to the radius, so each car or group
 * becomes one closed polygon around the web. All metrics are min-max
 * normalised to a shared [0, 1] value range, so every axis reads the same
 * scale and the disc centre is the 0 point.
 */
public class PolarRadarPlots {

    /** The mtcars measurements used as radar axes, in clockwise order. */
    private static final String[] METRICS = {"mpg", "hp", "disp", "wt", "drat", "qsec"};

    /** Utility class; not meant to be instantiated. */
    private PolarRadarPlots() {
    }

    /**
     * 26. Radar profile — one car's normalised measurements drawn as a filled
     * web: the classic single-entity spider chart.
     *
     * @return the radar plot
     */
    @SamplePlot(description = "Single-car radar profile as a filled polygon on a polar web.",
            title = "Radar Profile (Geoms.polygon + coordPolar)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POLYGON, SampleGeom.POINT},
            coords = {SampleCoord.POLAR},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadarProfile() {
        var df = normalizedProfile(List.of("Mazda RX4"));
        return ggplot(df, aes().x("metric").y("value"))
                .geoms(polygon().alpha(0.5), point())
                .coord(coordPolar())
                .labs(labs("26. Radar Profile (Geoms.polygon + coordPolar)", "Metric", "Normalised value"));
    }

    /**
     * 27. Radar comparison — two cars overlaid as colour-coded polygons, so the
     * trade-offs between a compact car and a gas-guzzler show at a glance.
     *
     * @return the radar plot
     */
    @SamplePlot(description = "Two cars overlaid as colour-coded radar polygons.",
            title = "Radar Comparison (two cars)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POLYGON, SampleGeom.POINT},
            coords = {SampleCoord.POLAR},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadarComparison() {
        var df = normalizedProfile(List.of("Mazda RX4", "Duster 360"));
        return ggplot(df, aes().x("metric").y("value").color("car"))
                .geoms(polygon().alpha(0.35), point())
                .coord(coordPolar())
                .labs(labs("27. Radar Comparison (two cars)", "Metric", "Normalised value").legendTitle("Car"));
    }

    /**
     * 28. Radar by cylinder group — the average normalised profile of the 4, 6,
     * and 8-cylinder cars, three polygons on one web.
     *
     * @return the radar plot
     */
    @SamplePlot(description = "Average normalised radar profile per cylinder group.",
            title = "Radar by Cylinder Group",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POLYGON, SampleGeom.POINT},
            coords = {SampleCoord.POLAR},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadarByCylinder() {
        var df = normalizedByCylinder();
        return ggplot(df, aes().x("metric").y("value").color("cyl"))
                .geoms(polygon().alpha(0.3), point())
                .coord(coordPolar())
                .labs(labs("28. Radar by Cylinder Group", "Metric", "Normalised value")
                        .legendTitle("Cylinders")
                        .map("4", "4 cyl").map("6", "6 cyl").map("8", "8 cyl"));
    }

    /**
     * 29. Radar outline — the same web drawn as an unfilled closed outline, the
     * bare skeleton of the spider chart.
     *
     * @return the radar plot
     */
    @SamplePlot(description = "Radar web drawn as an unfilled closed outline.",
            title = "Radar Outline (unfilled polygon)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POLYGON, SampleGeom.POINT},
            coords = {SampleCoord.POLAR},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createRadarOutline() {
        var df = normalizedProfile(List.of("Mazda RX4"));
        return ggplot(df, aes().x("metric").y("value"))
                .geoms(polygon().filled(false).lineWidth(2.0), point())
                .coord(coordPolar())
                .labs(labs("29. Radar Outline (unfilled polygon)", "Metric", "Normalised value"));
    }

    private static Plot<DataFrame> ggplot(DataFrame df, Aes aes) {
        return Ggplot.ggplot(df, aes);
    }

    /**
     * Builds the long-format frame for a set of cars: one row per metric, with
     * each metric min-max normalised to [0, 1] across the whole mtcars sample,
     * so the axes agree no matter which cars are selected.
     */
    private static DataFrame normalizedProfile(List<String> carNames) {
        var numeric = MtcarsDatasets.loadNumericMtcars();
        var models = MtcarsDatasets.loadMtcars().getColumn("model");

        var metrics = new ArrayList<String>();
        var values = new ArrayList<Double>();
        var cars = new ArrayList<String>();

        for (var metric : METRICS) {
            var series = numeric.getColumn(metric);
            var min = Double.MAX_VALUE;
            var max = -Double.MAX_VALUE;
            for (var i = 0; i < series.size(); i++) {
                var v = ((Number) series.get(i)).doubleValue();
                if (v < min) min = v;
                if (v > max) max = v;
            }
            for (var i = 0; i < numeric.height(); i++) {
                var model = models.get(i).toString();
                if (!carNames.contains(model)) {
                    continue;
                }
                metrics.add(metric);
                values.add((((Number) series.get(i)).doubleValue() - min) / (max - min));
                cars.add(model);
            }
        }

        return DataFrame.byColumn("metric", "value", "car").of(
                Series.of(metrics.toArray(String[]::new)),
                Series.ofDouble(values.stream().mapToDouble(Double::doubleValue).toArray()),
                Series.of(cars.toArray(String[]::new)));
    }

    /**
     * Averages the per-car normalised profiles by cylinder count, so each of the
     * three groups is one polygon.
     */
    private static DataFrame normalizedByCylinder() {
        var numeric = MtcarsDatasets.loadNumericMtcars();
        var models = MtcarsDatasets.loadMtcars().getColumn("model");

        var cylByModel = new HashMap<String, String>();
        for (var i = 0; i < numeric.height(); i++) {
            var cyl = ((Number) numeric.getColumn("cyl").get(i)).intValue();
            cylByModel.put(models.get(i).toString(), String.valueOf(cyl));
        }

        var perCar = normalizedProfile(cylByModel.keySet().stream().toList());
        var sums = new HashMap<String, Double>();
        var counts = new HashMap<String, Integer>();
        for (var i = 0; i < perCar.height(); i++) {
            var metric = String.valueOf(perCar.getColumn("metric").get(i));
            var car = String.valueOf(perCar.getColumn("car").get(i));
            var value = ((Number) perCar.getColumn("value").get(i)).doubleValue();
            var key = metric + "|" + cylByModel.get(car);
            sums.merge(key, value, Double::sum);
            counts.merge(key, 1, Integer::sum);
        }

        var metrics = new ArrayList<String>();
        var values = new ArrayList<Double>();
        var cyls = new ArrayList<String>();
        for (var metric : METRICS) {
            for (var cyl : List.of("4", "6", "8")) {
                var key = metric + "|" + cyl;
                if (counts.get(key) == null) {
                    continue;
                }
                metrics.add(metric);
                values.add(sums.get(key) / counts.get(key));
                cyls.add(cyl);
            }
        }

        return DataFrame.byColumn("metric", "value", "cyl").of(
                Series.of(metrics.toArray(String[]::new)),
                Series.ofDouble(values.stream().mapToDouble(Double::doubleValue).toArray()),
                Series.of(cyls.toArray(String[]::new)));
    }
}
