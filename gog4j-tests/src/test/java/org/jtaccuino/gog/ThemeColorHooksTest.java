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

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.scale.DiscreteColorScale;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.theme.DerivedTheme;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;

class ThemeColorHooksTest {

    private static final Color CANONICAL_FALLBACK = Color.web("#e31a1c");
    private static final Color CANONICAL_STROKE = Color.web("#bd0026");
    private static final Color CANONICAL_NEUTRAL = Color.web("#666666");

    @Test
    void builtInThemesShareTheCanonicalColorHooks() {
        var palette = Theme.theme_gray().categoricalPalette();
        for (var theme : List.of(Theme.theme_gray(), Theme.theme_bw(), Theme.theme_dark())) {
            assertEquals(palette, theme.categoricalPalette());
            assertEquals("viridis", theme.defaultContinuousRamp());
            assertEquals(CANONICAL_NEUTRAL, theme.neutralKeyColor());
            assertEquals(CANONICAL_FALLBACK, theme.fallbackColor());
            assertEquals(CANONICAL_STROKE, theme.fallbackStroke());
        }
    }

    @Test
    void derivedThemeCopiesAndOverridesTheColorHooks() {
        var base = Theme.theme_gray();
        var derived = new DerivedTheme(base);
        assertEquals(base.categoricalPalette(), derived.categoricalPalette());
        assertEquals(base.defaultContinuousRamp(), derived.defaultContinuousRamp());
        assertEquals(base.neutralKeyColor(), derived.neutralKeyColor());
        assertEquals(base.fallbackColor(), derived.fallbackColor());
        assertEquals(base.fallbackStroke(), derived.fallbackStroke());

        var tinted = derived
                .categoricalPalette(List.of(Color.RED, Color.BLUE))
                .defaultContinuousRamp("plasma")
                .neutralKeyColor(Color.BLACK)
                .fallbackColor(Color.MAGENTA)
                .fallbackStroke(Color.CYAN);
        assertEquals(List.of(Color.RED, Color.BLUE), tinted.categoricalPalette());
        assertEquals("plasma", tinted.defaultContinuousRamp());
        assertEquals(Color.BLACK, tinted.neutralKeyColor());
        assertEquals(Color.MAGENTA, tinted.fallbackColor());
        assertEquals(Color.CYAN, tinted.fallbackStroke());
        assertEquals(CANONICAL_NEUTRAL, base.neutralKeyColor(), "the base theme is unaffected");
    }

    @Test
    void resolveGlobalColorUsesTheThemePaletteInCategoryOrder() {
        var df = DataFrame.byColumn("g").of(Series.of("b", "a", "b", "c"));
        var theme = new DerivedTheme(Theme.theme_gray())
                .categoricalPalette(List.of(Color.RED, Color.BLUE, Color.GREEN));
        var ext = new DflibDataExtractor();
        assertEquals(Color.RED, Scale.resolveGlobalColor(df, ext, "g", "a", null,
                theme.categoricalPalette(), theme.fallbackColor()));
        assertEquals(Color.BLUE, Scale.resolveGlobalColor(df, ext, "g", "b", null,
                theme.categoricalPalette(), theme.fallbackColor()));
        assertEquals(Color.GREEN, Scale.resolveGlobalColor(df, ext, "g", "c", null,
                theme.categoricalPalette(), theme.fallbackColor()));
    }

    @Test
    void resolveGlobalColorUsesTheThemeFallbackForUnknownLookups() {
        var df = DataFrame.byColumn("g").of(Series.of("a"));
        var theme = new DerivedTheme(Theme.theme_gray()).fallbackColor(Color.MAGENTA);
        var ext = new DflibDataExtractor();
        assertEquals(Color.MAGENTA, Scale.resolveGlobalColor(df, ext, "g", "unknown", null,
                theme.categoricalPalette(), theme.fallbackColor()));
        assertEquals(Color.MAGENTA, Scale.resolveGlobalColor(df, ext, null, "x", null,
                theme.categoricalPalette(), theme.fallbackColor()));
        assertEquals(Color.MAGENTA, Scale.resolveGlobalColor(df, ext, "g", null, null,
                theme.categoricalPalette(), theme.fallbackColor()));
    }

    @Test
    void resolveConstantColorUsesTheThemePalette() {
        var theme = new DerivedTheme(Theme.theme_gray())
                .categoricalPalette(List.of(Color.RED, Color.BLUE));
        assertEquals(Color.RED, Scale.resolveConstantColor(List.of("L1", "L2"), "L1", theme.categoricalPalette()));
        assertEquals(Color.BLUE, Scale.resolveConstantColor(List.of("L1", "L2"), "L2", theme.categoricalPalette()));
    }

    @Test
    void discreteColorScaleThreadsThemePaletteAndFallback() {
        var df = DataFrame.byColumn("g").of(Series.of("a", "b"));
        var theme = new DerivedTheme(Theme.theme_gray())
                .categoricalPalette(List.of(Color.RED, Color.BLUE))
                .fallbackColor(Color.MAGENTA);
        var scale = DiscreteColorScale.forColumn(df, new DflibDataExtractor(), "g", null, null,
                theme.categoricalPalette(), theme.fallbackColor());
        assertEquals(Color.RED, scale.colorFor("a"));
        assertEquals(Color.BLUE, scale.colorFor("b"));
        assertEquals(Color.MAGENTA, scale.colorFor("unknown"));
        assertEquals(Color.MAGENTA, scale.colorFor(null));
    }

    @Test
    void forLabelsThreadsTheThemePalette() {
        var theme = new DerivedTheme(Theme.theme_gray())
                .categoricalPalette(List.of(Color.RED, Color.BLUE));
        var scale = DiscreteColorScale.forLabels("color:l1/l2", List.of("l1", "l2"), null,
                theme.categoricalPalette(), theme.fallbackColor());
        assertEquals(Color.RED, scale.colorFor("l1"));
        assertEquals(Color.BLUE, scale.colorFor("l2"));
    }
}
