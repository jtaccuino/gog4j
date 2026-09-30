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

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$double;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.dflib.DataFrame;
import org.dflib.csv.Csv;

/**
 * Data loading and transformation utilities for the USDA meat production dataset.
 * <p>
 * Provides long-format, consumption-per-year, and heatmap aggregations.
 */
public class MeatDatasets {

    /** The meat-production animal categories found in the source CSV. */
    public static final List<String> ANIMAL_NAMES = List.of("beef", "broilers", "lamb_and_mutton", "other_chicken", "pork", "turkey", "veal");

    private static final SoftCache<DataFrame> PRODUCTION = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private MeatDatasets() {
    }

    /**
     * Loads raw meat.csv data via a temporary file path, performs the
     * melt operation (wide-to-long) and data cleaning directly in Java,
     * returning a perfect conventional long-format DataFrame.
     *
     * @return the long-format {@link DataFrame} with date, animal, and weight columns
     */
    public static DataFrame getProductionData() {
        return PRODUCTION.get(MeatDatasets::readProductionData);
    }

    private static DataFrame readProductionData() {

        var rawMpgMeat = Csv.loader()
                .dateCol("date")
                .load(CsvResources.reader("/examples/meat/meat.csv"));
        // These meat type columns from the original meat.csv will be melted
        var meatTypes = List.of("beef", "veal", "pork", "lamb_and_mutton", "broilers", "other_chicken", "turkey");

        List<Object> unpivotedValues = new ArrayList<>();

        // Iterate DataFrame rows
        for (var row : rawMpgMeat) {
            for (String meat : meatTypes) {
                unpivotedValues.add(row.get("date"));
                unpivotedValues.add(meat);
                unpivotedValues.add(row.get(meat));
            }
        }

        // Fold unpivoted values into the long DataFrame
        return DataFrame.foldByRow("date", "animal", "weight")
                .of(unpivotedValues.toArray())
                .rows($col("weight").ne("")).select()
                .cols("weight").merge($col("weight").castAsFloat());
    }

    /**
     * Aggregates the long-format meat data into annual total consumption per animal.
     *
     * @return a {@link DataFrame} with year, animal, and total_consumption columns
     */
    public static DataFrame getConsumptionPerYear() {
        var rawMpgMeat = getProductionData();
        var aggregated = rawMpgMeat
                .cols("year").merge(
                $col("date").mapVal(d -> ((LocalDate) d).getYear()))
                .group("year", "animal")
                .agg(
                        $col("year"),
                        $col("animal"),
                        $double("weight").sum().as("total_consumption")
                );
        return aggregated;
    }

    /**
     * Returns consumption per year with animal as a string category.
     * Columns: year (int), animal (String), consumption (double).
     *
     * @return DataFrame suitable for heatmaps with discrete x-axis
     */
    public static DataFrame getHeatmapData() {
        var raw = getConsumptionPerYear();

        List<Object> vals = new ArrayList<>();
        for (var row : raw) {
            var animal = row.get("animal");
            if (animal == null) continue;
            var year = row.get("year");
            var cons = row.get("total_consumption");
            if (year != null && cons != null) {
                vals.add(year);
                vals.add(animal);
                vals.add(cons);
            }
        }
        return DataFrame.foldByRow("year", "animal", "consumption").of(vals.toArray());
    }

    /**
     * Prints a summary of the given DataFrame (dimensions, column types).
     *
     * @param df the {@link DataFrame} to inspect
     */
    public static void printInfo(DataFrame df) {
        System.out.println("DataFrame Info:");
        System.out.println("Dimensions: " + df.height() + " rows x " + df.width() + " columns");
        System.out.println("----------------------------------------");

        for (String col : df.getColumnsIndex()) {
            Class<?> type = df.getColumn(col).getInferredType();
            System.out.printf("%-20s %s%n", col, type.getSimpleName());
        }
    }
}
