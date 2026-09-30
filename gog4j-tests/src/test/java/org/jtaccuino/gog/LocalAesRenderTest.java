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
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.layer.LayerData;
import org.jtaccuino.gog.layer.PanelContext;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that a per-geom (local) aesthetic mapping is actually threaded
 * through to a {@link Layer} when it is rendered, rather than being silently
 * ignored in favour of the plot-global {@link Aes}.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class LocalAesRenderTest {

    private record SpyLayer(AtomicReference<Aes> seen) implements Layer<DataFrame> {
        @Override
        public void render(DrawSurface gc, PanelContext<DataFrame> ctx, LayerData data) {
            seen.set(ctx.plot().aes());
        }
    }

    private static DataFrame sampleData() {
        var n = 8;
        var x = new double[n];
        var y = new double[n];
        var g = new String[n];
        for (var i = 0; i < n; i++) {
            x[i] = i;
            y[i] = i * 2.0;
            g[i] = i % 2 == 0 ? "a" : "b";
        }
        return DataFrame.byColumn("x", "y", "g")
                .of(Series.ofDouble(x), Series.ofDouble(y), Series.of(g));
    }

    private <T> T onFxThread(Callable<T> work) throws Exception {
        var task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(60, TimeUnit.SECONDS);
    }

    private void forceRedraw(Plot<DataFrame> plot) throws Exception {
        onFxThread(() -> {
            var holder = new StackPane(plot);
            holder.setPrefSize(400, 300);
            new Scene(holder, 400, 300);
            holder.applyCss();
            holder.layout();
            plot.redraw();
            return null;
        });
    }

    @Test
    void localAesIsThreadedThroughToTheLayerOnRender() throws Exception {
        var plot = Ggplot.ggplot(sampleData(), aes().x("x").y("y"))
                .geoms(Geoms.point().size(4.0));

        var seen = new AtomicReference<Aes>();
        plot.registerInternalLayer(new SpyLayer(seen), aes().color("g"));

        forceRedraw(plot);

        var effective = seen.get();
        assertEquals("x", effective.x(), "global x aesthetic must survive the merge");
        assertEquals("y", effective.y(), "global y aesthetic must survive the merge");
        assertEquals("g", effective.color(), "the per-geom color override must reach the layer");
        assertNull(effective.size(), "unset per-geom size must fall back to the global (none)");
    }
}
