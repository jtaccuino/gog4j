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
 * Records the duration of the per-cell extent collection performed when laying
 * out a {@code Facets.grid} panel matrix. Each cell's raw and folded axis extents
 * are computed independently, so this is a strong parallelization candidate.
 */
@Name("org.jtaccuino.gog.jfr.CellExtentCollection")
@Label("Cell Extent Collection")
@Category({"JTaccuino", "gog4j", "Prepare"})
@Description("Duration of computing per-cell axis extents for a facet grid")
@StackTrace(false)
public final class CellExtentCollectionEvent extends jdk.jfr.Event {

    /** Creates a new event measuring per-panel cell-extent aggregation. */
    public CellExtentCollectionEvent() { }

    /** The total number of cells in the grid. */
    @Label("Cell Count")
    public int cellCount;

    /** The number of geometry layers whose domains are folded per cell. */
    @Label("Layer Count")
    public int layerCount;

    /** The number of rows in the grid. */
    @Label("Row Span")
    public int rowSpan;

    /** The number of columns in the grid. */
    @Label("Column Span")
    public int colSpan;
}
