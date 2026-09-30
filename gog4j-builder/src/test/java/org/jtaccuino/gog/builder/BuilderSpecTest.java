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
package org.jtaccuino.gog.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the {@link BuilderSpec} assembles a {@code Plot} from a
 * {@link BuilderModel} and that the result renders end to end — the same path
 * the preview pane drives.
 */
class BuilderSpecTest {

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

    @Test
    void buildsAndRendersAScatter() throws Exception {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.color = "drv";
        var point = new BuilderModel.Geom(BuilderModel.GeomKind.POINT);
        point.doubles.put(BuilderModel.Params.SIZE, 2.0);
        model.geoms.add(point);

        var failure = new Throwable[1];
        var rendered = new double[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var plot = BuilderSpec.build(model);
                assertEquals("MPG", model.dataset().displayName());
                var root = new StackPane(plot);
                new Scene(root, 600, 480);
                root.applyCss();
                root.layout();
                var canvas = new Canvas(600, 480);
                plot.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), 600, 480);
                rendered[0] = plot.lastRenderNanos();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "render task timed out");
        if (failure[0] != null) {
            throw new AssertionError("BuilderSpec.build() failed", failure[0]);
        }
        assertTrue(rendered[0] > 0, "the assembled plot must render in full fidelity");
    }

    @Test
    void buildsABarChartWithHlineAndManualColors() throws Exception {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "class";
        model.y = "hwy";
        model.color = "drv";
        model.colorScale = BuilderModel.ColorScaleKind.MANUAL;
        model.manualColors.add(Color.web("#4c72b0"));
        model.manualColors.add(Color.web("#dd8452"));
        var bar = new BuilderModel.Geom(BuilderModel.GeomKind.BAR);
        bar.colors.put(BuilderModel.Params.FILL, Color.web("#55a868"));
        model.geoms.add(bar);
        var hline = new BuilderModel.Geom(BuilderModel.GeomKind.HLINE);
        hline.doubles.put(BuilderModel.Params.YINTERCEPT, 25.0);
        model.geoms.add(hline);

        var failure = new Throwable[1];
        var rendered = new double[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var plot = BuilderSpec.build(model);
                var root = new StackPane(plot);
                new Scene(root, 600, 480);
                root.applyCss();
                root.layout();
                var canvas = new Canvas(600, 480);
                plot.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), 600, 480);
                rendered[0] = plot.lastRenderNanos();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "render task timed out");
        if (failure[0] != null) {
            throw new AssertionError("BuilderSpec.build() failed", failure[0]);
        }
        assertTrue(rendered[0] > 0, "the assembled bar/hline plot must render");
    }

    @Test
    void buildsAndRendersA3dScatter() throws Exception {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        model.z = "drv";
        model.coord = BuilderModel.CoordKind.COORD3D;
        model.coord3dPanels = CubePanel.NONE;
        model.coord3dLight = true;
        var point3d = new BuilderModel.Geom(BuilderModel.GeomKind.POINT3D);
        point3d.doubles.put(BuilderModel.Params.SIZE, 5.0);
        model.geoms.add(point3d);

        var failure = new Throwable[1];
        var rendered = new double[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var plot = BuilderSpec.build(model);
                var root = new StackPane(plot);
                new Scene(root, 600, 480);
                root.applyCss();
                root.layout();
                var canvas = new Canvas(600, 480);
                plot.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), 600, 480);
                rendered[0] = plot.lastRenderNanos();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "render task timed out");
        if (failure[0] != null) {
            throw new AssertionError("BuilderSpec.build() failed", failure[0]);
        }
        assertTrue(rendered[0] > 0, "the assembled 3D plot must render in full fidelity");
    }

    @Test
    void buildsAndRendersAFacetedGrid() throws Exception {
        var model = new BuilderModel();
        model.setDataset(DflibDatasets.all().get(0));
        model.x = "displ";
        model.y = "hwy";
        var point = new BuilderModel.Geom(BuilderModel.GeomKind.POINT);
        model.geoms.add(point);
        model.facet = BuilderModel.FacetKind.GRID;
        model.facetGridRow = "drv";
        model.facetGridCol = "cyl";

        var failure = new Throwable[1];
        var rendered = new double[1];
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var plot = BuilderSpec.build(model);
                var root = new StackPane(plot);
                new Scene(root, 600, 480);
                root.applyCss();
                root.layout();
                var canvas = new Canvas(600, 480);
                plot.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), 600, 480);
                rendered[0] = plot.lastRenderNanos();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "render task timed out");
        if (failure[0] != null) {
            throw new AssertionError("BuilderSpec.build() failed", failure[0]);
        }
        assertTrue(rendered[0] > 0, "the assembled faceted plot must render");
    }
}
