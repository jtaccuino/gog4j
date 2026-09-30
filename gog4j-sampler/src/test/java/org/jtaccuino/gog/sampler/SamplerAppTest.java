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
package org.jtaccuino.gog.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.SourceTheme;
import org.jtaccuino.gog.sampler.ui.LoadProgressSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Headless smoke test that the {@link SamplerApp} wires its filter bar, list,
 * and result count without throwing.
 */
class SamplerAppTest {

    private Stage activeStage;

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
        // Tests close their Stages (so windows are not pinned for the rest of
        // the JVM); without this, closing the last window shuts the whole
        // toolkit down and every later Platform.runLater queues forever.
        Platform.setImplicitExit(false);
    }

    @AfterEach
    void settlePendingLoads() throws Exception {
        // Shown stages are held by the JavaFX window bookkeeping until they are
        // closed; leaving them up pins the whole sampler scene (every example
        // card, plot and canvas) for the rest of the JVM, which blows the test
        // heap. Tear the window down before the bookkeeping is left behind.
        if (activeStage != null) {
            var stage = activeStage;
            activeStage = null;
            // Fire-and-forget: the window only needs to leave the toolkit's
            // bookkeeping, nothing in a later test depends on the close.
            Platform.runLater(stage::close);
        }
        // start() builds visible cards that schedule debounced plot loads against
        // the shared LoadProgress singleton; drain them so no in-flight
        // beginLoad leaks into a later test class' counter assertions.
        LoadProgressSupport.settle();
        // Never let one test's persisted sampler state leak into the next.
        clearPrefs(throwawayNode());
        // The shared source theme is global state; restore the default so other
        // suites that assert on the light palette are not poisoned.
        onFx(() -> SourceTheme.CURRENT.set(SourceTheme.LIGHT));
    }

    private static void clearPrefs(Preferences node) {
        try {
            node.clear();
        } catch (BackingStoreException ignored) {
            // Best effort: the node may belong to a platform that defers writes.
        }
    }

    /** A throwaway preference node kept clean for every test. */
    private static Preferences throwawayNode() {
        return Preferences.userRoot().node("/org/jtaccuino/gog/sampler/test");
    }

    private static SamplerApp newApp() {
        clearPrefs(throwawayNode());
        return new SamplerApp(new SamplerPreferences(throwawayNode()));
    }

    /** A fresh stage whose closure is deferred to the {@link AfterEach} hook. */
    private Stage stage() {
        activeStage = new Stage();
        return activeStage;
    }

    private static void onFx(Runnable r) throws Exception {
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(60, TimeUnit.SECONDS), "FX task timed out");
    }

    @Test
    void startBuildsTheSamplerScene() throws Exception {
        var failure = new Throwable[1];
        onFx(() -> {
            try {
                var app = newApp();
                app.start(stage());
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("start() threw", failure[0]);
        }
    }

    @Test
    void resultCountLabelReportsTheFullCatalogue() throws Exception {
        var text = new String[1];
        onFx(() -> {
            var app = newApp();
            var stage = stage();
            app.start(stage);
            text[0] = app.filterBar().countLabel().getText();
        });
        assertTrue(text[0].matches("\\d+ of \\d+ examples"),
                "count label must read 'N of M examples', got '" + text[0] + "'");
        var matcher = Pattern.compile("(\\d+) of (\\d+)").matcher(text[0]);
        matcher.find();
        var shown = Integer.parseInt(matcher.group(1));
        var total = Integer.parseInt(matcher.group(2));
        assertEquals(total, shown, "unfiltered sampler must show the whole catalogue");
    }

    @Test
    void persistedRenderModeRestoredOnStart() throws Exception {
        var prefs = new SamplerPreferences(throwawayNode());
        prefs.saveRenderMode(RenderMode.FULL);
        var failure = new Throwable[1];
        onFx(() -> {
            try {
                var app = new SamplerApp(prefs);
                app.start(stage());
                assertEquals(RenderMode.FULL, app.filterBar().renderModeProperty().get(),
                        "restored render mode must match the persisted one");
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("render-mode restore failed", failure[0]);
        }
    }

    @Test
    void renderModeTogglePersistsTheSelection() throws Exception {
        var prefs = new SamplerPreferences(throwawayNode());
        var failure = new Throwable[1];
        onFx(() -> {
            try {
                var app = new SamplerApp(prefs);
                app.start(stage());
                app.filterBar().renderModeProperty().set(RenderMode.FULL);
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("render-mode toggle persistence failed", failure[0]);
        }
        assertEquals(RenderMode.FULL, prefs.renderMode(),
                "a FULL selection must be persisted");
    }

    @Test
    void persistedDarkThemeRestoredOnStart() throws Exception {
        var prefs = new SamplerPreferences(throwawayNode());
        prefs.saveDarkTheme(true);
        var failure = new Throwable[1];
        onFx(() -> {
            try {
                var app = new SamplerApp(prefs);
                app.start(stage());
                assertEquals(SourceTheme.DARK, SourceTheme.CURRENT.get(),
                        "restored theme must match the persisted one");
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("dark-theme restore failed", failure[0]);
        }
    }

    @Test
    void themeTogglePersistsTheSelection() throws Exception {
        var prefs = new SamplerPreferences(throwawayNode());
        var failure = new Throwable[1];
        onFx(() -> {
            try {
                var app = new SamplerApp(prefs);
                app.start(stage());
                app.filterBar().themeProperty().set(SourceTheme.DARK);
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] != null) {
            throw new AssertionError("theme toggle persistence failed", failure[0]);
        }
        assertTrue(prefs.darkTheme(), "a dark theme selection must be persisted");
    }
}
