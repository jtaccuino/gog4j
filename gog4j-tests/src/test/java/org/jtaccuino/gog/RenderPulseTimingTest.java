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
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.layer.Position.STACK;

import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Prints per-frame render times for representative plots — a manual smoke to
 * watch layout regressions, not an assertion-bearing CI gate. Tagged
 * {@code benchmark}: excluded from {@code test}, run via {@code benchmarkTest}.
 */
@Tag("benchmark")
@ExtendWith(JavaFxToolkitExtension.class)
class RenderPulseTimingTest {

    private static final int W = 900;
    private static final int H = 560;

    @Test
    void printRenderTimes() throws Exception {
        var scatter = build(() -> ggplot(DiamondsDatasets.loadDiamonds(),
                aes().x("carat").y("price").color("cut"))
                .geoms(point().size(2.0))
                .labs(labs("t", "carat", "price")));

        var stacked = build(() -> ggplot(DiamondsDatasets.loadDiamonds(),
                aes().x("carat").fill("cut"))
                .geoms(density().stat("count").position(STACK).alpha(0.85))
                .labs(labs("t", "carat", "count")));

        time("scatter (mapped y)", scatter);
        time("density stat y (probe)", stacked);
    }

    private static Plot<DataFrame> build(
            Supplier<Plot<DataFrame>> work) throws Exception {
        var task = new FutureTask<>(work::get);
        Platform.runLater(task);
        task.get();
        return task.get();
    }

    private void time(String label, Plot<DataFrame> plot) throws Exception {
        var task = new FutureTask<>(() -> {
            for (int i = 0; i < 3; i++) {
                plot.renderTo(new SvgDrawSurface(W, H, Color.WHITE), W, H);
            }
            long best = Long.MAX_VALUE;
            long total = 0;
            int runs = 10;
            for (int i = 0; i < runs; i++) {
                var s = new SvgDrawSurface(W, H, Color.WHITE);
                long t0 = System.nanoTime();
                plot.renderTo(s, W, H);
                long dt = System.nanoTime() - t0;
                best = Math.min(best, dt);
                total += dt;
            }
            System.out.printf("%s: best=%.1fms avg=%.1fms%n",
                    label, best / 1e6, total / (double) runs / 1e6);
            return (Void) null;
        });
        Platform.runLater(task);
        task.get();
    }
}
