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
 * The category of a {@code Tag}, used to group filter chips and to keep the
 * dataset source, geoms, stats, scales, coordinate systems, facets, positions,
 * themes, guide strategies, and feature tags apart. Every {@link SamplePlot}
 * carries exactly one dataset tag and any number of the other kinds.
 * <p>
 * This enum lives in the {@code ap} source set alongside the {@code Sample*}
 * tag catalogs so each catalog can reference its kind at compile time; the
 * runtime {@code Tag} record (main) reads it from the classifier classpath.
 */
public enum TagKind {

    /** The actual data source backing the example, such as diamonds or mtcars. */
    DATASET("Dataset"),
    /** The geom layers the example renders, such as point or boxplot. */
    GEOM("Geom"),
    /** The statistical transformation the example applies, such as bin or smooth. */
    STAT("Stat"),
    /** The scale configuration the example uses, such as reversed axes or viridis. */
    SCALE("Scale"),
    /** The coordinate system the example uses, such as polar or radial. */
    COORD("Coordinate"),
    /** The faceting strategy the example uses, such as grid or wrap. */
    FACET("Facet"),
    /** The position adjustment the example uses, such as dodge or stack. */
    POSITION("Position"),
    /** The plot theme the example uses, such as gray or dark. */
    THEME("Theme"),
    /** A guide/legend placement or layout strategy, such as on-top, inside, or merged. */
    GUIDE("Guide"),
    /** A cross-cutting capability, such as annotations or minor grids. */
    FEATURE("Feature");

    private final String label;

    TagKind(String label) {
        this.label = label;
    }

    /**
     * Returns a human-readable display name for this kind.
     *
     * @return the display name, e.g. {@code "Coordinate"}
     */
    public String label() {
        return label;
    }
}
