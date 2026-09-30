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
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the same density plot through both backends and compares the results,
 * the same check {@link SvgMatchesCanvasTest} runs for points and reference
 * lines. A density is a filled polygon whose silhouette is defined by a
 * kernel-density estimate, so the two rasterisers must agree on where the
 * filled area sits; anything missing or displaced breaks the ink-coverage and
 * block-delta metrics.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class SvgMatchesCanvasDensityTest {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static final int INK_THRESHOLD = 235;
    private static final int BLOCK = 20;
    private static final double MAX_BLOCK_DELTA = 20.0;
    private static final double MIN_INK_RATIO = 0.6;
    private static final double MAX_INK_RATIO = 1.4;

    /** Three groups drawn from distinct distributions, so each density differs. */
    private static DataFrame sampleData() {
        var rng = new Random(11L);
        var n = 400;
        var x = new String[n];
        var y = new double[n];
        for (var i = 0; i < n; i++) {
            var group = i % 3;
            x[i] = switch (group) {
                case 0 -> "a";
                case 1 -> "b";
                default -> "c";
            };
            // The density trace must be visibly asymmetric and grouped
            y[i] = 30 * (i % 2) + rng.nextGaussian() * (3.0 + group);
        }
        return DataFrame.byColumn("x", "y")
                .of(Series.of(x), Series.ofDouble(y));
    }

    private static Plot<DataFrame> samplePlot() {
        return Ggplot.ggplot(sampleData(),
                             Aes.aes().x("y").fill("x"))
                .geoms(density().alpha(0.4))
                .guides(Guides.none())
                .labs("SVG density backend check", "value", "density");
    }

    /** Same distributions mapped on y, so the estimate runs along x. */
    private static Plot<DataFrame> sampleFlippedPlot() {
        return Ggplot.ggplot(sampleData(),
                             Aes.aes().y("y").fill("x"))
                .geoms(density().alpha(0.4))
                .guides(Guides.none())
                .labs("SVG density backend check", "density", "value");
    }

    private static BufferedImage renderViaCanvas(Plot<DataFrame> plot) throws Exception {
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

    private static String renderToSvg(Plot<DataFrame> plot) throws Exception {
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

    @Test
    void densitySvgIsAcceptedByAnIndependentRenderer() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // Batik parses and rasterises strictly; anything malformed throws here.
        var raster = rasterize(renderToSvg(samplePlot()));
        assertTrue(raster.getWidth() == WIDTH && raster.getHeight() == HEIGHT,
                   "Batik rendered the declared canvas size");
        var flipped = rasterize(renderToSvg(sampleFlippedPlot()));
        assertTrue(flipped.getWidth() == WIDTH && flipped.getHeight() == HEIGHT,
                   "Batik rendered the declared canvas size for the flipped orientation");
    }

    @Test
    void densitySvgDrawsAsMuchInkAsTheCanvas() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        assertInkMatches(samplePlot(), "normal");
        assertInkMatches(sampleFlippedPlot(), "flipped");
    }

    @Test
    void noRegionOfTheDensityDiffersSubstantially() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        assertRegionsMatch(samplePlot(), "normal");
        assertRegionsMatch(sampleFlippedPlot(), "flipped");
    }

    @Test
    void unmappedDensityAxisIsFittedToTheEstimate() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // The density axis is computed, not read from a mapped column, so the
        // y axis must be fitted to the estimate's peak (here ~0.13) instead of
        // being stuck on the plot's neutral 0..1 seed, which would squash the
        // curve to the baseline (the diagonal-cell defect from the pairs plot).
        var ticks = yAxisTicks(samplePlot());
        assertTrue(ticks.length > 0, "the density plot should render numeric y-axis ticks");
        assertTrue(ticks[ticks.length - 1] < 0.25,
                   "the density axis must fit the estimate's peak, but the largest y tick was "
                   + ticks[ticks.length - 1]);
    }

    /**
     * The numeric y-axis tick labels: the leftmost column of tick numbers, with
     * the x-axis labels spanning the panel further to the right.
     *
     * @return the parsed y tick values, in ascending order
     */
    private static double[] yAxisTicks(Plot<DataFrame> plot) throws Exception {
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

    private static void assertInkMatches(Plot<DataFrame> plot, String what) throws Exception {
        var canvasInk = inkFraction(renderViaCanvas(plot));
        var svgInk = inkFraction(rasterize(renderToSvg(plot)));

        assertTrue(canvasInk > 0.005, "the reference rendering itself must contain content");
        var ratio = svgInk / canvasInk;
        assertTrue(ratio >= MIN_INK_RATIO && ratio <= MAX_INK_RATIO,
                   String.format("the %s SVG covers %.2f%% of the figure against the canvas's %.2f%%"
                                 + " (ratio %.2f); a layer is probably missing or drawn twice",
                                 what, svgInk * 100, canvasInk * 100, ratio));
    }

    private static void assertRegionsMatch(Plot<DataFrame> plot, String what) throws Exception {
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
}
