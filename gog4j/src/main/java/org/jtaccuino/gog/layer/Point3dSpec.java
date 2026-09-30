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
 * Specification object for 3D point rendering properties (size, color, shape,
 * opacity) and the 2D reference elements that
 * {@link org.jtaccuino.gog.Geoms#point3d()} can project onto cube faces
 * (reference circles, points, and connecting lines).
 */
public class Point3dSpec {

    /**
     * How reference points are rendered onto cube faces.
     */
    public enum RefPoints {
        /** No reference points. */
        NONE,
        /** Reference points drawn as circles. */
        CIRCLES,
        /** Reference points drawn as plain dots. */
        POINTS
    }

    private double size = 7.0;
    private Color color;
    private PointShape shape = PointShape.CIRCLE;
    private double opacity = 1.0;

    private boolean rawPoints = true;
    private boolean refLines = false;
    private RefPoints refPoints = RefPoints.NONE;
    private CubeFace[] refFaces = {CubeFace.ZMIN};
    private double refCircleRadius = 1.5;
    private int refCircleVertices = 16;

    private Color refLineColor;
    private double refLineLinewidth = 0.25;
    private double[] refLineDashes;
    private Double refLineAlpha;

    private Color refPointColor;
    private Color refPointFill;
    private PointShape refPointShape;
    private Double refPointSize;
    private Double refPointAlpha;
    private double refPointStroke = 0.5;

    /** Creates a spec with default point-rendering properties. */
    public Point3dSpec() {}

    /** {@return the point diameter in pixels} */
    public double getSize() { return size; }

    /**
     * {@return this} Sets the point diameter in pixels.
     *
     * @param size the point diameter in pixels
     */
    public Point3dSpec size(double size) { this.size = size; return this; }

    /** {@return the fill color, or {@code null} if not set} */
    public Color getColor() { return color; }

    /**
     * {@return this} Sets the fill color.
     *
     * @param color the JavaFX {@link Color}
     */
    public Point3dSpec color(Color color) { this.color = color; return this; }

    /** {@return the point shape symbol} */
    public PointShape getShape() { return shape; }

    /**
     * {@return this} Sets the point shape symbol.
     *
     * @param shape the {@link PointShape} symbol
     */
    public Point3dSpec shape(PointShape shape) { this.shape = shape; return this; }

    /** {@return the opacity in [0, 1]} */
    public double getOpacity() { return opacity; }

    /**
     * {@return this} Sets the opacity in [0, 1].
     *
     * @param opacity the opacity between 0.0 and 1.0
     */
    public Point3dSpec opacity(double opacity) { this.opacity = opacity; return this; }

    /** {@return whether the raw 3D points are drawn} */
    public boolean isRawPoints() { return rawPoints; }

    /**
     * {@return this} Controls whether the raw 3D points are drawn (default {@code true}).
     *
     * @param rawPoints {@code true} to draw the raw points
     */
    public Point3dSpec rawPoints(boolean rawPoints) { this.rawPoints = rawPoints; return this; }

    /** {@return whether reference lines project each point onto its faces} */
    public boolean isRefLines() { return refLines; }

    /**
     * {@return this} Controls whether a dashed/solid line connects each point
     * to its projection on the configured faces (default {@code false}).
     *
     * @param refLines {@code true} to draw the reference lines
     */
    public Point3dSpec refLines(boolean refLines) { this.refLines = refLines; return this; }

    /** {@return the reference-point rendering mode} */
    public RefPoints getRefPoints() { return refPoints; }

    /**
     * {@return this} Sets how the projected reference points are rendered.
     *
     * @param refPoints {@code NONE} for no reference points (default),
     *                  {@code CIRCLES} for circular shadows on the faces, or
     *                  {@code POINTS} for single dots
     */
    public Point3dSpec refPoints(RefPoints refPoints) {
        this.refPoints = refPoints;
        return this;
    }

    /** {@return the cube faces reference elements project to} */
    public CubeFace[] getRefFaces() { return refFaces.clone(); }

    /**
     * {@return this} Sets the cube faces the reference elements project to
     * (any combination of {@link CubeFace}; the default is the bottom face
     * {@link CubeFace#ZMIN}).
     *
     * @param refFaces the cube faces; empty resets to {@link CubeFace#ZMIN}
     */
    public Point3dSpec refFaces(CubeFace... refFaces) {
        this.refFaces = refFaces == null || refFaces.length == 0
                ? new CubeFace[]{CubeFace.ZMIN}
                : refFaces.clone();
        return this;
    }

    /** {@return the circle radius as a fraction of the standardized cube} */
    public double getRefCircleRadius() { return refCircleRadius; }

    /**
     * {@return this} Sets the radius of the circular reference points as a
     * percentage of the standardized coordinate space (default {@code 1.5}).
     *
     * @param radius the circle radius as a percentage of the standard cube
     */
    public Point3dSpec refCircleRadius(double radius) { this.refCircleRadius = radius; return this; }

    /** {@return the vertex count of the circular reference points} */
    public int getRefCircleVertices() { return refCircleVertices; }

    /**
     * {@return this} Sets the number of vertices used to approximate the
     * circular reference points (default 16; higher is smoother).
     *
     * @param vertices the vertex count
     */
    public Point3dSpec refCircleVertices(int vertices) { this.refCircleVertices = vertices; return this; }

    // --- reference-line styling ---

    /** {@return the reference-line colour override, or {@code null} to inherit} */
    public Color getRefLineColor() { return refLineColor; }

    /**
     * {@return this} Sets the reference-line colour.
     *
     * @param color the line colour
     */
    public Point3dSpec refLineColor(Color color) { this.refLineColor = color; return this; }

    /** {@return the reference-line stroke width in pixels} */
    public double getRefLineLinewidth() { return refLineLinewidth; }

    /**
     * {@return this} Sets the reference-line stroke width in pixels
     * (default 0.25).
     *
     * @param linewidth the stroke width
     */
    public Point3dSpec refLineLinewidth(double linewidth) { this.refLineLinewidth = linewidth; return this; }

    /** {@return the reference-line dash pattern, or {@code null} for solid} */
    public double[] getRefLineDashes() { return refLineDashes; }

    /**
     * {@return this} Sets the reference-line dash pattern.
     *
     * @param dashes alternating on/off lengths, or {@code null} for solid
     */
    public Point3dSpec refLineDashes(double... dashes) { this.refLineDashes = dashes; return this; }

    /** {@return the reference-line opacity override, or {@code null} to inherit} */
    public Double getRefLineAlpha() { return refLineAlpha; }

    /**
     * {@return this} Sets the reference-line opacity.
     *
     * @param alpha the opacity in [0, 1]
     */
    public Point3dSpec refLineAlpha(double alpha) { this.refLineAlpha = alpha; return this; }

    // --- reference-point styling ---

    /** {@return the reference-point colour override, or {@code null} to inherit} */
    public Color getRefPointColor() { return refPointColor; }

    /**
     * {@return this} Sets the reference-point colour (their outline/stroke).
     *
     * @param color the colour
     */
    public Point3dSpec refPointColor(Color color) { this.refPointColor = color; return this; }

    /** {@return the reference-point fill override, or {@code null} to inherit} */
    public Color getRefPointFill() { return refPointFill; }

    /**
     * {@return this} Sets the reference-point fill.
     *
     * @param fill the fill colour
     */
    public Point3dSpec refPointFill(Color fill) { this.refPointFill = fill; return this; }

    /** {@return the reference-point shape override, or {@code null} to inherit} */
    public PointShape getRefPointShape() { return refPointShape; }

    /**
     * {@return this} Sets the reference-point shape symbol.
     *
     * @param shape the shape symbol
     */
    public Point3dSpec refPointShape(PointShape shape) { this.refPointShape = shape; return this; }

    /** {@return the reference-point size override, or {@code null} to inherit} */
    public Double getRefPointSize() { return refPointSize; }

    /**
     * {@return this} Sets the reference-point diameter in pixels.
     *
     * @param size the diameter
     */
    public Point3dSpec refPointSize(double size) { this.refPointSize = size; return this; }

    /** {@return the reference-point opacity override, or {@code null} to inherit} */
    public Double getRefPointAlpha() { return refPointAlpha; }

    /**
     * {@return this} Sets the reference-point opacity.
     *
     * @param alpha the opacity in [0, 1]
     */
    public Point3dSpec refPointAlpha(double alpha) { this.refPointAlpha = alpha; return this; }

    /** {@return the reference-point border stroke width} */
    public double getRefPointStroke() { return refPointStroke; }

    /**
     * {@return this} Sets the reference-point border stroke width
     * (default 0.5, circles only).
     *
     * @param stroke the stroke width
     */
    public Point3dSpec refPointStroke(double stroke) { this.refPointStroke = stroke; return this; }
}
