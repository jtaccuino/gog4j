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
package org.jtaccuino.gog.sampler.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jtaccuino.gog.controls.PlotCard;
import org.jtaccuino.gog.sampler.meta.TagKind;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the cell's primary-press consumption does not swallow presses that
 * target interactive controls (the source drawer's nudge handle).
 */
class SampleListCellTest {

    @BeforeAll
    static void startToolkit() {
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assumeTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        } catch (IllegalStateException alreadyRunning) {
            // already up
        } catch (UnsupportedOperationException | InterruptedException noToolkit) {
            assumeTrue(false, "No JavaFX toolkit available: " + noToolkit.getMessage());
        }
    }

    private static void onFx(Runnable r) throws Exception {
        // Assertions inside the FX task must reach the test thread: the FX
        // thread's uncaught-exception handler would otherwise swallow them.
        var error = new Throwable[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(60, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw new AssertionError(error[0]);
        }
    }

    @AfterEach
    void settlePendingLoads() throws Exception {
        // updateItem schedules a debounced plot load against the shared
        // LoadProgress singleton. Drain it so no in-flight beginLoad leaks into
        // a later test class' counter assertions.
        LoadProgressSupport.settle();
    }

    private static SamplerExample example() {
        var tag = new Tag("geom:point", "Point", TagKind.GEOM);
        return new SamplerExample("Title", "desc", "createTitle",
                () -> new Pane(), List.of(tag), "source");
    }

    @Test
    void pressOnNudgeIsNotConsumedByTheCell() throws Exception {
        onFx(() -> {
            var cell = cellInScene();
            var card = (PlotCard) cell.getGraphic();
            var button = findButton(card);
            assertTrue(button != null, "card must expose the drawer nudge button");

            var consumed = new boolean[1];
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED,
                    e -> consumed[0] = e.isConsumed());
            // Node.fireEvent dispatches a copy, so the consumed state must be
            // observed from a filter registered after the cell's own one.
            button.fireEvent(press());
            assertFalse(consumed[0],
                    "a press targeting the drawer nudge must reach the button");
        });
    }

    @Test
    void pressOnCardBodyIsConsumedToBlockSelection() throws Exception {
        onFx(() -> {
            var cell = cellInScene();
            var card = (PlotCard) cell.getGraphic();

            var consumed = new boolean[1];
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED,
                    e -> consumed[0] = e.isConsumed());
            card.fireEvent(press());
            assertTrue(consumed[0],
                    "a press on the card body must be consumed to block selection");
        });
    }

    private static MouseEvent press() {
        return new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                MouseButton.PRIMARY, 1, false, false, false, false,
                true, false, false, false, true, false, null);
    }

    /**
     * A cell attached to a scene so its default skin parents the card graphic:
     * only then does the cell sit on the event dispatch chain of presses that
     * land on the card (or its buttons).
     */
    private static SampleListCell cellInScene() {
        var cell = new SampleListCell(tag -> { });
        var root = new StackPane(cell);
        new Scene(root);
        root.applyCss();
        root.layout();
        cell.updateItem(example(), false);
        root.applyCss();
        root.layout();
        return cell;
    }

    private static Button findButton(Parent root) {
        for (var node : root.getChildrenUnmodifiable()) {
            if (node instanceof Button button) {
                return button;
            }
            if (node instanceof Parent parent) {
                var found = findButton(parent);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
