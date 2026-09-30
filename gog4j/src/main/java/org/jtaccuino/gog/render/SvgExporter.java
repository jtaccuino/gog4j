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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.GgFigure;

/**
 * Writes a {@link GgFigure} to SVG.
 * <p>
 * The plot is laid out in an off-screen scene and then rendered through
 * {@link GgFigure#renderTo}, the same pipeline that paints the screen — so the vector
 * output matches the on-screen figure by construction.
 *
 * <pre>{@code
 * new SvgExporter().size(1400, 620).write(plot, Path.of("manhattan.svg"));
 * }</pre>
 *
 * <h2>Threading</h2>
 * Layout and text measurement use JavaFX, so an initialised toolkit is required —
 * {@code Platform.startup(...)} is enough, no window is shown. Call from the
 * JavaFX application thread.
 */
public class SvgExporter {

    /**
     * Creates an exporter with default settings which can be overridden
     * fluently before writing ({@link #size}, {@link #background},
     * {@link #batchPoints}, {@link #rasterizeAbove}, {@link #rasterScale},
     * {@link #compress}).
     */
    public SvgExporter() {
    }

    private double width = 1200;
    private double height = 700;
    private Color background = Color.WHITE;
    private boolean batchPoints = true;
    private int rasterizeAbove = 50_000;
    private double rasterScale = 3.0;
    private boolean compress = false;

    /**
     * Sets the size the plot is laid out at.
     *
     * @param width  width in pixels
     * @param height height in pixels
     * @return this exporter for fluid chaining
     */
    public SvgExporter size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets the colour painted behind the figure.
     *
     * @param background the backdrop, or {@code null} for a transparent one
     * @return this exporter for fluid chaining
     */
    public SvgExporter background(Color background) {
        this.background = background;
        return this;
    }

    /**
     * Groups same-styled points so that their shared paint is written once
     * rather than per element. On by default.
     *
     * @param batch {@code true} to batch
     * @return this exporter for fluid chaining
     */
    public SvgExporter batchPoints(boolean batch) {
        this.batchPoints = batch;
        return this;
    }

    /**
     * Sets the primitive count above which a layer is embedded as a raster image
     * rather than emitted as vector shapes, keeping axes and labels vector
     * either way. Pass {@link Integer#MAX_VALUE} to force pure vector output.
     *
     * @param threshold the primitive count
     * @return this exporter for fluid chaining
     */
    public SvgExporter rasterizeAbove(int threshold) {
        this.rasterizeAbove = threshold;
        return this;
    }

    /**
     * Sets the resolution multiplier for rasterised layers.
     *
     * @param scale pixels rendered per output pixel
     * @return this exporter for fluid chaining
     */
    public SvgExporter rasterScale(double scale) {
        this.rasterScale = scale;
        return this;
    }

    /**
     * Writes gzip-compressed SVG ({@code .svgz}), which every browser and most
     * vector editors read directly.
     *
     * @param compress {@code true} to compress
     * @return this exporter for fluid chaining
     */
    public SvgExporter compress(boolean compress) {
        this.compress = compress;
        return this;
    }

    /**
     * Renders a figure and returns the SVG source.
     *
     * @param figure the figure to render
     * @return the SVG document
     */
    public String toSvg(GgFigure figure) {
        // A scene is needed so that layout and CSS resolve; it is never shown.
        var holder = new StackPane();
        holder.setPrefSize(width, height);
        new Scene(holder, width, height);
        if (figure instanceof Node node) {
            holder.getChildren().add(node);
        }
        holder.applyCss();
        holder.layout();

        var surface = new SvgDrawSurface(width, height, background)
                .batchPoints(batchPoints)
                .rasterizeAbove(rasterizeAbove)
                .rasterScale(rasterScale);
        figure.renderTo(surface, width, height);

        // Leave the figure unattached so the caller can render it again elsewhere
        if (figure instanceof Node node) {
            holder.getChildren().remove(node);
        }
        return surface.toSvg();
    }

    /**
     * Renders a figure and writes it to a file.
     *
     * @param figure   the figure to render
     * @param target the destination; {@code .svgz} is written when compression is on
     */
    public void write(GgFigure figure, Path target) {
        var svg = toSvg(figure).getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = compress
                ? new GZIPOutputStream(Files.newOutputStream(target))
                : Files.newOutputStream(target)) {
            out.write(svg);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }

    /**
     * Renders a plot at the given size and writes it to a file.
     *
     * @param figure   the figure to render
     * @param width  width in pixels
     * @param height height in pixels
     * @param target the destination file
     */
    public static void write(GgFigure figure, double width, double height, Path target) {
        new SvgExporter().size(width, height).write(figure, target);
    }
}
