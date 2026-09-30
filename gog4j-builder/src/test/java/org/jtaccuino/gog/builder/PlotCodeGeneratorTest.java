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
package org.jtaccuino.gog.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.geometry.SurfaceMethod;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.stat.SmoothMethod;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link PlotCodeGenerator} emits the idiomatic gog4j recipe that
 * matches the {@link BuilderModel}: the dataset load, the aesthetic mapping,
 * the configured geoms (only parameters that were actually changed) and the
 * presentation options, with the imports the snippet needs.
 */
class PlotCodeGeneratorTest {

    @Test
    void emitsTheCoreScatterRecipe() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        var point = new BuilderModel.Geom(BuilderModel.GeomKind.POINT);
        point.doubles.put(BuilderModel.Params.SIZE, 2.0);
        model.geoms.add(point);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("import org.jtaccuino.gog.dflib.data.MpgDatasets;"),
                "the snippet must import the dataset loader");
        assertTrue(code.contains("import static org.jtaccuino.gog.Aes.aes;"));
        assertTrue(code.contains("import static org.jtaccuino.gog.Geoms.point;"));
        assertTrue(code.contains("var df = MpgDatasets.loadMpg();"));
        assertTrue(code.contains("ggplot(df, aes().x(\"displ\").y(\"hwy\"))"));
        assertTrue(code.contains("point().size(2.0)"),
                "only the changed point size is spelled out");
        assertTrue(code.contains("public static Plot<DataFrame> buildPlot()"));
    }

    @Test
    void emitsTheFullPipeline() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.color = "drv";
        var point = new BuilderModel.Geom(BuilderModel.GeomKind.POINT);
        model.geoms.add(point);
        var smooth = new BuilderModel.Geom(BuilderModel.GeomKind.SMOOTH);
        smooth.enums.put(BuilderModel.Params.METHOD, SmoothMethod.LM);
        smooth.doubles.put(BuilderModel.Params.SPAN, 0.8);
        model.geoms.add(smooth);
        model.scaleY = BuilderModel.ScaleKind.LOG10;
        model.coord = BuilderModel.CoordKind.FLIP;
        model.theme = BuilderModel.ThemeKind.BW;
        model.showXGrid = false;
        model.title = "MPG vs displacement";
        model.xLabel = "displ";
        model.yLabel = "hwy";

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("aes().x(\"displ\").y(\"hwy\").color(\"drv\")"));
        assertTrue(code.contains("point()"));
        assertTrue(code.contains("smooth().method(SmoothMethod.LM).span(0.8)"));
        assertTrue(code.contains("import org.jtaccuino.gog.stat.SmoothMethod;"));
        assertTrue(code.contains("scaleYLog10()"));
        assertTrue(code.contains("coordFlip()"));
        assertTrue(code.contains("Theme.theme_bw()"));
        assertTrue(code.contains(".theme(t -> t.showXGrid(false))"));
        assertTrue(code.contains("labs(\"MPG vs displacement\", \"displ\", \"hwy\")"));
    }

    @Test
    void omitsUnsetParameters() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(2));
        model.x = "bill_length_mm";
        model.y = "body_mass_g";
        var point = new BuilderModel.Geom(BuilderModel.GeomKind.POINT);
        model.geoms.add(point);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("point()"));
        assertFalse(code.contains("point().size"), "unset size must stay implicit");
        assertFalse(code.contains(".scales("), "AUTO scales must not be emitted");
        assertFalse(code.contains(".coord("), "the default coordinate system is implicit");
        assertFalse(code.contains(".theme("), "the default gray theme is implicit");
        assertFalse(code.contains(".labs("), "empty labels must not be emitted");
    }

    @Test
    void emitsBarPositionAndFill() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "class";
        model.y = "hwy";
        var bar = new BuilderModel.Geom(BuilderModel.GeomKind.BAR);
        bar.enums.put(BuilderModel.Params.POSITION, Position.DODGE);
        bar.colors.put(BuilderModel.Params.FILL, Color.web("#4c72b0"));
        model.geoms.add(bar);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("bar().position(Position.DODGE).fill(Color.web(\"#4c72b0\"))"));
        assertTrue(code.contains("import org.jtaccuino.gog.layer.Position;"));
        assertTrue(code.contains("import javafx.scene.paint.Color;"));
    }

    @Test
    void emitsManualColorScaleAndLimits() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.color = "drv";
        model.colorScale = BuilderModel.ColorScaleKind.MANUAL;
        model.manualColors.add(Color.RED);
        model.manualColors.add(Color.BLUE);
        model.scaleY = BuilderModel.ScaleKind.LIMITS;
        model.scaleYMin = 0.0;
        model.scaleYMax = 100.0;

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("import static org.jtaccuino.gog.scale.Scales.scaleColorManual;"),
                "manual scale must import its factory");
        assertTrue(code.contains("scaleColorManual()"), "manual scale factory emitted");
        assertTrue(code.contains(".color(\""), "manual scale maps the distinct values");
        assertTrue(code.contains("Color.web(\"#ff0000\")"), "the first manual color appears");
        assertTrue(code.contains("scaleYLimits(0.0, 100.0)"), "limits scale emitted");
    }

    @Test
    void emitsCoordAndAnnotationOptions() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.coord = BuilderModel.CoordKind.POLAR;
        model.polarTheta = "class";
        model.polarStart = 90.0;
        var hline = new BuilderModel.Geom(BuilderModel.GeomKind.HLINE);
        hline.doubles.put(BuilderModel.Params.YINTERCEPT, 3.5);
        hline.flags.put(BuilderModel.Params.DASHED, true);
        model.geoms.add(hline);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("coordPolar().theta(\"class\").start(90.0)"),
                "polar coord carries its theta and start options");
        assertTrue(code.contains("hline(3.5).dashed()"), "hline carries its intercept and dash flag");
    }

    @Test
    void emits3dCoordAndPoint3d() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.z = "drv";
        model.coord = BuilderModel.CoordKind.COORD3D;
        model.coord3dPitch = 35.0;
        model.coord3dPanels = CubePanel.NONE;
        model.coord3dLight = true;
        var point3d = new BuilderModel.Geom(BuilderModel.GeomKind.POINT3D);
        point3d.doubles.put(BuilderModel.Params.SIZE, 6.0);
        model.geoms.add(point3d);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("aes().x(\"displ\").y(\"hwy\").z(\"drv\")"),
                "the z binding must be emitted");
        assertTrue(code.contains("point3d().size(6.0)"));
        assertTrue(code.contains("import static org.jtaccuino.gog.Coords.coord3d;"));
        assertTrue(code.contains("coord3d().pitch(35.0).panels(CubePanel.NONE).light(Light3d.defaultLight())"),
                "the 3D view controls must be chained");
        assertTrue(code.contains("import org.jtaccuino.gog.coord.CubePanel;"));
        assertTrue(code.contains("import org.jtaccuino.gog.coord.Light3d;"));
    }

    @Test
    void emits3dSurfaceGeom() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(7));
        model.x = "x";
        model.y = "y";
        model.z = "z";
        model.coord = BuilderModel.CoordKind.COORD3D;
        var surface = new BuilderModel.Geom(BuilderModel.GeomKind.SURFACE3D);
        surface.enums.put(BuilderModel.Params.METHOD, SurfaceMethod.GRID);
        surface.colors.put(BuilderModel.Params.FILL, Color.web("#2ca02c"));
        model.geoms.add(surface);

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("surface3d().fill(Color.web(\"#2ca02c\")).method(SurfaceMethod.GRID)"));
        assertTrue(code.contains("import org.jtaccuino.gog.geometry.SurfaceMethod;"));
        assertTrue(code.contains("var df = MountainDatasets.loadMountain();"));
    }

    @Test
    void emitsWrapFacet() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.geoms.add(new BuilderModel.Geom(BuilderModel.GeomKind.POINT));
        model.facet = BuilderModel.FacetKind.WRAP;
        model.facetWrapColumn = "class";
        model.facetWrapCols = 3;

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("import org.jtaccuino.gog.Facets;"));
        assertTrue(code.contains(".facets(Facets.wrap(\"class\", 3))"));
    }

    @Test
    void emitsGridFacetWithOptions() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.geoms.add(new BuilderModel.Geom(BuilderModel.GeomKind.POINT));
        model.facet = BuilderModel.FacetKind.GRID;
        model.facetGridRow = "drv";
        model.facetGridCol = "cyl";
        model.facetScale = GridOptions.Scale.FREE_Y;
        model.facetSwitch = GridOptions.GridSwitch.BOTH;
        model.facetAsTable = false;

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains("import org.jtaccuino.gog.Facets;"));
        assertTrue(code.contains("import org.jtaccuino.gog.facet.GridOptions;"));
        assertTrue(code.contains(".facets(Facets.grid(\"drv\", \"cyl\", GridOptions.defaults()"
                        + ".withScale(GridOptions.Scale.FREE_Y)"
                        + ".withSwitch(GridOptions.GridSwitch.BOTH)"
                        + ".withAsTable(false)))"),
                "grid facets must carry the changed options only: " + code);
    }

    @Test
    void emitsGridFacetWithDefaultsAndNullColumn() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.geoms.add(new BuilderModel.Geom(BuilderModel.GeomKind.POINT));
        model.facet = BuilderModel.FacetKind.GRID;
        model.facetGridRow = "drv";

        var code = PlotCodeGenerator.emit(model);

        assertTrue(code.contains(".facets(Facets.grid(\"drv\", null))"),
                "default options and a null column must stay implicit: " + code);
        assertFalse(code.contains("GridOptions"), "no options import when all are default");
    }

    @Test
    void omitsFacetsWhenUnset() {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.geoms.add(new BuilderModel.Geom(BuilderModel.GeomKind.POINT));

        var code = PlotCodeGenerator.emit(model);

        assertFalse(code.contains(".facets("), "no facet call when none is selected");
        assertFalse(code.contains("import org.jtaccuino.gog.Facets;"));
    }
}
