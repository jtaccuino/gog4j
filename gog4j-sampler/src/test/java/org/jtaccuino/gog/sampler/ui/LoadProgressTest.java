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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the shared load-progress coordinator: counters mutate from any
 * thread and the observable property follows them after the next FX pulse.
 */
class LoadProgressTest {

    private LoadProgress progress;

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

    @BeforeEach
    void newProgress() {
        // A private instance rather than the shared singleton: the cards created
        // by other test classes keep mutating the singleton from loader threads,
        // so asserting on it here would make this unit test order-dependent.
        progress = new LoadProgress();
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

    @Test
    void pendingCountTracksLoadsAndQueuedInstalls() {
        assertEquals(0, progress.pendingCount());
        progress.beginLoad();
        assertEquals(1, progress.pendingCount());
        progress.enqueueInstall();
        assertEquals(2, progress.pendingCount());
        progress.endLoad();
        progress.dequeueInstall();
        assertEquals(0, progress.pendingCount());
    }

    @Test
    void pendingPropertyFollowsCountersAfterAPulse() throws Exception {
        progress.beginLoad();
        progress.enqueueInstall();
        onFx(() -> { });
        assertEquals(2, progress.pendingProperty().get());
        progress.endLoad();
        progress.dequeueInstall();
        onFx(() -> { });
        assertEquals(0, progress.pendingProperty().get());
    }
}
