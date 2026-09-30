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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.Surface3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the 3-D surface figures through the SVG backend and checks
 * that each geometry reaches the surface: filled {@code <polygon>} tiles from
 * the grid/function/density/ridgeline/contour geoms, and the polygon panels,
 * point overlay, and residual {@code <line>} segments of
 * {@code Geoms.smooth3d()}.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Surface3dRenderTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    @Test
    void gridSurfaceRendersTiles() throws Exception {
        var svg = renderSvg(Surface3dPlots.createGridSurface());
        assertTrue(svg.contains("sombrero"), "the SVG must carry the surface figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 300, "a 20x20 grid tessellates into many tiles");
    }

    @Test
    void rightTriangleGridRendersTiles() throws Exception {
        var svg = renderSvg(Surface3dPlots.createGridSurfaceRight());
        assertTrue(svg.contains("RIGHT1"), "the SVG must carry the right-triangle figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 500, "right-triangle tiling doubles the tile count");
    }

    @Test
    void functionSurfaceRendersTiles() throws Exception {
        var svg = renderSvg(Surface3dPlots.createFunctionSurface());
        assertTrue(svg.contains("sin(r)/r"), "the SVG must carry the function figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "the evaluated grid tessellates into tiles");
    }

    @Test
    void densitySurfaceRendersTiles() throws Exception {
        var svg = renderSvg(Surface3dPlots.createDensitySurface());
        assertTrue(svg.contains("kernel surface"), "the SVG must carry the density figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "the density grid tessellates into tiles");
    }

    @Test
    void ridgelineRendersRidges() throws Exception {
        var svg = renderSvg(Surface3dPlots.createRidgeline());
        assertTrue(svg.contains("slices"), "the SVG must carry the ridgeline figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 5, "five slices become five ridge polygons");
        // Verify back-to-front paint order: the far slice (largest screen x)
        // is drawn first, each later slice is nearer (smaller screen x).
        assertRidgelinePaintedFarFirst(svg);
    }

    @Test
    void contourRendersBands() throws Exception {
        var svg = renderSvg(Surface3dPlots.createContour());
        assertTrue(svg.contains("layer cake"), "the SVG must carry the contour figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 5, "the contour bands must be filled polygons");
    }

    @Test
    void smoothRendersPanelsPointsAndResiduals() throws Exception {
        var svg = renderSvg(Surface3dPlots.createSmooth());
        assertTrue(svg.contains("loess + se"), "the SVG must carry the smooth figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "the fitted surface panels must be polygons");
        assertTrue(countOccurrences(svg, "<line ") >= 1, "the residual segments must be drawn");
    }

    @Test
    void smoothLmRendersFullDomainPlane() throws Exception {
        var svg = renderSvg(Surface3dPlots.createSmoothLm());
        assertTrue(svg.contains("FULL"), "the SVG must carry the lm figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "the lm plane panels must be polygons");
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /**
     * Asserts that the orange-filled ridge slices appear in the SVG in
     * back-to-front paint order: the far slice (largest first-vertex screen-x)
     * drawn first, the nearest slice last.
     */
    private static void assertRidgelinePaintedFarFirst(String svg) {
        var firstXPattern = Pattern.compile(
                "<polygon points=\"(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)[^\"]*\" fill=\"#ff7f00\"/>");
        var matcher = firstXPattern.matcher(svg);
        var firstXs = new ArrayList<Double>();
        while (matcher.find()) {
            firstXs.add(Double.parseDouble(matcher.group(1)));
        }
        assertEquals(5, firstXs.size(), "five ridge slices must be painted");
        for (int i = 0; i + 1 < firstXs.size(); i++) {
            assertTrue(firstXs.get(i) > firstXs.get(i + 1),
                    "slice " + i + " (sx=" + firstXs.get(i)
                            + ") must be farther than slice " + (i + 1)
                            + " (sx=" + firstXs.get(i + 1) + ")");
        }
    }
}
