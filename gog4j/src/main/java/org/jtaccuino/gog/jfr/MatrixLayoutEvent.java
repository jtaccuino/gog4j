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
 * Records the resolved geometry of one {@code matrixPlot} render: the cell
 * grid dimensions and the pixel width/height of a single cell. A fractional
 * {@code cellWidth} or {@code cellHeight} pushes every cell primitive onto
 * subpixel coordinates, which costs ~10x in the raster paths, so this event
 * makes that diagnosable.
 */
@Name("org.jtaccuino.gog.jfr.MatrixLayout")
@Label("Matrix Layout")
@Category({"JTaccuino", "gog4j", "Layout"})
@Description("Resolved matrix cell grid geometry (cols, rows, cell size, origin integrality)")
@StackTrace(false)
public final class MatrixLayoutEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the matrix cell grid geometry. */
    public MatrixLayoutEvent() { }

    /** The number of cell columns in the matrix grid. */
    @Label("Cell Columns")
    public int numCols;

    /** The number of cell rows in the matrix grid. */
    @Label("Cell Rows")
    public int numRows;

    /** The pixel width of a single cell. */
    @Label("Cell Width")
    public double cellWidth;

    /** The pixel height of a single cell. */
    @Label("Cell Height")
    public double cellHeight;

    /** Whether every cell origin is on a whole-pixel coordinate. */
    @Label("Origins Integral")
    public boolean originsIntegral;
}
