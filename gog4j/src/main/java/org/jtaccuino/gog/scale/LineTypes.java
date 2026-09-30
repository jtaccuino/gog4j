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

import java.util.Locale;

/**
 * The predefined conventional line types for the {@code linetype} aesthetic,
 * mirroring {@code Scales.scaleLinetypeManual(values = 1:6)}.
 * <p>
 * The dash arrays reuse {@link LineDash} conventions: {@code dashed} =
 * {@code {6.0, 4.0}}, {@code dotted} = {@code {1.0, 3.0}},
 * {@code dotdash} = {@code {1.0, 3.0, 6.0, 3.0}}, {@code longdash} =
 * {@code {6.0, 3.0}}, {@code twodash} = {@code {2.0, 2.0}}, and
 * {@code solid} = no dashes.
 *
 * @see LineType
 */
public enum LineTypes implements LineType {

    /** A solid line (no dashes). */
    SOLID,
    /** Dashes: {@code {6.0, 4.0}}. */
    DASHED,
    /** Dots: {@code {1.0, 3.0}}. */
    DOTTED,
    /** Dot-dash: {@code {1.0, 3.0, 6.0, 3.0}}. */
    DOTDASH,
    /** Long dashes: {@code {6.0, 3.0}}. */
    LONGDASH,
    /** Two dashes: {@code {2.0, 2.0}}. */
    TWODASH;

    @Override
    public double[] dashes() {
        var d = LineDash.of(name().toLowerCase(Locale.ROOT));
        return d == null ? new double[0] : d.clone();
    }
}
