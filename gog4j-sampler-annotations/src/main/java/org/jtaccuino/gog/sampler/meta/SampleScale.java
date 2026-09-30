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
 * The {@link TagKind#SCALE} catalogue: the scale configurations an example
 * uses, mirroring the {@code Scales} factory methods and the {@code xlim}/
 * {@code ylim} shortcuts.
 */
public enum SampleScale implements SampleTag {

    X_LIMITS("x-limits", "X Limits"),
    Y_LIMITS("y-limits", "Y Limits"),
    X_REVERSE("x-reverse", "Reversed X Axis"),
    Y_SQRT("y-sqrt", "Y Square Root"),
    X_CONTINUOUS("x-continuous", "Continuous X"),
    Y_CONTINUOUS("y-continuous", "Continuous Y"),
    X_POSITION("x-position", "X Axis Position"),
    Y_POSITION("y-position", "Y Axis Position"),
    COLOR_CONTINUOUS("color-continuous", "Continuous Colour"),
    COLOR_DISCRETE("color-discrete", "Discrete Colour"),
    COLOR_BINNED("color-binned", "Binned Colour"),
    COLOR_MANUAL("color-manual", "Manual Colour"),
    COLOR_BREWER("color-brewer", "Brewer Colour"),
    SHAPE_MANUAL("shape-manual", "Manual Shape"),
    SIZE_MANUAL("size-manual", "Manual Size"),
    LINETYPE_MANUAL("linetype-manual", "Manual Linetype");

    private final String feature;
    private final String label;

    SampleScale(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.SCALE;
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
