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
package org.jtaccuino.gog.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.GgFigurePane;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.ui.LoadProgressSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that each plot card reports the time its plot rendering took. The
 * render happens when the plot is laid out, so the card must be on a scene.
 */
class PlotCardTest {

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
        // setExample schedules a debounced plot load against the shared
        // LoadProgress singleton; drain it so no in-flight beginLoad leaks into
        // a later test class' counter assertions.
        LoadProgressSupport.settle();
    }

    @Test
    void successfulRenderShowsARenderingDuration() throws Throwable {
        var example = new SamplerExample("Timed", "desc", "factory",
                () -> Ggplot.ggplot(Aes.aes()), List.of(), "");

        var card = new PlotCard[1];
        onFx(() -> {
            var c = new PlotCard();
            c.setPrefSize(500, 600);
            var root = new StackPane(c);
            root.setPrefSize(500, 600);
            new Scene(root);
            card[0] = c;
        });

        onFx(() -> card[0].setSpec(example.toSpec()));

        // Drive the render synchronously on the FX thread so the plot records a
        // real render duration, then wait for the timing label to carry it.
        var failure = new Throwable[1];
        var figureRef = new GgFigurePane[1];
        Label timing = null;
        for (int i = 0; i < 40 && timing == null && failure[0] == null; i++) {
            var labelRef = new Label[1];
            onFx(() -> {
                try {
                    // Card children: 0 = content column, 1 = source drawer.
                    // Content children: 0 = title, 1 = tag chips, 2 = plot pane,
                    // 3 = footer (timing row), 4 = description.
                    var content = (Pane) card[0].getChildren().get(0);
                    var plotPane = (Pane) content.getChildren().get(2);
                    for (var node : plotPane.getChildren()) {
                        if (node instanceof GgFigurePane fig) {
                            figureRef[0] = fig;
                            var canvas = new Canvas(500, 520);
                            fig.renderTo(new FxDrawSurface(
                                    canvas.getGraphicsContext2D()), 500, 520);
                        }
                    }
                    labelRef[0] = findTimingLabel(content.getChildren().get(3));
                } catch (Throwable t) {
                    failure[0] = t;
                }
            });
            timing = labelRef[0];
            if (timing == null) {
                Thread.sleep(50);
            }
        }
        if (failure[0] != null) {
            throw failure[0];
        }
        assertTrue(figureRef[0] != null && figureRef[0].lastRenderNanos() > 0,
                "renderTo must record a render duration");
        assertTrue(timing != null, "timing label did not appear after a successful render");
        assertTrue(timing.getText().matches("[0-9.]+ (ms|s)"),
                "expected a rendering duration, got '" + timing.getText() + "'");
    }

    @Test
    void setExamplePopulatesTheSourceDrawer() throws Exception {
        var src = """
                public static Plot<DataFrame> createMyPlot() {
                    var df = MpgDatasets.loadMpg();
                    return ggplot(df, aes().x("displ").y("hwy"));
                }
                """;
        var example = new SamplerExample("Source", "desc", "createMyPlot",
                () -> new Pane(), List.of(), src);

        var card = new PlotCard[1];
        onFx(() -> {
            var c = new PlotCard();
            c.setPrefSize(500, 600);
            new Scene(new StackPane(c));
            card[0] = c;
            c.setSpec(example.toSpec());
        });

        onFx(() -> {
            var drawer = card[0].sourceDrawer();
            assertTrue(drawer.sourceArea().getText().startsWith("var df = MpgDatasets.loadMpg();"),
                    "card's drawer must carry the example's dedented source");
            assertFalse(drawer.isOpen(), "setting an example must leave the drawer collapsed");
        });
    }

    @Test
    void changingRenderModeRestartsTheLoadForTheCurrentExample() throws Exception {
        var example = new SamplerExample("Timed", "desc", "factory",
                () -> Ggplot.ggplot(Aes.aes()), List.of(), "");

        var card = new PlotCard[1];
        onFx(() -> {
            var c = new PlotCard();
            c.setPrefSize(500, 600);
            var root = new StackPane(c);
            root.setPrefSize(500, 600);
            new Scene(root);
            card[0] = c;
            c.setSpec(example.toSpec());
        });

        // Let the debounced load start and record its generation.
        Thread.sleep(250);
        onFx(() -> {
            assertEquals(RenderMode.FAST, card[0].renderMode(),
                    "card must start in FAST mode");
            card[0].setRenderMode(RenderMode.FULL);
            assertEquals(RenderMode.FULL, card[0].renderMode(),
                    "the card must track the new render mode");
            // The mode flip must schedule a fresh load: the loading placeholder
            // replaces the (already installed or pending) figure pane.
            var content = (Parent) card[0].getChildren().get(0);
            var plotPane = content.getChildrenUnmodifiable().get(2);
            assertTrue(plotPane instanceof Pane,
                    "content child 2 must still be the plot pane");
            assertTrue(findLoadingLabel(card[0]) != null,
                    "a mode flip must put the card back into the loading phase");
        });
    }

    @Test
    void offscreenExampleDoesNotLoadUntilVisible() throws Exception {
        var calls = new AtomicInteger();
        var example = new SamplerExample("Lazy", "desc", "factory",
                () -> {
                    calls.incrementAndGet();
                    return new Pane();
                }, List.of(), "");

        var card = new PlotCard[1];
        onFx(() -> {
            var c = new PlotCard();
            c.setPrefSize(500, 600);
            var root = new StackPane(c);
            root.setPrefSize(500, 600);
            new Scene(root);
            card[0] = c;
            c.setViewportVisible(false);
            c.setSpec(example.toSpec());
        });

        // Let the debounce window pass; an off-screen card must never start the
        // factory or install anything, but keeps its chrome (title/source).
        Thread.sleep(400);
        onFx(() -> {
            assertEquals(0, calls.get(),
                    "an off-screen card must not invoke the plot factory");
            var plotPane = plotPaneOf(card[0]);
            assertTrue(plotPane.getChildren().isEmpty(),
                    "an off-screen card must not install a plot or loading placeholder");
            var content = (Parent) card[0].getChildren().get(0);
            var title = (Label) content.getChildrenUnmodifiable().get(0);
            assertEquals("Lazy", title.getText(),
                    "the off-screen card keeps its chrome (title) while released");
        });

        // Scroll into view: the debounced load starts and installs the figure.
        onFx(() -> card[0].setViewportVisible(true));
        awaitInstalled(card);
        onFx(() -> assertEquals(1, calls.get(), "the factory must run exactly once"));
    }

    @Test
    void leavingTheViewportReleasesTheInstalledFigure() throws Exception {
        var calls = new AtomicInteger();
        var example = new SamplerExample("Lazy", "desc", "factory",
                () -> {
                    calls.incrementAndGet();
                    return new Pane();
                }, List.of(), "");

        var card = new PlotCard[1];
        onFx(() -> {
            var c = new PlotCard();
            c.setPrefSize(500, 600);
            var root = new StackPane(c);
            root.setPrefSize(500, 600);
            new Scene(root);
            card[0] = c;
            c.setSpec(example.toSpec());
        });
        awaitInstalled(card);

        // Leaving the viewport releases the figure and its canvas.
        onFx(() -> card[0].setViewportVisible(false));
        onFx(() -> {
            var plotPane = plotPaneOf(card[0]);
            assertTrue(plotPane.getChildren().isEmpty(),
                    "leaving the viewport must release the installed figure");
        });

        // Re-entering restarts the load for the same example.
        onFx(() -> card[0].setViewportVisible(true));
        awaitInstalled(card);
        onFx(() -> assertEquals(2, calls.get(),
                "re-entering the viewport must reload the released figure"));
    }

    private static Pane plotPaneOf(PlotCard card) {
        var content = (Parent) card.getChildren().get(0);
        return (Pane) content.getChildrenUnmodifiable().get(2);
    }

    private static boolean isInstalled(PlotCard card) {
        var children = plotPaneOf(card).getChildren();
        // The loading/failure placeholders are Labels; only the installed figure
        // is a plain region.
        return children.size() == 1 && !(children.get(0) instanceof Label);
    }

    private static void awaitInstalled(PlotCard[] card) throws Exception {
        var installed = new boolean[1];
        for (int i = 0; i < 40 && !installed[0]; i++) {
            onFx(() -> installed[0] = isInstalled(card[0]));
            if (!installed[0]) {
                Thread.sleep(50);
            }
        }
        assertTrue(installed[0], "card did not install its figure in time");
    }

    private static Node findLoadingLabel(Node node) {
        if (node instanceof Label lbl && "Loading\u2026".equals(lbl.getText())) {
            return lbl;
        }
        if (node instanceof Parent parent) {
            for (var child : parent.getChildrenUnmodifiable()) {
                var found = findLoadingLabel(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Label findTimingLabel(Node node) {
        if (node instanceof Label lbl && lbl.isManaged() && !lbl.getText().isBlank()
                && lbl.getText().matches("[0-9.]+ (ms|s)")) {
            return lbl;
        }
        if (node instanceof Parent parent) {
            for (var child : parent.getChildrenUnmodifiable()) {
                var found = findTimingLabel(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
