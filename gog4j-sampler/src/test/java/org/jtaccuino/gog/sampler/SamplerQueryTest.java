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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies the pure text/tag matching rules used to filter the sampler list.
 */
class SamplerQueryTest {

    private static final Tag FAKE = new Tag("geom:point", "Point", org.jtaccuino.gog.sampler.meta.TagKind.GEOM);

    private static SamplerExample example(String title, String description) {
        return new SamplerExample(title, description, "createExample", () -> new Object(),
                List.of(FAKE), "source");
    }

    @Test
    void blankQueryMatchesEverything() {
        var ex = example("Title", "some description");
        assertTrue(SamplerQuery.matchesText(ex, null));
        assertTrue(SamplerQuery.matchesText(ex, "  "));
    }

    @Test
    void matchesOnTitleAndMethodName() {
        var ex = example("Notched Boxplot", null);
        assertTrue(SamplerQuery.matchesText(ex, "notched"));
        assertTrue(SamplerQuery.matchesText(ex, "createexample"));
    }

    @Test
    void matchesOnDescriptionText() {
        var ex = example("Some Plot", "a loess smoothing fit through the points");
        assertTrue(SamplerQuery.matchesText(ex, "loess"),
                "description text must be part of the search haystack");
        assertTrue(SamplerQuery.matchesText(ex, "SMOOTHING"),
                "description search must be case-insensitive");
    }

    @Test
    void matchesOnTagNameAndLabel() {
        var ex = example("Title", null);
        assertTrue(SamplerQuery.matchesText(ex, "geom:point"));
        assertTrue(SamplerQuery.matchesText(ex, "point"));
    }

    @Test
    void nonMatchingTextFails() {
        var ex = example("Title", "description");
        assertFalse(SamplerQuery.matchesText(ex, "nothing-here"));
    }

    @Test
    void matchesRequiresEveryActiveTag() {
        var ex = example("Title", null);
        assertTrue(SamplerQuery.matches(ex, "", List.of(FAKE)));
        var other = new Tag("geom:boxplot", "Boxplot", org.jtaccuino.gog.sampler.meta.TagKind.GEOM);
        assertFalse(SamplerQuery.matches(ex, "", List.of(FAKE, other)),
                "an example missing an active tag must not match");
        assertFalse(SamplerQuery.matches(ex, "unmatched", List.of()),
                "an example failing the text query must not match");
    }
}
