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

class GrayTheme implements Theme3d {
    @Override public Color plotBackground() { return Color.web("#ebebeb"); }
    @Override public Color paneBackground() { return Color.WHITE; }
    @Override public Color gridLineColor() { return Color.WHITE; }
    @Override public Color axisLineColor() { return Color.web("#333333"); }
    @Override public Color textColor() { return Color.web("#222222"); }
    @Override public Color defaultGeomFill() { return Color.web("#e31a1c"); }
    @Override public Color defaultGeomStroke() { return Color.web("#bd0026"); }
    @Override public Color tickLabelColor() { return Color.web("#555555"); } // Subtle, calm gray for ticks
    @Override public double axisLineWidth() { return 1.0; }
    @Override public double gridLineWidth() { return 0.8; }
    @Override public double xLabelRotationAngle() { return 0.0; }

    // --- Extended defaults for the gray standard theme ---
    @Override public Color axisTitleColor() { return Color.web("#444444"); } // More subdued anthracite
    @Override public Color titleColor() { return Color.web("#111111"); }     // Crisp black
    @Override public Color panelBorderColor() { return null; }               // Default: no outer border
    @Override public double panelBorderWidth() { return 1.0; }
    @Override public Color stripBackground() { return Color.web("#d9d9d9"); } // Classic ggplot strip (gray bar)
    @Override public Color stripTextColor() { return Color.web("#333333"); }
    @Override public double facetHGap() { return 15; }
    @Override public double facetVGap() { return 15; }
}
