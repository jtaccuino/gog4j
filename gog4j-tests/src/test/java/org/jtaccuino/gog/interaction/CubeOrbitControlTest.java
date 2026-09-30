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
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.event.EventType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots;
import org.jtaccuino.gog.examples.dflib.OrbitPlots;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The {@link CubeOrbitControl} mini-globe: it draws only the three axis lines
 * (no wireframe), rejects 2-D plots, and its drag rotates the plot through the
 * bound {@link Orbit3d} controller with the same configurable sensitivity.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class CubeOrbitControlTest {

    @Test
    void rejectsTwoDimensionalPlots() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = DiamondsPlots.createScatterPlot();
        onFxThread(() -> {
            assertThrows(IllegalArgumentException.class, () -> new CubeOrbitControl(plot));
            return null;
        });
    }

    @Test
    void drawsOnlyCanvasChildrenAndNoWireframeEdges() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var control = new CubeOrbitControl(plot);
            assertEquals(1, control.getChildren().size(),
                    "the globe must host exactly the drawing canvas");
            return null;
        });
    }

    @Test
    void dragRotatesThroughTheBoundOrbitAtItsSensitivity() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var orbit = Orbit3d.attach(plot);
            var control = new CubeOrbitControl(plot).bind(orbit);
            control.getOnMousePressed().handle(press(50, 50));
            control.getOnMouseDragged().handle(drag(100, 50));
            // The mini-globe drag follows the bound orbit's trackball gesture,
            // so all three angles move and the connected plot repaints.
            var coord = (Coord3D) plot.descriptor().coord();
            assertEquals(-29.1057100631564, coord.pitch(), 1e-9);
            assertEquals(-67.16091853076423, coord.roll(), 1e-9);
            assertEquals(-2.896791073974877, coord.yaw(), 1e-9);
            return null;
        });
    }

    @Test
    void unboundedDragUsesTheDefaultTrackballSensitivity() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var control = new CubeOrbitControl(plot);
            control.getOnMousePressed().handle(press(50, 50));
            control.getOnMouseDragged().handle(drag(100, 50));
            var coord = (Coord3D) plot.descriptor().coord();
            assertEquals(-29.1057100631564, coord.pitch(), 1e-9);
            assertEquals(-67.16091853076423, coord.roll(), 1e-9);
            assertEquals(-2.896791073974877, coord.yaw(), 1e-9);
            return null;
        });
    }

    private static MouseEvent press(double x, double y) {
        return mouse(MouseEvent.MOUSE_PRESSED, x, y);
    }

    private static MouseEvent drag(double x, double y) {
        return mouse(MouseEvent.MOUSE_DRAGGED, x, y);
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, double x, double y) {
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null);
    }
}
