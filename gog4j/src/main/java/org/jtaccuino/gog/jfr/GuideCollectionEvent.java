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
 * Records the duration of collecting the plot's guide inlays (legends and
 * colourbars) from the resolved scales and mapped aesthetics.
 */
@Name("org.jtaccuino.gog.jfr.GuideCollection")
@Label("Guide Collection")
@Category({"JTaccuino", "gog4j", "Guides"})
@Description("Duration of building the plot's guide inlays")
@StackTrace(false)
public final class GuideCollectionEvent extends jdk.jfr.Event {

    /** Creates a new event measuring collection of the guide inlays. */
    public GuideCollectionEvent() { }

    /** The number of guide inlays collected. */
    @Label("Guide Count")
    public int guideCount;
}
