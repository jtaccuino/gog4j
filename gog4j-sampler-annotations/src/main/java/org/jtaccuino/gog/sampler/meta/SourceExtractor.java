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
package org.jtaccuino.gog.sampler.meta;

/**
 * Extracts a single {@code public static} factory method (returning a
 * {@code Plot}, {@code ComposedPlot}, or {@code PlotMatrix}) from the text
 * of its Java source file, including any javadoc that immediately precedes it.
 * Used by {@link SampleRegistryProcessor} to bake the method source into the
 * generated registry.
 */
final class SourceExtractor {

    private SourceExtractor() {
    }

    static String extractMethod(String source, String methodName) {
        int signature = findSignature(source, methodName);
        if (signature < 0) {
            return "";
        }
        int open = source.indexOf('{', signature);
        if (open < 0) {
            return "";
        }
        int close = matchBrace(source, open + 1);
        if (close < 0) {
            return "";
        }
        int end = source.indexOf('\n', close);
        end = end < 0 ? source.length() : end + 1;
        int start = source.lastIndexOf('\n', signature) + 1;
        start = includePrecedingJavadoc(source, start);
        return source.substring(start, end);
    }

    private static int findSignature(String source, String methodName) {
        int searchFrom = 0;
        while (true) {
            int name = source.indexOf(methodName, searchFrom);
            if (name < 0) {
                return -1;
            }
            if (isFactorySignature(source, name)) {
                return name;
            }
            searchFrom = name + methodName.length();
        }
    }

    private static boolean isFactorySignature(String source, int name) {
        int lineStart = source.lastIndexOf('\n', name) + 1;
        var line = source.substring(lineStart, name);
        if (!line.contains("public static ")) {
            return false;
        }
        int paren = source.indexOf('(', name);
        if (paren < 0) {
            return false;
        }
        for (int i = name; i < paren; i++) {
            if (source.charAt(i) == '\n') {
                return false;
            }
        }
        return true;
    }

    private static int includePrecedingJavadoc(String source, int signatureLineStart) {
        int cursor = signatureLineStart;
        while (cursor > 0) {
            if (source.charAt(cursor - 1) == '\n') {
                cursor--;
                continue;
            }
            if (Character.isWhitespace(source.charAt(cursor - 1))) {
                cursor--;
                continue;
            }
            break;
        }
        if (cursor > 0 && source.startsWith("*/", cursor - 2)) {
            int commentEnd = cursor - 2;
            int open = source.lastIndexOf("/**", commentEnd);
            if (open >= 0) {
                int lineStart = source.lastIndexOf('\n', open) + 1;
                var between = source.substring(commentEnd, signatureLineStart - 1);
                if (between.isBlank()) {
                    return lineStart;
                }
            }
        }
        return signatureLineStart;
    }

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
