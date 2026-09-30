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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dflib.DataFrame;
import org.jtaccuino.gog.examples.dflib.Volume3dPlots;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The solid 3D geometries ({@code Geoms.col3d()}/{@code Geoms.bar3d()}/
 * {@code Geoms.voxel3d()}) render their visible side faces in different shades
 * of the base colour via each layer's {@code SOLID_LIGHT} — an HSL diffuse
 * light from {@code (-0.5, 0, 1)} at {@code contrast = 0.4} — instead of a
 * single flat fill.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Solid3dLightingTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    private static final Pattern FILL = Pattern.compile("fill=\"#([0-9a-f]{6})\"");

    @Test
    void singleColumnsRenderThreeShadesOfTheBaseColour() throws Exception {
        var svg = renderSvg(Volume3dPlots.createSingleColumn());
        var shades = shadedFills(svg, 0.25);
        assertTrue(shades.size() >= 3,
                "a column's top and two side faces must shade differently, got " + shades);
        double spread = brightnessShades(svg, 0.25);
        assertTrue(spread > 0.05,
                "the lit faces must differ by more than antialiasing noise, spread=" + spread);
    }

    @Test
    void discreteBarsRenderDifferentShadesPerVisibleFace() throws Exception {
        var svg = renderSvg(Volume3dPlots.createBarsDiscrete());
        var shades = shadedFills(svg, 0.40);
        assertTrue(shades.size() >= 3,
                "discrete bars must shade their visible faces differently, got " + shades);
    }

    @Test
    void continuousHistogramBarsStillShadeAtFullHSVBrightness() throws Exception {
        // The bins' fill (#ff7f00) has full HSV brightness but sits mid-way in
        // HSL lightness, where the SOLID_LIGHT's HSL blend can both darken the
        // shadowed faces and brighten the lit ones, separating the histogram's
        // visible faces into distinguishable shades.
        var svg = renderSvg(Volume3dPlots.createBarsContinuous());
        var shades = shadedFills(svg, 0.20);
        assertTrue(shades.size() >= 3,
                "the 2-D histogram must shade its visible faces, got " + shades);
        double spread = brightnessShades(svg, 0.20);
        assertTrue(spread > 0.05,
                "the lit histogram faces must differ by more than antialiasing noise, spread=" + spread);
    }

    /** The distinct fill hex values of filled polygons whose colour is saturated
     * enough to be a bar face (not a grey cube wall). */
    private static Set<String> shadedFills(String svg, double saturation) {
        var out = new LinkedHashSet<String>();
        for (var match : fills(svg)) {
            double[] rgb = hex(match);
            double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
            double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
            if (max - min > saturation) {
                out.add(match);
            }
        }
        return out;
    }

    /** The difference between the lightest and darkest shaded polygon fill. */
    private static double brightnessShades(String svg, double saturation) {
        double hi = -1, lo = 2;
        for (var match : fills(svg)) {
            double[] rgb = hex(match);
            double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
            double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
            if (max - min <= saturation) {
                continue;
            }
            // Use the minimum channel so the metric detects visible RGB
            // shading even when a saturated primary (e.g. full-brightness red
            // in #ff7f00) stays at 1.0 across all lit faces.
            hi = Math.max(hi, min);
            lo = Math.min(lo, min);
        }
        return hi - lo;
    }

    private static Iterable<String> fills(String svg) {
        var out = new LinkedHashSet<String>();
        Matcher m = FILL.matcher(svg);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private static double[] hex(String rrgb) {
        return new double[] {
                Integer.parseInt(rrgb.substring(0, 2), 16) / 255.0,
                Integer.parseInt(rrgb.substring(2, 4), 16) / 255.0,
                Integer.parseInt(rrgb.substring(4, 6), 16) / 255.0
        };
    }

    private static String renderSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }
}
