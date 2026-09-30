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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dflib.DataFrame;
import org.dflib.Series;

/**
 * The aggregated counts behind the polar figures, mirroring the datasets the
 * {@code Coords.coordPolar()} reference derives from {@code mtcars} with
 * {@code Geoms.bar()}: the cylinder counts become coxcomb wedges, and their
 * share of the total becomes pie slices.
 */
public final class PolarDatasets {

    private PolarDatasets() {
    }

    /**
     * Number of cars per cylinder count, one row per level — the
     * {@code x = factor(cyl)} basis of the coxcomb figures.
     *
     * @return a {@link DataFrame} with columns {@code cyl} and {@code count}
     */
    public static DataFrame loadCylCounts() {
        var df = MtcarsDatasets.loadMtcars();
        var counts = new LinkedHashMap<String, Integer>();
        for (var j = 0; j < df.height(); j++) {
            var cyl = String.valueOf(df.getColumn("cyl").get(j));
            counts.merge(cyl, 1, Integer::sum);
        }
        return buildFrame(counts.keySet(), counts);
    }

    /**
     * The cylinder counts with a constant label column — the {@code x = factor(1)}
     * of the reference pie, where every group shares one radial band so the slices
     * occupy the whole disc.
     *
     * @return a {@link DataFrame} with columns {@code label}, {@code cyl}, and {@code count}
     */
    public static DataFrame loadPieFrame() {
        var cylCounts = loadCylCounts();
        var labels = new ArrayList<String>();
        var cyls = new ArrayList<String>();
        var counts = new ArrayList<Integer>();
        for (var row : cylCounts) {
            labels.add("all");
            cyls.add(String.valueOf(row.get("cyl")));
            counts.add(((Number) row.get("count")).intValue());
        }
        return DataFrame.byColumn("label", "cyl", "count").of(
                Series.of(labels.toArray(String[]::new)),
                Series.of(cyls.toArray(String[]::new)),
                Series.ofInt(counts.stream().mapToInt(Integer::intValue).toArray()));
    }

    /**
     * Diamonds by clarity and cut, the grouping behind the stacked rose: one
     * wedge per clarity level with the cut counts stacked as annular rings.
     * Follows the default level order (I1, SI2, SI1, VS2, VS1, VVS2,
     * VVS1, IF for clarity; Fair, Good, Very Good, Premium, Ideal for cut).
     *
     * @return a {@link DataFrame} with columns {@code clarity}, {@code cut}, and {@code count}
     */
    public static DataFrame loadClarityCutCounts() {
        var df = DiamondsDatasets.loadDiamonds();
        var counts = new LinkedHashMap<String, Integer>();
        for (var j = 0; j < df.height(); j++) {
            var key = String.valueOf(df.getColumn("clarity").get(j))
                    + "|" + String.valueOf(df.getColumn("cut").get(j));
            counts.merge(key, 1, Integer::sum);
        }
        var clarityOrder = List.of("I1", "SI2", "SI1", "VS2", "VS1", "VVS2", "VVS1", "IF");
        var cutOrder = List.of("Fair", "Good", "Very Good", "Premium", "Ideal");
        var claritys = new ArrayList<String>();
        var cuts = new ArrayList<String>();
        var nums = new ArrayList<Integer>();
        for (var clarity : clarityOrder) {
            for (var cut : cutOrder) {
                var n = counts.get(clarity + "|" + cut);
                if (n == null) {
                    continue;
                }
                claritys.add(clarity);
                cuts.add(cut);
                nums.add(n);
            }
        }
        return DataFrame.byColumn("clarity", "cut", "count").of(
                Series.of(claritys.toArray(String[]::new)),
                Series.of(cuts.toArray(String[]::new)),
                Series.ofInt(nums.stream().mapToInt(Integer::intValue).toArray()));
    }

    private static DataFrame buildFrame(Collection<String> keys, Map<String, Integer> counts) {
        var sorted = new ArrayList<>(keys);
        sorted.sort(Comparator.comparingInt(Integer::parseInt));
        var cyls = new ArrayList<String>();
        var nums = new ArrayList<Integer>();
        for (var key : sorted) {
            cyls.add(key);
            nums.add(counts.get(key));
        }
        return DataFrame.byColumn("cyl", "count").of(
                Series.of(cyls.toArray(String[]::new)),
                Series.ofInt(nums.stream().mapToInt(Integer::intValue).toArray()));
    }
}
