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
 * The per-plot data a {@link Guide} is built against: the resolved values a
 * scale produces when the plot is built. A legend guide is built with its
 * categories and key styles, a colorbar guide with its domain, breaks, labels,
 * and color ramp.
 */
public sealed interface GuideData permits GuideLegend.Data, GuideColorbar.Data, GuideColorsteps.Data, GuideAlpha.Data {

    /**
     * {@return the column name the guide annotates, or {@code null} for a}
     * statistic-driven guide (e.g. a density colorbar).
     */
    String columnName();
}
