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

import org.jtaccuino.gog.MinMax;

/**
 * Axis domain expansion, following the {@code expansion()} function: the
 * range of a continuous scale is widened by a multiplicative fraction of the
 * data span on either side, optionally plus a constant amount in data units.
 * <p>
 * The default for continuous scales is {@code expansion(mult = 0.05)} — five
 * per cent headroom on both sides. {@link #none()} switches the padding off
 * entirely, so the data reaches exactly to the panel edges.
 */
public final class Expansion {

    private static final Expansion NONE = expansion(0.0, 0.0);

    /**
     * The default expansion for continuous scales: five per cent headroom on
     * both sides, mirroring {@code expansion(mult = 0.05)}.
     */
    public static final Expansion DEFAULT = mult(0.05);

    private final double multLower;
    private final double multUpper;
    private final double addLower;
    private final double addUpper;

    private Expansion(double multLower, double multUpper, double addLower, double addUpper) {
        this.multLower = multLower;
        this.multUpper = multUpper;
        this.addLower = addLower;
        this.addUpper = addUpper;
    }

    /**
     * Expands both ends of the scale by the given fraction of the data span.
     *
     * @param mult the fraction of the span to add at the lower and upper end
     * @return the expansion, mirroring {@code expansion(mult = mult)}
     */
    public static Expansion mult(double mult) {
        return mult(mult, mult);
    }

    /**
     * Expands the ends of the scale by possibly different fractions of the data
     * span.
     *
     * @param multLower the fraction of the span to add at the lower end
     * @param multUpper the fraction of the span to add at the upper end
     * @return the expansion, mirroring {@code expansion(mult = c(multLower, multUpper))}
     */
    public static Expansion mult(double multLower, double multUpper) {
        return expansion(multLower, multUpper, 0.0, 0.0);
    }

    /**
     * Expands both ends of the scale by a constant amount in data units.
     *
     * @param add the constant amount to add at the lower and upper end
     * @return the expansion, mirroring {@code expansion(add = add)}
     */
    public static Expansion add(double add) {
        return add(add, add);
    }

    /**
     * Expands the ends of the scale by possibly different constant amounts in
     * data units.
     *
     * @param addLower the constant amount to add at the lower end
     * @param addUpper the constant amount to add at the upper end
     * @return the expansion, mirroring {@code expansion(add = c(addLower, addUpper))}
     */
    public static Expansion add(double addLower, double addUpper) {
        return expansion(0.0, 0.0, addLower, addUpper);
    }

    /**
     * Expands both ends of the scale by both a fraction of the data span and a
     * constant amount in data units.
     *
     * @param mult the fraction of the span to add at the lower and upper end
     * @param add the constant amount to add at the lower and upper end
     * @return the expansion, mirroring {@code expansion(mult = mult, add = add)}
     */
    public static Expansion expansion(double mult, double add) {
        return expansion(mult, mult, add, add);
    }

    /**
     * Expands the ends of the scale by possibly different fractions of the data
     * span and constant amounts in data units.
     *
     * @param multLower the fraction of the span to add at the lower end
     * @param multUpper the fraction of the span to add at the upper end
     * @param addLower the constant amount to add at the lower end
     * @param addUpper the constant amount to add at the upper end
     * @return the expansion, mirroring
     *         {@code expansion(mult = c(multLower, multUpper), add = c(addLower, addUpper))}
     */
    public static Expansion expansion(double multLower, double multUpper, double addLower, double addUpper) {
        return new Expansion(multLower, multUpper, addLower, addUpper);
    }

    /**
     * No expansion: the scale spans the data range exactly.
     *
     * @return an expansion that leaves the domain untouched
     */
    public static Expansion none() {
        return NONE;
    }

    /**
     * Applies this expansion to a data range.
     *
     * @param lo the lower data bound
     * @param hi the upper data bound
     * @return the expanded range {@code [lo', hi']}
     */
    public MinMax expand(double lo, double hi) {
        var range = hi - lo;
        return new MinMax(lo - multLower * range - addLower, hi + multUpper * range + addUpper);
    }
}
