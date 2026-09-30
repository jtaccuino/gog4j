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
package org.jtaccuino.gog;

import java.util.EnumMap;

import java.util.Map;

/**
 * Aesthetic mapping specification for the <b>gog4j</b> Grammar of Graphics framework.
 * <p>
 * {@code Aes} binds dataset column names to visual properties of plot geometries,
 * such as x-axis position ({@link #x(String)}), y-axis position ({@link #y(String)}),
 * z-axis position ({@link #z(String)}), color ({@link #color(String)}), fill ({@link #fill(String)}),
 * shape ({@link #shape(String)}), size ({@link #size(String)}), alpha ({@link #alpha(String)}),
 * linetype ({@link #linetype(String)}),
 * grouping ({@link #group(String)}), and the endpoint positions of range or
 * directed geometries ({@link #xmin(String)}, {@link #xmax(String)},
 * {@link #ymin(String)}, {@link #ymax(String)}, {@link #xend(String)}, {@link #yend(String)}).
 * <p>
 * Each aesthetic is stored as a typed {@link AesValue}: the string setters wrap
 * plain column names as {@link AesValue.Raw} values (parsing {@code ::}- and
 * {@code @}-prefixed wire references) and the {@code *Value} setters accept any
 * {@link AesValue}, so stat references ({@link AesValue#afterStat}), after-scale
 * references ({@link AesValue#afterScale}) and staged values
 * ({@link AesValue#stage}) compose directly.
 * <p>
 * Example usage:
 * <pre>{@code
 * Aes mapping = aes().x("displ").y("hwy").color("drv");
 * Aes staged  = aes().fill(AesValue.afterStat(AesValue.ComputedVariable.COUNT));
 * }</pre>
 */
public class Aes {

    /**
     * Returns whether an aesthetic wire value refers to a stat-computed column
     * (rendered by {@link AesValue#afterStat} as {@code ::name}).
     *
     * @param value an aesthetic wire value
     * @return {@code true} when {@code value} is a computed-aesthetic reference
     */
    public static boolean isComputed(String value) {
        return value != null && value.startsWith(AesValue.STAT_PREFIX);
    }

    /**
     * Strips the computed-aesthetic prefix from a wire value, yielding the raw
     * stat output column name. For non-computed values the input is returned as-is.
     *
     * @param value an aesthetic wire value
     * @return the stat column name, or {@code value} if not computed
     */
    public static String statColumn(String value) {
        return isComputed(value) ? value.substring(AesValue.STAT_PREFIX.length()) : value;
    }

    /**
     * Builds an {@code Aes} mapping from a {@code Map} of aesthetic name to
     * column (or computed-aesthetic wire) value — the programmatic counterpart
     * to the fluent builder, useful for tooling and metaprogramming. Recognised
     * keys are {@code x, y, z, xend, yend, xmin, xmax, ymin, ymax, color, fill,
     * shape, linetype, size, alpha, group, label}. Wire values ({@code ::name},
     * {@code @name}) are parsed into their {@link AesValue} references.
     *
     * @param map aesthetic key/value pairs
     * @return a new {@code Aes} populated from {@code map}
     * @throws IllegalArgumentException for an unrecognised aesthetic key
     */
    public static Aes aesOf(Map<String, String> map) {
        Aes a = aes();
        for (var e : map.entrySet()) {
            var value = e.getValue();
            a.set(Aesthetic.of(e.getKey()), value == null ? null : AesValue.raw(value));
        }
        return a;
    }

    private final Map<Aesthetic, AesValue> values;

    /** Constructs an empty {@code Aes}; create instances via {@link #aes()}. */
    private Aes() {
        this(new EnumMap<>(Aesthetic.class));
    }

    private Aes(EnumMap<Aesthetic, AesValue> values) {
        this.values = values;
    }

    /**
     * Factory method for creating a new {@code Aes} aesthetic mapping instance.
     *
     * @return a new {@code Aes} builder instance
     */
    public static Aes aes() {
        return new Aes();
    }

    /**
     * Maps a dataset column to the X-axis position aesthetic.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes x(String col) {
        values.put(Aesthetic.X, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the X-axis position aesthetic.
     *
     * @param value the {@link AesValue}, e.g. {@code AesValue.afterStat(...)}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes x(AesValue value) {
        values.put(Aesthetic.X, value);
        return this;
    }

    /**
     * Maps a dataset column to the Y-axis position aesthetic.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes y(String col) {
        values.put(Aesthetic.Y, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Y-axis position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes y(AesValue value) {
        values.put(Aesthetic.Y, value);
        return this;
    }

    /**
     * Maps a dataset column to the X-end position aesthetic — the end point of
     * a directed geometry such as {@code Geoms.segment()}/{@code Geoms.curve()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xend(String col) {
        values.put(Aesthetic.XEND, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the X-end position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xend(AesValue value) {
        values.put(Aesthetic.XEND, value);
        return this;
    }

    /**
     * Maps a dataset column to the Y-end position aesthetic — the end point of
     * a directed geometry such as {@code Geoms.segment()}/{@code Geoms.curve()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes yend(String col) {
        values.put(Aesthetic.YEND, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Y-end position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes yend(AesValue value) {
        values.put(Aesthetic.YEND, value);
        return this;
    }

    /**
     * Maps a dataset column to the Z-end position aesthetic — the 3D end point
     * of a directed geometry such as {@code Geoms.segment3d()}.
     *
     * @param col the column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zend(String col) {
        values.put(Aesthetic.ZEND, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Maps a dataset column to the X-min position aesthetic — the lower bound
     * of a range geometry such as {@code Geoms.errorbarh()}/{@code Geoms.ribbon()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xmin(String col) {
        values.put(Aesthetic.XMIN, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the X-min position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xmin(AesValue value) {
        values.put(Aesthetic.XMIN, value);
        return this;
    }

    /**
     * Maps a dataset column to the X-max position aesthetic — the upper bound
     * of a range geometry such as {@code Geoms.errorbarh()}/{@code Geoms.ribbon()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xmax(String col) {
        values.put(Aesthetic.XMAX, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the X-max position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes xmax(AesValue value) {
        values.put(Aesthetic.XMAX, value);
        return this;
    }

    /**
     * Maps a dataset column to the Y-min position aesthetic — the lower bound
     * of a range geometry such as {@code Geoms.errorbar()}/{@code Geoms.ribbon()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes ymin(String col) {
        values.put(Aesthetic.YMIN, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Y-min position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes ymin(AesValue value) {
        values.put(Aesthetic.YMIN, value);
        return this;
    }

    /**
     * Maps a dataset column to the Y-max position aesthetic — the upper bound
     * of a range geometry such as {@code Geoms.errorbar()}/{@code Geoms.ribbon()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes ymax(String col) {
        values.put(Aesthetic.YMAX, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Y-max position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes ymax(AesValue value) {
        values.put(Aesthetic.YMAX, value);
        return this;
    }

    /**
     * Maps a dataset column to the Z-min position aesthetic — the lower bound
     * of a 3D range geometry.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zmin(String col) {
        values.put(Aesthetic.ZMIN, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Z-min position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zmin(AesValue value) {
        values.put(Aesthetic.ZMIN, value);
        return this;
    }

    /**
     * Maps a dataset column to the Z-max position aesthetic — the upper bound
     * of a 3D range geometry.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zmax(String col) {
        values.put(Aesthetic.ZMAX, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Z-max position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zmax(AesValue value) {
        values.put(Aesthetic.ZMAX, value);
        return this;
    }

    /**
     * Maps a dataset column to the Z-axis position aesthetic (3D).
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes z(String col) {
        values.put(Aesthetic.Z, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the Z-axis position aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes z(AesValue value) {
        values.put(Aesthetic.Z, value);
        return this;
    }

    /**
     * Maps a dataset column to the observation weight aesthetic, used by
     * counting statistics such as {@code Stats.bar3d()}.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes weight(String col) {
        values.put(Aesthetic.WEIGHT, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the observation weight aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes weight(AesValue value) {
        values.put(Aesthetic.WEIGHT, value);
        return this;
    }

    /**
     * Maps a dataset column to the explicit data grouping aesthetic.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes group(String col) {
        values.put(Aesthetic.GROUP, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the explicit data grouping aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes group(AesValue value) {
        values.put(Aesthetic.GROUP, value);
        return this;
    }

    /**
     * Maps a dataset column to the stroke/outline color aesthetic.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes color(String col) {
        values.put(Aesthetic.COLOR, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the stroke/outline color aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes color(AesValue value) {
        values.put(Aesthetic.COLOR, value);
        return this;
    }

    /**
     * Maps a dataset column to the fill color aesthetic.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes fill(String col) {
        values.put(Aesthetic.FILL, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the fill color aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes fill(AesValue value) {
        values.put(Aesthetic.FILL, value);
        return this;
    }

    /**
     * Maps a dataset column to point shape symbols (e.g., circle, square, triangle, diamond, cross).
     *
     * @param columnName column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes shape(String columnName) {
        values.put(Aesthetic.SHAPE, columnName == null ? null : AesValue.raw(columnName));
        return this;
    }

    /**
     * Binds a typed value to the point shape aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes shape(AesValue value) {
        values.put(Aesthetic.SHAPE, value);
        return this;
    }

    /**
     * Maps a dataset column to line dash patterns (e.g., solid, dashed,
     * dotted). Each distinct category is drawn with a different dash pattern,
     * following the {@code linetypeScale} semantics.
     *
     * @param columnName column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes linetype(String columnName) {
        values.put(Aesthetic.LINETYPE, columnName == null ? null : AesValue.raw(columnName));
        return this;
    }

    /**
     * Binds a typed value to the line-type aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes linetype(AesValue value) {
        values.put(Aesthetic.LINETYPE, value);
        return this;
    }

    /**
     * Maps a dataset column to point sizes. Values scale the point area:
     * the smallest value gets the smallest dot, the largest the biggest,
     * with radius proportional to the square root of the value (conventional
     * a size scale semantics).
     *
     * @param columnName column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes size(String columnName) {
        values.put(Aesthetic.SIZE, columnName == null ? null : AesValue.raw(columnName));
        return this;
    }

    /**
     * Binds a typed value to the point size aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes size(AesValue value) {
        values.put(Aesthetic.SIZE, value);
        return this;
    }

    /**
     * Maps a dataset column to the transparency (alpha) aesthetic.
     * Values are normalised to [0,&nbsp;1] over the column range and
     * multiplied with the geom's base opacity. A column of {@code 0.5}
     * makes every point half-transparent; a column that varies from
     * {@code 0.2} to {@code 1.0} produces a gradient from nearly
     * invisible to fully opaque.
     *
     * @param columnName column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes alpha(String columnName) {
        values.put(Aesthetic.ALPHA, columnName == null ? null : AesValue.raw(columnName));
        return this;
    }

    /**
     * Binds a typed value to the alpha (transparency) aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes alpha(AesValue value) {
        values.put(Aesthetic.ALPHA, value);
        return this;
    }

    /**
     * Maps a dataset column to the text drawn by {@code Geoms.text()}.
     * Rows with a {@code null} or blank value are left unlabelled.
     *
     * @param col column name in the DataFrame
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes label(String col) {
        values.put(Aesthetic.LABEL, col == null ? null : AesValue.raw(col));
        return this;
    }

    /**
     * Binds a typed value to the text label aesthetic.
     *
     * @param value the {@link AesValue}
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes label(AesValue value) {
        values.put(Aesthetic.LABEL, value);
        return this;
    }

    /**
     * Returns the wire value mapped to the text label aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String label() { return render(Aesthetic.LABEL); }

    /**
     * Returns the typed value mapped to the text label aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue labelValue() { return values.get(Aesthetic.LABEL); }

    /**
     * Returns the wire value mapped to the X-axis position.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String x() { return render(Aesthetic.X); }

    /**
     * Returns the typed value mapped to the X-axis position.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue xValue() { return values.get(Aesthetic.X); }

    /**
     * Returns the wire value mapped to the Y-axis position.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String y() { return render(Aesthetic.Y); }

    /**
     * Returns the typed value mapped to the Y-axis position.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue yValue() { return values.get(Aesthetic.Y); }

    /**
     * Returns the wire value mapped to the X-end position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String xend() { return render(Aesthetic.XEND); }

    /**
     * Returns the typed value mapped to the X-end position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue xendValue() { return values.get(Aesthetic.XEND); }

    /**
     * Returns the wire value mapped to the Y-end position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String yend() { return render(Aesthetic.YEND); }

    /**
     * Returns the typed value mapped to the Y-end position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue yendValue() { return values.get(Aesthetic.YEND); }

    /**
     * Returns the wire value mapped to the X-min position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String xmin() { return render(Aesthetic.XMIN); }

    /**
     * Returns the typed value mapped to the X-min position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue xminValue() { return values.get(Aesthetic.XMIN); }

    /**
     * Returns the wire value mapped to the X-max position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String xmax() { return render(Aesthetic.XMAX); }

    /**
     * Returns the typed value mapped to the X-max position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue xmaxValue() { return values.get(Aesthetic.XMAX); }

    /**
     * Returns the wire value mapped to the Y-min position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String ymin() { return render(Aesthetic.YMIN); }

    /**
     * Returns the typed value mapped to the Y-min position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue yminValue() { return values.get(Aesthetic.YMIN); }

    /**
     * Returns the wire value mapped to the Y-max position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String ymax() { return render(Aesthetic.YMAX); }

    /**
     * Returns the typed value mapped to the Y-max position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue ymaxValue() { return values.get(Aesthetic.YMAX); }

    /**
     * Returns the wire value mapped to the Z-min position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String zmin() { return render(Aesthetic.ZMIN); }

    /**
     * Returns the typed value mapped to the Z-min position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue zminValue() { return values.get(Aesthetic.ZMIN); }

    /**
     * Returns the wire value mapped to the Z-max position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String zmax() { return render(Aesthetic.ZMAX); }

    /**
     * Returns the typed value mapped to the Z-max position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue zmaxValue() { return values.get(Aesthetic.ZMAX); }

    /**
     * Returns the wire value mapped to the Z-axis position (3D).
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String z() { return render(Aesthetic.Z); }

    /**
     * Returns the typed value mapped to the Z-axis position (3D).
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue zValue() { return values.get(Aesthetic.Z); }

    /**
     * Returns the wire value mapped to the Z-end position aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String zend() { return render(Aesthetic.ZEND); }

    /**
     * Returns the typed value mapped to the Z-end position aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue zendValue() { return values.get(Aesthetic.ZEND); }

    /**
     * Binds a typed value to the Z-end position aesthetic.
     *
     * @param value the {@link AesValue}, or {@code null} to unset
     * @return this {@code Aes} instance for fluid chaining
     */
    public Aes zend(AesValue value) {
        values.put(Aesthetic.ZEND, value);
        return this;
    }

    /**
     * Returns the wire value mapped to the stroke/outline color aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String color() { return render(Aesthetic.COLOR); }

    /**
     * Returns the typed value mapped to the stroke/outline color aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue colorValue() { return values.get(Aesthetic.COLOR); }

    /**
     * Returns the wire value mapped to the fill color aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String fill() { return render(Aesthetic.FILL); }

    /**
     * Returns the typed value mapped to the fill color aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue fillValue() { return values.get(Aesthetic.FILL); }

    /**
     * Returns the wire value mapped to the point shape aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String shape() { return render(Aesthetic.SHAPE); }

    /**
     * Returns the typed value mapped to the point shape aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue shapeValue() { return values.get(Aesthetic.SHAPE); }

    /**
     * Returns the wire value mapped to the observation weight aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String weight() { return render(Aesthetic.WEIGHT); }

    /**
     * Returns the typed value mapped to the observation weight aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue weightValue() { return values.get(Aesthetic.WEIGHT); }

    /**
     * Returns the wire value mapped to the line-type aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String linetype() { return render(Aesthetic.LINETYPE); }

    /**
     * Returns the typed value mapped to the line-type aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue linetypeValue() { return values.get(Aesthetic.LINETYPE); }

    /**
     * Returns the wire value mapped to the point size aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String size() { return render(Aesthetic.SIZE); }

    /**
     * Returns the typed value mapped to the point size aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue sizeValue() { return values.get(Aesthetic.SIZE); }

    /**
     * Returns the wire value mapped to the alpha (transparency) aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String alpha() { return render(Aesthetic.ALPHA); }

    /**
     * Returns the typed value mapped to the alpha (transparency) aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue alphaValue() { return values.get(Aesthetic.ALPHA); }

    /**
     * Returns the wire value mapped to the data grouping aesthetic.
     *
     * @return the wire value, or {@code null} if not mapped
     */
    public String group() { return render(Aesthetic.GROUP); }

    /**
     * Returns the typed value mapped to the data grouping aesthetic.
     *
     * @return the {@link AesValue}, or {@code null} if not mapped
     */
    public AesValue groupValue() { return values.get(Aesthetic.GROUP); }

    /**
     * Merges a local (per-geom) aesthetic mapping over this mapping, following
     * the override semantics: every aesthetic set on {@code local}
     * replaces the corresponding one on {@code this}, while the aesthetics
     * {@code local} leaves unset fall back to {@code this}. Never mutates
     * either mapping.
     * <p>
     * The result is the effective mapping a geometry should render with when it
     * carries its own {@code aes()} over a plot-global mapping.
     *
     * @param local the local aesthetic mapping overriding this one, or
     *              {@code null} to return this mapping unchanged
     * @return a new merged {@code Aes}, or {@code this} when {@code local} is
     *         {@code null}
     */
    public Aes overrideWith(Aes local) {
        if (local == null) {
            return this;
        }
        var merged = new EnumMap<>(values);
        for (var aesthetic : Aesthetic.values()) {
            var localValue = local.values.get(aesthetic);
            if (localValue != null) {
                merged.put(aesthetic, localValue);
            }
        }
        return new Aes(merged);
    }
    /**
     * The typed value currently mapped to the given aesthetic.
     *
     * @param aesthetic the aesthetic to read
     * @return the mapped {@link AesValue}, or {@code null} when unset
     */
    AesValue value(Aesthetic aesthetic) {
        return values.get(aesthetic);
    }

    /**
     * Maps a typed value to the given aesthetic.
     *
     * @param aesthetic the aesthetic to write
     * @param v         the {@link AesValue}, or {@code null} to unset
     * @return this {@code Aes} instance for fluid chaining
     */
    Aes set(Aesthetic aesthetic, AesValue v) {
        values.put(aesthetic, v);
        return this;
    }

    private String render(Aesthetic aesthetic) {
        var v = values.get(aesthetic);
        return v == null ? null : v.render();
    }
}
