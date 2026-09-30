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

import org.jtaccuino.gog.facet.FacetValues;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * Restricted, package-private facet matching for data-driven layers such as
 * reference lines. A layer's own summary frame is matched to the panel being
 * drawn exactly as usual: each facet variable the frame carries is
 * filtered to the panel's level, a missing column draws the frame in every
 * panel, and a {@link FacetValues#ALL margin} level is skipped because the
 * cell aggregates over it.
 */
final class PanelFacet {

    private PanelFacet() {
    }

    /**
     * Restricts {@code data} to the rows matching the current facet panel.
     * Every facet variable the layer's own frame carries is matched to its
     * panel level; a variable the frame lacks is ignored, so such a summary
     * frame draws in every panel.
     *
     * @param <DF>        the DataFrame type
     * @param data        the layer's own summary frame
     * @param ext         the data extraction strategy
     * @param facetValues the panel facet levels
     * @return the restricted frame, or {@code null} when no row matches
     */
    static <DF> DF restrict(DF data, DataExtractor<DF> ext, FacetValues facetValues) {
        if (data == null) {
            return null;
        }
        if (facetValues == null || facetValues.isEmpty()) {
            return data;
        }
        DF frame = data;
        for (var var : facetValues.levels().keySet()) {
            var value = facetValues.level(var);
            if (FacetValues.ALL.equals(value)) {
                continue;
            }
            if (!hasColumn(ext, data, var)) {
                continue;
            }
            var matched = ext.partition(frame, var).get(value);
            if (matched == null) {
                return null;
            }
            frame = matched;
        }
        return frame;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasColumn(DataExtractor ext, Object df, String column) {
        try {
            return !ext.getColumn(df, column).isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
