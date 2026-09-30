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
package org.jtaccuino.gog.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Records the full duration of rendering one facet panel — domain resolution,
 * scale construction, coordinate preparation and every layer drawing pass. Each
 * panel is independent, so this is the coarse per-panel parent that fragments
 * the drawing phase and the boundary for per-panel parallelization.
 */
@Name("org.jtaccuino.gog.jfr.FacetPanel")
@Label("Facet Panel")
@Category({"JTaccuino", "gog4j", "Render"})
@Description("Duration of rendering one facet panel")
@StackTrace(false)
public final class FacetPanelEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the render of a single facet panel. */
    public FacetPanelEvent() { }

    /** The zero-based index of the panel being rendered. */
    @Label("Panel Index")
    public int panelIndex;

    /** The total number of panels in the plot. */
    @Label("Panel Count")
    public int panelCount;

    /** The facet layout: {@code wrap} or {@code grid}. */
    @Label("Facet Mode")
    public String facetMode;
}
