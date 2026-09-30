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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.interaction.Orbit3dOptions.Gesture;
import org.jtaccuino.gog.interaction.Orbit3dOptions.Readout;

/**
 * Drag-to-rotate interaction for a 3-D {@link Plot}.
 * <p>
 * Attaches to the plot's panes with {@link #attach(Plot, Orbit3dOptions)},
 * translating drag deltas into live {@link Coord3D} rotation. Each change
 * repaints the plot through the figure-wide dirty contract (mutate the
 * descriptor's coordinate system, then {@code markDirty()}), so the same
 * mechanism stays reusable for 2-D plots later. The rotation math is
 * separated into the package-private {@code applyModifier}/{@code
 * applyTrackball} cores so it can be unit-tested without a JavaFX
 * toolkit.
 * <p>
 * The {@code Orbit3d} instance returned by {@code attach} is the live
 * controller: {@link #onChange(Runnable)} registers a listener fired after
 * every angle change (a connected {@link CubeOrbitControl} can rebind itself
 * through it), and {@link #detach()} removes the handlers and readout again.
 */
public final class Orbit3d {

    /**
     * Node property key set on a plot while an {@link Orbit3d} controller is
     * attached. Hosts that otherwise consume mouse presses on their children
     * (such as the sampler's virtualized list cells) can query
     * {@link #isAttached(Node)} to let the drag gesture through.
     */
    public static final String ATTACHED_PROPERTY = "org.jtaccuino.gog.interaction.orbit3d";

    private static final String POPUP_STYLE =
            "-fx-background-color: rgba(30, 30, 30, 0.88);"
            + "-fx-text-fill: #ffffff;"
            + "-fx-padding: 6 12 6 12;"
            + "-fx-background-radius: 5;"
            + "-fx-font-size: 11px;"
            + "-fx-font-family: 'System';"
            + "-fx-font-weight: bold;";
    private static final String OVERLAY_STYLE =
            "-fx-background-color: rgba(30, 30, 30, 0.70);"
            + "-fx-text-fill: #ffffff;"
            + "-fx-padding: 4 10 4 10;"
            + "-fx-background-radius: 5;"
            + "-fx-font-size: 11px;"
            + "-fx-font-family: 'System';";

    private final Plot<?> plot;
    private final Coord3D coord;
    private final Orbit3dOptions options;
    private final List<Runnable> changeListeners = new ArrayList<>();

    private Label readout;
    private double pressX;
    private double pressY;
    private boolean dragging;

    private Orbit3d(Plot<?> plot, Orbit3dOptions options) {
        if (!(plot.descriptor().coord() instanceof Coord3D coord)) {
            throw new IllegalArgumentException(
                    "Orbit3d needs a 3-D coordinate system; use plot3d()/ggplot3d() or bind a coord3d()");
        }
        this.plot = plot;
        this.coord = coord;
        this.options = options;
        plot.getProperties().put(ATTACHED_PROPERTY, this);
        installHandlers();
        if (options.readout() != Readout.NONE) {
            installReadout();
        }
    }

    /**
     * Attaches a drag-to-rotate controller to a 3-D plot with the default
     * {@link Orbit3dOptions}.
     *
     * @param plot the 3-D plot to rotate
     * @return the live controller
     */
    public static Orbit3d attach(Plot<?> plot) {
        return attach(plot, Orbit3dOptions.defaults());
    }

    /**
     * Attaches a drag-to-rotate controller to a 3-D plot.
     *
     * @param plot    the 3-D plot to rotate
     * @param options the gesture and readout configuration
     * @return the live controller
     * @throws IllegalArgumentException if the plot does not use a 3-D
     *         coordinate system
     */
    public static Orbit3d attach(Plot<?> plot, Orbit3dOptions options) {
        return new Orbit3d(plot, options);
    }

    /**
     * Whether an {@link Orbit3d} controller is attached to the given node or any
     * of its ancestors. Hosts can use this to avoid consuming the mouse press
     * that begins the drag gesture.
     *
     * @param node the node to test, may be {@code null}
     * @return {@code true} when an attached orbit controller is in the chain
     */
    public static boolean isAttached(Node node) {
        for (var n = node; n != null; n = n.getParent()) {
            if (n.getProperties().containsKey(ATTACHED_PROPERTY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The plot this controller rotates.
     *
     * @return the owned plot
     */
    public Plot<?> plot() {
        return plot;
    }

    /**
     * The coordinate system this controller mutates.
     *
     * @return the rotation target
     */
    public Coord3D coord() {
        return coord;
    }

    /**
     * The options this controller was attached with.
     *
     * @return the active configuration
     */
    public Orbit3dOptions options() {
        return options;
    }

    /**
     * Registers a listener fired after every angle change, whether the drag
     * originated on the plot or from a connected controller (e.g. a
     * {@link CubeOrbitControl}).
     *
     * @param listener the listener to run after each rotation
     * @return this controller for fluid chaining
     */
    public Orbit3d onChange(Runnable listener) {
        changeListeners.add(listener);
        return this;
    }

    /**
     * Programmatically rotates the cube by the given deltas, as if dragged.
     *
     * @param dPitch the pitch delta in degrees
     * @param dRoll  the roll delta in degrees
     * @param dYaw   the yaw delta in degrees
     * @return this controller for fluid chaining
     */
    public Orbit3d rotate(double dPitch, double dRoll, double dYaw) {
        coord.pitch(coord.pitch() + dPitch);
        coord.roll(coord.roll() + dRoll);
        coord.yaw(coord.yaw() + dYaw);
        notifyAnglesChanged();
        return this;
    }

    /**
     * Removes this controller's mouse handlers and readout from the plot.
     *
     * @return this controller
     */
    public Orbit3d detach() {
        plot.getProperties().remove(ATTACHED_PROPERTY);
        plot.setOnMousePressed(null);
        plot.setOnMouseDragged(null);
        plot.setOnMouseReleased(null);
        if (readout != null) {
            plot.getChildren().remove(readout);
            readout = null;
        }
        return this;
    }

    /**
     * Applies a {@link Gesture#MODIFIER} drag delta to the coordinate system.
     * Horizontal drag rotates the view axis (yaw), vertical drag tips the
     * elevation (pitch); with {@code roll} pressed the modifier maps to roll
     * instead.
     *
     * @param coord            the coordinate system to rotate
     * @param dX               the horizontal drag delta in pixels
     * @param dY               the vertical drag delta in pixels
     * @param rollPressed      whether the roll modifier is held
     * @param degreesPerPixel  the rotation sensitivity
     */
    static void applyModifier(Coord3D coord, double dX, double dY, boolean rollPressed,
            double degreesPerPixel) {
        if (rollPressed) {
            coord.roll(coord.roll() + dX * degreesPerPixel);
        } else {
            coord.yaw(coord.yaw() + dX * degreesPerPixel);
            coord.pitch(coord.pitch() + dY * degreesPerPixel);
        }
    }

    /**
     * Applies a {@link Gesture#TRACKBALL} drag to the coordinate system. The
     * cursor moves a point on an invisible sphere centred on the plot, so the
     * cube spins about the axis swept between the press and current points —
     * a combined, direct "grab and turn" motion that covers all three angles
     * in one continuous drag.
     *
     * @param coord           the coordinate system to rotate
     * @param cx              the pane centre x
     * @param cy              the pane centre y
     * @param startX          the press x
     * @param startY          the press y
     * @param curX            the current x
     * @param curY            the current y
     * @param sensitivity     the rotation scale: 1.0 maps the swept arc onto
     *                        the cube one-to-one
     */
    static void applyTrackball(Coord3D coord, double cx, double cy,
            double startX, double startY, double curX, double curY,
            double sensitivity) {
        double radius = Math.max(1.0, Math.min(cx, cy));
        double[] p0 = spherePoint(startX, startY, cx, cy, radius);
        double[] p1 = spherePoint(curX, curY, cx, cy, radius);
        double[] axis = cross(p0, p1);
        double s = length(axis);
        if (s < 1e-12) {
            return;
        }
        axis = scale(axis, 1.0 / s);
        double angle = 2.0 * Math.atan2(s, dot(p0, p1) + 1.0) * sensitivity;
        applyAxisRotation(coord, axis, angle);
    }

    /** Maps a pane point onto the unit virtual sphere (screen y grows down). */
    private static double[] spherePoint(double x, double y, double cx, double cy,
            double radius) {
        double vx = (x - cx) / radius;
        double vy = (y - cy) / radius;
        double d = vx * vx + vy * vy;
        if (d <= 1.0) {
            return new double[] {vx, vy, Math.sqrt(1.0 - d)};
        }
        double n = Math.sqrt(d);
        return new double[] {vx / n, vy / n, 0.0};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    private static double[] scale(double[] a, double f) {
        return new double[] {a[0] * f, a[1] * f, a[2] * f};
    }

    /**
     * Applies a world-space rotation to the coordinate system: builds its
     * rotation matrix ({@code M = Rz(yaw)·Ry(pitch)·Rx(roll)} applied
     * row-vector wise, exactly as {@code Coord3D} does), rotates it by
     * the given axis-angle, and decomposes the result back into
     * pitch/roll/yaw.
     */
    private static void applyAxisRotation(Coord3D coord, double[] axis, double angle) {
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

        double[][] dR = axisAngle(axis, angle);
        // New row-convention matrix = M · dR^T (the world-space rotation applied
        // to the cube), matching how Coord3D applies its own M.
        double[][] n = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                n[i][j] = m[i][0] * dR[j][0] + m[i][1] * dR[j][1] + m[i][2] * dR[j][2];
            }
        }

        double pitch = Math.asin(clamp(-n[2][0]));
        double roll;
        double yaw;
        if (Math.abs(Math.cos(pitch)) > 1e-9) {
            roll = Math.atan2(n[2][1], n[2][2]);
            yaw = Math.atan2(n[1][0], n[0][0]);
        } else {
            roll = n[2][0] < 0 ? Math.atan2(n[1][2], n[1][1])
                    : Math.atan2(-n[1][2], n[1][1]);
            yaw = 0.0;
        }
        coord.pitch(Math.toDegrees(pitch));
        coord.roll(Math.toDegrees(roll));
        coord.yaw(Math.toDegrees(yaw));
    }

    private static double[][] axisAngle(double[] axis, double angle) {
        double x = axis[0], y = axis[1], z = axis[2];
        double c = Math.cos(angle), s = Math.sin(angle), t = 1 - c;
        return new double[][] {
            {t * x * x + c, t * x * y - s * z, t * x * z + s * y},
            {t * x * y + s * z, t * y * y + c, t * y * z - s * x},
            {t * x * z - s * y, t * y * z + s * x, t * z * z + c}
        };
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }

    /**
     * Fires the post-change bookkeeping shared by every rotation source: repaint
     * the plot, refresh the readout, and notify connected controllers.
     */
    void notifyAnglesChanged() {
        plot.markDirty();
        updateReadout();
        changeListeners.forEach(Runnable::run);
    }

    private void installHandlers() {
        plot.setOnMousePressed(e -> {
            this.pressX = e.getX();
            this.pressY = e.getY();
            this.dragging = true;
            if (options.readout() == Readout.POPUP) {
                movePopup(e.getX(), e.getY());
                readout.setVisible(true);
            }
        });

        plot.setOnMouseDragged(e -> {
            if (!this.dragging) {
                return;
            }
            double dx = e.getX() - pressX;
            double dy = e.getY() - pressY;
            double degreesPerPixel = options.degreesPerPixel();
            if (options.gesture() == Gesture.TRACKBALL) {
                double scale = degreesPerPixel / Orbit3dOptions.DEFAULT_DEGREES_PER_PIXEL;
                var center = coord.project(0.5, 0.5, 0.5);
                applyTrackball(coord, center.sx(), center.sy(),
                        pressX, pressY, e.getX(), e.getY(), scale);
            } else {
                applyModifier(coord, dx, dy, rollModifier(e), degreesPerPixel);
            }
            if (options.readout() == Readout.POPUP) {
                movePopup(e.getX(), e.getY());
            }
            notifyAnglesChanged();
        });

        plot.setOnMouseReleased(e -> {
            dragging = false;
            if (options.readout() == Readout.POPUP) {
                readout.setVisible(false);
            }
        });
    }

    private static boolean rollModifier(MouseEvent e) {
        return e.isAltDown() || e.getButton() == MouseButton.SECONDARY;
    }

    private void installReadout() {
        readout = new Label();
        readout.setMouseTransparent(true);
        readout.setFont(Font.font("System", FontWeight.BOLD, 11));
        readout.setStyle(options.readout() == Readout.OVERLAY ? OVERLAY_STYLE : POPUP_STYLE);
        readout.setVisible(options.readout() == Readout.OVERLAY);
        readout.setLayoutX(options.readout() == Readout.OVERLAY ? 10 : 0);
        plot.getChildren().add(readout);
        readout.toFront();
        updateReadout();
        if (options.readout() == Readout.OVERLAY) {
            plot.heightProperty().addListener((obs, o, n) -> pinOverlay());
            pinOverlay();
        }
    }

    private void pinOverlay() {
        if (readout == null) {
            return;
        }
        readout.setLayoutY(Math.max(4.0, plot.getHeight() - 22.0));
    }

    private void movePopup(double x, double y) {
        if (readout != null) {
            readout.setLayoutX(x + 12);
            readout.setLayoutY(y + 15);
        }
    }

    private void updateReadout() {
        if (readout == null || options.readout() == Readout.NONE) {
            return;
        }
        readout.setText(String.format(Locale.ROOT, "pitch %5.1f°  roll %5.1f°  yaw %5.1f°",
                coord.pitch(), coord.roll(), coord.yaw()));
    }
}
