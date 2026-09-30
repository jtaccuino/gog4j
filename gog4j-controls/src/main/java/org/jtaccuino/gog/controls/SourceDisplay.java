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

/**
 * Turns a baked-in factory-method source into the compact body shown in the
 * source drawer: the {@code public static ... () { } } wrapper is dropped and
 * the statements are dedented to the method-body column.
 */
final class SourceDisplay {

    private SourceDisplay() {
    }

    /**
     * Returns the body of the given factory method with the signature line(s)
     * and enclosing braces removed and the statements dedented.
     *
     * @param source the baked method source (signature through closing brace)
     * @return the method body, or the original text if no method shape is found
     */
    static String methodBody(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        int signature = source.indexOf("public static ");
        if (signature < 0) {
            return source;
        }
        int open = source.indexOf('{', signature);
        if (open < 0) {
            return source;
        }
        int close = matchBrace(source, open + 1);
        if (close < 0) {
            return source;
        }
        var body = source.substring(open + 1, close);
        // The body starts on the line after the signature's opening brace.
        while (body.startsWith("\n") || body.startsWith("\r")) {
            body = body.substring(1);
        }
        return dedent(body);
    }

    /**
     * Removes the common leading indentation of non-blank lines.
     *
     * @param text the text block to dedent
     * @return the dedented text
     */
    private static String dedent(String text) {
        var lines = text.split("\n", -1);
        int min = Integer.MAX_VALUE;
        for (var line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int lead = 0;
            while (lead < line.length() && line.charAt(lead) == ' ') {
                lead++;
            }
            min = Math.min(min, lead);
        }
        if (min == Integer.MAX_VALUE) {
            min = 0;
        }
        var out = new StringBuilder();
        for (var line : lines) {
            int cut = Math.min(min, line.length());
            out.append(line.substring(cut)).append('\n');
        }
        // The wrapper's closing brace leaves a trailing indented blank line;
        // strip it (and any other trailing whitespace) off the display text.
        return out.toString().stripTrailing() + "\n";
    }

    /**
     * Matches the closing brace of the method body, skipping strings, comments,
     * and nested braces.
     *
     * @param source the full method source
     * @param index the index just after the opening brace
     * @return the index of the matching closing brace, or {@code -1}
     */
    private static int matchBrace(String source, int index) {
        int depth = 1;
        int i = index;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"') {
                i = skipString(source, i);
                continue;
            }
            if (c == '/' && i + 1 < source.length()) {
                if (source.charAt(i + 1) == '/') {
                    i = source.indexOf('\n', i);
                    if (i < 0) {
                        return -1;
                    }
                    continue;
                }
                if (source.charAt(i + 1) == '*') {
                    int close = source.indexOf("*/", i + 2);
                    if (close < 0) {
                        return -1;
                    }
                    i = close + 2;
                    continue;
                }
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    private static int skipString(String source, int index) {
        if (source.startsWith("\"\"\"", index)) {
            int close = source.indexOf("\"\"\"", index + 3);
            return close < 0 ? source.length() : close + 3;
        }
        int i = index + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return source.length();
    }
}
