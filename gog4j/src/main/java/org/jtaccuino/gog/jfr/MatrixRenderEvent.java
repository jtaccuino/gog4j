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
 * Records the full duration of a {@code PlotMatrix.renderTo(...)} pass, the
 * outer measure for end-to-end scatter-plot-matrix rendering. Nested
 * {@link PlotRenderEvent}s from each cell break the total down per panel.
 */
@Name("org.jtaccuino.gog.jfr.MatrixRender")
@Label("PlotMatrix Render")
@Category({"JTaccuino", "gog4j", "Render"})
@Description("Duration of a complete PlotMatrix render pass")
@StackTrace(false)
public final class MatrixRenderEvent extends jdk.jfr.Event {

    /** Creates a new event measuring a complete {@code PlotMatrix.renderTo} pass. */
    public MatrixRenderEvent() { }

    /** The number of grid rows. */
    @Label("Rows")
    public int rows;

    /** The number of grid columns. */
    @Label("Cols")
    public int cols;

    /** The number of cells in the grid. */
    @Label("Panel Count")
    public int panelCount;
}
