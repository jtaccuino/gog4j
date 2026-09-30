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
 * The {@link TagKind#FACET} catalogue: the faceting strategies an example uses,
 * mirroring {@code Facets.wrap}/{@code FacetSpec.grid} and the grid options
 * (free scales, switched axes, as-table layout).
 */
public enum SampleFacet implements SampleTag {

    WRAP("wrap", "Wrap"),
    GRID("grid", "Grid"),
    FREE_SCALES("free-scales", "Free Scales"),
    SWITCH_AXES("switch-axes", "Switched Axes"),
    AS_TABLE("as-table", "As Table");

    private final String feature;
    private final String label;

    SampleFacet(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.FACET;
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
