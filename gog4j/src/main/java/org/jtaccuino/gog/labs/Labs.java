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
package org.jtaccuino.gog.labs;

/**
 * Entry point for specifying plot labels and label dictionaries.
 * <p>
 * Every method returns an immutable {@link LabsSpec}; {@link #labs},
 * {@link #title}, {@link #xLabel}, {@link #yLabel}, {@link #map} and
 * {@link #legendTitle} each build a specification for the corresponding
 * part of the plot.
 */
public class Labs {

    /** Utility class; not meant to be instantiated. */
    private Labs() {
    }

    /**
     * Creates a declarative axis and title label specification for the plot.
     *
     * @param title  the main plot title
     * @param xLabel the X-axis label
     * @param yLabel the Y-axis label
     * @return a new {@link LabsSpec} holding the three labels
     */
    public static LabsSpec labs(String title, String xLabel, String yLabel) {
        return new LabsSpec(title, xLabel, yLabel);
    }

    /**
     * Creates a label specification containing only a title.
     *
     * @param title the main plot title
     * @return a new {@link LabsSpec} instance
     */
    public static LabsSpec title(String title) {
        return new LabsSpec(title);
    }

    /**
     * Creates a label specification containing only an X-axis label.
     *
     * @param xLabel the X-axis label
     * @return a new {@link LabsSpec} instance
     */
    public static LabsSpec xLabel(String xLabel) {
        return new LabsSpec().xLabel(xLabel);
    }

    /**
     * Creates a label specification containing only a Y-axis label.
     *
     * @param yLabel the Y-axis label
     * @return a new {@link LabsSpec} instance
     */
    public static LabsSpec yLabel(String yLabel) {
        return new LabsSpec().yLabel(yLabel);
    }

    /**
     * Creates a label specification with a single category-to-display-name mapping.
     *
     * @param name        the raw category name
     * @param displayName the display name
     * @return a new {@link LabsSpec} instance
     */
    public static LabsSpec map(String name, String displayName) {
        return new LabsSpec().map(name, displayName);
    }

    /**
     * Creates a label specification with only a legend (guide) title, the label
     * shown above the fill/colour swatches.
     *
     * @param legendTitle the legend title text
     * @return a new {@link LabsSpec} instance
     */
    public static LabsSpec legendTitle(String legendTitle) {
        return new LabsSpec().legendTitle(legendTitle);
    }
}
