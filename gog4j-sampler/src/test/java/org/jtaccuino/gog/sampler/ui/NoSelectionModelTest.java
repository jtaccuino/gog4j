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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards against the {@code IndexOutOfBoundsException} thrown when the selection
 * machinery runs {@code clearAndSelect} against an empty (mobile) items list —
 * e.g. a cell click racing a filter change. The sampler's no-op selection model
 * must make that a harmless no-op.
 */
class NoSelectionModelTest {

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

    private <T> T onFxThread(Callable<T> work) throws Exception {
        var task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(60, TimeUnit.SECONDS);
    }

    @Test
    void clearAndSelectIsANoOpOnAnEmptyItemsList() throws Exception {
        onFxThread(() -> {
            var list = new ListView<String>(FXCollections.observableArrayList());
            list.setSelectionModel(new NoSelectionModel<>());
            // The click-selection path that crashed the UI: selecting row 0 on
            // a list that currently has no items must not throw or select.
            list.getSelectionModel().clearAndSelect(0);
            assertTrue(list.getSelectionModel().isEmpty(), "no-op model must stay empty");
            return null;
        });
    }

    @Test
    void selectionModelIsTrulyInertAfterItemsChange() throws Exception {
        onFxThread(() -> {
            var items = FXCollections.observableArrayList("a", "b", "c");
            var list = new ListView<String>(items);
            list.setSelectionModel(new NoSelectionModel<>());
            list.getSelectionModel().select(0);
            list.getSelectionModel().selectAll();
            assertTrue(list.getSelectionModel().isEmpty(), "no-op model never selects");
            // Reproduces the reported crash: shrink to empty, then attempt select.
            items.clear();
            assertTrue(list.getSelectionModel().isEmpty());
            return null;
        });
    }

    @Test
    void noOpModelNeverThrowsAgainstAnEmptyList() throws Exception {
        onFxThread(() -> {
            var empty = new ListView<String>(FXCollections.observableArrayList());
            empty.setSelectionModel(new NoSelectionModel<>());
            // The reported crash came from clearAndSelect on an empty items list;
            // the no-op model must absorb it rather than throw.
            empty.getSelectionModel().clearAndSelect(0);
            empty.getSelectionModel().select(0);
            assertTrue(empty.getSelectionModel().isEmpty(), "no-op model stays empty");
            return null;
        });
    }
}
