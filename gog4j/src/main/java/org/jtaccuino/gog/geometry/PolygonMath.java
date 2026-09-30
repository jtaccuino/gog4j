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

/**
 * Shared 2D polygon/segment math used by the pairwise 3D depth sorter —
 * axis-aligned rectangle overlap, point-in-polygon containment, segment
 * depth interpolation, segment intersection, closest point on a segment and
 * intersection centroid — together with the convex-hull and
 * polygon-clipping behavior consumed by the surface geoms.
 */
public final class PolygonMath {

    /** Numerical tolerance for coplanar tie-breaking. */
    public static final double COPLANAR_TOL = 1e-8;

    private PolygonMath() {
    }

    /**
     * Convex hull of a point set (2D, Andrew's monotone chain), matching
     * {@code grDevices::chull()}. Returns hull vertex indices into the input
     * arrays in winding order.
     *
     * @param xs the point x coordinates
     * @param ys the point y coordinates
     * @return the hull vertex indices, or {@code null} when degenerate
     */
    public static int[] convexHull(double[] xs, double[] ys) {
        int n = xs.length;
        if (n <= 2) {
            return null;
        }
        var idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, (a, b) -> {
            int c = Double.compare(xs[a], xs[b]);
            if (c != 0) {
                return c;
            }
            return Double.compare(ys[a], ys[b]);
        });

        int[] hull = new int[2 * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int p = idx[i];
            while (k >= 2 && cross(xs, ys, hull[k - 2], hull[k - 1], p) <= 0) {
                k--;
            }
            hull[k++] = p;
        }
        int lower = k + 1;
        for (int i = n - 2; i >= 0; i--) {
            int p = idx[i];
            while (k >= lower && cross(xs, ys, hull[k - 2], hull[k - 1], p) <= 0) {
                k--;
            }
            hull[k++] = p;
        }
        if (k <= 3) {
            return Arrays.copyOf(hull, k <= 3 ? n : k);
        }
        return Arrays.copyOf(hull, k - 1);
    }

    /**
     * Clips a subject polygon against a convex clip polygon (Sutherland–Hodgman),
     * matching the {@code polyclip::polyclip(A, B, op="intersection")} behavior
     * used when the clip region is convex (e.g. the convex hull of data points).
     *
     * @param subX subject polygon x
     * @param subY subject polygon y
     * @param clipX convex clip polygon x (must be simple and convex)
     * @param clipY convex clip polygon y
     * @return the intersection polygon vertices, or {@code null} when empty
     */
    public static double[][] clipConvex(double[] subX, double[] subY,
                                        double[] clipX, double[] clipY) {
        double[] outX = subX.clone();
        double[] outY = subY.clone();
        int m = clipX.length;
        for (int edge = 0; edge < m && outX.length > 0; edge++) {
            int a = edge, b = (edge + 1) % m;
            double ex = clipX[b] - clipX[a];
            double ey = clipY[b] - clipY[a];
            var nextX = new ArrayList<Double>();
            var nextY = new ArrayList<Double>();
            double ax = outX[outX.length - 1], ay = outY[outY.length - 1];
            boolean aIn = inside(ax, ay, a, ex, ey, clipX, clipY);
            for (int i = 0; i < outX.length; i++) {
                double bx = outX[i], by = outY[i];
                boolean bIn = inside(bx, by, a, ex, ey, clipX, clipY);
                if (aIn != bIn) {
                    double[] ipt = intersect(ax, ay, bx, by, clipX[a], clipY[a], clipX[b], clipY[b]);
                    if (ipt != null) {
                        nextX.add(ipt[0]);
                        nextY.add(ipt[1]);
                    }
                }
                if (bIn) {
                    nextX.add(bx);
                    nextY.add(by);
                }
                ax = bx;
                ay = by;
                aIn = bIn;
            }
            outX = nextX.stream().mapToDouble(Double::doubleValue).toArray();
            outY = nextY.stream().mapToDouble(Double::doubleValue).toArray();
        }
        if (outX.length == 0) {
            return null;
        }
        var res = new double[outX.length][2];
        for (int i = 0; i < outX.length; i++) {
            res[i][0] = outX[i];
            res[i][1] = outY[i];
        }
        return res;
    }

    private static boolean inside(double px, double py, int a, double ex, double ey,
                              double[] clipX, double[] clipY) {
        return ex * (py - clipY[a]) - ey * (px - clipX[a]) >= 0;
    }

    /**
     * Ray-casting point-in-polygon test (mirrors the R implementation).
     *
     * @param px the query point x coordinate
     * @param py the query point y coordinate
     * @param polyX the polygon x coordinates
     * @param polyY the polygon y coordinates
     * @return {@code true} when the point lies inside the polygon
     */
    public static boolean pointInPolygon(double px, double py, double[] polyX, double[] polyY) {
        int n = polyX.length;
        boolean inside = false;
        int j = n - 1;
        for (int i = 0; i < n; i++) {
            if (((polyY[i] > py) != (polyY[j] > py)) && (px < (polyX[j] - polyX[i]) * (py - polyY[i])
                    / (polyY[j] - polyY[i]) + polyX[i])) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

    /**
     * Whether two segments intersect; returns the intersection point or
     * {@code null} when parallel/colinear or disjoint.
     *
     * @param x1 the first segment's start x
     * @param y1 the first segment's start y
     * @param x2 the first segment's end x
     * @param y2 the first segment's end y
     * @param x3 the second segment's start x
     * @param y3 the second segment's start y
     * @param x4 the second segment's end x
     * @param y4 the second segment's end y
     * @return the intersection point {@code {x, y}}, or {@code null}
     */
    public static double[] segmentIntersection(double x1, double y1, double x2, double y2,
                                               double x3, double y3, double x4, double y4) {
        double dx1 = x2 - x1, dy1 = y2 - y1;
        double dx2 = x4 - x3, dy2 = y4 - y3;
        double denom = dx1 * dy2 - dy1 * dx2;
        if (Math.abs(denom) < 1e-12) {
            return null;
        }
        double t = ((x3 - x1) * dy2 - (y3 - y1) * dx2) / denom;
        double u = ((x3 - x1) * dy1 - (y3 - y1) * dx1) / denom;
        if (t < 0 || t > 1 || u < 0 || u > 1) {
            return null;
        }
        return new double[] {x1 + t * dx1, y1 + t * dy1};
    }

    /**
     * Closest point on a segment to a given point, clamped to the endpoints.
     *
     * @param px the query point x coordinate
     * @param py the query point y coordinate
     * @param x1 the segment's start x
     * @param y1 the segment's start y
     * @param x2 the segment's end x
     * @param y2 the segment's end y
     * @return the closest point on the segment as {@code {x, y}}
     */
    public static double[] closestPointOnSegment(double px, double py,
                                                 double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-20) {
            return new double[] {x1, y1};
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        return new double[] {x1 + t * dx, y1 + t * dy};
    }

    /**
     * Squared distance from a point to a segment, clamped to the endpoints.
     *
     * @param px the query point x coordinate
     * @param py the query point y coordinate
     * @param x1 the segment's start x
     * @param y1 the segment's start y
     * @param x2 the segment's end x
     * @param y2 the segment's end y
     * @return the squared distance between the point and the segment
     */
    public static double distanceSqToSegment(double px, double py,
                                             double x1, double y1, double x2, double y2) {
        double[] p = closestPointOnSegment(px, py, x1, y1, x2, y2);
        double ex = px - p[0];
        double ey = py - p[1];
        return ex * ex + ey * ey;
    }

    /**
     * Linear interpolation of depth along a segment at a given xy point.
     *
     * @param x0 the segment's start x
     * @param y0 the segment's start y
     * @param depth0 the segment's start depth
     * @param x1 the segment's end x
     * @param y1 the segment's end y
     * @param depth1 the segment's end depth
     * @param px the query point x coordinate
     * @param py the query point y coordinate
     * @return the interpolated depth at {@code (px, py)}
     */
    public static double interpolateZOnSegment(double x0, double y0, double depth0,
                                               double x1, double y1, double depth1,
                                               double px, double py) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-20) {
            return (depth0 + depth1) / 2;
        }
        double t = ((px - x0) * dx + (py - y0) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        return depth0 + t * (depth1 - depth0);
    }

    /**
     * Least-squares plane-fit depth interpolation at a query point, falling
     * back to inverse-distance weighting when the plane fit is degenerate.
     *
     * @param polyX the polygon x coordinates
     * @param polyY the polygon y coordinates
     * @param polyZ the polygon depth values
     * @param beta the precomputed plane coefficients, or {@code null} to fit
     * @param xy the query point as {@code {x, y}}
     * @return the interpolated depth at {@code xy}
     */
    public static double interpolateZ(double[] polyX, double[] polyY, double[] polyZ, double[] beta, double[] xy) {
        if (beta != null) {
            return beta[0] + beta[1] * xy[0] + beta[2] * xy[1];
        }
        int n = polyX.length;
        if (n >= 3) {
            double[] b = fitPlane(polyX, polyY, polyZ, n);
            if (b != null) {
                return b[0] + b[1] * xy[0] + b[2] * xy[1];
            }
        }
        double wsum = 0, zsum = 0;
        for (int i = 0; i < n; i++) {
            double d = Math.hypot(polyX[i] - xy[0], polyY[i] - xy[1]);
            double w = 1 / d;
            wsum += w;
            zsum += w * polyZ[i];
        }
        return wsum > 0 ? zsum / wsum : 0;
    }

    /**
     * Fits z = b0 + b1*x + b2*y by least squares; {@code null} when singular.
     *
     * @param x the point x coordinates
     * @param y the point y coordinates
     * @param z the point z values
     * @param n the number of points to use
     * @return the coefficients {@code {b0, b1, b2}}, or {@code null} when singular
     */
    public static double[] fitPlane(double[] x, double[] y, double[] z, int n) {
        // Normal equations: (X'X) b = X'z, X = [1 x y].
        double s00 = n, s01 = 0, s02 = 0, s11 = 0, s12 = 0, s22 = 0;
        double r0 = 0, r1 = 0, r2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = x[i], yi = y[i], zi = z[i];
            s01 += xi;
            s02 += yi;
            s11 += xi * xi;
            s12 += xi * yi;
            s22 += yi * yi;
            r0 += zi;
            r1 += xi * zi;
            r2 += yi * zi;
        }
        double det = s00 * (s11 * s22 - s12 * s12) - s01 * (s01 * s22 - s12 * s02)
                + s02 * (s01 * s12 - s11 * s02);
        if (Math.abs(det) < 1e-12) {
            return null;
        }
        double b0 = ((s11 * s22 - s12 * s12) * r0
                - (s01 * s22 - s12 * s02) * r1
                + (s01 * s12 - s11 * s02) * r2) / det;
        double b1 = (-(s01 * s22 - s12 * s02) * r0
                + (s00 * s22 - s02 * s02) * r1
                - (s00 * s12 - s01 * s02) * r2) / det;
        double b2 = ((s01 * s12 - s11 * s02) * r0
                - (s00 * s12 - s01 * s02) * r1
                + (s00 * s11 - s01 * s01) * r2) / det;
        return new double[] {b0, b1, b2};
    }

    /**
     * SAT-overlap test for two simple polygons (mirrors {@code sat_overlap}).
     *
     * @param x1 the first polygon's x coordinates
     * @param y1 the first polygon's y coordinates
     * @param x2 the second polygon's x coordinates
     * @param y2 the second polygon's y coordinates
     * @return {@code true} when the two polygons overlap
     */
    public static boolean satOverlap(double[] x1, double[] y1, double[] x2, double[] y2) {
        if (satOverlapOn(x1, y1, x2, y2) && satOverlapOn(x2, y2, x1, y1)) {
            return true;
        }
        return false;
    }

    private static boolean satOverlapOn(double[] aX, double[] aY, double[] bX, double[] bY) {
        int n = aX.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ex = aX[j] - aX[i], ey = aY[j] - aY[i];
            double nx = -ey, ny = ex;
            double l = Math.hypot(nx, ny);
            if (l == 0) {
                continue;
            }
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE;
            double minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                double p = aX[k] * nx + aY[k] * ny;
                minA = Math.min(minA, p);
                maxA = Math.max(maxA, p);
            }
            for (int k = 0; k < bX.length; k++) {
                double p = bX[k] * nx + bY[k] * ny;
                minB = Math.min(minB, p);
                maxB = Math.max(maxB, p);
            }
            if (maxA <= minB + 1e-9 || maxB <= minA + 1e-9) {
                return false;
            }
        }
        return true;
    }

    /**
     * Centroid of a polygon (area-weighted, closes the ring if needed).
     *
     * @param xs the polygon x coordinates
     * @param ys the polygon y coordinates
     * @return the centroid as {@code {x, y}}
     */
    public static double[] centroid(double[] xs, double[] ys) {
        int n = xs.length;
        double area2 = 0, cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double cross = xs[i] * ys[j] - xs[j] * ys[i];
            area2 += cross;
            cx += (xs[i] + xs[j]) * cross;
            cy += (ys[i] + ys[j]) * cross;
        }
        if (area2 == 0) {
            double sx = 0, sy = 0;
            for (int i = 0; i < n; i++) {
                sx += xs[i];
                sy += ys[i];
            }
            return new double[] {sx / n, sy / n};
        }
        return new double[] {cx / (3 * area2), cy / (3 * area2)};
    }

    /**
     * An approximate intersection centroid of two simple polygons: the mean of
     * the vertices of A that lie inside B, the vertices of B inside A, and the
     * midpoints of every edge-crossing pair. Non-zero when the footprints
     * overlap; otherwise {@code null}. (The reference implementation averages
     * the centroids of the polygon-clipping intersection polygons; sampling
     * the overlap boundary yields a stable stand-in for the same quantity.)
     *
     * @param aX the first polygon's x coordinates
     * @param aY the first polygon's y coordinates
     * @param bX the second polygon's x coordinates
     * @param bY the second polygon's y coordinates
     * @return the approximate intersection centroid, or {@code null} when disjoint
     */
    public static double[] intersectionCentroid(double[] aX, double[] aY,
                                                double[] bX, double[] bY) {
        var samples = new ArrayList<double[]>();
        for (int i = 0; i < aX.length; i++) {
            if (pointInPolygon(aX[i], aY[i], bX, bY)) {
                samples.add(new double[] {aX[i], aY[i]});
            }
        }
        for (int i = 0; i < bX.length; i++) {
            if (pointInPolygon(bX[i], bY[i], aX, aY)) {
                samples.add(new double[] {bX[i], bY[i]});
            }
        }
        int nA = aX.length;
        for (int i = 0; i < nA; i++) {
            int j = (i + 1) % nA;
            int kB = bX.length;
            for (int k = 0; k < kB; k++) {
                int l = (k + 1) % kB;
                double[] p = segmentIntersection(aX[i], aY[i], aX[j], aY[j],
                        bX[k], bY[k], bX[l], bY[l]);
                if (p != null) {
                    samples.add(p);
                }
            }
        }
        if (samples.isEmpty()) {
            return null;
        }
        double sx = 0, sy = 0;
        for (var p : samples) {
            sx += p[0];
            sy += p[1];
        }
        return new double[] {sx / samples.size(), sy / samples.size()};
    }

    /**
     * Signed area of a polygon (positive = CCW).
     *
     * @param xs the polygon x coordinates
     * @param ys the polygon y coordinates
     * @return the signed area
     */
    public static double signedArea(double[] xs, double[] ys) {
        int n = xs.length;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += xs[i] * ys[j] - xs[j] * ys[i];
        }
        return sum / 2;
    }

    private static double cross(double[] xs, double[] ys, int a, int b, int c) {
        return (xs[b] - xs[a]) * (ys[c] - ys[a]) - (xs[c] - xs[a]) * (ys[b] - ys[a]);
    }

    private static double[] intersect(double x1, double y1, double x2, double y2,
                                      double x3, double y3, double x4, double y4) {
        return segmentIntersection(x1, y1, x2, y2, x3, y3, x4, y4);
    }
}
