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
 * Records the duration of a single layer's {@code prepare()} call inside the
 * plot's preparation phase. Each layer prepares independently over the global
 * data, so these events are the prime candidates for parallelization.
 */
@Name("org.jtaccuino.gog.jfr.LayerPrepare")
@Label("Layer Prepare")
@Category({"JTaccuino", "gog4j", "Prepare"})
@Description("Duration of one geometry layer's prepare call")
@StackTrace(false)
public final class LayerPrepareEvent extends jdk.jfr.Event {

    /** Creates a new event measuring the prepare pass of one layer. */
    public LayerPrepareEvent() { }

    /** The simple class name of the layer implementation. */
    @Label("Layer Type")
    public String layerType;

    /** The zero-based index of the layer within the plot's layer list. */
    @Label("Layer Index")
    public int layerIndex;
}
