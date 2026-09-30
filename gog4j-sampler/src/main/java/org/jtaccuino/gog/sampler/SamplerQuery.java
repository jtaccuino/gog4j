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
package org.jtaccuino.gog.sampler;

import java.util.List;
import java.util.Locale;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;

/**
 * Pure filtering predicates for the sampler list, kept free of JavaFX so the
 * matching rules can be unit-tested without a running toolkit.
 */
final class SamplerQuery {

    private SamplerQuery() {
    }

    /**
     * Returns whether the example passes the text query and every active tag.
     *
     * @param example the example to test
     * @param query the free-text search query, or {@code null}/blank for none
     * @param activeTags the tags that must all be present on the example
     * @return {@code true} if the example matches
     */
    static boolean matches(SamplerExample example, String query, List<Tag> activeTags) {
        if (!matchesText(example, query)) {
            return false;
        }
        for (var active : activeTags) {
            if (!example.tags().contains(active)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the free-text query is contained in the example's title,
     * method name, description, or any tag name/label, case-insensitively.
     *
     * @param example the example to test
     * @param query the free-text search query, or {@code null}/blank for none
     * @return {@code true} if the text matches
     */
    static boolean matchesText(SamplerExample example, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        var needle = query.trim().toLowerCase(Locale.ROOT);
        var haystack = (example.displayTitle() + " " + example.methodName() + " "
                + (example.description() == null ? "" : example.description()))
                .toLowerCase(Locale.ROOT)
                + " " + example.tags().stream()
                        .map(tag -> tag.name() + " " + tag.label())
                        .reduce("", (a, b) -> a + " " + b)
                        .toLowerCase(Locale.ROOT);
        return haystack.contains(needle);
    }
}
