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
 * Records the resolved geometry of one facet render: the panel grid dimensions
 * and the pixel width/height of a single panel. A fractional {@code panelWidth}
 * or {@code panelHeight} pushes every panel primitive onto subpixel coordinates,
 * which costs ~10x in the raster paths, so this event makes that diagnosable.
 */
@Name("org.jtaccuino.gog.jfr.FacetLayout")
@Label("Facet Layout")
@Category({"JTaccuino", "gog4j", "Layout"})
@Description("Resolved facet panel grid geometry (cols, rows, panel size, origin integrality)")
@StackTrace(false)
public final class FacetLayoutEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the facet grid geometry. */
    public FacetLayoutEvent() { }

    /** The number of panel columns in the facet grid. */
    @Label("Panel Columns")
    public int numCols;

    /** The number of panel rows in the facet grid. */
    @Label("Panel Rows")
    public int numRows;

    /** The pixel width of a single panel. */
    @Label("Panel Width")
    public double panelWidth;

    /** The pixel height of a single panel. */
    @Label("Panel Height")
    public double panelHeight;

    /** Whether every panel origin is on a whole-pixel coordinate. */
    @Label("Origins Integral")
    public boolean originsIntegral;
}
