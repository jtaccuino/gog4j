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
 * 2D Delaunay triangulation via Bowyer–Watson with a super-triangle, the Java
 * counterpart of {@code geometry::delaunayn(points, options = "Qt Qc Qz Qbb")}
 * used to tessellate a Delaunay point set into tiles.
 * <p>
 * The input is normalized to a unit-ish box before triangulation to avoid
 * numerical issues for large coordinate offsets, and the returned triangles are
 * re-indexed against the original point order. Every triangle is wound
 * counter-clockwise.
 */
public final class Delaunay2D {

    private Delaunay2D() {
    }

    /**
     * Triangulates a set of 2D points.
     *
     * @param x the x coordinates
     * @param y the y coordinates
     * @return the triangle vertex indices into the input arrays; each row has
     *         exactly three indices forming a CCW triangle
     * @throws IllegalArgumentException when fewer than three distinct points
     *                                  are supplied
     */
    public static int[][] triangulate(double[] x, double[] y) {
        int n = x.length;
        if (n < 3) {
            throw new IllegalArgumentException("Need at least 3 points for triangulation");
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) {
                throw new IllegalArgumentException("Points must be finite for Delaunay triangulation");
            }
            minX = Math.min(minX, x[i]);
            maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]);
            maxY = Math.max(maxY, y[i]);
        }
        double xScale = Math.max(maxX - minX, 1e-10);
        double yScale = Math.max(maxY - minY, 1e-10);

        // Super-triangle corners (normalized space). Axis-aligned, so
        // orientation is preserved through the mapping to original coords.
        List<double[]> points = new ArrayList<>(n + 3);
        points.add(new double[] {-1e9, -1e9});
        points.add(new double[] {1e9, -1e9});
        points.add(new double[] {0, 1e9});
        for (int i = 0; i < n; i++) {
            points.add(new double[] {(x[i] - minX) / xScale, (y[i] - minY) / yScale});
        }

        List<Triangle> triangles = new ArrayList<>();
        triangles.add(new Triangle(0, 1, 2));

        for (int i = 0; i < n; i++) {
            var p = points.get(i + 3);
            var badTriangles = triangles.stream()
                    .filter(t -> t.circumcircleContains(p, points))
                    .toList();

            var boundaryEdges = new ArrayList<int[]>();
            for (Triangle t : badTriangles) {
                for (var e : t.edges()) {
                    boolean shared = badTriangles.stream()
                            .anyMatch(b -> !b.equals(t) && b.hasEdge(e[0], e[1]));
                    if (!shared) {
                        boundaryEdges.add(e);
                    }
                }
            }

            triangles.removeAll(badTriangles);
            for (var e : boundaryEdges) {
                triangles.add(new Triangle(e[0], e[1], i + 3));
            }
        }

        // Keep only triangles free of super-triangle vertices.
        var result = new ArrayList<int[]>();
        for (Triangle t : triangles) {
            if (t.a() >= 3 && t.b() >= 3 && t.c() >= 3) {
                int a = t.a() - 3, b = t.b() - 3, c = t.c() - 3;
                double signed = (x[b] - x[a]) * (y[c] - y[a]) - (x[c] - x[a]) * (y[b] - y[a]);
                if (signed < 0) {
                    int tmp = b;
                    b = c;
                    c = tmp;
                }
                result.add(new int[] {a, b, c});
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Delaunay triangulation produced no triangles");
        }
        return result.toArray(int[][]::new);
    }

    /**
     * A triangle stored as vertex indices into the working point list.
     */
    private record Triangle(int a, int b, int c) {

        @SuppressWarnings("unused")
        private int[][] edges() {
            return new int[][] {{a, b}, {b, c}, {c, a}};
        }

        private boolean hasEdge(int p, int q) {
            return (a == p && b == q) || (a == q && b == p)
                    || (b == p && c == q) || (b == q && c == p)
                    || (c == p && a == q) || (c == q && a == p);
        }

        /**
         * Whether the circumcircle of this triangle contains {@code p}.
         */
        private boolean circumcircleContains(double[] p, List<double[]> points) {
            var a = points.get(this.a);
            var b = points.get(this.b);
            var c = points.get(this.c);

            double d = 2 * (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1]));
            if (d == 0) {
                return false;
            }
            double ux = ((a[0] * a[0] + a[1] * a[1]) * (b[1] - c[1])
                    + (b[0] * b[0] + b[1] * b[1]) * (c[1] - a[1])
                    + (c[0] * c[0] + c[1] * c[1]) * (a[1] - b[1])) / d;
            double uy = ((a[0] * a[0] + a[1] * a[1]) * (c[0] - b[0])
                    + (b[0] * b[0] + b[1] * b[1]) * (a[0] - c[0])
                    + (c[0] * c[0] + c[1] * c[1]) * (b[0] - a[0])) / d;
            double dx = p[0] - ux;
            double dy = p[1] - uy;
            double rx = a[0] - ux;
            double ry = a[1] - uy;
            return dx * dx + dy * dy <= rx * rx + ry * ry;
        }
    }
}
