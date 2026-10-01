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
import static org.jtaccuino.gog.Geoms.hline;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.vline;
import static org.jtaccuino.gog.Guides.guide;
import static org.jtaccuino.gog.Guides.guideColorbar;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.Ggplot;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.examples.dflib.SeattleWeatherPlots;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.jtaccuino.gog.theme.GuidePosition;
import org.jtaccuino.gog.theme.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Renders the same plot through both backends and compares the results, which is
 * the only check that catches an SVG backend that is well-formed but draws the
 * wrong picture.
 * <p>
 * The two rasterisers are different engines, so glyph shapes and antialiasing
 * never match exactly; the test asserts that the overwhelming majority of pixels
 * agree, which is enough to catch missing layers, misplaced geometry, wrong
 * colours, or a broken clip.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class SvgMatchesCanvasTest {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    /** Mean brightness below which a pixel counts as carrying ink rather than background. */
    private static final int INK_THRESHOLD = 235;

    /**
     * Side of the blocks the two images are averaged over before comparison.
     * Averaging discards the glyph-shape and antialiasing differences that two
     * independent rasterisers will always have, while keeping the metric
     * sensitive to content that is missing or in the wrong place.
     */
    private static final int BLOCK = 20;

    /**
     * Largest mean-brightness difference any block may show, in grey levels.
     * <p>
     * Measured headroom on this plot: the real export reaches 12.4, a blank
     * image 34.4, and shifting the reference by ten pixels 22.9. The bound sits
     * above the honest difference and below every failure mode checked.
     */
    private static final double MAX_BLOCK_DELTA = 20.0;

    /**
     * First block row below the title, where the plot panel starts. Glyph
     * antialiasing for the large title differs between the two rasterisers by
     * more than {@link #MAX_BLOCK_DELTA} allows, which says nothing about the
     * axes, so the timestamp comparison starts below it.
     */
    private static final int TITLE_BAND = 40;

    /** How far the exported ink coverage may drift from the canvas rendering. */
    private static final double MIN_INK_RATIO = 0.6;
    private static final double MAX_INK_RATIO = 1.4;

    /** A small frame with two groups, enough to exercise colours, clipping, and axes. */
    private static DataFrame sampleData() {
        var n = 60;
        var x = new double[n];
        var y = new double[n];
        var g = new String[n];
        for (var i = 0; i < n; i++) {
            x[i] = i;
            y[i] = (i % 17) * 3.0 + (i % 5);
            g[i] = i % 2 == 0 ? "a" : "b";
        }
        return DataFrame.byColumn("x", "y", "g")
                .of(Series.ofDouble(x), Series.ofDouble(y), Series.of(g));
    }

    private static Plot<DataFrame> samplePlot() {
        return Ggplot.ggplot(sampleData(),
                             Aes.aes().x("x").y("y").color("g"))
                .geoms(
                    point().size(6.0),
                    hline(20.0).color(Color.web("#c0392b")).dashed(),
                    vline(30.0).color(Color.web("#2e86c1")).dashed()
                )
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs("SVG backend check", "x axis", "y axis");
    }

    private static BufferedImage renderViaCanvas() throws Exception {
        return renderViaCanvas(samplePlot());
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

    private static String renderToSvg() throws Exception {
        return renderToSvg(samplePlot());
    }

    private static String renderToSvg(Plot<DataFrame> plot) throws Exception {
        return onFxThread(() -> new SvgExporter().size(WIDTH, HEIGHT).toSvg(plot));
    }

    /**
     * A day of hourly readings against a measurement, so the x axis has to break
     * at hours and print date-time labels rather than raw numbers.
     */
    private static DataFrame hourlyData() {
        var n = 24;
        var when = new LocalDateTime[n];
        var reading = new double[n];
        var start = LocalDateTime.of(2010, 1, 1, 0, 0);
        for (var i = 0; i < n; i++) {
            when[i] = start.plusHours(i);
            reading[i] = 4.0 + 3.0 * Math.sin(i / 3.0);
        }
        return DataFrame.byColumn("when", "reading")
                .of(Series.of(when), Series.ofDouble(reading));
    }

    private static Plot<DataFrame> timestampPlot() {
        return Ggplot.ggplot(hourlyData(), Aes.aes().x("when").y("reading"))
                .geoms(line())
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs("hourly readings", "when", "reading");
    }

    private static Plot<DataFrame> samplePlotWithGuides(GuidePosition position) {
        return Ggplot.ggplot(sampleData(),
                             Aes.aes().x("x").y("y").color("g"))
                .geoms(point().size(6.0))
                .theme(Theme.derive(Theme.theme_gray()).guidePosition(position))
                .labs("SVG guide check", "x axis", "y axis");
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
        // Flatten onto white so transparency does not read as a difference
        var flattened = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g = flattened.createGraphics();
        g.setColor(WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.drawImage(decoded, 0, 0, null);
        g.dispose();
        return flattened;
    }

    @Test
    void exportedSvgIsAcceptedByAnIndependentRenderer() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // Batik parses and rasterises strictly; anything malformed throws here.
        var raster = rasterize(renderToSvg());
        assertTrue(raster.getWidth() == WIDTH && raster.getHeight() == HEIGHT,
                   "Batik rendered the declared canvas size");
    }

    @Test
    void theExportDrawsAsMuchInkAsTheCanvas() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        var canvasInk = inkFraction(renderViaCanvas());
        var svgInk = inkFraction(rasterize(renderToSvg()));

        assertTrue(canvasInk > 0.005, "the reference rendering itself must contain content");
        var ratio = svgInk / canvasInk;
        assertTrue(ratio >= MIN_INK_RATIO && ratio <= MAX_INK_RATIO,
                   () -> String.format("the SVG covers %.2f%% of the figure against the canvas's %.2f%%"
                                      + " (ratio %.2f); a layer is probably missing or drawn twice",
                                      svgInk * 100, canvasInk * 100, ratio));
    }

    @Test
    void noRegionOfTheFigureDiffersSubstantially() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        var fromCanvas = renderViaCanvas();
        var fromSvg = rasterize(renderToSvg());

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

        double worstDelta = worst;
        String worstBlock = worstAt;
        assertTrue(worstDelta <= MAX_BLOCK_DELTA,
                   () -> String.format("the block at %s differs by %.1f grey levels between the canvas and"
                                       + " the SVG rendering, above the %.0f allowed",
                                       worstBlock, worstDelta, MAX_BLOCK_DELTA));
    }

    @Test
    void timestampAxisMatchesBetweenBackends() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // The point is the x axis: a timestamp axis emits its own break and
        // label code, so the two backends have to agree on where the breaks and
        // their text land, not merely on how much ink is drawn.
        var fromCanvas = renderViaCanvas(timestampPlot());
        var fromSvg = rasterize(renderToSvg(timestampPlot()));

        var worst = 0.0;
        var worstAt = "";
        for (var by = TITLE_BAND; by + BLOCK <= HEIGHT; by += BLOCK) {
            for (var bx = 0; bx + BLOCK <= WIDTH; bx += BLOCK) {
                var delta = Math.abs(meanBrightness(fromCanvas, bx, by) - meanBrightness(fromSvg, bx, by));
                if (delta > worst) {
                    worst = delta;
                    worstAt = "(" + bx + "," + by + ")";
                }
            }
        }

        double worstDelta = worst;
        String worstBlock = worstAt;
        assertTrue(inkFraction(fromCanvas) > 0.005, "the reference rendering itself must contain content");

        assertTrue(worstDelta <= MAX_BLOCK_DELTA,
                   () -> String.format("the timestamp plot's block at %s differs by %.1f grey levels between"
                                       + " the canvas and the SVG rendering, above the %.0f allowed",
                                       worstBlock, worstDelta, MAX_BLOCK_DELTA));
    }

    @Test
    void aTemporalColumnOnTheVerticalAxisRenders() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // A line with the timestamp on y, not x: the averaging path must resolve
        // the temporal position through the scale, not cast it to a Number.
        var plot = SeattleWeatherPlots.createFlippedTemperature();
        var svg = renderToSvg(plot);
        assertTrue(svg.contains("<path"), "the line must project to SVG path geometry");
        assertNotNull(renderViaCanvas(plot), "the canvas backend must render the same figure");
    }

    @Test
    void positionedGuidesMatchBetweenBackends() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        for (var position : new GuidePosition[]{GuidePosition.LEFT, GuidePosition.TOP, GuidePosition.RIGHT}) {
            var plot = samplePlotWithGuides(position);
            var fromCanvas = renderViaCanvas(plot);
            var fromSvg = rasterize(renderToSvg(plot));

            var worst = 0.0;
            for (var by = 0; by + BLOCK <= HEIGHT; by += BLOCK) {
                for (var bx = 0; bx + BLOCK <= WIDTH; bx += BLOCK) {
                    var delta = Math.abs(meanBrightness(fromCanvas, bx, by) - meanBrightness(fromSvg, bx, by));
                    if (delta > worst) {
                        worst = delta;
                    }
                }
            }
            double worstDelta = worst;
            assertTrue(worstDelta <= MAX_BLOCK_DELTA,
                       () -> String.format("the %s legend differs by %.1f grey levels between the canvas and"
                                           + " the SVG rendering, above the %.0f allowed",
                                           position, worstDelta, MAX_BLOCK_DELTA));
        }
    }

    @Test
    void themeDefaultBottomColorbarFlowsHorizontally() throws Exception {
        assumeTrue(JavaFxToolkitExtension.isToolkitUp());
        // A continuous colour guide on the bottom strip resolves its direction
        // from the theme's default position, not a per-guide override. It must
        // therefore render as a wide bar, not a tall one, so it uses the strip's
        // horizontal space following convention.
        var plot = Ggplot.ggplot(sampleData(), Aes.aes().x("x").y("y").color("x"))
                .geoms(point().size(6.0))
                .guides(guide(Aesthetic.COLOR, guideColorbar()))
                .theme(t -> t.guidePosition(GuidePosition.BOTTOM))
                .labs("bottom colourbar", "x axis", "y axis");
        var svg = renderToSvg(plot);

        var bar = Pattern
                .compile("<rect x=\\S+ y=\\S+ width=\"([0-9.]+)\" height=\"([0-9.]+)\" fill=\"url\\(#[a-zA-Z0-9]+\\)\"/>")
                .matcher(svg);
        var matched = new ArrayList<String>();
        while (bar.find()) {
            matched.add(bar.group());
            var w = Double.parseDouble(bar.group(1));
            var h = Double.parseDouble(bar.group(2));
            assertTrue(w > h, () -> "the bottom colourbar should be a horizontal bar, but drew a " + w + "x" + h + " rect");
        }
        assertFalse(matched.isEmpty(), "expected at least one colourbar bar to be drawn");
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
