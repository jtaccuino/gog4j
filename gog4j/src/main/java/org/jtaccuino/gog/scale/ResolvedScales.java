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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.theme.Theme;

/**
 * Eagerly resolved aesthetic scales built once per render pass over the global
 * DataFrame. Layers, the legend, and the tooltip all share the same instance
 * per column, so each column's palette is computed exactly once.
 *
 * <p>Discrete colour scales are built <em>with</em> the plot's
 * {@link ScaleSpec}, so {@code scaleColorManual} is honoured uniformly
 * across all layers.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public final class ResolvedScales<DF> {

    private final DF globalDf;
    private final DataExtractor<DF> ext;
    private final ScaleSpec spec;
    private final LabsSpec labs;
    private final List<Color> palette;
    private final Color fallback;
    private final Map<String, ColorResolver> colorResolvers = new HashMap<>();
    private final Map<String, List<?>> extraColumns;
    private final Map<String, DiscreteColorScale> colorScales = new HashMap<>();
    private final Map<String, ContinuousColorScale> continuousColorScales = new HashMap<>();
    private final Map<String, DiscreteShapeScale> shapeScales = new HashMap<>();
    private final Map<String, DiscreteLinetypeScale> linetypeScales = new HashMap<>();
    private final Map<String, MinMax> numericRanges = new HashMap<>();

    private ResolvedScales(DF globalDf, DataExtractor<DF> ext, ScaleSpec spec, LabsSpec labs,
                           List<Color> palette, Color fallback, Map<String, List<?>> extraColumns) {
        this.globalDf = globalDf;
        this.ext = ext;
        this.spec = spec;
        this.labs = labs;
        this.palette = palette;
        this.fallback = fallback;
        this.extraColumns = extraColumns;
    }

    /**
     * Builds the resolved scales for the global DataFrame.
     *
     * @param df    the unpartitioned master DataFrame
     * @param ext   the data extraction strategy
     * @param spec  the plot's scale specification
     * @param labs  the plot's label dictionary
     * @param theme the active plot theme (its categorical palette and fallback
     *              colour drive automatic colour assignment)
     * @param <DF>  the DataFrame type
     * @return the resolved scales
     */
    public static <DF> ResolvedScales<DF> forData(DF df, DataExtractor<DF> ext, ScaleSpec spec, LabsSpec labs,
                                                  Theme theme) {
        return new ResolvedScales<>(df, ext, spec, labs, theme.categoricalPalette(), theme.fallbackColor(), Map.of());
    }

    /**
     * Builds the resolved scales for the global DataFrame, also making
     * stat-computed columns available for aesthetic resolution. The
     * {@code extraColumns} map holds per-name columns produced by a layer's
     * {@code stat} (e.g. {@code density}) that do not exist in the raw frame;
     * they are consulted as a fallback whenever an aesthetic refers to a
     * computed variable via {@link AesValue#afterStat(String)}.
     *
     * @param df            the unpartitioned master DataFrame
     * @param ext           the data extraction strategy
     * @param spec          the plot's scale specification
     * @param labs          the plot's label dictionary
     * @param extraColumns  stat-computed columns keyed by bare column name
     * @param theme         the active plot theme (its categorical palette and
     *                      fallback colour drive automatic colour assignment)
     * @param <DF>          the DataFrame type
     * @return the resolved scales
     */
    public static <DF> ResolvedScales<DF> forData(DF df, DataExtractor<DF> ext, ScaleSpec spec, LabsSpec labs,
                                                  Map<String, List<?>> extraColumns, Theme theme) {
        return new ResolvedScales<>(df, ext, spec, labs, theme.categoricalPalette(), theme.fallbackColor(), extraColumns);
    }

    /**
     * Whether the given column is a stat-computed column carried only in the
     * stat output (e.g. {@code count}, {@code density}) rather than the raw
     * frame. Such columns resolve their scales from {@code StatData} instead
     * of the model DataFrame.
     *
     * @param columnName the bare column name
     * @return {@code true} when the column lives in the stat output
     */
    public boolean isStatColumn(String columnName) {
        return columnName != null && extraColumns.containsKey(columnName);
    }

    /**
     * Unpacks an aesthetic value to the bare column name it refers to,
     * stripping any {@code afterStat(...)} prefix so a computed aesthetic
     * like {@code "::density"} resolves to {@code "density"}.
     *
     * @param columnName the aesthetic value
     * @return the bare (possibly stat-computed) column name
     */
    private String resolveName(String columnName) {
        return Aes.statColumn(columnName);
    }

    /**
     * Reads a column's values, consulting the stat-computed column map before
     * the raw frame. Used so {@code afterStat} aesthetics (which name a
     * variable that exists only in a layer's {@code StatData}, not in the raw
     * DataFrame) resolve to a value list the scale can build upon.
     *
     * @param columnName the aesthetic value
     * @return the column values, or {@code null} when not found anywhere
     */
    private List<?> readColumn(String columnName) {
        var name = resolveName(columnName);
        if (extraColumns.containsKey(name)) {
            return extraColumns.get(name);
        }
        return ext.getColumn(globalDf, name);
    }

    /**
     * Determines the column type, honouring stat-computed columns: a computed
     * column is classified by {@link DataExtractor.ColumnType#ofValue(Object)},
     * so a stat that carries dates or date-times keeps its temporal type instead
     * of degrading to text. Otherwise the raw extractor's classification is used.
     *
     * @param columnName the aesthetic value
     * @return the resolved {@link DataExtractor.ColumnType}
     */
    private DataExtractor.ColumnType columnType(String columnName) {
        var name = resolveName(columnName);
        if (extraColumns.containsKey(name)) {
            return classify(extraColumns.get(name));
        }
        try {
            return ext.columnType(globalDf, name);
        } catch (IllegalArgumentException missing) {
            // The column exists neither in the stat output nor the raw frame:
            // report it as text so color resolution degrades, not crashes.
            return DataExtractor.ColumnType.TEXT;
        }
    }

    /**
     * Classifies a stat-computed column by its first non-null value, or
     * {@link DataExtractor.ColumnType#TEXT} when the column holds no values.
     *
     * @param values the computed column values
     * @return the resolved column type
     */
    private static DataExtractor.ColumnType classify(List<?> values) {
        for (var v : values) {
            if (v != null) {
                return DataExtractor.ColumnType.ofValue(v);
            }
        }
        return DataExtractor.ColumnType.TEXT;
    }

    /**
     * Returns the discrete colour scale for a column, building it lazily on
     * first access. The palette includes the effect of any manual colour
     * mapping configured via plot's {@link ScaleSpec}.
     *
     * @param columnName the column mapped to the colour or fill aesthetic
     * @return the colour scale, or {@code null} when {@code columnName} is null
     */
    public DiscreteColorScale colorScale(String columnName) {
        if (columnName == null) {
            return null;
        }
        return colorScales.computeIfAbsent(columnName,
                col -> DiscreteColorScale.forColumn(globalDf, ext, col, spec, labs, palette, fallback));
    }

    /**
     * Returns the discrete shape scale for a column, building it lazily on
     * first access.
     *
     * @param columnName the column mapped to the shape aesthetic
     * @return the shape scale, or {@code null} when {@code columnName} is null
     */
    public DiscreteShapeScale shapeScale(String columnName) {
        if (columnName == null) {
            return null;
        }
        return shapeScales.computeIfAbsent(columnName,
                col -> DiscreteShapeScale.forColumn(globalDf, ext, col, spec));
    }

    /**
     * Returns the discrete line-type scale for a column, building it lazily on
     * first access.
     *
     * @param columnName the column mapped to the line-type aesthetic
     * @return the line-type scale, or {@code null} when {@code columnName} is null
     */
    public DiscreteLinetypeScale linetypeScale(String columnName) {
        if (columnName == null) {
            return null;
        }
        return linetypeScales.computeIfAbsent(columnName,
                col -> DiscreteLinetypeScale.forColumn(globalDf, ext, col, spec));
    }

    /**
     * Returns the global {@code [min, max]} range for a continuous column
     * (size or alpha), building it lazily on first access.
     *
     * @param columnName the column mapped to the size or alpha aesthetic
     * @return the numeric range, or {@code null} when {@code columnName}
     *         is null or the column is empty / non-numeric
     */
    public MinMax numericRange(String columnName) {
        if (columnName == null) {
            return null;
        }
        return numericRanges.computeIfAbsent(columnName, col -> {
            var values = readColumn(col);
            if (values == null) {
                return null;
            }
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (var v : values) {
                if (v == null) {
                    continue;
                }
                double d = Values.toDouble(v, Double.NaN);
                if (Double.isNaN(d)) {
                    continue;
                }
                min = Math.min(min, d);
                max = Math.max(max, d);
            }
            return min <= max ? new MinMax(min, max) : null;
        });
    }

    /**
     * Returns whether the given column should be coloured through the continuous
     * colour scale (value-interpolated) rather than the discrete palette.
     * Numeric columns are continuous unless a manual palette has been set, which
     * is always categorical, one key per colour.
     *
     * @param columnName the column mapped to a colour or fill aesthetic
     * @return {@code true} when the column colours continuously
     */
    public boolean isContinuousColor(String columnName) {
        if (columnName == null
                || spec.getManualColors() != null
                || spec.isDiscreteColorRamp()) {
            return false;
        }
        return columnType(columnName) == DataExtractor.ColumnType.NUMBER;
    }

    /**
     * Returns the column names of all registered colour scales (both discrete
     * and continuous). Used by the tooltip glyph resolution to try every known
     * colour column when the plot-global aes does not carry one.
     *
     * @return an unmodifiable view of the registered colour-column names
     */
    public Set<String> colorScaleColumns() {
        var cols = new LinkedHashSet<String>(colorScales.keySet());
        cols.addAll(continuousColorScales.keySet());
        return cols;
    }

    /**
     * Returns the continuous colour scale for a numeric column, honouring a
     * plot-level gradient ({@code scaleColorGradient}) or named colour ramp
     * ({@code scaleColorViridisC}, {@code scaleColorBrewer}) before falling
     * back to "viridis". Built lazily and cached per column. Works for
     * {@code afterStat} columns resolved from the stat output as well as raw
     * frame columns.
     *
     * @param columnName the numeric column mapped to a colour or fill aesthetic
     * @return the continuous colour scale, or {@code null} when the column
     *         carries no numeric range
     */
    public ContinuousColorScale continuousColorScale(String columnName) {
        return continuousColorScales.computeIfAbsent(columnName, col -> {
            var values = readColumn(col);
            return values == null ? null : buildContinuousScale(col, values);
        });
    }

    /**
     * Returns the fully resolved colour mapping for a column, building it lazily
     * on first access. The resolver carries the continuous-vs-discrete decision
     * and any binned step colours, so an element loop can colour each row with
     * a single {@link ColorResolver#forValue} call.
     *
     * @param columnName the column mapped to a colour or fill aesthetic
     * @return the resolved colour mapping, or {@code null} when the column is null
     */
    public ColorResolver colorResolverFor(String columnName) {
        if (columnName == null) {
            return null;
        }
        return colorResolvers.computeIfAbsent(columnName, this::resolveColorResolver);
    }

    private ColorResolver resolveColorResolver(String columnName) {
        if (isContinuousColor(columnName)) {
            var scale = continuousColorScale(columnName);
            if (scale == null) {
                return ColorResolver.empty();
            }
            if (spec.isBinned()) {
                var steps = Math.max(1, spec.getColorSteps());
                return ColorResolver.binned(scale.sampleSteps(steps), scale.domain(), steps);
            }
            return ColorResolver.continuous(scale);
        }
        var discrete = colorScale(columnName);
        return discrete != null ? ColorResolver.discrete(discrete) : ColorResolver.empty();
    }

    /**
     * Resolves the colour for a value of a computed column whose own output
     * (rather than any raw/stat column) drives the colour scale, as with an
     * {@code afterStat} expression: the scale is built over the <em>given</em>
     * value list — the expression's computed output — so the plotted values
     * span the whole ramp instead of pooling at one end of a foreign column's
     * domain. The scale is cached under the column key, mirroring {@link
     * #continuousColorScale(String)}.
     *
     * @param columnName the column key the expression is mapped to
     * @param values     the expression's computed output values
     * @param rawValue   the per-row value to colour
     * @return the resolved colour, or {@code null} when it cannot be determined
     */
    public Color resolveColorForValues(String columnName, List<?> values, Object rawValue) {
        if (columnName == null || values == null || rawValue == null) {
            return null;
        }
        registerContinuousValues(columnName, values);
        return resolvedColorFor(columnName, rawValue);
    }

    /**
     * Registers a continuous colour scale for a computed column over the
     * <em>given</em> value list — the expression's computed output — without
     * resolving a colour, so the scale is ready (e.g. for a colourbar guide)
     * before any row is drawn.
     *
     * @param columnName the column key the expression is mapped to
     * @param values     the expression's computed output values
     */
    public void registerContinuousValues(String columnName, List<?> values) {
        if (columnName == null || values == null) {
            return;
        }
        continuousColorScales.put(columnName, buildContinuousScale(columnName, values));
    }

    private ContinuousColorScale buildContinuousScale(String columnName, List<?> values) {
        var gradient = spec.getContinuousGradient();
        if (gradient != null && !gradient.isEmpty()) {
            return ContinuousColorScale.fromColors(resolveName(columnName), values, gradient);
        }
        var cmap = spec.getContinuousColorMap();
        return ContinuousColorScale.forRange(resolveName(columnName), values,
                cmap != null ? cmap : "viridis");
    }

    /**
     * Resolves the colour for a single raw value of a colour/fill column,
     * routing continuous numeric columns through the value-interpolating
     * {@link ContinuousColorScale} (mirroring the colourbar) and everything
     * else through the discrete palette. Binned scales map the value to the
     * colour of the bin it falls in.
     *
     * @param columnName the column mapped to a colour or fill aesthetic
     * @param rawValue   the per-row or per-group value to colour
     * @return the resolved colour, or {@code null} when it cannot be determined
     */
    public Color resolvedColorFor(String columnName, Object rawValue) {
        var resolver = colorResolverFor(columnName);
        return resolver == null ? null : resolver.forValue(rawValue);
    }

    /**
     * Resolves the colour of the hover-tooltip dot for a colour/fill column,
     * mirroring exactly what the geometry drew: continuous columns interpolate
     * the row's numeric value against the colourbar (gradient, named ramp, or
     * binned steps), discrete columns match the tooltip's first-line value
     * against the category palette. Returns {@code null} when no colour can be
     * determined.
     *
     * @param columnName the column mapped to the colour or fill aesthetic
     * @param tooltipText the hovered geometry's tooltip text; its first line
     *                    carries the mapped value, either as {@code "Label:
     *                    value"} or as a bare value
     * @return the tooltip dot colour, or {@code null}
     */
    public Color tooltipColor(String columnName, String tooltipText) {
        if (columnName == null || tooltipText == null) {
            return null;
        }
        if (isContinuousColor(columnName)) {
            var numeric = firstLineValue(tooltipText);
            if (numeric == null) {
                return null;
            }
            try {
                return resolvedColorFor(columnName, Double.parseDouble(numeric));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        var scale = colorScale(columnName);
        if (scale == null) {
            return null;
        }
        var category = matchDiscrete(scale.categories(), tooltipText);
        return category != null ? scale.colorFor(category) : null;
    }

    /**
     * Resolves the point-shape of the hover-tooltip indicator for a shape
     * column, mirroring exactly what the geometry drew: the tooltip's per-line
     * values are matched against the shape scale's categories (honouring
     * {@code Scales.scaleShapeManual} overrides). Returns {@code null} when no shape
     * can be determined.
     *
     * @param columnName  the column mapped to the shape aesthetic
     * @param tooltipText the hovered geometry's tooltip text; one of its lines
     *                    carries the mapped shape value as {@code "Key: value"}
     * @return the tooltip point shape, or {@code null}
     */
    public PointShape tooltipShape(String columnName, String tooltipText) {
        if (columnName == null || tooltipText == null) {
            return null;
        }
        var scale = shapeScale(columnName);
        if (scale == null) {
            return null;
        }
        var category = matchDiscrete(scale.categories(), tooltipText);
        return category != null ? scale.shapeFor(category) : null;
    }

    /**
     * Finds which category of a discrete scale a tooltip names: each
     * {@code "Key: value"} line (or the whole first line, for bare group
     * values) is compared against the category's display label. Numbers are
     * compared through {@link Values#label}, so an integral {@code Double}
     * category {@code 4.0} matches a {@code "4"} tooltip value exactly, and a
     * {@code "2.35"} axis value never matches the category {@code "5"} by
     * substring. Returns {@code null} when no category agrees with any line.
     *
     * @param categories  the scale's ordered category values
     * @param tooltipText the tooltip text to search
     * @return the matched category, or {@code null}
     */
    private static Object matchDiscrete(List<?> categories, String tooltipText) {
        var start = 0;
        while (start <= tooltipText.length()) {
            var nl = tooltipText.indexOf('\n', start);
            var line = nl >= 0 ? tooltipText.substring(start, nl) : tooltipText.substring(start);
            var colon = line.indexOf(':');
            var value = (colon >= 0 ? line.substring(colon + 1) : line).trim();
            for (var cat : categories) {
                if (cat != null && Values.label(cat).trim().equalsIgnoreCase(value)) {
                    return cat;
                }
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return null;
    }

    private static String firstLineValue(String tooltipText) {
        int nl = tooltipText.indexOf('\n');
        var first = nl >= 0 ? tooltipText.substring(0, nl) : tooltipText;
        int colon = first.indexOf(':');
        return (colon >= 0 ? first.substring(colon + 1) : first).trim();
    }
}
