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
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.geometry.GridMetrics;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.col3d()} — turns grid points into rectangular 3-D columns (the
 * 3-D analogue of {@code Geoms.col()}).
 * <p>
 * Each input row becomes a column extending from a base level ({@code zmin}
 * parameter, {@code zmin} aesthetic, or 0) up to its {@code z} value. Column
 * footprints follow the grid spacing estimated by {@link GridMetrics#resolution},
 * scaled by {@code width}. Faces
 * are emitted with the hierarchical group {@code "col{i}__{face}"} and the
 * {@code col_id}/{@code face_type} computed variables; mapped non-positional
 * aesthetics (fill/colour/alpha) are preserved per vertex.
 *
 * @param <DF> the DataFrame type
 */
public class StatCol3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.col3d()} transformation.
     */
    public StatCol3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var rawX = aes.x() == null ? null : ext.getColumn(df, aes.x());
        var rawY = aes.y() == null ? null : ext.getColumn(df, aes.y());
        var rawZ = aes.z() == null ? null : ext.getColumn(df, aes.z());
        if (rawX == null || rawY == null || rawZ == null) {
            return StatData.builder().build();
        }
        var dropMissing = params.getBoolean("na.rm", false);

        var zminParam = params.get("zmin", null);
        var zminCol = aes.zmin() == null ? null : ext.getColumn(df, aes.zmin());
        Double zminConst = zminParam instanceof Number num ? num.doubleValue() : null;

        var rows = numericRows(rawX, rawY, rawZ, dropMissing);
        if (rows == null || rows.rows().length < 1) {
            return StatData.builder().build();
        }
        var zmin = new double[rows.rows().length];
        for (int i = 0; i < rows.rows().length; i++) {
            int orig = rows.rows()[i];
            if (zminConst != null) {
                zmin[i] = zminConst;
            } else if (zminCol != null && zminCol.get(orig) instanceof Number num) {
                zmin[i] = num.doubleValue();
            } else {
                zmin[i] = 0;
            }
        }

        var xPos = numericPositions(ext, df, aes.x(), rawX, rows.rows());
        var yPos = numericPositions(ext, df, aes.y(), rawY, rows.rows());

        double xSpacing = GridMetrics.resolution(xPos, false);
        double ySpacing = GridMetrics.resolution(yPos, false);
        double width = params.getDouble("width", 1.0);
        CubeFace[] faces = VolumeFaces.selectFaces(params.get("faces", "all"));

        var builder = StatData.builder();
        for (int i = 0; i < rows.rows().length; i++) {
            double cx = xPos[i];
            double cy = yPos[i];
            double zLow = Math.min(zmin[i], rows.z()[i]);
            double zHigh = Math.max(zmin[i], rows.z()[i]);
            double halfX = xSpacing * width / 2;
            double halfY = ySpacing * width / 2;
            var corners = VolumeFaces.columnCorners(cx - halfX, cx + halfX, cy - halfY, cy + halfY,
                    zLow, zHigh, faces);
            int orig = rows.rows()[i];
            double zRaw = rows.z()[i];
            VolumeFaces.emitFaces(builder, corners, faces, "col", "colId", i,
                    b -> {
                        b.add("zRaw", zRaw);
                        addPreserved(ext, df, aes, orig, b);
                    });
        }
        return builder.build();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "colId", "faceType", "zRaw");
    }
}
