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

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.examples.dflib.AnnotatePlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the Sprint-7 annotation figures through the SVG backend and checks
 * the {@code annotate()} family end to end: {@link org.jtaccuino.gog.layer.Annotations}
 * data-free layers (point, text, segment, rect, hline, vline) must appear as
 * geometry in the exported SVG, and a log axis must draw its sub-decade minor
 * ticks automatically. Guarded on a live JavaFX toolkit, like the other
 * render tests.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class AnnotateTest {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;

    @Test
    void pointTextSegmentAndRectAnnotationsRender() throws Exception {
        var svg = renderSvg(AnnotatePlots.createAnnotations());
        assertNotNull(svg);
        assertTrue(svg.contains("annotate()"), "the SVG must carry the annotate() figure");
        assertTrue(svg.contains("point"), "the point annotation must render");
        assertTrue(svg.contains("text"), "the text annotation must render");
        assertTrue(svg.contains("segment"), "the segment annotation must render");
        assertTrue(svg.contains("rect"), "the rect annotation must render");
    }

    // GrayTheme: major grid white at 0.8, minor grid the same color at half width (0.4).
    private static final String MINOR_GRID = "stroke=\"#ffffff\" stroke-width=\"0.4\"";

    @Test
    void logAxisDrawsAutomaticSubDecadeTicks() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1, 2, 3, 4, 5), Series.of(10, 100, 1000, 10000, 100000));
        var plot = ggplot(df, aes().x("x").y("y"))
                .geoms(point())
                .coord(Coords.coordTrans(null, ScaleTransform.LOG10))
                .theme(t -> t.showMinorTicks(true));
        var svg = renderSvg(plot);
        assertNotNull(svg);
        assertTrue(!svg.contains("stroke=\"#333333\" stroke-width=\"0.4\""),
                "log sub-decades draw axis tick marks only, never full-width grid lines");
        assertTrue(svg.contains(subDecadeTick(svg, 3.75)),
                "every sub-decade of the log axis must draw a short tick mark");
        assertTrue(svg.contains(subDecadeTick(svg, 7.5)),
                "the 5·10ⁿ sub-decade must draw a mid-length tick mark");
    }

    @Test
    void logSubDecadesFollowMinorTickToggle() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1, 2, 3, 4, 5), Series.of(10, 100, 1000, 10000, 100000));
        var plot = ggplot(df, aes().x("x").y("y"))
                .geoms(point())
                .coord(Coords.coordTrans(null, ScaleTransform.LOG10))
                .theme(t -> t.showMinorTicks(false));
        var svg = renderSvg(plot);
        assertNotNull(svg);
        assertTrue(!svg.contains(subDecadeTick(svg, 3.75)),
                "showMinorTicks(false) must suppress the log sub-decade ticks");
    }

    // Log sub-decade ticks render at the theme's minor-tick length (default
    // 3.75) and twice it for the 5·10ⁿ mid ticks, outward from the log axis
    // edge. Panel geometry is read from the left axis line in the SVG itself
    // so the assertion is independent of the test render size.
    private static String subDecadeTick(String svg, double length) {
        var matcher = LEFT_AXIS_LINE.matcher(svg);
        double left = -1;
        while (matcher.find()) {
            double x = Double.parseDouble(matcher.group(1));
            left = left == -1 ? x : Math.min(left, x);
        }
        assertTrue(left >= 0, "the SVG must contain the left axis line");
        return "x2=\"" + svgNumber(left - length) + "\"";
    }

    private static final Pattern LEFT_AXIS_LINE =
            Pattern.compile("<line x1=\"([\\d.]+)\" y1=\"50\" x2=\"\\1\"");

    private static String svgNumber(double v) {
        return v == Math.floor(v) ? Long.toString((long) v) : Double.toString(v);
    }

    @Test
    void minorGridRendersOnALinearAxis() throws Exception {
        var svg = renderSvg(AnnotatePlots.createMinorGridLinear());
        assertNotNull(svg);
        assertTrue(svg.contains("minor grid on a linear axis"),
                "the SVG must carry the linear minor-grid figure");
        assertTrue(svg.contains(MINOR_GRID),
                "a linear axis (showMinorGrid on by default) must draw minor-grid lines in the theme's minor-grid color/width");
    }

    @Test
    void minorGridCanBeTurnedOff() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1, 2, 3), Series.of(10, 100, 1000));
        var plot = Ggplot.ggplot(df, aes().x("x").y("y"))
                .geoms(Geoms.point())
                .theme(t -> t.showMinorGrid(false).showMinorTicks(false));
        var svg = renderSvg(plot);
        assertNotNull(svg);
        assertTrue(!svg.contains("stroke-width=\"0.4\""),
                "turning the minor grid and ticks off must remove the 0.4px minor lines");
    }

    // Major ticks render outward from the panel edges at the theme's major tick
    // length (default 5): X-axis ticks run down from the bottom axis line, Y-axis
    // ticks run left from the left axis line. The left panel edge grows by the
    // measured y-tick-label width, which can differ by a pixel across font stacks,
    // so the assertions read the panel edges out of the rendered SVG (as
    // subDecadeTick does) instead of hard-coding coordinates. Minor ticks/grid are
    // off to isolate the majors.
    private static final Pattern AXIS_LINES =
            Pattern.compile("<line x1=\"([\\d.]+)\" y1=\"([\\d.]+)\" x2=\"([\\d.]+)\" y2=\"([\\d.]+)\" stroke=\"#333333\" stroke-width=\"1\"/>");

    private static double panelLeft(String svg) {
        double left = -1;
        var matcher = AXIS_LINES.matcher(svg);
        while (matcher.find()) {
            double x = Double.parseDouble(matcher.group(1));
            double y1 = Double.parseDouble(matcher.group(2));
            double y2 = Double.parseDouble(matcher.group(4));
            if (x == Double.parseDouble(matcher.group(3)) && Math.abs(y2 - y1) > left) {
                left = Math.abs(y2 - y1);
                left = x;
            }
        }
        return left;
    }

    private static double panelBottom(String svg) {
        double bottom = -1;
        var matcher = AXIS_LINES.matcher(svg);
        while (matcher.find()) {
            double x1 = Double.parseDouble(matcher.group(1));
            double x2 = Double.parseDouble(matcher.group(3));
            double y = Double.parseDouble(matcher.group(2));
            double y2 = Double.parseDouble(matcher.group(4));
            if (y == y2 && Math.abs(x2 - x1) > bottom) {
                bottom = Math.abs(x2 - x1);
                bottom = y;
            }
        }
        return bottom;
    }

    private static boolean hasXAxisMajorTick(String svg) {
        double left = panelLeft(svg);
        double bottom = panelBottom(svg);
        if (left < 0 || bottom < 0) {
            return false;
        }
        var matcher = AXIS_LINES.matcher(svg);
        while (matcher.find()) {
            double x1 = Double.parseDouble(matcher.group(1));
            double x2 = Double.parseDouble(matcher.group(3));
            double y1 = Double.parseDouble(matcher.group(2));
            double y2 = Double.parseDouble(matcher.group(4));
            if (x1 == x2 && x1 != left && (y1 == bottom || y2 == bottom) && y1 != y2) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasYAxisMajorTick(String svg) {
        double left = panelLeft(svg);
        if (left < 0) {
            return false;
        }
        var matcher = AXIS_LINES.matcher(svg);
        while (matcher.find()) {
            double x1 = Double.parseDouble(matcher.group(1));
            double x2 = Double.parseDouble(matcher.group(3));
            double y1 = Double.parseDouble(matcher.group(2));
            double y2 = Double.parseDouble(matcher.group(4));
            if (y1 == y2 && Math.min(x1, x2) < left && Math.max(x1, x2) == left) {
                return true;
            }
        }
        return false;
    }

    @Test
    void majorTicksFollowTheThemeToggle() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1, 2, 3), Series.of(10, 100, 1000));
        var base = Ggplot.ggplot(df, aes().x("x").y("y"))
                .geoms(Geoms.point())
                .theme(t -> t.showMinorGrid(false).showMinorTicks(false));

        var svg = renderSvg(base);
        assertTrue(hasXAxisMajorTick(svg) && hasYAxisMajorTick(svg),
                "major ticks must draw by default, on both axes");

        var tickless = renderSvg(base.theme(t -> t.showMajorTicks(false)));
        assertTrue(!hasXAxisMajorTick(tickless) && !hasYAxisMajorTick(tickless),
                "showMajorTicks(false) must remove the major tick marks on both axes");
    }

    @Test
    void xAxisTicksFollowThePerAxisToggle() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.of(1, 2, 3), Series.of(10, 100, 1000));
        var plot = Ggplot.ggplot(df, aes().x("x").y("y"))
                .geoms(Geoms.point())
                .theme(t -> t.showXAxisTicks(false).showMinorTicks(false).showMinorGrid(false));
        var svg = renderSvg(plot);
        assertTrue(!hasXAxisMajorTick(svg),
                "showXAxisTicks(false) must remove the X-axis ticks");
        assertTrue(hasYAxisMajorTick(svg),
                "the Y-axis ticks must remain when only the X-axis ticks are blanked");
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }
}
