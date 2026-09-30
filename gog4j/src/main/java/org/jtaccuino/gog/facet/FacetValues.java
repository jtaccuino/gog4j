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

import java.util.Map;

/**
 * The per-panel facet levels of a single facet cell.
 * <p>
 * For a wrap facet this carries the single variable-to-level pair; for a grid
 * facet it carries one pair per row and column variable. A level may be the
 * margin sentinel {@link #ALL}, meaning the cell aggregates across that
 * variable (the {@code (all)} strip label). Data-driven layers use these
 * values to restrict their own rows to the panel being drawn.
 *
 * @param levels the facet variable to panel-level map, one entry per variable
 */
public record FacetValues(Map<String, Object> levels) {

    /** The show-everything margin sentinel — the {@code (all)} strip label. */
    public static final Object ALL = "(all)";

    /**
     * Wraps a facet variable-to-level map.
     *
     * @param levels the facet variable to panel-level map
     * @return an immutable {@link FacetValues}
     */
    public static FacetValues of(Map<String, Object> levels) {
        return new FacetValues(Map.copyOf(levels));
    }

    /**
     * The level of a facet variable in this cell.
     *
     * @param var the facet variable name
     * @return the panel level, or {@code null} when the variable is present
     */
    public Object level(String var) {
        return levels.get(var);
    }

    /**
     * Whether the given facet variable of this cell is a margin (that is, the
     * cell aggregates across it). Unknown variables are not margins.
     *
     * @param var the facet variable name
     * @return {@code true} when the variable's level is the {@link #ALL} sentinel
     */
    public boolean isMargin(String var) {
        return ALL.equals(levels.get(var));
    }

    /**
     * Whether this cell carries a level for the given facet variable.
     *
     * @param var the facet variable name
     * @return {@code true} when the variable is present in this cell
     */
    public boolean contains(String var) {
        return levels.containsKey(var);
    }

    /**
     * Whether this cell carries no facet levels at all (an unfaceted, single
     * panel).
     *
     * @return {@code true} when there are no variables
     */
    public boolean isEmpty() {
        return levels.isEmpty();
    }
}
