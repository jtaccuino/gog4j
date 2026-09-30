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
 * The {@link TagKind#GUIDE} catalogue: the legend/guide placement, key, layout,
 * and overflow strategies the examples demonstrate.
 */
public enum SampleGuide implements SampleTag {

    DEFAULT("placement-default", "Default Placement"),
    TOP("placement-top", "Legend on Top"),
    BOTTOM("placement-bottom", "Legend on Bottom"),
    LEFT("placement-left", "Legend on the Left"),
    RIGHT("placement-right", "Legend on the Right"),
    INSIDE("placement-inside", "Legend Inside"),
    ANCHORED("placement-anchored", "Anchored"),
    STACKED("layout-stacked", "Stacked Guides"),
    TWO_COLUMN("layout-two-column", "Two-Column"),
    BY_ROWS("layout-by-rows", "By Rows"),
    REVERSED("layout-reversed", "Reversed Order"),
    BOXED("layout-boxed", "Legend Box"),
    MERGED("keys-merged", "Merged Keys"),
    SEPARATE("keys-separate", "Separate Guides"),
    PER_AESTHETIC("keys-per-aesthetic", "Per-Aesthetic Titles"),
    KEY_COLOUR("keys-colour", "Colour Keys"),
    KEY_SHAPE("keys-shape", "Shape Keys"),
    KEY_SIZE("keys-size", "Size Keys"),
    KEY_ALPHA("keys-alpha", "Alpha Keys"),
    COLORBAR("style-colourbar", "Colourbar"),
    THEMED("style-themed", "Themed"),
    OVERFLOW_BOTTOM("overflow-bottom", "Overflow to Bottom"),
    OVERFLOW_HYBRID("overflow-hybrid", "Hybrid Overflow"),
    OVERFLOW_UNIFORM("overflow-uniform", "Uniform Overflow"),
    OVERFLOW_GEOMETRY("overflow-geometry", "Geometry Overflow");

    private final String feature;
    private final String label;

    SampleGuide(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.GUIDE;
    }

    @Override
    public String feature() {
        return feature;
    }

    @Override
    public String label() {
        return label;
    }
}
