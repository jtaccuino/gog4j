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
 * {@code Stats.voxel3d()} — turns sparse 3-D points into fixed-size cubes
 * centered on each coordinate. Voxel
 * footprints follow the per-axis grid spacing estimated by
 * {@link GridMetrics#resolution}, scaled by {@code width}; faces are emitted
 * with the hierarchical group {@code "voxel{i}__{face}"} and the
 * {@code voxel_id}/{@code face_type} computed variables.
 *
 * @param <DF> the DataFrame type
 */
public class StatVoxel3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a {@code Stats.voxel3d()} transformation.
     */
    public StatVoxel3d() {
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

        var rows = numericRows(rawX, rawY, rawZ, dropMissing);
        if (rows == null || rows.rows().length < 1) {
            return StatData.builder().build();
        }

        double[] xPos = numericPositions(ext, df, aes.x(), rawX, rows.rows());
        double[] yPos = numericPositions(ext, df, aes.y(), rawY, rows.rows());
        double[] zPos = numericPositions(ext, df, aes.z(), rawZ, rows.rows());

        double xSpacing = GridMetrics.resolution(xPos, false);
        double ySpacing = GridMetrics.resolution(yPos, false);
        double zSpacing = GridMetrics.resolution(zPos, false);
        double width = params.getDouble("width", 1.0);
        CubeFace[] faces = VolumeFaces.selectFaces(params.get("faces", "all"));

        var builder = StatData.builder();
        for (int i = 0; i < rows.rows().length; i++) {
            var corners = VolumeFaces.voxelCorners(xPos[i], yPos[i], zPos[i],
                    xSpacing * width / 2, ySpacing * width / 2, zSpacing * width / 2, faces);
            int orig = rows.rows()[i];
            double zRaw = rows.z()[i];
            VolumeFaces.emitFaces(builder, corners, faces, "voxel", "voxelId", i,
                    b -> {
                        b.add("zRaw", zRaw);
                        addPreserved(ext, df, aes, orig, b);
                    });
        }
        return builder.build();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "voxelId", "faceType", "zRaw");
    }
}
