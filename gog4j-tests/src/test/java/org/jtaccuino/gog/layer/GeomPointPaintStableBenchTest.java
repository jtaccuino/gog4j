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
package org.jtaccuino.gog.layer;

import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.PlotMatrix;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots;
import org.jtaccuino.gog.examples.dflib.GwasPlots;
import org.jtaccuino.gog.examples.dflib.MatrixPlots;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Prints FX-thread renderTo timings for GeomPoint with paint-stable on/off. */
@Tag("benchmark")
class GeomPointPaintStableBenchTest {

    private static final int W = 900;
    private static final int H = 560;
    private static final int MATRIX_W = 1200;
    private static final int MATRIX_H = 900;

    @BeforeAll
    static void startFx() throws InterruptedException {
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit already brought up by an earlier test in this JVM.
        }
    }

    @Test
    void printTimings() throws Exception {
        // diamonds clarity scatter: 53,940 points, 8 interleaved colours
        var clarity = ggplot(DiamondsDatasets.loadDiamonds(),
                Aes.aes().x("carat").y("price").color("clarity"))
                .geoms(point().size(2.0))
                .theme(Theme.theme_bw())
                .labs(labs("t", "carat", "price"));

        // the human height GWAS Manhattan: 467k points, chromosome-sorted colours
        var gwas = GwasPlots.createManhattan();

        var clarityNoLegend = ggplot(DiamondsDatasets.loadDiamonds(),
                Aes.aes().x("carat").y("price").color("clarity"))
                .geoms(point().size(2.0))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs(labs("t", "carat", "price"));

        // Faceted by Clarity: 8 panels (2x4), constant-style points + per-panel
        // LOESS smooth, fractional panelWidth from the /numCols division.
        var faceted = DiamondsPlots.createFacetByClarity();

        // Facet grid Cut x Clarity: 5x7 = 35 panels, free scales, uniform panel
        // weights, fractional colWidth/rowHeight from the grid division.
        var cutClarityGrid = DiamondsPlots.createFacetCutClarityGrid();

        // Diamonds pair plot matrices over the full 54k-row dataset: 4x4 cell
        // grids, fractional proportional cell widths/heights from the division.
        var diamondsMatrix = MatrixPlots.createDiamondsMatrix();
        var diamondsMatrixByCut = MatrixPlots.createDiamondsMatrixByCut();

        System.out.println("FULL renderTo + canvas snapshot (render thread rasterization):");
        print("  clarity 54k (no legend) bucketOFF", clarityNoLegend, false, true, 3);
        print("  clarity 54k (no legend) bucketON ", clarityNoLegend, true, true, 3);
        print("  clarity 54k (legend) bucketON ", clarity, true, true, 3);
        print("  faceted clarity (8 panels)  ", faceted, true, true, 3);
        print("  cut x clarity grid (35 panels)", cutClarityGrid, true, true, 3);
        printMatrix("  diamonds matrix (4x4)      ", diamondsMatrix, true, 3);
        printMatrix("  diamonds matrix by cut (4x4)", diamondsMatrixByCut, true, 3);
        print("  gwas 467k ", gwas, true, true, 2);

        System.out.println("FAST renderTo + canvas snapshot (RenderMode.FAST: stroke suppress + overdraw bin):");
        printMatrixFast("  diamonds matrix (4x4)      ", MatrixPlots.createDiamondsMatrix(), true, 3);
        printMatrixFast("  diamonds matrix by cut (4x4)", MatrixPlots.createDiamondsMatrixByCut(), true, 3);

        System.out.println("renderTo only (FX recording, no snapshot):");
        print("  clarity 54k (legend)", clarity, true, false, 10);
        print("  faceted clarity (8 panels)", faceted, true, false, 10);
        print("  cut x clarity grid (35 panels)", cutClarityGrid, true, false, 10);
        print("  gwas 467k ", gwas, true, false, 10);
        printMatrixFx("  diamonds matrix (4x4)      ", diamondsMatrix, 5);
        printMatrixFx("  diamonds matrix by cut (4x4)", diamondsMatrixByCut, 5);

        printFacetLayout("faceted clarity", faceted);
        printFacetLayout("cut x clarity grid", cutClarityGrid);
        printMatrixLayout("diamonds matrix", diamondsMatrix);
        printMatrixLayout("diamonds matrix by cut", diamondsMatrixByCut);
    }

    private void printFacetLayout(String label, Plot<DataFrame> plot) throws Exception {
        var task = new FutureTask<>(() -> {
            var rec = new Recording();
            rec.enable("org.jtaccuino.gog.jfr.FacetLayout");
            rec.start();
            render(plot, false);
            rec.stop();
            try {
                var f = Files.createTempFile("gog-facet-layout", ".jfr");
                rec.dump(f);
                try (var file = new RecordingFile(f)) {
                    while (file.hasMoreEvents()) {
                        var evt = file.readEvent();
                        if (!evt.getEventType().getName().equals("org.jtaccuino.gog.jfr.FacetLayout")) {
                            continue;
                        }
                        System.out.printf("  %s facet layout: cols=%d rows=%d panelW=%.3f panelH=%.3f originsIntegral=%s%n",
                                label, evt.getInt("numCols"), evt.getInt("numRows"),
                                evt.getDouble("panelWidth"), evt.getDouble("panelHeight"),
                                evt.getBoolean("originsIntegral"));
                    }
                }
                Files.deleteIfExists(f);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return (Void) null;
        });
        Platform.runLater(task);
        task.get();
    }

    private void print(String label, Plot<DataFrame> plot, boolean bucket, boolean snapshot, int runs)
            throws Exception {
        var old = GeomPoint.PAINT_STABLE_THRESHOLD;
        GeomPoint.PAINT_STABLE_THRESHOLD = bucket ? 0 : Integer.MAX_VALUE;
        try {
            var task = new FutureTask<>(() -> {
                for (int i = 0; i < runs; i++) {
                    render(plot, snapshot);
                }
                long best = Long.MAX_VALUE;
                long total = 0;
                for (int i = 0; i < runs; i++) {
                    long t0 = System.nanoTime();
                    render(plot, snapshot);
                    long dt = System.nanoTime() - t0;
                    best = Math.min(best, dt);
                    total += dt;
                }
                System.out.printf("  %s: best=%.1fms avg=%.1fms%n",
                        label, best / 1e6, total / (double) runs / 1e6);
                return (Void) null;
            });
            Platform.runLater(task);
            task.get();
        } finally {
            GeomPoint.PAINT_STABLE_THRESHOLD = old;
        }
    }

    private static void render(Plot<DataFrame> plot, boolean snapshot) {
        var canvas = new Canvas(W, H);
        plot.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), W, H);
        if (snapshot) {
            // Forces the render thread to rasterize the recorded display list
            // now, approximating the cached-texture build at scroll-into time.
            var params = new SnapshotParameters();
            canvas.snapshot(params, new WritableImage(W, H));
        }
    }

    private void printMatrix(String label, PlotMatrix<DataFrame> matrix, boolean bucket, int runs)
            throws Exception {
        var old = GeomPoint.PAINT_STABLE_THRESHOLD;
        GeomPoint.PAINT_STABLE_THRESHOLD = bucket ? 0 : Integer.MAX_VALUE;
        try {
            var task = new FutureTask<>(() -> {
                for (int i = 0; i < runs; i++) {
                    renderMatrix(matrix, true);
                }
                long best = Long.MAX_VALUE;
                long total = 0;
                for (int i = 0; i < runs; i++) {
                    long t0 = System.nanoTime();
                    renderMatrix(matrix, true);
                    long dt = System.nanoTime() - t0;
                    best = Math.min(best, dt);
                    total += dt;
                }
                System.out.printf("  %s: best=%.1fms avg=%.1fms%n",
                        label, best / 1e6, total / (double) runs / 1e6);
                return (Void) null;
            });
            Platform.runLater(task);
            task.get();
        } finally {
            GeomPoint.PAINT_STABLE_THRESHOLD = old;
        }
    }

    private void printMatrixFx(String label, PlotMatrix<DataFrame> matrix, int runs)
            throws Exception {
        var task = new FutureTask<>(() -> {
            for (int i = 0; i < runs; i++) {
                renderMatrix(matrix, false);
            }
            long best = Long.MAX_VALUE;
            long total = 0;
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                renderMatrix(matrix, false);
                long dt = System.nanoTime() - t0;
                best = Math.min(best, dt);
                total += dt;
            }
            System.out.printf("  %s: best=%.1fms avg=%.1fms%n",
                    label, best / 1e6, total / (double) runs / 1e6);
            return (Void) null;
        });
        Platform.runLater(task);
        task.get();
    }

    private void printMatrixFast(String label, PlotMatrix<DataFrame> matrix, boolean bucket, int runs)
            throws Exception {
        var old = GeomPoint.PAINT_STABLE_THRESHOLD;
        GeomPoint.PAINT_STABLE_THRESHOLD = bucket ? 0 : Integer.MAX_VALUE;
        matrix.renderMode(RenderMode.FAST);
        try {
            var task = new FutureTask<>(() -> {
                for (int i = 0; i < runs; i++) {
                    renderMatrix(matrix, true);
                }
                long best = Long.MAX_VALUE;
                long total = 0;
                for (int i = 0; i < runs; i++) {
                    long t0 = System.nanoTime();
                    renderMatrix(matrix, true);
                    long dt = System.nanoTime() - t0;
                    best = Math.min(best, dt);
                    total += dt;
                }
                System.out.printf("  %s: best=%.1fms avg=%.1fms%n",
                        label, best / 1e6, total / (double) runs / 1e6);
                return (Void) null;
            });
            Platform.runLater(task);
            task.get();
        } finally {
            GeomPoint.PAINT_STABLE_THRESHOLD = old;
            matrix.renderMode(RenderMode.FULL);
        }
    }

    private void printMatrixLayout(String label, PlotMatrix<DataFrame> matrix) throws Exception {
        var task = new FutureTask<>(() -> {
            var rec = new Recording();
            rec.enable("org.jtaccuino.gog.jfr.MatrixLayout");
            rec.start();
            renderMatrix(matrix, false);
            rec.stop();
            try {
                var f = Files.createTempFile("gog-matrix-layout", ".jfr");
                rec.dump(f);
                try (var file = new RecordingFile(f)) {
                    while (file.hasMoreEvents()) {
                        var evt = file.readEvent();
                        if (!evt.getEventType().getName().equals("org.jtaccuino.gog.jfr.MatrixLayout")) {
                            continue;
                        }
                        System.out.printf("  %s matrix layout: cols=%d rows=%d cellW=%.3f cellH=%.3f originsIntegral=%s%n",
                                label, evt.getInt("numCols"), evt.getInt("numRows"),
                                evt.getDouble("cellWidth"), evt.getDouble("cellHeight"),
                                evt.getBoolean("originsIntegral"));
                    }
                }
                Files.deleteIfExists(f);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return (Void) null;
        });
        Platform.runLater(task);
        task.get();
    }

    private static void renderMatrix(PlotMatrix<DataFrame> matrix, boolean snapshot) {
        var canvas = new Canvas(MATRIX_W, MATRIX_H);
        matrix.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), MATRIX_W, MATRIX_H);
        if (snapshot) {
            // Forces the render thread to rasterize the recorded display list now,
            // approximating the cached-texture build at scroll-into time.
            var params = new SnapshotParameters();
            canvas.snapshot(params, new WritableImage(MATRIX_W, MATRIX_H));
        }
    }
}
