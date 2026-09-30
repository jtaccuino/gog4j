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

import static org.jtaccuino.gog.Aes.aes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link GgFigurePane#applyTheme} re-themes a figure in place: a
 * {@link Plot} swaps its descriptor and resolved theme, a {@link ComposedPlot}
 * cascades to its leaves, and a {@link PlotMatrix} themes every cell.
 */
class ApplyThemeTest {

    private static final Color DARK_PANE = Theme.theme_dark().paneBackground();

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

    @Test
    void plotAppliesTheThemeToDescriptorAndResolvedFigure() throws Exception {
        onFx(() -> {
            var plot = Ggplot.ggplot(aes());
            assertEquals(Theme.theme_gray().paneBackground(),
                    plot.getTheme().paneBackground(),
                    "a fresh plot must default to the gray theme");
            plot.applyTheme(Theme.theme_dark());
            assertEquals(DARK_PANE, plot.getTheme().paneBackground(),
                    "the descriptor theme must switch to dark");
            assertEquals(DARK_PANE, plot.themeProperty().get().paneBackground(),
                    "the resolved figure theme must switch to dark");
        });
    }

    @Test
    void composedPlotCascadesTheThemeToItsLeaves() throws Exception {
        onFx(() -> {
            var composed = Ggplot.composedPlot(Ggplot.ggplot(aes()), Ggplot.ggplot(aes()));
            composed.applyTheme(Theme.theme_dark());
            for (var leaf : composed.leaves()) {
                if (leaf instanceof Plot<?> p) {
                    assertEquals(DARK_PANE, p.getTheme().paneBackground(),
                            "every leaf plot must re-theme to dark");
                }
            }
            assertEquals(DARK_PANE, composed.themeProperty().get().paneBackground(),
                    "the composed figure must re-resolve to dark");
        });
    }

    @Test
    void matrixAppliesTheThemeToEveryCell() throws Exception {
        onFx(() -> {
            var df = DataFrame.byColumn("x").of(Series.of(1.0, 2.0, 3.0));
            var d1 = Ggplot.plot(df, aes().x("x"));
            var d2 = Ggplot.plot(df, aes().x("x"));
            var d3 = Ggplot.plot(df, aes().x("x"));
            var d4 = Ggplot.plot(df, aes().x("x"));
            var matrix = PlotMatrix.<DataFrame>create(2, 2).cells(d1, d2, d3, d4);

            matrix.applyTheme(Theme.theme_dark());
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    assertEquals(DARK_PANE, matrix.cell(r, c).theme().paneBackground(),
                            "every cell descriptor must re-theme to dark");
                }
            }
            assertEquals(DARK_PANE, matrix.themeProperty().get().paneBackground(),
                    "the matrix figure must re-resolve to dark");
        });
    }
}
