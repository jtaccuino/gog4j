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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Highlights Java factory-method bodies in the sampler's source view. The
 * tokenizer is deliberately small — keywords, strings/char/text-blocks,
 * comments, numbers, {@code @annotations} and identifiers invoked as methods —
 * and colors come from the shared {@link SourceTheme}. {@code CodeArea}
 * supports only a foreground color (plus italic/bold), so comments are
 * additionally italicised to stand apart.
 */
final class JavaSyntaxDecorator implements SyntaxDecorator {

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "permits",
            "private", "protected", "public", "record", "return", "sealed",
            "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "var", "void",
            "volatile", "while", "yield", "true", "false", "null");

    /** One colored run of text; {@code italic} flags comments. */
    record Segment(String text, Color color, boolean italic) {
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        String line = model.getPlainText(index);
        var builder = RichParagraph.builder();
        for (var segment : tokenize(line, SourceTheme.CURRENT.get())) {
            if (segment.italic()) {
                builder.addSegment(segment.text(), StyleAttributeMap.builder()
                        .set(StyleAttributeMap.TEXT_COLOR, segment.color())
                        .set(StyleAttributeMap.ITALIC, Boolean.TRUE)
                        .build());
            } else {
                builder.addSegment(segment.text(),
                        StyleAttributeMap.of(StyleAttributeMap.TEXT_COLOR, segment.color()));
            }
        }
        if (builder.getSegmentCount() == 0) {
            builder.addSegment("");
        }
        return builder.build();
    }

    @Override
    public void handleChange(CodeTextModel model, TextPos start, TextPos end,
            int charsTop, int linesAdded, int charsBottom) {
        // Styles are derived per paragraph on demand; nothing to invalidate.
    }

    /**
     * Splits one line of Java source into styled runs. The returned segments
     * cover the line exactly, in order, so they can be re-assembled verbatim.
     *
     * @param line the source line
     * @param theme the color scheme
     * @return the styled segments
     */
    static List<Segment> tokenize(String line, SourceTheme theme) {
        var segments = new ArrayList<Segment>();
        var plain = new StringBuilder();
        int i = 0;
        int n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < n) {
                if (line.charAt(i + 1) == '/') {
                    flushPlain(segments, plain, theme.text());
                    segments.add(new Segment(line.substring(i), theme.comment(), true));
                    break;
                }
                if (line.charAt(i + 1) == '*') {
                    flushPlain(segments, plain, theme.text());
                    int close = line.indexOf("*/", i + 2);
                    int end = close < 0 ? n : close + 2;
                    segments.add(new Segment(line.substring(i, end), theme.comment(), true));
                    i = end;
                    continue;
                }
            }
            if (c == '"') {
                flushPlain(segments, plain, theme.text());
                int end = skipString(line, i);
                segments.add(new Segment(line.substring(i, end), theme.string(), false));
                i = end;
                continue;
            }
            if (c == '\'') {
                flushPlain(segments, plain, theme.text());
                int end = skipCharLiteral(line, i);
                segments.add(new Segment(line.substring(i, end), theme.string(), false));
                i = end;
                continue;
            }
            if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(line.charAt(i + 1))) {
                flushPlain(segments, plain, theme.text());
                int end = i + 1;
                while (end < n && (Character.isJavaIdentifierPart(line.charAt(end))
                        || line.charAt(end) == '.')) {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), theme.annotation(), false));
                i = end;
                continue;
            }
            if (Character.isDigit(c)
                    || (c == '.' && i + 1 < n && Character.isDigit(line.charAt(i + 1)))) {
                flushPlain(segments, plain, theme.text());
                int end = i;
                while (end < n && isNumberChar(line.charAt(end))) {
                    end++;
                }
                segments.add(new Segment(line.substring(i, end), theme.number(), false));
                i = end;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < n && Character.isJavaIdentifierPart(line.charAt(i))) {
                    i++;
                }
                String word = line.substring(start, i);
                if (KEYWORDS.contains(word)) {
                    flushPlain(segments, plain, theme.text());
                    segments.add(new Segment(word, theme.keyword(), false));
                } else if (followedByCall(line, i)) {
                    flushPlain(segments, plain, theme.text());
                    segments.add(new Segment(word, theme.method(), false));
                } else {
                    plain.append(word);
                }
                continue;
            }
            plain.append(c);
            i++;
        }
        flushPlain(segments, plain, theme.text());
        return segments;
    }

    private static void flushPlain(List<Segment> segments, StringBuilder plain, Color color) {
        if (plain.length() > 0) {
            segments.add(new Segment(plain.toString(), color, false));
            plain.setLength(0);
        }
    }

    /** Whether the identifier at {@code wordEnd} is immediately followed by {@code (}. */
    private static boolean followedByCall(String line, int wordEnd) {
        int j = wordEnd;
        while (j < line.length() && Character.isWhitespace(line.charAt(j))) {
            j++;
        }
        return j < line.length() && line.charAt(j) == '(';
    }

    private static boolean isNumberChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_';
    }

    /** Returns the index just past the (possibly text-block) string at {@code index}. */
    private static int skipString(String line, int index) {
        if (line.startsWith("\"\"\"", index)) {
            int close = line.indexOf("\"\"\"", index + 3);
            return close < 0 ? line.length() : close + 3;
        }
        int i = index + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return line.length();
    }

    /** Returns the index just past the char literal at {@code index}. */
    private static int skipCharLiteral(String line, int index) {
        int i = index + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '\'') {
                return i + 1;
            }
            i++;
        }
        return line.length();
    }
}
