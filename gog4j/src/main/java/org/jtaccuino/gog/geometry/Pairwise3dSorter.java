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
import java.util.Arrays;
import java.util.List;

/**
 * Pairwise depth sorter for 3D painting — the {@code "pairwise"} depth-sort
 * method.
 * <p>
 * Operates entirely in transformed space: every primitives supplies its
 * projected screen coordinates ({@code x}, {@code y}) and its camera depth
 * ({@code z}). For each pair of primitives a type-specific geometric test
 * decides which one occludes the other, the constraints are collected into a
 * directed graph, weak edges that cause cycles are broken, and the primitives
 * are returned in back-to-front painting order.
 * <p>
 * The type-specific comparisons mirror the R modules:
 * <ul>
 * <li>polygon–polygon: SAT overlap + depth interpolation at the intersection
 * centroid;</li>
 * <li>polygon–point: point-in-polygon + depth interpolation at the point;</li>
 * <li>polygon–segment: segment clip against the polygon + depth interpolation
 * at the clipped midpoint;</li>
 * <li>segment–segment: segment intersection + depth interpolation at the
 * crossing;</li>
 * <li>point–segment: bbox proximity + depth at the closest point on the
 * segment.</li>
 * </ul>
 * When a pair is (near-)coplanar, smaller primitives render on top
 * (point &gt; segment &gt; polygon).
 */
public final class Pairwise3dSorter {

    private Pairwise3dSorter() {
    }

    /**
     * A primitive in transformed space: projected screen coordinates and camera
     * depth (larger = farther from the viewer). Each kind carries its own
     * scalar or array representation, so points and segments hold no arrays.
     */
    public sealed interface Primitive permits PrimitivePolygon, PrimitiveSegment, PrimitivePoint {

        /**
         * The coplanar tie-break priority (higher renders on top): points (3)
         * above segments (2) above polygons (1).
         *
         * @return the tie-break priority
         */
        int priority();

        /**
         * The primitive's mean depth, used by the topological sort to order
         * primitives that share no pairwise constraint.
         *
         * @return the mean depth (larger = farther)
         */
        double meanDepth();
    }

    /**
     * A filled polygon ring in transformed space. Carries its vertex arrays and
     * caches their bounds, since the pairwise comparisons read them once per
     * primitive pair.
     */
    public static final class PrimitivePolygon implements Primitive {

        private final double[] x;
        private final double[] y;
        private final double[] depth;

        // Bounds of x/y/depth, computed once on construction. The pairwise
        // comparisons read polygon bounds on every pair, so re-scanning the
        // vertex arrays on each call would turn a sort into O(k^2 * n) work;
        // caching makes the per-pair tests O(1).
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;
        private final double minZ;
        private final double maxZ;

        /**
         * Constructs a polygon primitive and computes its vertex bounds.
         *
         * @param x     projected screen x per vertex
         * @param y     projected screen y per vertex
         * @param depth camera depth per vertex (larger = farther from the viewer)
         */
        public PrimitivePolygon(double[] x, double[] y, double[] depth) {
            this.x = x;
            this.y = y;
            this.depth = depth;
            double mnX = x[0], mxX = x[0];
            double mnY = y[0], mxY = y[0];
            double mnZ = depth[0], mxZ = depth[0];
            for (int i = 1; i < x.length; i++) {
                double v = x[i];
                if (v < mnX) {
                    mnX = v;
                } else if (v > mxX) {
                    mxX = v;
                }
                v = y[i];
                if (v < mnY) {
                    mnY = v;
                } else if (v > mxY) {
                    mxY = v;
                }
                v = depth[i];
                if (v < mnZ) {
                    mnZ = v;
                } else if (v > mxZ) {
                    mxZ = v;
                }
            }
            this.minX = mnX;
            this.maxX = mxX;
            this.minY = mnY;
            this.maxY = mxY;
            this.minZ = mnZ;
            this.maxZ = mxZ;
        }

        /** {@return the projected screen x per vertex} */
        public double[] x() {
            return x;
        }

        /** {@return the projected screen y per vertex} */
        public double[] y() {
            return y;
        }

        /** {@return the camera depth per vertex (larger = farther)} */
        public double[] depth() {
            return depth;
        }

        @Override
        public int priority() {
            return 1;
        }

        @Override
        public double meanDepth() {
            double s = 0;
            for (double d : depth) {
                s += d;
            }
            return depth.length == 0 ? 0 : s / depth.length;
        }

        private double minX() {
            return minX;
        }

        private double maxX() {
            return maxX;
        }

        private double minY() {
            return minY;
        }

        private double maxY() {
            return maxY;
        }

        private double minZ() {
            return minZ;
        }

        private double maxZ() {
            return maxZ;
        }

        private double[] beta() {
            if (depth.length < 3) {
                return null;
            }
            return PolygonMath.fitPlane(x, y, depth, depth.length);
        }

        /**
         * Creates a polygon primitive from <em>nearness</em> values (larger =
         * closer to the camera), converting them to the internal depth
         * convention (larger = farther).
         *
         * @param x         projected screen x per vertex
         * @param y         projected screen y per vertex
         * @param nearness  per-vertex nearness (larger = nearer)
         * @return a polygon primitive with depths where larger = farther
         */
        public static PrimitivePolygon fromNearness(double[] x, double[] y, double[] nearness) {
            double[] depth = new double[nearness.length];
            for (int i = 0; i < nearness.length; i++) {
                depth[i] = -nearness[i];
            }
            return new PrimitivePolygon(x, y, depth);
        }
    }

    /**
     * A two-vertex segment in transformed space, held as two scalar endpoints.
     *
     * @param x0     the segment's start x
     * @param y0     the segment's start y
     * @param depth0 the segment's start depth (larger = farther from the viewer)
     * @param x1     the segment's end x
     * @param y1     the segment's end y
     * @param depth1 the segment's end depth (larger = farther from the viewer)
     */
    public record PrimitiveSegment(double x0, double y0, double depth0,
                                   double x1, double y1, double depth1)
            implements Primitive {

        /**
         * Creates a segment primitive from <em>nearness</em> values (larger =
         * closer to the camera), converting them to the internal depth
         * convention (larger = farther).
         *
         * @param x0        the segment's start x
         * @param y0        the segment's start y
         * @param nearness0 the segment's start nearness (larger = nearer)
         * @param x1        the segment's end x
         * @param y1        the segment's end y
         * @param nearness1 the segment's end nearness (larger = nearer)
         * @return a segment primitive with depths where larger = farther
         */
        public static PrimitiveSegment fromNearness(double x0, double y0, double nearness0,
                                                    double x1, double y1, double nearness1) {
            return new PrimitiveSegment(x0, y0, -nearness0, x1, y1, -nearness1);
        }

        @Override
        public int priority() {
            return 2;
        }

        @Override
        public double meanDepth() {
            return (depth0 + depth1) / 2;
        }
    }

    /**
     * A single point in transformed space, held as three scalars.
     *
     * @param x     the point's x coordinate
     * @param y     the point's y coordinate
     * @param depth the point's depth (larger = farther from the viewer)
     */
    public record PrimitivePoint(double x, double y, double depth) implements Primitive {

        /**
         * Creates a point primitive from a <em>nearness</em> value (larger =
         * closer to the camera), converting it to the internal depth convention
         * (larger = farther).
         *
         * @param x        the point's x coordinate
         * @param y        the point's y coordinate
         * @param nearness the point's nearness (larger = nearer)
         * @return a point primitive with depth where larger = farther
         */
        public static PrimitivePoint fromNearness(double x, double y, double nearness) {
            return new PrimitivePoint(x, y, -nearness);
        }

        @Override
        public int priority() {
            return 3;
        }

        @Override
        public double meanDepth() {
            return depth;
        }
    }

    /**
     * Sorts the given primitives back-to-front.
     *
     * @param prims the primitives to order
     * @return the primitives' indices in painting order (farthest first)
     */
    public static int[] sort(List<Primitive> prims) {
        return renderOrder(prims);
    }

    private static int[] renderOrder(List<Primitive> prims) {
        int k = prims.size();
        double[][] delta = new double[k][k];
        for (int i = 0; i < k; i++) {
            Arrays.fill(delta[i], Double.NaN);
        }

        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                double dlt = compare(prims.get(i), prims.get(j));
                if (!Double.isNaN(dlt)) {
                    delta[i][j] = dlt;
                    delta[j][i] = -dlt;
                }
            }
        }

        boolean[][] adj = breakCycles(delta);
        double[] depths = new double[k];
        for (int i = 0; i < k; i++) {
            depths[i] = prims.get(i).meanDepth();
        }
        return topologicalSort(adj, depths);
    }

    /**
     * Compares two primitives and returns the depth delta of i minus j at a
     * shared sample point ({@code NaN} = no constraint). A positive delta means
     * i is farther away (behind) j and must be painted first.
     */
    private static double compare(Primitive di, Primitive dj) {
        double dlt = compareRaw(di, dj);
        if (!Double.isNaN(dlt) && Math.abs(dlt) < PolygonMath.COPLANAR_TOL) {
            int pi = di.priority();
            int pj = dj.priority();
            if (pi != pj) {
                // Smaller primitives render on top: i behind j when j is smaller.
                return pj - pi;
            }
        }
        return dlt;
    }

    private static double compareRaw(Primitive di, Primitive dj) {
        return switch (di) {
            case PrimitivePolygon pi -> switch (dj) {
                case PrimitivePolygon pj -> comparePolyPoly(pi, pj);
                case PrimitiveSegment sj -> comparePolySegment(pi, sj);
                case PrimitivePoint pt -> comparePolyPoint(pi, pt);
            };
            case PrimitiveSegment si -> switch (dj) {
                case PrimitivePolygon pj -> -comparePolySegment(pj, si);
                case PrimitiveSegment sj -> compareSegSeg(si, sj);
                case PrimitivePoint pt -> -comparePointSeg(pt, si);
            };
            case PrimitivePoint pi -> switch (dj) {
                case PrimitivePolygon pj -> -comparePolyPoint(pj, pi);
                case PrimitiveSegment sj -> comparePointSeg(pi, sj);
                case PrimitivePoint pj -> Double.NaN;
            };
        };
    }

    private static double comparePolyPoly(PrimitivePolygon di, PrimitivePolygon dj) {
        // Bounding box overlap check (projected xy).
        if (di.maxX() <= dj.minX() || di.minX() >= dj.maxX()
                || di.maxY() <= dj.minY() || di.minY() >= dj.maxY()) {
            return Double.NaN;
        }
        if (!PolygonMath.satOverlap(di.x(), di.y(), dj.x(), dj.y())) {
            return Double.NaN;
        }
        // xy overlap but no z overlap: averaged depth precedence.
        if (di.minZ() >= dj.maxZ() || dj.minZ() >= di.maxZ()) {
            return ((di.minZ() + di.maxZ()) / 2) - ((dj.minZ() + dj.maxZ()) / 2);
        }
        // Overlapping in all dimensions: sample depth at the intersection.
        double[] intC = PolygonMath.intersectionCentroid(di.x(), di.y(), dj.x(), dj.y());
        if (intC == null) {
            return Double.NaN;
        }
        double zi = PolygonMath.interpolateZ(di.x(), di.y(), di.depth(), di.beta(), intC);
        double zj = PolygonMath.interpolateZ(dj.x(), dj.y(), dj.depth(), dj.beta(), intC);
        return zi - zj;
    }

    private static double comparePolyPoint(PrimitivePolygon poly, PrimitivePoint point) {
        double px = point.x();
        double py = point.y();
        double pz = point.depth();
        if (px < poly.minX() || px > poly.maxX() || py < poly.minY() || py > poly.maxY()) {
            return Double.NaN;
        }
        if (!PolygonMath.pointInPolygon(px, py, poly.x(), poly.y())) {
            return Double.NaN;
        }
        double polyZ = PolygonMath.interpolateZ(poly.x(), poly.y(), poly.depth(), poly.beta(), new double[] {px, py});
        return polyZ - pz;
    }

    private static double comparePolySegment(PrimitivePolygon poly, PrimitiveSegment seg) {
        double sxMin = Math.min(seg.x0(), seg.x1()), sxMax = Math.max(seg.x0(), seg.x1());
        double syMin = Math.min(seg.y0(), seg.y1()), syMax = Math.max(seg.y0(), seg.y1());
        if (sxMin >= poly.maxX() || sxMax <= poly.minX()
                || syMin >= poly.maxY() || syMax <= poly.minY()) {
            return Double.NaN;
        }
        // Clip the segment against the polygon to find the overlap region.
        double[] sample = segmentPolygonClipMidpoint(seg, poly);
        if (sample == null) {
            return Double.NaN;
        }
        double polyZ = PolygonMath.interpolateZ(poly.x(), poly.y(), poly.depth(), poly.beta(), sample);
        double segZ = PolygonMath.interpolateZOnSegment(
                seg.x0(), seg.y0(), seg.depth0(), seg.x1(), seg.y1(), seg.depth1(),
                sample[0], sample[1]);
        return polyZ - segZ;
    }

    /**
     * Clips a segment against a polygon and returns the mean coordinate of the
     * inside portions ({@code null} when the segment never enters the polygon).
     */
    private static double[] segmentPolygonClipMidpoint(PrimitiveSegment seg, PrimitivePolygon poly) {
        double sx0 = seg.x0(), sy0 = seg.y0();
        double sx1 = seg.x1(), sy1 = seg.y1();
        double dx = sx1 - sx0, dy = sy1 - sy0;
        double lenSq = dx * dx + dy * dy;
        List<double[]> insidePoints = new ArrayList<>();

        boolean endIn = PolygonMath.pointInPolygon(sx0, sy0, poly.x(), poly.y());
        if (endIn) {
            insidePoints.add(new double[] {sx0, sy0});
        }
        if (lenSq < 1e-20) {
            return insidePoints.isEmpty() ? null : insidePoints.get(0);
        }

        List<double[]> crossings = new ArrayList<>();
        int n = poly.x().length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double[] p = PolygonMath.segmentIntersection(
                    sx0, sy0, sx1, sy1,
                    poly.x()[i], poly.y()[i], poly.x()[j], poly.y()[j]);
            if (p != null) {
                crossings.add(p);
            }
        }

        // The portion of the segment inside a simple polygon is a union of
        // intervals bracketed by the crossings (plus the endpoints when they
        // are inside). Sample the midpoint of each inside interval.
        if (endIn && !crossings.isEmpty()) {
            double[] c = crossings.get(0);
            insidePoints.add(new double[] {(sx0 + c[0]) / 2, (sy0 + c[1]) / 2});
        }
        for (int i = 0; i + 1 < crossings.size(); i += 2) {
            double[] a = crossings.get(i);
            double[] b = crossings.get(i + 1);
            double mx = (a[0] + b[0]) / 2;
            double my = (a[1] + b[1]) / 2;
            if (PolygonMath.pointInPolygon(mx, my, poly.x(), poly.y())) {
                insidePoints.add(new double[] {mx, my});
            }
        }
        if (!endIn && !crossings.isEmpty()) {
            double[] last = crossings.get(crossings.size() - 1);
            insidePoints.add(new double[] {(sx1 + last[0]) / 2, (sy1 + last[1]) / 2});
        }
        if (insidePoints.isEmpty()) {
            return null;
        }
        double sxSum = 0, sySum = 0;
        for (var p : insidePoints) {
            sxSum += p[0];
            sySum += p[1];
        }
        return new double[] {sxSum / insidePoints.size(), sySum / insidePoints.size()};
    }

    private static double compareSegSeg(PrimitiveSegment si, PrimitiveSegment sj) {
        double[] p = PolygonMath.segmentIntersection(
                si.x0(), si.y0(), si.x1(), si.y1(),
                sj.x0(), sj.y0(), sj.x1(), sj.y1());
        if (p == null) {
            return Double.NaN;
        }
        double zi = PolygonMath.interpolateZOnSegment(
                si.x0(), si.y0(), si.depth0(), si.x1(), si.y1(), si.depth1(), p[0], p[1]);
        double zj = PolygonMath.interpolateZOnSegment(
                sj.x0(), sj.y0(), sj.depth0(), sj.x1(), sj.y1(), sj.depth1(), p[0], p[1]);
        return zi - zj;
    }

    private static double comparePointSeg(PrimitivePoint point, PrimitiveSegment seg) {
        double px = point.x();
        double py = point.y();
        double pz = point.depth();
        double sxMin = Math.min(seg.x0(), seg.x1()), sxMax = Math.max(seg.x0(), seg.x1());
        double syMin = Math.min(seg.y0(), seg.y1()), syMax = Math.max(seg.y0(), seg.y1());
        if (px < sxMin || px > sxMax || py < syMin || py > syMax) {
            return Double.NaN;
        }
        double[] closest = PolygonMath.closestPointOnSegment(px, py, seg.x0(), seg.y0(), seg.x1(), seg.y1());
        double segZ = PolygonMath.interpolateZOnSegment(
                seg.x0(), seg.y0(), seg.depth0(), seg.x1(), seg.y1(), seg.depth1(),
                closest[0], closest[1]);
        return segZ - pz;
    }

    /**
     * Removes the weakest constraints until the graph is acyclic (mirrors
     * {@code break_cycles}): repeatedly checks for cycles and clears the
     * constraint pair with the smallest absolute delta.
     */
    private static boolean[][] breakCycles(double[][] delta) {
        int n = delta.length;
        double[][] d = new double[n][];
        for (int i = 0; i < n; i++) {
            d[i] = delta[i].clone();
        }
        int maxIterations = n * n;
        for (int iter = 0; iter < maxIterations; iter++) {
            boolean[][] adj = dgt(d);
            if (isAcyclic(adj)) {
                return adj;
            }
            double best = Double.MAX_VALUE;
            int bi = -1, bj = -1;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (!Double.isNaN(d[i][j]) && Math.abs(d[i][j]) < best) {
                        best = Math.abs(d[i][j]);
                        bi = i;
                        bj = j;
                    }
                }
            }
            if (bi < 0 || !Double.isFinite(best)) {
                break;
            }
            d[bi][bj] = Double.NaN;
            d[bj][bi] = Double.NaN;
        }
        return dgt(d);
    }

    private static boolean[][] dgt(double[][] delta) {
        int n = delta.length;
        boolean[][] adj = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                adj[i][j] = !Double.isNaN(delta[i][j]) && delta[i][j] > 0;
            }
        }
        return adj;
    }

    private static boolean isAcyclic(boolean[][] adj) {
        int n = adj.length;
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (adj[i][j]) {
                    inDegree[j]++;
                }
            }
        }
        boolean[] queued = new boolean[n];
        int[] queue = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue[tail++] = i;
                queued[i] = true;
            }
        }
        int processed = 0;
        while (head < tail) {
            int cur = queue[head++];
            processed++;
            for (int j = 0; j < n; j++) {
                if (adj[cur][j] && !queued[j]) {
                    inDegree[j]--;
                    if (inDegree[j] == 0) {
                        queue[tail++] = j;
                        queued[j] = true;
                    }
                }
            }
        }
        return processed == n;
    }

    /**
     * Topological sort with depth-ordered queues (farthest first), mirroring
     * {@code topological_sort} in the R reference. Frees back-to-front edges.
     */
    private static int[] topologicalSort(boolean[][] adj, double[] depths) {
        int n = adj.length;
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (adj[i][j]) {
                    inDegree[j]++;
                }
            }
        }
        var result = new ArrayList<Integer>(n);
        var queue = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
            }
        }
        sortByDepthDesc(queue, depths);

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            result.add(current);
            var newFree = new ArrayList<Integer>();
            for (int j = 0; j < n; j++) {
                if (adj[current][j]) {
                    inDegree[j]--;
                    if (inDegree[j] == 0) {
                        newFree.add(j);
                    }
                }
            }
            sortByDepthDesc(newFree, depths);
            queue.addAll(newFree);
            sortByDepthDesc(queue, depths);
        }

        if (result.size() < n) {
            for (int i = 0; i < n; i++) {
                if (!result.contains(i)) {
                    result.add(i);
                }
            }
        }
        int[] out = new int[result.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = result.get(i);
        }
        return out;
    }

    private static void sortByDepthDesc(List<Integer> idx, double[] depths) {
        idx.sort((a, b) -> Double.compare(depths[b], depths[a]));
    }
}
