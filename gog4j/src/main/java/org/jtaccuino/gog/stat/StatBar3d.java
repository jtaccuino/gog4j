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
package org.jtaccuino.gog.stat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.GridMetrics;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.bar3d()} — automatic 2-D counting or binning into 3-D bars (the
 * 3-D analogue of {@code Geoms.bar()}/{@code Geoms.histogram()}).
 * <p>
 * The stat detects whether x and y are discrete or continuous: both discrete
 * count each (x, y) combination, both continuous bin into a 2-D grid, and a
 * mixed pair bins the continuous axis while keeping the discrete groups. The
 * bar height defaults to {@code count}, or to whichever computed variable
 * ({@code count}/{@code proportion}/{@code ncount}/{@code density}/
 * {@code ndensity}) the {@code z} aesthetic maps to.
 *
 * @param <DF> the DataFrame type
 */
public class StatBar3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.bar3d()} transformation.
     */
    public StatBar3d() {
    }

    /** One aggregated bar: its bin position, count, and preserved aesthetics. */
    private record Agg(double x, double y, double count, double binArea,
                       Object fill, Object color, Object alpha) {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var rawX = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var rawY = aes.y() == null ? null : ext.getColumn(df, aes.y());
        if (rawX == null || rawY == null) {
            return StatData.builder().build();
        }
        var dropMissing = params.getBoolean("na.rm", false);
        var weightCol = aes.weight() == null ? null : ext.getColumn(df, aes.weight());
        int n = rawX.size();
        if (n < 1) {
            return StatData.builder().build();
        }

        boolean xDiscrete = ext.columnType(df, aes.x()) != DataExtractor.ColumnType.NUMBER;
        boolean yDiscrete = ext.columnType(df, aes.y()) != DataExtractor.ColumnType.NUMBER;

        var fillCol = aes.fill() == null || Aes.isComputed(aes.fill()) ? null : ext.getColumn(df, aes.fill());
        var colorCol = aes.color() == null || Aes.isComputed(aes.color()) ? null : ext.getColumn(df, aes.color());
        var alphaCol = aes.alpha() == null || Aes.isComputed(aes.alpha()) ? null : ext.getColumn(df, aes.alpha());

        // Weighted input rows, dropping missing positions when requested.
        var rows = new ArrayList<int[]>();
        for (int i = 0; i < n; i++) {
            var rx = rawX.get(i);
            var ry = rawY.get(i);
            // Discrete (text) positions are valid and mapped to ordinals below;
            // only null positions are dropped.
            if (rx == null || ry == null) {
                if (dropMissing) {
                    continue;
                }
                return StatData.builder().build();
            }
            rows.add(new int[] {i});
        }

        var agg = aggregate(rawX, rawY, weightCol, fillCol, colorCol, alphaCol, rows,
                xDiscrete, yDiscrete,
                params.get("bins", 10), params.get("binwidth", null), params.getBoolean("drop", true));

        if (agg.isEmpty()) {
            return StatData.builder().build();
        }

        // Computed bar statistics.
        double total = 0;
        double maxCount = 0;
        for (var a : agg) {
            total += a.count();
            maxCount = Math.max(maxCount, a.count());
        }
        double[] proportion = new double[agg.size()];
        double[] ncount = new double[agg.size()];
        double[] density = new double[agg.size()];
        double[] ndensity = new double[agg.size()];
        double maxDensity = 0;
        for (int i = 0; i < agg.size(); i++) {
            var a = agg.get(i);
            proportion[i] = total > 0 ? a.count() / total : 0;
            ncount[i] = maxCount > 0 ? a.count() / maxCount : 0;
            density[i] = total > 0 && a.binArea() > 0 ? a.count() / (total * a.binArea()) : 0;
            maxDensity = Math.max(maxDensity, density[i]);
        }
        for (int i = 0; i < agg.size(); i++) {
            ndensity[i] = maxDensity > 0 ? density[i] / maxDensity : 0;
        }

        double xSpacing = GridMetrics.resolution(agg.stream().mapToDouble(Agg::x).toArray(), false);
        double ySpacing = GridMetrics.resolution(agg.stream().mapToDouble(Agg::y).toArray(), false);
        double widthX = widthParam(params, 0, xDiscrete ? 0.9 : 1.0);
        double widthY = widthParam(params, 1, yDiscrete ? 0.9 : 1.0);
        CubeFace[] faces = VolumeFaces.selectFaces(params.get("faces", "all"));

        String heightCol = aes.z() != null && Aes.isComputed(aes.z()) ? Aes.statColumn(aes.z()) : "count";

        var builder = StatData.builder();
        for (int i = 0; i < agg.size(); i++) {
            var a = agg.get(i);
            double height = switch (heightCol) {
                case "proportion" -> proportion[i];
                case "ncount" -> ncount[i];
                case "density" -> density[i];
                case "ndensity" -> ndensity[i];
                default -> a.count();
            };
            double halfX = xSpacing * widthX / 2;
            double halfY = ySpacing * widthY / 2;
            var corners = VolumeFaces.columnCorners(a.x() - halfX, a.x() + halfX,
                    a.y() - halfY, a.y() + halfY, 0, height, faces);
            var acc = a;
            var idx = i;
            VolumeFaces.emitFaces(builder, corners, faces, "col", "colId", i,
                    v -> {
                        v.add("count", acc.count());
                        v.add("proportion", proportion[idx]);
                        v.add("ncount", ncount[idx]);
                        v.add("density", density[idx]);
                        v.add("ndensity", ndensity[idx]);
                        if (acc.fill() != null) {
                            v.add(Aes.statColumn(aes.fill()), acc.fill());
                        }
                        if (acc.color() != null && !acc.color().equals(acc.fill())) {
                            v.add(Aes.statColumn(aes.color()), acc.color());
                        }
                        if (acc.alpha() != null) {
                            v.add(Aes.statColumn(aes.alpha()), acc.alpha());
                        }
                    });
        }
        return builder.build();
    }

    /**
     * Aggregates the input rows by the chosen strategy: count per combination
     * (both discrete), 2-D bin (both continuous), or continuous-bin within
     * discrete groups (mixed).
     */
    private List<Agg> aggregate(List<?> rawX, List<?> rawY, List<?> weightCol,
                                List<?> fillCol, List<?> colorCol, List<?> alphaCol,
                                List<int[]> rows, boolean xDiscrete, boolean yDiscrete,
                                Object binsObj, Object binwidthObj, boolean drop) {
        int[] bins = binsObj instanceof int[] ia ? ia : new int[] {binsObj instanceof Number num ? num.intValue() : 10};
        if (bins.length == 1) {
            bins = new int[] {bins[0], bins[0]};
        }
        double[] binwidth = null;
        if (binwidthObj instanceof double[] da && da.length > 0) {
            binwidth = da.length == 1 ? new double[] {da[0], da[0]} : da;
        } else if (binwidthObj instanceof Number num) {
            binwidth = new double[] {num.doubleValue(), num.doubleValue()};
        }

        if (!xDiscrete && !yDiscrete) {
            return bin2d(rawX, rawY, weightCol, rows, bins, binwidth, drop);
        }
        if (xDiscrete && yDiscrete) {
            return count2d(rawX, rawY, weightCol, fillCol, colorCol, alphaCol, rows, drop);
        }
        return binMixed(rawX, rawY, weightCol, fillCol, colorCol, alphaCol, rows,
                xDiscrete, bins, binwidth, drop);
    }

    /** Both continuous: 2-D binning with uniform centers. */
    private List<Agg> bin2d(List<?> rawX, List<?> rawY, List<?> weightCol, List<int[]> rows,
                            int[] bins, double[] binwidth, boolean drop) {
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (int[] r : rows) {
            double x = Values.toDouble(rawX.get(r[0]));
            double y = Values.toDouble(rawY.get(r[0]));
            xMin = Math.min(xMin, x);
            xMax = Math.max(xMax, x);
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }
        double[] xBr = binwidth != null ? seq(xMin, xMax + binwidth[0], binwidth[0]) : linspace(xMin, xMax, bins[0] + 1);
        double[] yBr = binwidth != null ? seq(yMin, yMax + binwidth[1], binwidth[1]) : linspace(yMin, yMax, bins[1] + 1);
        double xWidth = xBr[1] - xBr[0];
        double yWidth = yBr[1] - yBr[0];
        double binArea = xWidth * yWidth;
        double[] xCenters = centers(xBr);
        double[] yCenters = centers(yBr);

        var counts = new LinkedHashMap<Long, double[]>();
        for (int[] r : rows) {
            double x = Values.toDouble(rawX.get(r[0]));
            double y = Values.toDouble(rawY.get(r[0]));
            int xi = binIndex(x, xBr);
            int yi = binIndex(y, yBr);
            if (xi < 0 || yi < 0) {
                continue;
            }
            long key = (long) xi << 32 | (yi & 0xffffffffL);
            counts.computeIfAbsent(key, k -> new double[] {0})[0] += weight(weightCol, r[0]);
        }

        var out = new ArrayList<Agg>();
        for (var e : counts.entrySet()) {
            int xi = (int) (e.getKey() >> 32);
            int yi = (int) (long) e.getKey();
            out.add(new Agg(xCenters[xi], yCenters[yi], e.getValue()[0], binArea, null, null, null));
        }
        if (!drop) {
            for (int xi = 0; xi < xCenters.length; xi++) {
                for (int yi = 0; yi < yCenters.length; yi++) {
                    long key = (long) xi << 32 | (yi & 0xffffffffL);
                    if (!counts.containsKey(key)) {
                        out.add(new Agg(xCenters[xi], yCenters[yi], 0, binArea, null, null, null));
                    }
                }
            }
        }
        return out;
    }

    /** Both discrete: count each (x, y) combination at its ordinal position. */
    private List<Agg> count2d(List<?> rawX, List<?> rawY, List<?> weightCol,
                              List<?> fillCol, List<?> colorCol, List<?> alphaCol, List<int[]> rows,
                              boolean drop) {
        var xIndex = ordinalIndex(rawX, rows);
        var yIndex = ordinalIndex(rawY, rows);
        var counts = new LinkedHashMap<Long, double[]>();
        for (int[] r : rows) {
            int xi = xIndex.get(rawX.get(r[0]));
            int yi = yIndex.get(rawY.get(r[0]));
            long key = (long) xi << 32 | (yi & 0xffffffffL);
            counts.computeIfAbsent(key, k -> new double[] {0})[0] += weight(weightCol, r[0]);
        }
        var out = new ArrayList<Agg>();
        for (var e : counts.entrySet()) {
            int xi = (int) (e.getKey() >> 32);
            int yi = (int) (long) e.getKey();
            // The first row observed at this combination carries the aesthetics.
            Object fill = null, color = null, alpha = null;
            for (int[] r : rows) {
                int ri = r[0];
                if (xIndex.get(rawX.get(ri)) == xi && yIndex.get(rawY.get(ri)) == yi) {
                    if (fillCol != null) {
                        fill = fillCol.get(ri);
                    }
                    if (colorCol != null) {
                        color = colorCol.get(ri);
                    }
                    if (alphaCol != null) {
                        alpha = alphaCol.get(ri);
                    }
                    break;
                }
            }
            out.add(new Agg(xi, yi, e.getValue()[0], 1, fill, color, alpha));
        }
        if (!drop) {
            for (var xe : xIndex.entrySet()) {
                for (var ye : yIndex.entrySet()) {
                    long key = (long) xe.getValue() << 32 | (ye.getValue() & 0xffffffffL);
                    if (!counts.containsKey(key)) {
                        out.add(new Agg(xe.getValue(), ye.getValue(), 0, 1, null, null, null));
                    }
                }
            }
        }
        return out;
    }

    /** Mixed: bin the continuous axis, keep the discrete groups. */
    private List<Agg> binMixed(List<?> rawX, List<?> rawY, List<?> weightCol,
                               List<?> fillCol, List<?> colorCol, List<?> alphaCol, List<int[]> rows,
                               boolean xDiscrete, int[] bins, double[] binwidth, boolean drop) {
        boolean contIsX = !xDiscrete;
        double cMin = Double.POSITIVE_INFINITY, cMax = Double.NEGATIVE_INFINITY;
        for (int[] r : rows) {
            double v = contIsX ? Values.toDouble(rawX.get(r[0])) : Values.toDouble(rawY.get(r[0]));
            cMin = Math.min(cMin, v);
            cMax = Math.max(cMax, v);
        }
        int cBins = contIsX ? bins[0] : bins[1];
        Double cBinwidth = binwidth != null ? (contIsX ? binwidth[0] : binwidth[1]) : null;
        double[] br = cBinwidth != null ? seq(cMin, cMax + cBinwidth, cBinwidth) : linspace(cMin, cMax, cBins + 1);
        double[] centers = centers(br);
        double cWidth = br[1] - br[0];

        // Ordinal positions for the discrete axis.
        var discIndex = xDiscrete ? ordinalIndex(rawX, rows) : ordinalIndex(rawY, rows);

        var counts = new LinkedHashMap<Long, double[]>();
        for (int[] r : rows) {
            int di = xDiscrete ? discIndex.get(rawX.get(r[0])) : discIndex.get(rawY.get(r[0]));
            double cv = contIsX ? Values.toDouble(rawX.get(r[0])) : Values.toDouble(rawY.get(r[0]));
            int ci = binIndex(cv, br);
            if (ci < 0) {
                continue;
            }
            long key = (long) di << 32 | (ci & 0xffffffffL);
            counts.computeIfAbsent(key, k -> new double[] {0})[0] += weight(weightCol, r[0]);
        }
        var out = new ArrayList<Agg>();
        for (var e : counts.entrySet()) {
            int di = (int) (e.getKey() >> 32);
            int ci = (int) (long) e.getKey();
            double x = contIsX ? centers[ci] : di;
            double y = contIsX ? di : centers[ci];
            Object fill = null, color = null, alpha = null;
            for (int[] r : rows) {
                int ri = r[0];
                int rd = xDiscrete ? discIndex.get(rawX.get(ri)) : discIndex.get(rawY.get(ri));
                if (rd == di) {
                    if (fillCol != null) {
                        fill = fillCol.get(ri);
                    }
                    if (colorCol != null) {
                        color = colorCol.get(ri);
                    }
                    if (alphaCol != null) {
                        alpha = alphaCol.get(ri);
                    }
                    break;
                }
            }
            out.add(new Agg(x, y, e.getValue()[0], cWidth, fill, color, alpha));
        }
        if (!drop) {
            for (var de : discIndex.entrySet()) {
                for (int ci = 0; ci < centers.length; ci++) {
                    long key = (long) de.getValue() << 32 | (ci & 0xffffffffL);
                    if (!counts.containsKey(key)) {
                        double x = contIsX ? centers[ci] : de.getValue();
                        double y = contIsX ? de.getValue() : centers[ci];
                        out.add(new Agg(x, y, 0, cWidth, null, null, null));
                    }
                }
            }
        }
        return out;
    }

    private static double weight(List<?> weightCol, int row) {
        if (weightCol == null) {
            return 1;
        }
        var v = weightCol.get(row);
        return v instanceof Number num ? num.doubleValue() : 0;
    }

    private static Map<Object, Integer> ordinalIndex(List<?> values, List<int[]> rows) {
        var index = new LinkedHashMap<Object, Integer>();
        for (int[] r : rows) {
            var v = values.get(r[0]);
            index.computeIfAbsent(v, k -> index.size());
        }
        return index;
    }

    private static int binIndex(double v, double[] breaks) {
        for (int i = 0; i < breaks.length - 1; i++) {
            if (v >= breaks[i] && (i == breaks.length - 2 ? v <= breaks[i + 1] : v < breaks[i + 1])) {
                return i;
            }
        }
        return -1;
    }

    private static double[] centers(double[] breaks) {
        var out = new double[breaks.length - 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = (breaks[i] + breaks[i + 1]) / 2;
        }
        return out;
    }

    private static double[] seq(double from, double to, double by) {
        var out = new ArrayList<Double>();
        for (double v = from; v <= to + 1e-9; v += by) {
            out.add(v);
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double[] linspace(double from, double to, int count) {
        if (count < 2) {
            return new double[] {from, to};
        }
        var out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = from + (to - from) * i / (count - 1);
        }
        return out;
    }

    private static double widthParam(StatParams params, int index, double fallback) {
        var v = params.get("width", fallback);
        if (v instanceof double[] d && d.length > index) {
            return d[index];
        }
        if (v instanceof Number num) {
            return num.doubleValue();
        }
        return fallback;
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "colId", "faceType", "count", "proportion", "ncount", "density", "ndensity");
    }
}
