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
package org.jtaccuino.gog;

/**
 * Immutable presentation options controlling which axis labels and titles a
 * plot draws.
 * <p>
 * Each knob is tri-state: {@code null} means "follow the containing context" —
 * a standalone plot labels both axes, while a cell of a composite {@code PlotMatrix}
 * resolves unset knobs from its grid position under the matrix's
 * {@link OuterLabels} policy. Explicit values always win.
 * <p>
 * Instances are created from the static shorthand presets
 * ({@link #defaults()}, {@link #margins()}, {@link #all()}, {@link #noLabels()})
 * and adjusted fluidly with the lowercase copy setters
 * ({@link #xLabels(boolean)}, {@link #yLabels(boolean)}, {@link #xTitle(boolean)},
 * {@link #yTitle(boolean)}), mirroring the grammar of the library's other
 * immutable option types.
 */
public final class PlotOptions {

    /** Where the outer-axis labels of a composite matrix sit. */
    public enum OuterLabels {
        /** Only the outer edges label their axes, the outer-edge look. */
        MARGINS,
        /** Every cell labels both axes. */
        ALL
    }

    private final OuterLabels outer;
    private final Boolean xLabels;
    private final Boolean yLabels;
    private final Boolean xTitle;
    private final Boolean yTitle;

    private PlotOptions(OuterLabels outer, Boolean xLabels, Boolean yLabels, Boolean xTitle, Boolean yTitle) {
        this.outer = outer;
        this.xLabels = xLabels;
        this.yLabels = yLabels;
        this.xTitle = xTitle;
        this.yTitle = yTitle;
    }

    /**
     * All knobs unset: a standalone plot labels both axes; a matrix cell follows
     * the matrix position policy ({@link OuterLabels#MARGINS} by default).
     *
     * @return the default {@link PlotOptions}
     */
    public static PlotOptions defaults() {
        return new PlotOptions(OuterLabels.MARGINS, null, null, null, null);
    }

    /**
     * The generalized-pairs preset: only the outer edges of the matrix label their
     * axes.
     *
     * @return a {@link PlotOptions} with the {@link OuterLabels#MARGINS} policy
     */
    public static PlotOptions margins() {
        return new PlotOptions(OuterLabels.MARGINS, null, null, null, null);
    }

    /**
     * The preset labeling every matrix cell on both axes.
     *
     * @return a {@link PlotOptions} with the {@link OuterLabels#ALL} policy
     */
    public static PlotOptions all() {
        return new PlotOptions(OuterLabels.ALL, null, null, null, null);
    }

    /**
     * The preset suppressing every tick label and axis title, keeping the tick
     * marks themselves.
     *
     * @return a {@link PlotOptions} with all label knobs explicitly {@code false}
     */
    public static PlotOptions noLabels() {
        return new PlotOptions(OuterLabels.MARGINS, false, false, false, false);
    }

    /**
     * Returns a copy with the x tick labels shown or hidden.
     *
     * @param show whether to draw the x tick labels
     * @return an updated {@link PlotOptions}
     */
    public PlotOptions xLabels(boolean show) {
        return new PlotOptions(outer, show, yLabels, xTitle, yTitle);
    }

    /**
     * Returns a copy with the y tick labels shown or hidden.
     *
     * @param show whether to draw the y tick labels
     * @return an updated {@link PlotOptions}
     */
    public PlotOptions yLabels(boolean show) {
        return new PlotOptions(outer, xLabels, show, xTitle, yTitle);
    }

    /**
     * Returns a copy with the x axis title shown or hidden.
     *
     * @param show whether to draw the x axis title
     * @return an updated {@link PlotOptions}
     */
    public PlotOptions xTitle(boolean show) {
        return new PlotOptions(outer, xLabels, yLabels, show, yTitle);
    }

    /**
     * Returns a copy with the y axis title shown or hidden.
     *
     * @param show whether to draw the y axis title
     * @return an updated {@link PlotOptions}
     */
    public PlotOptions yTitle(boolean show) {
        return new PlotOptions(outer, xLabels, yLabels, xTitle, show);
    }

    /**
     * The matrix position policy carried by these options.
     *
     * @return the {@link OuterLabels} policy
     */
    public OuterLabels outerLabels() {
        return outer;
    }

    /**
     * The raw tri-state x tick-label knob (nullable: unset means follow context).
     *
     * @return the knob, or {@code null} when unset
     */
    public Boolean xLabels() {
        return xLabels;
    }

    /**
     * The raw tri-state y tick-label knob (nullable: unset means follow context).
     *
     * @return the knob, or {@code null} when unset
     */
    public Boolean yLabels() {
        return yLabels;
    }

    /**
     * The raw tri-state x axis-title knob (nullable: unset means follow context).
     *
     * @return the knob, or {@code null} when unset
     */
    public Boolean xTitle() {
        return xTitle;
    }

    /**
     * The raw tri-state y axis-title knob (nullable: unset means follow context).
     *
     * @return the knob, or {@code null} when unset
     */
    public Boolean yTitle() {
        return yTitle;
    }

    /**
     * Resolves the unset knobs of a matrix cell against its grid position under
     * the given matrix policy, producing a fully explicit copy: an explicit
     * knob wins, unset knobs default to drawing when the policy labels this
     * position (the bottom row for x, the left column for y).
     *
     * @param policy    the matrix {@link OuterLabels} policy
     * @param bottomRow whether the cell sits on the bottom row of the matrix
     * @param leftCol   whether the cell sits on the left column of the matrix
     * @return a fully explicit {@link PlotOptions}
     */
    public PlotOptions resolvedBy(OuterLabels policy, boolean bottomRow, boolean leftCol) {
        var outerLabels = policy == null ? outer : policy;
        return new PlotOptions(outerLabels,
                resolve(xLabels, outerLabels == OuterLabels.ALL || bottomRow),
                resolve(yLabels, outerLabels == OuterLabels.ALL || leftCol),
                resolve(xTitle, outerLabels == OuterLabels.ALL || bottomRow),
                resolve(yTitle, outerLabels == OuterLabels.ALL || leftCol));
    }

    /**
     * Resolves the unset knobs as a standalone plot: both axes are labeled by
     * default.
     *
     * @return a fully explicit {@link PlotOptions}
     */
    public PlotOptions resolvedForStandalone() {
        return new PlotOptions(outer,
                resolve(xLabels, true),
                resolve(yLabels, true),
                resolve(xTitle, true),
                resolve(yTitle, true));
    }

    /**
     * Whether the x tick labels are drawn (only meaningful on a resolved copy).
     *
     * @return the resolved x tick-label switch
     */
    public boolean drawXLabels() {
        return Boolean.TRUE.equals(xLabels);
    }

    /**
     * Whether the y tick labels are drawn (only meaningful on a resolved copy).
     *
     * @return the resolved y tick-label switch
     */
    public boolean drawYLabels() {
        return Boolean.TRUE.equals(yLabels);
    }

    /**
     * Whether the x axis title is drawn (only meaningful on a resolved copy).
     *
     * @return the resolved x axis-title switch
     */
    public boolean drawXTitles() {
        return Boolean.TRUE.equals(xTitle);
    }

    /**
     * Whether the y axis title is drawn (only meaningful on a resolved copy).
     *
     * @return the resolved y axis-title switch
     */
    public boolean drawYTitles() {
        return Boolean.TRUE.equals(yTitle);
    }

    private static boolean resolve(Boolean knob, boolean fallback) {
        return knob != null ? knob : fallback;
    }
}
