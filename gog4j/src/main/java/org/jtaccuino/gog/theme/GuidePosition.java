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
package org.jtaccuino.gog.theme;

/**
 * The side of the plot panel the guides are placed against, mirroring
 * the {@code GuidePosition}. {@link #INSIDE} floats the guides over
 * the panel, anchored by the theme's inside anchor.
 */
public enum GuidePosition {
    /** Guides placed above the panel. */
    TOP,
    /** Guides placed to the right of the panel. */
    RIGHT,
    /** Guides placed below the panel. */
    BOTTOM,
    /** Guides placed to the left of the panel. */
    LEFT,
    /** Guides floated over the panel, anchored by the theme's inside anchor. */
    INSIDE
}
