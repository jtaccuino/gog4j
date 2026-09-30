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
 * Records the duration of preparing a single facet panel — computing its pixel
 * scales, domain bounds and layer domain expansions — before rendering. Each
 * panel's preparation is independent, making it a parallelization candidate.
 */
@Name("org.jtaccuino.gog.jfr.PanelPrep")
@Label("Panel Prepare")
@Category({"JTaccuino", "gog4j", "Prepare"})
@Description("Duration of preparing one facet panel's scales and domains")
@StackTrace(false)
public final class PanelPrepEvent extends jdk.jfr.Event {

    /** Creates a new event measuring preparation of a single panel. */
    public PanelPrepEvent() { }

    /** The zero-based index of the panel within the panel list. */
    @Label("Panel Index")
    public int panelIndex;

    /** The total number of panels in the plot. */
    @Label("Panel Count")
    public int panelCount;
}
