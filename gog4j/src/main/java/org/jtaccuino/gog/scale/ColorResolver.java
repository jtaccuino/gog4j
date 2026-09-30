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
package org.jtaccuino.gog.scale;

import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;

/**
 * A per-column colour resolver: the fully resolved state of a colour/fill
 * column's mapping, precomputed once so a render loop can colour each element
 * with a single O(1) {@link #forValue(Object)} call.
 * <p>
 * The resolver decides continuous-vs-discrete once up front (including the
 * about-to-be-coloured column's type and any plot-level scale specification)
 * and, for binned continuous scales, pre-samples the step colours and domain
 * so the per-element work is nothing but an array/map lookup.
 */
public final class ColorResolver {

    private final DiscreteColorScale discrete;
    private final ContinuousColorScale continuous;
    private final List<Color> binnedColors;
    private final MinMax domain;
    private final int steps;

    private ColorResolver(DiscreteColorScale discrete, ContinuousColorScale continuous,
                          List<Color> binnedColors, MinMax domain, int steps) {
        this.discrete = discrete;
        this.continuous = continuous;
        this.binnedColors = binnedColors;
        this.domain = domain;
        this.steps = steps;
    }

    /**
     * A resolver that never colours anything (unmappable or empty column).
     *
     * @return the no-colour resolver
     */
    public static ColorResolver empty() {
        return new ColorResolver(null, null, null, null, 0);
    }

    /**
     * A categorical resolver honouring the discrete palette.
     *
     * @param scale the discrete colour scale to resolve through
     * @return a discrete-mode resolver
     */
    public static ColorResolver discrete(DiscreteColorScale scale) {
        return new ColorResolver(scale, null, null, null, 0);
    }

    /**
     * A value-interpolating resolver over a continuous ramp.
     *
     * @param scale the continuous colour scale to resolve through
     * @return a continuous-mode resolver
     */
    public static ColorResolver continuous(ContinuousColorScale scale) {
        return new ColorResolver(null, scale, null, null, 0);
    }

    /**
     * A continuous resolver mapping values to pre-sampled discrete steps.
     *
     * @param colors the pre-sampled step colours, one per bin
     * @param domain the numeric domain the bins were sampled over
     * @param steps  the number of sampled steps
     * @return a binned-mode resolver
     */
    public static ColorResolver binned(List<Color> colors, MinMax domain, int steps) {
        return new ColorResolver(null, null, colors, domain, steps);
    }

    /**
     * Resolves the colour of a single raw value.
     *
     * @param rawValue the per-row or per-group value to colour
     * @return the resolved colour, or {@code null} when it cannot be determined
     */
    public Color forValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (discrete != null) {
            return discrete.colorFor(rawValue);
        }
        if (!(rawValue instanceof Number n)) {
            return null;
        }
        double v = n.doubleValue();
        if (binnedColors != null) {
            double span = domain.max() - domain.min();
            int idx = span <= 0 ? 0 : (int) Math.floor((v - domain.min()) / span * steps);
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= binnedColors.size()) {
                idx = binnedColors.size() - 1;
            }
            return binnedColors.get(idx);
        }
        return continuous.colorFor(v);
    }
}
