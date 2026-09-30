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
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.identity3d()} statistical transformation — the pass-through
 * statistic of the 3D geometry family.
 * <p>
 * It performs no aggregation: every input row survives as one output row with
 * a numeric {@code x}/{@code y}/{@code z} position (discrete columns are
 * mapped to their ordinal position through an ordinal factor encoding so a
 * geometry can consume them as coordinates. Two derived outputs support the
 * depth pipeline:
 * <ul>
 * <li>{@code group} — the hierarchical group id ({@code "<group>__group"}, or
 * {@code "-1__group"} when no group aesthetic is mapped) that
 * {@link org.jtaccuino.gog.layer.DepthSorter} uses to keep grouped primitives
 * contiguous back-to-front, and</li>
 * <li>{@code x_raw}/{@code y_raw}/{@code z_raw} — the original values kept for
 * labelling, referenceable as {@code afterStat} computed variables.</li>
 * </ul>
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatIdentity3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.identity3d()} transformation.
     */
    public StatIdentity3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var yCol = aes.y() == null ? null : ext.getColumn(df, aes.y());
        var zCol = aes.z() == null ? null : ext.getColumn(df, aes.z());
        var gCol = aes.group() == null ? null : ext.getColumn(df, aes.group());
        if (xCol == null || yCol == null) {
            return StatData.builder().build();
        }

        var xPos = positions(ext, df, aes.x(), xCol);
        var yPos = positions(ext, df, aes.y(), yCol);
        var zPos = zCol == null ? Map.of() : positions(ext, df, aes.z(), zCol);

        var builder = StatData.builder();
        for (var i = 0; i < xCol.size(); i++) {
            var rx = xCol.get(i);
            var ry = yCol.get(i);
            var rz = zCol == null ? null : zCol.get(i);
            if (rx == null || ry == null || (zCol != null && rz == null)) {
                continue;
            }
            var group = gCol == null ? null : gCol.get(i);
            builder.add("x", xPos.get(rx));
            builder.add("y", yPos.get(ry));
            if (zCol != null) {
                builder.add("z", zPos.get(rz));
            }
            builder.add("group", group == null ? "-1__group" : group + "__group");
            builder.add("xRaw", rx);
            builder.add("yRaw", ry);
            if (zCol != null) {
                builder.add("zRaw", rz);
            }
        }
        return builder.build();
    }

    /**
     * Maps every distinct raw value of a column to its numeric position: the
     * value itself for continuous columns, a 1-based ordinal for discrete ones
     * (ordered by label, deterministic).
     */
    private static <X> Map<Object, Double> positions(DataExtractor<X> ext, X df, String column, List<?> values) {
        var numeric = ext.columnType(df, column) == DataExtractor.ColumnType.NUMBER;
        var positions = new LinkedHashMap<Object, Double>();
        if (numeric) {
            for (var v : values) {
                if (v != null) {
                    positions.putIfAbsent(v, Values.toDouble(v));
                }
            }
            return positions;
        }
        var distinct = new ArrayList<Object>();
        for (var v : values) {
            if (v != null && !positions.containsKey(v)) {
                distinct.add(v);
                positions.put(v, (double) distinct.size());
            }
        }
        return positions;
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "xRaw", "yRaw", "zRaw");
    }
}
