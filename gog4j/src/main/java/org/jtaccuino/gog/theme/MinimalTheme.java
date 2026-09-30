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
package org.jtaccuino.gog.theme;

import javafx.scene.paint.Color;

/**
 * The {@code theme_minimal()} look: a white background with no panel
 * border and minimal grid lines — a clean, borderless theme for schematic
 * plots.
 */
class MinimalTheme implements Theme3d {
    @Override public Color plotBackground() { return Color.WHITE; }
    @Override public Color paneBackground() { return Color.WHITE; }
    @Override public Color gridLineColor() { return Color.web("#f0f0f0"); }
    @Override public Color axisLineColor() { return Color.web("#555555"); }
    @Override public Color textColor() { return Color.web("#111111"); }
    @Override public Color defaultGeomFill() { return Color.web("#e31a1c"); }
    @Override public Color defaultGeomStroke() { return Color.web("#bd0026"); }
    @Override public Color tickLabelColor() { return Color.web("#333333"); }
    @Override public double axisLineWidth() { return 0.8; }
    @Override public double gridLineWidth() { return 0.6; }
    @Override public double xLabelRotationAngle() { return 0.0; }

    @Override public Color axisTitleColor() { return Color.web("#333333"); }
    @Override public Color titleColor() { return Color.web("#111111"); }
    @Override public Color panelBorderColor() { return null; }               // theme_minimal: no border
    @Override public double panelBorderWidth() { return 1.0; }
    @Override public Color stripBackground() { return Color.WHITE; }
    @Override public Color stripTextColor() { return Color.web("#333333"); }
    @Override public double facetHGap() { return 15; }
    @Override public double facetVGap() { return 15; }
}
