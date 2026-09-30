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

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.cor()} statistical transformation: computes the Pearson
 * correlation coefficient of an {@code (x, y)} column pair and exposes the
 * formatted label ({@code "r = 0.93"}) as a per-row computed variable that can
 * be referenced from an aesthetic mapping via
 * {@link AesValue#afterStat(AesValue.ComputedVariable)} (e.g.
 * {@code aes().label(AesValue.afterStat(AesValue.ComputedVariable.CORR))}).
 * <p>
 * The output passes the {@code x}/{@code y} values through untouched and fills
 * the {@code corr} column on the single anchor row (the row whose x value is
 * maximal) while every other row stays blank, so a sparse text layer draws
 * exactly one correlation annotation per cell frame.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatCor<DF> implements Stat<DF> {

    // The correlation label and its anchor depend only on the (x, y) column
    // pair and the data; a matrix cell re-renders the same full-dataset frame
    // on every redraw, so memoize the last computed output. Keyed on the input
    // frame instance and column names: when they are unchanged the data has not
    // changed and the previous StatData is reused verbatim.
    private DF cachedDf;
    private String cachedXName;
    private String cachedYName;
    private StatData cached;

    /**
     * Constructs a Stats.cor transformation.
     */
    public StatCor() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xName = aes.x();
        var yName = aes.y();
        if (Objects.equals(df, cachedDf) && java.util.Objects.equals(xName, cachedXName)
                && java.util.Objects.equals(yName, cachedYName)) {
            return cached;
        }
        var xCol = xName == null ? null : ext.getColumn(df, xName);
        var yCol = yName == null ? null : ext.getColumn(df, yName);
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }
        double rho = pearson(xCol, yCol);
        String text = String.format(Locale.ROOT, "r = %.2f", rho);
        int anchor = anchorRow(xCol, yCol);
        int rows = ext.getRowCount(df);
        var builder = StatData.builder();
        for (int i = 0; i < rows; i++) {
            builder.add("x", xCol.get(i))
                    .add("y", yCol.get(i))
                    .add(AesValue.ComputedVariable.CORR.column(), i == anchor ? text : "");
        }
        cachedDf = df;
        cachedXName = xName;
        cachedYName = yName;
        cached = builder.build();
        return cached;
    }

    @Override
    public List<String> outputColumns() {
        return List.of("x", "y", AesValue.ComputedVariable.CORR.column());
    }

    /**
     * Pearson sample correlation between two parallel columns.
     * Rows where either value is null or non-numeric are skipped.
     *
     * @param a first column
     * @param b the second column
     * @return the correlation coefficient, or {@link Double#NaN} when fewer
     *         than two usable rows remain
     */
    public static double pearson(List<?> a, List<?> b) {
        int n = Math.min(a.size(), b.size());
        int count = 0;
        double sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) {
            var xa = Values.toDoubleOrNull(a.get(i));
            var ya = Values.toDoubleOrNull(b.get(i));
            if (xa == null || ya == null) continue;
            sumX += xa;
            sumY += ya;
            count++;
        }
        if (count < 2) return Double.NaN;
        double meanX = sumX / count;
        double meanY = sumY / count;
        double num = 0, dx2 = 0, dy2 = 0;
        for (int i = 0; i < n; i++) {
            var xa = Values.toDoubleOrNull(a.get(i));
            var ya = Values.toDoubleOrNull(b.get(i));
            if (xa == null || ya == null) continue;
            double dx = xa - meanX;
            double dy = ya - meanY;
            num += dx * dy;
            dx2 += dx * dx;
            dy2 += dy * dy;
        }
        double denom = Math.sqrt(dx2 * dy2);
        return denom == 0 ? 0.0 : num / denom;
    }

    /**
     * Row index whose x value is maximal; ties broken by y.
     * Skips null / non-numeric entries.
     */
    private static int anchorRow(List<?> x, List<?> y) {
        int n = Math.min(x.size(), y.size());
        int best = -1;
        double bx = Double.NEGATIVE_INFINITY;
        double by = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            var vx = Values.toDoubleOrNull(x.get(i));
            var vy = Values.toDoubleOrNull(y.get(i));
            if (vx == null || vy == null) continue;
            if (best == -1
                    || vx > bx
                    || (vx == bx && vy > by)) {
                best = i;
                bx = vx;
                by = vy;
            }
        }
        return best >= 0 ? best : 0;
    }
}
