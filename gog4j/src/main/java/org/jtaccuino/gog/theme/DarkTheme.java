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

class DarkTheme implements Theme3d {
    // 1. BACKGROUND: Deep, rich anthracite dark gray
    @Override public Color plotBackground() { return Color.web("#1e1e1e"); }
    @Override public Color paneBackground() { return Color.web("#1e1e1e"); }

    // 2. GRID: Bright, sharp contrast!
    // Translucent white ensures precise grid visibility
    // without overpowering foreground geoms.
    @Override public Color gridLineColor() { return Color.web("#ffffff", 0.12); }

    // 3. AXES & TEXT: Crisp, clear white/silver for perfect readability
    @Override public Color axisLineColor() { return Color.web("#aaaaaa"); }
    @Override public Color textColor() { return Color.web("#ffffff"); }
    @Override public Color axisTitleColor() { return Color.web("#e0e0e0"); }
    @Override public Color tickLabelColor() { return Color.web("#b0b0b0"); } // Soft, clear light gray for numbers
    @Override public Color titleColor() { return Color.web("#ffffff"); }

    // --- Geoms & Fonts (Stable and proven) ---
    @Override public Color defaultGeomFill() { return Color.web("#00ffcc"); } // Neon cyan for striking contrast
    @Override public Color defaultGeomStroke() { return Color.web("#00b38f"); }
    @Override public double axisLineWidth() { return 1.2; }
    @Override public double gridLineWidth() { return 0.6; }
    @Override public double xLabelRotationAngle() { return 0.0; }
    @Override public Color panelBorderColor() { return Color.web("#555555"); }
    @Override public double panelBorderWidth() { return 1.0; }
    @Override public Color stripBackground() { return Color.web("#2d2d2d"); }
    @Override public Color stripTextColor() { return Color.web("#00ffcc"); }
    @Override public double facetHGap() { return 15; }
    @Override public double facetVGap() { return 15; }
}
