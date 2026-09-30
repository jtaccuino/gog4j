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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drives the real hover handler on representative 3D gallery plots and asserts
 * that the tooltip pipeline reports data-space values rather than internal
 * render artifacts (depth scales, per-polygon group ids). Regression coverage
 * for {@code GeomPolygon3d}/{@code GeomPoint3d} {@code locate()} and the
 * {@code GeomSegment3d}/{@code GeomPath3d}/{@code GeomText3d} tooltips.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Tooltip3dRenderTest {

    @Test
    void polygonFamiliesReportDataSpaceCoordinates() throws Exception {
        var surface = scan(example("org.jtaccuino.gog.examples.dflib.Surface3dPlots",
                "createContour"));
        assertTrue(surface.size() >= 3, "expected surface tooltips, got " + surface);

        var column = scan(example("org.jtaccuino.gog.examples.dflib.Volume3dPlots",
                "createSingleColumn"));
        assertTrue(column.size() >= 3, "expected column tooltips, got " + column);

        for (var text : concat(surface, column)) {
            assertTrue(text.matches("X: .+\\nY: .+\\nZ: .+"),
                    "polygon tooltip must be data-space X/Y/Z, was: " + text);
            assertFalse(text.contains("__"), "internal group id leaked: " + text);
            assertFalse(text.contains("Group: "), "internal group label leaked: " + text);
        }
    }

    @Test
    void segmentAndPathReportEndpoints() throws Exception {
        var segments = scan(example("org.jtaccuino.gog.examples.dflib.Annotation3dPlots",
                "createSegments3d"));
        assertTrue(segments.size() >= 3, "expected segment tooltips, got " + segments);

        var path = scan(example("org.jtaccuino.gog.examples.dflib.Annotation3dPlots",
                "createPath3d"));
        assertTrue(path.size() >= 3, "expected path tooltips, got " + path);

        for (var text : concat(segments, path)) {
            assertTrue(text.matches("X: .+\\nY: .+\\nZ: .+\\nXend: .+\\nYend: .+\\nZend: .+"),
                    "segment tooltip must be data-space endpoints, was: " + text);
        }
    }

    @Test
    void textReportsLabels() throws Exception {
        var text = scan(example("org.jtaccuino.gog.examples.dflib.Annotation3dPlots",
                "createText3d"));
        assertTrue(text.size() >= 2, "expected text tooltips, got " + text);
        for (var label : text.keySet()) {
            assertFalse(label.matches(".*\\n.*"), "text tooltip must be a single label, was: " + label);
            assertTrue(List.of("Ideal", "Premium", "Very Good", "Good", "Fair").contains(label),
                    "unexpected 3D text label: " + label);
        }
    }

    @Test
    void pointScatterReportsDataSpaceCoordinates() throws Exception {
        var scatter = scan(example("org.jtaccuino.gog.examples.dflib.DiamondsPlots3d",
                "create3dScatterPriceCaratDepth"));
        assertTrue(scatter.size() >= 3, "expected scatter tooltips, got " + scatter);
        for (var text : scatter.keySet()) {
            assertTrue(text.matches("X: .+\\nY: .+\\nZ: .+"),
                    "scatter tooltip must be data-space X/Y/Z, was: " + text);
        }
    }

    private static List<String> concat(Map<String, Integer> a, Map<String, Integer> b) {
        var out = new ArrayList<String>(a.keySet());
        out.addAll(b.keySet());
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Plot<DataFrame> example(String clsName, String method) throws Exception {
        var cls = Class.forName(clsName);
        return (Plot<DataFrame>) cls.getMethod(method).invoke(null);
    }

    private Map<String, Integer> scan(Plot<DataFrame> plot) throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(), "JavaFX toolkit unavailable");
        int w = 800;
        int h = 500;
        var surface = new SvgDrawSurface(w, h, Color.WHITE);
        var label = labelField();
        var container = containerField();
        onFxThread(() -> {
            plot.renderTo(surface, w, h);
            return null;
        });

        var found = new HashMap<String, Integer>();
        for (int y = 30; y < h - 8; y += 30) {
            for (int x = 8; x < w - 8; x += 30) {
                String text = fireAndRead(plot, label, container, x, y);
                if (text != null) {
                    found.merge(text, 1, Integer::sum);
                }
            }
        }
        return found;
    }

    private String fireAndRead(Plot<?> plot, Field label, Field container, int x, int y)
            throws Exception {
        double dx = x;
        double dy = y;
        var evt = new MouseEvent(MouseEvent.MOUSE_MOVED, dx, dy, dx, dy, MouseButton.NONE,
                1, false, false, false, false, false, false, false, false, true, false, null);
        var holder = new Object() {
            String text;
        };
        onFxThread(() -> {
            plot.getOnMouseMoved().handle(evt);
            var hbox = (HBox) container.get(plot);
            holder.text = hbox.isVisible() ? ((Label) label.get(plot)).getText() : null;
            return null;
        });
        return holder.text;
    }

    private static Field containerField() throws Exception {
        var f = Plot.class.getDeclaredField("hoverTooltipContainer");
        f.setAccessible(true);
        return f;
    }

    private static Field labelField() throws Exception {
        var f = Plot.class.getDeclaredField("tooltipTextLabel");
        f.setAccessible(true);
        return f;
    }
}
