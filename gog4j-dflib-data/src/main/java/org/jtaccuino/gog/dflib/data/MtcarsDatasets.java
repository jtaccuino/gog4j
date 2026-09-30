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
package org.jtaccuino.gog.dflib.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.csv.Csv;

/**
 * Data loading for the Motor Trend car-road-tests (mtcars) dataset.
 * <p>
 * Provides a {@link #loadMtcars()} method that returns the raw DataFrame.
 */
public class MtcarsDatasets {

    private static final SoftCache<DataFrame> MTCARS = new SoftCache<>();
    private static final SoftCache<DataFrame> NUMERIC_MTCARS = new SoftCache<>();
    private static final SoftCache<DataFrame> NAMED_MTCARS = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private MtcarsDatasets() {
    }

    /**
     * Loads the mtcars CSV.
     *
     * @return a {@link DataFrame} with columns mpg, cyl, disp, hp, wt, etc.
     */
    public static DataFrame loadMtcars() {
        return MTCARS.get(MtcarsDatasets::readMtcars);
    }

    private static DataFrame readMtcars() {
        return Csv.loader()
                .doubleCol("mpg")
                .doubleCol("wt")
                .load(CsvResources.reader("/examples/mtcars/mtcars.csv"));
    }

    /**
     * Loads mtcars with its measurement columns typed as numbers, so the
     * continuous variables (mpg, disp, hp, …) get continuous scales instead of
     * being treated as categories. Leaves {@link #loadMtcars()} untouched for
     * the plots that rely on its string columns.
     *
     * @return a {@link DataFrame} with numeric mpg, cyl, disp, hp, drat, wt, qsec, vs, am, gear, carb
     */
    public static DataFrame loadNumericMtcars() {
        return NUMERIC_MTCARS.get(MtcarsDatasets::readNumericMtcars);
    }

    private static DataFrame readNumericMtcars() {
        return Csv.loader()
                .doubleCol("mpg")
                .doubleCol("cyl")
                .doubleCol("disp")
                .doubleCol("hp")
                .doubleCol("drat")
                .doubleCol("wt")
                .doubleCol("qsec")
                .doubleCol("vs")
                .doubleCol("am")
                .doubleCol("gear")
                .doubleCol("carb")
                .load(CsvResources.reader("/examples/mtcars/mtcars.csv"));
    }

    /**
     * Loads mtcars with the car names as a {@code car} column, the counterpart
     * of {@code rownames(mtcars)} in the wind-rose reference example
     * ({@code Geoms.text(angle = 90)} around the rim).
     *
     * @return a {@link DataFrame} with numeric {@code mpg} and a {@code car}
     *         column holding each car's name
     */
    public static DataFrame loadNamedMtcars() {
        return NAMED_MTCARS.get(MtcarsDatasets::readNamedMtcars);
    }

    private static DataFrame readNamedMtcars() {
        var numeric = loadNumericMtcars();
        var names = new ArrayList<String>();
        var cars = loadMtcars();
        for (var i = 0; i < cars.height(); i++) {
            names.add(cars.getColumn(0).get(i).toString());
        }
        return DataFrame.byColumn("mpg", "car").of(
                numeric.getColumn("mpg"),
                Series.of(names.toArray(String[]::new)));
    }

    /**
     * Mean weight per cylinder count, the mtcars analogue of the reference
     * example's {@code mean_wt <- data.frame(cyl = c(4, 6, 8), wt = c(2.28, 3.11, 4.00))}.
     *
     * @return a {@link DataFrame} with columns {@code cyl} and {@code wt}
     */
    public static DataFrame meanWeightByCyl() {
        return DataFrame.byColumn("cyl", "wt").of(
                Series.of("4", "6", "8"),
                Series.ofDouble(2.2857, 3.1171, 3.9992));
    }

    /**
     * Mean fuel economy per cylinder count, the vertical-rule analogue of
     * {@link #meanWeightByCyl()}.
     *
     * @return a {@link DataFrame} with columns {@code cyl} and {@code mpg}
     */
    public static DataFrame meanMpgByCyl() {
        return DataFrame.byColumn("cyl", "mpg").of(
                Series.of("4", "6", "8"),
                Series.ofDouble(26.6636, 19.7429, 15.1000));
    }

/**
     * Per-cylinder least-squares fit of {@code mpg ~ wt}, one row per cylinder
     * count. These are the per-facet analogue of the global coefficients in
     * {@code abline(intercept = 37, slope = -5)}.
     *
     * @return a {@link DataFrame} with columns {@code cyl}, {@code slope}, {@code intercept}
     */
    public static DataFrame lmFitByCyl() {
        return DataFrame.byColumn("cyl", "slope", "intercept").of(
                Series.of("4", "6", "8"),
                Series.ofDouble(-5.6470, -2.7801, -2.1924),
                Series.ofDouble(39.5712, 28.4088, 23.8680));
    }

    /**
     * Number of cars per grouping of cylinder count, number of forward gears, and
     * transmission type, derived from the loaded mtcars data.
     * <p>
     * {@code cyl} and {@code am} are kept as strings so the plot treats them as
     * categorical facet and stacked-fill groups; {@code gear} is a numeric
     * category (the bar x position) with {@code am} (0 = automatic, 1 = manual)
     * resolved to a legend label via {@code labs.map}; {@code n} is the count
     * stacked on the y axis.
     *
     * @return a {@link DataFrame} with columns {@code cyl}, {@code gear}, {@code am}, {@code n}
     */
    @SuppressWarnings("StringSplitter") // "|" split keys; Pattern.split() still trips the checker
    public static DataFrame countsByCylGearAm() {
        var df = loadMtcars();
        var keyed = new HashMap<String, Map<String, Integer>>();
        for (var j = 0; j < df.height(); j++) {
            var cylindersForRow = String.valueOf(df.getColumn("cyl").get(j));
            var gear = String.valueOf(df.getColumn("gear").get(j));
            var am = String.valueOf(df.getColumn("am").get(j));
            var key = cylindersForRow + "|" + gear;
            var byAm = keyed.computeIfAbsent(key, k -> new HashMap<>());
            byAm.merge(am, 1, Integer::sum);
        }

        var cylinders = new ArrayList<String>();
        var gears = new ArrayList<Integer>();
        var trans = new ArrayList<String>();
        var counts = new ArrayList<Integer>();
        var sortedKeys = new ArrayList<>(keyed.keySet());
        sortedKeys.sort(Comparator.comparingInt((String a) -> Integer.parseInt(a.split("\\|")[0]))
                .thenComparingInt(a -> Integer.parseInt(a.split("\\|")[1])));
        for (var key : sortedKeys) {
            var parts = key.split("\\|");
            var byAm = keyed.get(key);
            var ams = new ArrayList<>(byAm.keySet());
            ams.sort(Comparator.comparingInt(Integer::parseInt));
            for (var am : ams) {
                cylinders.add(parts[0]);
                gears.add(Integer.parseInt(parts[1]));
                trans.add(am);
                counts.add(byAm.get(am));
            }
        }

        return DataFrame.byColumn("cyl", "gear", "am", "n").of(
                Series.of(cylinders.toArray()),
                Series.ofInt(gears.stream().mapToInt(Integer::intValue).toArray()),
                Series.of(trans.toArray()),
                Series.ofInt(counts.stream().mapToInt(Integer::intValue).toArray()));
    }
}
