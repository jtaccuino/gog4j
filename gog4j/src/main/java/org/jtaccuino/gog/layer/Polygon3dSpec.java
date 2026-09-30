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
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.SurfaceMethod;

/**
 * Rendering specification for the 3D polygon geometry family
 * ({@link org.jtaccuino.gog.Geoms#polygon3d()},
 * {@link org.jtaccuino.gog.Geoms#surface3d()},
 * {@link org.jtaccuino.gog.Geoms#ridgeline3d()},
 * {@link org.jtaccuino.gog.Geoms#contour3d()} and
 * {@link org.jtaccuino.gog.Geoms#smooth3d()}), holding the same parameters the
 * {@link GeomPolygon3d} base geometry uses.
 * <p>
 * It carries the aesthetic defaults (fill, stroke colour, line width, alpha),
 * the depth-ordering strategy ({@link SortMethod}), the depth scaling of
 * stroke widths, optional back-face culling and forced convexity, and the
 * optional {@link Light3d}. A separate {@link #getDefaultLight() default
 * light} (e.g. {@link #SOLID_LIGHT} for the solid geometries) shades the
 * polygons when no light is set at the layer, coord or plot level. The
 * surface/ridgeline/contour configuration parameters (grid type, ridge
 * direction, contour breaks…) that the geometry passes on to the
 * tessellation helpers are also held here.
 */
public class Polygon3dSpec {

    /** How the polygon primitives are ordered back-to-front. */
    public enum SortMethod {
        /** Pairwise depth sorting for polygons, painter's fallback otherwise. */
        AUTO,
        /** Per-primitive pairwise depth comparison. */
        PAIRWISE,
        /** Single pass over group means, far to near. */
        PAINTER
    }

    private Color fill = Color.GRAY;
    private Color colour = Color.GRAY;
    private double linewidth = 1.0;
    private double[] linetype;
    private double alpha = 1.0;

    private SortMethod sortMethod = SortMethod.AUTO;
    private boolean scaleDepth = true;
    private boolean cullBackfaces = false;
    private boolean forceConvex = false;
    private Light3d light;
    private Light3d defaultLight;

    // Surface tessellation (Geoms.surface3d()).
    private SurfaceMethod method = SurfaceMethod.AUTO;
    private GridGeometry grid = GridGeometry.RECTANGLE;

    // Ridgeline construction (Geoms.ridgeline3d()).
    private GridDirection direction = GridDirection.X;
    private Double base;

    // Contour construction (Geoms.contour3d()).
    private int bins = 10;
    private Double binwidth;
    private double[] breaks;

    // Smooth overlay styling (Geoms.smooth3d()).
    private Color pointColour;
    private Color pointFill;
    private Double pointSize;
    private PointShape pointShape;
    private Double pointAlpha;
    private double pointStroke = 0.5;
    private Color residualColour;
    private double residualLinewidth = 0.5;
    private double[] residualLinetype;
    private Double residualAlpha;

    /** Creates a spec with the default polygon-rendering properties. */
    public Polygon3dSpec() {
    }

    /**
     * The default light for solid (bar/column/voxel) geometries: a diffuse
     * source from above-front at 0.4 contrast strength, with the default light
     * direction {@code (-0.5, 0, 1)}. HSL shading keeps even fills
     * at full RGB brightness (e.g. {@code #ff7f00}) shadeable — an hsv-space
     * brightening is capped at 100% on those fills and leaves the faces a
     * flat, unlit orange.
     */
    public static final Light3d SOLID_LIGHT = Light3d.of(Light3d.Method.DIFFUSE,
            Light3d.Mode.HSL, true, true, 0.4, new double[] {-0.5, 0, 1}, null,
            false, Light3d.Anchor.SCENE, -1, 0);

    /**
     * Creates the default spec for solid geometries: back-face culling plus the
     * {@link #SOLID_LIGHT default light} used only when neither the layer, the
     * coord nor the plot supplies an explicit light.
     *
     * @return a solid-geometry {@link Polygon3dSpec}
     */
    public static Polygon3dSpec solid() {
        return new Polygon3dSpec().cullBackfaces(true)
                .defaultLight(SOLID_LIGHT);
    }

    /** {@return the polygon fill colour} */
    public Color getFill() { return fill; }

    /**
     * {@return this} Sets the polygon fill colour.
     *
     * @param fill the JavaFX {@link Color}, or {@code null} to inherit
     */
    public Polygon3dSpec fill(Color fill) { this.fill = fill; return this; }

    /** {@return the polygon stroke colour} */
    public Color getColour() { return colour; }

    /**
     * {@return this} Sets the polygon stroke colour.
     *
     * @param colour the JavaFX {@link Color}, or {@code null} to skip the stroke
     */
    public Polygon3dSpec colour(Color colour) { this.colour = colour; return this; }

    /** {@return the polygon stroke width in pixels} */
    public double getLinewidth() { return linewidth; }

    /**
     * {@return this} Sets the polygon stroke width in pixels.
     *
     * @param linewidth the stroke width
     */
    public Polygon3dSpec linewidth(double linewidth) { this.linewidth = linewidth; return this; }

    /** {@return the polygon stroke dash pattern, or {@code null} for solid} */
    public double[] getLinetype() { return linetype == null ? null : linetype.clone(); }

    /**
     * {@return this} Sets the polygon stroke dash pattern.
     *
     * @param linetype alternating on/off lengths, or {@code null} for solid
     */
    public Polygon3dSpec linetype(double... linetype) { this.linetype = linetype; return this; }

    /** {@return the polygon opacity in [0, 1]} */
    public double getAlpha() { return alpha; }

    /**
     * {@return this} Sets the polygon opacity in [0, 1].
     *
     * @param alpha the opacity between 0.0 and 1.0
     */
    public Polygon3dSpec alpha(double alpha) { this.alpha = alpha; return this; }

    /** {@return the depth-ordering strategy} */
    public SortMethod getSortMethod() { return sortMethod; }

    /**
     * {@return this} Sets the depth-ordering strategy.
     *
     * @param sortMethod the {@link SortMethod}
     */
    public Polygon3dSpec sortMethod(SortMethod sortMethod) { this.sortMethod = sortMethod; return this; }

    /** {@return whether stroke widths scale with distance} */
    public boolean isScaleDepth() { return scaleDepth; }

    /**
     * {@return this} Sets whether stroke widths and point sizes scale with distance.
     *
     * @param scaleDepth {@code true} to scale by depth
     */
    public Polygon3dSpec scaleDepth(boolean scaleDepth) { this.scaleDepth = scaleDepth; return this; }

    /** {@return whether back faces are dropped} */
    public boolean isCullBackfaces() { return cullBackfaces; }

    /**
     * {@return this} Sets whether polygons whose projected winding is the wrong
     * way round (facing away from the viewer) are skipped. The front-facing
     * winding is established by projecting the bottom face of the cube, so the
     * tiles must use a consistent winding in the x–y plane.
     *
     * @param cullBackfaces {@code true} to cull back faces
     */
    public Polygon3dSpec cullBackfaces(boolean cullBackfaces) { this.cullBackfaces = cullBackfaces; return this; }

    /** {@return whether non-convex vertices are dropped} */
    public boolean isForceConvex() { return forceConvex; }

    /**
     * {@return this} Sets whether each polygon is replaced by its convex hull
     * before drawing.
     *
     * @param forceConvex {@code true} to draw convex hulls
     */
    public Polygon3dSpec forceConvex(boolean forceConvex) { this.forceConvex = forceConvex; return this; }

    /** {@return the light source, or {@code null} for flat rendering} */
    public Light3d getLight() { return light; }

    /**
     * {@return this} Sets the {@link Light3d} used to shade the polygons.
     *
     * @param light the light source, or {@code null} to disable shading
     */
    public Polygon3dSpec light(Light3d light) { this.light = light; return this; }

    /**
     * {@return the light to fall back on when neither the layer, the coord
     * nor the plot supplies an explicit light; {@code null} for flat
     * rendering}
     */
    public Light3d getDefaultLight() { return defaultLight; }

    /**
     * {@return this} Sets the {@link Light3d} this geometry falls back on
     * when no light is supplied at the layer, coord or plot level.
     *
     * @param light the fallback light source, or {@code null} for none
     */
    public Polygon3dSpec defaultLight(Light3d light) { this.defaultLight = light; return this; }

    // --- surface tessellation ---

    /** {@return the surface tessellation {@link SurfaceMethod}} */
    public SurfaceMethod getMethod() { return method; }

    /**
     * {@return this} Sets the surface tessellation method.
     *
     * @param method the {@link SurfaceMethod} ({@link SurfaceMethod#AUTO} by default)
     */
    public Polygon3dSpec method(SurfaceMethod method) { this.method = method; return this; }

    /** {@return the lattice {@link GridGeometry} for regular-grid tessellation} */
    public GridGeometry getGrid() { return grid; }

    /**
     * {@return this} Sets the grid geometry for regular-grid tessellation.
     *
     * @param grid the {@link GridGeometry} (default {@link GridGeometry#RECTANGLE})
     */
    public Polygon3dSpec grid(GridGeometry grid) { this.grid = grid; return this; }

    // --- ridgeline construction ---

    /** {@return the ridge {@link GridDirection}} */
    public GridDirection getDirection() { return direction; }

    /**
     * {@return this} Sets the ridge direction.
     *
     * @param direction the {@link GridDirection} ({@link GridDirection#X} by default)
     */
    public Polygon3dSpec direction(GridDirection direction) { this.direction = direction; return this; }

    /** {@return the ridge base z, or {@code null} for {@code min(z)}} */
    public Double getBase() { return base; }

    /**
     * {@return this} Sets the ridge base z value.
     *
     * @param base the base z, or {@code null} for the data minimum
     */
    public Polygon3dSpec base(Double base) { this.base = base; return this; }

    // --- contour construction ---

    /** {@return the contour bin count} */
    public int getBins() { return bins; }

    /**
     * {@return this} Sets the contour bin count.
     *
     * @param bins the number of bins (default 10)
     */
    public Polygon3dSpec bins(int bins) { this.bins = bins; return this; }

    /** {@return the contour bin width, or {@code null}} */
    public Double getBinwidth() { return binwidth; }

    /**
     * {@return this} Sets the contour bin width.
     *
     * @param binwidth the bin width, or {@code null}
     */
    public Polygon3dSpec binwidth(Double binwidth) { this.binwidth = binwidth; return this; }

    /** {@return the explicit contour break levels, or {@code null}} */
    public double[] getBreaks() { return breaks == null ? null : breaks.clone(); }

    /**
     * {@return this} Sets the explicit contour break levels.
     *
     * @param breaks the break levels, or {@code null}
     */
    public Polygon3dSpec breaks(double... breaks) { this.breaks = breaks; return this; }

    // --- smooth overlay styling ---

    /** {@return the smooth data-point colour, or {@code null} to inherit} */
    public Color getPointColour() { return pointColour; }

    /**
     * {@return this} Sets the colour of the smooth data-point overlay.
     *
     * @param colour the point colour, or {@code null} to inherit
     */
    public Polygon3dSpec pointColour(Color colour) { this.pointColour = colour; return this; }

    /** {@return the smooth data-point fill, or {@code null} to inherit} */
    public Color getPointFill() { return pointFill; }

    /**
     * {@return this} Sets the fill of the smooth data-point overlay.
     *
     * @param fill the point fill, or {@code null} to inherit
     */
    public Polygon3dSpec pointFill(Color fill) { this.pointFill = fill; return this; }

    /** {@return the smooth data-point size, or {@code null} to inherit} */
    public Double getPointSize() { return pointSize; }

    /**
     * {@return this} Sets the size of the smooth data-point overlay.
     *
     * @param size the point diameter, or {@code null} to inherit
     */
    public Polygon3dSpec pointSize(Double size) { this.pointSize = size; return this; }

    /** {@return the smooth data-point shape, or {@code null} to inherit} */
    public PointShape getPointShape() { return pointShape; }

    /**
     * {@return this} Sets the shape of the smooth data-point overlay.
     *
     * @param shape the point shape, or {@code null} to inherit
     */
    public Polygon3dSpec pointShape(PointShape shape) { this.pointShape = shape; return this; }

    /** {@return the smooth data-point opacity, or {@code null} to inherit} */
    public Double getPointAlpha() { return pointAlpha; }

    /**
     * {@return this} Sets the opacity of the smooth data-point overlay.
     *
     * @param alpha the point opacity, or {@code null} to inherit
     */
    public Polygon3dSpec pointAlpha(Double alpha) { this.pointAlpha = alpha; return this; }

    /** {@return the smooth data-point border stroke width} */
    public double getPointStroke() { return pointStroke; }

    /**
     * {@return this} Sets the border stroke width of the smooth data-point overlay.
     *
     * @param stroke the stroke width
     */
    public Polygon3dSpec pointStroke(double stroke) { this.pointStroke = stroke; return this; }

    /** {@return the smooth residual-segment colour, or {@code null} to inherit} */
    public Color getResidualColour() { return residualColour; }

    /**
     * {@return this} Sets the colour of the smooth residual segments.
     *
     * @param colour the segment colour, or {@code null} to inherit
     */
    public Polygon3dSpec residualColour(Color colour) { this.residualColour = colour; return this; }

    /** {@return the smooth residual-segment stroke width} */
    public double getResidualLinewidth() { return residualLinewidth; }

    /**
     * {@return this} Sets the stroke width of the smooth residual segments.
     *
     * @param linewidth the stroke width
     */
    public Polygon3dSpec residualLinewidth(double linewidth) { this.residualLinewidth = linewidth; return this; }

    /** {@return the smooth residual-segment dash pattern, or {@code null} for solid} */
    public double[] getResidualLinetype() { return residualLinetype == null ? null : residualLinetype.clone(); }

    /**
     * {@return this} Sets the dash pattern of the smooth residual segments.
     *
     * @param linetype alternating on/off lengths, or {@code null} for solid
     */
    public Polygon3dSpec residualLinetype(double... linetype) { this.residualLinetype = linetype; return this; }

    /** {@return the smooth residual-segment opacity, or {@code null} to inherit} */
    public Double getResidualAlpha() { return residualAlpha; }

    /**
     * {@return this} Sets the opacity of the smooth residual segments.
     *
     * @param alpha the segment opacity, or {@code null} to inherit
     */
    public Polygon3dSpec residualAlpha(Double alpha) { this.residualAlpha = alpha; return this; }
}
