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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jtaccuino.gog.guide.GuideLegend;
import org.jtaccuino.gog.guide.GuideStyle;
import org.jtaccuino.gog.guide.GuideTheme;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.theme.GuidePosition;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;

class GuideThemeAndMergeTest {

    private static final Font RED_FONT = Font.font("Custom", 14);

    @Test
    void guideThemeOverridesWinOverThemeDefaults() {
        var theme = Theme.theme_gray();
        var base = GuideStyle.from(theme);
        var styled = GuideStyle.from(theme, GuideStyle.class == null ? null
                : Guides.guideLegend().theme(GuideTheme.title(Color.RED, RED_FONT)));

        assertEquals(Color.RED, styled.titleColor());
        assertSame(RED_FONT, styled.titleFont());
        // Untouched components fall back to the theme.
        assertEquals(base.keyFont(), styled.keyFont());
        assertEquals(base.frameColor(), styled.frameColor());
    }

    @Test
    void nullOverrideIsPureTheme() {
        var theme = Theme.theme_gray();
        var plain = GuideStyle.from(theme);
        var withNull = GuideStyle.from(theme, Guides.guideLegend());
        assertEquals(plain, withNull);
    }

    @Test
    void keyColorOverrideAppliesToKeyLabels() {
        var theme = Theme.theme_gray();
        var styled = GuideStyle.from(theme,
                Guides.guideLegend().theme(GuideTheme.keys(Color.MAGENTA, null)));
        assertNull(styled.titleColor().equals(Color.MAGENTA) ? Color.MAGENTA : null,
                "title untouched by a keys-only override");
        assertEquals(Color.MAGENTA, styled.keyColor());
    }

    @Test
    void elementsStyleIndependently() {
        var theme = Theme.theme_gray();
        var base = GuideStyle.from(theme);
        // By default the label-tier colors all derive from the theme text color.
        assertEquals(theme.textColor(), base.textColor());
        assertEquals(theme.textColor(), base.tickColor());
        assertEquals(theme.textColor(), base.keyColor());

        // A tick-only override changes tick marks but nothing else.
        var styled = GuideStyle.from(theme,
                Guides.guideColorbar().theme(GuideTheme.NONE.withTickColor(Color.CHARTREUSE)));
        assertEquals(Color.CHARTREUSE, styled.tickColor());
        assertEquals(base.textColor(), styled.textColor());
        assertEquals(base.keyColor(), styled.keyColor());
        assertEquals(base.titleColor(), styled.titleColor());
        assertEquals(base.frameColor(), styled.frameColor());
    }

    @Test
    void frameColorIsPerGuideOverridable() {
        var theme = Theme.theme_gray();
        var styled = GuideStyle.from(theme,
                Guides.guideColorbar().theme(GuideTheme.NONE.withFrameColor(Color.ORANGE)));
        assertEquals(Color.ORANGE, styled.frameColor());
    }

    @Test
    void shapeKeysFoldIntoMatchingLabels() {
        var legendKeys = List.of(
                new GuideLegend.Key("f", "f", Color.BLUE, null, null),
                new GuideLegend.Key("r", "r", Color.RED, null, null));
        var shapeKeys = List.of(
                new GuideLegend.Key("f", "f", Color.BLACK, PointShape.TRIANGLE, null),
                new GuideLegend.Key("r", "r", Color.BLACK, PointShape.SQUARE, null));

        var merged = Plot.foldShapes(legendKeys, shapeKeys);

        assertEquals(2, merged.size());
        assertEquals(PointShape.TRIANGLE, merged.get(0).shape());
        assertEquals(Color.BLUE, merged.get(0).color(), "the colour encoding survives the merge");
        assertEquals(PointShape.SQUARE, merged.get(1).shape());
    }

    @Test
    void foldRejectsMismatchedLabelsOrCounts() {
        var keys = List.of(new GuideLegend.Key("a", "a", Color.BLUE, null, null));
        var otherCount = List.of(
                new GuideLegend.Key("a", "a", Color.BLACK, null, null),
                new GuideLegend.Key("b", "b", Color.BLACK, null, null));
        var otherLabels = List.of(new GuideLegend.Key("a", "A", Color.BLACK, null, null));

        assertNull(Plot.foldShapes(keys, otherCount));
        assertNull(Plot.foldShapes(keys, otherLabels));
    }

    @Test
    void insideAnchorRoundTripsThroughCopies() {
        var legend = Guides.guideLegend()
                .inside(0.25, 0.75)
                .title("t")
                .position(GuidePosition.INSIDE);

        assertEquals(0.25, legend.insideAnchor().x());
        assertEquals(0.75, legend.insideAnchor().y());
        assertEquals("t", legend.title());
        assertSame(GuidePosition.INSIDE, legend.position());

        assertNull(Guides.guideLegend().insideAnchor(), "unset anchors inherit the theme");
    }
}
