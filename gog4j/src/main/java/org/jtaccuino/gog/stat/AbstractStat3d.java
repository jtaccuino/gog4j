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
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Common plumbing for the 3-D statistic family ({@code Stats.bar3d()},
 * {@code Stats.col3d()}, {@code Stats.voxel3d()}, {@code Stats.identity3d()},
 * {@code Stats.hull3d()}, {@code Stats.surface3d()}, {@code Stats.density3d()},
 * {@code Stats.function3d()}, {@code Stats.smooth3d()},
 * {@code Stats.distributions3d()}).
 * <p>
 * Every 3-D stat reads its positions from plain {@code x}/{@code y}/{@code z}
 * aesthetics, so the base class carries the shared row bookkeeping they used to
 * duplicate: numeric null/NA filtering, ordinal mapping of discrete columns,
 * per-group partitioning, gradient column emission, and the pass-through of
 * preserved aesthetics.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public abstract class AbstractStat3d<DF> implements Stat<DF> {

    AbstractStat3d() {
    }

    @Override
    public Aes requiredAes() {
        return Aes.aes();
    }

    /**
     * The numeric x/y/z rows kept after {@code na.rm} filtering, with the
     * original row index of each value.
     *
     * @param rawX         the raw x column
     * @param rawY         the raw y column
     * @param rawZ         the raw z column
     * @param dropMissing  whether to silently skip non-numeric rows ({@code true})
     *                     or abort the whole stat ({@code false})
     * @return the kept numeric rows, or {@code null} when a non-numeric row is
     *         present and cannot be dropped
     */
    protected NumericRows numericRows(List<?> rawX, List<?> rawY, List<?> rawZ, boolean dropMissing) {
        var n = rawX.size();
        var x = new double[n];
        var y = new double[n];
        var z = new double[n];
        var keep = new boolean[n];
        int kept = 0;
        for (int i = 0; i < n; i++) {
            if (!(rawX.get(i) instanceof Number)
                    || !(rawY.get(i) instanceof Number)
                    || !(rawZ.get(i) instanceof Number)) {
                if (dropMissing) {
                    continue;
                }
                return null;
            }
            x[i] = ((Number) rawX.get(i)).doubleValue();
            y[i] = ((Number) rawY.get(i)).doubleValue();
            z[i] = ((Number) rawZ.get(i)).doubleValue();
            keep[i] = true;
            kept++;
        }
        var rows = new int[kept];
        var cx = new double[kept];
        var cy = new double[kept];
        var cz = new double[kept];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                rows[k] = i;
                cx[k] = x[i];
                cy[k] = y[i];
                cz[k] = z[i];
                k++;
            }
        }
        return new NumericRows(cx, cy, cz, rows);
    }

    /**
     * The numeric rows kept after {@code na.rm} filtering, partitioned by group
     * label (a single {@code "-1"} bucket when no group is mapped). Validates
     * only those columns that are supplied; pass {@code null} to skip an axis.
     *
     * @param rawX         the raw x column
     * @param rawY         the raw y column
     * @param rawZ         the raw z column, or {@code null} to skip z validation
     * @param rawG         the raw group column, or {@code null}
     * @param dropMissing  whether to silently skip non-numeric rows ({@code true})
     *                     or abort the whole stat ({@code false})
     * @return group label ({@code Map<String, List<Integer>>}) to row indices, or
     *         {@code null} when a non-numeric row is present and cannot be dropped
     */
    protected Map<String, List<Integer>> numericGroups(List<?> rawX, List<?> rawY, List<?> rawZ,
            List<?> rawG, boolean dropMissing) {
        var groups = new LinkedHashMap<String, List<Integer>>();
        for (int i = 0; i < rawX.size(); i++) {
            var rz = rawZ == null ? null : rawZ.get(i);
            if (!(rawX.get(i) instanceof Number) || !(rawY.get(i) instanceof Number)
                    || (rawZ != null && !(rz instanceof Number))) {
                if (dropMissing) {
                    continue;
                }
                return null;
            }
            var label = rawG == null || rawG.get(i) == null ? "-1" : String.valueOf(rawG.get(i));
            groups.computeIfAbsent(label, k -> new ArrayList<>()).add(i);
        }
        return groups;
    }

    /**
     * Numeric positions for a position column identified by {@code rows}:
     * identity for continuous columns, a 0-based ordinal for discrete ones.
     *
     * @param ext    the data extraction strategy
     * @param df     the underlying DataFrame
     * @param column the position aesthetic name
     * @param values the raw column values
     * @param rows   the row indices to position
     * @return the numeric positions, parallel to {@code rows}
     */
    protected double[] numericPositions(DataExtractor<DF> ext, DF df, String column,
            List<?> values, int[] rows) {
        var numeric = ext.columnType(df, column) == DataExtractor.ColumnType.NUMBER;
        var out = new double[rows.length];
        if (numeric) {
            for (int i = 0; i < rows.length; i++) {
                out[i] = Values.toDouble(values.get(rows[i]));
            }
            return out;
        }
        var index = new LinkedHashMap<Object, Double>();
        for (int i = 0; i < rows.length; i++) {
            var v = values.get(rows[i]);
            if (!index.containsKey(v)) {
                index.put(v, (double) index.size());
            }
            out[i] = index.get(v);
        }
        return out;
    }

    /**
     * Copies the mapped non-positional aesthetic values of one input row into
     * the stat output under their bare column names, so the geometry can
     * resolve per-face fill/colour/alpha against the scales.
     *
     * @param ext  the data extraction strategy
     * @param df   the underlying DataFrame
     * @param aes  the effective aesthetic mapping
     * @param row  the input row to preserve
     * @param b    the stat output builder
     */
    protected void addPreserved(DataExtractor<DF> ext, DF df, Aes aes, int row,
            StatData.Builder b) {
        for (String col : new String[] {aes.fill(), aes.color(), aes.alpha(), aes.group()}) {
            if (col == null || Aes.isComputed(col)) {
                continue;
            }
            var values = ext.getColumn(df, col);
            if (values != null && !values.isEmpty()) {
                b.add(col, values.get(Math.min(row, values.size() - 1)));
            }
        }
    }

    /**
     * Adds the derived gradient columns ({@code dzdx}/{@code dzdy},
     * {@code slope}, {@code aspect}) of one point row to the stat output.
     *
     * @param b    the stat output builder
     * @param grad the per-point gradients
     * @param i    the point row index
     */
    protected static void addGradient(StatData.Builder b, MeshUtil.Gradient grad, int i) {
        b.add("dzdx", grad.dzdx()[i]);
        b.add("dzdy", grad.dzdy()[i]);
        b.add("slope", Math.sqrt(grad.dzdx()[i] * grad.dzdx()[i] + grad.dzdy()[i] * grad.dzdy()[i]));
        b.add("aspect", Math.atan2(grad.dzdy()[i], grad.dzdx()[i]));
    }

    /**
     * The numeric rows kept by {@link #numericRows}: parallel arrays plus the
     * original row index of every value.
     *
     * @param x    the kept x values
     * @param y    the kept y values
     * @param z    the kept z values
     * @param rows the original row indices
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only numeric row carrier
    protected record NumericRows(double[] x, double[] y, double[] z, int[] rows) {
    }
}
