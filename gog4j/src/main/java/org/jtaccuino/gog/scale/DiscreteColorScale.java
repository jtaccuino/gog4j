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
import javafx.scene.paint.Color;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * A discrete colour scale: maps the ordered categories of a column to the
 * categorical palette, one colour per category, and carries the legend's keys
 * and display labels.
 */
public final class DiscreteColorScale implements ColorScale {

    private final String columnName;
    private final List<Object> categories;
    private final List<String> labels;
    private final List<Color> colors;
    private final Color fallback;
    private final Map<Object, Integer> index;

    private DiscreteColorScale(String columnName, List<Object> categories,
                               List<String> labels, List<Color> colors, Color fallback) {
        this.columnName = columnName;
        this.categories = categories;
        this.labels = labels;
        this.colors = colors;
        this.fallback = fallback;
        this.index = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            index.put(categories.get(i), i);
        }
    }

    /**
     * Builds the discrete colour scale for a column: the ordered distinct
     * categories in scale order, each coloured by the automatic palette or an
     * explicit manual palette, and labelled through the plot's label dictionary.
     *
     * @param df         the master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the colour aesthetic
     * @param spec       the plot's scale specification, or {@code null} for defaults
     * @param labs       the plot's label dictionary, or {@code null} for raw names
     * @param palette    the theme's categorical palette, in assignment order
     * @param fallback   the theme's absolute fallback color for unmapped lookups
     * @param <DF>       the DataFrame type
     * @return the discrete colour scale
     */
    public static <DF> DiscreteColorScale forColumn(DF df, DataExtractor<DF> ext, String columnName,
                                                    ScaleSpec spec, LabsSpec labs,
                                                    List<Color> palette, Color fallback) {
        var categories = Scale.uniqueCategories(df, ext, columnName);
        var colors = new ArrayList<Color>(categories.size());
        var labels = new ArrayList<String>(categories.size());
        for (var cat : categories) {
            colors.add(Scale.resolveGlobalColor(df, ext, columnName, cat, spec, palette, fallback));
            labels.add(labs != null ? labs.map(cat.toString()) : cat.toString());
        }
        return new DiscreteColorScale(columnName, categories, labels, colors, fallback);
    }

    /**
     * Builds a discrete colour scale from an explicit list of constant group
     * labels — the keys shared by data-less layers such as
     * {@code Geoms.function()}. Each label is assigned the automatic palette
     * colour in list order (the caller keeps the list sorted to mirror the
     * categorical assignment of {@link Scale#resolveGlobalColor}).
     *
     * @param columnName the pseudo-column key for this scale (its legend name)
     * @param labels     the ordered constant labels
     * @param labs       the plot's label dictionary, or {@code null} for raw names
     * @param palette    the theme's categorical palette, in assignment order
     * @param fallback   the theme's absolute fallback color for unmapped lookups
     * @return the discrete colour scale
     */
    public static DiscreteColorScale forLabels(String columnName, List<String> labels, LabsSpec labs,
                                               List<Color> palette, Color fallback) {
        var categories = new ArrayList<Object>(labels.size());
        var colors = new ArrayList<Color>(labels.size());
        var displayLabels = new ArrayList<String>(labels.size());
        for (var raw : labels) {
            categories.add(raw);
            colors.add(Scale.resolveConstantColor(labels, raw, palette));
            displayLabels.add(labs != null ? labs.map(raw) : raw);
        }
        return new DiscreteColorScale(columnName, categories, displayLabels, colors, fallback);
    }

    @Override
    public boolean isContinuous() { return false; }

    /** {@return the aesthetic column this scale maps} */
    public String columnName() { return columnName; }

    /** {@return the ordered category values in scale order} */
    public List<Object> categories() { return categories; }

    /** {@return the display label for each category, aligned with {@link #categories()}} */
    public List<String> labels() { return labels; }

    /** {@return the colour for each category, aligned with {@link #categories()}} */
    public List<Color> colors() { return colors; }

    /**
     * Resolves the colour for a category value.
     *
     * @param category the raw category value
     * @return the category's colour, or the scale's theme-sourced fallback
     *         colour when the value is unknown or {@code null}
     */
    public Color colorFor(Object category) {
        if (category == null) {
            return fallback;
        }
        var idx = index.get(category);
        return idx == null ? fallback : colors.get(idx);
    }
}
