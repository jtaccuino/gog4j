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
package org.jtaccuino.gog.sampler.registry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFacet;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SampleGuide;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.sampler.meta.SampleStat;
import org.jtaccuino.gog.sampler.meta.SampleTag;
import org.jtaccuino.gog.sampler.meta.SampleTheme;
import org.jtaccuino.gog.sampler.meta.TagKind;

/**
 * The runtime facade over the per-kind {@code Sample*} tag catalogues. Every
 * registry tag id resolves against exactly one enum constant; {@link #all()}
 * builds the canonical {@link Tag} list and {@link #byName(String)} translates
 * the processor's comma-separated ids ({@code "<kind>:<feature>"}, e.g.
 * {@code "geom:boxplot"}) back into {@link Tag}s.
 */
public final class Tags {

    // Fixed kind order for all(); dataset always first, mirroring the processor.
    private static final TagKind[] KIND_ORDER = {
            TagKind.DATASET, TagKind.GEOM, TagKind.STAT, TagKind.COORD,
            TagKind.SCALE, TagKind.POSITION, TagKind.THEME, TagKind.GUIDE,
            TagKind.FEATURE, TagKind.FACET
    };

    private static final Map<String, Tag> BY_ID = byId();

    private Tags() {
    }

    /**
     * Returns every tag in the catalogue, in fixed kind order with the dataset
     * kind first.
     *
     * @return all tags derived from the {@code Sample*} enums
     */
    public static List<Tag> all() {
        return Arrays.stream(KIND_ORDER)
                .flatMap(kind -> Arrays.stream(catalogue(kind))
                        .map(sample -> new Tag(sample.id(), sample.label(), kind)))
                .toList();
    }

    /**
     * Returns only the catalogue tags that at least one example actually uses,
     * still in fixed kind order. The full catalogues contain constants no
     * example references (e.g. {@code geom:none}, {@code theme:gray}, some
     * {@code guide:placement-*} variants); the filter UI should not offer tags
     * that can never match an example.
     *
     * @param examples the registered examples
     * @return the used tags in catalogue order
     */
    public static List<Tag> usedBy(List<SamplerExample> examples) {
        var used = new HashSet<String>();
        for (var example : examples) {
            for (var tag : example.tags()) {
                used.add(tag.name());
            }
        }
        return all().stream()
                .filter(tag -> used.contains(tag.name()))
                .toList();
    }

    /**
     * Resolves a tag by its {@code <kind>:<feature>} id.
     *
     * @param id the tag id, e.g. {@code "geom:boxplot"}
     * @return the matching tag
     * @throws IllegalArgumentException if no tag in the catalogue has that id
     */
    public static Tag byName(String id) {
        var tag = BY_ID.get(id);
        if (tag == null) {
            throw new IllegalArgumentException("unknown tag id: " + id);
        }
        return tag;
    }

    private static Map<String, Tag> byId() {
        return all().stream().collect(Collectors.toUnmodifiableMap(Tag::name, tag -> tag));
    }

    private static SampleTag[] catalogue(TagKind kind) {
        return switch (kind) {
            case DATASET -> SampleDataset.values();
            case GEOM -> SampleGeom.values();
            case STAT -> SampleStat.values();
            case SCALE -> SampleScale.values();
            case COORD -> SampleCoord.values();
            case FACET -> SampleFacet.values();
            case POSITION -> SamplePosition.values();
            case THEME -> SampleTheme.values();
            case GUIDE -> SampleGuide.values();
            case FEATURE -> SampleFeature.values();
        };
    }
}
