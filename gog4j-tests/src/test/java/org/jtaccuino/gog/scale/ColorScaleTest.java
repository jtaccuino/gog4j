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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;

class ColorScaleTest {

    @Test
    void continuousForRangeComputesDomainBreaksAndLabels() {
        var scale = ContinuousColorScale.forRange("wt", List.of(1.5, 2.0, 3.5, 4.0, 5.5), null);
        assertNotNull(scale);
        assertTrue(scale.isContinuous());
        assertEquals(1.5, scale.min());
        assertEquals(5.5, scale.max());
        assertEquals(1.5, scale.domain().min());
        assertEquals(5.5, scale.domain().max());
        assertFalse(scale.breaks().isEmpty());
        assertEquals(scale.breaks().size(), scale.labels().size());
        assertEquals("wt", scale.columnName());
    }

    @Test
    void continuousForRangeReturnsNullWithoutNumbers() {
        assertNull(ContinuousColorScale.forRange("g", List.of("a", "b"), null));
        assertNull(ContinuousColorScale.forRange("g", List.of(), null));
    }

    @Test
    void continuousForRangeDefaultsRampToViridis() {
        var scale = ContinuousColorScale.forRange("wt", List.of(1.0, 2.0), null);
        assertEquals("viridis", scale.name());
        var plasma = ContinuousColorScale.forRange("wt", List.of(1.0, 2.0), "plasma");
        assertEquals("plasma", plasma.name());
        assertNotEquals(scale.colorFor(1.0), plasma.colorFor(1.0));
    }

    @Test
    void continuousInterpolatesWithinDomain() {
        var scale = ContinuousColorScale.forRange("wt", List.of(0.0, 10.0), null);
        assertEquals(scale.colorFor(0.0), scale.colorFor(-5.0));
        assertEquals(scale.colorFor(10.0), scale.colorFor(20.0));
        assertNotEquals(scale.colorFor(0.0), scale.colorFor(10.0));
    }

    @Test
    void discreteForColumnBuildsCategoriesColorsLabels() {
        var df = DataFrame.byColumn("g").of(Series.of("b", "a", "b"));
        var scale = DiscreteColorScale.forColumn(df, new DflibDataExtractor(), "g", null, null,
                Theme.theme_gray().categoricalPalette(), Theme.theme_gray().fallbackColor());
        assertFalse(scale.isContinuous());
        assertEquals(List.of("a", "b"), scale.categories());
        assertEquals(List.of("a", "b"), scale.labels());
        assertEquals(2, scale.colors().size());
        assertNotEquals(scale.colorFor("a"), scale.colorFor("b"));
        assertEquals(scale.colorFor("a"), scale.colorFor("a"));
    }

    @Test
    void discreteForColumnHonorsManualPalette() {
        var df = DataFrame.byColumn("g").of(Series.of("b", "a", "b"));
        var spec = new ScaleSpec();
        spec.setManualColors(java.util.Map.of("a", Color.RED, "b", Color.BLUE));
        var scale = DiscreteColorScale.forColumn(df, new DflibDataExtractor(), "g", spec, null,
                Theme.theme_gray().categoricalPalette(), Theme.theme_gray().fallbackColor());
        assertEquals(Color.RED, scale.colorFor("a"));
        assertEquals(Color.BLUE, scale.colorFor("b"));
    }

    @Test
    void resolveConstantColorAssignsPaletteInListOrder() {
        var palette = Theme.theme_gray().categoricalPalette();
        assertEquals(palette.get(0), Scale.resolveConstantColor(List.of("BJ 0", "BJ 2"), "BJ 0", palette));
        assertEquals(palette.get(1), Scale.resolveConstantColor(List.of("BJ 0", "BJ 2"), "BJ 2", palette));
        // Sorting the labels ("BJ 0" < "BJ 2") matches the categorical order the
        // data-driven palette uses, so the two keys stay distinguishable.
        assertNotEquals(Scale.resolveConstantColor(List.of("BJ 0", "BJ 2"), "BJ 0", palette),
                Scale.resolveConstantColor(List.of("BJ 0", "BJ 2"), "BJ 2", palette));
    }

    @Test
    void resolveConstantColorUsesDistinctColorsWithinThePalette() {
        var palette = Theme.theme_gray().categoricalPalette();
        var many = List.of("a", "b", "c", "d", "e", "f", "g");
        assertNotEquals(Scale.resolveConstantColor(many, "a", palette), Scale.resolveConstantColor(many, "g", palette),
                "the 7th label takes its own colour (pink), not a repeat of the first");
    }

    @Test
    void resolveConstantColorCyclesPastThePalette() {
        var palette = Theme.theme_gray().categoricalPalette();
        var eleven = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
        assertEquals(Scale.resolveConstantColor(eleven, "a", palette), Scale.resolveConstantColor(eleven, "k", palette),
                "the 11th label reuses the first palette colour");
    }

    @Test
    void forLabelsBuildsKeysWithPaletteColorsAndLabelledNames() {
        var palette = Theme.theme_gray().categoricalPalette();
        var scale = DiscreteColorScale.forLabels("color:BJ 0/BJ 2", List.of("BJ 0", "BJ 2"), null,
                palette, Theme.theme_gray().fallbackColor());
        assertEquals(List.of("BJ 0", "BJ 2"), scale.categories());
        assertEquals(palette.get(0), scale.colorFor("BJ 0"));
        assertEquals(palette.get(1), scale.colorFor("BJ 2"));
        assertEquals(2, scale.colors().size());
        // The pseudo column name for a label legend uniquely identifies it among
        // data-column legends.
        assertEquals("color:BJ 0/BJ 2", scale.columnName());
    }
}
