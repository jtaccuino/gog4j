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
package org.jtaccuino.gog.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JavaSyntaxDecoratorTest {

    @AfterEach
    void restoreLightTheme() {
        // The shared theme is global state; never leak a dark selection into
        // other suites that assert on the light palette.
        SourceTheme.CURRENT.set(SourceTheme.LIGHT);
    }

    @BeforeAll
    static void startToolkit() {
        // Constructing a Control (CodeArea) requires the toolkit; keep the
        // shared test JVM from poisoning Control.<clinit> for the other suites.
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assumeTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        } catch (IllegalStateException alreadyRunning) {
            // already up
        } catch (UnsupportedOperationException | InterruptedException noToolkit) {
            assumeTrue(false, "No JavaFX toolkit available: " + noToolkit.getMessage());
        }
    }

    private static void onFx(Runnable r) throws Exception {
        var error = new Throwable[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(60, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw new AssertionError(error[0]);
        }
    }

    @Test
    void keywordsStringsNumbersAndMethodsGetTheirLightColors() {
        var tokens = JavaSyntaxDecorator.tokenize(
                "var df = MpgDatasets.loadMpg(); // load",
                SourceTheme.LIGHT);
        assertEquals(SourceTheme.LIGHT.keyword(), colorOf(tokens, "var"));
        assertEquals(SourceTheme.LIGHT.method(), colorOf(tokens, "loadMpg"));
        assertTrue(tokens.stream()
                .filter(t -> t.text().contains("df"))
                .allMatch(t -> SourceTheme.LIGHT.text().equals(t.color())),
                "plain identifiers must keep the default text color");
    }

    @Test
    void stringsCommentsAndNumbersAreStyled() {
        var tokens = JavaSyntaxDecorator.tokenize(
                "s = \"a\\\"b\" + 42; // note\n",
                SourceTheme.LIGHT);
        assertEquals(SourceTheme.LIGHT.string(), colorOf(tokens, "\"a\\\"b\""));
        assertEquals(SourceTheme.LIGHT.number(), colorOf(tokens, "42"));
        var comment = tokens.stream()
                .filter(t -> t.text().contains("//"))
                .findFirst()
                .orElseThrow();
        assertEquals(SourceTheme.LIGHT.comment(), comment.color());
        assertTrue(comment.italic(), "comments must be italicised");
    }

    @Test
    void annotationsAreStyledAndTextBlocksAreKeptAsStrings() {
        var tokens = JavaSyntaxDecorator.tokenize(
                "@Override\nString s = \"\"\"\n    text\n\"\"\";",
                SourceTheme.DARK);
        assertEquals(SourceTheme.DARK.annotation(), colorOf(tokens, "@Override"));
        assertTrue(tokens.stream().anyMatch(t -> t.text().startsWith("\"\"\"")),
                "a text block must be tokenised as a string");
    }

    @Test
    void darkThemeUsesDarculaColors() {
        var tokens = JavaSyntaxDecorator.tokenize("return new Plot();", SourceTheme.DARK);
        assertEquals(SourceTheme.DARK.keyword(), colorOf(tokens, "return"));
        assertEquals(SourceTheme.DARK.keyword(), colorOf(tokens, "new"));
        assertEquals(SourceTheme.DARK.method(), colorOf(tokens, "Plot"));
    }

    @Test
    void segmentsReassembleTheLineVerbatim() {
        String line = "var x = foo(1, \"two\"); // done";
        var tokens = JavaSyntaxDecorator.tokenize(line, SourceTheme.LIGHT);
        assertEquals(line, tokens.stream()
                .map(JavaSyntaxDecorator.Segment::text)
                .collect(Collectors.joining()));
    }

    @Test
    void decoratorBuildsAHighlightedParagraphOnTheModel() throws Exception {
        onFx(() -> {
            var area = new CodeArea();
            area.setSyntaxDecorator(new JavaSyntaxDecorator());
            area.setText("var df = load();\n");
            var model = (CodeTextModel) area.getModel();
            var paragraph = model.getParagraph(0);
            assertEquals("var df = load();", paragraph.getPlainText());
            var resolver = new StyleResolver() {
                @Override
                public StyleAttributeMap resolveStyles(StyleAttributeMap attrs) {
                    return attrs;
                }

                @Override
                public WritableImage snapshot(javafx.scene.Node node) {
                    return null;
                }
            };
            boolean foundKeyword = false;
            for (int i = 0; i < paragraph.getSegmentCount(); i++) {
                var segment = paragraph.getSegment(i);
                if ("var".equals(segment.getText())) {
                    foundKeyword = true;
                    var style = segment.getStyleAttributeMap(resolver);
                    assertNotNull(style);
                    assertEquals(SourceTheme.LIGHT.keyword(), style.getTextColor(),
                            "the var keyword must carry the keyword color");
                }
            }
            assertTrue(foundKeyword, "the paragraph must expose a styled var token");
        });
    }

    private static Color colorOf(List<JavaSyntaxDecorator.Segment> tokens, String text) {
        return tokens.stream()
                .filter(t -> t.text().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no token found for '" + text + "'"))
                .color();
    }
}
