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
package org.jtaccuino.gog;

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.Volume3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression tests for the two solid-3-D rendering defects:
 *
 * <ol>
 *   <li>bars must shade at their own full brightness — the continuous
 *       histogram may no longer flatten to a single uniform orange because the
 *       solid light is applied in RGB space (an HSL brightening caps out for
 *       fills that are already fully bright);</li>
 *   <li>every saturated bar/column/voxel vertex must project on or inside the
 *       cube's screen-space silhouette — the x/y domain folds the stat corner
 *       columns in, so extreme bars butt flush against the walls instead of
 *       overhanging half their width outside the cube.</li>
 * </ol>
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Solid3dBoundsTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    /** Perpendicular distance in pixels treated as "on" the silhouette edge. */
    private static final double ON_EDGE_PX = 0.5;

    private static final Pattern BACKGROUND = Pattern.compile(
            "<path d=\"([^\"]*?Z)\" fill=\"#ebebeb\"/>");
    private static final Pattern POLYGON = Pattern.compile(
            "<polygon points=\"([0-9., \\-]+)\" fill=\"#([0-9a-f]{6})\"/>");

    @Test
    void gridColumnsStayInsideTheCube() throws Exception {
        assertBarsInsideCube(renderSvg(Volume3dPlots.createColumns()));
    }

    @Test
    void zminGappedColumnsStayInsideTheCube() throws Exception {
        assertBarsInsideCube(renderSvg(Volume3dPlots.createColumnsZminGaps()));
    }

    @Test
    void discreteBarsStayInsideTheCube() throws Exception {
        assertBarsInsideCube(renderSvg(Volume3dPlots.createBarsDiscrete()));
    }

    @Test
    void voxelsStayInsideTheCube() throws Exception {
        assertBarsInsideCube(renderSvg(Volume3dPlots.createVoxels()));
    }

    private static void assertBarsInsideCube(String svg) {
        var hull = cubeCorners(svg);
        var polygon = convexHull(hull);
        assertTrue(polygon.size() >= 6,
                "the cube must project to at least a hexagon, got " + polygon.size());
        Matcher m = POLYGON.matcher(svg);
        var checked = 0;
        var outside = 0;
        var firstOutside = (double[]) null;
        while (m.find()) {
            if (!isSaturated(m.group(2))) {
                continue;
            }
            for (double[] p : points(m.group(1))) {
                checked++;
                if (!inside(polygon, p)) {
                    outside++;
                    if (firstOutside == null) {
                        firstOutside = p;
                    }
                }
            }
        }
        assertTrue(checked > 0, "the figure must carry saturated bar faces to check");
        assertTrue(outside == 0,
                outside + " of " + checked + " bar vertices project outside the cube\n"
                        + "firstOutside=" + (firstOutside == null ? "n/a"
                                : String.format("(%.1f,%.1f)", firstOutside[0], firstOutside[1]))
                        + "\npolygon=" + polygon.stream()
                                .map(q -> String.format("(%.1f,%.1f)", q[0], q[1]))
                                .collect(Collectors.joining("; ", "[", "]")));
    }

    /**
     * Returns the projected silhouette of the cube, recovered from the three
     * background faces (the two walls plus the floor) the coord draws with
     * the neutral {@code #ebebeb} fill.
     */
    private static List<double[]> cubeCorners(String svg) {
        var seen = new ArrayList<double[]>();
        Matcher m = BACKGROUND.matcher(svg);
        int faces = 0;
        while (m.find()) {
            faces++;
            for (double[] p : points(m.group(1))) {
                if (!seen.stream().anyMatch(q -> Math.abs(q[0] - p[0]) < 0.01
                        && Math.abs(q[1] - p[1]) < 0.01)) {
                    seen.add(p);
                }
            }
        }
        assertTrue(faces >= 3, "the coord must draw the three background faces, got " + faces);
        assertTrue(seen.size() >= 6, "the cube must project to at least a hexagon, got " + seen.size());
        return seen;
    }

    private static List<double[]> points(String coords) {
        var out = new ArrayList<double[]>();
        var nums = Pattern.compile("\\d+(?:\\.\\d+)?");
        var m = nums.matcher(coords);
        while (m.find()) {
            double a = Double.parseDouble(m.group());
            if (m.find()) {
                out.add(new double[] {a, Double.parseDouble(m.group())});
            }
        }
        return out;
    }

    private static boolean isSaturated(String hex) {
        double[] rgb = hex(hex);
        double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        return max - min > 0.25;
    }

    private static double[] hex(String rrgb) {
        return new double[] {
                Integer.parseInt(rrgb.substring(0, 2), 16) / 255.0,
                Integer.parseInt(rrgb.substring(2, 4), 16) / 255.0,
                Integer.parseInt(rrgb.substring(4, 6), 16) / 255.0
        };
    }

    /** Monotone-chain convex hull of the projected cube corners. */
    private static List<double[]> convexHull(List<double[]> pts) {
        var ps = new ArrayList<>(pts);
        ps.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        var hull = new ArrayList<double[]>();
        for (int pass = 0; pass < 2; pass++) {
            int start = hull.size();
            for (var p : pass == 0 ? ps : ps.reversed()) {
                while (hull.size() - start >= 2
                        && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                    hull.remove(hull.size() - 1);
                }
                hull.add(p);
            }
            hull.remove(hull.size() - 1);
        }
        return hull;
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    /**
     * Whether {@code p} lies on or inside the convex {@code polygon}.
     *
     * The comparison is done on the perpendicular pixel distance to each edge
     * rather than on the raw cross product: the SVG marks are rounded to two
     * decimals, so a vertex that sits exactly on the cube silhouette jitters by
     * fractions of a pixel while a genuine overhang is tens of pixels wide. An
     * epsilon of half a pixel therefore absorbs the rounding noise without
     * masking real protrusions.
     */
    private static boolean inside(List<double[]> polygon, double[] p) {
        double sign = 0;
        for (int i = 0; i < polygon.size(); i++) {
            double[] a = polygon.get(i);
            double[] b = polygon.get((i + 1) % polygon.size());
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double dist = cross(a, b, p) / len;
            if (Math.abs(dist) < ON_EDGE_PX) {
                continue;
            }
            double s = Math.signum(dist);
            if (sign != 0 && s != sign) {
                return false;
            }
            sign = s;
        }
        return true;
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }
}
