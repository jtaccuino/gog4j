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

import java.util.HashMap;
import java.util.Map;
import org.jtaccuino.gog.Aesthetic;

/**
 * Specification for plot labels including title, X/Y axis labels,
 * and a label dictionary for mapping category names to display names.
 */
public class LabsSpec {

    // TODO: subtitle, caption, dictionary (map)

    private String title;
    private String xLabel;
    private String yLabel;
    private String legendTitle;

    private Map<String, String> dict = new HashMap<>();

    private Map<Aesthetic, String> legendTitles = new HashMap<>();

    /**
     * Creates an empty label specification.
     */
    public LabsSpec() {
    }

    /**
     * Creates a label specification with only a title.
     *
     * @param title the main plot title
     */
    public LabsSpec(String title) {
        this.title = title;
    }

    /**
     * Creates a label specification with a title and X/Y axis labels.
     *
     * @param title  the main plot title
     * @param xLabel the X-axis label
     * @param yLabel the Y-axis label
     */
    public LabsSpec(String title, String xLabel, String yLabel) {
        this(title, xLabel, yLabel, null, null, null);
    }

    /**
     * Creates a label specification with a title, axis labels, and a label dictionary.
     *
     * @param title  the main plot title
     * @param xLabel the X-axis label
     * @param yLabel the Y-axis label
     * @param dict   the label display-name dictionary
     */
    public LabsSpec(String title, String xLabel, String yLabel, Map<String, String> dict) {
        this(title, xLabel, yLabel, null, dict, null);
    }

    private LabsSpec(String title, String xLabel, String yLabel, String legendTitle, Map<String, String> dict,
            Map<Aesthetic, String> legendTitles) {
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.legendTitle = legendTitle;
        if (dict != null) {
            this.dict = dict;
        }
        if (legendTitles != null) {
            this.legendTitles = legendTitles;
        }
    }

    /**
     * Returns the plot title.
     *
     * @return the title string, or {@code null}
     */
    public String title() { return title; }

    /** {@return whether the title is null or empty} */
    public boolean titleIsEmpty() { return null == title || title.isEmpty(); }

    /**
     * Returns the X-axis label.
     *
     * @return the X-axis label string, or {@code null}
     */
    public String xLabel() { return xLabel; }

    /** {@return whether the X-axis label is null or empty} */
    public boolean xLabelIsEmpty() { return null == xLabel || xLabel.isEmpty(); }

    /**
     * Returns the Y-axis label.
     *
     * @return the Y-axis label string, or {@code null}
     */
    public String yLabel() { return yLabel; }

    /** {@return whether the Y-axis label is null or empty} */
    public boolean yLabelIsEmpty() { return null == yLabel || yLabel.isEmpty(); }

    /**
     * Returns the legend (guide) title, shown above the swatches and their
     * labels. Falls back to the grouping column name when not set.
     *
     * @return the legend title string, or {@code null}
     */
    public String legendTitle() { return legendTitle; }

    /** {@return whether the legend title is null or empty} */
    public boolean legendTitleIsEmpty() { return null == legendTitle || legendTitle.isEmpty(); }

    /**
     * Returns the per-aesthetic guide title, following the
     * {@code labs(colour = "...")}.
     *
     * @param aesthetic the aesthetic the title is set for
     * @return the title string, or {@code null} when not set
     */
    public String legendTitle(Aesthetic aesthetic) { return legendTitles.get(aesthetic); }

    /**
     * Resolves a category name to its display name via label dictionary.
     *
     * @param name the raw category name
     * @return the display name, or the original name if not found
     */
    public String map(String name) { return dict.getOrDefault(name, name); }

    /**
     * Creates a new {@code LabsSpec} with the given title, preserving existing
     * axis labels, legend title, and dictionary.
     *
     * @param title the new title
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec title(String title) { return new LabsSpec(title, this.xLabel, this.yLabel, this.legendTitle, this.dict, this.legendTitles); }

    /**
     * Creates a new {@code LabsSpec} with the given X-axis label, preserving
     * existing title, Y-label, legend title, and dictionary.
     *
     * @param xLabel the new X-axis label
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec xLabel(String xLabel) { return new LabsSpec(this.title, xLabel, this.yLabel, this.legendTitle, this.dict, this.legendTitles); }

    /**
     * Creates a new {@code LabsSpec} with the given Y-axis label, preserving
     * existing title, X-label, legend title, and dictionary.
     *
     * @param yLabel the new Y-axis label
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec yLabel(String yLabel) { return new LabsSpec(this.title, this.xLabel, yLabel, this.legendTitle, this.dict, this.legendTitles); }

    /**
     * Creates a new {@code LabsSpec} with the given legend title, preserving
     * all other fields.
     *
     * @param legendTitle the legend (guide) title
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec legendTitle(String legendTitle) { return new LabsSpec(this.title, this.xLabel, this.yLabel, legendTitle, this.dict, this.legendTitles); }

    /**
     * Creates a new {@code LabsSpec} with the given per-aesthetic guide title,
     * following the {@code labs(colour = "Drive type")}. Takes precedence
     * over the global legend title for that aesthetic.
     *
     * @param aesthetic the aesthetic to title
     * @param legendTitle the guide title for that aesthetic
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec legendTitle(Aesthetic aesthetic, String legendTitle) {
        var next = new HashMap<>(this.legendTitles);
        next.put(aesthetic, legendTitle);
        return new LabsSpec(this.title, this.xLabel, this.yLabel, this.legendTitle, this.dict, next);
    }

    /**
     * Creates a new {@code LabsSpec} with an additional label mapping,
     * preserving all existing fields.
     *
     * @param name        the raw category name
     * @param displayName the display name
     * @return a new {@code LabsSpec} instance
     */
    public LabsSpec map(String name, String displayName) {
        return new LabsSpec(this.title, this.xLabel, this.yLabel, this.legendTitle,
                addToMap(name, displayName, new HashMap<>(this.dict)), this.legendTitles);
    }

    private static Map<String, String> addToMap(String key, String value, Map<String, String> map) {
        map.put(key, value);
        return map;
    }
}
