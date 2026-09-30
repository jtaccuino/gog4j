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

import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import org.jtaccuino.gog.benchmark.JfrBenchmarkExtension;
import org.jtaccuino.gog.benchmark.MeasurePhases;
import org.jtaccuino.gog.benchmark.PhaseTimings;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.examples.dflib.ComposedPlots;
import org.jtaccuino.gog.examples.dflib.DiamondsPlots;
import org.jtaccuino.gog.examples.dflib.FacetGridPlots;
import org.jtaccuino.gog.examples.dflib.GwasPlots;
import org.jtaccuino.gog.examples.dflib.MatrixPlots;
import org.jtaccuino.gog.examples.dflib.MpgPlots;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Records JFR timing events for representative plots, matrices, and
 * compositions (each rendered in its own recording after 5 warmup + 20
 * measured passes to an off-screen JavaFX canvas on the headless platform,
 * heavy full-dataset cases warmed up 15× and measured 10×) and prints
 * best/avg/p95 per phase, sorted slowest-first. Not an assertion-bearing CI
 * gate; the numbers are the review artifact before/after each parallelization
 * step. Tagged {@code benchmark}: excluded from {@code test}, run via
 * {@code ./gradlew benchmarkTest}.
 */
@Tag("benchmark")
@ExtendWith(JfrBenchmarkExtension.class)
class JfrBenchmarkTest {

    private static final int W = 900;
    private static final int H = 560;
    private static final int MATRIX_W = 1200;
    private static final int MATRIX_H = 900;

    @MeasurePhases
    @Test
    void facetByClarity(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createFacetByClarity();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void colorByClarity(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createColorByClarity();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void manhattan(PhaseTimings timings) throws Exception {
        var plot = GwasPlots.createManhattan();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void freeScalesGrid(PhaseTimings timings) throws Exception {
        var plot = FacetGridPlots.createFreeScalesGrid();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void cutClarityGrid(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createFacetCutClarityGrid();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void groupedSmooth(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createGroupedSmoothByCut();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void twoSmooth(PhaseTimings timings) throws Exception {
        var plot = MpgPlots.createTwoLocalSmoothMappings();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void opacityColorByClarity(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createOpacityColorByClarity();
        timings.measure(() -> renderOnce(plot));
    }

    @MeasurePhases
    @Test
    void opacityConstant(PhaseTimings timings) throws Exception {
        var plot = DiamondsPlots.createOpacityConstant();
        timings.measure(() -> renderOnce(plot));
    }

    // ─── Phase F: matrices and compositions ─────────────────────────────

    @MeasurePhases
    @Test
    void matrixTipsMixed(PhaseTimings timings) throws Exception {
        var matrix = MatrixPlots.createTipsMixed();
        timings.measure(() -> renderOnce(matrix, MATRIX_W, MATRIX_H));
    }

    @MeasurePhases
    @Test
    void matrixPenguinsHue(PhaseTimings timings) throws Exception {
        var matrix = MatrixPlots.createPenguinsMatrixBySpecies();
        timings.measure(() -> renderOnce(matrix, MATRIX_W, MATRIX_H));
    }

    @MeasurePhases(warmup = 15, measured = 10)
    @Test
    void matrixDiamondsNumericFull(PhaseTimings timings) throws Exception {
        var matrix = MatrixPlots.createDiamondsMatrix();
        timings.measure(() -> renderOnce(matrix, MATRIX_W, MATRIX_H));
    }

    @MeasurePhases(warmup = 15, measured = 10)
    @Test
    void matrixDiamondsColorFull(PhaseTimings timings) throws Exception {
        var matrix = MatrixPlots.createDiamondsMatrixByCut();
        timings.measure(() -> renderOnce(matrix, MATRIX_W, MATRIX_H));
    }

    @MeasurePhases(warmup = 15, measured = 10)
    @Test
    void composedDiamondsLarge(PhaseTimings timings) throws Exception {
        var composed = ComposedPlots.createDiamondsGrid();
        timings.measure(() -> renderOnce(composed, W, H));
    }

    @MeasurePhases(warmup = 15, measured = 10)
    @Test
    void composedMixedAreaFull(PhaseTimings timings) throws Exception {
        var composed = ComposedPlots.createDiamondsByCutLegend();
        timings.measure(() -> renderOnce(composed, W, H));
    }

    /** A sampled twin of the full diamonds matrix, for the scaling comparison. */
    @MeasurePhases
    @Test
    void matrixDiamondsSampledTwin(PhaseTimings timings) throws Exception {
        var sampled = DiamondsDatasets.loadDiamonds().head(2000);
        var matrix = Ggplot.matrixPlot(sampled, "carat", "depth", "price", "table");
        timings.measure(() -> renderOnce(matrix, MATRIX_W, MATRIX_H));
    }

    private static void renderOnce(GgFigure figure) throws Exception {
        renderOnce(figure, W, H);
    }

    private static void renderOnce(GgFigure figure, int width, int height) throws Exception {
        runOnFx(() -> {
            var canvas = new Canvas(width, height);
            figure.renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), width, height);
            return null;
        });
    }

    private static <T> T runOnFx(java.util.concurrent.Callable<T> callable) throws Exception {
        var task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get();
    }
}
