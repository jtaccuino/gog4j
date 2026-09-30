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
package org.jtaccuino.gog.examples.dflib;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.col3d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot3d;
import static org.jtaccuino.gog.Ggplot.plot3d;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.PlotDescriptor3D;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Examples for the discoverable 3D entry points: {@link
 * org.jtaccuino.gog.Ggplot#plot3d(Object, org.jtaccuino.gog.Aes)} and {@link
 * org.jtaccuino.gog.Ggplot#ggplot3d(Object, org.jtaccuino.gog.Aes)} together
 * with the {@link PlotDescriptor3D} cube DSL.
 */
public class Plot3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Plot3dPlots() {
    }

    /** {@return a regular 4×4 grid with a computed column height} */
    private static DataFrame columnGrid() {
        var xs = new double[16];
        var ys = new double[16];
        var zs = new double[16];
        int k = 0;
        for (int x = 1; x <= 4; x++) {
            for (int y = 1; y <= 4; y++) {
                xs[k] = x;
                ys[k] = y;
                zs[k] = x * y;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z")
                .of(Series.ofDouble(xs), Series.ofDouble(ys), Series.ofDouble(zs));
    }

    /** {@return the simplest 3D recipe: {@code ggplot3d} with the default view} */
    @SamplePlot(description = "3D scatter of mpg displacement, highway mileage and car class.",
            title = "3D: ggplot3d() Scatter",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createScatter() {
        return ggplot3d(MpgDatasets.loadMpg(),
                aes().x("displ").y("hwy").z("drv").color("class"))
                .geoms(point3d().size(6.0))
                .labs(labs("ggplot3d(): 3D scatterplot", "displacement", "highway"));
    }

    /** {@return the descriptor DSL setting the cube rotation} */
    @SamplePlot(description = "The plot3d() descriptor with an explicit camera view.",
            title = "3D: plot3d() View",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.VIEW})
    public static Plot<DataFrame> createView() {
        PlotDescriptor3D<DataFrame> descriptor = plot3d(MpgDatasets.loadMpg(),
                aes().x("displ").y("hwy").z("drv").color("class"))
                .view(35, -75, -55);
        descriptor.geoms(point3d().size(6.0));
        return ggplot3d(descriptor)
                .labs(labs("plot3d().view(35, -75, -55)", "displacement", "highway"));
    }

    /** {@return the descriptor DSL setting a plot-level light on solid columns} */
    @SamplePlot(description = "Solid 3D columns lit by a default plot-level light.",
            title = "3D: plot3d() Light",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.COL},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.LIGHTING})
    public static Plot<DataFrame> createLight() {
        PlotDescriptor3D<DataFrame> descriptor = plot3d(columnGrid(),
                aes().x("x").y("y").z("z"))
                .panels(CubePanel.NONE)
                .light(Light3d.defaultLight());
        descriptor.geoms(col3d().fill(Color.web("#2ca02c")).color(Color.web("#1a6b1a")));
        return ggplot3d(descriptor)
                .labs(labs("plot3d().light(default)", "x", "y"));
    }
}
