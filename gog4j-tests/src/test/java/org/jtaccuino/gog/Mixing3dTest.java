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
import static org.jtaccuino.gog.Geoms.density2d;
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.layer.Positions.positionOnFace;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.layer.GeomTile;
import org.jtaccuino.gog.layer.PositionAdjust;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The {@code Positions.positionOnFace(...)} pipeline for 2-D/3-D layer mixing:
 * how the {@link PositionAdjust.PositionOnFace} factories parse and validate
 * cube faces and in-face axes, how a 3-D layer flattens onto a face while a 2-D
 * layer is placed along the face plane, and the guard rails that reject a face
 * placement without a 3-D scene.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Mixing3dTest {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 640;

    // ----- Positions.positionOnFace factories -----

    @Test
    void positionOnFaceParsesFaceNames() {
        var zmin = positionOnFace("zmin");
        assertEquals(CubeFace.ZMIN, ((PositionAdjust.PositionOnFace) zmin).face());
        assertEquals(null, ((PositionAdjust.PositionOnFace) zmin).axes());
        assertEquals(CubeFace.YMAX, ((PositionAdjust.PositionOnFace) positionOnFace("ymax")).face());
        assertEquals(CubeFace.XMIN, ((PositionAdjust.PositionOnFace) positionOnFace(CubeFace.XMIN)).face());
    }

    @Test
    void positionOnFaceRejectsUnknownFaceNames() {
        assertThrows(IllegalArgumentException.class, () -> positionOnFace("front"));
        assertThrows(IllegalArgumentException.class, () -> positionOnFace("3D"));
    }

    @Test
    void positionOnFaceCarriesTheTwoInFaceAxes() {
        var placed = (PositionAdjust.PositionOnFace) positionOnFace(CubeFace.ZMIN, "x", "y");
        assertEquals(CubeFace.ZMIN, placed.face());
        assertEquals(List.of("x", "y"), placed.axes());
        assertEquals(List.of("y", "z"),
                ((PositionAdjust.PositionOnFace) positionOnFace("xmin", "y", "z")).axes());
    }

    @Test
    void positionOnFaceRejectsInvalidAxes() {
        // the face axis is not in-face
        assertThrows(IllegalArgumentException.class, () -> positionOnFace(CubeFace.ZMIN, "x", "z"));
        // duplicate dimensions
        assertThrows(IllegalArgumentException.class, () -> positionOnFace(CubeFace.ZMIN, "y", "y"));
        // unknown dimension name
        assertThrows(IllegalArgumentException.class, () -> positionOnFace(CubeFace.ZMIN, "x", "r"));
    }

    // ----- validation of face placement against the scene -----

    @Test
    void facePlacementWithoutACubeRejects() {
        var df = grid();
        var plot = ggplot(df, aes().x("x").y("y").fill("fill"))
                .layer(new GeomTile<DataFrame>()
                                .position(positionOnFace(CubeFace.ZMAX, "x", "y")),
                        aes().fill("fill"));
        assertThrows(IllegalArgumentException.class, () -> render(plot));
    }

    @Test
    void twoDimensionalLayerRequiresAxes() {
        var df = grid();
        assertThrows(IllegalArgumentException.class,
                () -> render(ggplot(df, aes().x("x").y("y").z("z"))
                        .geoms(point3d())
                        .geoms(tile().position(positionOnFace(CubeFace.ZMAX)))
                        .coord(coord3d())));
    }

    @Test
    void threeDimensionalLayerCannotTakeAxes() {
        var df = grid();
        assertThrows(IllegalArgumentException.class,
                () -> render(ggplot(df, aes().x("x").y("y").z("z"))
                        .geoms(point3d().position(positionOnFace(CubeFace.ZMIN, "x", "y")))
                        .coord(coord3d())));
    }

    // ----- render smoke tests -----

    @Test
    void density2dOnAFaceRendersContours() {
        String svg = render(ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(point3d())
                .geoms(density2d().bins(4).position(positionOnFace(CubeFace.ZMIN, "x", "y")))
                .coord(coord3d()));
        assertTrue(svg.contains("<path"), "density contours must emit path elements");
    }

    @Test
    void tilesOnAFaceRenderRects() {
        String svg = render(ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(point3d())
                .layer(new GeomTile<DataFrame>().cmap("plasma")
                                .position(positionOnFace(CubeFace.ZMAX, "x", "y")),
                        aes().fill("fill"))
                .coord(coord3d()));
        assertTrue(svg.contains("<rect"), "face-placed heatmap tiles must emit rect elements");
        assertTrue(svg.contains("fill="), "tiles must carry a fill colour");
    }

    @Test
    void flattened3dRendersPoints() {
        String svg = render(ggplot(grid(), aes().x("x").y("y").z("z"))
                .geoms(point3d())
                .geoms(point3d().color(Color.web("#d62728"))
                        .position(positionOnFace(CubeFace.ZMIN)))
                .coord(coord3d()));
        assertNotNull(svg);
        assertTrue(svg.contains("circle") || svg.contains("<path"),
                "flattened 3-D points must render as circles or paths");
    }

    // ----- Fixtures -----

    /** {@return a regular 5×5 grid carrying x, y, z and a fill value} */
    private static DataFrame grid() {
        var xs = new double[25];
        var ys = new double[25];
        var zs = new double[25];
        var fills = new double[25];
        int k = 0;
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                xs[k] = x;
                ys[k] = y;
                zs[k] = x + y;
                fills[k] = x * y;
                k++;
            }
        }
        return DataFrame.byColumn("x", "y", "z", "fill")
                .of(Series.ofDouble(xs), Series.ofDouble(ys),
                        Series.ofDouble(zs), Series.ofDouble(fills));
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
