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
package org.jtaccuino.gog.coord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.PolygonMath;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.theme.AxisStyle;
import org.jtaccuino.gog.theme.CubeStyle;
import org.jtaccuino.gog.theme.Theme;

/**
 * 3D coordinate system, the cube view model behind {@code coord3d()}.
 * <p>
 * Rotates the data cube by {@code pitch} (about the y axis), {@code roll}
 * (about the x axis) and {@code yaw} (about the z axis), applies optional
 * perspective projection at camera distance {@code dist}, and renders the six
 * cube faces classified as background / foreground. The projection pipeline
 * runs the standard 3-D transform: data is scaled to a standard
 * {@code [-0.5, 0.5]} domain per axis, possibly stretched by axis ratios,
 * rotated, perspective-divided, and fitted into the panel via projected
 * plot bounds.
 */
@SuppressWarnings("EnumOrdinal")
public class Coord3D implements Coord {

    /** A 3-component vector in cube or scale space. */
    private record Vector3(double x, double y, double z) {}

    /** Depth-scaling strengths for the cube furniture (grid, border, ticks, text). */
    private record ScaleDepth(double grid, double border, double ticks, double text) {}

    /** The projected extent of the cube, padded and zoomed, in projected units. */
    private record PlotBounds(double xMin, double xMax, double yMin, double yMax) {}

    private double pitch = 0;
    private double roll = -60;
    private double yaw = -30;
    private boolean persp = true;
    private double dist = 2;
    private double zoom = 1;
    private ScaleMode scales = ScaleMode.FREE;
    private Vector3 ratio = new Vector3(1, 1, 1);
    private boolean expand = true;
    private boolean clip = false;
    private CubePanel[] panels = {CubePanel.BACKGROUND};
    private CubeFace[] xlabels = {};
    private CubeFace[] ylabels = {};
    private CubeFace[] zlabels = {};
    private boolean rotateLabels = true;
    private ScaleDepth scaleDepth = new ScaleDepth(1.0, 1.0, 1.0, 1.0);
    private double[] labelReach = {0, 0, 0}; // perpendicular reach of the tick labels per axis
    private Light3d light;

    private double centerX, centerY;
    private double panelX, panelY, panelW, panelH, panelSide;
    private double canvasX, canvasY, canvasW, canvasH;

    private double minDataX, maxDataX, minDataY, maxDataY, minDataZ, maxDataZ;
    private List<Double> zBreaks;
    private List<String> zLabels;
    private boolean zIsCategorical;
    private List<String> zCategories;
    private boolean xIsCategorical;
    private List<String> xCategories;
    private boolean yIsCategorical;
    private List<String> yCategories;
    private String xLabel = "x", yLabel = "y", zLabel = "z";
    private AxisStyle axisStyle;
    private CubeStyle cubeStyle;
    private Theme theme;

    // --- projection state, rebuilt lazily whenever the view or data changes ---
    private Vector3 effectiveRatios;
    private PlotBounds plotBounds;
    private double m11, m12, m13, m21, m22, m23, m31, m32, m33;
    private BgFgFaces visibleFaces;
    private boolean projectionDirty = true;

    // --- axis-furniture continuity state ---
    // The edge chosen per axis persists across frames so that orbiting the cube
    // keeps the ticks/labels/titles on a consistent edge, switching only when
    // the chosen face is no longer drawn or another edge clearly wins.
    private final AxisSelection[] lastAxisSelection = new AxisSelection[3];

    // --- transient face-placement state for Positions.positionOnFace(...) layers ---
    // While a layer configured with Positions.positionOnFace(...) renders, this coordinate
    // system projects every mark onto the configured cube face instead of its
    // natural 3-D position: 3-D layers flatten onto the face, 2-D layers have
    // their x/y placed along the two in-face axes. The plot brackets the layer
    // render between beginFaceRender()/endFaceRender().
    private CubeFace renderFace;
    private int[] renderFaceAxes;

    private static final double[] EX = {-0.5, 0.5};

    /** Screen-space text boxes of the tick labels drawn in the current render
     * pass, so a label that would collide with any already-drawn label (its own
     * axis's neighbour, a neighbour axis's label at a cube corner, or a label
     * that wraps around the short edge of a small panel) is skipped. */
    private final List<double[][]> drawnLabelBoxes = new ArrayList<>();

    // Face corners (indices into the 8-corner cube bit table
    // (bx<<2)|(by<<1)|bz, rows ordered so the perimeter run is [0,2,3,1]).
    private static final int[][] FACE_CORNERS = {
        {0, 2, 1, 3}, // xmin: rows (y,z): (-,-),(+,-),(-,+),(+,+)
        {4, 6, 5, 7}, // xmax
        {0, 4, 1, 5}, // ymin: rows (x,z)
        {2, 6, 3, 7}, // ymax
        {0, 4, 2, 6}, // zmin: rows (x,y)
        {1, 5, 3, 7}  // zmax
    };

    /** Creates a 3D coordinate system with the default view: pitch 0, roll -60,
     * yaw -30 and camera distance 2. */
    public Coord3D() {
    }

    /** Sets the rotation about the y axis in degrees (default 0).
     * @param pitch the rotation angle in degrees
     * @return this coordinate system for fluid chaining */
    public Coord3D pitch(double pitch) { this.pitch = pitch; projectionDirty = true; return this; }

    /** The rotation about the y axis in degrees (default 0).
     * @return the pitch angle in degrees */
    public double pitch() { return pitch; }

    /** Sets the rotation about the x axis in degrees (default -60).
     * @param roll the rotation angle in degrees
     * @return this coordinate system for fluid chaining */
    public Coord3D roll(double roll) { this.roll = roll; projectionDirty = true; return this; }

    /** The rotation about the x axis in degrees (default -60).
     * @return the roll angle in degrees */
    public double roll() { return roll; }

    /** Sets the rotation about the z axis in degrees (default -30).
     * @param yaw the rotation angle in degrees
     * @return this coordinate system for fluid chaining */
    public Coord3D yaw(double yaw) { this.yaw = yaw; projectionDirty = true; return this; }

    /** The rotation about the z axis in degrees (default -30).
     * @return the yaw angle in degrees */
    public double yaw() { return yaw; }

    /** Enables (default) or disables perspective projection.
     * @param persp {@code true} for perspective, {@code false} for orthographic
     * @return this coordinate system for fluid chaining */
    public Coord3D persp(boolean persp) { this.persp = persp; projectionDirty = true; return this; }

    /** Sets the camera distance from the cube center (default 2, perspective only).
     * @param dist the camera distance
     * @return this coordinate system for fluid chaining */
    public Coord3D dist(double dist) { this.dist = dist; projectionDirty = true; return this; }

    /** Sets the global zoom factor (default 1, &gt; 1 zooms in).
     * @param zoom the zoom factor
     * @return this coordinate system for fluid chaining */
    public Coord3D zoom(double zoom) { this.zoom = zoom; projectionDirty = true; return this; }

    /** Sets the aspect scaling behaviour, {@link ScaleMode#FREE} (default) or
     * {@link ScaleMode#FIXED}.
     * @param scales the aspect behaviour
     * @return this coordinate system for fluid chaining */
    public Coord3D scales(ScaleMode scales) {
        if (scales == null) {
            throw new IllegalArgumentException("scales must not be null");
        }
        this.scales = scales;
        projectionDirty = true;
        return this;
    }

    /** Sets per-axis scaling ratios for non-uniform data ranges (default 1,1,1).
     * @param x the scaling ratio applied along the x-axis
     * @param y the scaling ratio applied along the y-axis
     * @param z the scaling ratio applied along the z-axis
     * @return this coordinate system for fluid chaining */
    public Coord3D ratio(double x, double y, double z) {
        this.ratio = new Vector3(x, y, z);
        projectionDirty = true;
        return this;
    }

    /** Controls whether axis ranges are expanded beyond the data (default {@code true}).
     * @param expand {@code false} disables the default expansion
     * @return this coordinate system for fluid chaining */
    public Coord3D expand(boolean expand) { this.expand = expand; return this; }

    /** Controls whether layer content may draw outside the panel (default off).
     * @param clip {@code true} clips to the panel, {@code false} (default) allows overflow
     * @return this coordinate system for fluid chaining */
    public Coord3D clip(boolean clip) { this.clip = clip; return this; }

    /** Selects which cube faces to render: {@link CubePanel#BACKGROUND} (default),
     * {@link CubePanel#FOREGROUND}, {@link CubePanel#ALL},
     * {@link CubePanel#NONE}, position rules
     * ({@link CubePanel#NEAR}/{@link CubePanel#FAR}/{@link CubePanel#LEFT}/
     * {@link CubePanel#RIGHT}/{@link CubePanel#TOP}/{@link CubePanel#BOTTOM}/
     * {@link CubePanel#FRONT}/{@link CubePanel#BACK}), or explicit face names.
     * @param panels the faces to draw; empty uses the {@link CubePanel#BACKGROUND} default
     * @return this coordinate system for fluid chaining */
    public Coord3D panels(CubePanel... panels) {
        this.panels = panels == null || panels.length == 0
                ? new CubePanel[]{CubePanel.BACKGROUND}
                : panels;
        projectionDirty = true;
        return this;
    }

    /** Sets the placement of the x-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces whose
     * intersection is the edge to label along.
     * @param xlabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this coordinate system for fluid chaining */
    public Coord3D xlabels(CubeFace... xlabels) { this.xlabels = labelSpec(xlabels); projectionDirty = true; return this; }

    /** Sets the placement of the y-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces whose
     * intersection is the edge to label along.
     * @param ylabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this coordinate system for fluid chaining */
    public Coord3D ylabels(CubeFace... ylabels) { this.ylabels = labelSpec(ylabels); projectionDirty = true; return this; }

    /** Sets the placement of the z-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces whose
     * intersection is the edge to label along.
     * @param zlabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this coordinate system for fluid chaining */
    public Coord3D zlabels(CubeFace... zlabels) { this.zlabels = labelSpec(zlabels); projectionDirty = true; return this; }

    /** Sets whether axis labels rotate to align with their projected edge (default {@code true}).
     * @param rotateLabels {@code true} to auto-rotate labels
     * @return this coordinate system for fluid chaining */
    public Coord3D rotateLabels(boolean rotateLabels) { this.rotateLabels = rotateLabels; return this; }

    /** Sets the light source used to shade the 3D layer polygons.
     * <p>
     * The coord-level light combines with the plot-level
     * {@code Plot.light(Light3d)} — both may not be set at once — and is
     * overridden by any layer-level {@code light(...)} configured on a
     * {@code Geoms.polygon3d()}/{@code Geoms.surface3d()} geometry. A {@code null} light
     * leaves the layer and plot settings (or the flat default) in charge.
     *
     * @param light the {@link Light3d}, or {@code null} for none
     * @return this coordinate system for fluid chaining */
    public Coord3D light(Light3d light) { this.light = light; return this; }

    /** {@return the coord-level light, or {@code null} when unset} */
    public Light3d getLight() { return light; }

    /** Sets the depth-scaling strengths for grid, border, ticks, and text
     * (default {@code 1} for all). A strength of 0 disables the perspective
     * size cue; axis titles are never depth-scaled.
     * @param grid the grid strength
     * @param border the border strength
     * @param ticks the ticks strength
     * @param text the text strength
     * @return this coordinate system for fluid chaining */
    public Coord3D scaleDepth(double grid, double border, double ticks, double text) {
        if (grid < 0 || border < 0 || ticks < 0 || text < 0) {
            throw new IllegalArgumentException("scaleDepth values must be non-negative");
        }
        this.scaleDepth = new ScaleDepth(grid, border, ticks, text);
        return this;
    }

    /** {@return whether this coord keeps the default panel expansion for its scales} */
    public boolean expand() { return expand; }

    /** {@return whether layers are clipped to the panel (clipping is off by default)} */
    public boolean clips() { return clip; }

    private static CubeFace[] labelSpec(CubeFace... faces) {
        if (faces != null && faces.length > 0 && faces.length != 2) {
            throw new IllegalArgumentException("labels must be empty (auto) or a pair of adjacent faces");
        }
        return faces == null ? new CubeFace[0] : faces;
    }

    /** Sets the label rendered along the x-axis.
     * @param xLabel the label for the x-axis
     * @return this coordinate system for fluid chaining */
    public Coord3D xLabel(String xLabel) { this.xLabel = xLabel; return this; }

    /** Sets the label rendered along the y-axis.
     * @param yLabel the label for the y-axis
     * @return this coordinate system for fluid chaining */
    public Coord3D yLabel(String yLabel) { this.yLabel = yLabel; return this; }

    /** Sets the label rendered along the z-axis.
     * @param zLabel the label for the z-axis
     * @return this coordinate system for fluid chaining */
    public Coord3D zLabel(String zLabel) { this.zLabel = zLabel; return this; }

    /** Sets the computed data bounds for all three axes.
     * @param minX the minimum data value of the x-axis
     * @param maxX the maximum data value of the x-axis
     * @param minY the minimum data value of the y-axis
     * @param maxY the maximum data value of the y-axis
     * @param minZ the minimum data value of the z-axis
     * @param maxZ the maximum data value of the z-axis */
    public void setDataBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this.minDataX = minX; this.maxDataX = maxX;
        this.minDataY = minY; this.maxDataY = maxY;
        this.minDataZ = minZ; this.maxDataZ = maxZ;
        if (zIsCategorical) {
            zBoundsFromCategories();
        }
        projectionDirty = true;
    }

    /** Sets custom tick positions for the z-axis.
     * @param breaks the break values in data space, or {@code null} to derive
     *        them from the data
     * @return this coordinate system for fluid chaining */
    public Coord3D zBreaks(List<Double> breaks) {
        this.zBreaks = breaks == null ? null : List.copyOf(breaks);
        projectionDirty = true;
        return this;
    }

    /** Sets custom labels drawn at the {@link #zBreaks(List)} positions, paired
     * positionally with them.
     * @param labels the label for each break, in the same order
     * @return this coordinate system for fluid chaining */
    public Coord3D zLabels(List<String> labels) {
        this.zLabels = labels == null ? null : List.copyOf(labels);
        return this;
    }

    /** Configures the z-axis for categorical (rather than continuous) mapping.
     * @param categories the category values, sorted and stored as labels */
    public void setZCategories(List<?> categories) {
        this.zIsCategorical = true;
        this.zCategories = new ArrayList<>();
        for (var c : categories) {
            if (c != null) zCategories.add(c.toString());
        }
        zCategories.sort(String::compareTo);
        zBoundsFromCategories();
        projectionDirty = true;
    }

    /**
     * Puts a categorical z domain on the half-band grid [-0.5, N−0.5], the same
     * half-band padding a 2-D discrete axis carries. Category index {@code i}
     * then normalises to the band-centre cube fraction {@code (i + 0.5) / N} in
     * {@link #projectDataRaw} — exactly where the axis tick is drawn — instead
     * of overflowing past the cube faces when the raw column bounds span
     * nothing sensible (e.g. {-0.5, N−0.5} after a string column's min/max).
     */
    private void zBoundsFromCategories() {
        this.minDataZ = -0.5;
        this.maxDataZ = zCategories.isEmpty() ? 0.5 : zCategories.size() - 0.5;
    }

    /** {@return whether the z-axis is configured as categorical} */
    public boolean isZCategorical() { return zIsCategorical; }

    /** {@return the sorted category labels for a categorical z-axis} */
    public List<String> getZCategories() { return zCategories; }

    /**
     * Converts a raw z value to its data-space position: the sorted category
     * index for a categorical z-axis (falling back to {@code 0} for unknown
     * categories) or the numeric value itself otherwise.
     *
     * @param raw the raw z value from the data frame
     * @return the z value in data space
     */
    public double toZValue(Object raw) {
        if (zIsCategorical) {
            var idx = zCategories.indexOf(raw.toString());
            return idx < 0 ? 0 : idx;
        }
        return Values.toDouble(raw);
    }

    /** Configures the x-axis for categorical (rather than continuous) mapping.
     * The categories keep the order they arrive in — the discrete-domain band
     * order established by {@code Scale.createDiscrete()} — and are stored as
     * labels.
     * @param categories the category values in band order */
    public void setXCategories(List<?> categories) {
        this.xIsCategorical = true;
        this.xCategories = new ArrayList<>();
        for (var c : categories) {
            if (c != null) xCategories.add(c.toString());
        }
        projectionDirty = true;
    }

    /** Configures the y-axis for categorical (rather than continuous) mapping.
     * @param categories the category values in band order */
    public void setYCategories(List<?> categories) {
        this.yIsCategorical = true;
        this.yCategories = new ArrayList<>();
        for (var c : categories) {
            if (c != null) yCategories.add(c.toString());
        }
        projectionDirty = true;
    }

    /**
     * Sets the full drawing area (the plot or composed-cell canvas) the cube's
     * axis furniture may extend into, wider than the cube panel. Axis titles
     * and labels are clamped to this area rather than the panel, so they can
     * sit in the plot margins where the cube is small.
     *
     * @param canvasX the left edge of the canvas, in pixels
     * @param canvasY the top edge of the canvas, in pixels
     * @param canvasW the canvas width, in pixels
     * @param canvasH the canvas height, in pixels
     */
    public void setCanvasBounds(double canvasX, double canvasY, double canvasW, double canvasH) {
        this.canvasX = canvasX;
        this.canvasY = canvasY;
        this.canvasW = canvasW;
        this.canvasH = canvasH;
    }

    /**
     * Sets the pixel bounds of the panel within which the projected cube is
     * fitted (the cube keeps its natural square aspect).
     *
     * @param panelX the left edge of the panel, in pixels
     * @param panelY the top edge of the panel, in pixels
     * @param panelW the width of the panel, in pixels
     * @param panelH the height of the panel, in pixels
     */
    public void setPanelBounds(double panelX, double panelY, double panelW, double panelH) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.panelH = panelH;
        this.panelSide = Math.min(panelW, panelH);
        centerX = panelX + panelW / 2.0;
        centerY = panelY + panelH / 2.0;
        if (canvasW <= 0 || canvasH <= 0) {
            canvasX = panelX;
            canvasY = panelY;
            canvasW = panelW;
            canvasH = panelH;
        }
        projectionDirty = true;
    }

    /**
     * Prepares the coordinate system with the theme used to style its axes and
     * cube, resolving the drawing constants once. Subsequent calls with the
     * same theme are no-ops; passing a different theme rebuilds the styles.
     *
     * @param t the active theme
     */
    public void prepare(Theme t) {
        if (Objects.equals(theme, t)) {
            return;
        }
        theme = t;
        axisStyle = null;
        cubeStyle = null;
    }

    /** {@return the resolved axis style, derived lazily from {@link #theme}} */
    private AxisStyle axisStyle() {
        if (axisStyle == null) {
            axisStyle = AxisStyle.from(theme);
        }
        return axisStyle;
    }

    /** {@return the resolved cube style, derived lazily from {@link #theme}} */
    private CubeStyle cubeStyle() {
        if (cubeStyle == null) {
            cubeStyle = CubeStyle.from(theme);
        }
        return cubeStyle;
    }

    /**
     * {@return the axis style of the z-axis}, which uses the dedicated
     * z-axis tick, text, and title element
     * chain, falling back to the shared axis style for themes without the
     * optional 3D elements.
     */
    private AxisStyle zAxisStyle() {
        var zAxis = cubeStyle().zAxis();
        return zAxis != null ? zAxis : axisStyle();
    }

    /** The projected 2D screen coordinates, the view depth (for sorting), and
     * the depth scaling factor for size/line art (perspective only).
     * @param sx the horizontal screen coordinate
     * @param sy the vertical screen coordinate
     * @param depth the depth value for z-sorting
     * @param depthScale the size scaling factor
     */
    public record ProjResult(double sx, double sy, double depth, double depthScale) {}

    /**
     * Projects normalized coordinates [0,1] to 2D screen space using the
     * configured rotation, projection, and camera distance.
     *
     * @param xNorm x in [0,1]
     * @param yNorm y in [0,1]
     * @param zNorm z in [0,1]
     * @return the projected screen position, depth, and depth scaling
     */
    public ProjResult project(double xNorm, double yNorm, double zNorm) {
        return toPixels(rotatePerspective(xNorm - 0.5, yNorm - 0.5, zNorm - 0.5));
    }

    /**
     * Projects data-space coordinates to screen space by first normalising
     * using the data bounds (or category positions for a categorical z-axis).
     * While a {@code Positions.positionOnFace(...)} layer is rendering (see
     * {@link #beginFaceRender}), the point is instead projected orthogonally
     * onto the configured cube face.
     *
     * @param x the x data value
     * @param y the y data value
     * @param z the z data value
     * @return the projected screen position, depth, and depth scaling
     */
    public ProjResult projectData(double x, double y, double z) {
        CubeFace face = renderFace;
        if (face != null) {
            return switch (faceAxis(face)) {
                case 0 -> projectDataRaw(faceCoordinate(face), y, z);
                case 1 -> projectDataRaw(x, faceCoordinate(face), z);
                default -> projectDataRaw(x, y, faceCoordinate(face));
            };
        }
        return projectDataRaw(x, y, z);
    }

    /**
     * The raw projection of a data-space point, ignoring any transient face
     * placement — the shared core of {@link #projectData} and
     * {@link #projectToFace}.
     */
    private ProjResult projectDataRaw(double x, double y, double z) {
        double nx = (x - minDataX) / span(maxDataX - minDataX) - 0.5;
        double ny = (y - minDataY) / span(maxDataY - minDataY) - 0.5;
        double nz = (z - minDataZ) / span(maxDataZ - minDataZ) - 0.5;
        return project(nx + 0.5, ny + 0.5, nz + 0.5);
    }

    /**
     * Begins rendering a {@code Positions.positionOnFace(...)} layer: while the transient
     * face placement is active, {@link #projectData} flattens 3-D points onto
     * {@code face} and {@link #xPixel}/{@link #yPixel} place 2-D points along
     * the two in-face axes (when {@code axes} names them). Always balance with a
     * matching {@link #endFaceRender} call.
     *
     * @param face the cube face the layer is drawn onto
     * @param axes the two in-face 3-D dimensions ({@code "x"}, {@code "y"},
     *             {@code "z"}) a 2-D layer's x and y map to, or {@code null} to
     *             flatten a native 3-D layer
     */
    public void beginFaceRender(CubeFace face, List<String> axes) {
        int[] faceAxes = null;
        if (axes != null) {
            if (axes.size() != 2) {
                throw new IllegalArgumentException("positionOnFace needs exactly two in-face axes, got "
                        + String.join(",", axes));
            }
            faceAxes = new int[]{axisIndex(axes.get(0)), axisIndex(axes.get(1))};
            int faceAxis = faceAxis(face);
            for (int a : faceAxes) {
                if (a == faceAxis || a == -1) {
                    throw new IllegalArgumentException("positionOnFace axes " + String.join(",", axes)
                            + " must name two distinct in-face dimensions of the " + face.name() + " face");
                }
            }
        }
        this.renderFace = face;
        this.renderFaceAxes = faceAxes;
    }

    /** Ends a transient face placement begun with {@link #beginFaceRender}. */
    public void endFaceRender() {
        this.renderFace = null;
        this.renderFaceAxes = null;
    }

    /**
     * Whether a {@code Positions.positionOnFace(...)} 2-D layer is currently rendering — the
     * face placement is active with its two in-face axes resolved. Geometries
     * use this to draw their marks on the face plane (e.g. tiles as projected
     * quads) instead of as flat panel shapes.
     *
     * @return {@code true} while a 2-D face layer renders
     */
    public boolean isFaceRendering() {
        return renderFace != null && renderFaceAxes != null;
    }

    /**
     * The projected pixel polygon of a cube face, in the same perimeter order
     * the face is filled with — the four corners run {@code [0, 2, 3, 1]} of
     * the face's corner table. The plot clips {@code Positions.positionOnFace(...)} layers
     * to this polygon so their marks never spill past the face plane.
     *
     * @param face the cube face
     * @return the {@code [x0, y0, x1, y1, x2, y2, x3, y3]} screen polygon
     */
    public double[] facePolygon(CubeFace face) {
        buildProjection();
        int faceOrd = face.ordinal();
        double[] out = new double[8];
        int[] perim = {0, 2, 3, 1};
        int k = 0;
        for (int pi : perim) {
            int idx = FACE_CORNERS[faceOrd][pi];
            int bx = (idx >> 2) & 1, by = (idx >> 1) & 1, bz = idx & 1;
            var c = toPixels(rotatePerspective(EX[bx], EX[by], EX[bz]));
            out[k++] = c.sx;
            out[k++] = c.sy;
        }
        return out;
    }

    /** {@return the axis index (0=x, 1=y, 2=z) of a dimension name}, or -1 when unknown. */
    private static int axisIndex(String axis) {
        return switch (axis) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> -1;
        };
    }

    /**
     * Maps a 2-D layer's {@code x}/{@code y} data pair onto the transient face
     * plane: the values are placed along the two in-face axes and the face axis
     * coordinate is fixed. Uses {@link #projectDataRaw} so a simultaneous
     * flattening never recurses. A face render without the two in-face axes
     * (a flattened 3-D layer) projects the pair as-is rather than placing it.
     */
    private ProjResult projectFaceData(double xData, double yData) {
        int faceAxis = faceAxis(renderFace);
        double[] p = new double[3];
        if (renderFaceAxes == null) {
            p[faceAxis] = faceCoordinate(renderFace);
            p[(faceAxis + 1) % 3] = xData;
            p[(faceAxis + 2) % 3] = yData;
        } else {
            int a0 = renderFaceAxes[0];
            int a1 = renderFaceAxes[1];
            p[a0] = xData;
            p[a1] = yData;
            p[faceAxis] = faceCoordinate(renderFace);
        }
        return projectDataRaw(p[0], p[1], p[2]);
    }

    /**
     * Maps a data point to its horizontal pixel position. While a
     * {@code Positions.positionOnFace(...)} layer with two in-face axes renders, the face
     * plane point is projected through the 3-D camera instead of the flat
     * panel scale; otherwise the plain Cartesian mapping (axis-aligned with
     * {@link #isFlipped()}) applies.
     *
     * @param sx     the scale of the horizontal axis
     * @param sy     the scale of the vertical axis
     * @param xData  the position of the x aesthetic in data space
     * @param yData  the position of the y aesthetic in data space
     * @return the horizontal pixel coordinate
     */
    @Override
    public double xPixel(Scale sx, Scale sy, double xData, double yData) {
        if (renderFace != null) {
            return projectFaceData(xData, yData).sx();
        }
        return isFlipped() ? sx.toPixel(yData) : sx.toPixel(xData);
    }

    /**
     * Maps a data point to its vertical pixel position — the
     * {@link #xPixel(Scale, Scale, double, double)} counterpart.
     *
     * @param sx     the scale of the horizontal axis
     * @param sy     the scale of the vertical axis
     * @param xData  the position of the x aesthetic in data space
     * @param yData  the position of the y aesthetic in data space
     * @return the vertical pixel coordinate
     */
    @Override
    public double yPixel(Scale sx, Scale sy, double xData, double yData) {
        if (renderFace != null) {
            return projectFaceData(xData, yData).sy();
        }
        return isFlipped() ? sy.toPixel(xData) : sy.toPixel(yData);
    }

    /**
     * {@return the data span along each axis} guarded against zero/NaN spans.
     * Layers generating reference elements sized in data units (for example
     * the circular reference points of {@code Geoms.point3d()}) use these to
     * convert a standardized radius fraction back into per-axis data units.
     */
    public double dataSpanX() { return span(maxDataX - minDataX); }
    /** {@return the data span along the y axis, guarded against zero/NaN} */
    public double dataSpanY() { return span(maxDataY - minDataY); }
    /** {@return the data span along the z axis, guarded against zero/NaN} */
    public double dataSpanZ() { return span(maxDataZ - minDataZ); }

    /**
     * {@return the axis (0=x, 1=y, 2=z) a cube face is perpendicular to}.
     *
     * @param face the cube face
     */
    public static int faceAxis(CubeFace face) {
        return face.ordinal() / 2;
    }

    /**
     * {@return the fixed data coordinate of a cube face} — the minimum data
     * value along the face's axis for the {@code min} faces, the maximum for
     * the {@code max} faces. Used to place points orthogonally projected onto
     * a face.
     *
     * @param face the cube face
     */
    public double faceCoordinate(CubeFace face) {
        int f = face.ordinal();
        boolean side = f % 2 == 1;
        return switch (f / 2) {
            case 0 -> side ? maxDataX : minDataX;
            case 1 -> side ? maxDataY : minDataY;
            default -> side ? maxDataZ : minDataZ;
        };
    }

    /**
     * Projects a data point orthogonally onto a cube face — the coordinate
     * along the face's axis is replaced by the face side, the other two kept.
     * This is the reference-element projection rule used for
     * {@code Positions.positionOnFace(...)}: a point's shadow on the
     * {@link CubeFace#ZMIN} face keeps its x and y and sits at the bottom of
     * the cube.
     *
     * @param x    the x data value
     * @param y    the y data value
     * @param z    the z data value
     * @param face the cube face
     * @return the projected screen position
     */
    public ProjResult projectToFace(double x, double y, double z, CubeFace face) {
        int axis = faceAxis(face);
        double fixed = faceCoordinate(face);
        return switch (axis) {
            case 0 -> projectData(fixed, y, z);
            case 1 -> projectData(x, fixed, z);
            default -> projectData(x, y, fixed);
        };
    }

    // --- projection pipeline ---

    /**
     * Rotates a scene-space unit normal into camera space: the z-flip and
     * pitch/roll/yaw rotation applied to points, without the perspective or
     * ratio rescaling. This is how the cube grounds
     * camera-anchored lights ({@link Light3d} with
     * {@code Light3d.Anchor#CAMERA}), which hold their direction on the view
     * plane instead of in the data cube.
     *
     * @param n the scene-space unit normal
     * @return the camera-space unit normal
     */
    public double[] rotateNormalToCamera(double[] n) {
        buildProjection();
        double cz = -n[2];
        return new double[] {
            m11 * n[0] + m21 * n[1] + m31 * cz,
            m12 * n[0] + m22 * n[1] + m32 * cz,
            m13 * n[0] + m23 * n[1] + m33 * cz
        };
    }

    /** A point projected into the cube's own space (before fitting to pixels). */
    private record ProjectedPoint(double x, double y, double z, double depth, double depthScale) {}

    /** The standard 3-D transform pipeline, without npc fitting.
     * Inputs are standard-domain coordinates in [{-0.5},{0.5}].
     * <p>
     * The rotation is applied as a row vector times the matrix, which is the
     * transpose of a column-vector multiplication. Since the rotation matrix
     * is orthonormal, applying it the wrong way round yields the inverse
     * rotation and a mirrored view.
     */
    private ProjectedPoint rotatePerspective(double nx, double ny, double nz) {
        double cx = nx * effectiveRatios.x();
        double cy = ny * effectiveRatios.y();
        double cz = -nz * effectiveRatios.z();
        double rx = m11 * cx + m21 * cy + m31 * cz;
        double ry = m12 * cx + m22 * cy + m32 * cz;
        double rz = m13 * cx + m23 * cy + m33 * cz;
        if (persp) {
            double zDepth = rz + dist;
            double factor = dist / zDepth;
            double depth = Math.sqrt(rx * rx + ry * ry + zDepth * zDepth);
            return new ProjectedPoint(rx * factor, ry * factor, rz, depth, dist / zDepth);
        }
        return new ProjectedPoint(rx, ry, rz, rz, 1.0);
    }

    private ProjResult toPixels(ProjectedPoint p) {
        buildProjection();
        double nxNpc = (p.x - plotBounds.xMin()) / (plotBounds.xMax() - plotBounds.xMin());
        double nyNpc = (p.y - plotBounds.yMin()) / (plotBounds.yMax() - plotBounds.yMin());
        double offX = panelX + (panelW - panelSide) / 2.0;
        double offY = panelY + (panelH - panelSide) / 2.0;
        double px = offX + nxNpc * panelSide;
        double py = offY + (1 - nyNpc) * panelSide;
        return new ProjResult(px, py, p.depth, p.depthScale);
    }

    /** Recomputes the effective ratios, rotation matrix, and plot bounds when
     * the view or data has changed. */
    private void buildProjection() {
        if (!projectionDirty) return;
        computeEffectiveRatios();
        double p = Math.toRadians(pitch);
        double r = Math.toRadians(roll);
        double y = Math.toRadians(yaw);
        double cp = Math.cos(p), sp = Math.sin(p);
        double cr = Math.cos(r), sr = Math.sin(r);
        double cy = Math.cos(y), sy = Math.sin(y);
        m11 = cy * cp;
        m12 = cy * sp * sr - sy * cr;
        m13 = cy * sp * cr + sy * sr;
        m21 = sy * cp;
        m22 = sy * sp * sr + cy * cr;
        m23 = sy * sp * cr - cy * sr;
        m31 = -sp;
        m32 = cp * sr;
        m33 = cp * cr;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double[] projX = new double[8], projY = new double[8], projZ = new double[8];
        for (int bx = 0; bx < 2; bx++) {
            for (int by = 0; by < 2; by++) {
                for (int bz = 0; bz < 2; bz++) {
                    int i = (bx << 2) | (by << 1) | bz;
                    var pj = rotatePerspective(EX[bx], EX[by], EX[bz]);
                    projX[i] = pj.x; projY[i] = pj.y; projZ[i] = pj.z;
                    minX = Math.min(minX, pj.x); maxX = Math.max(maxX, pj.x);
                    minY = Math.min(minY, pj.y); maxY = Math.max(maxY, pj.y);
                }
            }
        }
        // Fit the projected bounding box of the transformed cube into the
        // panel, centring that box rather than the projected origin. The
        // bounds are measured after the perspective division and their
        // centre is mapped to the panel centre; anchoring on the origin
        // instead leaves a perspective-foreshortened view sunk toward one side
        // of its panel -- a wide empty band on the other side -- because near
        // corners project further than far ones. Padding each side by 5% of
        // the projected span matches the cube plotting bounds. The centre of
        // the box also stays the rotation pivot, so the cube appears to turn
        // in place.
        double hx = (maxX - minX) / 2.0 * 1.1;
        double hy = (maxY - minY) / 2.0 * 1.1;
        double boxCx = (minX + maxX) / 2.0;
        double boxCy = (minY + maxY) / 2.0;
        if (zoom != 1) {
            hx /= zoom;
            hy /= zoom;
        }
        plotBounds = new PlotBounds(boxCx - hx, boxCx + hx, boxCy - hy, boxCy + hy);
        visibleFaces = classifyFaces(projX, projY, projZ);
        projectionDirty = false;
    }

    private void computeEffectiveRatios() {
        if (scales == ScaleMode.FREE) {
            effectiveRatios = ratio;
        } else {
            double xSpan = span(maxDataX - minDataX);
            double ySpan = span(maxDataY - minDataY);
            double zSpan = span(maxDataZ - minDataZ);
            double ex = ratio.x() * xSpan, ey = ratio.y() * ySpan, ez = ratio.z() * zSpan;
            double max = Math.max(ex, Math.max(ey, ez));
            effectiveRatios = new Vector3(ex / max, ey / max, ez / max);
        }
    }

    private static double span(double v) {
        return v == 0 || Double.isNaN(v) ? 1 : v;
    }

    /** Faces classified as background / foreground for the current view. */
    @SuppressWarnings("ArrayRecordComponent")
    private record BgFgFaces(CubeFace[] bg, CubeFace[] fg) {}

    /** Classifies the six faces of the cube by their projected hull membership
     * and depth: a face is foreground when it is on the silhouette that faces
     * the viewer. */
    private BgFgFaces classifyFaces(double[] projX, double[] projY, double[] projZ) {
        var hull = convexHull(projX, projY);
        boolean[] onHull = new boolean[8];
        for (int i : hull) onHull[i] = true;

        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (double z : projZ) {
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        double[] zMean = new double[6], zMin = new double[6];
        double[] xMean = new double[6], yMean = new double[6];
        int[] hullCount = new int[6];
        boolean[] near = new boolean[6], far = new boolean[6];
        for (int f = 0; f < 6; f++) {
            double sx = 0, sy = 0, sz = 0;
            double zm = Double.POSITIVE_INFINITY;
            for (int idx : FACE_CORNERS[f]) {
                sx += projX[idx]; sy += projY[idx]; sz += projZ[idx];
                zm = Math.min(zm, projZ[idx]);
                if (onHull[idx]) hullCount[f]++;
                if (projZ[idx] == maxZ) near[f] = true;
                if (projZ[idx] == minZ) far[f] = true;
            }
            xMean[f] = sx / 4; yMean[f] = sy / 4; zMean[f] = sz / 4; zMin[f] = zm;
        }

        String view;
        if (hull.size() == 4) {
            view = "face";
        } else if (distinct(hullCount) == 1) {
            view = "corner";
        } else {
            view = "edge";
        }

        boolean[] foreground = new boolean[6];
        double minZMean = minOf(zMean), minZMin = minOf(zMin);
        for (int f = 0; f < 6; f++) {
            if ("face".equals(view)) {
                foreground[f] = zMean[f] == minZMean;
            } else if ("edge".equals(view)) {
                foreground[f] = hullCount[f] == 4;
            } else {
                foreground[f] = zMin[f] == minZMin;
            }
        }

        List<CubeFace> bg = new ArrayList<>();
        List<CubeFace> fg = new ArrayList<>();
        var selected = resolvePanels(xMean, yMean, zMean, near, far, foreground);
        for (int f = 0; f < 6; f++) {
            if (!selected[f]) continue;
            if (foreground[f]) fg.add(CubeFace.values()[f]); else bg.add(CubeFace.values()[f]);
        }
        return new BgFgFaces(bg.toArray(CubeFace[]::new), fg.toArray(CubeFace[]::new));
    }

    private static int distinct(int[] values) {
        Set<Integer> seen = new HashSet<>();
        for (int v : values) seen.add(v);
        return seen.size();
    }

    private static double minOf(double[] v) {
        double m = Double.POSITIVE_INFINITY;
        for (double d : v) m = Math.min(m, d);
        return m;
    }

    private static double maxOf(double[] v) {
        double m = Double.NEGATIVE_INFINITY;
        for (double d : v) m = Math.max(m, d);
        return m;
    }

    private static int indexOf(double[] v, double target) {
        for (int i = 0; i < v.length; i++) if (v[i] == target) return i;
        return 0;
    }

    /** Expands the user {@code panels} spec into a per-face visibility mask
     * from the per-face near/far and foreground classification. */
    private boolean[] resolvePanels(double[] xMean, double[] yMean, double[] zMean,
            boolean[] near, boolean[] far, boolean[] foreground) {
        boolean[] mask = new boolean[6];
        boolean all = false, none = false;
        for (CubePanel p : panels) {
            switch (p) {
                case BACKGROUND -> { for (int f = 0; f < 6; f++) if (!foreground[f]) mask[f] = true; }
                case FOREGROUND -> { for (int f = 0; f < 6; f++) if (foreground[f]) mask[f] = true; }
                case ALL -> all = true;
                case NONE -> none = true;
                case NEAR -> { for (int f = 0; f < 6; f++) if (near[f]) mask[f] = true; }
                case FAR -> { for (int f = 0; f < 6; f++) if (far[f]) mask[f] = true; }
                case FRONT -> { mask[indexOf(zMean, maxOf(zMean))] = true; }
                case BACK -> { mask[indexOf(zMean, minOf(zMean))] = true; }
                case LEFT -> { mask[indexOf(xMean, minOf(xMean))] = true; }
                case RIGHT -> { mask[indexOf(xMean, maxOf(xMean))] = true; }
                case TOP -> { mask[indexOf(yMean, maxOf(yMean))] = true; }
                case BOTTOM -> { mask[indexOf(yMean, minOf(yMean))] = true; }
                default -> {
                    CubeFace face = p.asFace();
                    if (face != null) mask[face.ordinal()] = true;
                }
            }
        }
        if (all) Arrays.fill(mask, true);
        if (none) Arrays.fill(mask, false);
        return mask;
    }

    /** Andrew's monotone-chain convex hull over the projected corners. */
    private static List<Integer> convexHull(double[] x, double[] y) {
        Integer[] order = new Integer[8];
        for (int i = 0; i < 8; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> x[a] != x[b] ? Double.compare(x[a], x[b]) : Double.compare(y[a], y[b]));
        int[] hull = new int[16];
        int n = 0;
        for (Integer o : order) {
            while (n >= 2 && cross(x, y, hull[n - 2], hull[n - 1], o) <= 0) n--;
            hull[n++] = o;
        }
        int lowerSize = n;
        for (int k = order.length - 1; k >= 0; k--) {
            int o = order[k];
            while (n > lowerSize && cross(x, y, hull[n - 2], hull[n - 1], o) <= 0) n--;
            hull[n++] = o;
        }
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) result.add(hull[i]);
        return result;
    }

    private static double cross(double[] x, double[] y, int a, int b, int c) {
        return (x[b] - x[a]) * (y[c] - y[a]) - (y[b] - y[a]) * (x[c] - x[a]);
    }

    // --- cube rendering ---

    /** Renders the cube background (faces, grid, border, axis furniture) before
     * the data layers.
     * @param gc the surface to draw onto */
    public void renderBackground(DrawSurface gc) {
        buildProjection();
        for (CubeFace face : visibleFaces.bg) {
            drawFacePolygon(gc, face, false);
        }
        drawGrid(gc, visibleFaces.bg, cubeStyle().grid(), scaleDepth.grid());
        for (CubeFace face : visibleFaces.bg) {
            drawBorder(gc, face, scaleDepth.border());
        }
        renderTicksAndLabels(gc);
        gc.setTextBaseline(VPos.BASELINE);
    }

    /** Renders the cube foreground faces and grid over the data layers, then the
     * axis titles on top of everything so a title that could not fit outside the
     * panel (and was placed inward over the cube) stays readable.
     * @param gc the surface to draw onto */
    public void renderForeground(DrawSurface gc) {
        buildProjection();
        for (CubeFace face : visibleFaces.fg) {
            drawFacePolygon(gc, face, true);
        }
        drawGrid(gc, visibleFaces.fg, cubeStyle().gridForeground(), scaleDepth.grid());
        for (CubeFace face : visibleFaces.fg) {
            drawBorder(gc, face, scaleDepth.border());
        }
        renderAxesTitles(gc);
    }

    private void drawFacePolygon(DrawSurface gc, CubeFace cubeFace, boolean foreground) {
        int face = cubeFace.ordinal();
        var panel = foreground ? cubeStyle().panelForeground() : cubeStyle().panel();
        var corners = new ArrayList<ProjResult>();
        for (int idx : FACE_CORNERS[face]) {
            int bx = (idx >> 2) & 1, by = (idx >> 1) & 1, bz = idx & 1;
            corners.add(toPixels(rotatePerspective(EX[bx], EX[by], EX[bz])));
        }
        gc.setFill(panel.fill());
        gc.setStroke(panel.colour());
        gc.setLineWidth(panel.linewidth());
        gc.beginPath();
        gc.moveTo(corners.get(0).sx, corners.get(0).sy);
        for (int i = 2; i < corners.size(); i++) {
            var c = corners.get(i);
            gc.lineTo(c.sx, c.sy);
        }
        gc.lineTo(corners.get(1).sx, corners.get(1).sy);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    private void drawBorder(DrawSurface gc, CubeFace cubeFace, double strength) {
        int face = cubeFace.ordinal();
        var panel = cubeStyle().panel();
        double baseLw = panel.linewidth();
        var pts = new ArrayList<ProjResult>();
        for (int idx : FACE_CORNERS[face]) {
            int bx = (idx >> 2) & 1, by = (idx >> 1) & 1, bz = idx & 1;
            pts.add(toPixels(rotatePerspective(EX[bx], EX[by], EX[bz])));
        }
        gc.setStroke(panel.colour());
        // FACE_CORNERS rows are not in perimeter order; the boundary run is
        // [0, 2, 3, 1] (the same run the face fill uses). Drawing consecutive
        // pairs would stroke the two diagonals of each face.
        int[] perim = {0, 2, 3, 1};
        for (int e = 0; e < 4; e++) {
            var a = pts.get(perim[e]);
            var b = pts.get(perim[(e + 1) % 4]);
            double ds = Math.pow((a.depthScale + b.depthScale) / 2, strength);
            gc.setLineWidth(Math.max(0, baseLw * ds));
            gc.strokeLine(a.sx, a.sy, b.sx, b.sy);
        }
    }

    private void drawGrid(DrawSurface gc, CubeFace[] faces, Color color, double strength) {
        if (faces.length == 0) return;
        double baseLw = cubeStyle().gridLineWidth();
        record Segment(ProjResult a, ProjResult b) {}
        List<Segment> segs = new ArrayList<>();
        // Gridlines and the axis furniture share the same break positions: the
        // standard coordinate of a break is -0.5 + ts, exactly where the ticks
        // place their mark, so a face gridline always runs through the matching
        // tick on the edge. (zBreakTs() already handles the categorical z case.)
        double[][] breaks = {breakValues(minDataX, maxDataX), breakValues(minDataY, maxDataY), zBreakValues()};
        double[][] ts = {breakTs(minDataX, maxDataX), breakTs(minDataY, maxDataY), zBreakTs()};
        for (CubeFace cubeFace : faces) {
            int face = cubeFace.ordinal();
            int axis = face / 2;
            double fixed = face % 2 == 0 ? -0.5 : 0.5;
            for (int v = 0; v < 3; v++) {
                if (v == axis) continue;
                int other = 3 - axis - v;
                for (int i = 0; i < breaks[v].length; i++) {
                    double t = -0.5 + ts[v][i];
                    // A gridline on the face plane: the face's axis is fixed at
                    // `fixed`, the grid dimension v sits at the break position,
                    // and the line spans the remaining dimension.
                    double[] a = {-0.5, -0.5, -0.5};
                    double[] b2 = {0.5, 0.5, 0.5};
                    a[axis] = fixed;
                    b2[axis] = fixed;
                    a[v] = t;
                    b2[v] = t;
                    a[other] = -0.5;
                    b2[other] = 0.5;
                    segs.add(new Segment(
                            toPixels(rotatePerspective(a[0], a[1], a[2])),
                            toPixels(rotatePerspective(b2[0], b2[1], b2[2]))));
                }
            }
        }
        segs.sort(Comparator.comparingDouble(s -> (s.a.depth + s.b.depth) / 2));
        gc.setStroke(color);
        for (var s : segs) {
            double ds = Math.pow((s.a.depthScale + s.b.depthScale) / 2, strength);
            gc.setLineWidth(Math.max(0, baseLw * ds));
            gc.strokeLine(s.a.sx, s.a.sy, s.b.sx, s.b.sy);
        }
    }

    // --- breaks ---

    /** {@return the number of categories for a categorical axis, or {@code -1}
     * when the axis maps continuously} */
    private int categoryCount(int axis) {
        return switch (axis) {
            case 0 -> xIsCategorical ? xCategories.size() : -1;
            case 1 -> yIsCategorical ? yCategories.size() : -1;
            default -> zIsCategorical ? zCategories.size() : -1;
        };
    }

    /** {@return the category label at an ordinal tick position, or {@code null}
     * when the position falls outside the band count} */
    private String categoryLabel(int axis, double pos) {
        var cats = switch (axis) {
            case 0 -> xCategories;
            case 1 -> yCategories;
            default -> zCategories;
        };
        if (cats == null) return null;
        int i = (int) Math.round(pos);
        return i >= 0 && i < cats.size() ? cats.get(i) : null;
    }

    /** {@return the custom z label paired with the given break value, or
     * {@code null} when no matching custom label is configured} */
    private String customZLabel(double value) {
        if (zBreaks == null || zLabels == null) return null;
        for (int i = 0; i < zBreaks.size() && i < zLabels.size(); i++) {
            if (Math.abs(zBreaks.get(i) - value) < 1e-9) {
                return zLabels.get(i);
            }
        }
        return null;
    }

    /** Tick positions for an axis: plain ordinals {@code 0..N-1} for a
     * categorical axis (one tick centred under each band), pretty breaks
     * otherwise. */
    private double[] axisBreakValues(int axis) {
        int n = categoryCount(axis);
        if (n >= 0) {
            double[] b = new double[n];
            for (int i = 0; i < n; i++) b[i] = i;
            return b;
        }
        return switch (axis) {
            case 0 -> breakValues(minDataX, maxDataX);
            case 1 -> breakValues(minDataY, maxDataY);
            default -> breakValues(minDataZ, maxDataZ);
        };
    }

    /** Edge fractions for {@link #axisBreakValues}. Each categorical tick sits
     * at the centre of its band — {@code (i + 0.5) / n}, leaving a half-band
     * gap at either end — so the mark (and its label) aligns with the middle
     * of the bar, column, or tile it labels. */
    private double[] axisBreakTs(int axis) {
        int n = categoryCount(axis);
        if (n >= 0) {
            double[] ts = new double[n];
            for (int i = 0; i < n; i++) ts[i] = (i + 0.5) / n;
            return ts;
        }
        return switch (axis) {
            case 0 -> breakTs(minDataX, maxDataX);
            case 1 -> breakTs(minDataY, maxDataY);
            default -> breakTs(minDataZ, maxDataZ);
        };
    }

    private double[] zBreakValues() {
        if (zBreaks != null && !zBreaks.isEmpty()) {
            double[] b = new double[zBreaks.size()];
            for (int i = 0; i < b.length; i++) b[i] = zBreaks.get(i);
            return breaksWithin(minDataZ, maxDataZ, b);
        }
        if (zIsCategorical) {
            int n = zCategories.size();
            double[] b = new double[n];
            for (int i = 0; i < n; i++) b[i] = i;
            return b;
        }
        return breakValues(minDataZ, maxDataZ);
    }

    private static double[] breakValues(double min, double max) {
        return breaksWithin(min, max, prettyBreaks(min, max, 6));
    }

    private double[] zBreakTs() {
        if (zBreaks != null && !zBreaks.isEmpty()) {
            double[] breaks = zBreakValues();
            double[] ts = new double[breaks.length];
            double span = maxDataZ - minDataZ;
            for (int i = 0; i < breaks.length; i++) {
                ts[i] = span == 0 ? 0 : (breaks[i] - minDataZ) / span;
            }
            return ts;
        }
        if (zIsCategorical) {
            int n = zCategories.size();
            double[] ts = new double[n];
            for (int i = 0; i < n; i++) ts[i] = (i + 0.5) / n;
            return ts;
        }
        return breakTs(minDataZ, maxDataZ);
    }

    private static double[] breakTs(double min, double max) {
        double[] breaks = breaksWithin(min, max, prettyBreaks(min, max, 6));
        double[] ts = new double[breaks.length];
        for (int i = 0; i < breaks.length; i++) {
            ts[i] = (breaks[i] - min) / (max - min);
        }
        return ts;
    }

    /** Keeps only the breaks that fall inside the axis {@code [min, max]} range,
     * so no tick mark or gridline ever renders past the cube corners. */
    private static double[] breaksWithin(double min, double max, double[] breaks) {
        int n = 0;
        for (double v : breaks) {
            if (v >= min && v <= max) n++;
        }
        if (n == breaks.length) return breaks;
        double[] kept = new double[n];
        int k = 0;
        for (double v : breaks) {
            if (v >= min && v <= max) kept[k++] = v;
        }
        return kept;
    }

    /**
     * Generates "pretty" tick positions at round numbers (1, 2, 5, 10, 20, …).
     * Adapted from Wilkinson's algorithm.
     */
    private static double[] prettyBreaks(double min, double max, int target) {
        if (min >= max) return new double[]{min};
        double range = max - min;
        double roughStep = range / (target - 1);
        double mag = Math.pow(10, Math.floor(Math.log10(roughStep)));
        double norm = roughStep / mag;
        double niceStep;
        if (norm <= 1.5) niceStep = 1 * mag;
        else if (norm <= 3.5) niceStep = 2 * mag;
        else if (norm <= 7.5) niceStep = 5 * mag;
        else niceStep = 10 * mag;

        double start = Math.floor(min / niceStep) * niceStep;
        int n = (int) Math.ceil((max - start) / niceStep) + 1;
        double[] b = new double[n];
        for (int i = 0; i < n; i++) b[i] = start + i * niceStep;
        return b;
    }

    /** Formats a tick value, stripping unnecessary trailing zeros. */
    private static String formatBreak(double v) {
        if (v == Math.rint(v)) return String.valueOf((int) v);
        var s = String.format(Locale.US, "%.2f", v);
        if (s.endsWith(".00")) return s.substring(0, s.length() - 3);
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // --- axis furniture ---

    private final Text _meas = new Text();

    /** An edge of the cube parallel to an axis, in standard coordinates. */
    @SuppressWarnings("ArrayRecordComponent")
    private record CubeEdge(int axis, Vector3 v1, Vector3 v2) {}

    /**
     * The axis selection for one axis: the labelled edge, the face whose
     * gridlines the labels sit inline with (the chosen face), and
     * whether that edge lies on the cube/panel silhouettes. The chosen face
     * fixes the gridline direction labels and ticks follow.
     */
    @SuppressWarnings("ArrayRecordComponent")
    private record AxisSelection(CubeEdge edge, int faceIdx, boolean onCube, boolean onPanel) {}

    /** A scored axis-edge candidate (edge plus one of the faces it touches). */
    private record Scored(CubeEdge edge, int face, boolean onCube, boolean onPanel, double perp,
                          double area, double depth, double corner, double length) {}

    /** The component of a cube-space vector along the given axis. */
    private static double component(Vector3 v, int axis) {
        return switch (axis) {
            case 0 -> v.x();
            case 1 -> v.y();
            default -> v.z();
        };
    }

    /**
     * Selects the edge of the cube along which an axis's ticks, labels, and
     * title are drawn: every axis-parallel edge that touches a rendered face
     * is scored by its externality to the cube/panel silhouettes, its
     * perpendicularity to the face's gridlines, its length, its projected
     * face area, its depth, and its bottom-left position; the best edge wins.
     * This places the x and y furniture on the bottom-front edges and the z
     * furniture on the left.
     */
    private AxisSelection chooseAxisEdge(int axis, List<CubeFace> drawnFaces) {
        int a2 = (axis + 1) % 3, a3 = (axis + 2) % 3;
        var edges = new ArrayList<CubeEdge>();
        var touching = new ArrayList<List<CubeFace>>();
        for (int s2 = 0; s2 < 2; s2++) {
            for (int s3 = 0; s3 < 2; s3++) {
                double v2 = s2 == 0 ? -0.5 : 0.5;
                double v3 = s3 == 0 ? -0.5 : 0.5;
                double[] v1 = {-0.5, -0.5, -0.5}, v2v = {0.5, 0.5, 0.5};
                v1[axis] = -0.5; v2v[axis] = 0.5;
                v1[a2] = v2; v2v[a2] = v2;
                v1[a3] = v3; v2v[a3] = v3;
                edges.add(new CubeEdge(axis, new Vector3(v1[0], v1[1], v1[2]), new Vector3(v2v[0], v2v[1], v2v[2])));
                touching.add(List.of(CubeFace.values()[a2 * 2 + s2], CubeFace.values()[a3 * 2 + s3]));
            }
        }

        // Scoring happens in the transformed (pre-npc) coordinates returned by
        // the standard 3-D transform, whose y is the math y (up-positive). Screen
        // y is down-positive, so scoring on screen coords would invert the
        // bottom-left position preference and place furniture on the top edges.
        double[] cubeHull = projectedHull(allCubeCorners());
        double[] panelHull = projectedHull(drawnFacesCorners(drawnFaces));
        double maxCornerAll = -1e18;
        for (var c : allCubeCorners()) {
            var p = rotatePerspective(c[0], c[1], c[2]);
            maxCornerAll = Math.max(maxCornerAll, p.x + p.y);
        }

        var scored = new ArrayList<Scored>();
        for (int e = 0; e < edges.size(); e++) {
            var edge = edges.get(e);
            var p1 = rotatePerspective(edge.v1().x(), edge.v1().y(), edge.v1().z());
            var p2 = rotatePerspective(edge.v2().x(), edge.v2().y(), edge.v2().z());
            for (CubeFace face : touching.get(e)) {
                if (!drawnFaces.contains(face)) {
                    continue;
                }
                boolean onCube = edgeOnHull(p1, p2, cubeHull);
                boolean onPanel = edgeOnHull(p1, p2, panelHull);
                double perp = perpendicularityScore(edge, face, axis);
                double area = faceArea2d(face);
                double depth = (p1.depth + p2.depth) / 2;
                double corner = (p1.x + p1.y + p2.x + p2.y) / 2;
                double length = Math.hypot(p2.x - p1.x, p2.y - p1.y);
                scored.add(new Scored(edge, face.ordinal(), onCube, onPanel,
                        perp, area, depth, corner, length));
            }
        }
        if (scored.isEmpty()) {
            return null;
        }

        // Silhouette externality scales the score rather than filtering the
        // candidates outright. A hard filter (a hierarchical silhouette-first
        // reduction) is too blunt: in near-face-on views like
        // coord3d().pitch(0).roll(0).yaw(0) the only edge whose face projects a
        // non-degenerate gridline can be interior to the silhouettes, and
        // excluding it stripped the axis of its labels. Instead every candidate
        // stays eligible and the interior edges pay a heavy price, so a
        // well-rendering interior edge still wins when nothing on the silhouette
        // works, while the length/area advantages that once let interior edges
        // beat silhouette edges in coord3d().panels(CubePanel.ALL) can no longer
        // do so.
        double maxLen = 0, maxArea = 0, minCorner = 1e18;
        for (var s : scored) {
            maxLen = Math.max(maxLen, s.length());
            maxArea = Math.max(maxArea, s.area());
            minCorner = Math.min(minCorner, s.corner());
        }
        double lenDenom = maxLen > 0 ? maxLen : 1;
        double areaDenom = maxArea > 0 ? maxArea : 1;
        double maxInverted = maxCornerAll - minCorner;
        if (maxInverted <= 0) {
            maxInverted = 1;
        }
        // Scoring weights: perpendicularity 5, length 3, area 2, position 2.
        // Ties resolve by depth (closer wins), then corner (bottom-left wins).
        Scored best = null;
        double bestScore = -1e18, bestDepth = 0, bestCorner = 0;
        for (var s : scored) {
            double score = axisEdgeScore(s, lenDenom, areaDenom, maxCornerAll, maxInverted)
                    * externality(s);
            if (best == null || score > bestScore + 1e-12
                    || (Math.abs(score - bestScore) <= 1e-12
                            && (s.depth() < bestDepth
                                    || (s.depth() == bestDepth && s.corner() < bestCorner)))) {
                bestScore = score;
                bestDepth = s.depth();
                bestCorner = s.corner();
                best = s;
            }
        }
        if (best == null) {
            return null;
        }
        // Continuity across frames: while the previously chosen edge for this
        // axis is still a valid candidate (its face is still drawn and the
        // edge is still visible), keep it. This stops the furniture from
        // hopping between cube edges as the view orbits; it only moves when
        // the old face rotates out of the drawn set (or the edge becomes
        // degenerate, e.g. projecting edge-on). The cube's own rotation is
        // reflected in the smooth motion of the ticks/labels along the edge.
        var last = lastAxisSelection[axis];
        if (last != null) {
            boolean stillValid = false;
            for (var s : scored) {
                if (sameEdge(s.edge(), last.edge()) && s.face() == last.faceIdx()) {
                    stillValid = true;
                    break;
                }
            }
            if (stillValid) {
                lastAxisSelection[axis] = last;
                return last;
            }
        }
        lastAxisSelection[axis] = new AxisSelection(best.edge(), best.face(),
                best.onCube(), best.onPanel());
        return lastAxisSelection[axis];
    }

    /** A silhouette-externality weight for an axis-edge candidate: edges on both
     * the cube and panel hulls are preferred, then the panel hull only, then
     * the cube hull only; an edge on neither silhouette is heavily discounted
     * so its projected length/area advantages cannot beat a silhouette edge
     * (the coord3d().panels(CubePanel.ALL) label-collision bug), yet it stays eligible
     * for degenerate views where no silhouette edge renders a usable face. */
    private static double externality(Scored s) {
        if (s.onCube() && s.onPanel()) return 1.0;
        if (s.onPanel()) return 0.8;
        if (s.onCube()) return 0.6;
        return 0.05;
    }

    /** The score for one axis-edge candidate: each normalised factor
     * raised to its weight (perpendicularity 5, length 3, area 2, position 2,
     * normalised to sum one) and multiplied. */
    private static double axisEdgeScore(Scored s, double lenDenom, double areaDenom,
            double maxCornerAll, double maxInverted) {
        double np = Math.max(0, Math.min(1, s.perp() / 90.0));
        double nl = s.length() / lenDenom;
        double na = s.area() / areaDenom;
        double npos = (maxCornerAll - s.corner()) / maxInverted;
        return Math.pow(np, 5.0 / 12) * Math.pow(nl, 3.0 / 12)
                * Math.pow(na, 2.0 / 12) * Math.pow(npos, 2.0 / 12);
    }

    private static boolean sameEdge(CubeEdge a, CubeEdge b) {
        return a.axis() == b.axis()
                && Math.abs(a.v1().x() - b.v1().x()) < 1e-9
                && Math.abs(a.v1().y() - b.v1().y()) < 1e-9
                && Math.abs(a.v1().z() - b.v1().z()) < 1e-9;
    }

    /** The projected 2-D silhouette hull of a set of standard-domain corners. */
    private double[] projectedHull(List<double[]> corners) {
        var xs = new ArrayList<Double>();
        var ys = new ArrayList<Double>();
        for (var c : corners) {
            var p = rotatePerspective(c[0], c[1], c[2]);
            xs.add(p.x);
            ys.add(p.y);
        }
        return convexHullOf(xs, ys);
    }

    private static double[] convexHullOf(List<Double> xs, List<Double> ys) {
        var xa = xs.stream().mapToDouble(Double::doubleValue).toArray();
        var ya = ys.stream().mapToDouble(Double::doubleValue).toArray();
        int[] hull = PolygonMath.convexHull(xa, ya);
        if (hull == null || hull.length < 3) {
            // Degenerate (fewer than three distinct/collinear points): no
            // meaningful silhouette. edgeOnHull() treats a null hull as
            // "everything counts", which is what we want for empty panels.
            return null;
        }
        var out = new double[hull.length * 2];
        for (int i = 0; i < hull.length; i++) {
            out[i * 2] = xa[hull[i]];
            out[i * 2 + 1] = ya[hull[i]];
        }
        return out;
    }

    private List<double[]> allCubeCorners() {
        var out = new ArrayList<double[]>(8);
        for (int bx = 0; bx < 2; bx++) {
            for (int by = 0; by < 2; by++) {
                for (int bz = 0; bz < 2; bz++) {
                    out.add(new double[] {EX[bx], EX[by], EX[bz]});
                }
            }
        }
        return out;
    }

    private List<double[]> drawnFacesCorners(List<CubeFace> drawnFaces) {
        var out = new ArrayList<double[]>();
        for (CubeFace face : drawnFaces) {
            int fi = face.ordinal();
            for (int idx : FACE_CORNERS[fi]) {
                int bx = (idx >> 2) & 1, by = (idx >> 1) & 1, bz = idx & 1;
                out.add(new double[] {EX[bx], EX[by], EX[bz]});
            }
        }
        return out;
    }

    /** Whether an edge's projected midpoint lies on the hull boundary. */
    private static boolean edgeOnHull(ProjectedPoint a, ProjectedPoint b, double[] hull) {
        if (hull == null || hull.length < 3) {
            return true;
        }
        double midX = (a.x + b.x) / 2, midY = (a.y + b.y) / 2;
        int n = hull.length / 2;
        double scale = 0;
        for (int i = 0; i < n; i++) {
            scale = Math.max(scale, Math.abs(hull[i * 2]));
            scale = Math.max(scale, Math.abs(hull[i * 2 + 1]));
        }
        if (!Double.isFinite(scale) || scale <= 0) {
            return true;
        }
        double tol = 1e-6 * scale;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            var c = PolygonMath.closestPointOnSegment(midX, midY,
                    hull[i * 2], hull[i * 2 + 1], hull[j * 2], hull[j * 2 + 1]);
            if (Math.hypot(c[0] - midX, c[1] - midY) <= tol) {
                return true;
            }
        }
        return false;
    }

    /**
     * How perpendicular an axis edge is to the gridlines that should cross it
     * on the face, measured in the projected plane and averaged over the face's
     * gridline pairs.
     */
    private double perpendicularityScore(CubeEdge edge, CubeFace face, int axis) {
        var e0 = rotatePerspective(edge.v1().x(), edge.v1().y(), edge.v1().z());
        var e1 = rotatePerspective(edge.v2().x(), edge.v2().y(), edge.v2().z());
        double ex = e1.x - e0.x, ey = e1.y - e0.y;
        double el = Math.hypot(ex, ey);
        if (el < 1e-9) {
            return 0;
        }
        ex /= el;
        ey /= el;
        int faceAxis = face.ordinal() / 2;
        double sum = 0;
        int n = 0;
        for (int a = 0; a < 3; a++) {
            if (a == faceAxis || a == axis) {
                continue;
            }
            double[] p1 = {0, 0, 0}, p2 = {0, 0, 0};
            p1[a] = -0.5;
            p2[a] = 0.5;
            var d0 = rotatePerspective(p1[0], p1[1], p1[2]);
            var d1 = rotatePerspective(p2[0], p2[1], p2[2]);
            double ax = d1.x - d0.x, ay = d1.y - d0.y;
            double al = Math.hypot(ax, ay);
            if (al < 1e-9) {
                continue;
            }
            ax /= al;
            ay /= al;
            double dot = Math.abs(ex * ax + ey * ay);
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            sum += 90 - Math.abs(angle - 90);
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    /** The projected 2-D area of a cube face: the shoelace area of its four
     * projected corners. */
    private double faceArea2d(CubeFace face) {
        int fi = face.ordinal();
        var xs = new ArrayList<Double>();
        var ys = new ArrayList<Double>();
        for (int idx : FACE_CORNERS[fi]) {
            int bx = (idx >> 2) & 1, by = (idx >> 1) & 1, bz = idx & 1;
            var p = rotatePerspective(EX[bx], EX[by], EX[bz]);
            xs.add(p.x);
            ys.add(p.y);
        }
        var xa = xs.stream().mapToDouble(Double::doubleValue).toArray();
        var ya = ys.stream().mapToDouble(Double::doubleValue).toArray();
        int[] hull = PolygonMath.convexHull(xa, ya);
        if (hull.length < 3) {
            return 0;
        }
        var hx = new double[hull.length];
        var hy = new double[hull.length];
        for (int i = 0; i < hull.length; i++) {
            hx[i] = xa[hull[i]];
            hy[i] = ya[hull[i]];
        }
        return Math.abs(PolygonMath.signedArea(hx, hy)) / 2;
    }

    private CubeFace[] labelsFor(int axis) {
        return switch (axis) {
            case 0 -> xlabels;
            case 1 -> ylabels;
            default -> zlabels;
        };
    }

    /** Selects the edge to label along for a given axis, honouring an explicit
     * {@code xlabels}/{@code ylabels}/{@code zlabels} spec (a pair of adjacent
     * faces whose intersection is the edge) or falling back to the
     * peripheral auto edge. The selection also carries the chosen face whose
     * gridlines the labels run inline with (the chosen face):
     * the first named face that is drawn for a manual spec, or the face
     * paired with the winning edge for auto selection. */
    private AxisSelection chooseLabelEdge(int axis, CubeFace[] spec, List<CubeFace> drawnFaces) {
        if (spec.length == 2) {
            int f1 = spec[0].ordinal();
            int f2 = spec[1].ordinal();
            var fixed = fixedValues(axis, f1, f2);
            if (fixed != null) {
                // Labels can only be placed on visible (drawn) faces; when
                // neither of the specified faces is drawn the manual edge is
                // ignored in favour of the peripheral auto edge for this axis.
                boolean f1Drawn = drawnFaces.contains(spec[0]);
                boolean f2Drawn = drawnFaces.contains(spec[1]);
                if (!f1Drawn && !f2Drawn) {
                    return chooseAxisEdge(axis, drawnFaces);
                }
                double[] v1 = {-0.5, -0.5, -0.5};
                double[] v2 = {0.5, 0.5, 0.5};
                int a = fixed[0] / 2, b = fixed[1] / 2;
                v1[a] = fixed[0] % 2 == 0 ? -0.5 : 0.5; v2[a] = v1[a];
                v1[b] = fixed[1] % 2 == 0 ? -0.5 : 0.5; v2[b] = v1[b];
                v1[axis] = -0.5; v2[axis] = 0.5;
                var edge = new CubeEdge(axis, new Vector3(v1[0], v1[1], v1[2]), new Vector3(v2[0], v2[1], v2[2]));
                // Chosen face honours the manual order: the first face
                // when it is drawn, otherwise the second.
                int faceIdx = f1Drawn ? f1 : f2;
                var p1 = rotatePerspective(edge.v1().x(), edge.v1().y(), edge.v1().z());
                var p2 = rotatePerspective(edge.v2().x(), edge.v2().y(), edge.v2().z());
                boolean onCube = edgeOnHull(p1, p2, projectedHull(allCubeCorners()));
                boolean onPanel = edgeOnHull(p1, p2,
                        projectedHull(drawnFacesCorners(drawnFaces)));
                return new AxisSelection(edge, faceIdx, onCube, onPanel);
            }
        }
        return chooseAxisEdge(axis, drawnFaces);
    }

    /** Given an edge axis and two faces, returns the two face indices that fix
     * the edge's other two dimensions, or {@code null} if the faces don't
     * sandwich an axis-parallel edge (e.g. they fix the edge axis itself or the
     * same dimension twice). the values are combined face indices
     * {@code (dim*2 + side)}. */
    private static int[] fixedValues(int axis, int face1, int face2) {
        int d1 = face1 / 2, d2 = face2 / 2;
        if (d1 == axis || d2 == axis || d1 == d2) return null;
        return new int[]{face1, face2};
    }

    private void renderTicksAndLabels(DrawSurface gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        drawnLabelBoxes.clear();
        renderAxisFurniture(gc, 0, axisStyle(), cubeStyle().zTickLength(), cubeStyle().tickLabelPad());
        renderAxisFurniture(gc, 1, axisStyle(), cubeStyle().zTickLength(), cubeStyle().tickLabelPad());
        renderAxisFurniture(gc, 2, zAxisStyle(), cubeStyle().zTickLength(), cubeStyle().tickLabelPad());
    }

    private List<CubeFace> allVisibleFaces() {
        var all = new ArrayList<CubeFace>();
        for (CubeFace f : visibleFaces.bg) {
            all.add(f);
        }
        for (CubeFace f : visibleFaces.fg) {
            all.add(f);
        }
        return all;
    }

    private void renderAxisFurniture(DrawSurface gc, int axis, AxisStyle st,
            double tickLength, double labelPad) {
        labelReach[axis] = 0;
        var drawnFaces = allVisibleFaces();
        var sel = chooseLabelEdge(axis, labelsFor(axis), drawnFaces);
        if (sel == null) return;
        var edge = sel.edge();
        var v1 = edge.v1();
        var v2 = edge.v2();
        double[] values = axisBreakValues(axis);
        double[] ts = axisBreakTs(axis);

        var e0 = toPixels(rotatePerspective(v1.x(), v1.y(), v1.z()));
        var e1 = toPixels(rotatePerspective(v2.x(), v2.y(), v2.z()));
        double ul = Math.hypot(e1.sx - e0.sx, e1.sy - e0.sy);
        if (ul < 1e-9) return;
        double ux = (e1.sx - e0.sx) / ul, uy = (e1.sy - e0.sy) / ul;
        double mmx = (e0.sx + e1.sx) / 2 - centerX;
        double mmy = (e0.sy + e1.sy) / 2 - centerY;
        double nx = -uy, ny = ux;
        if (nx * mmx + ny * mmy < 0) { nx = -nx; ny = -ny; }

        // Labels and ticks are rotated to run inline with the gridlines of the
        // chosen face: the free axis on that face perpendicular to the labelled
        // axis, so they stay orthogonal to the axis rather than parallel to it.
        // Titles keep the axis-edge angle instead.
        int faceAxis = sel.faceIdx() / 2;
        int free = 0;
        while (free == axis || free == faceAxis) free++;
        double faceVal = component(v1, faceAxis);
        double[] gp0 = {0, 0, 0}, gp1 = {0, 0, 0};
        gp0[free] = -0.5;
        gp1[free] = 0.5;
        var g0 = toPixels(rotatePerspective(gp0[0], gp0[1], gp0[2]));
        var g1 = toPixels(rotatePerspective(gp1[0], gp1[1], gp1[2]));
        double gx = g1.sx - g0.sx, gy = g1.sy - g0.sy;
        double gl = Math.hypot(gx, gy);
        if (gl < 1e-9) return;
        gx /= gl;
        gy /= gl;
        // Ticks and labels extend along the free axis, pointing away from the
        // cube centre (the projected origin): choose the sign whose direction
        // has a positive component from the centre to the edge's midpoint. This
        // is stable under rotation — unlike the silhouette-boolean flip, which
        // jumped the furniture to the other side of the edge as the view moved.
        var em = toPixels(rotatePerspective(
                (v1.x() + v2.x()) / 2, (v1.y() + v2.y()) / 2, (v1.z() + v2.z()) / 2));
        double freeSign = (em.sx - centerX) * gx + (em.sy - centerY) * gy >= 0 ? 1 : -1;
        double flipOutX = gx * freeSign, flipOutY = gy * freeSign;
        // The labels' outward direction through the edge midpoint: the title is
        // placed along it (and the label reach measured along it) so the title
        // always sits beyond its labels — the edge normal can point the other
        // way in a near-face-on view.
        double[] outMid = labelOutDirection(sel, axis);
        double outMidX = outMid[0], outMidY = outMid[1];

        gc.setStroke(st.tickColor());
        gc.setLineWidth(st.axisLineWidth());
        gc.setFill(st.tickLabelColor());
        gc.setFont(st.tickLabelFont());
        _meas.setFont(st.tickLabelFont());

        double tickStrength = scaleDepth.ticks();
        double textStrength = scaleDepth.text();
        // Reference gridline direction through the edge midpoint, used to fold
        // every label on this axis the same way (see below).
        double labelRefAngle = gridlineAngle(axis, faceAxis, faceVal, free, 0.0);
        for (int bi = 0; bi < values.length; bi++) {
            if (ts[bi] < -0.01 || ts[bi] > 1.01) continue;
            double[] sp = {v1.x() + (v2.x() - v1.x()) * ts[bi],
                           v1.y() + (v2.y() - v1.y()) * ts[bi],
                           v1.z() + (v2.z() - v1.z()) * ts[bi]};
            var pos = toPixels(rotatePerspective(sp[0], sp[1], sp[2]));
            double tickPx = Math.max(0, tickLength * Math.pow(pos.depthScale, tickStrength));
            // The tick and label extend along the gridline that passes through
            // this break: the free-axis direction projected at the break's own
            // position (axis=ac, face=faceVal), not through the cube centre.
            // Under perspective the projected direction of the free-axis line
            // varies along the face, so a centre-fixed direction would leave the
            // ticks pointing at the wrong angle while the gridlines fan out.
            double ac = -0.5 + ts[bi];
            double rawAngle = gridlineAngle(axis, faceAxis, faceVal, free, ac);
            double outX = Math.cos(Math.toRadians(rawAngle)) * freeSign;
            double outY = Math.sin(Math.toRadians(rawAngle)) * freeSign;
            double tx = pos.sx + outX * tickPx, ty = pos.sy + outY * tickPx;
            gc.strokeLine(pos.sx, pos.sy, tx, ty);

            String label = axis == 2 ? customZLabel(values[bi]) : null;
            if (label == null) label = categoryLabel(axis, values[bi]);
            if (label == null) label = formatBreak(values[bi]);
            if (label.isEmpty()) continue;
            _meas.setText(label);
            double textW = _meas.getLayoutBounds().getWidth();
            double textH = _meas.getLayoutBounds().getHeight();
            double textPx = Math.max(0, labelPad * Math.pow(pos.depthScale, textStrength));
            // The label anchor is a fixed distance past its own tick tip, along
            // the same gridline direction the tick uses. This is reliable in
            // every view: the label always sits just beyond its tick and never
            // beside it, behind it, or far away. Offsetting along the edge
            // normal instead drifted the labels away from the oblique ticks
            // (and in near-face-on views the "away from centre" normal could
            // even point opposite the tick), and scaling by 1/(out·normal) to
            // hold a constant normal margin blew the offset up whenever the
            // gridline ran nearly parallel to the edge.
            double lx = pos.sx + outX * (tickPx + textPx);
            double ly = pos.sy + outY * (tickPx + textPx);
            if (!rotateLabels) {
                // Centred horizontal label; skip it if its text box would
                // overlap an already drawn label on this panel.
                if (labelCollides(lx, ly, 0, textW, textH, true)) {
                    continue;
                }
                gc.fillText(label, lx, ly);
                noteLabelReach(axis, pos.sx, pos.sy, outMidX, outMidY, lx, ly, textW, textH, 0, true, true);
                continue;
            }
            // Label angle follows the gridline at this break: from the free
            // axis min end to its max end on the chosen face, folded into
            // [-90, 90] so the text always reads. The fold is
            // chosen once per axis from the gridline direction through the
            // edge midpoint, so every label on the axis shares one reading
            // direction and they flip together — never independently — when
            // the axis turns over.
            double angle = foldLabel(rawAngle + ((flipOutX * Math.cos(Math.toRadians(labelRefAngle))
                    + flipOutY * Math.sin(Math.toRadians(labelRefAngle))) < 0 ? 180 : 0));
            double tRad = Math.toRadians(angle);
            double tdx = Math.cos(tRad), tdy = Math.sin(tRad);
            double dot = outX * tdx + outY * tdy;
            boolean leftEnd = dot > 0;
            // In a small panel (or where two axes' furniture meet at a corner)
            // tick labels can collide with each other; skip a label whose text
            // box would overlap any label already drawn on this panel. Its tick
            // is already drawn and its gridline still runs through the break,
            // so the axis stays readable without the smear.
            if (labelCollides(lx, ly, angle, textW, textH, leftEnd)) {
                continue;
            }
            gc.setTextAlign(leftEnd ? TextAlignment.LEFT : TextAlignment.RIGHT);
            noteLabelReach(axis, pos.sx, pos.sy, outMidX, outMidY, lx, ly, textW, textH, angle, false, leftEnd);
            gc.save();
            gc.translate(lx, ly);
            gc.rotate(angle);
            gc.fillText(label, 0, 0);
            gc.restore();
        }
        gc.setTextAlign(TextAlignment.CENTER);
    }

    /** Whether the label text box at the given anchor collides with any label
     * already drawn in this render pass. Records the box when it is clear. */
    private boolean labelCollides(double lx, double ly, double angleDeg,
            double textW, double textH, boolean leftEnd) {
        double[][] box = labelBox(lx, ly, angleDeg, textW, textH, leftEnd);
        for (double[][] other : drawnLabelBoxes) {
            if (boxesOverlap(box, other)) {
                return true;
            }
        }
        drawnLabelBoxes.add(box);
        return false;
    }

    /** The screen-space corners of a label's text box, for the label-thinning
     * overlap check. {@code leftEnd} anchors the box at its near (edge-facing)
     * end; {@code centered} centres it (the horizontal-label case). */
    private static double[][] labelBox(double lx, double ly, double angleDeg,
            double textW, double textH, boolean leftEnd) {
        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double[] xs = leftEnd ? new double[]{0, textW, textW, 0}
                : new double[]{-textW, 0, 0, -textW};
        double[][] out = new double[4][2];
        for (int i = 0; i < 4; i++) {
            double x = xs[i];
            double y = (i < 2 ? -1 : 1) * textH / 2;
            out[i][0] = lx + x * cos - y * sin;
            out[i][1] = ly + x * sin + y * cos;
        }
        return out;
    }

    /** Whether two convex screen-space polygons overlap (separating-axis test). */
    private static boolean boxesOverlap(double[][] a, double[][] b) {
        return !separated(a, b) && !separated(b, a);
    }

    private static boolean separated(double[][] a, double[][] b) {
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            double nx = -(a[j][1] - a[i][1]), ny = a[j][0] - a[i][0];
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE;
            double minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
            for (double[] p : a) {
                double v = p[0] * nx + p[1] * ny;
                minA = Math.min(minA, v);
                maxA = Math.max(maxA, v);
            }
            for (double[] p : b) {
                double v = p[0] * nx + p[1] * ny;
                minB = Math.min(minB, v);
                maxB = Math.max(maxB, v);
            }
            if (maxA < minB || maxB < minA) {
                return true;
            }
        }
        return false;
    }

    private void noteLabelReach(int axis, double px, double py, double nx, double ny,
            double ax, double ay, double textW, double textH, double angleDeg,
            boolean centered, boolean leftEnd) {
        double tRad = Math.toRadians(angleDeg);
        double tdx = Math.cos(tRad), tdy = Math.sin(tRad);
        double vdx = -tdy, vdy = tdx;
        double[] span = centered ? new double[]{-textW / 2, textW / 2}
                : (leftEnd ? new double[]{0, textW} : new double[]{-textW, 0});
        double reach = Double.NEGATIVE_INFINITY;
        for (double t : span) {
            for (double s : new double[]{-textH / 2, textH / 2}) {
                double crx = ax + t * tdx + s * vdx;
                double cry = ay + t * tdy + s * vdy;
                reach = Math.max(reach, (crx - px) * nx + (cry - py) * ny);
            }
        }
        if (reach > labelReach[axis]) labelReach[axis] = reach;
    }

    /** The projected angle (degrees) of the chosen face's gridline at a given
     * axis break: from the free-axis min end to its max end on the face.
     * @param axis the labelled axis
     * @param faceAxis the face-normal axis of the chosen face
     * @param faceVal the fixed face coordinate on {@code faceAxis}
     * @param free the free axis on the face (perpendicular to both)
     * @param ac the break coordinate along the labelled axis
     * @return the raw gridline angle in degrees, not yet folded */
    private double gridlineAngle(int axis, int faceAxis, double faceVal, int free, double ac) {
        double[] qa = {0, 0, 0}, qb = {0, 0, 0};
        qa[axis] = qb[axis] = ac;
        qa[faceAxis] = qb[faceAxis] = faceVal;
        qa[free] = -0.5;
        qb[free] = 0.5;
        var pa = toPixels(rotatePerspective(qa[0], qa[1], qa[2]));
        var pb = toPixels(rotatePerspective(qb[0], qb[1], qb[2]));
        return Math.toDegrees(Math.atan2(pb.sy - pa.sy, pb.sx - pa.sx));
    }

    /** Folds an angle into [-90, 90] so text always reads upright. */
    private static double foldLabel(double angle) {
        while (angle > 90) angle -= 180;
        while (angle < -90) angle += 180;
        return angle;
    }

    /** The unit screen direction the tick labels of an axis extend away from their
     * edge: the free-axis gridline through the edge midpoint, oriented away from
     * the cube centre — the same construction the ticks and labels use. The
     * edge normal (perpendicular to the edge) can point the other way in a
     * near-face-on view, which would set the axis title on the opposite side of
     * the edge from its labels. */
    private double[] labelOutDirection(AxisSelection sel, int axis) {
        var v1 = sel.edge().v1();
        var v2 = sel.edge().v2();
        int faceAxis = sel.faceIdx() / 2;
        int free = 0;
        while (free == axis || free == faceAxis) free++;
        double faceVal = component(v1, faceAxis);
        double raw = gridlineAngle(axis, faceAxis, faceVal, free, 0.0);
        double[] c0 = {0, 0, 0}, c1 = {0, 0, 0};
        c0[free] = -0.5;
        c1[free] = 0.5;
        var pa = toPixels(rotatePerspective(c0[0], c0[1], c0[2]));
        var pb = toPixels(rotatePerspective(c1[0], c1[1], c1[2]));
        double gx = pb.sx - pa.sx, gy = pb.sy - pa.sy;
        double gl = Math.hypot(gx, gy);
        gx /= gl;
        gy /= gl;
        var em = toPixels(rotatePerspective(
                (v1.x() + v2.x()) / 2, (v1.y() + v2.y()) / 2, (v1.z() + v2.z()) / 2));
        double freeSign = (em.sx - centerX) * gx + (em.sy - centerY) * gy >= 0 ? 1 : -1;
        double rad = Math.toRadians(raw);
        return new double[] {Math.cos(rad) * freeSign, Math.sin(rad) * freeSign};
    }

    private void renderAxesTitles(DrawSurface gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        renderAxisTitle(gc, 0, xLabel, axisStyle());
        renderAxisTitle(gc, 1, yLabel, axisStyle());
        renderAxisTitle(gc, 2, zLabel, zAxisStyle());
    }

    private void renderAxisTitle(DrawSurface gc, int axis, String title, AxisStyle st) {
        var drawnFaces = allVisibleFaces();
        var sel = chooseLabelEdge(axis, labelsFor(axis), drawnFaces);
        if (sel == null) return;
        var edge = sel.edge();
        var v1 = edge.v1();
        var v2 = edge.v2();
        double pad = labelReach[axis] + cubeStyle().axisTitlePad();
        gc.setFill(st.titleColor());
        gc.setFont(st.titleFont());
        _meas.setFont(st.titleFont());

        var e0 = toPixels(rotatePerspective(v1.x(), v1.y(), v1.z()));
        var e1 = toPixels(rotatePerspective(v2.x(), v2.y(), v2.z()));
        double ux = e1.sx - e0.sx, uy = e1.sy - e0.sy;
        double ul = Math.sqrt(ux * ux + uy * uy);
        if (ul < 1e-9) return;
        ux /= ul; uy /= ul;
        // The title sits beyond the tick labels, along the same outward
        // direction the labels extend (not the edge normal, which can point the
        // other way in a near-face-on view).
        double[] out = labelOutDirection(sel, axis);
        double ox = out[0], oy = out[1];

        // Titles are centered along the label edge (peripheral titles are
        // centered; the internal-axis "auto" near-end placement never applies
        // because every labelled edge — auto or an explicit xlabels/ylabels/
        // zlabels spec — is a peripheral label edge). The title's near edge
        // clears the tick labels by axisTitlePad: the anchor sits at the label
        // reach plus the title's own half-extent toward the edge.
        double tc = 0.5;
        double cx = e0.sx + (e1.sx - e0.sx) * tc;
        double cy = e0.sy + (e1.sy - e0.sy) * tc;
        double angle = Math.toDegrees(Math.atan2(uy, ux));
        if (angle > 90) angle -= 180; else if (angle < -90) angle += 180;
        _meas.setText(title);
        double textW = _meas.getLayoutBounds().getWidth();
        double textH = _meas.getLayoutBounds().getHeight();
        double rad = Math.toRadians(angle);
        double tdx = Math.cos(rad), tdy = Math.sin(rad);
        double pdx = -tdy, pdy = tdx;
        // The title box's extent toward the edge (the -out direction): placing
        // the center at labelReach + axisTitlePad + halfExtent puts the near
        // edge at labelReach + axisTitlePad, clear of the tick labels.
        double halfExtent = textW / 2 * Math.abs(tdx * ox + tdy * oy)
                + textH / 2 * Math.abs(pdx * ox + pdy * oy);
        double dist = pad + halfExtent;
        double hw = Math.abs(tdx) * textW / 2 + Math.abs(tdy) * textH / 2;
        double hh = Math.abs(tdy) * textW / 2 + Math.abs(tdx) * textH / 2;
        // Prefer the outward side (beyond the labels); if that would project off the
        // visible canvas — e.g. the elongated cube of coord3d(scales="fixed") —
        // fall back to the inward side instead of clamping the title onto the
        // tick labels. The clamp uses the full canvas, not the cube panel, so a
        // title can sit in the plot margin beside a small cube (composed cells).
        double pxOut = cx + ox * dist, pyOut = cy + oy * dist;
        double pxIn = cx - ox * dist, pyIn = cy - oy * dist;
        boolean fitsOut = pxOut >= canvasX + hw && pxOut <= canvasX + canvasW - hw
                && pyOut >= canvasY + hh && pyOut <= canvasY + canvasH - hh;
        boolean fitsIn = pxIn >= canvasX + hw && pxIn <= canvasX + canvasW - hw
                && pyIn >= canvasY + hh && pyIn <= canvasY + canvasH - hh;
        double px, py;
        if (fitsOut) {
            px = pxOut;
            py = pyOut;
        } else if (fitsIn) {
            px = pxIn;
            py = pyIn;
        } else {
            px = Math.max(canvasX + hw, Math.min(canvasX + canvasW - hw, pxOut));
            py = Math.max(canvasY + hh, Math.min(canvasY + canvasH - hh, pyOut));
        }
        gc.save();
        gc.translate(px, py);
        gc.rotate(angle);
        gc.fillText(title, 0, 0);
        gc.restore();
    }

    // --- Coord interface ---
    @Override
    public boolean isFlipped() { return false; }

    @Override
    public MinMax adjustXBounds(MinMax computedBounds) { return computedBounds; }

    @Override
    public MinMax adjustYBounds(MinMax computedBounds) { return computedBounds; }
}
