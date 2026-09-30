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
import org.dflib.DataFrame;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.examples.dflib.CoordView3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the {@code coord3d()} view-sweep figures and guards the axis
 * furniture: axis titles must stay inside the canvas even when the labelled
 * cube edge projects close to (or past) the panel boundary — the elongated
 * cube produced by {@code coord3d(scales = "fixed")} used to push the carat
 * title about 26px below the 700px canvas.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class CoordView3dRenderTest {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    // <g transform="translate(px,py)"> ... <text ...>...
    private static final Pattern ANCHORED_TEXT = Pattern.compile(
            "<g transform=\"translate\\((-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)\\)\">\\s*<g transform=\"rotate\\([^)]*\\)\">\\s*<text");

    @Test
    void fixedScalesKeepEveryAxisTitleInsideTheCanvas() throws Exception {
        var svg = renderSvg(CoordView3dPlots.createFixedScales());
        assertTrue(svg.contains("Fixed scales"), "the SVG must carry the fixed-scales figure");
        var anchored = anchoredTextEndpoints(svg);
        assertTrue(anchored.size() >= 3, "the three axis titles must be anchored, got " + anchored);
        for (double[] p : anchored) {
            assertTrue(p[0] >= 0 && p[0] <= WIDTH, "title must not poke out horizontally at x=" + p[0]);
            assertTrue(p[1] >= 0 && p[1] <= HEIGHT, "title must not fall off the canvas at y=" + p[1]);
        }
    }

    /**
     * The axis furniture must stay glued to its cube edge while the view
     * orbits: consecutive tick labels move smoothly along the edge rather than
     * hopping to a different edge (the old winner-take-all edge selection
     * jumped between cube edges and the on-silhouette boolean flipped the
     * tick direction). Every tick label in one frame must have a counterpart
     * within half the panel width in the adjacent 10-degree frame.
     */
    @Test
    void axisFurnitureStaysContinuousAcrossAnOrbitSweep() throws Exception {
        var plot = CoordView3dPlots.createDefaultView();
        var coord = (Coord3D) plot.descriptor().coord();
        List<double[]> prev = null;
        for (int yaw = -60; yaw <= 120; yaw += 10) {
            double pyaw = yaw;
            List<double[]> anchors = onFxThread(() -> {
                coord.pitch(0).roll(-60).yaw(pyaw);
                String svg = new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot);
                return anchoredTextEndpoints(svg);
            });
            if (prev != null) {
                for (double[] a : anchors) {
                    boolean hasNearby = false;
                    for (double[] b : prev) {
                        if (Math.hypot(a[0] - b[0], a[1] - b[1]) < WIDTH / 2.0) {
                            hasNearby = true;
                            break;
                        }
                    }
                    assertTrue(hasNearby,
                            "a tick label must not hop between edges at yaw " + yaw
                                    + " (anchor " + a[0] + "," + a[1] + ")");
                }
            }
            prev = anchors;
        }
    }

    /** The (x, y) pixel anchors of text nodes placed via a translate group. */
    private static List<double[]> anchoredTextEndpoints(String svg) {
        var out = new ArrayList<double[]>();
        Matcher m = ANCHORED_TEXT.matcher(svg);
        while (m.find()) {
            out.add(new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))});
        }
        return out;
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }
}
