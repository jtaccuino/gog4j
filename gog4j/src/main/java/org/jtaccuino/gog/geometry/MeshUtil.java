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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mesh construction utilities shared by the 3D surface stats and geoms:
 * point-grid generation, tessellation of points into polygon tiles,
 * point-level gradient computation, ridgeline construction and contour-band
 * construction.
 */
public final class MeshUtil {

    private MeshUtil() {
    }

    /**
     * A flattened point grid in row-major order.
     *
     * @param x the x coordinates of the grid points
     * @param y the y coordinates of the grid points
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only layout carrier
    public record PointGrid(double[] x, double[] y) {
    }

    /**
     * One polygon tile produced by tessellation.
     *
     * @param vertexIdx the indices of the tile's vertices, in ring order
     * @param group the group label the tile belongs to
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only index carrier
    public record Tile(int[] vertexIdx, String group) {
    }

    /**
     * A closed polygon loop (used by contours and ridgelines).
     *
     * @param x the loop's x coordinates
     * @param y the loop's y coordinates
     * @param z the loop's z coordinates
     * @param zmax the loop's maximum z, used for the group label
     * @param group the group label the loop belongs to
     * @param subId an optional sub-identifier within the group
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only vertex carrier
    public record Loop(double[] x, double[] y, double[] z, double zmax, String group, String subId) {
    }

    // ------------------------------------------------------------------
    // Grid generation
    // ------------------------------------------------------------------

    /**
     * Generates a regular grid covering {@code [xlim[0], xlim[1]] × [ylim[0],
     * ylim[1]]}, mirroring {@code make_point_grid()}.
     *
     * @param grid      the {@link GridGeometry} of the lattice
     * @param n         the per-dimension resolution (a single int or a pair
     *                  {@code {nx, ny}})
     * @param direction the {@link GridDirection} of the grid (transposes it)
     * @param xlim      the x extent
     * @param ylim      the y extent
     * @param trim      whether to trim the equilateral grid to the bounding box
     * @return the generated grid vertices
     */
    public static PointGrid makePointGrid(GridGeometry grid, int[] n, GridDirection direction,
                                          double[] xlim, double[] ylim, boolean trim) {
        int nx = n == null || n.length == 0 ? 40 : n[0];
        int ny = n == null || n.length < 2 ? nx : n[1];
        if (nx < 2 || ny < 2) {
            throw new IllegalArgumentException("`n` must be at least 2");
        }

        double[] gx;
        double[] gy;
        if (grid == GridGeometry.EQUILATERAL) {
            double triHeight = Math.sqrt(3) / 2;
            int nxE = nx;
            int nyE = n != null && n.length >= 2 ? ny : (int) Math.round(nx * (1 / triHeight));
            var xs = new ArrayList<Double>();
            var ys = new ArrayList<Double>();
            for (int r = 0; r < nyE; r++) {
                double yy = r * triHeight;
                for (int c = 0; c < nxE; c++) {
                    double xx = c + (r % 2 == 0 ? 0 : 0.5);
                    xs.add(xx);
                    ys.add(yy);
                }
            }
            gx = xs.stream().mapToDouble(Double::doubleValue).toArray();
            gy = ys.stream().mapToDouble(Double::doubleValue).toArray();
            if (trim) {
                double maxX = Double.NEGATIVE_INFINITY;
                for (double v : gx) {
                    maxX = Math.max(maxX, v);
                }
                double secondMaxX = Double.NEGATIVE_INFINITY;
                for (double v : gx) {
                    if (v != maxX) {
                        secondMaxX = Math.max(secondMaxX, v);
                    }
                }
                double minX = Double.POSITIVE_INFINITY;
                for (double v : gx) {
                    minX = Math.min(minX, v);
                }
                for (int i = 0; i < gx.length; i++) {
                    if (gx[i] == maxX) {
                        gx[i] = secondMaxX;
                    }
                }
                // Re-pad the right edge with the left-column points (mirrors R).
                int leftCount = 0;
                for (double v : gx) {
                    if (v == minX) {
                        leftCount++;
                    }
                }
                var padX = new double[gx.length + leftCount];
                var padY = new double[gy.length + leftCount];
                System.arraycopy(gx, 0, padX, 0, gx.length);
                System.arraycopy(gy, 0, padY, 0, gy.length);
                int k = gx.length;
                for (int i = 0; i < gx.length && k < padX.length; i++) {
                    if (gx[i] == minX) {
                        padX[k] = maxX;
                        padY[k] = gy[i];
                        k++;
                    }
                }
                gx = padX;
                gy = padY;
            }
        } else {
            gx = new double[nx * ny];
            gy = new double[nx * ny];
            int k = 0;
            for (int r = 0; r < ny; r++) {
                for (int c = 0; c < nx; c++) {
                    gx[k] = c;
                    gy[k] = r;
                    k++;
                }
            }
        }

        if (direction == GridDirection.Y) {
            for (int i = 0; i < gx.length; i++) {
                double t = gx[i];
                gx[i] = gy[i];
                gy[i] = t;
            }
        }
        rescale(gx, xlim);
        rescale(gy, ylim);
        return new PointGrid(gx, gy);
    }

    private static void rescale(double[] vals, double[] range) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : vals) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double span = max - min;
        for (int i = 0; i < vals.length; i++) {
            vals[i] = span == 0 ? range[0] : range[0] + (vals[i] - min) / span * (range[1] - range[0]);
        }
    }

    // ------------------------------------------------------------------
    // Tessellation
    // ------------------------------------------------------------------

    /**
     * Converts a point collection into polygon tiles, mirroring
     * {@code points_to_tiles()}.
     *
     * @param x           point x coordinates
     * @param y           point y coordinates
     * @param group       per-point group id
     * @param row         per-point row index (regular grid), or {@code null}
     * @param col         per-point column index (regular grid), or {@code null}
     * @param method      the {@link SurfaceMethod}
     * @param gridType    the {@link GridGeometry} for regular-grid lattice tiles
     * @param groupPrefix the prefix for generated tile groups
     * @return the tessellated tiles
     */
    public static List<Tile> pointsToTiles(double[] x, double[] y, String[] group,
                                           int[] row, int[] col,
                                           SurfaceMethod method, GridGeometry gridType, String groupPrefix) {
        boolean regular;
        if (method == SurfaceMethod.DELAUNAY) {
            regular = false;
        } else if (method == SurfaceMethod.GRID) {
            regular = true;
        } else if (row == null && col == null) {
            regular = isRegularGrid(x, y, group);
        } else {
            regular = true;
        }

        if (!regular || gridType == GridGeometry.EQUILATERAL) {
            return delaunayPointsToTiles(x, y, group, groupPrefix);
        }
        return rectPointsToTiles(x, y, group, row, col, gridType, groupPrefix);
    }

    /**
     * Whether the points form a complete regular grid per group
     * ({@code is_regular_grid}).
     *
     * @param x the point x coordinates
     * @param y the point y coordinates
     * @param group the per-point group label, or {@code null}
     * @return {@code true} when every group forms a complete grid
     */
    public static boolean isRegularGrid(double[] x, double[] y, String[] group) {
        if (group == null || allSameGroup(group)) {
            return completeGrid(x, y);
        }
        Map<String, int[]> byGroup = new HashMap<>();
        for (int i = 0; i < x.length; i++) {
            var g = group[i];
            var c = byGroup.computeIfAbsent(g, k -> new int[] {0, 0});
            c[0]++;
        }
        for (var entry : byGroup.entrySet()) {
            String g = entry.getKey();
            int count = entry.getValue()[0];
            int n = 0;
            double[] gx = new double[count];
            double[] gy = new double[count];
            for (int i = 0; i < x.length; i++) {
                if (group[i].equals(g)) {
                    gx[n] = x[i];
                    gy[n] = y[i];
                    n++;
                }
            }
            if (!completeGrid(gx, gy)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allSameGroup(String[] group) {
        String first = group[0];
        for (String g : group) {
            if (!g.equals(first)) {
                return false;
            }
        }
        return true;
    }

    private static boolean completeGrid(double[] x, double[] y) {
        double[] xu = Arrays.stream(x).sorted().distinct().toArray();
        double[] yu = Arrays.stream(y).sorted().distinct().toArray();
        return x.length == xu.length * yu.length;
    }

    private static List<Tile> rectPointsToTiles(double[] x, double[] y, String[] group,
                                                int[] row, int[] col, GridGeometry gridType,
                                                String groupPrefix) {
        int[] r;
        int[] c;
        if (row == null || col == null) {
            r = new int[x.length];
            c = new int[y.length];
            double[] xu = Arrays.stream(x).sorted().distinct().toArray();
            double[] yu = Arrays.stream(y).sorted().distinct().toArray();
            for (int i = 0; i < x.length; i++) {
                c[i] = indexOf(xu, x[i]) + 1;
                r[i] = indexOf(yu, y[i]) + 1;
            }
        } else {
            r = row;
            c = col;
        }

        int nCol = 0, nRow = 0;
        for (int i = 0; i < r.length; i++) {
            nCol = Math.max(nCol, c[i]);
            nRow = Math.max(nRow, r[i]);
        }
        if (nCol < 2 || nRow < 2) {
            throw new IllegalArgumentException("Need at least 2 points in each dimension to create tiles");
        }

        // Map (row, col) -> point index.
        Map<Long, Integer> lookup = new HashMap<>();
        for (int i = 0; i < r.length; i++) {
            lookup.put(key(r[i], c[i]), i);
        }

        boolean multiGroup = group != null && !allSameGroup(group);
        var tiles = new ArrayList<Tile>();
        int tileId = 1;
        List<int[]> quads = new ArrayList<>();
        List<String> quadGroups = new ArrayList<>();
        for (int tr = 1; tr <= nRow - 1; tr++) {
            for (int tc = 1; tc <= nCol - 1; tc++) {
                int[] corners = new int[4];
                int[] dc = {0, 1, 1, 0};
                int[] dr = {0, 0, 1, 1};
                boolean complete = true;
                for (int k = 0; k < 4; k++) {
                    Integer idx = lookup.get(key(tr + dr[k], tc + dc[k]));
                    if (idx == null) {
                        complete = false;
                        break;
                    }
                    corners[k] = idx;
                }
                if (!complete) {
                    continue;
                }
                String bg = multiGroup ? group[corners[0]] : null;
                quads.add(corners);
                quadGroups.add(bg);
            }
        }

        if (gridType == GridGeometry.RIGHT1 || gridType == GridGeometry.RIGHT2) {
            for (int q = 0; q < quads.size(); q++) {
                int[] corners = quads.get(q);
                String bg = quadGroups.get(q);
                int[] vi = gridType == GridGeometry.RIGHT1
                        ? new int[] {corners[0], corners[1], corners[2], corners[0], corners[2], corners[3]}
                        : new int[] {corners[0], corners[1], corners[3], corners[1], corners[2], corners[3]};
                for (int t = 0; t < 2; t++) {
                    int[] tri = {vi[t * 3], vi[t * 3 + 1], vi[t * 3 + 2]};
                    tiles.add(new Tile(tri, tileGroup(groupPrefix, bg, tileId)));
                    tileId++;
                }
            }
        } else {
            for (int q = 0; q < quads.size(); q++) {
                tiles.add(new Tile(quads.get(q), tileGroup(groupPrefix, quadGroups.get(q), tileId)));
                tileId++;
            }
        }
        return tiles;
    }

    private static String tileGroup(String groupPrefix, String baseGroup, int tileId) {
        return baseGroup == null ? groupPrefix + tileId : groupPrefix + tileId + "::" + baseGroup;
    }

    private static List<Tile> delaunayPointsToTiles(double[] x, double[] y, String[] group,
                                                    String groupPrefix) {
        var tiles = new ArrayList<Tile>();
        if (group == null || allSameGroup(group)) {
            tiles.addAll(delaunayGroup(x, y, group == null ? null : group[0], groupPrefix, 0));
            return tiles;
        }
        for (String g : distinctGroups(group)) {
            int count = 0;
            for (int i = 0; i < x.length; i++) {
                if (group[i].equals(g)) {
                    count++;
                }
            }
            double[] gx = new double[count];
            double[] gy = new double[count];
            int n = 0;
            for (int i = 0; i < x.length; i++) {
                if (group[i].equals(g)) {
                    gx[n] = x[i];
                    gy[n] = y[i];
                    n++;
                }
            }
            tiles.addAll(delaunayGroup(gx, gy, g, groupPrefix, 0));
        }
        return tiles;
    }

    private static List<Tile> delaunayGroup(double[] x, double[] y, String baseGroup,
                                            String groupPrefix, int start) {
        if (x.length < 3) {
            throw new IllegalArgumentException("Need at least 3 points for triangulation");
        }
        int[][] tri = Delaunay2D.triangulate(x, y);
        var tiles = new ArrayList<Tile>(tri.length);
        int tileId = 1;
        for (int[] t : tri) {
            tiles.add(new Tile(new int[] {t[0] + start, t[1] + start, t[2] + start},
                    groupPrefix + tileId + (baseGroup == null ? "" : "::" + baseGroup)));
            tileId++;
        }
        return tiles;
    }

    private static List<String> distinctGroups(String[] group) {
        var seen = new ArrayList<String>();
        for (String g : group) {
            if (!seen.contains(g)) {
                seen.add(g);
            }
        }
        return seen;
    }

    private static long key(int row, int col) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }

    private static int indexOf(double[] sorted, double v) {
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] == v) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Ridgelines
    // ------------------------------------------------------------------

    /**
     * Converts a point grid into ridgeline polygons, mirroring
     * {@code points_to_ridgelines()}. One closed polygon per slice along the
     * ridge axis, spanning from {@code base} up over the slice's ridge and back
     * down.
     *
     * @param x           point x coordinates
     * @param y           point y coordinates
     * @param z           point z coordinates
     * @param group       per-point group id
     * @param direction   the {@link GridDirection} of the ridges (slice along the
     *                    perpendicular axis)
     * @param base        the base z value, or {@code null} for {@code min(z)}
     * @param groupPrefix the prefix for generated group ids
     * @return the ridgeline polygons
     */
    public static List<Loop> pointsToRidgelines(double[] x, double[] y, double[] z,
                                                String[] group, GridDirection direction,
                                                Double base, String groupPrefix) {
        List<Integer> order = deduplicate(x, y);
        int n = order.size();
        double[] ux = new double[n];
        double[] uy = new double[n];
        double[] uz = new double[n];
        String[] ug = group == null ? null : new String[n];
        for (int i = 0; i < n; i++) {
            int o = order.get(i);
            ux[i] = x[o];
            uy[i] = y[o];
            uz[i] = z[o];
            if (ug != null) {
                ug[i] = group[o];
            }
        }

        double baseZ = base == null ? minOrNaN(uz) : base;
        boolean sliceByX = direction == GridDirection.X;
        double[] sliceValues = sliceByX ? ux : uy;
        double[] ridgeValues = sliceByX ? uy : ux;

        double[] sv = Arrays.stream(sliceValues).sorted().distinct().toArray();
        var ridges = new ArrayList<Loop>();
        for (int i = 0; i < sv.length; i++) {
            double s = sv[i];
            int count = 0;
            for (int k = 0; k < n; k++) {
                if (Math.abs(sliceValues[k] - s) < 1e-9) {
                    count++;
                }
            }
            if (count < 2) {
                continue;
            }
            double[] rv = new double[count];
            double[] rz = new double[count];
            int m = 0;
            for (int k = 0; k < n; k++) {
                if (Math.abs(sliceValues[k] - s) < 1e-9) {
                    rv[m] = ridgeValues[k];
                    rz[m] = uz[k];
                    m++;
                }
            }
            // Sort by ridge variable.
            int[] idx = indexesSorted(rv);
            double rMax = rz[idx[0]];
            double rMin = rv[idx[0]];
            for (int t = 1; t < idx.length; t++) {
                rMax = Math.max(rMax, rz[idx[t]]);
                rMin = Math.min(rMin, rv[idx[t]]);
            }
            int total = count + 2;
            double[] px = new double[total];
            double[] py = new double[total];
            double[] pz = new double[total];
            px[0] = sliceByX ? s : rv[idx[0]];
            py[0] = sliceByX ? rv[idx[0]] : s;
            pz[0] = baseZ;
            for (int t = 0; t < count; t++) {
                px[t + 1] = sliceByX ? s : rv[idx[t]];
                py[t + 1] = sliceByX ? rv[idx[t]] : s;
                pz[t + 1] = rz[idx[t]];
            }
            px[total - 1] = sliceByX ? s : rv[idx[count - 1]];
            py[total - 1] = sliceByX ? rv[idx[count - 1]] : s;
            pz[total - 1] = baseZ;
            ridges.add(new Loop(px, py, pz, rMax,
                    group == null ? groupPrefix + (i + 1) + "::" + ug[0]
                            : groupPrefix + (i + 1) + "::" + subGroup(ug[idx[0]]),
                    null));
        }
        return ridges;
    }

    private static String subGroup(String g) {
        int k = g.lastIndexOf("::");
        return k < 0 ? g : g.substring(k + 2);
    }

    private static double minOrNaN(double[] v) {
        double m = Double.POSITIVE_INFINITY;
        for (double d : v) {
            m = Math.min(m, d);
        }
        return m;
    }

    private static List<Integer> deduplicate(double[] x, double[] y) {
        var order = new ArrayList<Integer>();
        outer:
        for (int i = 0; i < x.length; i++) {
            for (int j : order) {
                if (x[j] == x[i] && y[j] == y[i]) {
                    continue outer;
                }
            }
            order.add(i);
        }
        return order;
    }

    private static int[] indexesSorted(double[] v) {
        Integer[] idx = new Integer[v.length];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, (a, b) -> Double.compare(v[a], v[b]));
        int[] out = new int[v.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = idx[i];
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Contours
    // ------------------------------------------------------------------

    /**
     * Converts a point grid into contour band polygons ("layer cake"), mirroring
     * {@code points_to_contours()}. Each band is the region where z lies at or
     * above the level's lower break; the polygon boundary is traced with
     * marching squares and bilinear interpolation. Ring loops at the same level
     * share the group id so an even-odd fill renders holes correctly.
     *
     * @param x           point x coordinates
     * @param y           point y coordinates
     * @param z           point z coordinates
     * @param group       per-point group id
     * @param breaks      explicit break levels, or {@code null} to compute from
     *                    {@code bins}/{@code binwidth}
     * @param bins        number of bins used when {@code breaks} is {@code null}
     * @param binwidth    bin width used when {@code breaks} is {@code null}
     * @return the contour band rings (one {@code Loop} per ring)
     */
    public static List<Loop> pointsToContours(double[] x, double[] y, double[] z, String[] group,
                                              double[] breaks, Integer bins, Double binwidth) {
        double[] xv = Arrays.stream(x).sorted().distinct().toArray();
        double[] yv = Arrays.stream(y).sorted().distinct().toArray();
        int nx = xv.length;
        int ny = yv.length;
        if (nx < 2 || ny < 2) {
            throw new IllegalArgumentException("Geoms.contour3d() requires at least a 2x2 grid of points");
        }

        double[][] zm = new double[ny][nx];
        for (double[] row : zm) {
            Arrays.fill(row, Double.NaN);
        }
        for (int i = 0; i < x.length; i++) {
            int xi = Arrays.binarySearch(xv, x[i]);
            int yi = Arrays.binarySearch(yv, y[i]);
            if (xi >= 0 && yi >= 0) {
                zm[yi][xi] = z[i];
            }
        }

        double zMin = Double.POSITIVE_INFINITY, zMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < x.length; i++) {
            zMin = Math.min(zMin, z[i]);
            zMax = Math.max(zMax, z[i]);
        }

        double[] br;
        if (breaks != null && breaks.length > 0) {
            br = breaks;
        } else if (binwidth != null) {
            double start = Math.floor(zMin / binwidth) * binwidth;
            double end = Math.ceil(zMax / binwidth) * binwidth;
            var seq = new ArrayList<Double>();
            for (double v = start; v <= end + 1e-9; v += binwidth) {
                seq.add(v);
            }
            br = seq.stream().mapToDouble(Double::doubleValue).toArray();
        } else {
            int b = bins == null ? 10 : bins;
            br = pretty(zMin, zMax, b);
        }
        if (br.length == 0 || br[0] > zMin) {
            var tmp = new double[br.length + 1];
            tmp[0] = zMin;
            System.arraycopy(br, 0, tmp, 1, br.length);
            br = tmp;
        }
        if (br[br.length - 1] < zMax) {
            var tmp = new double[br.length + 1];
            System.arraycopy(br, 0, tmp, 0, br.length);
            tmp[br.length] = zMax;
            br = tmp;
        }
        Arrays.sort(br);
        // Deduplicate.
        int uniq = 0;
        for (int i = 0; i < br.length; i++) {
            if (uniq == 0 || br[i] != br[uniq - 1]) {
                br[uniq++] = br[i];
            }
        }
        br = Arrays.copyOf(br, uniq);
        if (br.length < 2) {
            throw new IllegalArgumentException("Need at least 2 breaks to create contour bands");
        }

        var loops = new ArrayList<Loop>();
        for (int bi = 0; bi < br.length; bi++) {
            double level = br[bi];
            List<double[][]> segs = marchingSquares(xv, yv, zm, level);
            if (segs.isEmpty()) {
                continue;
            }
            List<List<double[]>> rings = chainSegments(segs);
            for (List<double[]> ring : rings) {
                double[] rx = new double[ring.size()];
                double[] ry = new double[ring.size()];
                for (int k = 0; k < ring.size(); k++) {
                    rx[k] = ring.get(k)[0];
                    ry[k] = ring.get(k)[1];
                }
                double centroidX = 0, centroidY = 0;
                for (int k = 0; k < rx.length; k++) {
                    centroidX += rx[k];
                    centroidY += ry[k];
                }
                centroidX /= rx.length;
                centroidY /= ry.length;
                double cz = bilinear(xv, yv, zm, centroidX, centroidY);
                boolean filled = !Double.isNaN(cz) && cz >= level;
                double[] pz = new double[rx.length];
                Arrays.fill(pz, level);
                String sub = filled ? null : "hole";
                loops.add(new Loop(rx, ry, pz, level, contourGroup(group, bi), sub));
            }
        }
        return loops;
    }

    private static String contourGroup(String[] group, int bi) {
        String base = group == null || group.length == 0 ? "1" : group[0];
        return "contour__layer::" + base + "::" + bi;
    }

    /** Approximates {@code pretty()}. */
    static double[] pretty(double lo, double hi, int n) {
        if (lo == hi) {
            return new double[] {lo};
        }
        double range = hi - lo;
        double span = range / Math.max(1, n);
        double[] steps = {1, 2, 2.5, 5, 10};
        double factor = Math.pow(10, Math.floor(Math.log10(span)));
        double step = 1;
        for (double s : steps) {
            if (s * factor >= span) {
                step = s * factor;
                break;
            }
        }
        var out = new ArrayList<Double>();
        for (double v = Math.floor(lo / step) * step; v <= hi + 1e-9; v += step) {
            out.add(v);
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double bilinear(double[] xv, double[] yv, double[][] zm, double px, double py) {
        int xi = Arrays.binarySearch(xv, px);
        if (xi < 0) {
            xi = -xi - 2;
        }
        int yi = Arrays.binarySearch(yv, py);
        if (yi < 0) {
            yi = -yi - 2;
        }
        xi = Math.max(0, Math.min(xi, xv.length - 2));
        yi = Math.max(0, Math.min(yi, yv.length - 2));
        double x0 = xv[xi], x1 = xv[xi + 1];
        double y0 = yv[yi], y1 = yv[yi + 1];
        double q00 = zm[yi][xi], q10 = zm[yi][xi + 1];
        double q01 = zm[yi + 1][xi], q11 = zm[yi + 1][xi + 1];
        if (Double.isNaN(q00) || Double.isNaN(q10) || Double.isNaN(q01) || Double.isNaN(q11)) {
            return Double.NaN;
        }
        double fx = (px - x0) / (x1 - x0);
        double fy = (py - y0) / (y1 - y0);
        return q00 * (1 - fx) * (1 - fy) + q10 * fx * (1 - fy)
                + q01 * (1 - fx) * fy + q11 * fx * fy;
    }

    /**
     * Traces the zero-crossings of {@code z == level} over every grid cell,
     * applying bilinear interpolation on each edge crossing.
     */
    private static List<double[][]> marchingSquares(double[] xv, double[] yv, double[][] zm, double level) {
        var segs = new ArrayList<double[][]>();
        for (int yi = 0; yi < yv.length - 1; yi++) {
            for (int xi = 0; xi < xv.length - 1; xi++) {
                double z00 = zm[yi][xi];
                double z10 = zm[yi][xi + 1];
                double z01 = zm[yi + 1][xi];
                double z11 = zm[yi + 1][xi + 1];
                if (Double.isNaN(z00) || Double.isNaN(z10) || Double.isNaN(z01) || Double.isNaN(z11)) {
                    continue;
                }
                double x0 = xv[xi], x1 = xv[xi + 1];
                double y0 = yv[yi], y1 = yv[yi + 1];
                // Edge crossings; edge order is bottom, right, top, left.
                double[][] pts = new double[4][];
                pts[0] = cross(x0, y0, x1, y0, z00, z10, level);
                pts[1] = cross(x1, y0, x1, y1, z10, z11, level);
                pts[2] = cross(x1, y1, x0, y1, z11, z01, level);
                pts[3] = cross(x0, y1, x0, y0, z01, z00, level);
                int count = 0;
                for (double[] p : pts) {
                    if (p != null) {
                        count++;
                    }
                }
                if (count == 2) {
                    double[] a = null, b = null;
                    for (double[] p : pts) {
                        if (p != null) {
                            if (a == null) {
                                a = p;
                            } else if (b == null) {
                                b = p;
                            }
                        }
                    }
                    segs.add(new double[][] {a, b});
                } else if (count == 4) {
                    // Saddle case: connect bottom-left and right-top when BL and
                    // TR corners are on the inside, otherwise bottom-right and top-left.
                    boolean bl = z00 >= level;
                    if (bl) {
                        segs.add(new double[][] {pts[0], pts[3]});
                        segs.add(new double[][] {pts[1], pts[2]});
                    } else {
                        segs.add(new double[][] {pts[0], pts[1]});
                        segs.add(new double[][] {pts[2], pts[3]});
                    }
                }
            }
        }
        return segs;
    }

    private static double[] cross(double ax, double ay, double bx, double by,
                                  double za, double zb, double level) {
        if ((za - level) * (zb - level) >= 0) {
            return null;
        }
        double t = (level - za) / (zb - za);
        return new double[] {ax + t * (bx - ax), ay + t * (by - ay)};
    }

    /** Chains contour segments into closed rings. */
    private static List<List<double[]>> chainSegments(List<double[][]> segs) {
        Map<String, List<Integer>> endpoint = new HashMap<>();
        for (int i = 0; i < segs.size(); i++) {
            double[][] s = segs.get(i);
            endpoint.computeIfAbsent(ptKey(s[0]), k -> new ArrayList<>()).add(i);
            endpoint.computeIfAbsent(ptKey(s[1]), k -> new ArrayList<>()).add(i);
        }
        boolean[] used = new boolean[segs.size()];
        var rings = new ArrayList<List<double[]>>();
        for (int i = 0; i < segs.size(); i++) {
            if (used[i]) {
                continue;
            }
            var ring = new ArrayList<double[]>();
            int cur = i;
            while (cur >= 0 && !used[cur]) {
                used[cur] = true;
                double[] a = segs.get(cur)[0];
                double[] b = segs.get(cur)[1];
                if (ring.isEmpty()) {
                    ring.add(copy(a));
                } else {
                    double[] prev = ring.get(ring.size() - 1);
                    if (!same(prev, a)) {
                        double[] tmp = a;
                        a = b;
                        b = tmp;
                    }
                }
                ring.add(copy(b));
                // Pick the next unused segment sharing the current endpoint.
                List<Integer> cand = endpoint.get(ptKey(b));
                int next = -1;
                for (int c : cand) {
                    if (!used[c]) {
                        next = c;
                        break;
                    }
                }
                cur = next;
            }
            rings.add(ring);
        }
        return rings;
    }

    private static String ptKey(double[] p) {
        return Math.round(p[0] * 1e6) + "," + Math.round(p[1] * 1e6);
    }

    private static boolean same(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1e-6 && Math.abs(a[1] - b[1]) < 1e-6;
    }

    private static double[] copy(double[] src) {
        return new double[] {src[0], src[1]};
    }

    /**
     * Point-level gradients, mirroring {@code compute_point_gradients()}.
     *
     * @param x   point x coordinates
     * @param y   point y coordinates
     * @param z   point z coordinates
     * @param row per-point row index, or {@code null}
     * @param col per-point column index, or {@code null}
     * @return parallel {@code dzdx}/{@code dzdy} arrays
     */
    public static Gradient gradients(double[] x, double[] y, double[] z, int[] row, int[] col) {
        double[] dzdx;
        double[] dzdy;
        if ((row != null && col != null) || isRegularGrid(x, y, null)) {
            var g = gridGradients(x, y, z, row, col);
            dzdx = g[0];
            dzdy = g[1];
        } else {
            var g = irregularGradients(x, y, z);
            dzdx = g[0];
            dzdy = g[1];
        }
        return new Gradient(dzdx, dzdy);
    }

    /**
     * Result of {@link #gradients}.
     *
     * @param dzdx the z gradient with respect to x at each point
     * @param dzdy the z gradient with respect to y at each point
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only gradient carrier
    public record Gradient(double[] dzdx, double[] dzdy) {
    }

    private static double[][] gridGradients(double[] x, double[] y, double[] z, int[] row, int[] col) {
        int[] r;
        int[] c;
        if (row == null || col == null) {
            r = new int[x.length];
            c = new int[y.length];
            double[] xu = Arrays.stream(x).sorted().distinct().toArray();
            double[] yu = Arrays.stream(y).sorted().distinct().toArray();
            for (int i = 0; i < x.length; i++) {
                c[i] = indexOf(xu, x[i]) + 1;
                r[i] = indexOf(yu, y[i]) + 1;
            }
        } else {
            r = row;
            c = col;
        }
        int nCol = 0, nRow = 0;
        for (int i = 0; i < r.length; i++) {
            nCol = Math.max(nCol, c[i]);
            nRow = Math.max(nRow, r[i]);
        }
        nCol = Math.max(nCol, 2);
        nRow = Math.max(nRow, 2);

        double[] xu = new double[nCol];
        double[] yu = new double[nRow];
        for (int i = 0; i < nCol; i++) {
            xu[i] = Double.NaN;
        }
        for (int i = 0; i < nRow; i++) {
            yu[i] = Double.NaN;
        }
        double[][] zMat = new double[nRow][nCol];
        for (int i = 0; i < x.length; i++) {
            xu[c[i] - 1] = x[i];
            yu[r[i] - 1] = y[i];
            zMat[r[i] - 1][c[i] - 1] = z[i];
        }
        double[] dx = new double[nCol];
        double[] dy = new double[nRow];
        for (int i = 0; i < nCol; i++) {
            dx[i] = Double.NaN;
        }
        for (int i = 0; i < nRow; i++) {
            dy[i] = Double.NaN;
        }
        double last = Double.NaN;
        for (int i = 0; i < nCol; i++) {
            if (!Double.isNaN(xu[i])) {
                if (Double.isNaN(last)) {
                    last = xu[i];
                } else {
                    dx[i] = xu[i] - last;
                    last = xu[i];
                }
            }
        }
        double expectedDx = 0;
        int dxn = 0;
        for (int i = 1; i < nCol; i++) {
            if (!Double.isNaN(xu[i]) && !Double.isNaN(xu[i - 1])) {
                expectedDx += dx[i];
                dxn++;
            }
        }
        double meanDx = dxn > 0 ? expectedDx / dxn : 1;
        double meanDy = 1;
        {
            int dyn = 0;
            double sum = 0;
            for (int i = 1; i < nRow; i++) {
                if (!Double.isNaN(yu[i]) && !Double.isNaN(yu[i - 1])) {
                    sum += yu[i] - yu[i - 1];
                    dyn++;
                }
            }
            meanDy = dyn > 0 ? sum / dyn : 1;
        }

        double[][] gx = new double[nRow][nCol];
        double[][] gy = new double[nRow][nCol];
        for (int rr = 0; rr < nRow; rr++) {
            for (int cc = 0; cc < nCol; cc++) {
                gx[rr][cc] = Double.NaN;
                gy[rr][cc] = Double.NaN;
                if (!Double.isNaN(zMat[rr][cc])) {
                    double zg = cc > 0 && !Double.isNaN(zMat[rr][cc - 1])
                            && cc < nCol - 1 && !Double.isNaN(zMat[rr][cc + 1])
                            ? (zMat[rr][cc + 1] - zMat[rr][cc - 1]) / (2 * meanDx)
                            : cc == 0 && cc + 1 < nCol && !Double.isNaN(zMat[rr][cc + 1])
                            ? (zMat[rr][cc + 1] - zMat[rr][cc]) / meanDx
                            : cc == nCol - 1 && cc - 1 >= 0 && !Double.isNaN(zMat[rr][cc - 1])
                            ? (zMat[rr][cc] - zMat[rr][cc - 1]) / meanDx
                            : Double.NaN;
                    double zg2 = rr > 0 && !Double.isNaN(zMat[rr - 1][cc])
                            && rr < nRow - 1 && !Double.isNaN(zMat[rr + 1][cc])
                            ? (zMat[rr + 1][cc] - zMat[rr - 1][cc]) / (2 * meanDy)
                            : rr == 0 && rr + 1 < nRow && !Double.isNaN(zMat[rr + 1][cc])
                            ? (zMat[rr + 1][cc] - zMat[rr][cc]) / meanDy
                            : rr == nRow - 1 && rr - 1 >= 0 && !Double.isNaN(zMat[rr - 1][cc])
                            ? (zMat[rr][cc] - zMat[rr - 1][cc]) / meanDy
                            : Double.NaN;
                    gx[rr][cc] = zg;
                    gy[rr][cc] = zg2;
                }
            }
        }

        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            outX[i] = gx[r[i] - 1][c[i] - 1];
            outY[i] = gy[r[i] - 1][c[i] - 1];
        }
        return new double[][] {outX, outY};
    }

    private static double[][] irregularGradients(double[] x, double[] y, double[] z) {
        int[][] tri = Delaunay2D.triangulate(x, y);
        int nTri = tri.length;
        double[] triGx = new double[nTri];
        double[] triGy = new double[nTri];
        double[] cx = new double[nTri];
        double[] cy = new double[nTri];
        for (int i = 0; i < nTri; i++) {
            int a = tri[i][0], b = tri[i][1], c = tri[i][2];
            double x1 = x[a], y1 = y[a], z1 = z[a];
            double x2 = x[b], y2 = y[b], z2 = z[b];
            double x3 = x[c], y3 = y[c], z3 = z[c];
            double v1x = x2 - x1, v1y = y2 - y1, v1z = z2 - z1;
            double v2x = x3 - x1, v2y = y3 - y1, v2z = z3 - z1;
            double nx = v1y * v2z - v1z * v2y;
            double ny = v1z * v2x - v1x * v2z;
            double nz = v1x * v2y - v1y * v2x;
            if (Math.abs(nz) > 1e-10) {
                triGx[i] = -nx / nz;
                triGy[i] = -ny / nz;
            } else {
                triGx[i] = Double.NaN;
                triGy[i] = Double.NaN;
            }
            cx[i] = (x1 + x2 + x3) / 3;
            cy[i] = (y1 + y2 + y3) / 3;
        }
        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            List<Integer> adjacent = new ArrayList<>();
            for (int t = 0; t < nTri; t++) {
                if (tri[t][0] == i || tri[t][1] == i || tri[t][2] == i) {
                    adjacent.add(t);
                }
            }
            double wSum = 0, gxSum = 0, gySum = 0;
            for (int t : adjacent) {
                if (Double.isNaN(triGx[t])) {
                    continue;
                }
                double d = Math.hypot(cx[t] - x[i], cy[t] - y[i]);
                double w = 1 / (d + 1e-10);
                wSum += w;
                gxSum += w * triGx[t];
                gySum += w * triGy[t];
            }
            outX[i] = wSum > 0 ? gxSum / wSum : Double.NaN;
            outY[i] = wSum > 0 ? gySum / wSum : Double.NaN;
        }
        return new double[][] {outX, outY};
    }
}
