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
package org.jtaccuino.gog.interaction;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.interaction.Orbit3dOptions.Gesture;

/**
 * A compact wireframe mini-globe that controls the rotation of a 3-D
 * {@link Plot}. The widget draws the cube's current orientation as the three
 * axis lines (x red / y green / z blue, matching the cube furniture) springing
 * from the cube origin inside a faint bounding ring, and dragging it rotates
 * the plot's {@link Coord3D} with the same gesture and sensitivity as the bound
 * {@link Orbit3d} controller: the default trackball "grab and turn" motion, or
 * the {@link Gesture#MODIFIER} drag (Alt or right-drag rolls).
 * <p>
 * Small, self-contained, and disconnected from the plot's own render pipeline:
 * the axes use their own camera-space rotation (a mirror of
 * {@link Coord3D#buildProjection()}) at a fixed perspective distance, so a
 * drag repaints just the widget and marks the plot dirty once.
 */
public final class CubeOrbitControl extends Pane {

    /** Default edge-to-edge widget size in pixels. */
    public static final double DEFAULT_SIZE = 150;

    /** Camera distance of the axis view, fixed at the default distance 2. */
    private static final double DIST = 2.0;

    /** Pane-scale factor turning the fixed camera coordinates into pixels. */
    private static final double SCALE_FACTOR = 0.46;

    /** The axis line length in cube units, from the origin. */
    private static final double AXIS_LENGTH = 0.5;

    private static final Color X_AXIS = Color.web("#d62728");
    private static final Color Y_AXIS = Color.web("#2ca02c");
    private static final Color Z_AXIS = Color.web("#1f78b4");

    /** A single oriented axis: its unit direction and colour. */
    private record Axis(int axis, Color color) {
    }

    private static final Axis[] AXES = {
        new Axis(0, X_AXIS),
        new Axis(1, Y_AXIS),
        new Axis(2, Z_AXIS)
    };

    private final Plot<?> plot;
    private final Coord3D coord;
    private final Canvas canvas;
    private final double size;
    private Orbit3d orbit;
    private double pressX;
    private double pressY;

    /**
     * Creates a mini-globe for a 3-D plot at the default {@link #DEFAULT_SIZE}.
     *
     * @param plot the plot whose coordinate system this widget rotates
     * @throws IllegalArgumentException if the plot does not use a 3-D
     *         coordinate system
     */
    public CubeOrbitControl(Plot<?> plot) {
        this(plot, DEFAULT_SIZE);
    }

    /**
     * Creates a mini-globe for a 3-D plot.
     *
     * @param plot the plot whose coordinate system this widget rotates
     * @param size the widget's edge-to-edge size in pixels
     * @throws IllegalArgumentException if the plot does not use a 3-D
     *         coordinate system
     */
    public CubeOrbitControl(Plot<?> plot, double size) {
        if (!(plot.descriptor().coord() instanceof Coord3D coord)) {
            throw new IllegalArgumentException(
                    "CubeOrbitControl needs a 3-D coordinate system; use plot3d()/ggplot3d() or bind a coord3d()");
        }
        this.plot = plot;
        this.coord = coord;
        this.size = size;
        setPrefSize(size, size);
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        canvas = new Canvas(size, size);
        getChildren().add(canvas);
        setOnMousePressed(e -> {
            this.pressX = e.getX();
            this.pressY = e.getY();
        });
        setOnMouseDragged(e -> {
            double dx = e.getX() - pressX;
            double dy = e.getY() - pressY;
            double degreesPerPixel = orbit != null
                    ? orbit.options().degreesPerPixel()
                    : Orbit3dOptions.DEFAULT_DEGREES_PER_PIXEL;
            Gesture gesture = orbit != null
                    ? orbit.options().gesture()
                    : Gesture.TRACKBALL;
            if (gesture == Gesture.TRACKBALL) {
                double scale = degreesPerPixel / Orbit3dOptions.DEFAULT_DEGREES_PER_PIXEL;
                Orbit3d.applyTrackball(coord, size / 2.0, size / 2.0,
                        pressX, pressY, e.getX(), e.getY(), scale);
            } else {
                Orbit3d.applyModifier(coord, dx, dy,
                        e.isAltDown() || e.getButton() == MouseButton.SECONDARY, degreesPerPixel);
            }
            if (orbit != null) {
                orbit.notifyAnglesChanged();
            } else {
                plot.markDirty();
            }
            redraw();
        });
        redraw();
    }

    /**
     * The plot whose orientation this widget reflects and rotates.
     *
     * @return the owned plot
     */
    public Plot<?> plot() {
        return plot;
    }

    /**
     * Connects this widget to an {@link Orbit3d} controller so both stay in
     * sync: the widget redraws when the plot is dragged through the orbit
     * controller (and any readout it owns updates when the widget is dragged).
     *
     * @param controller the controller to mirror
     * @return this widget for fluid chaining
     */
    public CubeOrbitControl bind(Orbit3d controller) {
        this.orbit = controller;
        controller.onChange(this::redraw);
        return this;
    }

    /**
     * The connected orbit controller, or {@code null}.
     *
     * @return the bound controller
     */
    public Orbit3d orbit() {
        return orbit;
    }

    /**
     * Redraws the wireframe from the plot's current rotation.
     *
     * @return this widget for fluid chaining
     */
    public CubeOrbitControl refresh() {
        redraw();
        return this;
    }

    private void redraw() {
        draw(canvas.getGraphicsContext2D());
    }

    private void draw(GraphicsContext g) {
        double s = size;
        double c = s / 2.0;
        g.clearRect(0, 0, s, s);
        g.setGlobalAlpha(1.0);

        // The faint bounding ring reads as a "globe" the axes float in.
        double ringR = s * 0.37;
        g.setStroke(new Color(0, 0, 0, 0.16));
        g.setLineWidth(1.0);
        g.strokeOval(c - ringR, c - ringR, ringR * 2, ringR * 2);

        // The axes' camera-space rotation, mirrored from Coord3D.buildProjection():
        // pitch about the y axis, roll about the x axis, yaw about the z axis.
        double p = Math.toRadians(coord.pitch());
        double r = Math.toRadians(coord.roll());
        double y = Math.toRadians(coord.yaw());
        double cp = Math.cos(p), sp = Math.sin(p);
        double cr = Math.cos(r), sr = Math.sin(r);
        double cy = Math.cos(y), sy = Math.sin(y);
        double[][] m = {
            {cy * cp, cy * sp * sr - sy * cr, cy * sp * cr + sy * sr},
            {sy * cp, sy * sp * sr + cy * cr, sy * sp * cr - cy * sr},
            {-sp, cp * sr, cp * cr}
        };

        double[] o = project(m, 0, 0, 0);

        // The three axis lines, from the origin to the positive tip; the
        // negative half of each axis is drawn fainter for orientation.
        g.setLineWidth(1.8);
        for (var axis : AXES) {
            double[] dir = dir(axis.axis());
            double[] tip = project(m, dir[0] * AXIS_LENGTH, dir[1] * AXIS_LENGTH,
                    dir[2] * AXIS_LENGTH);
            g.setStroke(axis.color());
            g.strokeLine(o[0], o[1], tip[0], tip[1]);
        }

        // The origin — the point every axis springs from.
        g.setFill(new Color(0, 0, 0, 0.45));
        g.fillOval(o[0] - 2, o[1] - 2, 4, 4);

        // Axis glyph labels near the positive tips.
        g.setFont(Font.font("System", 10));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        for (var axis : AXES) {
            double[] dir = dir(axis.axis());
            double[] tip = project(m, dir[0] * AXIS_LENGTH, dir[1] * AXIS_LENGTH,
                    dir[2] * AXIS_LENGTH);
            g.setFill(axis.color());
            g.fillText(axisGlyph(axis.axis()), tip[0], tip[1]);
        }
    }

    private static String axisGlyph(int axis) {
        return switch (axis) {
            case 0 -> "x";
            case 1 -> "y";
            default -> "z";
        };
    }

    /** The unit direction of an axis in cube space. */
    private static double[] dir(int axis) {
        double[] d = {0, 0, 0};
        d[axis] = 1;
        return d;
    }

    /** Projects a cube-space point through rotation and perspective, then to pixels. */
    private double[] project(double[][] m, double nx, double ny, double nz) {
        // Coord3D applies the rotation as m11*cx + m21*cy + m31*cz (the first
        // index walks the rows of the matrix column-wise), so the widget must
        // read the matrix by columns too — reading it by rows would be the
        // transpose, mirroring every axis across the pane diagonal.
        double nzNeg = -nz;
        double rx = m[0][0] * nx + m[1][0] * ny + m[2][0] * nzNeg;
        double ry = m[0][1] * nx + m[1][1] * ny + m[2][1] * nzNeg;
        double rz = m[0][2] * nx + m[1][2] * ny + m[2][2] * nzNeg;
        double factor = DIST / (rz + DIST);
        double scale = size * SCALE_FACTOR;
        return new double[] {size / 2.0 + rx * factor * scale,
                size / 2.0 - ry * factor * scale};
    }
}
