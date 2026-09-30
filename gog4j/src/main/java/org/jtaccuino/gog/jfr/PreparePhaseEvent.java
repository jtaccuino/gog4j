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
 * Records the total duration of the plot's {@code prepare()} phase, in which
 * shared scales are resolved and every layer computes its per-render state over
 * the global data. This is the aggregation of the individual {@link LayerPrepareEvent}s.
 */
@Name("org.jtaccuino.gog.jfr.PreparePhase")
@Label("Prepare Phase")
@Category({"JTaccuino", "gog4j", "Prepare"})
@Description("Total duration of the plot preparation phase")
@StackTrace(false)
public final class PreparePhaseEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the plot's prepare phase. */
    public PreparePhaseEvent() { }

    /** The number of geometry layers prepared in this phase. */
    @Label("Layer Count")
    public int layerCount;
}
