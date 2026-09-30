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
package org.jtaccuino.gog.render;

import static java.awt.Color.WHITE;
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Geoms.function;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.data.EmptyDataFrame;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders {@code Geoms.function()} curves through both backends and compares the
 * results, the same check the density and point tests run. A function curve is
 * a stroked polyline, so both rasterisers must agree on where the stroke lies;
 * anything missing or displaced breaks the ink-coverage and block-delta metrics.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class SvgMatchesCanvasFunctionTest {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static final int INK_THRESHOLD = 235;
    private static final int BLOCK = 20;
    private static final double MAX_BLOCK_DELTA = 25.0;
    private static final double MIN_INK_RATIO = 0.6;
    private static final double MAX_INK_RATIO = 1.4;

    /** A U-shaped wave so the dashed, non-monotonic curve stresses the polyline. */
    private static double curve(double x) {
        return Math.sin(x) * Math.exp(-0.2 * x);
    }

    private static Plot<DataFrame> overlayPlot() {
        var rng = new Random(11L);
        var n = 200;
        var values = new double[n];
        for (var i = 0; i < n; i++) {
            values[i] = rng.nextGaussian();
        }
        var df = DataFrame.byColumn("x").of(Series.ofDouble(values));
        return Ggplot.ggplot(df, Aes.aes().x("x"))
                .geoms(density())
                .geoms(function(SvgMatchesCanvasFunctionTest::curve).color(Color.web("#e31a1c")))
                .guides(Guides.none())
                .labs("SVG function backend check", "x", "density");
    }

    /** No data at all: the plot-level coordinate xlim sizes the x axis. */
    private static Plot<EmptyDataFrame> standalonePlot() {
        return Ggplot.ggplot(Aes.aes())
                .coord(Coord2D.cartesian().xlim(-4, 4))
                .geoms(function(SvgMatchesCanvasFunctionTest::curve).color(Color.web("#3182bd")))
                .guides(Guides.none())
                .labs("SVG function backend check", "x", "density");
    }

    private static BufferedImage renderViaCanvas(Plot<?> plot) throws Exception {
        return onFxThread(() -> {
            var holder = new StackPane(plot);
            holder.setPrefSize(WIDTH, HEIGHT);
            new Scene(holder, WIDTH, HEIGHT);
            holder.applyCss();
            holder.layout();
            plot.redraw();

            var params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            var image = holder.snapshot(params, new WritableImage(WIDTH, HEIGHT));
            return toBufferedImage(image);
        });
    }

    private static String renderToSvg(Plot<?> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }

    private static BufferedImage toBufferedImage(WritableImage image) {
        var w = (int) image.getWidth();
        var h = (int) image.getHeight();
        var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var reader = image.getPixelReader();
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return out;
    }

    private static BufferedImage rasterize(String svg) throws Exception {
        var transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) WIDTH);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) HEIGHT);
        var out = new ByteArrayOutputStream();
        transcoder.transcode(
                new TranscoderInput(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))),
                new TranscoderOutput(out));

        var decoded = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        var flattened = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g = flattened.createGraphics();
        g.setColor(WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.drawImage(decoded, 0, 0, null);
        g.dispose();
        return flattened;
    }

    private static void assertInkMatches(Plot<?> plot, String what) throws Exception {
        var canvasInk = inkFraction(renderViaCanvas(plot));
        var svgInk = inkFraction(rasterize(renderToSvg(plot)));

        assertTrue(canvasInk > 0.005, "the reference rendering itself must contain content");
        var ratio = svgInk / canvasInk;
        assertTrue(ratio >= MIN_INK_RATIO && ratio <= MAX_INK_RATIO,
                   () -> String.format("the %s SVG covers %.2f%% of the figure against the canvas's %.2f%%"
                                       + " (ratio %.2f); a layer is probably missing or drawn twice",
                                       what, svgInk * 100, canvasInk * 100, ratio));
    }

    private static void assertRegionsMatch(Plot<?> plot, String what) throws Exception {
        var fromCanvas = renderViaCanvas(plot);
        var fromSvg = rasterize(renderToSvg(plot));

        var worst = 0.0;
        var worstAt = "";
        for (var by = 0; by + BLOCK <= HEIGHT; by += BLOCK) {
            for (var bx = 0; bx + BLOCK <= WIDTH; bx += BLOCK) {
                var delta = Math.abs(meanBrightness(fromCanvas, bx, by) - meanBrightness(fromSvg, bx, by));
                if (delta > worst) {
                    worst = delta;
                    worstAt = "(" + bx + "," + by + ")";
                }
            }
        }

        assertTrue(worst <= MAX_BLOCK_DELTA,
                   String.format("the %s block at %s differs by %.1f grey levels between the canvas and"
                                 + " the SVG rendering, above the %.0f allowed",
                                 what, worstAt, worst, MAX_BLOCK_DELTA));
    }

    private static double meanBrightness(BufferedImage image, int bx, int by) {
        var sum = 0.0;
        for (var y = by; y < by + BLOCK; y++) {
            for (var x = bx; x < bx + BLOCK; x++) {
                var rgb = image.getRGB(x, y);
                sum += (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3.0;
            }
        }
        return sum / (BLOCK * BLOCK);
    }

    private static double inkFraction(BufferedImage image) {
        var ink = 0;
        for (var y = 0; y < HEIGHT; y++) {
            for (var x = 0; x < WIDTH; x++) {
                var rgb = image.getRGB(x, y);
                var brightness = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                if (brightness < INK_THRESHOLD) ink++;
            }
        }
        return (double) ink / (WIDTH * HEIGHT);
    }

    @Test
    void functionSvgIsAcceptedByAnIndependentRenderer() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        var raster = rasterize(renderToSvg(overlayPlot()));
        assertTrue(raster.getWidth() == WIDTH && raster.getHeight() == HEIGHT,
                   "Batik rendered the declared canvas size");
        var standalone = rasterize(renderToSvg(standalonePlot()));
        assertTrue(standalone.getWidth() == WIDTH && standalone.getHeight() == HEIGHT,
                   "Batik rendered the declared canvas size for the standalone curve");
    }

    @Test
    void functionSvgDrawsAsMuchInkAsTheCanvas() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        assertInkMatches(overlayPlot(), "overlay");
        assertInkMatches(standalonePlot(), "standalone");
    }

    @Test
    void noRegionOfTheFunctionDiffersSubstantially() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        assertRegionsMatch(overlayPlot(), "overlay");
        assertRegionsMatch(standalonePlot(), "standalone");
    }

    /**
     * The numeric y-axis tick labels. The y-axis gutter is the leftmost column
     * of tick numbers (the smallest {@code x} occupied by a numeric label),
     * with the x-axis labels spanning the panel further to the right — so the
     * numeric labels sharing that minimum {@code x} are exactly the y-axis
     * ticks. Non-numeric axis titles may sit even further left and are ignored.
     *
     * @return the parsed y tick values, in ascending order
     */
    private static double[] yAxisTicks(Plot<?> plot) throws Exception {
        var svg = renderToSvg(plot);
        var matcher = Pattern.compile("<text x=\"([0-9.]+)\"[^>]*>([^<]+)</text>").matcher(svg);
        var gutterX = Double.POSITIVE_INFINITY;
        var labels = new HashMap<Double, List<Double>>();
        while (matcher.find()) {
            var label = matcher.group(2).trim();
            if (label.matches("-?[0-9.]+(?:e[+-]?[0-9]+)?")) {
                var x = Double.parseDouble(matcher.group(1));
                gutterX = Math.min(gutterX, x);
                labels.computeIfAbsent(x, k -> new ArrayList<>())
                        .add(Double.parseDouble(label));
            }
        }
        if (Double.isInfinite(gutterX)) {
            return new double[0];
        }
        var gutter = labels.get(gutterX);
        double[] ticks = new double[gutter.size()];
        for (var i = 0; i < gutter.size(); i++) {
            ticks[i] = gutter.get(i);
        }
        Arrays.sort(ticks);
        return ticks;
    }

    @Test
    void dataLessPlotYAxisIsFittedToTheFunction() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        var ticks = yAxisTicks(standalonePlot());
        assertTrue(ticks.length > 0,
                   "the standalone plot should render numeric y-axis ticks");
        assertTrue(ticks[0] < 0.0,
                   "the standalone curve's y axis should extend below zero to fit the function's "
                   + "negative lobe, but the lowest y tick was " + ticks[0]);
    }
}
