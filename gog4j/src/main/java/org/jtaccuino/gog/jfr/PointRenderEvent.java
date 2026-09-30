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
 * Records the duration of emitting a geometry layer's point scatter — the loop
 * that transforms and draws every point of one panel. Point count grows linearly
 * with data size, so this isolates the per-point SVG emission cost.
 */
@Name("org.jtaccuino.gog.jfr.PointRender")
@Label("Point Render")
@Category({"JTaccuino", "gog4j", "Render"})
@Description("Duration of drawing one panel's point scatter")
@StackTrace(false)
public final class PointRenderEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the draw loop of one panel's point scatter. */
    public PointRenderEvent() { }

    /** The number of rows processed by the layer for this panel. */
    @Label("Point Count")
    public int pointCount;

    /** The number of rows skipped because their x or y value was null. */
    @Label("Skipped Count")
    public int skippedCount;

    /** The number of rows dropped by the interactive overdraw-binning fast path. */
    @Label("Binned Count")
    public int binnedCount;
}
