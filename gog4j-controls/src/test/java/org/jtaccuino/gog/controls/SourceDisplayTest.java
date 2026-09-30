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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the drawer's source body extraction: the method wrapper and its
 * leading indentation are stripped, leaving just the statements.
 */
class SourceDisplayTest {

    private static final String METHOD = """
                public static Plot<DataFrame> createScatterPlot() {
                    var df = DiamondsDatasets.loadDiamonds();
                    return ggplot(df, aes().x("carat").y("price"))
                            .geoms(point())
                            .labs(labs("Price vs Carat", "Carat", "Price"));
                }
            """;

    @Test
    void stripsTheMethodWrapperAndDedentsTheBody() {
        var body = SourceDisplay.methodBody(METHOD);
        assertTrue(body.startsWith("var df = DiamondsDatasets.loadDiamonds();"),
                "wrapper must be removed, got:\n" + body);
        assertTrue(body.contains("return ggplot(df, aes().x(\"carat\").y(\"price\"))"),
                "body statements must be present:\n" + body);
        assertTrue(body.contains(".geoms(point())"),
                "continuation lines must be present:\n" + body);
        assertTrue(body.lines().noneMatch(l -> l.startsWith("    var ")),
                "body must be dedented to the method-body column:\n" + body);
        assertTrue(body.lines().noneMatch(l -> l.contains("public static")),
                "signature must be dropped");
        assertTrue(body.lines().noneMatch(l -> l.trim().equals("}")),
                "closing brace must be dropped");
    }

    @Test
    void handlesNestedBracesAndStrings() {
        var src = """
                    public static Plot<DataFrame> createWithBraces() {
                        var s = "{not a brace}";
                        if (true) {
                            return ggplot(df, aes().x("a").y("b"));
                        }
                        return null;
                    }
                """;
        var body = SourceDisplay.methodBody(src);
        assertEquals("""
                        var s = "{not a brace}";
                        if (true) {
                            return ggplot(df, aes().x("a").y("b"));
                        }
                        return null;
                        """,
                body);
    }

    @Test
    void blankSourceStaysBlank() {
        assertEquals("", SourceDisplay.methodBody(""));
        assertEquals("", SourceDisplay.methodBody(null));
    }

    @Test
    void nonMethodTextIsPassedThrough() {
        assertEquals("var x = 1;\n", SourceDisplay.methodBody("var x = 1;\n"));
    }
}
