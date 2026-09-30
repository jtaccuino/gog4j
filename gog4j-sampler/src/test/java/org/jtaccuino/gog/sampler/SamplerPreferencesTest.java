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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.prefs.Preferences;
import org.jtaccuino.gog.RenderMode;
import org.junit.jupiter.api.Test;

class SamplerPreferencesTest {

    @Test
    void roundTripsQueryAndTagIdsAndRenderMode() throws Exception {
        var node = Preferences.userRoot().node("/org/jtaccuino/gog/sampler/round-trip");
        node.clear();
        var prefs = new SamplerPreferences(node);
        try {
            prefs.saveQuery("boxplot   mpg");
            prefs.saveTagIds(List.of("geom:boxplot", "dataset:mpg"));
            prefs.saveRenderMode(RenderMode.FULL);
            prefs.saveDarkTheme(true);

            assertEquals("boxplot   mpg", prefs.query(),
                    "query must round-trip exactly");
            assertEquals(List.of("geom:boxplot", "dataset:mpg"), prefs.tagIds(),
                    "tag ids must round-trip in order");
            assertEquals(RenderMode.FULL, prefs.renderMode(),
                    "render mode must round-trip");
            assertTrue(prefs.darkTheme(), "the dark theme must round-trip");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void defaultsAreFastEmptyAndNoTags() throws Exception {
        var node = Preferences.userRoot().node("/org/jtaccuino/gog/sampler/defaults");
        node.clear();
        var prefs = new SamplerPreferences(node);
        try {
            assertEquals("", prefs.query(), "no saved query must read back as empty");
            assertTrue(prefs.tagIds().isEmpty(), "no saved tags must read back empty");
            assertEquals(RenderMode.FAST, prefs.renderMode(),
                    "no saved mode must read back as FAST");
            assertTrue(!prefs.darkTheme(), "no saved theme must read back as light");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void clearResetsEveryKeyToItsDefault() throws Exception {
        var node = Preferences.userRoot().node("/org/jtaccuino/gog/sampler/clear");
        node.clear();
        var prefs = new SamplerPreferences(node);
        prefs.saveQuery("mpg");
        prefs.saveTagIds(List.of("geom:point"));
        prefs.saveRenderMode(RenderMode.FULL);
        prefs.saveDarkTheme(true);
        try {
            prefs.clear();
            assertEquals("", prefs.query(), "clear must wipe the query");
            assertTrue(prefs.tagIds().isEmpty(), "clear must wipe the tags");
            assertEquals(RenderMode.FAST, prefs.renderMode(),
                    "clear must restore the default render mode");
            assertTrue(!prefs.darkTheme(), "clear must restore the light theme");
        } finally {
            node.removeNode();
        }
    }
}
