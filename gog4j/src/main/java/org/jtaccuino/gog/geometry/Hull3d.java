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
package org.jtaccuino.gog.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3-D surface hulls for the hull statistic ({@code Stats.hull3d()}).
 * <p>
 * Both methods build on the in-repo {@link Delaunay3d} tetrahedralization:
 * <ul>
 * <li>{@link #convexHull(double[][])} returns the boundary triangles of the
 * full triangulation — the convex hull (star-shaped about its centroid, so the
 * centroid orientation test is exact);</li>
 * <li>{@link #alphaHull(double[][], double)} keeps only the tetrahedra whose
 * circumradius is at most {@code alpha} and returns the triangles on the
 * boundary of that alpha complex, oriented outward from a per-face interior
 * reference point (the opposite vertex of the adjacent interior tetrahedron),
 * falling back to the centroid.</li>
 * </ul>
 * Triangles are returned with outward-facing winding, ready for back-face
 * culling by winding.
 */
public final class Hull3d {

    private Hull3d() {
    }

    /**
     * The convex hull of the points as oriented triangle index triples.
     *
     * @param pts vertex coordinates as an n×3 array
     * @return the hull triangles, or an empty array when the set is degenerate
     */
    public static int[][] convexHull(double[][] pts) {
        var tets = Delaunay3d.tetrahedralize(pts);
        if (tets.isEmpty()) {
            return new int[0][];
        }
        int[][] faces = boundaryFaces(tets);
        return orientFaces(pts, faces, null);
    }

    /**
     * The alpha shape of the points as oriented triangle index triples.
     *
     * @param pts    vertex coordinates as an n×3 array
     * @param radius the alpha radius in data units
     * @return the alpha-shape triangles, or an empty array when no tetrahedron
     *         survives the radius filter
     */
    public static int[][] alphaHull(double[][] pts, double radius) {
        var tets = Delaunay3d.tetrahedralize(pts);
        if (tets.isEmpty()) {
            return new int[0][];
        }
        var kept = new ArrayList<Delaunay3d.Tetra>(tets.size());
        for (var t : tets) {
            if (t.radius <= radius) {
                kept.add(t);
            }
        }
        if (kept.isEmpty()) {
            return new int[0][];
        }
        // Per-face interior reference: the vertex opposite the face in the
        // adjacent kept tetrahedron, which lies on the interior side.
        Map<Face, Integer> refByFace = new HashMap<>();
        for (var t : kept) {
            refByFace.putIfAbsent(new Face(t.a, t.b, t.c), t.d);
            refByFace.putIfAbsent(new Face(t.a, t.b, t.d), t.c);
            refByFace.putIfAbsent(new Face(t.a, t.c, t.d), t.b);
            refByFace.putIfAbsent(new Face(t.b, t.c, t.d), t.a);
        }
        int[][] faces = boundaryFaces(kept);
        int[] reference = new int[faces.length];
        for (int i = 0; i < faces.length; i++) {
            reference[i] = refByFace.getOrDefault(new Face(faces[i][0], faces[i][1], faces[i][2]), -1);
        }
        return orientFaces(pts, faces, reference);
    }

    /**
     * The faces shared by exactly one tetrahedron of the given set — the
     * boundary of the union.
     */
    private static int[][] boundaryFaces(List<Delaunay3d.Tetra> tets) {
        var out = new ArrayList<int[]>();
        var counts = new HashMap<Face, int[]>();
        for (var t : tets) {
            addFace(counts, out, t.a, t.b, t.c);
            addFace(counts, out, t.a, t.b, t.d);
            addFace(counts, out, t.a, t.c, t.d);
            addFace(counts, out, t.b, t.c, t.d);
        }
        var result = new ArrayList<int[]>(out.size());
        for (var e : out) {
            result.add(e.clone());
        }
        return result.toArray(new int[0][]);
    }

    /** Records a face; a second occurrence cancels the first (shared face). */
    private static void addFace(Map<Face, int[]> counts, List<int[]> out, int a, int b, int c) {
        var face = new int[] {a, b, c};
        var key = new Face(a, b, c);
        var prior = counts.putIfAbsent(key, face);
        if (prior == null) {
            out.add(face);
        } else {
            out.remove(prior);
        }
    }

    /**
     * Corrects the winding of every triangle so its normal points outward: the
     * normal is flipped when
     * it points toward the interior reference point (or the centroid when no
     * per-face reference is available).
     *
     * @param pts       vertex coordinates
     * @param tri       the triangle index triples to orient (mutated)
     * @param reference per-face interior vertex index, or {@code null} to use
     *                  the centroid for every face
     * @return the oriented triangles (same array)
     */
    private static int[][] orientFaces(double[][] pts, int[][] tri, int[] reference) {
        double cx = 0, cy = 0, cz = 0;
        for (double[] p : pts) {
            cx += p[0];
            cy += p[1];
            cz += p[2];
        }
        int n = pts.length;
        cx /= n;
        cy /= n;
        cz /= n;
        for (int i = 0; i < tri.length; i++) {
            int a = tri[i][0], b = tri[i][1], c = tri[i][2];
            double ax = pts[a][0], ay = pts[a][1], az = pts[a][2];
            double bx = pts[b][0], by = pts[b][1], bz = pts[b][2];
            double cx2 = pts[c][0], cy2 = pts[c][1], cz2 = pts[c][2];
            double e1x = bx - ax, e1y = by - ay, e1z = bz - az;
            double e2x = cx2 - ax, e2y = cy2 - ay, e2z = cz2 - az;
            double nx = e1y * e2z - e1z * e2y;
            double ny = e1z * e2x - e1x * e2z;
            double nz = e1x * e2y - e1y * e2x;
            double fx = (ax + bx + cx2) / 3;
            double fy = (ay + by + cy2) / 3;
            double fz = (az + bz + cz2) / 3;
            double ox, oy, oz;
            if (reference != null && reference[i] >= 0) {
                int r = reference[i];
                ox = fx - pts[r][0];
                oy = fy - pts[r][1];
                oz = fz - pts[r][2];
            } else {
                ox = fx - cx;
                oy = fy - cy;
                oz = fz - cz;
            }
            if (nx * ox + ny * oy + nz * oz < 0) {
                int t = tri[i][1];
                tri[i][1] = tri[i][2];
                tri[i][2] = t;
            }
        }
        return tri;
    }

    /** Canonical (sorted) face key for matching shared faces. */
    private record Face(int a, int b, int c) {
        Face {
            if (a > b) {
                int t = a;
                a = b;
                b = t;
            }
            if (b > c) {
                int t = b;
                b = c;
                c = t;
            }
            if (a > b) {
                int t = a;
                a = b;
                b = t;
            }
        }
    }
}
