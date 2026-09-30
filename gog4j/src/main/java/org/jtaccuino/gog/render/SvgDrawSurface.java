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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javax.imageio.ImageIO;

/**
 * A {@link DrawSurface} that writes SVG.
 * <p>
 * Output is assembled in memory and retrieved with {@link #toSvg()}. The surface
 * mirrors the canvas state model: {@link #save()} and {@link #restore()} bracket
 * paint, font, transform, and clip, with transforms and clips realised as nested
 * {@code <g>} elements so that rotated text and panel clipping survive into the
 * vector output.
 *
 * <h2>Keeping the file usable</h2>
 * A scatter plot of half a million points would naively become half a million
 * elements and tens of megabytes. Two mechanisms prevent that:
 * <ul>
 *   <li><b>Point batching</b> — consecutive circles sharing a style are emitted
 *       as {@code <circle>} children of one styled {@code <g>}, which removes the
 *       repeated per-element paint attributes.</li>
 *   <li><b>Hybrid rasterisation</b> — a layer whose estimated primitive count
 *       exceeds {@link #rasterizeAbove(int)} is drawn to an off-screen canvas and
 *       embedded as a PNG {@code <image>}, while axes, grid, labels, and legend
 *       stay vector. This is the same trade-off a rasterized-vector trade-off offers as
 *       {@code rasterized=True}.</li>
 * </ul>
 */
public class SvgDrawSurface implements DrawSurface {

    private static final int DEFAULT_RASTER_THRESHOLD = 50_000;
    private static final double DEFAULT_RASTER_SCALE = 3.0;

    /** Below this diameter a point's outline is indistinguishable from its fill. */
    private static final double HAIRLINE_DIAMETER = 2.0;

    private final double width;
    private final double height;
    private final Color background;

    private final StringBuilder defs = new StringBuilder();
    private final StringBuilder body = new StringBuilder();

    private boolean batchPoints = true;
    private int rasterThreshold = DEFAULT_RASTER_THRESHOLD;
    private double rasterScale = DEFAULT_RASTER_SCALE;

    private int idCounter = 0;
    private int openGroups = 0;

    // --- Canvas-equivalent state ---
    private Paint fill = Color.BLACK;
    private Paint stroke = Color.BLACK;
    private double lineWidth = 1.0;
    private double[] dashes = null;
    private Font font = Font.getDefault();
    private TextAlignment textAlign = TextAlignment.LEFT;
    private VPos textBaseline = VPos.BASELINE;

    private final Deque<State> stateStack = new ArrayDeque<>();
    private final StringBuilder path = new StringBuilder();

    // --- Point batching ---
    private final List<double[]> dotBuffer = new ArrayList<>();
    private DotStyle dotStyle = null;
    private double[] pendingFill = null;
    private SymbolKind pendingKind = null;

    /** The symbol shapes that batching understands; both are drawn fill-then-outline. */
    private enum SymbolKind { ELLIPSE, RECT }

    // --- Hybrid rasterisation ---
    private Canvas rasterCanvas = null;
    private double[] rasterBounds = null;

    @SuppressWarnings("ArrayRecordComponent") // dashes is cloned on entry and never mutated afterwards
    private record State(Paint fill, Paint stroke, double lineWidth, double[] dashes,
                         Font font, TextAlignment align, VPos baseline, int openGroups) {
    }

    private record DotStyle(SymbolKind kind, Paint fill, Paint stroke, double lineWidth,
                            double w, double h, boolean stroked) {
    }

    /**
     * Creates an SVG drawing surface of the given dimensions.
     *
     * @param width      the drawing width in pixels
     * @param height     the drawing height in pixels
     * @param background the colour painted behind everything, or {@code null} for transparent
     */
    public SvgDrawSurface(double width, double height, Color background) {
        this.width = width;
        this.height = height;
        this.background = background;
    }

    /**
     * Enables or disables grouping of same-styled points.
     *
     * @param batch {@code true} to batch points into styled groups
     * @return this surface for fluid chaining
     */
    public SvgDrawSurface batchPoints(boolean batch) {
        this.batchPoints = batch;
        return this;
    }

    /**
     * Sets the primitive count above which a layer is embedded as a raster image
     * instead of vector shapes. Pass {@link Integer#MAX_VALUE} to always emit
     * vectors.
     *
     * @param threshold the primitive count
     * @return this surface for fluid chaining
     */
    public SvgDrawSurface rasterizeAbove(int threshold) {
        this.rasterThreshold = threshold;
        return this;
    }

    /**
     * Sets the resolution multiplier used when a layer is rasterised.
     *
     * @param scale pixels rendered per output pixel
     * @return this surface for fluid chaining
     */
    public SvgDrawSurface rasterScale(double scale) {
        this.rasterScale = scale;
        return this;
    }

    /**
     * Completes the document and returns it.
     *
     * @return the SVG source
     */
    public String toSvg() {
        flushDots();
        var out = new StringBuilder(body.length() + defs.length() + 512);
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
           .append("version=\"1.1\" width=\"").append(num(width)).append("\" height=\"").append(num(height))
           .append("\" viewBox=\"0 0 ").append(num(width)).append(' ').append(num(height)).append("\">\n");
        if (!defs.isEmpty()) {
            out.append("<defs>\n").append(defs).append("</defs>\n");
        }
        if (background != null) {
            out.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(paint(background)).append("\"/>\n");
        }
        out.append(body);
        // Close anything the caller left open rather than emit malformed XML
        for (var i = 0; i < openGroups; i++) {
            out.append("</g>\n");
        }
        out.append("</svg>\n");
        return out.toString();
    }

    // --- Paint state ---------------------------------------------------------

    @Override
    public void setFill(Paint paint) {
        if (!Objects.equals(this.fill, paint)) flushDots();
        this.fill = paint;
    }

    @Override
    public void setStroke(Paint paint) {
        if (!Objects.equals(this.stroke, paint)) flushDots();
        this.stroke = paint;
    }

    @Override
    public void setLineWidth(double width) {
        if (this.lineWidth != width) flushDots();
        this.lineWidth = width;
    }

    @Override
    public void setLineDashes(double... dashes) {
        this.dashes = dashes == null || dashes.length == 0 ? null : dashes.clone();
    }

    @Override public void setFont(Font font) { this.font = font; }
    @Override public Font getFont() { return font; }
    @Override public void setTextAlign(TextAlignment alignment) { this.textAlign = alignment; }
    @Override public void setTextBaseline(VPos baseline) { this.textBaseline = baseline; }

    // --- State stack ---------------------------------------------------------

    @Override
    public void save() {
        stateStack.push(new State(fill, stroke, lineWidth, dashes, font, textAlign, textBaseline, openGroups));
    }

    @Override
    public void restore() {
        if (stateStack.isEmpty()) return;
        flushDots();
        var s = stateStack.pop();
        while (openGroups > s.openGroups()) {
            body.append("</g>\n");
            openGroups--;
        }
        fill = s.fill();
        stroke = s.stroke();
        lineWidth = s.lineWidth();
        dashes = s.dashes();
        font = s.font();
        textAlign = s.align();
        textBaseline = s.baseline();
    }

    @Override
    public void translate(double x, double y) {
        flushDots();
        body.append("<g transform=\"translate(").append(num(x)).append(',').append(num(y)).append(")\">\n");
        openGroups++;
    }

    @Override
    public void rotate(double degrees) {
        flushDots();
        body.append("<g transform=\"rotate(").append(num(degrees)).append(")\">\n");
        openGroups++;
    }

    @Override
    public void scale(double sx, double sy) {
        flushDots();
        body.append("<g transform=\"scale(").append(num(sx)).append(' ').append(num(sy)).append(")\">\n");
        openGroups++;
    }

    // --- Shapes --------------------------------------------------------------

    @Override
    public void fillRect(double x, double y, double w, double h) {
        if (batchPoints) {
            flushPendingFill();
            pendingFill = new double[]{x, y, w, h};
            pendingKind = SymbolKind.RECT;
            return;
        }
        emitRect(x, y, w, h, fillAttrs());
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        if (matchesPendingFill(SymbolKind.RECT, x, y, w, h)) {
            bufferSymbol(SymbolKind.RECT, x, y, w, h, true);
            return;
        }
        flushPendingFill();
        emitRect(x, y, w, h, "fill=\"none\" " + strokeAttrs());
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        if (batchPoints) {
            // A fill not followed by a matching outline is a symbol in its own right
            flushPendingFill();
            pendingFill = new double[]{x, y, w, h};
            pendingKind = SymbolKind.ELLIPSE;
            return;
        }
        emitEllipse(x, y, w, h, fillAttrs());
    }

    @Override
    public void strokeOval(double x, double y, double w, double h) {
        if (matchesPendingFill(SymbolKind.ELLIPSE, x, y, w, h)) {
            // The canonical point: a filled shape immediately outlined. Recorded
            // as one symbol so the pair costs a single element, not two.
            bufferSymbol(SymbolKind.ELLIPSE, x, y, w, h, true);
            return;
        }
        flushPendingFill();
        emitEllipse(x, y, w, h, "fill=\"none\" " + strokeAttrs());
    }

    private boolean matchesPendingFill(SymbolKind kind, double x, double y, double w, double h) {
        return batchPoints && pendingFill != null && pendingKind == kind
                && pendingFill[0] == x && pendingFill[1] == y
                && pendingFill[2] == w && pendingFill[3] == h;
    }

    @Override
    public void strokeLine(double x1, double y1, double x2, double y2) {
        flushDots();
        body.append("<line x1=\"").append(num(x1)).append("\" y1=\"").append(num(y1))
            .append("\" x2=\"").append(num(x2)).append("\" y2=\"").append(num(y2))
            .append("\" ").append(strokeAttrs()).append("/>\n");
    }

    @Override
    public void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        flushDots();
        if (arcExtent >= 360 || arcExtent <= -360) {
            emitEllipse(x, y, w, h, fillAttrs());
            return;
        }
        var cx = x + w / 2.0;
        var cy = y + h / 2.0;
        var rx = w / 2.0;
        var ry = h / 2.0;
        var start = Math.toRadians(startAngle);
        var sx = cx + rx * Math.cos(start);
        var sy = cy + ry * Math.sin(start);
        body.append("<path d=\"M").append(num(cx)).append(' ').append(num(cy))
            .append(" L").append(num(sx)).append(' ').append(num(sy)).append(' ')
            .append(Arcs.arcData(cx, cy, rx, ry, startAngle, arcExtent)).append("Z\" ")
            .append(fillAttrs()).append("/>\n");
    }

    @Override
    public void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        flushDots();
        if (arcExtent >= 360 || arcExtent <= -360) {
            emitEllipse(x, y, w, h, "fill=\"none\" " + strokeAttrs());
            return;
        }
        var cx = x + w / 2.0;
        var cy = y + h / 2.0;
        var rx = w / 2.0;
        var ry = h / 2.0;
        var start = Math.toRadians(startAngle);
        var sx = cx + rx * Math.cos(start);
        var sy = cy + ry * Math.sin(start);
        body.append("<path d=\"M").append(num(sx)).append(' ').append(num(sy)).append(' ')
            .append(Arcs.arcData(cx, cy, rx, ry, startAngle, arcExtent))
            .append("\" fill=\"none\" ").append(strokeAttrs()).append("/>\n");
    }

    @Override
    public void arc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        var cx = x + w / 2.0;
        var cy = y + h / 2.0;
        path.append(Arcs.arcData(cx, cy, w / 2.0, h / 2.0, startAngle, arcExtent));
    }

    @Override
    public void fillPolygon(double[] xPoints, double[] yPoints, int n) {
        flushDots();
        body.append("<polygon points=\"").append(points(xPoints, yPoints, n))
            .append("\" ").append(fillAttrs()).append("/>\n");
    }

    @Override
    public void strokePolygon(double[] xPoints, double[] yPoints, int n) {
        flushDots();
        body.append("<polygon points=\"").append(points(xPoints, yPoints, n))
            .append("\" fill=\"none\" ").append(strokeAttrs()).append("/>\n");
    }

    // --- Paths ---------------------------------------------------------------

    @Override public void beginPath() { path.setLength(0); }

    @Override
    public void moveTo(double x, double y) {
        path.append('M').append(num(x)).append(' ').append(num(y)).append(' ');
    }

    @Override
    public void lineTo(double x, double y) {
        path.append('L').append(num(x)).append(' ').append(num(y)).append(' ');
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        path.append('M').append(num(x)).append(' ').append(num(y))
            .append('h').append(num(w)).append('v').append(num(h))
            .append('h').append(num(-w)).append("Z ");
    }

    @Override public void closePath() { path.append("Z "); }

    @Override
    public void stroke() {
        flushDots();
        if (path.isEmpty()) return;
        body.append("<path d=\"").append(path.toString().trim())
            .append("\" fill=\"none\" ").append(strokeAttrs()).append("/>\n");
    }

    @Override
    public void fill() {
        flushDots();
        if (path.isEmpty()) return;
        body.append("<path d=\"").append(path.toString().trim())
            .append("\" ").append(fillAttrs()).append("/>\n");
    }

    @Override
    public void clip() {
        flushDots();
        if (path.isEmpty()) return;
        var id = "clip" + idCounter++;
        defs.append("<clipPath id=\"").append(id).append("\"><path d=\"")
            .append(path.toString().trim()).append("\"/></clipPath>\n");
        body.append("<g clip-path=\"url(#").append(id).append(")\">\n");
        openGroups++;
    }

    // --- Text ----------------------------------------------------------------

    @Override
    public void fillText(String text, double x, double y) {
        flushDots();
        emitText(text, x, y, fillAttrs());
    }

    @Override
    public void strokeText(String text, double x, double y) {
        flushDots();
        emitText(text, x, y, "fill=\"none\" " + strokeAttrs() + " stroke-linejoin=\"round\"");
    }

    // --- Backend hooks -------------------------------------------------------

    @Override
    public DrawSurface beginLayer(int estimatedPrimitives, double x, double y, double w, double h) {
        if (estimatedPrimitives <= rasterThreshold || w <= 0 || h <= 0) {
            return this;
        }
        flushDots();
        rasterBounds = new double[]{x, y, w, h};
        rasterCanvas = new Canvas(Math.ceil(w * rasterScale), Math.ceil(h * rasterScale));
        var gc = rasterCanvas.getGraphicsContext2D();
        // Map plot coordinates onto the off-screen canvas
        gc.scale(rasterScale, rasterScale);
        gc.translate(-x, -y);
        return new FxDrawSurface(gc);
    }

    @Override
    public void endLayer() {
        if (rasterCanvas == null) return;
        var params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        var image = rasterCanvas.snapshot(params,
                new WritableImage((int) rasterCanvas.getWidth(), (int) rasterCanvas.getHeight()));

        body.append("<image x=\"").append(num(rasterBounds[0])).append("\" y=\"").append(num(rasterBounds[1]))
            .append("\" width=\"").append(num(rasterBounds[2])).append("\" height=\"").append(num(rasterBounds[3]))
            .append("\" preserveAspectRatio=\"none\" xlink:href=\"data:image/png;base64,")
            .append(encodePng(image)).append("\"/>\n");

        rasterCanvas = null;
        rasterBounds = null;
    }

    // --- Internals -----------------------------------------------------------

    private void bufferSymbol(SymbolKind kind, double x, double y, double w, double h, boolean stroked) {
        var style = new DotStyle(kind, fill, stroke, lineWidth, w, h,
                                 stroked && Math.min(w, h) > HAIRLINE_DIAMETER);
        if (dotStyle != null && !dotStyle.equals(style)) {
            flushDots();
        }
        dotStyle = style;
        pendingFill = null;
        pendingKind = null;
        dotBuffer.add(new double[]{x, y});
    }

    private void flushPendingFill() {
        if (pendingFill == null) return;
        var o = pendingFill;
        var kind = pendingKind;
        pendingFill = null;
        pendingKind = null;
        bufferSymbol(kind, o[0], o[1], o[2], o[3], false);
    }

    private void flushDots() {
        flushPendingFill();
        if (dotBuffer.isEmpty() || dotStyle == null) {
            dotBuffer.clear();
            dotStyle = null;
            return;
        }

        var attrs = new StringBuilder(paintAttr("fill", dotStyle.fill()));
        if (dotStyle.stroked()) {
            attrs.append(' ').append(paintAttr("stroke", dotStyle.stroke()))
                 .append(" stroke-width=\"").append(num(dotStyle.lineWidth())).append('"');
        }

        // A lone symbol carries its own paint; a run of them shares one styled
        // group, which drops the per-element cost to the coordinates alone.
        var grouped = dotBuffer.size() > 1;
        if (grouped) {
            body.append("<g ").append(attrs).append(">");
        }
        for (var c : dotBuffer) {
            var inline = grouped ? "" : " " + attrs;
            if (dotStyle.kind() == SymbolKind.RECT) {
                body.append("<rect x=\"").append(num(c[0])).append("\" y=\"").append(num(c[1]))
                    .append("\" width=\"").append(num(dotStyle.w())).append("\" height=\"").append(num(dotStyle.h()))
                    .append('"').append(inline).append("/>");
            } else {
                var rx = dotStyle.w() / 2.0;
                var ry = dotStyle.h() / 2.0;
                var cx = c[0] + rx;
                var cy = c[1] + ry;
                if (rx == ry) {
                    body.append("<circle cx=\"").append(num(cx)).append("\" cy=\"").append(num(cy))
                        .append("\" r=\"").append(num(rx)).append('"').append(inline).append("/>");
                } else {
                    body.append("<ellipse cx=\"").append(num(cx)).append("\" cy=\"").append(num(cy))
                        .append("\" rx=\"").append(num(rx)).append("\" ry=\"").append(num(ry))
                        .append('"').append(inline).append("/>");
                }
            }
        }
        if (grouped) {
            body.append("</g>");
        }
        body.append('\n');

        dotBuffer.clear();
        dotStyle = null;
    }

    private void emitRect(double x, double y, double w, double h, String attrs) {
        flushDots();
        body.append("<rect x=\"").append(num(x)).append("\" y=\"").append(num(y))
            .append("\" width=\"").append(num(w)).append("\" height=\"").append(num(h))
            .append("\" ").append(attrs).append("/>\n");
    }

    private void emitEllipse(double x, double y, double w, double h, String attrs) {
        flushDots();
        body.append("<ellipse cx=\"").append(num(x + w / 2.0)).append("\" cy=\"").append(num(y + h / 2.0))
            .append("\" rx=\"").append(num(w / 2.0)).append("\" ry=\"").append(num(h / 2.0))
            .append("\" ").append(attrs).append("/>\n");
    }

    private void emitText(String text, double x, double y, String paintAttrs) {
        body.append("<text x=\"").append(num(x)).append("\" y=\"").append(num(y))
            .append("\" font-family=\"").append(escape(fontFamily())).append('"')
            .append(" font-size=\"").append(num(font.getSize())).append('"');
        if (isBold()) {
            body.append(" font-weight=\"bold\"");
        }
        var anchor = switch (textAlign) {
            case CENTER -> "middle";
            case RIGHT -> "end";
            default -> "start";
        };
        body.append(" text-anchor=\"").append(anchor).append('"');
        var baseline = switch (textBaseline) {
            case TOP -> "hanging";
            case CENTER -> "central";
            case BOTTOM -> "text-after-edge";
            default -> null;
        };
        if (baseline != null) {
            body.append(" dominant-baseline=\"").append(baseline).append('"');
        }
        body.append(' ').append(paintAttrs).append(" paint-order=\"stroke\">")
            .append(escape(text)).append("</text>\n");
    }

    private String fontFamily() {
        var family = font.getFamily();
        // "System" is a JavaFX alias with no meaning to an SVG renderer
        return "System".equalsIgnoreCase(family) ? "sans-serif" : family;
    }

    private boolean isBold() {
        // JavaFX exposes weight only through the style and name strings
        var style = font.getStyle() == null ? "" : font.getStyle();
        var name = font.getName() == null ? "" : font.getName();
        return style.toLowerCase(Locale.ROOT).contains("bold")
                || name.toLowerCase(Locale.ROOT).contains("bold");
    }

    private String fillAttrs() {
        return paintAttr("fill", fill);
    }

    private String strokeAttrs() {
        var s = new StringBuilder(paintAttr("stroke", stroke));
        s.append(" stroke-width=\"").append(num(lineWidth)).append('"');
        if (dashes != null) {
            s.append(" stroke-dasharray=\"");
            for (var i = 0; i < dashes.length; i++) {
                if (i > 0) s.append(',');
                s.append(num(dashes[i]));
            }
            s.append('"');
        }
        return s.toString();
    }

    private String paintAttr(String name, Paint paint) {
        if (paint instanceof Color c) {
            var attr = name + "=\"" + colorHex(c) + "\"";
            if (c.getOpacity() < 1.0) {
                attr += " " + name + "-opacity=\"" + num(c.getOpacity()) + "\"";
            }
            return attr;
        }
        if (paint instanceof LinearGradient g) {
            return name + "=\"url(#" + gradientRef(g) + ")\"";
        }
        return name + "=\"" + paint(paint) + "\"";
    }

    private String gradientRef(LinearGradient g) {
        var id = "grad" + idCounter++;
        var units = g.isProportional() ? "objectBoundingBox" : "userSpaceOnUse";
        defs.append("<linearGradient id=\"").append(id).append("\" gradientUnits=\"").append(units)
            .append("\" x1=\"").append(num(g.getStartX())).append("\" y1=\"").append(num(g.getStartY()))
            .append("\" x2=\"").append(num(g.getEndX())).append("\" y2=\"").append(num(g.getEndY()))
            .append("\">\n");
        for (var stop : g.getStops()) {
            defs.append("<stop offset=\"").append(num(stop.getOffset()))
                .append("\" stop-color=\"").append(colorHex(stop.getColor()));
            if (stop.getColor().getOpacity() < 1.0) {
                defs.append("\" stop-opacity=\"").append(num(stop.getColor().getOpacity()));
            }
            defs.append("\"/>\n");
        }
        defs.append("</linearGradient>\n");
        return id;
    }

    private String paint(Paint p) {
        return p instanceof Color c ? colorHex(c) : "none";
    }

    private static String colorHex(Color c) {
        return String.format(Locale.US, "#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }

    private String points(double[] xPoints, double[] yPoints, int n) {
        var sb = new StringBuilder();
        for (var i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(num(xPoints[i])).append(',').append(num(yPoints[i]));
        }
        return sb.toString();
    }

    private static String encodePng(WritableImage image) {
        var w = (int) image.getWidth();
        var h = (int) image.getHeight();
        var buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(buffered, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode the rasterised layer", e);
        }
    }

    /** Formats a number compactly: no exponent, no trailing zeros, two decimals. */
    private static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        var s = String.format(Locale.US, "%.2f", v);
        // trim trailing zeros, then a dangling separator
        var end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }

    private static String escape(String text) {
        var sb = new StringBuilder(text.length() + 8);
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
