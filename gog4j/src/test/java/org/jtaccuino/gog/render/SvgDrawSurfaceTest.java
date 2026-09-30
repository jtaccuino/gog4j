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
package org.jtaccuino.gog.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SVG backend that need no JavaFX toolkit: they exercise
 * document structure, the state stack, and escaping.
 */
class SvgDrawSurfaceTest {

    private static SvgDrawSurface surface() {
        return new SvgDrawSurface(200, 100, Color.WHITE);
    }

    /** Parses the SVG, which fails loudly on unbalanced or malformed elements. */
    private static void assertWellFormed(String svg) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newDocumentBuilder()
                   .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AssertionError("SVG is not well-formed XML: " + e.getMessage() + "\n" + svg, e);
        }
    }

    @Test
    void emptyDocumentIsWellFormedAndCarriesTheViewBox() {
        var svg = surface().toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("viewBox=\"0 0 200 100\""), svg);
        assertTrue(svg.contains("width=\"200\""), svg);
    }

    @Test
    void shapesCarryTheCurrentPaint() {
        var s = surface();
        s.setFill(Color.web("#ff0000"));
        s.fillRect(1, 2, 3, 4);
        s.setStroke(Color.web("#00ff00"));
        s.setLineWidth(2.5);
        s.strokeLine(0, 0, 10, 10);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("<rect x=\"1\" y=\"2\" width=\"3\" height=\"4\" fill=\"#ff0000\"/>"), svg);
        assertTrue(svg.contains("stroke=\"#00ff00\""), svg);
        assertTrue(svg.contains("stroke-width=\"2.5\""), svg);
    }

    @Test
    void translucentPaintBecomesASeparateOpacityAttribute() {
        var s = surface();
        s.setFill(Color.color(1, 0, 0, 0.5));
        s.fillRect(0, 0, 1, 1);

        var svg = s.toSvg();
        assertTrue(svg.contains("fill=\"#ff0000\""), svg);
        assertTrue(svg.contains("fill-opacity=\"0.5\""), svg);
    }

    @Test
    void saveAndRestoreCloseTheGroupsOpenedByTransforms() {
        var s = surface();
        s.save();
        s.translate(10, 20);
        s.rotate(-90);
        s.setFill(Color.BLUE);
        s.fillText("x", 0, 0);
        s.restore();
        s.fillRect(0, 0, 1, 1);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertEquals(2, count(svg, "<g transform="), "one group per transform");
        assertEquals(2, count(svg, "</g>"), "both groups closed again by restore");
        assertTrue(svg.indexOf("</g>") < svg.lastIndexOf("<rect"), "the rect is drawn outside the groups");
    }

    @Test
    void restoreAlsoRestoresPaintState() {
        var s = surface();
        s.setFill(Color.web("#111111"));
        s.save();
        s.setFill(Color.web("#222222"));
        s.restore();
        s.fillRect(0, 0, 1, 1);

        assertTrue(s.toSvg().contains("fill=\"#111111\""), "the pre-save fill is back");
    }

    @Test
    void unbalancedSaveStillProducesWellFormedOutput() {
        var s = surface();
        s.save();
        s.translate(5, 5);
        // deliberately no restore
        assertWellFormed(s.toSvg());
    }

    @Test
    void clipBecomesAClipPathReference() {
        var s = surface();
        s.save();
        s.beginPath();
        s.rect(10, 10, 50, 50);
        s.clip();
        s.setFill(Color.BLACK);
        s.fillRect(0, 0, 200, 100);
        s.restore();

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("<clipPath id=\"clip0\">"), svg);
        assertTrue(svg.contains("clip-path=\"url(#clip0)\""), svg);
    }

    @Test
    void pathsAccumulateAndEmitOnStroke() {
        var s = surface();
        s.beginPath();
        s.moveTo(0, 0);
        s.lineTo(10, 10);
        s.lineTo(20, 0);
        s.stroke();

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("d=\"M0 0 L10 10 L20 0\""), svg);
        assertTrue(svg.contains("fill=\"none\""), svg);
    }

    @Test
    void dashPatternIsCarriedThrough() {
        var s = surface();
        s.setLineDashes(6, 4);
        s.strokeLine(0, 0, 10, 0);
        assertTrue(s.toSvg().contains("stroke-dasharray=\"6,4\""), s.toSvg());
    }

    @Test
    void textAlignmentMapsToTextAnchor() {
        var s = surface();
        s.setTextAlign(TextAlignment.CENTER);
        s.fillText("mid", 50, 50);
        s.setTextAlign(TextAlignment.RIGHT);
        s.fillText("end", 50, 60);

        var svg = s.toSvg();
        assertTrue(svg.contains("text-anchor=\"middle\""), svg);
        assertTrue(svg.contains("text-anchor=\"end\""), svg);
    }

    @Test
    void textBaselineMapsToDominantBaseline() {
        var s = surface();
        s.setTextBaseline(VPos.CENTER);
        s.fillText("c", 0, 0);
        assertTrue(s.toSvg().contains("dominant-baseline=\"central\""), s.toSvg());
    }

    @Test
    void boldFontsAreMarkedAsSuch() {
        var s = surface();
        s.setFont(Font.font("System", FontWeight.BOLD, 12));
        s.fillText("b", 0, 0);
        var svg = s.toSvg();
        assertTrue(svg.contains("font-weight=\"bold\""), svg);
        // "System" means nothing to an SVG renderer
        assertTrue(svg.contains("font-family=\"sans-serif\""), svg);
    }

    @Test
    void markupInLabelsIsEscaped() {
        var s = surface();
        s.fillText("a < b & \"c\"", 0, 0);
        var svg = s.toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("a &lt; b &amp; &quot;c&quot;"), svg);
        assertFalse(svg.contains("a < b"), "raw markup must not survive");
    }

    @Test
    void batchedPointsShareOneStyledGroup() {
        var s = surface();
        s.setFill(Color.web("#123456"));
        s.setStroke(Color.web("#654321"));
        for (var i = 0; i < 5; i++) {
            s.fillOval(i, i, 4, 4);
            s.strokeOval(i, i, 4, 4);
        }

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertEquals(5, count(svg, "<circle"), "one circle per point");
        assertEquals(1, count(svg, "<g fill=\"#123456\""), "all five share a single styled group");
    }

    @Test
    void aChangeOfPaintStartsANewBatch() {
        var s = surface();
        s.setFill(Color.RED);
        s.fillOval(0, 0, 2, 2);
        s.strokeOval(0, 0, 2, 2);
        s.setFill(Color.BLUE);
        s.fillOval(5, 5, 2, 2);
        s.strokeOval(5, 5, 2, 2);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertEquals(2, count(svg, "<circle"), "no point is lost");
        assertEquals(1, count(svg, "#ff0000"), "the red point keeps its colour");
        assertEquals(1, count(svg, "#0000ff"), "the blue point keeps its colour");
    }

    @Test
    void aLoneSymbolCarriesItsPaintDirectlyRatherThanInAGroup() {
        var s = surface();
        s.setFill(Color.RED);
        s.fillOval(0, 0, 2, 2);
        s.strokeOval(0, 0, 2, 2);

        var svg = s.toSvg();
        assertEquals(1, count(svg, "<circle"), svg);
        assertEquals(0, count(svg, "<g fill="), "a single symbol needs no wrapping group");
        assertTrue(svg.contains("<circle cx=\"1\" cy=\"1\" r=\"1\" fill=\"#ff0000\""), svg);
    }

    @Test
    void squarePointsBatchLikeRoundOnes() {
        var s = surface();
        s.setFill(Color.web("#abcdef"));
        s.setStroke(Color.web("#123456"));
        for (var i = 0; i < 4; i++) {
            s.fillRect(i * 10, 0, 5, 5);
            s.strokeRect(i * 10, 0, 5, 5);
        }

        var svg = s.toSvg();
        assertWellFormed(svg);
        // "<rect x=" excludes the document's own full-bleed background rect
        assertEquals(4, count(svg, "<rect x="), "one rect per square point");
        assertEquals(1, count(svg, "<g fill=\"#abcdef\""), "all four share a styled group");
    }

    @Test
    void anUnpairedFillRectIsStillDrawn() {
        var s = surface();
        s.setFill(Color.web("#ebebeb"));
        s.fillRect(0, 0, 200, 100);
        s.setStroke(Color.BLACK);
        s.strokeLine(0, 0, 10, 10);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertTrue(svg.contains("<rect x=\"0\" y=\"0\" width=\"200\" height=\"100\" fill=\"#ebebeb\""),
                   "a panel background is not swallowed by symbol batching: " + svg);
    }

    @Test
    void anUnpairedFillOvalIsStillDrawn() {
        var s = surface();
        s.setFill(Color.RED);
        s.fillOval(0, 0, 2, 2);
        s.fillOval(9, 9, 2, 2);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertEquals(2, count(svg, "<circle"), "neither point is swallowed by batching");
    }

    @Test
    void batchingCanBeDisabled() {
        var s = surface().batchPoints(false);
        s.setFill(Color.RED);
        s.fillOval(0, 0, 2, 2);
        s.strokeOval(0, 0, 2, 2);

        var svg = s.toSvg();
        assertWellFormed(svg);
        assertEquals(2, count(svg, "<ellipse"), "fill and outline stay separate elements");
    }

    private static int count(String haystack, String needle) {
        var n = 0;
        var i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }
}
