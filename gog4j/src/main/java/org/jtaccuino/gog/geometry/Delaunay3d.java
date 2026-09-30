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
import java.util.List;

/**
 * A 3-D Delaunay tetrahedralization via Bowyer–Watson insertion, mirroring the
 * in-repo geometry utilities the plan locks in (option 3a). It triangulates a
 * point cloud into tetrahedra whose circumspheres contain no other point.
 * <p>
 * The implementation inserts points one at a time into a triangulation that is
 * initialized with a large enclosing super-tetrahedron; every tetrahedron
 * whose circumsphere contains the new point is removed and re-tessellated
 * around the cavity's boundary. Tetrahedra that touch a super-tetrahedron
 * vertex are discarded at the end. The convex hull of the point set falls out
 * as the boundary of this triangulation, and the alpha complex is obtained by
 * filtering tetrahedra on circumradius (see {@link Hull3d#alphaHull(double[][], double)}).
 */
public final class Delaunay3d {

    /** One tetrahedron, its four vertex indices, and its circumsphere. */
    public static final class Tetra {
        final int a, b, c, d;
        final double cx, cy, cz;
        final double radius;

        Tetra(int a, int b, int c, int d, double cx, double cy, double cz, double radius) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.radius = radius;
        }
    }

    private Delaunay3d() {
    }

    /**
     * Triangulates the given points.
     *
     * @param pts vertex coordinates as an n×3 array
     * @return the tetrahedra over the input vertices, in insertion order
     */
    public static List<Tetra> tetrahedralize(double[][] pts) {
        int n = pts.length;
        if (n < 4) {
            return List.of();
        }

        // Enclosing super-tetrahedron: a large box around the data extent.
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (double[] p : pts) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            minZ = Math.min(minZ, p[2]);
            maxZ = Math.max(maxZ, p[2]);
        }
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;
        double cz = (minZ + maxZ) / 2;
        double span = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        double d = Math.max(span, 1e-9) * 2;
        int superA = n, superB = n + 1, superC = n + 2, superD = n + 3;
        // A regular tetrahedron centered on the data centroid, far outside it.
        double[][] superVerts = {
                {cx + d, cy - d, cz - d},
                {cx - d, cy + d, cz - d},
                {cx + d, cy + d, cz + d},
                {cx - d, cy - d, cz + d}
        };
        double[][] all = new double[n + 4][];
        System.arraycopy(pts, 0, all, 0, n);
        System.arraycopy(superVerts, 0, all, n, 4);

        var tets = new ArrayList<Tetra>();
        tets.add(circumsphere(superA, superB, superC, superD, all));
        for (int i = 0; i < n; i++) {
            insert(all, i, tets);
        }

        // Drop tetrahedra touching the super-tetrahedron.
        var out = new ArrayList<Tetra>(tets.size());
        for (var t : tets) {
            if (t.a < n && t.b < n && t.c < n && t.d < n) {
                out.add(t);
            }
        }
        return out;
    }

    private static void insert(double[][] all, int pi, List<Tetra> tets) {
        var bad = new ArrayList<Tetra>();
        for (var t : tets) {
            double dx = all[pi][0] - t.cx;
            double dy = all[pi][1] - t.cy;
            double dz = all[pi][2] - t.cz;
            double dist2 = dx * dx + dy * dy + dz * dz;
            double r = t.radius;
            if (dist2 <= r * r + 1e-9) {
                bad.add(t);
            }
        }
        if (bad.isEmpty()) {
            return;
        }
        tets.removeAll(bad);

        // Boundary faces of the union of removed tetrahedra.
        var boundary = new ArrayList<int[]>();
        for (var t : bad) {
            addFace(boundary, t.a, t.b, t.c, t.d);
        }
        for (int[] f : boundary) {
            tets.add(circumsphere(f[0], f[1], f[2], pi, all));
        }
    }

    /**
     * Adds the four faces of a tetrahedron to the cavity boundary: a face
     * shared by two removed tetrahedra cancels out; a face on the cavity
     * surface survives exactly once.
     */
    private static void addFace(List<int[]> boundary, int a, int b, int c, int d) {
        addFace(boundary, new int[] {a, b, c});
        addFace(boundary, new int[] {a, b, d});
        addFace(boundary, new int[] {a, c, d});
        addFace(boundary, new int[] {b, c, d});
    }

    private static void addFace(List<int[]> boundary, int[] face) {
        // Canonical orientation (smallest index first) for matching.
        if (face[0] > face[1]) {
            int t = face[0];
            face[0] = face[1];
            face[1] = t;
        }
        if (face[1] > face[2]) {
            int t = face[1];
            face[1] = face[2];
            face[2] = t;
        }
        if (face[0] > face[1]) {
            int t = face[0];
            face[0] = face[1];
            face[1] = t;
        }
        for (int i = 0; i < boundary.size(); i++) {
            var f = boundary.get(i);
            if (f[0] == face[0] && f[1] == face[1] && f[2] == face[2]) {
                boundary.remove(i);
                return;
            }
        }
        boundary.add(face);
    }

    private static Tetra circumsphere(int a, int b, int c, int d, double[][] p) {
        // Circumsphere of the tetrahedron via the linear system on squared
        // distances, with the center shifted to vertex a.
        double[][] m = {
                {2 * (p[b][0] - p[a][0]), 2 * (p[b][1] - p[a][1]), 2 * (p[b][2] - p[a][2])},
                {2 * (p[c][0] - p[a][0]), 2 * (p[c][1] - p[a][1]), 2 * (p[c][2] - p[a][2])},
                {2 * (p[d][0] - p[a][0]), 2 * (p[d][1] - p[a][1]), 2 * (p[d][2] - p[a][2])}
        };
        double[] rhs = {
                sq(p[a], p[b]),
                sq(p[a], p[c]),
                sq(p[a], p[d])
        };
        double[] center = solve3(m, rhs);
        if (center == null) {
            // Degenerate; give an empty sphere so the tetra is never used.
            return new Tetra(a, b, c, d, p[a][0], p[a][1], p[a][2], Double.POSITIVE_INFINITY);
        }
        double cx = p[a][0] + center[0];
        double cy = p[a][1] + center[1];
        double cz = p[a][2] + center[2];
        double r = Math.sqrt(sq(p[a], new double[] {cx, cy, cz}));
        return new Tetra(a, b, c, d, cx, cy, cz, r);
    }

    private static double sq(double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        return dx * dx + dy * dy + dz * dz;
    }

    private static double[] solve3(double[][] m, double[] rhs) {
        double det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
        if (Math.abs(det) < 1e-18) {
            return null;
        }
        double inv = 1.0 / det;
        return new double[] {
                (rhs[0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                        - m[0][1] * (rhs[1] * m[2][2] - m[1][2] * rhs[2])
                        + m[0][2] * (rhs[1] * m[2][1] - m[1][1] * rhs[2])) * inv,
                (m[0][0] * (rhs[1] * m[2][2] - m[1][2] * rhs[2])
                        - rhs[0] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                        + m[0][2] * (m[1][0] * rhs[2] - rhs[1] * m[2][0])) * inv,
                (m[0][0] * (m[1][1] * rhs[2] - rhs[1] * m[2][1])
                        - m[0][1] * (m[1][0] * rhs[2] - rhs[1] * m[2][0])
                        + rhs[0] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])) * inv
        };
    }
}
