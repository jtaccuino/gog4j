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
 * The {@link TagKind#DATASET} catalogue: the data sources the examples render,
 * keyed to the dflib loader classes in {@code org.jtaccuino.gog.dflib.data}.
 * {@link #SYNTHETIC} covers frames built inline by the example factory itself.
 */
public enum SampleDataset implements SampleTag {

    MPG("mpg", "Fuel Economy (mpg)"),
    MTCARS("mtcars", "Motor Trend Cars"),
    DIAMONDS("diamonds", "Diamonds"),
    FAITHFUL("faithful", "Old Faithful"),
    ANSCOMBE("anscombe", "Anscombe's Quartet"),
    MEAT("meat", "Meat Production"),
    SEATTLE_WEATHER("seattle-weather", "Seattle Hourly Weather"),
    GWAS("gwas", "Height GWAS"),
    POLAR("polar", "Polar Counts"),
    PENGUINS("penguins", "Palmer Penguins"),
    TIPS("tips", "Restaurant Tips"),
    MOUNTAIN("mountain", "Mountain Surface"),
    SPHERE("sphere", "Sphere Points"),
    SYNTHETIC("synthetic", "Synthetic / Function");

    private final String feature;
    private final String label;

    SampleDataset(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.DATASET;
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
