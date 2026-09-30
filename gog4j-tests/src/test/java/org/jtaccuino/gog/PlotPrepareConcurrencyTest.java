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

import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.layer.LayerData;
import org.jtaccuino.gog.layer.PanelContext;
import org.jtaccuino.gog.layer.PlotContext;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Guards the sampler's prepare contract: {@link Plot#prepareAsync()} runs on a
 * loader thread while layout/redraw runs on the JavaFX thread, so
 * {@code prepare()} must be serialized. A concurrent second entry would clear
 * {@code preparedData} mid-preparation and leave layers rendering empty.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class PlotPrepareConcurrencyTest {

    /** A layer that records the peak number of threads inside prepare(). */
    private static final class SlowLayer implements Layer<DataFrame> {

        private final AtomicInteger active = new AtomicInteger();
        private volatile int maxActive;

        @Override
        public LayerData prepare(PlotContext<DataFrame> ctx) {
            int now = active.incrementAndGet();
            if (now > maxActive) {
                maxActive = now;
            }
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            active.decrementAndGet();
            return LayerData.NONE;
        }

        @Override
        public void render(DrawSurface gc, PanelContext<DataFrame> ctx, LayerData data) {
            // no-op: this layer only exercises the prepare phase
        }
    }

    @Test
    void prepareRunsOnceEvenWhenCalledConcurrently() throws Exception {
        var df = DataFrame.byColumn("x", "y")
                .of(Series.ofDouble(1, 2, 3), Series.ofDouble(4, 5, 6));
        var layer = new SlowLayer();
        Plot<DataFrame> plot = Ggplot.ggplot(df, Aes.aes().x("x").y("y")).layer(layer);

        int threads = 4;
        var barrier = new CyclicBarrier(threads);
        var workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception ignored) {
                    return;
                }
                plot.prepareAsync();
            });
            workers[i].start();
        }
        for (var w : workers) {
            w.join();
        }

        assertEquals(1, layer.maxActive,
                "prepare() must be serialized between the loader and FX threads");
        // Sanity: the lock must still let a later render reuse the prepared state.
        onFxThread(() -> {
            var svg = new SvgDrawSurface(100, 100, Color.WHITE);
            plot.renderTo(svg, 100, 100);
            return null;
        });
    }
}
