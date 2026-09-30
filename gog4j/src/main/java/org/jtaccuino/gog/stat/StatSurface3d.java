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
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.surface3d()} — the point-row pass-through for 3D surfaces.
 * <p>
 * Requires {@code x}/{@code y}/{@code z}, filters NAs, and (when the data
 * form a complete regular grid) computes per-point gradients so the downstream
 * geometry can use them for lighting. The downstream geometry tessellates these
 * point rows into tiles.
 *
 * @param <DF> the DataFrame type
 */
public class StatSurface3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a Stats.surface3d() transformation.
     */
    public StatSurface3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        if (aes.x() == null || aes.y() == null || aes.z() == null) {
            return StatData.builder().build();
        }
        var dropMissing = params.getBoolean("na.rm", false);
        var rawX = ext.getColumn(df, aes.x());
        var rawY = ext.getColumn(df, aes.y());
        var rawZ = ext.getColumn(df, aes.z());
        var rawG = aes.group() == null ? null : ext.getColumn(df, aes.group());

        var rows = numericRows(rawX, rawY, rawZ, dropMissing);
        if (rows == null || rows.rows().length < 3) {
            return StatData.builder().build();
        }
        var x = rows.x();
        var y = rows.y();
        var z = rows.z();
        var g = rawG == null ? null : new String[rows.rows().length];
        for (int i = 0; i < rows.rows().length; i++) {
            if (rawG != null) {
                var orig = rawG.get(rows.rows()[i]);
                g[i] = orig == null ? "-1__group" : orig + "__group";
            }
        }

        boolean regular = MeshUtil.isRegularGrid(x, y, g);
        var grad = regular ? MeshUtil.gradients(x, y, z, null, null) : null;

        var builder = StatData.builder();
        for (int i = 0; i < rows.rows().length; i++) {
            builder.add("x", x[i]);
            builder.add("y", y[i]);
            builder.add("z", z[i]);
            builder.add("group", g != null ? g[i] : "-1__group");
            if (grad != null) {
                addGradient(builder, grad, i);
            }
        }
        return builder.build();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "dzdx", "dzdy", "slope", "aspect");
    }
}
