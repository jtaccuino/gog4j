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
import static org.jtaccuino.gog.Geoms.path3d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.segment3d;
import static org.jtaccuino.gog.Geoms.text3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.layer.Annotations3d;
import org.jtaccuino.gog.layer.Point3dSpec.RefPoints;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The 3-D primitive geometries: {@code Geoms.segment3d()},
 * {@code Geoms.path3d()}, {@code Geoms.text3d()} (billboard), the reference
 * elements of {@code Geoms.point3d()} (raw points, reference lines/circles on
 * cube faces), and the data-free {@code Annotations3d} family.
 */
public class Annotation3dPlots {

    /** Utility class; not meant to be instantiated. */
    private Annotation3dPlots() {
    }

    /** {@return the raw diamonds points with reference lines down to the zmin face} */
    @SamplePlot(description = "3D scatter with reference lines projected to the cube floor.",
            title = "3D: Point Reference Lines",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.REFERENCE})
    public static Plot<DataFrame> createRefLines() {
        return ggplot(data(), aes().x("carat").y("price").z("depth").color("clarity"))
                .geoms(point3d().size(4.0).opacity(0.6)
                        .refLines(true)
                        .refFaces(CubeFace.ZMIN))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.point3d(): reference lines to the zmin face",
                        "carat", "price"));
    }

    /** {@return pure reference circles (no raw points) lying on the zmin face} */
    @SamplePlot(description = "Reference circles on the cube floor instead of raw points.",
            title = "3D: Point Reference Circles",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.REFERENCE})
    public static Plot<DataFrame> createRefCircles() {
        return ggplot(data(), aes().x("carat").y("price").z("depth").color("clarity"))
                .geoms(point3d().size(4.0).opacity(0.6)
                        .rawPoints(false)
                        .refPoints(RefPoints.CIRCLES)
                        .refCircleRadius(2.0)
                        .refFaces(CubeFace.ZMIN))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.point3d(): reference circles on the zmin face",
                        "carat", "price"));
    }

    /** {@return a 3D segment layer drawing one segment per row} */
    @SamplePlot(description = "One 3D segment per row in synthetic data.",
            title = "3D: Segments",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.SEGMENT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createSegments3d() {
        return ggplot(segmentData(), aes().x("x").y("y").z("z")
                        .xend("xend").yend("yend").zend("zend"))
                .geoms(segment3d().color(Color.web("#1f77b4")).linewidth(1.2))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.segment3d(): one segment per row", "x", "y"));
    }

    /** {@return a 3D path that follows each group in row order} */
    @SamplePlot(description = "A winding 3D path, grouped into three trails.",
            title = "3D: Path",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.PATH},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createPath3d() {
        return ggplot(pathData(), aes().x("x").y("y").z("z").group("g"))
                .geoms(path3d().color(Color.web("#d62728")).linewidth(1.4))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.path3d(): a winding path through the cube", "x", "y"));
    }

    /** {@return billboard 3D text labels anchored to the raw diamonds points} */
    @SamplePlot(description = "Billboard 3D text labels anchored to diamond points.",
            title = "3D: Text Labels",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.TEXT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D})
    public static Plot<DataFrame> createText3d() {
        return ggplot(data().head(60), aes().x("carat").y("price").z("depth").label("cut"))
                .geoms(text3d().size(9.0).textOutlines(true).haloColor(Color.web("#f5f5f5")))
                .coord(Coords.coord3d())
                .labs(labs("Geoms.text3d(): billboard cut labels", "carat", "price"));
    }

    /** {@return scatter plus a data-free {@code Annotations3d} point, text, and segment} */
    @SamplePlot(description = "Scatter plus data-free 3D annotations: point, text and segment.",
            title = "3D: Annotations",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.COORD3D},
            features = {SampleFeature.THREE_D, SampleFeature.ANNOTATE})
    public static Plot<DataFrame> createAnnotate3d() {
        return ggplot(data(), aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .annotate(
                        Annotations3d.point(0.4, 15000, 60, 6.0, Color.web("#e377c2")),
                        Annotations3d.text(2.0, 15000, 60, "Annotations3d() marker", 12.0, Color.web("#8c564b"), 0, true),
                        Annotations3d.segment(0.4, 1000, 58, 2.8, 18000, 63,
                                Color.web("#17becf"), 1.6))
                .coord(Coords.coord3d())
                .labs(labs("Annotations3d(): point, text, and segment", "carat", "price"));
    }

    private static DataFrame data() {
        return DiamondsDatasets.loadDiamonds().head(1500);
    }

    private static DataFrame segmentData() {
        return DataFrame.byColumn("x", "y", "z", "xend", "yend", "zend")
                .of(Series.ofDouble(1, 2, 3, 1, 3, 5, 2, 4, 1),
                    Series.ofDouble(1, 1, 4, 4, 5, 5, 6, 6, 2),
                    Series.ofDouble(1, 5, 2, 4, 1, 6, 3, 2, 7),
                    Series.ofDouble(2, 3, 1, 3, 5, 2, 4, 1, 3),
                    Series.ofDouble(3, 2, 1, 5, 4, 6, 5, 3, 4),
                    Series.ofDouble(4, 6, 3, 5, 2, 7, 4, 1, 8));
    }

    private static DataFrame pathData() {
        return DataFrame.byColumn("x", "y", "z", "g")
                .of(Series.ofDouble(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
                    Series.ofDouble(1, 2, 2, 1, 3, 4, 4, 3, 5, 6, 6, 5),
                    Series.ofDouble(1, 2, 3, 2, 1, 2, 3, 2, 1, 2, 3, 2),
                    Series.of("a", "a", "a", "a", "b", "b", "b", "b", "c", "c", "c", "c"));
    }
}
