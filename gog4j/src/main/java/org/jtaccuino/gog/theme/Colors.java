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
 * Shared colour composition helpers used across the layer and theme code.
 */
public final class Colors {

    private Colors() {
    }

    /**
     * Composes a multiplicative opacity into a colour, leaving its own alpha
     * channel intact. Mirrors the {@code ElementRect(alpha = …)} semantics used
     * throughout the theming and 3D geometry code.
     *
     * @param color the base colour
     * @param alpha multiplicative opacity; {@code >= 1.0} leaves the colour
     *              unchanged
     * @return the colour with its opacity multiplied by {@code alpha}
     */
    public static Color withAlpha(Color color, double alpha) {
        if (alpha >= 1.0) {
            return color;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                color.getOpacity() * alpha);
    }

    /**
     * Applies the shade-strength blend used by the 3-D lighting to a colour: a
     * positive blend factor brightens the value/lightness channel toward 1, a
     * negative one darkens toward 0, preserving hue and saturation. Matches
     * the diffuse shading the 3-D polygon geometry applies.
     *
     * @param color the base colour
     * @param blend the shade factor in {@code [-1, 1]} ({@code (lN - 0.5) * 2 * contrast})
     * @param hsl   {@code true} to blend the HSL lightness channel instead of
     *              the default HSV brightness
     * @return the shaded colour
     */
    public static Color shade(Color color, double blend, boolean hsl) {
        if (blend >= 0) {
            if (hsl) {
                double l = lightness(color);
                return hslToRgb(hueHsl(color), saturationHsl(color), l + blend * (1.0 - l),
                        color.getOpacity());
            }
            double b = color.getBrightness();
            return Color.hsb(color.getHue(), color.getSaturation(), b + blend * (1.0 - b),
                    color.getOpacity());
        }
        if (hsl) {
            double l = lightness(color);
            return hslToRgb(hueHsl(color), saturationHsl(color), l * (1.0 + blend),
                    color.getOpacity());
        }
        double b = color.getBrightness();
        return Color.hsb(color.getHue(), color.getSaturation(), b * (1.0 + blend),
                color.getOpacity());
    }

    private static double hueHsl(Color c) {
        double r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        if (max == min) {
            return 0.0;
        }
        double d = max - min;
        double h;
        if (max == r) {
            h = (g - b) / d + (g < b ? 6 : 0);
        } else if (max == g) {
            h = (b - r) / d + 2;
        } else {
            h = (r - g) / d + 4;
        }
        return h / 6.0;
    }

    private static double saturationHsl(Color c) {
        double r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        if (max == min) {
            return 0.0;
        }
        double d = max - min;
        return l > 0.5 ? d / (2 - max - min) : d / (max + min);
    }

    private static double lightness(Color c) {
        return (Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()))
                + Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()))) / 2.0;
    }

    private static Color hslToRgb(double h, double s, double l, double opacity) {
        if (s == 0) {
            return new Color(l, l, l, opacity);
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        double r = hueToRgb(p, q, h + 1.0 / 3.0);
        double g = hueToRgb(p, q, h);
        double b = hueToRgb(p, q, h - 1.0 / 3.0);
        return new Color(Math.clamp(r, 0, 1), Math.clamp(g, 0, 1), Math.clamp(b, 0, 1), opacity);
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }
}
