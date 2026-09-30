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
package org.jtaccuino.gog.layer;

import javafx.scene.paint.Color;

/**
 * A geometry layer that paints every mark with a constant colour when no
 * colour aesthetic is mapped — used by the tooltip glyph resolution to report
 * the very colour the geometry was painted with instead of a theme default.
 */
public interface ConstantColorLayer {

    /**
     * The colour the geometry paints with when its colour/fill aesthetic is
     * ungrouped, or {@code null} when the geometry is colour-mapped and this
     * fallback does not apply.
     *
     * @return the constant paint colour, or {@code null}
     */
    Color getCustomColor();
}
