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
package org.jtaccuino.gog.facet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import javax.xml.parsers.DocumentBuilderFactory;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Facets;
import org.jtaccuino.gog.Geoms;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.facet.GridOptions.GridSwitch;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Verifies that a grid facet's axis labels and titles follow the strip switch:
 * {@code switch = "x"} moves the x-axis to the top, {@code switch = "y"} moves
 * the y-axis to the right, and {@code switch = "both"} does both. Only the
 * outer panels on the moved edge carry tick labels.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class FacetGridAxisSwitchTest {

    private static final Pattern NUMERIC = Pattern.compile("-?\\d+(\\.\\d+)?");

    /** A 2x2 grid: rows a/b, columns c/d, distinct x and y numeric ranges. */
    private static DataFrame gridData() {
        return DataFrame.byColumn("x", "y", "row", "col").of(
                Series.ofDouble(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                Series.ofDouble(100, 100, 100, 300, 300, 300, 500, 500, 500, 200, 200, 200),
                Series.of("a", "a", "a", "b", "b", "b", "a", "a", "a", "b", "b", "b"),
                Series.of("c", "c", "c", "c", "c", "c", "d", "d", "d", "d", "d", "d"));
    }

    private static Plot<DataFrame> gridPlot(GridSwitch preset) {
        return Ggplot.ggplot(gridData(), Aes.aes().x("x").y("y"))
                .geoms(Geoms.point().size(1.5))
                .facets(Facets.grid("row", "col", GridOptions.defaults().withSwitch(preset)))
                .labs(Labs.labs("t", "litres", "mpg"));
    }

    private static String renderSvg(Plot<DataFrame> plot) {
        var surface = new SvgDrawSurface(600, 400, Color.WHITE);
        plot.renderTo(surface, 600, 400);
        return surface.toSvg();
    }

    private static ParsedFigure parse(String svg) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));

            double minPanelX = Double.MAX_VALUE;
            double minPanelY = Double.MAX_VALUE;
            double maxPanelRight = -Double.MAX_VALUE;
            double maxPanelBottom = -Double.MAX_VALUE;
            var texts = new ArrayList<TextPos>();

            var rects = doc.getElementsByTagName("rect");
            for (var i = 0; i < rects.getLength(); i++) {
                var rect = (Element) rects.item(i);
                if (!"#ebebeb".equals(rect.getAttribute("fill"))) {
                    continue;
                }
                double x = Double.parseDouble(rect.getAttribute("x"));
                double y = Double.parseDouble(rect.getAttribute("y"));
                double w = Double.parseDouble(rect.getAttribute("width"));
                double h = Double.parseDouble(rect.getAttribute("height"));
                minPanelX = Math.min(minPanelX, x);
                minPanelY = Math.min(minPanelY, y);
                maxPanelRight = Math.max(maxPanelRight, x + w);
                maxPanelBottom = Math.max(maxPanelBottom, y + h);
            }

            var nodes = doc.getElementsByTagName("text");
            for (var i = 0; i < nodes.getLength(); i++) {
                texts.add(describe((Element) nodes.item(i)));
            }
            return new ParsedFigure(minPanelX, minPanelY, maxPanelRight, maxPanelBottom, texts);
        } catch (Exception e) {
            throw new AssertionError("Could not parse rendered SVG", e);
        }
    }

    /**
     * Describes a text element: its horizontal placement (rotate 90 = right,
     * rotate -90 = left) and the translate group transform, falling back to the
     * plain x/y attributes for unrotated labels.
     */
    private static TextPos describe(Element text) {
        double x = attr(text, "x");
        double y = attr(text, "y");
        double translateX = Double.NaN;
        double translateY = Double.NaN;
        boolean rotated = false;
        var parent = text.getParentNode();
        while (parent instanceof Element el) {
            if ("g".equals(el.getTagName())) {
                var transform = el.getAttribute("transform");
                if (transform.startsWith("rotate(")) {
                    rotated = true;
                }
                if (transform.startsWith("translate(")) {
                    var matcher = Pattern.compile("[-+]?\\d+(\\.\\d+)?")
                            .matcher(transform);
                    if (!matcher.find()) {
                        break;
                    }
                    translateX = Double.parseDouble(matcher.group());
                    if (!matcher.find()) {
                        break;
                    }
                    translateY = Double.parseDouble(matcher.group());
                }
            }
            parent = parent.getParentNode();
        }
        String content = text.getFirstChild() == null ? "" : text.getFirstChild().getNodeValue();
        if (!Double.isNaN(translateX)) {
            x = translateX;
            y = translateY;
        }
        return new TextPos(x, y, rotated, content);
    }

    private static double attr(Element e, String name) {
        var raw = e.getAttribute(name);
        return raw.isEmpty() ? 0.0 : Double.parseDouble(raw);
    }

    @Test
    void switchNoneKeepsXOnBottomAndYOnLeft() {
        var fig = parse(renderSvg(gridPlot(GridSwitch.NONE)));
        assertTrue(fig.anyNumericBelow(fig.maxPanelBottom()), "x labels stay below the bottom row");
        assertTrue(fig.anyNumericLeftOf(fig.minPanelX()), "y labels stay left of the left column");
        assertEquals(0, fig.countNumericAbove(fig.minPanelY()));
        assertEquals(0, fig.countNumericRightOf(fig.maxPanelRight()));
        assertTrue(fig.textAt("litres").stream().allMatch(p -> p.y() > fig.maxPanelBottom()));
        assertTrue(fig.textAt("mpg").stream().allMatch(p -> p.x() < fig.minPanelX()));
    }

    @Test
    void switchXPlacesLabelsAndTitleOnTop() {
        var fig = parse(renderSvg(gridPlot(GridSwitch.X)));
        assertTrue(fig.countNumericAbove(fig.minPanelY()) > 0, "x labels move above the top row");
        assertEquals(0, fig.countNumericBelow(fig.maxPanelBottom()));
        assertTrue(fig.anyNumericLeftOf(fig.minPanelX()), "y labels stay on the left");
        assertEquals(0, fig.countNumericRightOf(fig.maxPanelRight()));
        assertTrue(fig.textAt("litres").stream().allMatch(p -> p.y() < fig.minPanelY()));
        assertTrue(fig.textAt("mpg").stream().allMatch(p -> p.x() < fig.minPanelX()));
    }

    @Test
    void switchYPlacesLabelsAndTitleOnRight() {
        var fig = parse(renderSvg(gridPlot(GridSwitch.Y)));
        assertTrue(fig.countNumericRightOf(fig.maxPanelRight()) > 0, "y labels move right of the right column");
        assertEquals(0, fig.countNumericLeftOf(fig.minPanelX()));
        assertTrue(fig.anyNumericBelow(fig.maxPanelBottom()), "x labels stay on the bottom");
        assertEquals(0, fig.countNumericAbove(fig.minPanelY()));
        assertTrue(fig.textAt("mpg").stream().allMatch(p -> p.x() > fig.maxPanelRight()));
        assertTrue(fig.textAt("litres").stream().allMatch(p -> p.y() > fig.maxPanelBottom()));
    }

    @Test
    void switchBothPlacesBothAxesOnTheOuterEdges() {
        var fig = parse(renderSvg(gridPlot(GridSwitch.BOTH)));
        assertTrue(fig.countNumericAbove(fig.minPanelY()) > 0, "x labels move above the top row");
        assertTrue(fig.countNumericRightOf(fig.maxPanelRight()) > 0, "y labels move right of the right column");
        assertEquals(0, fig.countNumericBelow(fig.maxPanelBottom()), "no x labels linger on the bottom");
        assertEquals(0, fig.countNumericLeftOf(fig.minPanelX()), "no y labels linger on the left");
        assertTrue(fig.textAt("litres").stream().allMatch(p -> p.y() < fig.minPanelY()));
        assertTrue(fig.textAt("mpg").stream().allMatch(p -> p.x() > fig.maxPanelRight()));
    }

    private record ParsedFigure(double minPanelX, double minPanelY, double maxPanelRight,
                                double maxPanelBottom, List<TextPos> texts) {

        long countNumericAbove(double y) {
            return texts.stream().filter(p -> NUMERIC.matcher(p.content()).matches() && p.y() < y).count();
        }

        long countNumericBelow(double y) {
            return texts.stream().filter(p -> NUMERIC.matcher(p.content()).matches() && p.y() > y).count();
        }

        long countNumericLeftOf(double x) {
            return texts.stream().filter(p -> NUMERIC.matcher(p.content()).matches() && p.x() < x).count();
        }

        long countNumericRightOf(double x) {
            return texts.stream().filter(p -> NUMERIC.matcher(p.content()).matches() && p.x() > x).count();
        }

        boolean anyNumericBelow(double y) {
            return countNumericBelow(y) > 0;
        }

        boolean anyNumericLeftOf(double x) {
            return countNumericLeftOf(x) > 0;
        }

        List<TextPos> textAt(String content) {
            return texts.stream().filter(p -> p.content().equals(content)).toList();
        }
    }

    private record TextPos(double x, double y, boolean rotated, String content) {}
}
