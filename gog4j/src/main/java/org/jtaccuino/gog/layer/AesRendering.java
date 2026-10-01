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
package org.jtaccuino.gog.layer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.AesValueSupport;
import org.jtaccuino.gog.data.Temporals;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.stat.StatData;

/**
 * Resolution helpers shared by the stat-consuming geometries ({@code Geoms.bar},
 * {@code Geoms.histogram}, {@code Geoms.freqpoly}). They unpack {@link AesValue}s
 * into the column values, heights, and per-row colours a stat renderer needs:
 * stat references and inline expressions resolve against the {@link StatData}
 * output, while after-scale and staged references unwind through the full
 * {@link AesValue#resolve} seam.
 */
final class AesRendering {

    private AesRendering() {
    }

    /**
     * Builds a resolution context over the panel being rendered.
     *
     * @param ctx       the panel context
     * @param aes       the effective layer mapping
     * @param statData  the attached stat's output, or {@code null}
     * @param rawValues the raw model values of the aesthetic being resolved, or
     *                  {@code null} when none are available
     * @param <DF>      the DataFrame type
     * @return the resolution context
     */
    static <DF> AesValue.AesResolution<DF> resolution(PanelContext<DF> ctx, Aes aes,
                                                      StatData statData, List<?> rawValues) {
        return new AesValue.AesResolution<>(ctx, aes, statData, rawValues, new ArrayList<>());
    }

    /**
     * The tooltip label for the mapped x column: the labs-provided x-axis label
     * when set, else the generic {@code X}. Labelling the x value keeps it
     * distinct from a colour-hint line, which describes a stat output column
     * rather than the x value the geometry spans.
     *
     * @param ctx the panel context
     * @return the display label
     */
    static String xAxisName(PanelContext<?> ctx) {
        var labs = ctx.plot().labs();
        if (labs != null) {
            var label = labs.xLabel();
            if (label != null && !label.isEmpty()) {
                return label;
            }
        }
        return "X";
    }

    /**
     * The per-row height values for a stat-consuming geometry when the mapped
     * {@code y} is a computable {@link AesValue} (an {@code afterStat}
     * expression, an {@code afterScale} reference, or a staged pipeline), or
     * {@code null} when the caller should use its plain column fallback. A row
     * whose resolved value is not a number reports {@code NaN}, which callers
     * skip.
     *
     * @param ctx      the panel context
     * @param statData stat output
     * @param aes      the effective layer mapping
     * @return the computed heights, or {@code null}
     */
    static List<Double> computedHeights(PanelContext<?> ctx, StatData statData, Aes aes) {
        var yValue = aes == null ? null : aes.yValue();
        if (yValue instanceof AesValue.AfterStatExpr ex) {
            return toDoubles(ex.evaluate(statData));
        }
        if (yValue != null && !yValue.isRaw() && !(yValue instanceof AesValue.AfterStat)) {
            var res = resolution(ctx, aes, statData, null);
            int n = statData.rowCount();
            var out = new ArrayList<Double>(n);
            for (var i = 0; i < n; i++) {
                var resolved = yValue.resolve(res, i);
                out.add(resolved instanceof Number m ? m.doubleValue() : Double.NaN);
            }
            return out;
        }
        return null;
    }

    /**
     * Resolves the per-row fill colour of a stat row. {@code afterStat}
     * references and inline expressions map their value through the colour
     * scale; after-scale and staged references resolve directly to a {@link Color}.
     *
     * @param statData stat output
     * @param scales   the resolved scales
     * @param res      the resolution context
     * @param fill     the mapped fill {@link AesValue}, or {@code null}
     * @param row      the zero-based stat row
     * @return the fill colour, or {@code null} when none can be determined
     */
    static Color fillColor(StatData statData, ResolvedScales<?> scales,
                           AesValue.AesResolution<?> res, AesValue fill, int row) {
        if (fill instanceof AesValue.AfterStat || fill instanceof AesValue.AfterStatExpr) {
            var info = AesValueSupport.statColourInfo(fill, statData, row);
            if (info == null || info.rowValue() == null) {
                return null;
            }
            // The expression's own output drives the scale, so the plotted
            // values (e.g. ratios) span the ramp instead of pooling at one end
            // of the underlying count column's domain.
            return fill instanceof AesValue.AfterStatExpr
                    ? scales.resolveColorForValues(info.column(), info.values(), info.rowValue())
                    : scales.resolvedColorFor(info.column(), info.rowValue());
        }
        var resolved = fill == null ? null : fill.resolve(res, row);
        if (resolved instanceof Color color) {
            return color;
        }
        if (resolved instanceof Number number) {
            return scales.resolvedColorFor(fill.render(), number);
        }
        return null;
    }

    /**
     * Builds the colour hint line prefixed to a stat-geometry tooltip: it names
     * the stat column feeding the geometry's colour scale and the value the
     * mouse is over, so the plot's tooltip navigation resolves it to the exact
     * colour the geometry was painted with. The value mirrors {@link
     * #fillColor fillColor}: a plain {@code afterStat} reference reads its column
     * cell, an inline expression reports its <em>evaluated</em> result (not the
     * raw count), and a {@code stage()} delegates to its after-stat pin. The
     * fill aesthetic is tried first, then the color aesthetic. Returns
     * {@code null} when the geometry is not colour-mapped through a stat output
     * column.
     *
     * @param statData layer's stat output
     * @param aes      the effective aesthetic mapping
     * @param row      the zero-based stat row
     * @return the tooltip hint line, or {@code null}
     */
    static String statColourHint(StatData statData, Aes aes, int row) {
        var hint = statColourHintOf(statData, aes.fillValue(), row);
        return hint != null ? hint : statColourHintOf(statData, aes.colorValue(), row);
    }

    private static String statColourHintOf(StatData statData, AesValue value, int row) {
        if (value == null || row < 0) {
            return null;
        }
        if (value instanceof AesValue.AfterStat || value instanceof AesValue.AfterStatExpr) {
            var info = AesValueSupport.statColourInfo(value, statData, row);
            if (info == null || info.rowValue() == null) {
                return null;
            }
            return info.column() + ": " + preciseValue(info.rowValue());
        }
        if (value instanceof AesValue.Stage stage && stage.afterStat() != null) {
            return statColourHintOf(statData, stage.afterStat(), row);
        }
        return null;
    }

    /**
     * Renders a tooltip value with full precision so the colour-tooltip
     * navigation resolves the number back to the exact value the geometry was
     * painted with. Whole numbers keep an integer form; only
     * {@code afterStat} expression outputs (ratios, densities) gain the extra
     * decimals that {@link Values#label} would round away.
     */
    private static String preciseValue(Object value) {
        if (value instanceof Number n) {
            var v = n.doubleValue();
            if (v == Math.rint(v) && Math.abs(v) < 1e15) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
        return Values.label(value);
    }

    private static List<Double> toDoubles(List<?> values) {
        var out = new ArrayList<Double>(values.size());
        for (var v : values) {
            out.add(v instanceof Number m ? m.doubleValue() : Double.NaN);
        }
        return out;
    }

    /**
     * The raw grouping column name, when the fill or color aesthetic binds a
     * plain model column. Referential values ({@code afterStat}/{@code
     * afterScale}/{@code stage}) name stat output or scale-mapped variables
     * and yield {@code null} so the caller falls back to an ungrouped path.
     *
     * @param aes the layer's effective aesthetic mapping
     * @return the raw grouping column name, or {@code null}
     */
    static String rawGroupColumn(Aes aes) {
        var fill = aes.fillValue();
        if (fill != null) {
            return fill.isRaw() ? fill.render() : null;
        }
        var color = aes.colorValue();
        return color != null && color.isRaw() ? color.render() : null;
    }

    /**
     * The raw grouping column of a point-like geometry, preferring the color
     * aesthetic over the fill one, when either binds a plain model column.
     * {@link Point}'s fill is resolved from its color group where the two meet,
     * so the tooltip colour hint must name the column the geometry is actually
     * coloured by. Referential values ({@code afterStat}/{@code afterScale}/
     * {@code stage}) yield {@code null} so the caller falls back to an
     * ungrouped path.
     *
     * @param aes the layer's effective aesthetic mapping
     * @return the raw colour group column name, or {@code null}
     */
    static String colourGroupColumn(Aes aes) {
        var color = aes.colorValue();
        if (color != null) {
            return AesValue.rawColumn(color);
        }
        var fill = aes.fillValue();
        return fill == null ? null : AesValue.rawColumn(fill);
    }

    /**
     * Formats the hover value of the x position for a stat-geometry tooltip:
     * the category name for a discrete axis, a date or date-time for a temporal
     * axis, or the plain data value otherwise.
     *
     * @param xScale  the effective x scale
     * @param xIsDate whether the x column carries date values
     * @param xDouble the x value in data units
     * @return the display label
     */
    static String xValueLabel(Scale xScale, boolean xIsDate, double xDouble) {
        if (xScale.isDiscrete()) {
            var categories = xScale.categories();
            int idx = (int) Math.round(xDouble);
            return (idx >= 0 && idx < categories.size())
                    ? String.valueOf(categories.get(idx)) : String.valueOf(xDouble);
        }
        if (xIsDate) {
            return LocalDate.ofEpochDay((long) xDouble).toString();
        }
        if (xScale.isTimestampScale()) {
            return Temporals.label(Temporals.instantAt(xDouble));
        }
        return String.valueOf(xDouble);
    }

    /**
     * Assembles a stat-geometry tooltip value block: the x label and the value in
     * thousands-grouped form, prefixed by the colour hint line when the layer is
     * colour-mapped through the stat output.
     *
     * @param colourHint the colour hint line, or {@code null}
     * @param labelX     the formatted x label
     * @param value      the stat output value (height/count)
     * @return the assembled tooltip lines
     */
    static String statValueTooltip(String colourHint, String labelX, double value) {
        var valueLine = String.format("Value: %,.1f", value);
        return colourHint != null
                ? colourHint + "\n" + labelX + "\n" + valueLine
                : labelX + "\n" + valueLine;
    }

    /**
     * Assembles a grouped stat-geometry tooltip value block, prefixing the
     * group value the same way the paint path colours the bar.
     *
     * @param group   the grouping value
     * @param labelX  the formatted x label
     * @param value   the stat output value (height/count)
     * @return the assembled tooltip lines
     */
    static String groupedValueTooltip(Object group, String labelX, double value) {
        return "Group: " + group + "\n" + labelX + "\n" + String.format("Value: %,.1f", value);
    }
}
