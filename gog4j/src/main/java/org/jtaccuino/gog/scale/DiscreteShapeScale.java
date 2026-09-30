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
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * A discrete shape scale: maps the ordered categories of a column to the
 * {@link PointShape} enum in a repeating cycle, one shape per category, and
 * provides O(1) lookup via a value-to-index map.
 *
 * <p>The assignment order matches {@link Scale#resolveGlobalShape}: categories
 * are sorted alphabetically by their {@code toString()} form, and shapes are
 * assigned by index modulo the {@link PointShape} enum's length.
 */
public final class DiscreteShapeScale {

    private final String columnName;
    private final List<Object> categories;
    private final List<PointShape> shapes;
    private final Map<Object, Integer> index;

    private DiscreteShapeScale(String columnName, List<Object> categories,
                               List<PointShape> shapes, Map<Object, Integer> index) {
        this.columnName = columnName;
        this.categories = categories;
        this.shapes = shapes;
        this.index = index;
    }

    /**
     * Builds the discrete shape scale for a column: the ordered distinct
     * categories in scale order, each assigned a {@link PointShape} in a
     * repeating cycle.
     *
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the shape aesthetic
     * @param <DF>       the DataFrame type
     * @return the discrete shape scale
     */
    public static <DF> DiscreteShapeScale forColumn(DF df, DataExtractor<DF> ext, String columnName) {
        return forColumn(df, ext, columnName, null);
    }

    /**
     * Builds the discrete shape scale for a column, honouring explicit
     * category-to-shape overrides from a {@link ScaleSpec} (from
     * {@code scaleShapeManual}) before the repeating cycle.
     *
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the shape aesthetic
     * @param spec       the plot's scale specification, or {@code null} for defaults
     * @param <DF>       the DataFrame type
     * @return the discrete shape scale
     */
    public static <DF> DiscreteShapeScale forColumn(DF df, DataExtractor<DF> ext, String columnName,
                                                    ScaleSpec spec) {
        var categories = Scale.uniqueCategories(df, ext, columnName);
        var available = PointShape.values();
        var shapes = new ArrayList<PointShape>(categories.size());
        var indexMap = new HashMap<Object, Integer>();
        for (int i = 0; i < categories.size(); i++) {
            var manual = spec == null ? null : spec.manualShapeFor(categories.get(i));
            shapes.add(manual != null ? manual : available[i % available.length]);
            indexMap.put(categories.get(i), i);
        }
        return new DiscreteShapeScale(columnName, categories, shapes, indexMap);
    }

    /** {@return the aesthetic column this scale maps} */
    public String columnName() { return columnName; }

    /** {@return the ordered category values in scale order} */
    public List<Object> categories() { return categories; }

    /**
     * Resolves the shape for a category value.
     *
     * @param category the raw category value
     * @return the category's shape, or {@link PointShape#CIRCLE} when the
     *         value is unknown or {@code null}
     */
    public PointShape shapeFor(Object category) {
        if (category == null) {
            return PointShape.CIRCLE;
        }
        var idx = index.get(category);
        return idx == null ? PointShape.CIRCLE : shapes.get(idx);
    }
}
