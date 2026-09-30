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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.GgFigurePane;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tags;
import org.jtaccuino.gog.sampler.ui.FilterBar;
import org.jtaccuino.gog.sampler.ui.LoadProgressSupport;
import org.jtaccuino.gog.sampler.ui.ThemeSwitch;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the shared {@link SourceTheme} drives every drawer: flipping it
 * switches the drawer's theme CSS class and re-styles the code in place.
 */
class SourceThemeTest {

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

    @BeforeEach
    void startFromLightTheme() throws Exception {
        onFx(() -> SourceTheme.CURRENT.set(SourceTheme.LIGHT));
    }

    @AfterEach
    void restoreLightTheme() throws Exception {
        onFx(() -> SourceTheme.CURRENT.set(SourceTheme.LIGHT));
        // The plot-load tests drive the shared LoadProgress singleton; drain
        // it so no in-flight beginLoad leaks into a later test class.
        LoadProgressSupport.settle();
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
    void togglingTheSharedThemeSwitchesTheDrawerAndRestylesTheCode() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            assertTrue(drawer[0].getStyleClass().contains("source-drawer-theme-light"),
                    "the drawer must start in the light theme");
        });

        onFx(() -> SourceTheme.CURRENT.set(SourceTheme.DARK));
        onFx(() -> {
            assertTrue(drawer[0].getStyleClass().contains("source-drawer-theme-dark"),
                    "switching the theme must mark the drawer dark");
            assertFalse(drawer[0].getStyleClass().contains("source-drawer-theme-light"),
                    "the light class must be dropped");
            assertEquals(SourceTheme.DARK, SourceTheme.CURRENT.get());

            var model = (CodeTextModel) drawer[0].sourceArea().getModel();
            var paragraph = model.getParagraph(0);
            assertEquals("var df = MpgDatasets.loadMpg();", paragraph.getPlainText());
            var resolver = new StyleResolver() {
                @Override
                public StyleAttributeMap resolveStyles(StyleAttributeMap attrs) {
                    return attrs;
                }

                @Override
                public WritableImage snapshot(Node node) {
                    return null;
                }
            };
            Color keyword = null;
            for (int i = 0; i < paragraph.getSegmentCount(); i++) {
                var segment = paragraph.getSegment(i);
                if ("var".equals(segment.getText())) {
                    keyword = segment.getStyleAttributeMap(resolver).getTextColor();
                }
            }
            assertEquals(SourceTheme.DARK.keyword(), keyword,
                    "the code must re-style under the dark theme");
        });
    }

    @Test
    void theThemeSwitchSelectsTheSharedThemeAndTheCodePaneFollows() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            var sw = new ThemeSwitch();
            sw.dark().fire();
            assertEquals(SourceTheme.DARK, SourceTheme.CURRENT.get(),
                    "the switch must drive the shared theme");
            assertTrue(drawer[0].getStyleClass().contains("source-drawer-theme-dark"),
                    "the code pane must follow the dark selection");
            sw.light().fire();
            assertEquals(SourceTheme.LIGHT, SourceTheme.CURRENT.get(),
                    "the switch must flip back to light");
            assertTrue(drawer[0].getStyleClass().contains("source-drawer-theme-light"),
                    "the code pane must follow the light selection");
        });
    }

    @Test
    void theFilterBarThemeSwitchDrivesEveryDrawer() throws Exception {
        var drawer = new SourceDrawer[1];
        onFx(() -> {
            drawer[0] = new SourceDrawer();
            drawer[0].setSource(example().methodName(), example().source());
            var bar = new FilterBar(Tags.all());
            bar.themeDark().fire();
            assertEquals(SourceTheme.DARK, SourceTheme.CURRENT.get(),
                    "the filter-bar switch must select the dark theme");
            assertTrue(drawer[0].getStyleClass().contains("source-drawer-theme-dark"),
                    "selecting dark must switch the code pane automatically");
            bar.themeLight().fire();
            assertEquals(SourceTheme.LIGHT, SourceTheme.CURRENT.get(),
                    "the filter-bar switch must flip back to light");
        });
    }

    @Test
    void selectingDarkRethemesTheInstalledCardPlot() throws Exception {
        var card = new PlotCard[1];
        onFx(() -> {
            card[0] = new PlotCard();
            var root = new StackPane(card[0]);
            new Scene(root);
            var plotExample = new SamplerExample("Plot", "desc", "factory",
                    () -> Ggplot.ggplot(Aes.aes()),
                    List.of(), "");
            card[0].setSpec(plotExample.toSpec());
        });

        var lightPlot = awaitPlot(card);
        onFx(() -> assertEquals(Theme.theme_gray().paneBackground(),
                lightPlot.themeProperty().get().paneBackground(),
                "a fresh plot must render with the gray theme"));

        onFx(() -> SourceTheme.CURRENT.set(SourceTheme.DARK));
        var darkPlot = awaitPlot(card);
        onFx(() -> assertEquals(Theme.theme_dark().paneBackground(),
                darkPlot.themeProperty().get().paneBackground(),
                "switching dark must re-theme the installed plot"));
    }

    private static GgFigurePane awaitPlot(PlotCard[] card) throws Exception {
        for (int i = 0; i < 60; i++) {
            var ref = new GgFigurePane[1];
            onFx(() -> {
                var content = (Parent) card[0].getChildren().get(0);
                var plotPane = (Parent) content.getChildrenUnmodifiable().get(2);
                for (var node : plotPane.getChildrenUnmodifiable()) {
                    if (node instanceof GgFigurePane figure) {
                        ref[0] = figure;
                        return;
                    }
                }
            });
            if (ref[0] != null) {
                return ref[0];
            }
            Thread.sleep(50);
        }
        throw new AssertionError("no plot was installed on the card");
    }
}
