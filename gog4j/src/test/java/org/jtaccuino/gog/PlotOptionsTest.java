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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jtaccuino.gog.PlotOptions.OuterLabels;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link PlotOptions}: the preset shorthands, the lowercase copy
 * setters, the nullable tri-state readers, and — crucially — {@code resolvedBy}
 * filling unset knobs from the matrix position under each {@link OuterLabels}
 * policy while explicit values always win.
 */
class PlotOptionsTest {

    @Test
    void presetsSetExpectedKnobs() {
        var defaults = PlotOptions.defaults();
        assertSame(OuterLabels.MARGINS, defaults.outerLabels());
        assertNull(defaults.xLabels());
        assertNull(defaults.yLabels());
        assertNull(defaults.xTitle());
        assertNull(defaults.yTitle());
        assertFalse(defaults.drawXLabels(), "drawXLabels is only meaningful on a resolved copy");
        assertFalse(defaults.drawYLabels());

        assertSame(OuterLabels.ALL, PlotOptions.all().outerLabels());
        assertSame(OuterLabels.MARGINS, PlotOptions.margins().outerLabels());

        var none = PlotOptions.noLabels();
        assertSame(OuterLabels.MARGINS, none.outerLabels());
        assertFalse(none.drawXLabels());
        assertFalse(none.drawYLabels());
        assertFalse(none.drawXTitles());
        assertFalse(none.drawYTitles());
    }

    @Test
    void copySettersAreImmutable() {
        var base = PlotOptions.defaults();
        var xOnly = base.xLabels(true);
        assertSame(OuterLabels.MARGINS, xOnly.outerLabels());
        assertTrue(xOnly.xLabels());
        assertNull(base.xLabels(), "the original must not change");

        var yOnly = base.yLabels(false);
        assertFalse(yOnly.yLabels());
        assertNull(base.yLabels());

        var titlesOff = base.xTitle(false).yTitle(false);
        assertFalse(titlesOff.xTitle());
        assertFalse(titlesOff.yTitle());
        assertNull(base.xTitle());
        assertNull(base.yTitle());
    }

    @Test
    void resolvedByMarginsFillsByPosition() {
        var options = PlotOptions.margins();

        var bottomLeft = options.resolvedBy(OuterLabels.MARGINS, true, true);
        assertTrue(bottomLeft.drawXLabels());
        assertTrue(bottomLeft.drawYLabels());
        assertTrue(bottomLeft.drawXTitles());
        assertTrue(bottomLeft.drawYTitles());

        var inner = options.resolvedBy(OuterLabels.MARGINS, false, false);
        assertFalse(inner.drawXLabels(), "inner rows drop x labels under MARGINS");
        assertFalse(inner.drawYLabels(), "inner columns drop y labels under MARGINS");
        assertFalse(inner.drawXTitles());
        assertFalse(inner.drawYTitles());

        var bottomInner = options.resolvedBy(OuterLabels.MARGINS, true, false);
        assertTrue(bottomInner.drawXLabels(), "the bottom row keeps x labels");
        assertFalse(bottomInner.drawYLabels(), "a non-left column drops y labels");
    }

    @Test
    void resolvedByAllLabelsEveryCell() {
        var options = PlotOptions.all();
        for (var bottomRow : new boolean[] {false, true}) {
            for (var leftCol : new boolean[] {false, true}) {
                var resolved = options.resolvedBy(OuterLabels.ALL, bottomRow, leftCol);
                assertTrue(resolved.drawXLabels());
                assertTrue(resolved.drawYLabels());
                assertTrue(resolved.drawXTitles());
                assertTrue(resolved.drawYTitles());
            }
        }
    }

    @Test
    void resolvedByExplicitKnobWinsOverPosition() {
        var bottomInner = PlotOptions.defaults()
                .yLabels(true)
                .yTitle(true)
                .resolvedBy(OuterLabels.MARGINS, true, false);
        assertTrue(bottomInner.drawXLabels(), "bottom row labels x");
        assertTrue(bottomInner.drawYLabels(), "explicit yLabels(true) wins over position");
        assertTrue(bottomInner.drawYTitles(), "explicit yTitle(true) wins over position");
        assertTrue(bottomInner.drawXTitles(), "unset x title follows the position policy (bottom row draws)");
    }

    @Test
    void resolvedForStandaloneLabelsBothAxes() {
        var resolved = PlotOptions.defaults().resolvedForStandalone();
        assertTrue(resolved.drawXLabels());
        assertTrue(resolved.drawYLabels());
        assertTrue(resolved.drawXTitles());
        assertTrue(resolved.drawYTitles());

        var suppressed = PlotOptions.defaults().xLabels(false).resolvedForStandalone();
        assertFalse(suppressed.drawXLabels(), "explicit suppression survives standalone resolution");
        assertTrue(suppressed.drawYLabels());
    }
}
