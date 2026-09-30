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
import java.util.regex.Pattern;
import org.jtaccuino.gog.examples.dflib.CoordView3dArticlePlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the {@code Coords.coord3d()} article figures — the composed
 * multi-panel grids over the shared {@code sin(x)*cos(y)} function surface —
 * and checks that each reaches the surface (many filled tiles coloured by z)
 * and carries its composition title and per-panel captions.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class CoordView3dArticleRenderTest {

    private static final Pattern ROTATED_TEXT = Pattern.compile(
            "<g transform=\"translate\\((-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)\\)\">\\s*"
                    + "<g transform=\"rotate\\((-?\\d+(?:\\.\\d+)?)\\)\">\\s*<text[^>]*>([^<]*)</text>");

    @Test
    void rotationCompositionCarriesAllSixPanels() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createRotation());
        assertTrue(svg.contains("coord3d() rotation"), "the rotation figure must be titled");
        for (String caption : new String[] {
                "Default", "All angles 0", "Arbitrary combination",
                "pitch = 30", "roll = 30", "yaw = 30"}) {
            assertTrue(svg.contains(caption), "the rotation grid must include " + caption);
        }
        assertTrue(countOccurrences(svg, "<polygon") >= 5000,
                "six 40x40 function surfaces tessellate into many tiles");
    }

    @Test
    void perspectiveAndThemesRenderSurfaceTiles() throws Exception {
        for (var figure : new GgFigure[] {
                CoordView3dArticlePlots.createPerspective(),
                CoordView3dArticlePlots.createScales(),
                CoordView3dArticlePlots.createPanels(),
                CoordView3dArticlePlots.createZoom(),
                CoordView3dArticlePlots.createLabels(),
                CoordView3dArticlePlots.createThemes()}) {
            var svg = renderSvg(figure);
            assertTrue(countOccurrences(svg, "<polygon") >= 500,
                    "every article figure must render the function surface, got " + figure);
        }
    }

    @Test
    void themesShowDifferentBackgrounds() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createThemes());
        assertTrue(svg.contains("#1e1e1e"), "theme_dark() must render its dark panel background");
        assertTrue(svg.contains("theme_dark()") && svg.contains("theme_minimal()"),
                "the themes composition must carry both panel captions");
    }

    /**
     * A data-less {@code Geoms.function3d()} surface must report its explicit
     * {@code xlim}/{@code ylim} to the coord so the cube fits the evaluated
     * domain; otherwise the tiles at ±π render far outside the panel (the
     * article figures looked "way too big").
     */
    @Test
    void functionSurfaceFitsItsCanvas() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createScalesFree());
        double[] xRange = polygonXRange(svg);
        assertTrue(xRange[0] >= 0 && xRange[1] <= 900,
                "the single surface must stay inside the 900px canvas, got "
                        + xRange[0] + ".." + xRange[1]);
    }

    private static double[] polygonXRange(String svg) {
        var m = Pattern.compile("<polygon points=\"([0-9.,\\s-]+)\"").matcher(svg);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        while (m.find()) {
            var nums = Pattern.compile("-?\\d+\\.?\\d*").matcher(m.group(1));
            var xs = new ArrayList<Double>();
            while (nums.find()) {
                xs.add(Double.parseDouble(nums.group()));
            }
            for (int i = 0; i < xs.size(); i += 2) {
                min = Math.min(min, xs.get(i));
                max = Math.max(max, xs.get(i));
            }
        }
        return new double[] {min, max};
    }

    /**
     * Tick marks must be collinear with the gridline that passes through their
     * break: the tick direction is the free-axis line projected at the break's
     * own position, not through the cube centre (which, under perspective,
     * froze every tick at the centre angle while the gridlines fanned out).
     */
    @Test
    void tickMarksAlignWithTheirGridlines() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createLabelsAuto());
        var segments = lineSegments(svg);
        var ticks = new ArrayList<Seg>();
        var gridlines = new ArrayList<Seg>();
        for (var s : segments) {
            if (s.segLength() < 25) ticks.add(s); else gridlines.add(s);
        }
        assertTrue(ticks.size() >= 20, "a perspective cube must draw many ticks");
        assertTrue(gridlines.size() >= 20, "the cube faces must draw gridlines");
        int matched = 0;
        for (var t : ticks) {
            double minDist = Double.MAX_VALUE;
            double bestAngleDelta = Double.MAX_VALUE;
            for (var g : gridlines) {
                double d = Math.min(endpointDist(t, g), endpointDist(t.flip(), g));
                if (d < 2.0 && d < minDist) {
                    minDist = d;
                    bestAngleDelta = angleDelta(t, g);
                }
            }
            if (minDist < 2.0) {
                matched++;
                assertTrue(bestAngleDelta < 2.0,
                        "a tick must point along its gridline, off by " + bestAngleDelta + " deg");
            }
        }
        assertTrue(matched >= ticks.size() / 2,
                "most ticks must sit on a gridline to be checked, matched " + matched + " of " + ticks.size());
    }

    /**
     * With every face drawn the axis furniture must stay on the cube
     * silhouette: the z-axis edge selection used to fall on an interior edge,
     * so the z tick labels ran parallel to and collided with the y labels. The
     * z labels must now continue along the left silhouette edge beyond the y
     * labels rather than interleaving with them.
     */
    @Test
    void allPanelsKeepZLabelsBeyondTheYLabels() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createThemeForeground());
        int panelEnd = svg.indexOf("theme_light() + panel.foreground");
        assertTrue(panelEnd > 0, "the foreground composition must contain both panels");
        var labels = labelAnchors(svg.substring(0, panelEnd));

        // The x labels of the all-panels cube are near-vertical (|rot| ~ 77-90),
        // the y/z labels are shallow; the colorbar and axis titles are excluded
        // by position/value.
        var y = new ArrayList<AnchoredLabel>();
        var z = new ArrayList<AnchoredLabel>();
        for (var l : labels) {
            if (Math.abs(l.rot()) > 45 || l.x() > 350) continue;
            switch (l.text()) {
                case "-3", "-2", "2", "3" -> y.add(l);   // breaks unique to the y axis
                case "-0.5", "0.5" -> z.add(l);          // breaks unique to the z axis
                default -> { }
            }
        }
        assertTrue(y.size() == 4, "the y axis must expose its four unique breaks, got " + y.size());
        assertTrue(z.size() == 2, "the z axis must expose its two unique breaks, got " + z.size());

        // Direction along the y edge (from the -3 break to the +3 break). In the
        // fixed rendering the z labels sit beyond the +3 y break along this
        // direction; in the buggy rendering they were projected into the y span.
        AnchoredLabel a = y.get(0), b = y.get(y.size() - 1);
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double len = Math.hypot(dx, dy);
        dx /= len;
        dy /= len;
        double maxY = Double.NEGATIVE_INFINITY;
        for (var l : y) {
            maxY = Math.max(maxY, (l.x() - a.x()) * dx + (l.y() - a.y()) * dy);
        }
        for (var l : z) {
            double t = (l.x() - a.x()) * dx + (l.y() - a.y()) * dy;
            assertTrue(t > maxY + 5,
                    "a z label must sit beyond the y labels on the shared edge, at t=" + t + " maxY=" + maxY);
        }
    }

    /** An axis tick label: its anchor (x, y), rotation, and text. */
    private record AnchoredLabel(double x, double y, double rot, String text) {}

    /**
     * Tick labels along an axis must sit on a clean line. They used to be
     * offset along the per-break gridline direction with a {@code textW/2}
     * centring term (the anchor is one end of the text), so the labels fanned
     * with the gridlines under perspective and the wide labels jumped out —
     * wiggly instead of aligned. Every label set of the default-view figure
     * must now be nearly collinear.
     */
    @Test
    void tickLabelsSitOnCleanLines() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createLabelsAuto());
        var x = new ArrayList<AnchoredLabel>();
        var y = new ArrayList<AnchoredLabel>();
        var z = new ArrayList<AnchoredLabel>();
        for (var l : labelAnchors(svg)) {
            if (l.text().equals("x") || l.text().equals("y") || l.text().equals("z")) continue;
            if (l.rot() > 40) x.add(l);
            else if (l.rot() < -16.5) y.add(l);
            else if (l.rot() < -7) z.add(l);
        }
        for (var set : List.of(x, y, z)) {
            // The y/z rotation bands meet around -17 deg: in the recentred
            // fit the top z label can rotate just past the band edge and land
            // in the y set near the shared cube corner, leaving the z band with
            // four labels. 4 is enough to establish collinearity per axis.
            assertTrue(set.size() >= 4, "each axis must carry its tick labels, got " + set.size());
            double maxResidual = maxLineResidual(set);
            // That intruding z label then lies ~2px off the y line; 3px still
            // catches the fanning bug (several px) while tolerating it.
            assertTrue(maxResidual < 3.0,
                    "the tick labels of an axis must sit on a line, max residual " + maxResidual + "px");
        }
    }

    /**
     * Every tick label must sit reliably just past its own tick, in every
     * rotation of the sweep. Labels were once offset along the edge normal
     * while the ticks point along the gridline; in near-face-on views that
     * normal can point opposite the tick (labels landed behind the tick) and
     * in oblique views the labels drifted far from their ticks. Now each label
     * is a fixed distance beyond its tick along the tick's own direction, so it
     * must be near a tick tip and on the outward side of it.
     */
    @Test
    void tickLabelsSitJustPastTheirTicks() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createRotation());
        var ticks = new ArrayList<Seg>();
        for (var s : lineSegments(svg)) {
            if (s.segLength() < 25) ticks.add(s);
        }
        int checked = 0;
        for (var l : labelAnchors(svg)) {
            if (l.text().equals("x") || l.text().equals("y") || l.text().equals("z")) continue;
            boolean near = false;
            boolean beyond = false;
            for (var t : ticks) {
                double dx = t.x2() - t.x1(), dy = t.y2() - t.y1();
                double len2 = dx * dx + dy * dy;
                double proj = ((l.x() - t.x1()) * dx + (l.y() - t.y1()) * dy) / len2;
                double tipDist = Math.hypot(l.x() - t.x2(), l.y() - t.y2());
                if (tipDist < 8) {
                    near = true;
                    if (proj > 1.0) {
                        beyond = true;
                    }
                }
            }
            assertTrue(near, "a tick label must sit near a tick, got '" + l.text() + "'");
            assertTrue(beyond,
                    "a tick label must lie beyond a tick's tip (not beside or behind), got '" + l.text() + "'");
            checked++;
        }
        assertTrue(checked >= 60, "the rotation sweep must carry many tick labels, got " + checked);
    }

    /**
     * Composed 3-D grids must size their cells to the cube's square aspect
     * rather than filling a tall canvas, so a one-row composition does not
     * waste vertical space around a vertically-centred cube.
     */
    @Test
    void composed3dCellsAreSquare() throws Exception {
        var svg = renderSvg(CoordView3dArticlePlots.createThemes());
        var rects = new ArrayList<double[]>();
        var m = Pattern.compile(
                "<g transform=\"translate\\(\\d+\\.?\\d*,\\d+\\.?\\d*\\)\">\\s*"
                        + "<rect[^>]*width=\"([\\d.]+)\" height=\"([\\d.]+)\"").matcher(svg);
        while (m.find()) {
            rects.add(new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))});
        }
        assertTrue(rects.size() >= 2, "the theming composition must draw its two cells");
        for (double[] r : rects) {
            double aspect = r[0] / r[1];
            assertTrue(aspect > 0.9 && aspect < 1.1,
                    "a 3-D composed cell must be square, got aspect " + aspect);
        }
    }

    /** The largest perpendicular distance of any label from the least-squares
     * line through the set; near zero when the labels are collinear. */
    private static double maxLineResidual(List<AnchoredLabel> labels) {
        double n = labels.size();
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (var l : labels) {
            sx += l.x();
            sy += l.y();
            sxx += l.x() * l.x();
            sxy += l.x() * l.y();
        }
        double denom = n * sxx - sx * sx;
        if (Math.abs(denom) < 1e-9) {
            return 0;
        }
        double a = (n * sxy - sx * sy) / denom;
        double b = (sy - a * sx) / n;
        double scale = Math.sqrt(a * a + 1);
        double maxR = 0;
        for (var l : labels) {
            double r = Math.abs(l.y() - (a * l.x() + b)) / scale;
            maxR = Math.max(maxR, r);
        }
        return maxR;
    }

    /** The rotated text nodes of an SVG slice. */
    private static List<AnchoredLabel> labelAnchors(String svg) {
        var out = new ArrayList<AnchoredLabel>();
        var m = ROTATED_TEXT.matcher(svg);
        while (m.find()) {
            out.add(new AnchoredLabel(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3)), m.group(4)));
        }
        return out;
    }

    /** A directed screen segment from the SVG line elements. */
    private record Seg(double x1, double y1, double x2, double y2) {
        double segLength() {
            return Math.hypot(x2 - x1, y2 - y1);
        }

        Seg flip() {
            return new Seg(x2, y2, x1, y1);
        }
    }

    private static List<Seg> lineSegments(String svg) {
        var out = new ArrayList<Seg>();
        var m = Pattern.compile(
                "<line x1=\"(-?\\d+(?:\\.\\d+)?)\" y1=\"(-?\\d+(?:\\.\\d+)?)\" "
                        + "x2=\"(-?\\d+(?:\\.\\d+)?)\" y2=\"(-?\\d+(?:\\.\\d+)?)\"").matcher(svg);
        while (m.find()) {
            out.add(new Seg(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4))));
        }
        return out;
    }

    /** Distance between the nearest endpoints of two segments. */
    private static double endpointDist(Seg a, Seg b) {
        double d11 = Math.hypot(a.x1 - b.x1, a.y1 - b.y1);
        double d12 = Math.hypot(a.x1 - b.x2, a.y1 - b.y2);
        double d21 = Math.hypot(a.x2 - b.x1, a.y2 - b.y1);
        double d22 = Math.hypot(a.x2 - b.x2, a.y2 - b.y2);
        return Math.min(Math.min(d11, d12), Math.min(d21, d22));
    }

    /** The unsigned angle (degrees) between two segments' directions. */
    private static double angleDelta(Seg a, Seg b) {
        double aa = Math.toDegrees(Math.atan2(a.y2 - a.y1, a.x2 - a.x1)) % 180;
        double ab = Math.toDegrees(Math.atan2(b.y2 - b.y1, b.x2 - b.x1)) % 180;
        double d = Math.abs(aa - ab);
        return Math.min(d, 180 - d);
    }

    private static int countOccurrences(String text, String needle) {
        var matcher = Pattern.compile(Pattern.quote(needle)).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String renderSvg(GgFigure figure) throws Exception {
        return onFxThread(() -> new SvgExporter().size(1100, 700).toSvg(figure));
    }
}
