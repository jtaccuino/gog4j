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
 * The {@link TagKind#FEATURE} catalogue: cross-cutting capabilities that do not
 * map to a single geom, stat, scale, or coordinate system.
 */
public enum SampleFeature implements SampleTag {

    TWO_D("2d", "2D"),
    THREE_D("3d", "3D"),
    ANNOTATE("annotate", "Annotations"),
    ALPHA("alpha", "Alpha"),
    LINETYPE("linetype", "Linetype"),
    MINOR_GRID("minor-grid", "Minor Grid"),
    LOG_TICKS("log-ticks", "Log Ticks"),
    LAYERS("layers", "Layers"),
    COMPOSITION("composition", "Composition"),
    LIGHTING("lighting", "Lighting"),
    VIEW("view", "Camera View"),
    ORBIT("orbit", "Orbit"),
    PANELS("panels", "Cube Panels"),
    MIXING("mixing", "2D/3D Mixing"),
    REFERENCE("reference", "Reference Elements");

    private final String feature;
    private final String label;

    SampleFeature(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.FEATURE;
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
