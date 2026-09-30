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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.jtaccuino.gog.coord.CubeFace;

/**
 * Shared helpers for the volume statistics ({@code Stats.col3d()},
 * {@code Stats.bar3d()}, {@code Stats.voxel3d()}): resolving the
 * {@code faces} parameter and the box corner tables of columns and voxels.
 */
final class VolumeFaces {

    private VolumeFaces() {
    }

    /**
     * Resolves the {@code faces} parameter: {@code "all"} selects every face,
     * {@code "none"} selects none, and a list filters by face.
     *
     * @param faces the faces parameter value (a {@link CubeFace}, a
     *              {@code CubeFace[]}, or the legacy strings {@code "all"} /
     *              {@code "none"})
     * @return the selected cube faces
     * @throws IllegalArgumentException when an invalid face is given
     */
    static CubeFace[] selectFaces(Object faces) {
        if (faces instanceof CubeFace f) {
            return new CubeFace[] {f};
        }
        if (faces instanceof CubeFace[] arr) {
            if (arr.length == 0) {
                return CubeFace.values();
            }
            return arr.clone();
        }
        if (faces instanceof String s) {
            if ("all".equals(s)) {
                return CubeFace.values();
            }
            if ("none".equals(s)) {
                return new CubeFace[0];
            }
            throw new IllegalArgumentException(
                    "Invalid faces value: " + s + ". Use a CubeFace or CubeFace[].");
        }
        if (faces instanceof String[] arr) {
            if (arr.length == 1 && "all".equals(arr[0])) {
                return CubeFace.values();
            }
            if (arr.length == 1 && "none".equals(arr[0])) {
                return new CubeFace[0];
            }
            var out = new ArrayList<CubeFace>(arr.length);
            for (String name : arr) {
                out.add(CubeFace.valueOf(name.toUpperCase(Locale.ROOT)));
            }
            return out.toArray(CubeFace[]::new);
        }
        if (faces == null) {
            return CubeFace.values();
        }
        return new CubeFace[0];
    }

    /** The lowercase name of a cube face. */
    static String faceName(CubeFace face) {
        return face.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Emits one box primitive's vertices into a stat output: every vertex gets
     * its {@code x}/{@code y}/{@code z} position, the hierarchical group
     * {@code "{prefix}{i}__{face}"}, the primitive id ({@code idVar}), and the
     * {@code face_type} of the face it belongs to. The per-vertex callback is
     * invoked once per vertex (same face) so a stat can append its computed
     * variables and preserved aesthetics.
     *
     * @param b        the stat output builder
     * @param corners  the corner table from {@link #columnCorners} or
     *                 {@link #voxelCorners}
     * @param faces    the cube faces the corners were built for
     * @param idPrefix the group id prefix ({@code "col"}/{@code "voxel"})
     * @param idVar    the computed id variable name ({@code "colId"}/
     *                 {@code "voxelId"})
     * @param idValue  the 0-based primitive number
     * @param vertex   per-vertex extra columns, or {@code null}
     */
    static void emitFaces(StatData.Builder b, List<double[]> corners, CubeFace[] faces,
            String idPrefix, String idVar, int idValue, Consumer<StatData.Builder> vertex) {
        String baseGroup = idPrefix + (idValue + 1);
        int faceCount = corners.size() / 4;
        for (int f = 0; f < faceCount; f++) {
            CubeFace face = faces[f % faces.length];
            String group = baseGroup + "__" + faceName(face);
            for (int v = 0; v < 4; v++) {
                double[] corner = corners.get(f * 4 + v);
                b.add("x", corner[0]);
                b.add("y", corner[1]);
                b.add("z", corner[2]);
                b.add("group", group);
                b.add(idVar, idValue + 1);
                b.add("faceType", faceName(face));
                if (vertex != null) {
                    vertex.accept(b);
                }
            }
        }
    }

    /**
     * The 4×4 corner table of a box column: one
     * corner triple per column per face, laid out so each face is a quad.
     *
     * @param xLow   the low x bound of the column
     * @param xHigh  the high x bound
     * @param yLow   the low y bound
     * @param yHigh  the high y bound
     * @param zLow   the low z bound
     * @param zHigh  the high z bound
     * @param faces  the selected cube faces
     * @return one {@code {x, y, z}} triple per face vertex
     */
    @SuppressWarnings("EnumOrdinal")
    static List<double[]> columnCorners(double xLow, double xHigh, double yLow, double yHigh,
                                        double zLow, double zHigh, CubeFace[] faces) {
        // Corner order, from the min corner up:
        // 1 xmin,ymin,zmin  2 xmax,ymin,zmin  3 xmax,ymax,zmin  4 xmin,ymax,zmin
        // 5 xmin,ymin,zmax  6 xmax,ymin,zmax  7 xmax,ymax,zmax  8 xmin,ymax,zmax
        double[][] corners = {
                {xLow, yLow, zLow}, {xHigh, yLow, zLow}, {xHigh, yHigh, zLow}, {xLow, yHigh, zLow},
                {xLow, yLow, zHigh}, {xHigh, yLow, zHigh}, {xHigh, yHigh, zHigh}, {xLow, yHigh, zHigh}
        };
        int[][] defs = {
                {1, 5, 8, 4}, // xmin
                {2, 3, 7, 6}, // xmax
                {1, 2, 6, 5}, // ymin
                {4, 8, 7, 3}, // ymax
                {1, 4, 3, 2}, // zmin
                {5, 6, 7, 8}  // zmax
        };
        var out = new ArrayList<double[]>(faces.length * 4);
        for (CubeFace face : faces) {
            for (int k : defs[face.ordinal()]) {
                out.add(corners[k - 1]);
            }
        }
        return out;
    }

    /**
     * The corner table of a box voxel.
     *
     * @param cx    voxel center x
     * @param cy    voxel center y
     * @param cz    voxel center z
     * @param halfX half width in x
     * @param halfY half width in y
     * @param halfZ half width in z
     * @param faces the selected cube faces
     * @return one {@code {x, y, z}} triple per face vertex
     */
    @SuppressWarnings("EnumOrdinal")
    static List<double[]> voxelCorners(double cx, double cy, double cz,
                                       double halfX, double halfY, double halfZ, CubeFace[] faces) {
        double l = cx - halfX, r = cx + halfX;
        double f = cy - halfY, k = cy + halfY;
        double b = cz - halfZ, t = cz + halfZ;
        double[][] corners = {
                {l, k, b}, {r, k, b}, {r, f, b}, {l, f, b},
                {l, k, t}, {r, k, t}, {r, f, t}, {l, f, t}
        };
        int[][] defs = {
                {1, 4, 8, 5}, // xmin
                {3, 2, 6, 7}, // xmax
                {1, 5, 6, 2}, // ymin
                {4, 3, 7, 8}, // ymax
                {1, 2, 3, 4}, // zmin
                {5, 8, 7, 6}  // zmax
        };
        var out = new ArrayList<double[]>(faces.length * 4);
        for (CubeFace face : faces) {
            for (int v : defs[face.ordinal()]) {
                out.add(corners[v - 1]);
            }
        }
        return out;
    }
}
