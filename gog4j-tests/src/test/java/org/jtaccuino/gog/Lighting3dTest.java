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

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Coords.coord3d;
import static org.jtaccuino.gog.Geoms.col3d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Guides.guide;
import static org.jtaccuino.gog.Guides.guideColorbar;
import static org.jtaccuino.gog.Guides.guideColorbar3d;
import static org.jtaccuino.gog.Guides.guideLegend;
import static org.jtaccuino.gog.Guides.guideLegend3d;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.guide.Guide;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The {@code Light3d} model and shading pipeline: how each {@link Light3d.Method}
 * turns a face's normal into a shade, how the {@link Light3d.Mode} blend spaces
 * differ, how contrast and the scene/camera anchors steer the result, and how
 * the three light slots (layer, coord, plot) resolve with the double-light
 * collision rule.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Lighting3dTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;
    private static final Color BASE = Color.web("#2ca02c");
    private static final Color EDGE = Color.web("#1a6b1a");

    private static final Pattern FILL = Pattern.compile("fill=\"#([0-9a-f]{6})\"");
    private static final Pattern STOP = Pattern.compile("<stop offset=\"[^\"]*\" stop-color=\"#([0-9a-f]{6})\"");

    // ----- Light3d model -----

    @Test
    void defaultLightMatchesTheLight3dSpec() {
        var light = Light3d.defaultLight();
        assertEquals(Light3d.Method.DIFFUSE, light.method());
        assertEquals(Light3d.Mode.HSV, light.mode());
        assertTrue(light.fill());
        assertTrue(light.color());
        assertEquals(1.0, light.contrast());
        assertEquals(Light3d.Anchor.SCENE, light.anchor());
        assertFalse(light.distanceFalloff());
        assertEquals(null, light.position());
        assertEquals(-1, light.backfaceScale());
        assertEquals(0, light.backfaceOffset());
        double[] d = light.direction();
        assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9);
    }

    @Test
    void noneMethodDisablesShading() {
        assertEquals(Light3d.Method.NONE, Light3d.none().method());
    }

    @Test
    void directionIsNormalisedAndDefensivelyCopied() {
        var light = Light3d.builder().direction(2, 0, 4).build();
        double len = Math.sqrt(2 * 2 + 4 * 4);
        assertArrayEquals(new double[] {2 / len, 0, 4 / len}, light.direction(), 1e-9);
        double[] view = light.direction();
        view[0] = 99;
        assertNotEquals(99, light.direction()[0], 1e-9);
    }

    @Test
    void positionIsDefensivelyCopied() {
        var original = new double[] {1, 2, 3};
        var light = Light3d.of(Light3d.Method.DIFFUSE, Light3d.Mode.HSL, true, true,
                1.0, new double[] {-0.5, 0, 1}, original, true, Light3d.Anchor.SCENE, -1, 0);
        original[0] = 99;
        assertArrayEquals(new double[] {1, 2, 3}, light.position(), 1e-9);
        light.position()[0] = 77;
        assertArrayEquals(new double[] {1, 2, 3}, light.position(), 1e-9);
    }

    @Test
    void equalLightsAgreeOnEqualsAndHashCode() {
        var a = Light3d.builder().mode(Light3d.Mode.HSL).contrast(0.4).build();
        var b = Light3d.builder().mode(Light3d.Mode.HSL).contrast(0.4).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Light3d.builder().mode(Light3d.Mode.HSL).contrast(0.8).build());
    }

    // ----- Shading engine (lights supplied at layer level) -----

    @Test
    void onlyAnExplicitNoneMethodRendersFlat() throws Exception {
        assertEquals(1, saturatedFills(render(columnPlot(Light3d.none()))).size(),
                "light(method='none') must render single-colour");
        assertEquals(1, saturatedFills(render(columnPlot(null).light(Light3d.none()))).size(),
                "a plot light of method='none' must flatten the faces");
    }

    @Test
    void diffuseShadesDistinctFaces() throws Exception {
        var svg = render(columnPlot(diffuse()));
        assertTrue(saturatedFills(svg).size() >= 3,
                "a diffuse light must separate the faces");
        assertTrue(brightnessSpread(svg) > 0.05,
                "the shaded faces must differ by more than antialiasing noise");
    }

    @Test
    void higherContrastChangesTheShades() throws Exception {
        Set<String> gentle = saturatedFills(render(columnPlot(diffuse(0.3))));
        Set<String> strong = saturatedFills(render(columnPlot(diffuse(1.0))));
        assertNotEquals(gentle, strong,
                "a stronger contrast must change the face shades");
    }

    @Test
    void rgbLightReplacesFillWithNormalColours() throws Exception {
        var set = saturatedFills(render(columnPlot(rgb())));
        assertTrue(set.size() >= 3, "an rgb light must map each normal to its own colour, got " + set);
        assertFalse(set.contains("2ca02c"),
                "the rgb light must substitute the fill rather than shade it, got " + set);
    }

    @Test
    void hslAndHsvBlendSpacesDiffer() throws Exception {
        Set<String> hsv = saturatedFills(render(columnPlot(diffuse(Light3d.Mode.HSV, 0.7))));
        Set<String> hsl = saturatedFills(render(columnPlot(diffuse(Light3d.Mode.HSL, 0.7))));
        assertNotEquals(hsv, hsl, "the HSV and HSL blends must land on different colours");
    }

    @Test
    void directMethodClampsTheUnlitSide() throws Exception {
        // Shooting straight up, the column's top face (pointing +z) faces away
        // from the light. Diffuse colours it dark; direct clamps it to the
        // base fill, revealing the difference.
        Set<String> diffuse = saturatedFills(render(columnPlot(diffuseDirection(0, 0, -1))));
        Set<String> direct = saturatedFills(render(columnPlot(
                Light3d.builder().method(Light3d.Method.DIRECT).mode(Light3d.Mode.HSL)
                        .contrast(0.6).direction(0, 0, -1).build())));
        assertNotEquals(diffuse, direct,
                "direct (clamped) and diffuse shading must differ");
    }

    @Test
    void cameraAnchorRotatesNormalsWithTheView() throws Exception {
        Set<String> scene = saturatedFills(render(columnPlot(scatter(0.8, Light3d.Anchor.SCENE))));
        Set<String> camera = saturatedFills(render(columnPlot(scatter(0.8, Light3d.Anchor.CAMERA))));
        assertNotEquals(scene, camera, "a camera-anchored light must shade differently");
    }

    @Test
    void lightDirectionFlipsTheShadowedSide() throws Exception {
        // The face pointing into the light brightens, the one pointing away
        // darkens — so the whole SVG must change when the light is mirrored.
        String above = render(columnPlot(diffuseDirection(0, 0, 1)));
        String below = render(columnPlot(diffuseDirection(0, 0, -1)));
        assertNotEquals(above, below, "shooting the light from below must flip which faces brighten");
    }

    @Test
    void positionalLightWithFalloffShadesTheGrid() throws Exception {
        var set = saturatedFills(render(gridPlot(Light3d.builder().mode(Light3d.Mode.HSL).contrast(0.9)
                .position(3, 3, 2).distanceFalloff(true).build())));
        assertTrue(set.size() >= 3, "a positional light must shade the columns, got " + set);
    }

    // ----- Precedence: layer > coord > plot > default -----

    @Test
    void plotLightAppliesWhenLayerLightIsNull() throws Exception {
        var svg = render(columnPlot(null).light(diffuse()));
        assertTrue(saturatedFills(svg).size() >= 3, "a plot light must shade the faces");
    }

    @Test
    void coordLightAppliesWhenLayerLightIsNull() throws Exception {
        var svg = render(columnPlot(null).coord(coord3d().panels(CubePanel.NONE).light(diffuse())));
        assertTrue(saturatedFills(svg).size() >= 3, "the coord light must shade the faces");
    }

    @Test
    void layerLightOverridesThePlotLight() throws Exception {
        assertEquals(1, saturatedFills(render(columnPlot(Light3d.none()).light(diffuse()))).size(),
                "the layer's none light must flatten the faces over the plot light");
    }

    @Test
    void layerLightOverridesTheCoordLight() throws Exception {
        var plot = columnPlot(Light3d.none())
                .coord(coord3d().panels(CubePanel.NONE).light(diffuse()));
        assertEquals(1, saturatedFills(render(plot)).size(),
                "the layer's none light must flatten the faces over the coord light");
    }

    @Test
    void coordAndPlotLightsMustNotBothBeSet() {
        var plot = columnPlot(null)
                .coord(coord3d().panels(CubePanel.NONE).light(diffuse()))
                .light(diffuse());
        assertThrows(IllegalArgumentException.class, () -> render(plot),
                "coord3d(light=...) and Plot.light(...) may not be combined");
    }

    @Test
    void solidLayersShadeByDefaultForTheClassicCubeLook() throws Exception {
        // col3d()/bar3d()/voxel3d() attach the SOLID_LIGHT default, so
        // even without any explicit light the visible faces separate.
        var svg = render(columnPlot());
        assertTrue(saturatedFills(svg).size() >= 3,
                "solid layers carry a default light, got "
                        + saturatedFills(svg));
    }

    // ----- 3D guides (shared fill/colour scales) -----

    @Test
    void guideColorbar3dShadesTheGradientInSvg() throws Exception {
        Set<String> shaded = gradientStops(render(colourGuidePlot(guideColorbar3d())));
        Set<String> plain = gradientStops(render(colourGuidePlot(guideColorbar())));
        assertTrue(shaded.size() >= 2,
                "a mapped colour column must resolve a colourbar gradient, got " + shaded);
        assertNotEquals(plain, shaded,
                "guideColorbar3d() must shade the gradient stops differently");
    }

    @Test
    void guideLegend3dShadesTheKeys() throws Exception {
        String shaded = render(groupGuidePlot(guideLegend3d()));
        String plain = render(groupGuidePlot(guideLegend()));
        assertTrue(shaded.contains("first"), "the categorical legend must list its labels");
        assertNotEquals(plain, shaded,
                "guideLegend3d() must shade the key tiles differently");
    }

    // ----- Fixtures -----

    /** {@return a geometry-layer light with the default direction (-0.5, 0, 1)} */
    private static Light3d diffuse() {
        return diffuse(0.6);
    }

    private static Light3d diffuse(double contrast) {
        return diffuse(Light3d.Mode.HSL, contrast);
    }

    private static Light3d diffuse(Light3d.Mode mode, double contrast) {
        return Light3d.builder().mode(mode).contrast(contrast).build();
    }

    private static Light3d rgb() {
        return Light3d.of(Light3d.Method.RGB, Light3d.Mode.HSV, true, true,
                1.0, new double[] {-0.5, 0, 1}, null, false, Light3d.Anchor.SCENE, -1, 0);
    }

    private static Light3d scatter(double contrast, Light3d.Anchor anchor) {
        return Light3d.builder().mode(Light3d.Mode.HSL).contrast(contrast).anchor(anchor).build();
    }

    private static Light3d diffuseDirection(double x, double y, double z) {
        return Light3d.builder().mode(Light3d.Mode.HSL).contrast(1.0).direction(x, y, z).build();
    }

    /** {@return a single flat column whose visible faces are lighting surfaces;
     * {@code null} means the layer inherits (no solid default light)} */
    private static Plot<DataFrame> columnPlot(Light3d light) {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {1, 2}),
                        Series.ofDouble(new double[] {3, 4}));
        Plot<DataFrame> plot = ggplot(df, aes().x("x").y("y").z("z"))
                .coord(coord3d().panels(CubePanel.NONE))
                .labs(labs("single column", "x", "y"));
        return apply(plot, light);
    }

    private static Plot<DataFrame> gridPlot(Light3d light) {
        var rnd = new Random(7);
        var xs = new double[25];
        var ys = new double[25];
        var zs = new double[25];
        int k = 0;
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                xs[k] = x;
                ys[k] = y;
                zs[k] = x + y + rnd.nextGaussian() * 0.5;
                k++;
            }
        }
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
        Plot<DataFrame> plot = ggplot(df, aes().x("x").y("y").z("z"))
                .coord(coord3d().panels(CubePanel.NONE))
                .labs(labs("grid", "x", "y"));
        return apply(plot, light);
    }

    private static Plot<DataFrame> apply(Plot<DataFrame> plot, Light3d light) {
        var geom = col3d().fill(BASE).color(EDGE);
        if (light != null) {
            geom.light(light);
        } else {
            geom.light((Light3d) null);
        }
        return plot.geoms(geom);
    }

    /** The column without any explicit layer light (the solid default SHAPES). */
    private static Plot<DataFrame> columnPlot() {
        return columnPlot(Light3d.none()).geoms(col3d().fill(BASE).color(EDGE));
    }

    /** {@return a 3-D scatter sharing the z column's continuous colour scale} */
    private static Plot<DataFrame> colourGuidePlot(Guide<?> g) {
        var df = DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(new double[] {1, 2, 3, 4, 5, 6}),
                        Series.ofDouble(new double[] {1, 1, 2, 2, 3, 3}),
                        Series.ofDouble(new double[] {3, 2, 5, 4, 3, 6}));
        return ggplot(df, aes().x("x").y("y").z("z").color("z"))
                .geoms(point3d())
                .coord(coord3d().panels(CubePanel.NONE))
                .guides(guide(Aesthetic.COLOR, g))
                .labs(labs("colourbar guide", "x", "y"));
    }

    /** {@return a 3-D scatter grouped by a categorical column} */
    private static Plot<DataFrame> groupGuidePlot(Guide<?> g) {
        var df = DataFrame.byColumn("x", "y", "z", "grp")
                .of(Series.ofDouble(new double[] {1, 2, 3, 4, 5, 6}),
                        Series.ofDouble(new double[] {1, 1, 2, 2, 3, 3}),
                        Series.ofDouble(new double[] {3, 2, 5, 4, 3, 6}),
                        Series.of("first", "second", "third",
                                "first", "second", "third"));
        return ggplot(df, aes().x("x").y("y").z("z").color("grp"))
                .geoms(point3d())
                .coord(coord3d().panels(CubePanel.NONE))
                .guides(guide(Aesthetic.COLOR, g))
                .labs(labs("legend guide", "x", "y"));
    }

    // ----- SVG analysis -----

    /** The distinct gradient stop colours of the SVG's colorbar gradients. */
    private static Set<String> gradientStops(String svg) {
        var out = new LinkedHashSet<String>();
        Matcher m = STOP.matcher(svg);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /** The distinct saturated fills of filled polygons (base-colour faces). */
    private static Set<String> saturatedFills(String svg) {
        var out = new LinkedHashSet<String>();
        Matcher m = FILL.matcher(svg);
        while (m.find()) {
            String hex = m.group(1);
            double[] rgb = hex(hex);
            double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
            double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
            if (max - min > 0.15) {
                out.add(hex);
            }
        }
        return out;
    }

    /** The difference between the lightest and darkest shaded face fill. */
    private static double brightnessSpread(String svg) {
        double hi = -1, lo = 2;
        Matcher m = FILL.matcher(svg);
        while (m.find()) {
            double[] rgb = hex(m.group(1));
            double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
            double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
            if (max - min <= 0.15) {
                continue;
            }
            hi = Math.max(hi, min);
            lo = Math.min(lo, min);
        }
        return hi - lo;
    }

    private static double[] hex(String rrgb) {
        return new double[] {
                Integer.parseInt(rrgb.substring(0, 2), 16) / 255.0,
                Integer.parseInt(rrgb.substring(2, 4), 16) / 255.0,
                Integer.parseInt(rrgb.substring(4, 6), 16) / 255.0
        };
    }

    private static String render(Plot<DataFrame> plot) {
        try {
            return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("render failed", e);
        }
    }
}
