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
package org.jtaccuino.gog.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.stat.StatData;

/**
 * The precomputed smooth {@link StatData} fits for every group of a
 * colour-mapped {@code Geoms.smooth()} layer, computed once during
 * {@link Layer#prepare(org.jtaccuino.gog.layer.PlotContext)} instead of
 * per-panel.
 *
 * <p>Categories appear in alphabetical order (matching
 * {@link org.jtaccuino.gog.scale.Scale#uniqueCategories}) and each entry
 * holds the fitted curve for that group.
 *
 * @param groups the distinct group values in scale order
 * @param fits   the parallel list of fitted {@link StatData} curves
 */
public record GroupedSmooths(List<Object> groups, List<StatData> fits) {

    /**
     * Returns the precomputed fit for a group, or {@code null} when the group
     * has fewer than three observations and was skipped during prepare.
     *
     * @param group the raw group value
     * @return the fitted {@link StatData}, or {@code null}
     */
    public StatData fitFor(Object group) {
        var idx = groups.indexOf(group);
        return idx < 0 ? null : fits.get(idx);
    }

    /**
     * Builds a {@code GroupedSmooths} from a map of group → fit, preserving
     * the map's iteration order.
     *
     * @param byGroup the pre-populated map
     * @return a new instance
     */
    public static GroupedSmooths fromMap(Map<Object, StatData> byGroup) {
        var groups = new ArrayList<>(byGroup.keySet());
        var fits = groups.stream().map(byGroup::get).toList();
        return new GroupedSmooths(groups, fits);
    }
}
