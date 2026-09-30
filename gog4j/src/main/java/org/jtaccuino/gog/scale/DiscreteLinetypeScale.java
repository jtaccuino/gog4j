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
package org.jtaccuino.gog.scale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * A discrete line-type scale: maps the ordered categories of a column to the
 * {@code linetype} dash patterns in a repeating cycle, one pattern per
 * category, and provides O(1) lookup via a value-to-index map.
 *
 * <p>The assignment order matches {@link Scale#uniqueCategories}: categories
 * are sorted alphabetically by their {@code toString()} form, and patterns are
 * assigned by index modulo {@link LineDash#cycle(int) the line-type cycle}.
 */
public final class DiscreteLinetypeScale {

    private final String columnName;
    private final List<Object> categories;
    private final List<double[]> patterns;
    private final Map<Object, Integer> index;

    private DiscreteLinetypeScale(String columnName, List<Object> categories,
                                  List<double[]> patterns, Map<Object, Integer> index) {
        this.columnName = columnName;
        this.categories = categories;
        this.patterns = patterns;
        this.index = index;
    }

    /**
     * Builds the discrete line-type scale for a column: the ordered distinct
     * categories in scale order, each assigned a dash pattern in a repeating
     * cycle.
     *
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the line-type aesthetic
     * @param <DF>       the DataFrame type
     * @return the discrete line-type scale
     */
    public static <DF> DiscreteLinetypeScale forColumn(DF df, DataExtractor<DF> ext, String columnName) {
        return forColumn(df, ext, columnName, null);
    }

    /**
     * Builds the discrete line-type scale for a column, honouring explicit
     * category-to-dash overrides from a {@link ScaleSpec} (from
     * {@code scaleLinetypeManual}) before the repeating cycle.
     *
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the line-type aesthetic
     * @param spec       the plot's scale specification, or {@code null} for defaults
     * @param <DF>       the DataFrame type
     * @return the discrete line-type scale
     */
    public static <DF> DiscreteLinetypeScale forColumn(DF df, DataExtractor<DF> ext, String columnName,
                                                       ScaleSpec spec) {
        var categories = Scale.uniqueCategories(df, ext, columnName);
        var base = LineDash.cycle(categories.size());
        var patterns = new ArrayList<double[]>(categories.size());
        var indexMap = new HashMap<Object, Integer>();
        for (int i = 0; i < categories.size(); i++) {
            var manual = spec == null ? null : spec.manualLinetypeFor(categories.get(i));
            patterns.add(manual != null ? toArray(manual) : base.get(i));
            indexMap.put(categories.get(i), i);
        }
        return new DiscreteLinetypeScale(columnName, categories, patterns, indexMap);
    }

    private static double[] toArray(List<Double> dashes) {
        var arr = new double[dashes.size()];
        for (int i = 0; i < dashes.size(); i++) {
            arr[i] = dashes.get(i);
        }
        return arr;
    }

    /** {@return the aesthetic column this scale maps} */
    public String columnName() { return columnName; }

    /** {@return the ordered category values in scale order} */
    public List<Object> categories() { return categories; }

    /** {@return the assigned dash patterns, one per category in scale order} */
    public List<double[]> patterns() { return patterns; }

    /**
     * Resolves the dash pattern for a category value.
     *
     * @param category the raw category value
     * @return the category's dash pattern, or {@code solid} ({@code null}) when
     *         the value is unknown or {@code null}
     */
    public double[] patternFor(Object category) {
        if (category == null) {
            return null;
        }
        var idx = index.get(category);
        return idx == null ? null : patterns.get(idx);
    }
}
