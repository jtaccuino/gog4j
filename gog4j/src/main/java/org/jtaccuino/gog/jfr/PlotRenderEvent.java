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
 * Records the full duration of a {@code Plot.renderTo(...)} pass, the outer
 * measure for end-to-end render performance. Nested events ({@link PreparePhaseEvent},
 * {@link CellExtentCollectionEvent}, …) break that total down by phase, so a
 * recording can attribute time to preparation versus drawing.
 */
@Name("org.jtaccuino.gog.jfr.PlotRender")
@Label("Plot Render")
@Category({"JTaccuino", "gog4j", "Render"})
@Description("Duration of a complete plot render pass")
@StackTrace(false)
public final class PlotRenderEvent extends jdk.jfr.Event {

    /** Creates a new event measuring a complete {@code Plot.renderTo} pass. */
    public PlotRenderEvent() { }

    /** The number of facet panels drawn in this pass. */
    @Label("Panel Count")
    public int panelCount;

    /** The number of geometry layers registered on the plot. */
    @Label("Layer Count")
    public int layerCount;

    /** The facet layout: {@code none}, {@code wrap} or {@code grid}. */
    @Label("Facet Mode")
    public String facetMode;
}
