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
package org.jtaccuino.gog.facet;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable behaviour knobs for a grid facet layout, mirroring the arguments
 * of the {@code Facets.grid(..., scales, space, margins, switch, axes,
 * as.table, drop)}.
 * <p>
 * Instances are created from {@link #defaults()} and adjusted fluidly with the
 * {@code withXxx} copies, so a {@code Facets.grid(...)} call can be tuned with
 * chaining.
 */
public final class GridOptions {

    /** Whether each panel uses its own, some, or all of the axis scales. */
    public enum Scale {
        /** One shared scale for every panel, the default. */
        FIXED,
        /** Panels in each column share a y-scale; x stays fixed. */
        FREE_Y,
        /** Panels in each row share an x-scale; y stays fixed. */
        FREE_X,
        /** Every panel scales its axes independently. */
        FREE
    }

    /** Whether panels sizes track the extent of their scales. */
    public enum Space {
        /** All panels the same size, the default. */
        FIXED,
        /** Row height and column width follow the scale extent of each panel. */
        FREE
    }

    /** Which axis tick labels each panel draws. */
    public enum Axes {
        /** Only the outer panels label their axes (the left column and bottom row). */
        MARGINS,
        /** Every column labels its y-axis. */
        ALL_X,
        /** Every row labels its x-axis. */
        ALL_Y,
        /** Every panel labels both axes. */
        ALL
    }

    /** Where the strip labels sit. */
    public enum GridSwitch {
        /** Column strips on top, row strips on the right (default). */
        NONE,
        /** Column strips on the bottom. */
        X,
        /** Row strips on the left. */
        Y,
        /** Column strips on the bottom and row strips on the left. */
        BOTH
    }

    private final Scale scale;
    private final Space space;
    private final boolean margins;
    private final Set<String> marginVars;
    private final GridSwitch gridSwitch;
    private final boolean asTable;
    private final boolean drop;
    private final Axes axes;

    private GridOptions(Scale scale, Space space, boolean margins, Set<String> marginVars,
                        GridSwitch gridSwitch, boolean asTable, boolean drop, Axes axes) {
        this.scale = scale;
        this.space = space;
        this.margins = margins;
        this.marginVars = marginVars;
        this.gridSwitch = gridSwitch;
        this.asTable = asTable;
        this.drop = drop;
        this.axes = axes;
    }

    /**
     * The conventional defaults: fixed scales and space, no margins, no
     * strip switch, outer axes only, table-style row order with empty panels
     * dropped.
     *
     * @return a default {@link GridOptions}
     */
    public static GridOptions defaults() {
        return new GridOptions(Scale.FIXED, Space.FIXED, false, Set.of(),
                GridSwitch.NONE, true, true, Axes.MARGINS);
    }

    /**
     * Returns a copy with a different scale behaviour.
     *
     * @param scale the new scale mode
     * @return an updated {@link GridOptions}
     */
    public GridOptions withScale(Scale scale) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy with a different space behaviour.
     *
     * @param space the new space mode
     * @return an updated {@link GridOptions}
     */
    public GridOptions withSpace(Space space) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy with a different strip switch preset.
     *
     * @param gridSwitch the new strip switch preset
     * @return an updated {@link GridOptions}
     */
    public GridOptions withSwitch(GridSwitch gridSwitch) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy with a different outer-axis behaviour.
     *
     * @param axes the new axes mode
     * @return an updated {@link GridOptions}
     */
    public GridOptions withAxes(Axes axes) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy that turns the table-style row order on or off.
     * <p>
     * When {@code true} the first value of the row variable appears at the top
     * (as in a table). When {@code false} it appears at the bottom, the natural
     * reading order for a plot.
     *
     * @param asTable the new table-order flag
     * @return an updated {@link GridOptions}
     */
    public GridOptions withAsTable(boolean asTable) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy that turns the empty-panel drop behaviour on or off.
     * <p>
     * When {@code true} panels with no matching data are omitted from the grid.
     *
     * @param drop the new drop flag
     * @return an updated {@link GridOptions}
     */
    public GridOptions withDrop(boolean drop) {
        return new GridOptions(scale, space, margins, marginVars, gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy that adds margin (aggregate) rows and columns for every
     * faceting variable, the analogue of {@code Facets.grid(..., margins =
     * TRUE)}.
     *
     * @return an updated {@link GridOptions}
     */
    public GridOptions withMargins() {
        return new GridOptions(scale, space, true, Set.of(), gridSwitch, asTable, drop, axes);
    }

    /**
     * Returns a copy that adds margin (aggregate) rows and columns only for the
     * named variables, the analogue of {@code Facets.grid(..., margins =
     * "cyl")}. Null entries are ignored.
     *
     * @param variables the faceting variables that carry margins
     * @return an updated {@link GridOptions}
     */
    public GridOptions withMargins(String... variables) {
        var vars = Arrays.stream(variables)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new GridOptions(scale, space, true, vars, gridSwitch, asTable, drop, axes);
    }

    /** {@return the scale mode, never {@code null}} */
    public Scale scale() {
        return scale;
    }

    /** {@return the space mode, never {@code null}} */
    public Space space() {
        return space;
    }

    /** {@return whether any margin rows or columns are added} */
    public boolean margins() {
        return margins;
    }

    /**
     * {@return the named margin variables, or empty when {@link #margins()} is
     *      true for every faceting variable}
     */
    public Set<String> marginVars() {
        return marginVars;
    }

    /** {@return the strip switch preset, never {@code null}} */
    public GridSwitch switchPreset() {
        return gridSwitch;
    }

    /** {@return whether the row order follows table convention} */
    public boolean asTable() {
        return asTable;
    }

    /** {@return whether empty panels are omitted} */
    public boolean drop() {
        return drop;
    }

    /** {@return the outer-axis behaviour, never {@code null}} */
    public Axes axes() {
        return axes;
    }

    @Override
    public String toString() {
        return "GridOptions[scale=" + scale + ", space=" + space + ", margins=" + margins
                + ", marginVars=" + marginVars + ", switch=" + gridSwitch + ", asTable=" + asTable
                + ", drop=" + drop + ", axes=" + axes + "]";
    }
}
