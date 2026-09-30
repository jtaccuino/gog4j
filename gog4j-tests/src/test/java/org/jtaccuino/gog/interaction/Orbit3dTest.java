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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots;
import org.jtaccuino.gog.examples.dflib.OrbitPlots;
import org.jtaccuino.gog.interaction.Orbit3dOptions.Gesture;
import org.jtaccuino.gog.interaction.Orbit3dOptions.Readout;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The {@code Orbit3d} controller: the gesture core is pure math over a
 * {@link Coord3D} and needs no toolkit, while attaching to a real plot and
 * driving its synthetic mouse handlers is done on the FX thread so the repaint
 * contract (mark dirty, rebuild the canvas, refresh the readout) is exercised
 * for real.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class Orbit3dTest {

    // --- gesture core (no toolkit) ----------------------------------------

    @Test
    void modifierDragYawsAndPitches() {
        var coord = new Coord3D();
        Orbit3d.applyModifier(coord, 100, 20, false, 0.15);
        assertEquals(-15.0, coord.yaw(), 1e-9);
        assertEquals(3.0, coord.pitch(), 1e-9);
        assertEquals(-60.0, coord.roll(), 1e-9);
    }

    @Test
    void modifierWithRollPressedRolls() {
        var coord = new Coord3D();
        Orbit3d.applyModifier(coord, 40, 0, true, 0.15);
        assertEquals(-54.0, coord.roll(), 1e-9);
        assertEquals(-30.0, coord.yaw(), 1e-9);
        assertEquals(0.0, coord.pitch(), 1e-9);
    }

    @Test
    void modifierSensitivityIsCustomizable() {
        var coord = new Coord3D();
        Orbit3d.applyModifier(coord, 100, 0, false, 0.6);
        assertEquals(30.0, coord.yaw(), 1e-9);
    }

    @Test
    void trackballVerticalCenterDragRollsTheCube() {
        var coord = new Coord3D();
        Orbit3d.applyTrackball(coord, 50, 50, 50, 25, 50, 75, 1.0);
        assertEquals(0.0, coord.roll(), 1e-9);
        assertEquals(-30.0, coord.yaw(), 1e-9);
        assertEquals(0.0, coord.pitch(), 1e-9);
    }

    @Test
    void trackballScaleShrinksTheRotation() {
        var coord = new Coord3D();
        Orbit3d.applyTrackball(coord, 50, 50, 50, 25, 50, 75, 0.5);
        assertEquals(-30.0, coord.roll(), 1e-9);
        assertEquals(-30.0, coord.yaw(), 1e-9);
    }

    @Test
    void trackballHorizontalDragCombinesAllAngles() {
        var coord = new Coord3D();
        Orbit3d.applyTrackball(coord, 50, 50, 75, 50, 25, 50, 1.0);
        assertEquals(25.658906273255287, coord.pitch(), 1e-9);
        assertEquals(-73.89788624801398, coord.roll(), 1e-9);
        assertEquals(-86.3099324740202, coord.yaw(), 1e-9);
    }

    // --- attach + handlers (FX-threaded) ----------------------------------

    @Test
    void attachRejectsTwoDimensionalPlots() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = DiamondsPlots.createScatterPlot();
        onFxThread(() -> {
            assertThrows(IllegalArgumentException.class, () -> Orbit3d.attach(plot));
            return null;
        });
    }

    @Test
    void isAttachedTracksTheControllerLifecycle() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            assertFalse(Orbit3d.isAttached(plot), "a bare plot is not orbit-attached");
            var orbit = Orbit3d.attach(plot);
            assertTrue(Orbit3d.isAttached(plot), "an attached plot reports attached");
            // A host consuming presses queries the event target, which may be a
            // child of the plot (e.g. the canvas); the chain must resolve.
            var canvas = plot.getChildren().stream()
                    .filter(n -> n instanceof javafx.scene.canvas.Canvas)
                    .findFirst().orElse(plot);
            assertTrue(Orbit3d.isAttached(canvas),
                    "a descendant of the attached plot must resolve as attached");
            orbit.detach();
            assertFalse(Orbit3d.isAttached(plot), "detaching clears the marker");
            return null;
        });
    }

    @Test
    void hostFilterCanLetOrbitDragsThrough() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var holder = layOut(plot, 420, 360);
            var orbit = Orbit3d.attach(plot);
            var canvas = plot.getChildren().stream()
                    .filter(n -> n instanceof javafx.scene.canvas.Canvas)
                    .findFirst().orElse(plot);
            // Model the sampler cell's filter: consume a primary press unless
            // the target is orbit-attached. A press over the canvas (a plot
            // descendant) must not be consumed, so it reaches the plot handler.
            var consumed = new boolean[1];
            holder.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (e.getButton() == MouseButton.PRIMARY
                        && !Orbit3d.isAttached(
                                e.getTarget() instanceof javafx.scene.Node n ? n : null)) {
                    consumed[0] = true;
                    e.consume();
                }
            });
            double p0 = orbit.coord().pitch();
            javafx.event.Event.fireEvent(canvas, press(100, 100));
            assertFalse(consumed[0], "an orbit-attached press must not be consumed");
            plot.getOnMouseDragged().handle(drag(300, 260));
            relayout(holder);
            assertNotEquals(p0, orbit.coord().pitch(), 1e-9,
                    "the drag after a non-consumed press must rotate the plot");
            return null;
        });
    }

    @Test
    void dragRotatesPlotAndUpdatesReadout() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        var readout = new Object() {
            Label label;
        };
        onFxThread(() -> {
            var holder = layOut(plot, 420, 360);
            var orbit = Orbit3d.attach(plot, Orbit3dOptions.defaults()
                    .gesture(Gesture.MODIFIER).degreesPerPixel(0.15).readout(Readout.POPUP));
            plot.getOnMousePressed().handle(press(100, 100));
            plot.getOnMouseDragged().handle(drag(200, 120));
            relayout(holder);
            var label = findReadout(plot);
            readout.label = label;
            assertTrue(label != null, "popup readout must be installed");
            assertTrue(label.isVisible(), "popup must show while dragging");
            assertTrue(label.prefWidth(-1) > 0 && label.prefHeight(-1) > 0,
                    "the readout label must have a real size");
            assertTrue(label.getText().startsWith("pitch"), "readout must start with pitch");
            assertTrue(label.getText().contains("yaw"), "readout must include yaw");
            assertTrue(plot.getChildren().indexOf(label) > 0,
                    "readout must stay above the rebuilt canvas");
            assertEquals(3.0, orbit.coord().pitch(), 1e-9);
            assertEquals(-15.0, orbit.coord().yaw(), 1e-9);
            orbit.detach();
            return null;
        });
        assertTrue(readout.label != null);
    }

    @Test
    void defaultTrackballDragRotatesPlot() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var holder = layOut(plot, 420, 360);
            plot.redraw();

            var orbit = Orbit3d.attach(plot);
            assertTrue(orbit.options().gesture() == Gesture.TRACKBALL,
                    "the default gesture must be the trackball");
            assertTrue(orbit.options().readout() == Readout.OVERLAY,
                    "the default readout must be the always-visible overlay");

            plot.getOnMousePressed().handle(press(100, 100));
            plot.getOnMouseDragged().handle(drag(200, 120));
            relayout(holder);
            // The trackball sphere is centred on the cube centre (projected
            // origin), which must sit inside the panel and track it.
            var center = ((Coord3D) plot.descriptor().coord()).project(0.5, 0.5, 0.5);
            assertTrue(center.sx() >= 0 && center.sx() <= 420 && center.sy() >= 0
                            && center.sy() <= 360,
                    "the projected origin must land inside the panel, got ("
                            + center.sx() + "," + center.sy() + ")");
            double p0 = orbit.coord().pitch(), r0 = orbit.coord().roll(),
                    y0 = orbit.coord().yaw();
            assertNotEquals(0.0, p0, 1e-9);
            assertNotEquals(-30.0, y0, 1e-9);
            assertNotEquals(-60.0, r0, 1e-9);

            // A vertical drag through the cube centre is a pure roll: it keeps
            // the current pitch/yaw and only spins the cube about its centre.
            plot.getOnMousePressed().handle(press(center.sx(), center.sy() - 25));
            plot.getOnMouseDragged().handle(drag(center.sx(), center.sy() + 25));
            relayout(holder);
            assertEquals(p0, orbit.coord().pitch(), 1e-9,
                    "a centre-line drag must not pitch the cube");
            assertEquals(y0, orbit.coord().yaw(), 1e-9,
                    "a centre-line drag must not yaw the cube");
            assertNotEquals(r0, orbit.coord().roll(), 0.5,
                    "a centre-line drag must roll the cube about its own centre");

            var label = findReadout(plot);
            assertTrue(label != null, "the overlay readout must be installed");
            assertTrue(label.isVisible(), "the overlay must be visible without dragging");
            assertTrue(label.prefWidth(-1) > 0, "the overlay readout must have a real size");
            orbit.detach();
            return null;
        });
    }

    @Test
    void altDragRolls() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var orbit = Orbit3d.attach(plot, Orbit3dOptions.defaults()
                    .gesture(Gesture.MODIFIER).degreesPerPixel(0.15));
            plot.getOnMousePressed().handle(press(100, 100));
            plot.getOnMouseDragged().handle(drag(140, 100, true));
            assertEquals(-54.0, orbit.coord().roll(), 1e-9);
            assertEquals(-30.0, orbit.coord().yaw(), 1e-9);
            assertEquals(0.0, orbit.coord().pitch(), 1e-9);
            orbit.detach();
            return null;
        });
    }

    @Test
    void detachRemovesHandlersAndReadout() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var orbit = Orbit3d.attach(plot, Orbit3dOptions.controlled());
            assertTrue(findReadout(plot) != null, "overlay readout must be installed");
            assertTrue(findReadout(plot).isVisible(), "overlay must be visible immediately");
            orbit.detach();
            assertNull(plot.getOnMousePressed());
            assertNull(plot.getOnMouseDragged());
            assertNull(plot.getOnMouseReleased());
            assertTrue(findReadout(plot) == null, "readout must be removed on detach");
            return null;
        });
    }

    // --- helpers ----------------------------------------------------------

    private static Label findReadout(Plot<?> plot) {
        for (var child : plot.getChildren()) {
            if (child instanceof Label label && label.getText().startsWith("pitch")) {
                return label;
            }
        }
        return null;
    }

    private static MouseEvent press(double x, double y) {
        return mouse(MouseEvent.MOUSE_PRESSED, x, y, false);
    }

    private static StackPane layOut(Plot<?> plot, double width, double height) {
        var holder = new StackPane(plot);
        holder.setPrefSize(width, height);
        new Scene(holder, width, height);
        holder.applyCss();
        holder.layout();
        return holder;
    }

    private static void relayout(StackPane holder) {
        holder.applyCss();
        holder.layout();
    }

    private static MouseEvent drag(double x, double y) {
        return drag(x, y, false);
    }

    private static MouseEvent drag(double x, double y, boolean altDown) {
        return mouse(MouseEvent.MOUSE_DRAGGED, x, y, altDown);
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, double x, double y,
            boolean altDown) {
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, altDown, false, true, false, false, false, false, false, null);
    }
}
