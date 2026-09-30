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
package org.jtaccuino.gog.guide;

/**
 * How a size legend lays out its per-tick circles. The default lays a single size
 * legend's circles out so the larger ones sink behind the smaller — the classic
 * nested keys look.
 */
public enum SizeLegendStyle {

    /**
     * The conventional signature look: the largest circle sits at the back and each
     * smaller one is drawn on top of it, sharing a common center, so the keys
     * nest concentrically into one composited circle.
     */
    NESTED,

    /**
     * Each key circle keeps its own dedicated row sized to its own diameter, and
     * the circles never overlap one another; the keys grow from small to large
     * down the legend instead of compositing.
     */
    SEPARATED
}
