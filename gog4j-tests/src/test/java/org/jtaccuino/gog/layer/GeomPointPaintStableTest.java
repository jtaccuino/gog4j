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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the paint-stable ordering path in {@link GeomPoint} draws exactly
 * the same geometry as the interleaved single-pass path. A dense colour-mapped
 * scatter (the pathological case the ordering targets) is rendered through both
 * paths onto a recording surface; for fully opaque points the emitted shapes
 * are identical as a multiset, since only the draw order differs.
 */
class GeomPointPaintStableTest {

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        try {
            var latch = new java.util.concurrent.CountDownLatch(1);
            javafx.application.Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit already brought up by an earlier test in this JVM.
        }
    }

    @Test
    void bufferedPathEmitsTheSameShapesAsTheInterleavedPath() throws Exception {
        var plot = Ggplot.ggplot(largeInterleavedScatter(),
                                 Aes.aes().x("x").y("y").color("g"))
                .geoms(point().size(2.0))
                .theme(Theme.theme_bw());

        var interleaved = new RecordingSurface();
        var buffered = new RecordingSurface();
        var oldThreshold = GeomPoint.PAINT_STABLE_THRESHOLD;
        try {
            GeomPoint.PAINT_STABLE_THRESHOLD = Integer.MAX_VALUE;
            onFx(() -> plot.renderTo(interleaved, 600, 400));

            GeomPoint.PAINT_STABLE_THRESHOLD = 0;
            onFx(() -> plot.renderTo(buffered, 600, 400));
        } finally {
            GeomPoint.PAINT_STABLE_THRESHOLD = oldThreshold;
        }

        assertEquals(interleaved.ovals.size(), buffered.ovals.size(),
                "both paths must draw the same number of ovals");
        assertEquals(sort(interleaved.ovals), sort(buffered.ovals),
                "for fully opaque points the emitted oval multiset must be identical"
                + " regardless of draw order");
    }

    @Test
    void constantStyleLargeScatterStillDrawsEveryPoint() throws Exception {
        var plot = Ggplot.ggplot(largeConstantScatter(), Aes.aes().x("x").y("y"))
                .geoms(point().size(2.0))
                .theme(Theme.theme_bw());

        var buffered = new RecordingSurface();
        onFx(() -> plot.renderTo(buffered, 600, 400));

        // Constant style never buffers, but the recorded count must match rows
        // (a fill and a stroke oval per point).
        assertEquals(3000 * 2, buffered.ovals.size());
    }

    @Test
    void alphaLayerKeepsTheSameCountAndColours() throws Exception {
        // A semi-transparent layer uses per-style alpha colours; the buffered
        // path must still emit one fill+stroke per point with the same resolved
        // colours, just grouped by style.
        var plot = Ggplot.ggplot(largeInterleavedScatter(),
                                 Aes.aes().x("x").y("y").color("g"))
                .geoms(point().size(2.0).opacity(0.5))
                .theme(Theme.theme_bw());

        var interleaved = new RecordingSurface();
        var buffered = new RecordingSurface();
        var oldThreshold = GeomPoint.PAINT_STABLE_THRESHOLD;
        try {
            GeomPoint.PAINT_STABLE_THRESHOLD = Integer.MAX_VALUE;
            onFx(() -> plot.renderTo(interleaved, 600, 400));

            GeomPoint.PAINT_STABLE_THRESHOLD = 0;
            onFx(() -> plot.renderTo(buffered, 600, 400));
        } finally {
            GeomPoint.PAINT_STABLE_THRESHOLD = oldThreshold;
        }

        assertEquals(interleaved.ovals.size(), buffered.ovals.size(),
                "both paths must draw the same number of ovals for an alpha layer");
        assertEquals(sort(interleaved.ovals), sort(buffered.ovals),
                "the alpha-resolved colours and positions must match as a multiset");
    }

    private static void onFx(Runnable work) throws Exception {
        var task = new java.util.concurrent.FutureTask<>(work, null);
        javafx.application.Platform.runLater(task);
        task.get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    /** A multiset comparison: sort both by (x, y, fill) so draw order vanishes. */
    private static List<Oval> sort(List<Oval> ovals) {
        var copy = new ArrayList<>(ovals);
        copy.sort(Comparator.comparingDouble(Oval::x)
                .thenComparingDouble(Oval::y)
                .thenComparing(o -> o.fill().toString()));
        return copy;
    }

    private static DataFrame largeInterleavedScatter() {
        var n = 4000;
        var x = new double[n];
        var y = new double[n];
        var g = new String[n];
        var groups = new String[]{"a", "b", "c", "d", "e"};
        for (var i = 0; i < n; i++) {
            x[i] = i % 80;
            y[i] = (i * 7) % 67 + (i % 11);
            g[i] = groups[i % groups.length];
        }
        return DataFrame.byColumn("x", "y", "g")
                .of(Series.ofDouble(x), Series.ofDouble(y), Series.of(g));
    }

    private static DataFrame largeConstantScatter() {
        var n = 3000;
        var x = new double[n];
        var y = new double[n];
        for (var i = 0; i < n; i++) {
            x[i] = i % 70;
            y[i] = (i * 5) % 61;
        }
        return DataFrame.byColumn("x", "y")
                .of(Series.ofDouble(x), Series.ofDouble(y));
    }

    /** Records every oval drawn and the fill active when it was drawn. */
    private record Oval(double x, double y, double w, double h, Paint fill) {
    }

    private static final class RecordingSurface implements DrawSurface {

        final List<Oval> ovals = new ArrayList<>();
        private Paint fill = Color.BLACK;
        private Paint stroke = Color.BLACK;

        @Override public void setFill(Paint paint) { fill = paint; }
        @Override public void setStroke(Paint paint) { stroke = paint; }
        @Override public void setLineWidth(double width) { }
        @Override public void setLineDashes(double... dashes) { }
        @Override public void setFont(Font font) { }
        @Override public Font getFont() { return null; }
        @Override public void setTextAlign(TextAlignment alignment) { }
        @Override public void setTextBaseline(VPos baseline) { }
        @Override public void save() { }
        @Override public void restore() { }
        @Override public void translate(double x, double y) { }
        @Override public void rotate(double degrees) { }
        @Override public void scale(double sx, double sy) { }
        @Override public void fillRect(double x, double y, double w, double h) { }
        @Override public void strokeRect(double x, double y, double w, double h) { }
        @Override public void fillOval(double x, double y, double w, double h) {
            ovals.add(new Oval(x, y, w, h, fill));
        }
        @Override public void strokeOval(double x, double y, double w, double h) {
            ovals.add(new Oval(x, y, w, h, stroke));
        }
        @Override public void fillArc(double x, double y, double w, double h, double s, double a) { }
        @Override public void strokeArc(double x, double y, double w, double h, double s, double a) { }
        @Override public void arc(double x, double y, double w, double h, double s, double a) { }
        @Override public void strokeLine(double x1, double y1, double x2, double y2) { }
        @Override public void fillPolygon(double[] x, double[] y, int n) { }
        @Override public void strokePolygon(double[] x, double[] y, int n) { }
        @Override public void beginPath() { }
        @Override public void moveTo(double x, double y) { }
        @Override public void lineTo(double x, double y) { }
        @Override public void rect(double x, double y, double w, double h) { }
        @Override public void closePath() { }
        @Override public void stroke() { }
        @Override public void fill() { }
        @Override public void clip() { }
        @Override public void fillText(String text, double x, double y) { }
        @Override public void strokeText(String text, double x, double y) { }
    }
}
