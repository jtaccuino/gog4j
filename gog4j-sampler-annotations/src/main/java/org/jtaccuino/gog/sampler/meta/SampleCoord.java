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
 * The {@link TagKind#COORD} catalogue: the coordinate systems an example uses,
 * mirroring the {@code Coords} factory methods and the {@code Coord2D}
 * constants.
 */
public enum SampleCoord implements SampleTag {

    CARTESIAN("cartesian", "Cartesian"),
    EQUAL("equal", "Equal Aspect"),
    FLIP("flip", "Flipped"),
    POLAR("polar", "Polar"),
    RADIAL("radial", "Radial"),
    TRANS("trans", "Transformed"),
    COORD3D("coord3d", "3D (Cube)");

    private final String feature;
    private final String label;

    SampleCoord(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.COORD;
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
