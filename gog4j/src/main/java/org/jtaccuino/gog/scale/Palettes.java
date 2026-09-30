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
package org.jtaccuino.gog.scale;

import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;

/**
 * A registry of named colour palettes used to drive both the discrete
 * categorical scales ({@code scale_*_brewer}) and the continuous colour ramps
 * ({@code scale_*_viridis_c}, {@code scale_*_gradient}). Each entry is a list of
 * colours in ramp (low-to-high) order.
 * <p>
 * The sequential and diverging sets mirror the ColorBrewer palettes of the
 * same name; the qualitative sets (Set1, Set2, Set3, Dark2, Paired) are
 * categorical palettes with no natural ordering.
 */
public final class Palettes {

    /** Utility class; not meant to be instantiated. */
    private Palettes() {
    }

    // Sequential continuous ramps (shared with ContinuousColorScale), diverging
    // and qualitative ColorBrewer palettes.
    private static final Map<String, List<Color>> PALETTES = Map.ofEntries(
        Map.entry("viridis", List.of(
            Color.web("#440154"), Color.web("#3b528b"), Color.web("#21918c"),
            Color.web("#5ec962"), Color.web("#fde725"))),
        Map.entry("plasma", List.of(
            Color.web("#0d0887"), Color.web("#6a00a8"), Color.web("#b12a90"),
            Color.web("#e16462"), Color.web("#fca636"))),
        Map.entry("rdylbu", List.of(
            Color.web("#d73027"), Color.web("#fc8d59"), Color.web("#fee090"),
            Color.web("#e0f3f8"), Color.web("#91bfdb"), Color.web("#4575b4"))),
        Map.entry("rdgy", List.of(
            Color.web("#b2182b"), Color.web("#ef8a62"), Color.web("#fddbc7"),
            Color.web("#e0e0e0"), Color.web("#999999"), Color.web("#4d4d4d"))),
        Map.entry("rdbu", List.of(
            Color.web("#b2182b"), Color.web("#ef8a62"), Color.web("#fddbc7"),
            Color.web("#d1e5f0"), Color.web("#67a9cf"), Color.web("#2166ac"))),
        Map.entry("set1", List.of(
            Color.web("#e41a1c"), Color.web("#377eb8"), Color.web("#4daf4a"),
            Color.web("#984ea3"), Color.web("#ff7f00"), Color.web("#ffff33"),
            Color.web("#a65628"), Color.web("#f781bf"), Color.web("#999999"))),
        Map.entry("set2", List.of(
            Color.web("#66c2a5"), Color.web("#fc8d62"), Color.web("#8da0cb"),
            Color.web("#e78ac3"), Color.web("#a6d854"), Color.web("#ffd92f"),
            Color.web("#e5c494"), Color.web("#b3b3b3"))),
        Map.entry("set3", List.of(
            Color.web("#8dd3c7"), Color.web("#ffffb3"), Color.web("#bebada"),
            Color.web("#fb8072"), Color.web("#80b1d3"), Color.web("#fdb462"),
            Color.web("#b3de69"), Color.web("#fccde5"), Color.web("#d9d9d9"))),
        Map.entry("dark2", List.of(
            Color.web("#1b9e77"), Color.web("#d95f02"), Color.web("#7570b3"),
            Color.web("#e7298a"), Color.web("#66a61e"), Color.web("#e6ab02"),
            Color.web("#a6761d"), Color.web("#666666"))),
        Map.entry("paired", List.of(
            Color.web("#a6cee3"), Color.web("#1f78b4"), Color.web("#b2df8a"),
            Color.web("#33a02c"), Color.web("#fb9a99"), Color.web("#e31a1c"),
            Color.web("#fdbf6f"), Color.web("#ff7f00"), Color.web("#cab2d6"))));

    /**
     * Returns whether a named palette exists.
     *
     * @param name the palette name (case-insensitive)
     * @return {@code true} if the palette is registered
     */
    public static boolean exists(String name) {
        return resolveKey(name) != null;
    }

    /**
     * Returns the colours of a named palette, or {@code null} when the name is
     * unknown.
     *
     * @param name the palette name (case-insensitive, spaces/hyphens ignored)
     * @return the palette colours in ramp order, or {@code null}
     */
    public static List<Color> of(String name) {
        var key = resolveKey(name);
        return key == null ? null : PALETTES.get(key);
    }

    private static String resolveKey(String name) {
        if (name == null) {
            return null;
        }
        var normalized = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s_-]", "");
        for (var entry : PALETTES.entrySet()) {
            if (entry.getKey().equals(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
