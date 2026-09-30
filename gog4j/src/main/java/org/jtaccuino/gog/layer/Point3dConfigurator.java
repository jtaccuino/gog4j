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
package org.jtaccuino.gog.layer;

import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.CubeFace;

/**
 * Configurator (builder) for the {@link GeomPoint3d} geometry layer.
 * Provides a fluent API for setting point aesthetics.
 *
 * @param <DF> the data-frame type
 */
public class Point3dConfigurator<DF> extends BasePrimitive3dConfigurator<DF, GeomPoint3d<DF>, Point3dConfigurator<DF>> {
    private final Point3dSpec spec;

    /**
     * Creates a configurator for the given 3D point geometry layer.
     *
     * @param geom the underlying 3D point geometry layer
     * @param spec the specification to configure
     */
    public Point3dConfigurator(GeomPoint3d<DF> geom, Point3dSpec spec) {
        super(geom);
        this.spec = spec;
        this.geom.setSpec(spec);
    }

    /**
     * {@return this} Sets the point diameter in pixels.
     *
     * @param size the point diameter in pixels
     */
    public Point3dConfigurator<DF> size(double size) { this.spec.size(size); return this; }

    /**
     * {@return this} Sets the fill color.
     *
     * @param color the JavaFX fill {@link Color}
     */
    public Point3dConfigurator<DF> color(Color color) { this.spec.color(color); return this; }

    /**
     * {@return this} Sets the point shape symbol.
     *
     * @param shape the {@link PointShape} symbol
     */
    public Point3dConfigurator<DF> shape(PointShape shape) { this.spec.shape(shape); return this; }

    /**
     * {@return this} Sets the opacity in [0, 1].
     *
     * @param opacity the opacity between 0.0 and 1.0
     */
    public Point3dConfigurator<DF> opacity(double opacity) { this.spec.opacity(opacity); return this; }

    /**
     * {@return this} Sets the layer's position adjustment — e.g. cube-face
     * flattening via {@link Positions#positionOnFace} so every point projects
     * onto one face of the cube.
     *
     * @param position the {@link PositionAdjust} to apply
     */
    public Point3dConfigurator<DF> position(PositionAdjust position) { this.geom().position(position); return this; }

    /**
     * {@return this} Controls whether the raw 3D points are drawn.
     *
     * @param rawPoints {@code true} to draw the raw points (default)
     */
    public Point3dConfigurator<DF> rawPoints(boolean rawPoints) { this.spec.rawPoints(rawPoints); return this; }

    /**
     * {@return this} Controls whether reference lines project each point onto its faces.
     *
     * @param refLines {@code true} to draw the reference lines
     */
    public Point3dConfigurator<DF> refLines(boolean refLines) { this.spec.refLines(refLines); return this; }

    /**
     * {@return this} Sets how the projected reference points are rendered.
     *
     * @param refPoints {@code NONE} for no reference points (default),
     *                  {@code CIRCLES} for circular shadows, or {@code POINTS}
     *                  for single dots
     */
    public Point3dConfigurator<DF> refPoints(Point3dSpec.RefPoints refPoints) { this.spec.refPoints(refPoints); return this; }

    /**
     * {@return this} Sets the cube faces the reference elements project to.
     *
     * @param refFaces the cube faces (default {@link CubeFace#ZMIN})
     */
    public Point3dConfigurator<DF> refFaces(CubeFace... refFaces) { this.spec.refFaces(refFaces); return this; }

    /**
     * {@return this} Sets the radius of the circular reference points as a
     * percentage of the standardized coordinate space.
     *
     * @param radius the circle radius as a percentage (default {@code 1.5})
     */
    public Point3dConfigurator<DF> refCircleRadius(double radius) { this.spec.refCircleRadius(radius); return this; }

    /**
     * {@return this} Sets the vertex count of the circular reference points.
     *
     * @param vertices the vertex count (default 16)
     */
    public Point3dConfigurator<DF> refCircleVertices(int vertices) { this.spec.refCircleVertices(vertices); return this; }

    /**
     * {@return this} Sets the reference-line colour.
     *
     * @param color the line colour
     */
    public Point3dConfigurator<DF> refLineColor(Color color) { this.spec.refLineColor(color); return this; }

    /**
     * {@return this} Sets the reference-line stroke width in pixels.
     *
     * @param linewidth the stroke width (default 0.25)
     */
    public Point3dConfigurator<DF> refLineLinewidth(double linewidth) { this.spec.refLineLinewidth(linewidth); return this; }

    /**
     * {@return this} Sets the reference-line dash pattern.
     *
     * @param dashes alternating on/off lengths, or {@code null} for solid
     */
    public Point3dConfigurator<DF> refLineDashes(double... dashes) { this.spec.refLineDashes(dashes); return this; }

    /**
     * {@return this} Sets the reference-line opacity.
     *
     * @param alpha the opacity in [0, 1]
     */
    public Point3dConfigurator<DF> refLineAlpha(double alpha) { this.spec.refLineAlpha(alpha); return this; }

    /**
     * {@return this} Sets the reference-point colour.
     *
     * @param color the colour
     */
    public Point3dConfigurator<DF> refPointColor(Color color) { this.spec.refPointColor(color); return this; }

    /**
     * {@return this} Sets the reference-point fill.
     *
     * @param fill the fill colour
     */
    public Point3dConfigurator<DF> refPointFill(Color fill) { this.spec.refPointFill(fill); return this; }

    /**
     * {@return this} Sets the reference-point shape symbol.
     *
     * @param shape the shape symbol
     */
    public Point3dConfigurator<DF> refPointShape(PointShape shape) { this.spec.refPointShape(shape); return this; }

    /**
     * {@return this} Sets the reference-point diameter in pixels.
     *
     * @param size the diameter
     */
    public Point3dConfigurator<DF> refPointSize(double size) { this.spec.refPointSize(size); return this; }

    /**
     * {@return this} Sets the reference-point opacity.
     *
     * @param alpha the opacity in [0, 1]
     */
    public Point3dConfigurator<DF> refPointAlpha(double alpha) { this.spec.refPointAlpha(alpha); return this; }

    /**
     * {@return this} Sets the reference-point border stroke width.
     *
     * @param stroke the stroke width
     */
    public Point3dConfigurator<DF> refPointStroke(double stroke) { this.spec.refPointStroke(stroke); return this; }
}
