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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.Volume3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the 3-D volume figures through the SVG backend and checks that
 * each geometry reaches the surface: filled {@code <polygon>} faces from
 * {@code Geoms.col3d()}/{@code Geoms.bar3d()}/{@code Geoms.voxel3d()}, and the
 * triangular hull faces of {@code Geoms.hull3d()} (convex and alpha).
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Volume3dRenderTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    private static final Pattern TEXT = Pattern.compile("<text[^>]*>([^<]*)</text>");

    @Test
    void columnsRenderFaces() throws Exception {
        var svg = renderSvg(Volume3dPlots.createColumns());
        assertTrue(svg.contains("grid columns"), "the SVG must carry the column figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "each column face must be a polygon");
    }

    @Test
    void columnsZminGapsRenderFaces() throws Exception {
        var svg = renderSvg(Volume3dPlots.createColumnsZminGaps());
        assertTrue(svg.contains("zmin"), "the SVG must carry the zmin figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 100, "each column face must be a polygon");
    }

    @Test
    void columnsFacesSubsetRenders() throws Exception {
        var svg = renderSvg(Volume3dPlots.createColumnsFaces());
        assertTrue(svg.contains("faces"), "the SVG must carry the face-subset figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 50, "the selected faces must be polygons");
    }

    @Test
    void barsDiscreteRenderCounts() throws Exception {
        var svg = renderSvg(Volume3dPlots.createBarsDiscrete());
        assertTrue(svg.contains("discrete counts"), "the SVG must carry the discrete bar figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 20, "each (x, y) count bar must be a polygon");
    }

    @Test
    void barsDiscreteLabelTheirCategoryTicks() throws Exception {
        var svg = renderSvg(Volume3dPlots.createBarsDiscrete());
        // The discrete x/y bands must be labelled with their category letters
        // ('a'..'d', 'A'..'C'), one tick centred under each bar, instead of the
        // fraction digits (-0,5 / 0,5 / 1,5) that continuous pretty breaks
        // produced for the [-0.5, N-0.5] domain.
        var labels = textContent(svg);
        for (char cat : "aAbBcCd".toCharArray()) {
            assertTrue(labels.contains(String.valueOf(cat)),
                    "the discrete x/y axes must carry the category tick label '" + cat + "'");
        }
        assertFalse(labels.stream().anyMatch(Volume3dRenderTest::isFractionalTick),
                "categorical ticks must not show fractional half-steps, got " + labels);
    }

    private static boolean isFractionalTick(String label) {
        return Pattern.matches("-?\\d+,\\d+", label);
    }

    private static Set<String> textContent(String svg) {
        var out = new LinkedHashSet<String>();
        Matcher m = TEXT.matcher(svg);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    void barsContinuousRenderHistogram() throws Exception {
        var svg = renderSvg(Volume3dPlots.createBarsContinuous());
        assertTrue(svg.contains("histogram"), "the SVG must carry the histogram figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 20, "each filled bin must be a polygon");
    }

    @Test
    void voxelsRenderCubes() throws Exception {
        var svg = renderSvg(Volume3dPlots.createVoxels());
        assertTrue(svg.contains("sparse cubes"), "the SVG must carry the voxel figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 20, "each voxel face must be a polygon");
    }

    @Test
    void convexHullRendersTriangles() throws Exception {
        var svg = renderSvg(Volume3dPlots.createHullConvex());
        assertTrue(svg.contains("CONVEX"), "the SVG must carry the convex hull figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 50, "a sphere hull triangulates into many faces");
    }

    @Test
    void alphaHullRendersTriangles() throws Exception {
        var svg = renderSvg(Volume3dPlots.createHullAlpha());
        assertTrue(svg.contains("ALPHA"), "the SVG must carry the alpha hull figure");
        assertTrue(countOccurrences(svg, "<polygon") >= 50, "the torus alpha shape triangulates into faces");
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
}
