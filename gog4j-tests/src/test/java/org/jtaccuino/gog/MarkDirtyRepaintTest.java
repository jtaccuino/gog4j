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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.examples.dflib.OrbitPlots;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The figure-wide dirty-repaint contract: mutating the descriptor's state (the
 * cube view angles here) must <em>not</em> repaint the rendered canvas until
 * {@link GgFigurePane#markDirty()} is called, and {@code markDirty()} must then
 * rebuild the canvas with the new projection. The snapshot comparison proves a
 * real pixel change rather than just a flag flip.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class MarkDirtyRepaintTest {

    private static final int WIDTH = 520;
    private static final int HEIGHT = 420;

    @Test
    void canvasIsStableUntilMarkDirtyRepaints() throws Exception {
        Assumptions.assumeTrue(JavaFxToolkitExtension.isToolkitUp(),
                "JavaFX toolkit unavailable");
        var plot = OrbitPlots.createSpherePoints();
        onFxThread(() -> {
            var holder = new StackPane(plot);
            holder.setPrefSize(WIDTH, HEIGHT);
            new Scene(holder, WIDTH, HEIGHT);
            holder.applyCss();
            holder.layout();
            plot.redraw();

            var before = snapshot(plot);

            // Mutating the view angles alone must not touch the canvas.
            ((Coord3D) plot.descriptor().coord()).pitch(35).yaw(-55).roll(-75);
            var afterMutation = snapshot(plot);
            assertTrue(samePixels(before, afterMutation),
                    "canvas must stay stable until markDirty() is called");

            // markDirty() repaints with the new projection.
            plot.markDirty();
            var afterRepaint = snapshot(plot);
            assertFalse(samePixels(afterMutation, afterRepaint),
                    "markDirty() must rebuild the canvas with the rotated view");
            return null;
        });
    }

    private static WritableImage snapshot(Plot<?> plot) {
        var params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        var image = new WritableImage(WIDTH, HEIGHT);
        plot.snapshot(params, image);
        return image;
    }

    private static boolean samePixels(WritableImage a, WritableImage b) {
        PixelReader ra = a.getPixelReader();
        PixelReader rb = b.getPixelReader();
        int step = 2;
        for (int y = 0; y < HEIGHT; y += step) {
            for (int x = 0; x < WIDTH; x += step) {
                if (ra.getArgb(x, y) != rb.getArgb(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
