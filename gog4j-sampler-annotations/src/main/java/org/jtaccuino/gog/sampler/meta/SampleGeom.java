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
package org.jtaccuino.gog.sampler.meta;

/**
 * The {@link TagKind#GEOM} catalogue: the geom layers an example renders. The
 * values mirror the {@code Geoms} factory methods imported by the examples.
 */
public enum SampleGeom implements SampleTag {

    POINT("point", "Point"),
    SMOOTH("smooth", "Smooth"),
    BOXPLOT("boxplot", "Boxplot"),
    JITTER("jitter", "Jitter"),
    VIOLIN("violin", "Violin"),
    DENSITY("density", "Density"),
    DENSITY2D("density2d", "Density 2D"),
    DENSITY2D_FILLED("density2d_filled", "Density 2D Filled"),
    TILE("tile", "Tile"),
    COL("col", "Column"),
    BAR("bar", "Bar"),
    LINE("line", "Line"),
    AREA("area", "Area"),
    POLYGON("polygon", "Polygon"),
    SURFACE("surface", "Surface"),
    VOXEL("voxel", "Voxel"),
    HULL("hull", "Hull"),
    RIDGELINE("ridgeline", "Ridgeline"),
    CONTOUR("contour", "Contour"),
    TEXT("text", "Text"),
    FUNCTION("function", "Function"),
    ABLINE("abline", "Abline"),
    VLINE("vline", "Vline"),
    HLINE("hline", "Hline"),
    STEP("step", "Step"),
    PATH("path", "Path"),
    SEGMENT("segment", "Segment"),
    CURVE("curve", "Curve"),
    ERRORBAR("errorbar", "Errorbar"),
    ERRORBARH("errorbarh", "Errorbar (H)"),
    CROSSBAR("crossbar", "Crossbar"),
    HISTOGRAM("histogram", "Histogram"),
    FREQPOLY("freqpoly", "Freqpoly"),
    LINERANGE("linerange", "Line Range"),
    POINTRANGE("pointrange", "Point Range"),
    RIBBON("ribbon", "Ribbon"),
    NONE("none", "No Geom");

    private final String feature;
    private final String label;

    SampleGeom(String feature, String label) {
        this.feature = feature;
        this.label = label;
    }

    @Override
    public TagKind kind() {
        return TagKind.GEOM;
    }

    @Override
    public String feature() {
        return feature;
    }

    @Override
    public String label() {
        return label;
    }
}
