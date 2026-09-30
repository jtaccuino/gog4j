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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/**
 * Drains in-flight sampler loads so a debounced {@link PlotCard} load started
 * by one test cannot leak {@code beginLoad}/{@code endLoad} calls into a later
 * test's assertions on the shared {@link LoadProgress} singleton.
 */
public final class LoadProgressSupport {

    private LoadProgressSupport() {
    }

    /**
     * Waits past the card load debounce and any in-flight factory work, then
     * resets the shared counter. Call in an {@code @AfterEach} of any test that
     * installed a card (which schedules a plot load).
     *
     * @throws Exception if the FX drain fails
     */
    public static void settle() throws Exception {
        // The debounce window (LOAD_SETTLE_MS) plus slack for the loader pool.
        Thread.sleep(250);
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                LoadProgress.get().resetForTest();
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(30, TimeUnit.SECONDS), "FX drain timed out");
    }
}
