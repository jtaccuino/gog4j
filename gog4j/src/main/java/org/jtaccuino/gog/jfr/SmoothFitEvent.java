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
 * Records the duration of a single LOESS smoothing fit — the aggregation of
 * duplicate X values, the (super-linear) {@code LoessInterpolator} pass and the
 * per-unique-X variance-band scan. Runs once per group in the grouped path and
 * once per panel in the ungrouped fallback, so it is the prime target of the
 * per-group LOESS parallelization step.
 */
@Name("org.jtaccuino.gog.jfr.SmoothFit")
@Label("Smooth Fit")
@Category({"JTaccuino", "gog4j", "Stat"})
@Description("Duration of one LOESS smoothing fit")
@StackTrace(false)
public final class SmoothFitEvent extends jdk.jfr.Event {

    /** Creates a new event measuring a LOESS smooth fit. */
    public SmoothFitEvent() { }

    /** The number of raw (x, y) points fed into the fit. */
    @Label("Point Count")
    public int pointCount;

    /** The number of distinct X values after aggregation. */
    @Label("Unique X Count")
    public int uniqueXCount;

    /** Whether the confidence-interval band is computed ({@code true}) or not. */
    @Label("Confidence Band")
    public boolean confidenceBand;
}
