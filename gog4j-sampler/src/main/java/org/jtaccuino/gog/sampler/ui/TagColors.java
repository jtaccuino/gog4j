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
package org.jtaccuino.gog.sampler.ui;

import java.util.EnumMap;
import java.util.Map;
import org.jtaccuino.gog.sampler.meta.TagKind;

/**
 * Central colour scheme for tag chips. Each {@link TagKind} gets a soft pastel
 * background so the category of every tag is visible at a glance; both the
 * in-card tag chips and the active filter chips share this single source of
 * truth.
 */
final class TagColors {

    private static final Map<TagKind, String> BACKGROUNDS = new EnumMap<>(Map.of(
            TagKind.DATASET, "#cdeccd",
            TagKind.GEOM, "#ffd9b8",
            TagKind.STAT, "#d9e8c9",
            TagKind.SCALE, "#fff2b8",
            TagKind.COORD, "#e0d6f7",
            TagKind.FACET, "#c9d4f5",
            TagKind.POSITION, "#d6f0e3",
            TagKind.THEME, "#e8e8e8",
            TagKind.GUIDE, "#f8cdd6",
            TagKind.FEATURE, "#c9eef0"));

    private TagColors() {
    }

    /** The pastel background colour for chips of the given kind. */
    static String forKind(TagKind kind) {
        return BACKGROUNDS.getOrDefault(kind, "#ececec");
    }
}
