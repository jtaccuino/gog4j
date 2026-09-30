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
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the in-card source drawer opens from its nudge handle with the
 * dedented method body and closes again.
 */
class SourceDrawerTest {

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

    private Stage activeStage;

    /** A fresh stage recorded so the {@link AfterEach} hook can close it. */
    private Stage stage() {
        activeStage = new Stage();
        return activeStage;
    }

    @AfterEach
    void closeStage() {
        // A stage that is never closed stays registered with the JavaFX window
        // bookkeeping and pins its scene for the rest of the JVM. Fire-and-
        // forget: the close only needs to happen, not to be awaited. Wrapping
        // it so a headless-glass close hiccup can never take the FX thread
        // down (which would strand the remaining onFx waits).
        if (activeStage != null) {
            var stage = activeStage;
            activeStage = null;
            Platform.runLater(() -> {
                try {
                    stage.close();
                } catch (Throwable ignored) {
                    // Tearing the window down is best-effort; a headless glass
                    // quirk must not kill the FX thread mid-suite.
                }
            });
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

    private static SamplerExample example() {
        return new SamplerExample("My Plot", "desc", "createMyPlot",
                () -> new Object(), List.of(),
                """
                        public static Plot<DataFrame> createMyPlot() {
                            var df = MpgDatasets.loadMpg();
                            return ggplot(df, aes().x("displ").y("hwy"));
                        }
                        """);
    }

    @Test
    void startsCollapsedAndPopulatesFromTheExample() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> drawer[0] = new SourceDrawer());
        onFx(() -> assertFalse(drawer[0].isOpen(), "drawer must start collapsed"));

        onFx(() -> drawer[0].setSource(example().methodName(), example().source()));
        onFx(() -> {
            assertFalse(drawer[0].isOpen(), "setting an example must keep the drawer collapsed");
            var text = drawer[0].sourceArea().getText();
            assertTrue(text.startsWith("var df = MpgDatasets.loadMpg();"),
                    "drawer must hold the dedented method body, got:\n" + text);
            assertFalse(text.contains("public static"),
                    "method wrapper must not be shown");
        });
    }

    @Test
    void nudgeToggleOpensWithFittedHeightAndCloses() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
        });

        onFx(() -> drawer[0].toggle());
        onFx(() -> {
            assertTrue(drawer[0].isOpen(), "toggling the nudge must open the drawer");
            // Body has 3 non-empty lines; the panel must only claim those rows
            // (never grow taller than the code needs).
            assertEquals(4, drawer[0].visibleRows(),
                    "panel height must fit the code rows");
        });

        onFx(() -> drawer[0].toggle());
        onFx(() -> {
            assertFalse(drawer[0].isOpen(), "toggling again must close the drawer");
            assertEquals(drawer[0].closedSlide(), drawer[0].slideOffset(), 0.5,
                    "a closed drawer must keep the handle parked at the bottom edge");
        });
    }

    @Test
    void reopenStaysOpenAfterAClosedCycle() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
        });
        onFx(() -> drawer[0].toggle());
        onFx(() -> assertTrue(drawer[0].isOpen(), "drawer must open first time"));
        onFx(() -> drawer[0].toggle());
        onFx(() -> assertFalse(drawer[0].isOpen(), "drawer must close"));

        // Re-open must not be immediately undone by any stale close animation.
        onFx(() -> drawer[0].toggle());
        onFx(() -> assertTrue(drawer[0].isOpen(),
                "drawer must stay open after re-opening"));
    }

    @Test
    void clearingTheExampleCollapsesTheDrawer() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            drawer[0].toggle();
        });
        onFx(() -> assertTrue(drawer[0].isOpen(), "drawer must be open"));

        onFx(() -> drawer[0].setSource(null, null));
        onFx(() -> {
            assertFalse(drawer[0].isOpen(),
                    "clearing the example must collapse the drawer");
            assertEquals("", drawer[0].sourceArea().getText(),
                    "clearing must empty the code panel");
        });
    }

    @Test
    void sizesToTheCodeOnceLaidOutInsideAScene() throws Exception {
        // Fonts and control skins only resolve when the drawer is attached to a
        // live scene; the collapsed height must be measured there, not in the
        // detach-created state (which sizes the handle to zero and hides it).
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            var backdrop = new Pane();
            backdrop.setStyle("-fx-background-color: white;");
            backdrop.setPrefSize(760, 520);
            var layer = new StackPane(backdrop, drawer[0]);
            StackPane.setAlignment(drawer[0], Pos.BOTTOM_CENTER);
            drawer[0].setMaxHeight(Region.USE_PREF_SIZE);
            var stage = stage();
            stage.setScene(new Scene(layer, 760, 520));
            stage.show();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
        });
        onFx(() -> {
            var d = drawer[0];
            double height = d.getHeight();
            double slide = d.closedSlide();
            assertTrue(height > 60, "drawer must size itself to the code, was " + height);
            assertTrue(slide > 0 && slide < height,
                    "panel offset must hide only the panel, slide=" + slide);
            assertTrue(height - slide >= 10,
                    "a handle strip must remain visible at the bottom edge, was "
                            + (height - slide));
            assertEquals(slide, d.slideOffset(), 0.5,
                    "collapsed must park the handle at the bottom edge");
        });
    }

    @Test
    void escapeClosesTheOpenDrawer() throws Exception {
        var drawer = new SourceDrawer[1];
        var stage = new Stage[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            stage[0] = stage();
            stage[0].setScene(new Scene(new StackPane(drawer[0]), 400, 300));
            stage[0].show();
            drawer[0].toggle();
        });
        onFx(() -> assertTrue(drawer[0].isOpen(), "drawer must open"));
        onFx(() -> {
            var escape = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                    false, false, false, false);
            stage[0].getScene().getRoot().fireEvent(escape);
        });
        onFx(() -> assertFalse(drawer[0].isOpen(), "ESC must close the open drawer"));
    }

    @Test
    void clickOutsideClosesButClickInsideKeepsTheDrawerOpen() throws Exception {
        var drawer = new SourceDrawer[1];
        var stage = new Stage[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            stage[0] = stage();
            stage[0].setScene(new Scene(new StackPane(drawer[0]), 400, 300));
            stage[0].show();
            drawer[0].toggle();
        });

        // A click on the drawer itself must keep it open.
        onFx(() -> drawer[0].fireEvent(clickEvent()));
        onFx(() -> assertTrue(drawer[0].isOpen(),
                "a click on the drawer must keep it open"));

        // A click anywhere else in the scene must slide it shut.
        onFx(() -> stage[0].getScene().getRoot().fireEvent(clickEvent()));
        onFx(() -> assertFalse(drawer[0].isOpen(),
                "a click outside the drawer must close it"));
    }

    @Test
    void setCodeShowsArbitraryGeneratedSource() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setCode("build", "var df = MpgDatasets.loadMpg();\n"
                    + "return ggplot(df, aes().x(\"displ\").y(\"hwy\"));");
        });
        onFx(() -> {
            assertTrue(drawer[0].sourceArea().getText().contains("MpgDatasets.loadMpg()"),
                    "setCode must display the generated source verbatim");
            assertFalse(drawer[0].isOpen(), "setCode must leave the drawer collapsed");
        });
        onFx(() -> drawer[0].setCode("", ""));
        onFx(() -> assertTrue(drawer[0].sourceArea().getText().isEmpty(),
                "blank generated code must clear the drawer"));
    }

    private static MouseEvent clickEvent() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                MouseButton.PRIMARY, 1, false, false, false, false,
                true, false, false, false, true, false, null);
    }
}
