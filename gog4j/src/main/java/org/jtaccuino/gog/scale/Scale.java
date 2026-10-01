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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.theme.Theme;

/**
 * Represents a mathematical scale for an axis. Translates values
 * from data space to pixel coordinates on the JavaFX canvas.
 */
public class Scale {

    private final double minData;
    private final double maxData;
    private final double minPixel;
    private final double maxPixel;
    private ScaleTransform transform;
    private double minTransformed;
    private double maxTransformed;
    private boolean reversed;

    private List<String> tickLabels;
    private List<Object> categories;
    private Map<Object, Integer> categoryIndex;
    private Map<Double, String> positionLabels;
    private boolean dateScale;
    private boolean timestampScale;

    /**
     * Creates a scale mapping the given data range onto the given pixel range.
     *
     * @param minData  the minimum data value
     * @param maxData  the maximum data value
     * @param minPixel the pixel coordinate at the data minimum
     * @param maxPixel the pixel coordinate at the data maximum
     */
    public Scale(double minData, double maxData, double minPixel, double maxPixel) {
        this(minData, maxData, minPixel, maxPixel, ScaleTransform.IDENTITY);
    }

    /**
     * Creates a scale that maps values to pixels through a transformation, so
     * that a long-tailed quantity fits the panel without altering any value.
     *
     * @param minData   the minimum data value
     * @param maxData   the maximum data value
     * @param minPixel  the pixel coordinate at the data minimum
     * @param maxPixel  the pixel coordinate at the data maximum
     * @param transform the axis transformation; {@link ScaleTransform#IDENTITY} for a linear axis
     */
    public Scale(double minData, double maxData, double minPixel, double maxPixel, ScaleTransform transform) {
        this.transform = transform == null ? ScaleTransform.IDENTITY : transform;
        this.minData = this.transform.clampDomain(minData);
        this.maxData = this.transform.clampDomain(maxData);
        this.minPixel = minPixel;
        this.maxPixel = maxPixel;
        cacheTransformedBounds();
    }

    private void cacheTransformedBounds() {
        this.minTransformed = transform.forward(transform.clampDomain(minData));
        this.maxTransformed = transform.forward(transform.clampDomain(maxData));
    }

    /**
     * {@return the affine pixel mapping for this scale, or {@code null}}
     * <p>
     * An identity-transform, non-reversed scale maps a data value to a pixel
     * by pure linear interpolation, so the caller can precompute the slope and
     * intercept once and convert points without any virtual
     * {@link ScaleTransform} dispatch. Returns {@code null} when the scale is
     * transformed (logarithmic, ...), reversed or degenerate, and the caller
     * must fall back to {@link #toPixel(double)}.
     */
    public double[] linearPixelMapping() {
        if (!Objects.equals(transform, ScaleTransform.IDENTITY) || reversed) {
            return null;
        }
        if (maxTransformed == minTransformed) {
            return null;
        }
        double slope = (maxPixel - minPixel) / (maxTransformed - minTransformed);
        return new double[]{slope, minPixel - slope * minTransformed};
    }

    /** {@return the transformation this scale applies between data and pixel space} */
    public ScaleTransform transform() { return transform; }

    /**
     * Switches this scale to a base-10 logarithmic transform, the shorthand
     * behind {@code scaleYLog10()}.
     * <p>
     * Equivalent to installing {@link ScaleTransform#LOG10}; kept as a separate
     * setter because the transform is chosen after the scale is constructed.
     *
     * @param logTransform {@code true} for a log10 axis, {@code false} for linear
     */
    public void setLog(boolean logTransform) {
        this.transform = logTransform ? ScaleTransform.LOG10 : ScaleTransform.IDENTITY;
        cacheTransformedBounds();
    }

    /** {@return whether this scale applies a base-10 logarithmic transform} */
    public boolean isLog() { return Objects.equals(transform, ScaleTransform.LOG10); }

    /**
     * Inverts the data-to-pixel direction, so that the smallest value sits where
     * the largest normally would.
     *
     * @param reversed {@code true} to invert the axis direction
     */
    public void setReversed(boolean reversed) { this.reversed = reversed; }

    /** {@return whether the axis direction is inverted} */
    public boolean isReversed() { return reversed; }

    /**
     * Marks this scale as spanning a date column, whose values are stored as
     * epoch days. Axes then generate year-based breaks and format their labels
     * as years instead of printing the raw day count.
     *
     * @param dateScale {@code true} when the scale maps a date column
     */
    public void setDateScale(boolean dateScale) { this.dateScale = dateScale; }

    /** {@return whether this scale maps a date column} */
    public boolean isDateScale() { return dateScale; }

    /**
     * Marks this scale as spanning a timestamp column, whose values are stored as
     * epoch milliseconds. Axes then generate calendar-aligned breaks via
     * {@link org.jtaccuino.gog.data.Temporals} and format them as dates and
     * times instead of printing the raw millisecond count.
     * <p>
     * Kept separate from {@link #isDateScale()} because a timestamp axis needs
     * a different break ladder than a date axis: it can descend to hours,
     * minutes and seconds, and its labels carry a time of day.
     *
     * @param timestampScale {@code true} when the scale maps a timestamp column
     */
    public void setTimestampScale(boolean timestampScale) { this.timestampScale = timestampScale; }

    /** {@return whether this scale maps a timestamp column} */
    public boolean isTimestampScale() { return timestampScale; }

    /**
     * Registers explicit tick positions mapped to custom label strings.
     * <p>
     * Used by plots that place ticks at arbitrary data positions — chromosome
     * band centres, for instance — rather than at evenly spaced breaks.
     *
     * @param labels a map from tick data-position to label text
     */
    public void setPositionLabels(Map<Double, String> labels) { this.positionLabels = labels; }

    /** {@return whether labels were registered against explicit tick positions} */
    public boolean hasPositionLabels() { return positionLabels != null && !positionLabels.isEmpty(); }

    /**
     * Creates a discrete (categorical) scale.
     * Sets the data range to {@code [-0.5, N-0.5]} and builds a category→index map.
     *
     * @param categories the ordered list of category values
     * @param pixelMin   the pixel coordinate at the first category
     * @param pixelMax   the pixel coordinate at the last category
     * @return a new discrete {@link Scale}
     */
    public static Scale createDiscrete(List<Object> categories, double pixelMin, double pixelMax) {
        var s = new Scale(-0.5, categories.size() - 0.5, pixelMin, pixelMax);
        s.categories = categories;
        s.categoryIndex = new LinkedHashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            s.categoryIndex.put(categories.get(i), i);
        }
        List<String> defaultLabels = new ArrayList<>(categories.size());
        for (var cat : categories) {
            defaultLabels.add(cat == null ? "" : cat.toString());
        }
        s.tickLabels = defaultLabels;
        return s;
    }

    /** {@return the minimum data value} */
    public double minData() { return minData; }

    /** {@return the maximum data value} */
    public double maxData() { return maxData; }

    /** {@return the pixel coordinate at the data minimum} */
    public double minPixel() { return minPixel; }

    /** {@return the pixel coordinate at the data maximum} */
    public double maxPixel() { return maxPixel; }

    /** {@return whether this scale maps categories (discrete)} */
    public boolean isDiscrete() { return categories != null; }

    /** {@return the list of category values, or {@code null} for continuous scales} */
    public List<Object> categories() { return categories; }

    /** Overrides the auto-generated tick labels.
     * @param labels the tick labels to display, one per tick position */
    public void setTickLabels(List<String> labels) { this.tickLabels = labels; }

    /** {@return the current tick labels (auto-generated or custom)} */
    public List<String> getTickLabels() { return tickLabels; }

    /**
     * Converts a raw column value to a numeric data position.
     * For discrete scales, looks up the category → position mapping.
     * For continuous scales, extracts the double value.
     *
     * @param rawValue the raw column value to convert
     * @return the numeric data position
     */
    public double toData(Object rawValue) {
        if (categoryIndex != null) {
            Integer idx = categoryIndex.get(rawValue);
            return idx != null ? idx : 0;
        }
        return Values.toDouble(rawValue);
    }

    /**
     * Returns the formatted label for a tick value.
     * For discrete scales, returns the category name at position (int)tick.
     * For continuous scales, checks custom tickLabels then falls back to formatted number.
     *
     * @param tick the tick value in data units
     * @return the formatted label
     */
    public String getLabel(double tick) {
        int idx = (int) Math.round(tick);
        if (positionLabels != null) {
            var atPosition = positionLabels.get(tick);
            if (atPosition != null) {
                return atPosition;
            }
        }
        if (isLog() && tick > 0) {
            var exponent = Math.log10(tick);
            if (Math.abs(exponent - Math.round(exponent)) < 1e-9) {
                return String.format(Locale.US, "1e%.0f", (double) Math.round(exponent));
            }
            return String.format(Locale.US, "%.1e", tick);
        }
        if (tickLabels != null && idx >= 0 && idx < tickLabels.size()) {
            return tickLabels.get(idx);
        }
        return formatTick(tick);
    }

    /**
     * Formats a continuous tick value for display. Whole numbers print without a
     * decimal point, and values far from unity — which a transformed axis
     * routinely produces — keep enough precision to stay distinguishable.
     *
     * @param tick the tick value in data units
     * @return the formatted label
     */
    public static String formatTick(double tick) {
        if (tick == 0) return "0";
        var magnitude = Math.abs(tick);
        if (magnitude >= 1e6 || magnitude < 1e-4) {
            return String.format(Locale.US, "%.0e", tick);
        }
        if (tick == Math.rint(tick)) {
            return String.valueOf((long) tick);
        }
        if (magnitude < 0.01) {
            return String.format(Locale.US, "%.4f", tick);
        }
        if (magnitude < 1) {
            return String.format(Locale.US, "%.2f", tick);
        }
        return String.format(Locale.US, "%.1f", tick);
    }

    /**
     * Resolves the color for a category, honouring an explicit palette configured
     * via {@code scaleColorManual} before falling back to the automatic palette
     * derived from the active theme.
     *
     * @param <DF>       the DataFrame type
     * @param globalDf   the unpartitioned master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the color aesthetic
     * @param groupValue the category value to resolve
     * @param spec       the plot's scale specification, or {@code null} for defaults
     * @param palette    the theme's categorical palette, in assignment order
     * @param fallback   the theme's absolute fallback color for unmapped lookups
     * @return the resolved color
     */
    public static <DF> Color resolveGlobalColor(DF globalDf, DataExtractor<DF> ext,
                                                                   String columnName, Object groupValue, ScaleSpec spec,
                                                                   List<Color> palette, Color fallback) {
        if (columnName == null || groupValue == null) {
            return fallback;
        }

        if (spec != null) {
            var manual = spec.manualColorFor(groupValue);
            if (manual != null) return manual;
        }

        int idx = uniqueCategories(globalDf, ext, columnName).indexOf(groupValue);
        if (idx < 0) return fallback;
        if (palette.isEmpty()) return fallback;

        var specPalette = discretePalette(spec);
        var effective = specPalette.isEmpty() ? palette : specPalette;
        return effective.get(idx % effective.size());
    }

    /**
     * Returns the ordered list of colours used to map categories on a discrete
     * colour scale. A palette selected via the scale specification
     * ({@code scaleColorViridisD()}, {@code scaleColorBrewer()},
     * {@code scaleColorGradient(...)}) overrides the default
     * categorical palette; otherwise {@link List#of()} is returned and the
     * caller falls back to the theme's categorical palette.
     *
     * @param spec the plot's scale specification, or {@code null} for defaults
     * @return the ordered palette colours, or an empty list when the spec
     *         defines no palette override
     */
    public static List<Color> discretePalette(ScaleSpec spec) {
        if (spec != null) {
            if (spec.getContinuousGradient() != null && !spec.getContinuousGradient().isEmpty()) {
                return spec.getContinuousGradient();
            }
            var cmap = spec.getContinuousColorMap();
            if (cmap != null) {
                var cmapPalette = Palettes.of(cmap);
                if (cmapPalette != null) {
                    return cmapPalette;
                }
            }
        }
        return java.util.List.of();
    }

    /**
     * Resolves the automatic palette colour for a constant (data-less) group
     * label from the theme's categorical palette.
     *
     * @param sortedLabels all constant labels on the plot, in the order colours
     *                     should be assigned (caller keeps this sorted)
     * @param label        the label to resolve
     * @param palette      the theme's categorical palette, in assignment order
     * @return the palette colour for the label, falling back to the first
     *         palette colour when the label is unknown or the palette is empty
     */
    public static Color resolveConstantColor(List<String> sortedLabels, String label, List<Color> palette) {
        if (palette.isEmpty()) return DEFAULT_FALLBACK;
        var idx = sortedLabels.contains(label) ? sortedLabels.indexOf(label) : 0;
        return palette.get(idx % palette.size());
    }

    /**
     * Collects the distinct non-null values of a column in scale order
     * (alphabetically by string form), which is the order the categorical
     * palette, shape palette, and legend all follow.
     *
     * @param <DF>       the DataFrame type
     * @param globalDf   the unpartitioned master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column to inspect
     * @return the sorted distinct values
     */
    public static <DF> List<Object> uniqueCategories(DF globalDf, DataExtractor<DF> ext, String columnName) {
        var seen = new LinkedHashSet<Object>();
        for (var item : ext.getColumn(globalDf, columnName)) {
            if (item != null) seen.add(item);
        }
        var unique = new ArrayList<Object>(seen);
        unique.sort((a, b) -> a.toString().compareTo(b.toString()));
        return unique;
    }

    /**
     * Central, dynamic architecture anchor for point symbols: resolves shape
     * for categorical values automatically from the {@link PointShape} enum.
     *
     * @param <DF>       the DataFrame type
     * @param globalDf   the unpartitioned master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the shape aesthetic
     * @param groupValue the category value to resolve
     * @return the resolved shape
     */
    public static <DF> PointShape resolveGlobalShape(DF globalDf, DataExtractor<DF> ext, String columnName, Object groupValue) {
        if (columnName == null || groupValue == null) {
            return PointShape.CIRCLE;
        }

        int idx = uniqueCategories(globalDf, ext, columnName).indexOf(groupValue);
        if (idx < 0) return PointShape.CIRCLE;

        var availableShapes = PointShape.values();
        return availableShapes[idx % availableShapes.length];
    }

    /** The canonical absolute fallback colour, sourced from the default theme. */
    static final Color DEFAULT_FALLBACK = Theme.theme_gray().fallbackColor();

    /**
     * The representative size value for a legend key: the mean of the column's
     * numeric values across the rows matching the given category. Used when a
     * size mapping merges into another aesthetic's legend.
     *
     * @param <DF>       the DataFrame type
     * @param globalDf   the unpartitioned master DataFrame
     * @param ext        the data extraction strategy
     * @param columnName the column mapped to the size aesthetic
     * @param groupValue the category value to resolve
     * @return the mean value, or {@code null} when no numeric row matches
     */
    public static <DF> Double resolveGlobalSizeValue(DF globalDf, DataExtractor<DF> ext,
            String columnName, Object groupValue) {
        if (columnName == null || groupValue == null) {
            return null;
        }
        double sum = 0.0;
        int count = 0;
        for (var v : ext.getColumn(globalDf, columnName)) {
            if (groupValue.equals(v) && v instanceof Number n) {
                sum += n.doubleValue();
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

    /**
     * Maps a data value to its pixel coordinate using linear interpolation.
     * @param value the data value to convert
     * @return the pixel coordinate on the canvas
     */
    public double toPixel(double value) {
        if (maxTransformed == minTransformed) {
            return minPixel;
        }
        var t = (transform.forward(transform.clampDomain(value)) - minTransformed)
                / (maxTransformed - minTransformed);
        if (reversed) {
            t = 1.0 - t;
        }
        return minPixel + t * (maxPixel - minPixel);
    }

    /**
     * Maps a pixel coordinate back to a data value — the inverse of
     * {@link #toPixel(double)}. Used by hover hit-testing to convert a mouse
     * position into data space.
     *
     * @param pixel the pixel coordinate on the canvas
     * @return the value in data units
     */
    public double toData(double pixel) {
        if (maxPixel == minPixel) {
            return minData;
        }
        var t = (pixel - minPixel) / (maxPixel - minPixel);
        if (reversed) {
            t = 1.0 - t;
        }
        return transform.inverse(minTransformed + t * (maxTransformed - minTransformed));
    }

    /**
     * Computes a list of nice tick positions.
     * For discrete scales returns the category indices.
     * For continuous scales, uses {@code userBreaks} if provided, otherwise
     * generates "pretty" breaks at powers of 10.
     *
     * @param target    the desired approximate number of ticks
     * @param userBreaks explicit tick positions (may be {@code null})
     * @return the list of tick data-values
     */
    public List<Double> calculateTicks(int target, List<Double> userBreaks) {
        if (categories != null) {
            List<Double> ticks = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                ticks.add((double) i);
            }
            return ticks;
        }

        if (userBreaks != null && !userBreaks.isEmpty()) {
            return userBreaks;
        }

        // A transformed axis needs ticks spaced in transformed space, otherwise
        // they bunch up at one end of the panel.
        var transformed = transform.breaks(minData, maxData, target);
        if (!transformed.isEmpty()) {
            return transformed;
        }

        return niceBreaks(minData, maxData, target);
    }

    /**
     * Computes "pretty" break positions for a continuous range: multiples of
     * {@code 1}, {@code 2}, or {@code 5} times a power of ten that give close
     * to {@code target} evenly spaced values. The default tick generator of the
     * grammar, also used by the continuous colourbar guide.
     *
     * @param min    the lower bound of the range
     * @param max    the upper bound of the range
     * @param target the desired approximate number of breaks
     * @return the list of break positions
     */
    public static List<Double> niceBreaks(double min, double max, int target) {
        double range = max - min;
        if (range <= 0 || target <= 0) {
            return List.of(min);
        }

        double step = Math.pow(10, Math.floor(Math.log10(range / target)));
        var estimatedTicks = range / step;
        if (estimatedTicks > target * 1.5) {
            if (estimatedTicks <= target * 2.5) {
                step *= 2.0;
            } else if (estimatedTicks <= target * 5.0) {
                step *= 5.0;
            } else {
                step *= 10.0;
            }
        }

        double start = Math.ceil(min / step) * step;

        List<Double> ticks = new ArrayList<>();
        for (double v = start; v <= max; v += step) {
            ticks.add(Math.round(v * 1e10) / 1e10);
        }

        if (ticks.isEmpty()) {
            ticks.add(min);
            ticks.add(max);
        }

        return ticks;
    }
}
