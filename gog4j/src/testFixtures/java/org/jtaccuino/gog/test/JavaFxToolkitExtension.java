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
package org.jtaccuino.gog.test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Brings up the shared JavaFX toolkit once per JVM before a test class runs and
 * provides the {@link #onFxThread(Callable)} helper every render test used to
 * copy. Tests that cannot run without a toolkit are skipped via an
 * {@link Assumptions} failure, matching the headless CI behaviour.
 */
public class JavaFxToolkitExtension implements BeforeAllCallback {

    private static final AtomicBoolean TOOLKIT_UP = new AtomicBoolean();

    @Override
    public void beforeAll(ExtensionContext context) {
        startToolkit();
    }

    /**
     * {@return whether the JavaFX toolkit was successfully started}
     */
    public static boolean isToolkitUp() {
        return TOOLKIT_UP.get();
    }

    /**
     * Starts the JavaFX toolkit if it is not already running, skipping the
     * calling test class when no toolkit is available.
     */
    public static void startToolkit() {
        if (TOOLKIT_UP.get()) {
            return;
        }
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            Assumptions.assumeTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
            TOOLKIT_UP.set(true);
        } catch (IllegalStateException alreadyRunning) {
            TOOLKIT_UP.set(true);
        } catch (UnsupportedOperationException | InterruptedException noToolkit) {
            Assumptions.assumeTrue(false, "No JavaFX toolkit available: " + noToolkit.getMessage());
        }
    }

    /**
     * Runs {@code work} on the JavaFX application thread and returns its result.
     *
     * @param <T>  the result type
     * @param work the work to run
     * @return the result of {@code work}
     * @throws Exception if the work fails or the wait is interrupted
     */
    public static <T> T onFxThread(Callable<T> work) throws Exception {
        var task = new FutureTask<>(work);
        Platform.runLater(task);
        try {
            return task.get(120, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
