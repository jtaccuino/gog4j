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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.scale.Expansion;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.theme.AxisStyle;
import org.jtaccuino.gog.theme.CubeStyle;
import org.jtaccuino.gog.theme.DerivedTheme;
import org.jtaccuino.gog.theme.ElementRect;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.theme.Theme3d;
import org.junit.jupiter.api.Test;

class Theme3dHooksTest {

    private static final Color FALLBACK_PANEL_BORDER = Color.rgb(180, 180, 180, 0.8);

    @Test
    void builtInThemesImplementTheme3dAndDeriveTheCubeFromTheirOwn2dDefaults() {
        for (var theme : List.of(Theme.theme_gray(), Theme.theme_bw(), Theme.theme_dark())) {
            var cube = assertInstanceOf(Theme3d.class, theme);
            assertEquals(20.0, cube.cubePanelPad());
            assertEquals(0.65, cube.cubeSizeScale());
            assertEquals(0.03, cube.zCategoricalPad());
            assertEquals(5.0, cube.zTickLength());
            assertEquals(4.0, cube.cubeTickLabelPad());
            assertEquals(8.0, cube.cubeAxisTitlePad());
            assertEquals(0.3, cube.depthScaleStrength());
            assertEquals(0.5, cube.depthScaleMidpoint());
            assertEquals(1.0, cube.point3dStrokeWidth());
            assertEquals(3.0, cube.point3dHitTolerance());
            assertEquals(0.2, cube.cubeForegroundAlpha());
            // The cube faces and grid inherit the theme's own 2D panel and grid
            // defaults (the panel.background / grid chain), not a
            // separate hardcoded cube palette.
            assertEquals(theme.plotBackground(), cube.cubePanel().fill());
            assertEquals(theme.panelBorderColor() != null ? theme.panelBorderColor() : FALLBACK_PANEL_BORDER,
                    cube.cubePanel().colour());
            assertEquals(1.0, cube.cubePanel().linewidth());
            assertEquals(theme.gridLineColor(), cube.cubeGrid());
            assertEquals(0.5, cube.cubeGridLineWidth());
        }
    }

    @Test
    void cubeForegroundInheritsThePanelAtForegroundOpacity() {
        var cube = (Theme3d) Theme.theme_gray();
        var fg = cube.cubePanelForeground();
        assertEquals(0.2, fg.fill().getOpacity(), 1e-6);
        assertEquals(0.8 * 0.2, fg.colour().getOpacity(), 1e-6);
        assertEquals(cube.cubePanel().linewidth(), fg.linewidth());
    }

    @Test
    void zAxisElementChainInheritsTheSharedAxisHooks() {
        var cube = (Theme3d) Theme.theme_gray();
        assertEquals(cube.axisTickColor(), cube.axisTicksZColor());
        assertEquals(cube.tickLabelColor(), cube.axisTextZColor());
        assertEquals(cube.tickLabelFont(), cube.axisTextZFont());
        assertEquals(cube.axisTitleColor(), cube.axisTitleZColor());
        assertEquals(cube.axisTitleFont(), cube.axisTitleZFont());

        var zAxis = AxisStyle.forZ(cube);
        assertEquals(cube.axisTickColor(), zAxis.tickColor());
        assertEquals(cube.tickLabelColor(), zAxis.tickLabelColor());
        assertEquals(cube.tickLabelFont(), zAxis.tickLabelFont());
        assertEquals(cube.axisTitleColor(), zAxis.titleColor());
    }

    @Test
    void derivedThemePassesThroughThe3dHooks() {
        var base = (Theme3d) Theme.theme_gray();
        var derived = new DerivedTheme(base);
        assertInstanceOf(Theme3d.class, derived);
        assertEquals(base.cubePanel(), derived.cubePanel());
        assertEquals(base.cubeGrid(), derived.cubeGrid());
        assertEquals(base.depthScaleStrength(), derived.depthScaleStrength());
    }

    @Test
    void cubeStyleResolvesTheThemedElements() {
        var gray = (Theme3d) Theme.theme_gray();
        var cube = CubeStyle.from(gray);
        assertEquals(20.0, cube.panelPad());
        assertEquals(0.65, cube.sizeScale());
        assertEquals(gray.plotBackground(), cube.panel().fill());
        assertEquals(FALLBACK_PANEL_BORDER, cube.panel().colour());
        assertEquals(gray.gridLineColor(), cube.grid());
        assertEquals(0.3, cube.depthScaleStrength());
        assertEquals(0.5, cube.depthScaleMidpoint());
        assertEquals(1.0, cube.point3dStrokeWidth());
        assertEquals(3.0, cube.point3dHitTolerance());
        assertNotNull(cube.zAxis());

        var themed = new DerivedTheme(Theme.theme_gray())
                .plotBackground(Color.BLACK)
                .axisTickColor(Color.RED)
                .tickLabelColor(Color.GREEN);
        var styled = CubeStyle.from(themed);
        assertEquals(Color.BLACK, styled.panel().fill());
        assertEquals(Color.RED, styled.zAxis().tickColor());
        assertEquals(Color.GREEN, styled.zAxis().tickLabelColor());
    }

    @Test
    void cubeStyleFallsBackToTheConventionalLookWithoutTheme3d() {
        var plain = new PlainTheme();
        assertSame(CubeStyle.DEFAULTS, CubeStyle.from(plain));
        assertEquals(20.0, CubeStyle.from(plain).panelPad());
        assertNull(CubeStyle.from(plain).zAxis());
    }

    @Test
    void elementRectComposesItsAlphaIntoBothColours() {
        var fill = Color.rgb(240, 240, 240, 0.4);
        var border = Color.rgb(200, 200, 200, 0.7);
        var rect = ElementRect.of(fill, border, 1.0);
        assertEquals(fill, rect.fill());
        assertEquals(border, rect.colour());
        assertEquals(1.0, rect.linewidth());

        var faded = rect.withAlpha(0.5);
        assertEquals(0.4 * 0.5, faded.fill().getOpacity(), 1e-6);
        assertEquals(0.7 * 0.5, faded.colour().getOpacity(), 1e-6);

        assertNull(ElementRect.of(null, border, 1.0).fill());
        assertNull(ElementRect.of(fill, null, 1.0).colour());
    }

    @Test
    void scaleSpecKeepsTheZExpansionDecoupledFromY() {
        var spec = new ScaleSpec();
        assertEquals(Expansion.DEFAULT, spec.getYExpand());
        assertEquals(spec.getYExpand(), spec.getZExpand());

        spec.setZExpand(Expansion.mult(0.1));
        assertEquals(9.0, spec.getZExpand().expand(10.0, 20.0).min(), 0.0);
        assertEquals(21.0, spec.getZExpand().expand(10.0, 20.0).max(), 0.0);
        assertEquals(Expansion.DEFAULT, spec.getYExpand(), "the Y expansion is unaffected");

        spec.setZExpand(null);
        assertEquals(Expansion.none(), spec.getZExpand());
    }

    /**
     * A minimal {@link Theme} that deliberately does <em>not</em> implement
     * {@link Theme3d}, exercising the conventional-cube fallback path.
     */
    private static final class PlainTheme implements Theme {
        @Override public Color plotBackground() { return Color.WHITE; }
        @Override public Color paneBackground() { return Color.WHITE; }
        @Override public Color gridLineColor() { return Color.WHITE; }
        @Override public Color axisLineColor() { return Color.BLACK; }
        @Override public Color textColor() { return Color.BLACK; }
        @Override public Color defaultGeomFill() { return Color.RED; }
        @Override public Color defaultGeomStroke() { return Color.DARKRED; }
        @Override public Color tickLabelColor() { return Color.BLACK; }
        @Override public double axisLineWidth() { return 1.0; }
        @Override public double gridLineWidth() { return 0.8; }
        @Override public double xLabelRotationAngle() { return 0.0; }
        @Override public Color axisTitleColor() { return Color.BLACK; }
        @Override public Color titleColor() { return Color.BLACK; }
        @Override public Color panelBorderColor() { return null; }
        @Override public double panelBorderWidth() { return 1.0; }
        @Override public Color stripBackground() { return Color.LIGHTGRAY; }
        @Override public Color stripTextColor() { return Color.BLACK; }
        @Override public double facetHGap() { return 15.0; }
        @Override public double facetVGap() { return 15.0; }
    }
}
