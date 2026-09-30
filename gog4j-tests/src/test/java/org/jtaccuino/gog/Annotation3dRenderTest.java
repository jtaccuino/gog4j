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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.Annotation3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the 3-D primitive figures through the SVG backend and checks
 * that each geometry reaches the surface: per-row {@code <line>} segments,
 * grouped paths, billboard {@code <text>} labels, the filled reference
 * {@code <polygon>} circles of {@code Geoms.point3d()}, and the data-free
 * {@code Annotations3d.point()}/{@code .text()}/{@code .segment()} layers.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Annotation3dRenderTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    @Test
    void refCirclesRenderAsFilledPolygons() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createRefCircles());
        assertTrue(svg.contains("reference circles"), "the SVG must carry the circle figure");
        assertTrue(svg.contains("<polygon"), "reference circles must project to polygons");
    }

    @Test
    void refLinesRenderAsLineSegments() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createRefLines());
        assertTrue(svg.contains("reference lines"), "the SVG must carry the line figure");
        assertTrue(countOccurrences(svg, "<line ") >= 100, "every point needs a reference line");
    }

    @Test
    void segments3dRenderPerRow() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createSegments3d());
        assertTrue(svg.contains("one segment per row"), "the SVG must carry the segment figure");
        assertTrue(countOccurrences(svg, "<line ") >= 9, "nine rows must yield nine segments");
    }

    @Test
    void path3dConnectsConsecutivePoints() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createPath3d());
        assertTrue(svg.contains("winding path"), "the SVG must carry the path figure");
        assertTrue(countOccurrences(svg, "<line ") >= 9, "each group's runs become segments");
    }

    @Test
    void text3dRendersBillboardLabels() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createText3d());
        assertTrue(svg.contains("billboard cut labels"), "the SVG must carry the text figure");
        assertTrue(svg.contains("<text "), "billboard labels must project to SVG text");
        // Each of the 60 labels must be drawn inside its own translate group at
        // the projected position; a missing translate piles every label on the
        // origin (regression).
        assertTrue(countOccurrences(svg, "transform=\"translate(") >= 60,
                "each billboard label must be translated to its projected position");
    }

    @Test
    void annotate3dRendersPointTextAndSegment() throws Exception {
        var svg = renderSvg(Annotation3dPlots.createAnnotate3d());
        assertTrue(svg.contains("Annotations3d()"), "the SVG must carry the annotation figure");
        assertTrue(svg.contains("Annotations3d() marker"), "the annotation label must be drawn");
        assertTrue(countOccurrences(svg, "<line ") >= 1, "the annotation segment must be drawn");
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
