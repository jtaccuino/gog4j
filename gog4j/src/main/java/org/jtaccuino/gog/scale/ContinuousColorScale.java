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
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;

/**
 * A continuous colour scale: maps numeric data values to colours by
 * interpolating through a named colour ramp, and carries the colourbar's
 * domain, tick breaks, and labels.
 * <p>
 * Built-in ramps: viridis, plasma, blues, greens, reds, greys.
 * Each ramp defines 5 colour stops at equally-spaced positions.
 * <p>
 * Layers that colour by a continuous quantity use the ramp directly via
 * {@link #colorFor(double, double, double)}; the plot pipeline builds a fully
 * specified scale through {@link #forRange} to drive a colourbar guide.
 */
public class ContinuousColorScale implements ColorScale {

    private static final Map<String, List<Stop>> RAMPS = new LinkedHashMap<>();

    static {
        RAMPS.put("viridis", List.of(
            new Stop(0.00, Color.web("#440154")),
            new Stop(0.25, Color.web("#3b528b")),
            new Stop(0.50, Color.web("#21918c")),
            new Stop(0.75, Color.web("#5ec962")),
            new Stop(1.00, Color.web("#fde725"))
        ));
        RAMPS.put("plasma", List.of(
            new Stop(0.00, Color.web("#0d0887")),
            new Stop(0.25, Color.web("#6a00a8")),
            new Stop(0.50, Color.web("#b12a90")),
            new Stop(0.75, Color.web("#e16462")),
            new Stop(1.00, Color.web("#fca636"))
        ));
        RAMPS.put("blues", List.of(
            new Stop(0.00, Color.web("#f7fbff")),
            new Stop(0.25, Color.web("#c6dbef")),
            new Stop(0.50, Color.web("#6baed6")),
            new Stop(0.75, Color.web("#2171b5")),
            new Stop(1.00, Color.web("#08306b"))
        ));
        RAMPS.put("greens", List.of(
            new Stop(0.00, Color.web("#f7fcf5")),
            new Stop(0.25, Color.web("#c7e9c0")),
            new Stop(0.50, Color.web("#74c476")),
            new Stop(0.75, Color.web("#238b45")),
            new Stop(1.00, Color.web("#00441b"))
        ));
        RAMPS.put("reds", List.of(
            new Stop(0.00, Color.web("#fff5f0")),
            new Stop(0.25, Color.web("#fcbba1")),
            new Stop(0.50, Color.web("#fb6a4a")),
            new Stop(0.75, Color.web("#de2d26")),
            new Stop(1.00, Color.web("#67000d"))
        ));
        RAMPS.put("greys", List.of(
            new Stop(0.00, Color.web("#ffffff")),
            new Stop(0.25, Color.web("#cccccc")),
            new Stop(0.50, Color.web("#999999")),
            new Stop(0.75, Color.web("#666666")),
            new Stop(1.00, Color.web("#000000"))
        ));
    }

    private final List<Stop> stops;
    private final String name;
    private final String columnName;
    private final double min;
    private final double max;
    private final List<Double> breaks;
    private final List<String> labels;

    /**
     * Creates a continuous colour scale from the named built-in ramp.
     *
     * @param cmapName the name of a built-in colour ramp (defaults to "viridis" if not found)
     */
    public ContinuousColorScale(String cmapName) {
        this(null, cmapName);
    }

    /** Creates a scale using the default "viridis" colour ramp. */
    public ContinuousColorScale() { this((String) null); }

    /**
     * Creates a continuous colour scale from an explicit list of colours,
     * distributed at evenly-spaced positions across the ramp — the engine
     * behind {@code scaleColorGradient(low, high)} and brewer palettes.
     *
     * @param colors the ramp colours in low-to-high order
     * @throws NullPointerException if {@code colors} is {@code null}
     */
    public ContinuousColorScale(List<Color> colors) {
        this(null, Double.NaN, Double.NaN, null, null, "custom", stopsFromColors(requireColors(colors)));
    }

    private static List<Color> requireColors(List<Color> colors) {
        if (colors == null || colors.isEmpty()) {
            throw new NullPointerException("colors must be non-null and non-empty");
        }
        return colors;
    }

    /**
     * Builds a fully specified continuous scale for a column of values: the
     * numeric domain, the "pretty" tick breaks with {@link Scale#formatTick}
     * labels, and the colour ramp.
     *
     * @param columnName the aesthetic column this scale maps
     * @param values     the raw column values
     * @param cmapName   the colour ramp name, or {@code null} for viridis
     * @return the scale, or {@code null} when the values carry no numeric range
     */
    public static ContinuousColorScale forRange(String columnName, List<?> values, String cmapName) {
        double fMin = Double.MAX_VALUE, fMax = -Double.MAX_VALUE;
        for (var v : values) {
            if (v instanceof Number n) {
                double d = n.doubleValue();
                if (d < fMin) fMin = d;
                if (d > fMax) fMax = d;
            }
        }
        if (fMax < fMin) {
            return null;
        }
        var breaks = Scale.niceBreaks(fMin, fMax, 5);
        var labels = breaks.stream().map(Scale::formatTick).toList();
        return new ContinuousColorScale(columnName, fMin, fMax, breaks, labels, cmapName);
    }

    private ContinuousColorScale(String columnName, String cmapName) {
        this(columnName, Double.NaN, Double.NaN, null, null, cmapName);
    }

    private ContinuousColorScale(String columnName, double min, double max,
                                 List<Double> breaks, List<String> labels, String cmapName) {
        this(columnName, min, max, breaks, labels, resolveRampName(cmapName), rampStops(cmapName));
    }

    private static String resolveRampName(String cmapName) {
        if (cmapName != null && (RAMPS.containsKey(cmapName) || Palettes.of(cmapName) != null)) {
            return cmapName;
        }
        return "viridis";
    }

    private static List<Stop> rampStops(String cmapName) {
        var found = cmapName == null ? null : RAMPS.get(cmapName);
        if (found != null) {
            return found;
        }
        // A brewer/palette name produces an evenly-spaced ramp from its colours.
        var palette = cmapName == null ? null : Palettes.of(cmapName);
        if (palette != null) {
            return stopsFromColors(palette);
        }
        return RAMPS.get("viridis");
    }

    private static List<Stop> stopsFromColors(List<Color> colors) {
        int n = colors.size();
        var out = new ArrayList<Stop>(n);
        for (int i = 0; i < n; i++) {
            double pos = n <= 1 ? 0.0 : (double) i / (n - 1);
            out.add(new Stop(pos, colors.get(i)));
        }
        return out;
    }

    /**
     * Builds a fully specified continuous scale from an explicit list of colour
     * stops — the engine for plot-level gradients and brewer ramps.
     *
     * @param columnName the aesthetic column this scale maps
     * @param values     the raw column values
     * @param colors     the ramp colours in low-to-high order
     * @return the scale, or {@code null} when the values carry no numeric range
     */
    public static ContinuousColorScale fromColors(String columnName, List<?> values, List<Color> colors) {
        double fMin = Double.MAX_VALUE, fMax = -Double.MAX_VALUE;
        for (var v : values) {
            if (v instanceof Number n) {
                double d = n.doubleValue();
                if (d < fMin) fMin = d;
                if (d > fMax) fMax = d;
            }
        }
        if (fMax < fMin) {
            return null;
        }
        var breaks = Scale.niceBreaks(fMin, fMax, 5);
        var labels = breaks.stream().map(Scale::formatTick).toList();
        return new ContinuousColorScale(columnName, fMin, fMax, breaks, labels, "custom", stopsFromColors(colors));
    }

    private ContinuousColorScale(String columnName, double min, double max,
                                 List<Double> breaks, List<String> labels, String name, List<Stop> stops) {
        this.name = name;
        this.stops = stops;
        this.columnName = columnName;
        this.min = min;
        this.max = max;
        this.breaks = breaks;
        this.labels = labels;
    }

    @Override
    public boolean isContinuous() { return true; }

    /** {@return the configured colour ramp name} */
    public String name() { return name; }

    /** {@return the aesthetic column this scale maps} */
    public String columnName() { return columnName; }

    /** {@return the lower bound of the mapped domain} */
    public double min() { return min; }

    /** {@return the upper bound of the mapped domain} */
    public double max() { return max; }

    /** {@return the colourbar's domain} */
    public MinMax domain() { return new MinMax(min, max); }

    /** {@return the tick break positions for the colourbar} */
    public List<Double> breaks() { return breaks; }

    /** {@return the tick labels for the colourbar breaks} */
    public List<String> labels() { return labels; }

    /** {@return the distinct ramp colours, in low-to-high order} */
    public List<Color> colors() {
        var out = new java.util.ArrayList<Color>(stops.size());
        for (var s : stops) {
            out.add(s.color());
        }
        return out;
    }

    /**
     * Bins the ramp into {@code n} discrete steps and returns one colour per
     * step — the palette behind {@code scale_*_binned} and {code _d} scales.
     *
     * @param n the number of discrete steps
     * @return {@code n} distinct ramp colours sampled across the ramp
     */
    public List<Color> sampleSteps(int n) {
        var out = new java.util.ArrayList<Color>(Math.max(1, n));
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0.0 : (double) i / (n - 1);
            out.add(colorAt(t));
        }
        return out;
    }

    /** Interpolates the ramp at a fractional position {@code [0, 1]}. */
    private Color colorAt(double t) {
        t = Math.max(0, Math.min(1, t));
        for (int i = 0; i < stops.size() - 1; i++) {
            var lo = stops.get(i);
            var hi = stops.get(i + 1);
            if (t >= lo.pos() && t <= hi.pos()) {
                double local = hi.pos() - lo.pos() <= 0 ? 0.0 : (t - lo.pos()) / (hi.pos() - lo.pos());
                return lo.color().interpolate(hi.color(), local);
            }
        }
        return stops.get(stops.size() - 1).color();
    }

    /**
     * Maps a data value to a colour within this scale's stored domain.
     *
     * @param value the data value to map
     * @return the interpolated {@link Color}
     */
    public Color colorFor(double value) {
        return colorFor(value, min, max);
    }

    /**
     * Maps a data value to a colour by interpolating within the colour ramp.
     *
     * @param value the data value to map
     * @param min   the minimum of the data range
     * @param max   the maximum of the data range
     * @return the interpolated {@link Color}
     */
    public Color colorFor(double value, double min, double max) {
        if (max <= min) return stops.get(0).color();
        double t = (value - min) / (max - min);
        t = Math.max(0, Math.min(1, t));

        for (int i = 0; i < stops.size() - 1; i++) {
            var lo = stops.get(i);
            var hi = stops.get(i + 1);
            if (t >= lo.pos() && t <= hi.pos()) {
                double local = (t - lo.pos()) / (hi.pos() - lo.pos());
                return lo.color().interpolate(hi.color(), local);
            }
        }
        return stops.get(stops.size() - 1).color();
    }

    /** A colour stop at a position {@code [0, 1]} along the ramp. */
    private record Stop(double pos, Color color) {}
}
