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

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * A {@link DrawSurface} that paints onto a JavaFX canvas — the backend used for
 * interactive display and for the rasterised PNG export.
 * <p>
 * Every method delegates straight to the wrapped {@link GraphicsContext}.
 */
public class FxDrawSurface implements DrawSurface {

    private final GraphicsContext gc;

    /**
     * Wraps a JavaFX canvas graphics context so drawing calls delegate to it.
     *
     * @param gc the graphics context to paint onto
     */
    public FxDrawSurface(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Returns the wrapped graphics context, for the rare case a caller needs a
     * canvas feature outside this abstraction.
     *
     * @return the graphics context
     */
    public GraphicsContext graphicsContext() {
        return gc;
    }

    @Override public void setFill(Paint paint) { gc.setFill(paint); }
    @Override public void setStroke(Paint paint) { gc.setStroke(paint); }
    @Override public void setLineWidth(double width) { gc.setLineWidth(width); }
    @Override public void setLineDashes(double... dashes) { gc.setLineDashes(dashes); }
    @Override public void setFont(Font font) { gc.setFont(font); }
    @Override public Font getFont() { return gc.getFont(); }
    @Override public void setTextAlign(TextAlignment alignment) { gc.setTextAlign(alignment); }
    @Override public void setTextBaseline(VPos baseline) { gc.setTextBaseline(baseline); }

    @Override public void save() { gc.save(); }
    @Override public void restore() { gc.restore(); }
    @Override public void translate(double x, double y) { gc.translate(x, y); }
    @Override public void rotate(double degrees) { gc.rotate(degrees); }
    @Override public void scale(double sx, double sy) { gc.scale(sx, sy); }

    @Override public void fillRect(double x, double y, double w, double h) { gc.fillRect(x, y, w, h); }
    @Override public void strokeRect(double x, double y, double w, double h) { gc.strokeRect(x, y, w, h); }
    @Override public void fillOval(double x, double y, double w, double h) { gc.fillOval(x, y, w, h); }
    @Override public void strokeOval(double x, double y, double w, double h) { gc.strokeOval(x, y, w, h); }
    @Override public void strokeLine(double x1, double y1, double x2, double y2) { gc.strokeLine(x1, y1, x2, y2); }

    @Override
    public void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        if (arcExtent >= 360 || arcExtent <= -360) {
            gc.fillOval(x, y, w, h);
        } else {
            gc.fillArc(x, y, w, h, -startAngle, -arcExtent, ArcType.ROUND);
        }
    }

    @Override
    public void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        if (arcExtent >= 360 || arcExtent <= -360) {
            gc.strokeOval(x, y, w, h);
        } else {
            gc.strokeArc(x, y, w, h, -startAngle, -arcExtent, ArcType.OPEN);
        }
    }

    @Override
    public void arc(double x, double y, double w, double h, double startAngle, double arcExtent) {
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        gc.appendSVGPath(Arcs.arcData(cx, cy, w / 2.0, h / 2.0, startAngle, arcExtent));
    }

    @Override
    public void fillPolygon(double[] xPoints, double[] yPoints, int n) {
        gc.fillPolygon(xPoints, yPoints, n);
    }

    @Override
    public void strokePolygon(double[] xPoints, double[] yPoints, int n) {
        gc.strokePolygon(xPoints, yPoints, n);
    }

    @Override public void beginPath() { gc.beginPath(); }
    @Override public void moveTo(double x, double y) { gc.moveTo(x, y); }
    @Override public void lineTo(double x, double y) { gc.lineTo(x, y); }
    @Override public void rect(double x, double y, double w, double h) { gc.rect(x, y, w, h); }
    @Override public void closePath() { gc.closePath(); }
    @Override public void stroke() { gc.stroke(); }
    @Override public void fill() { gc.fill(); }
    @Override public void clip() { gc.clip(); }

    @Override public void fillText(String text, double x, double y) { gc.fillText(text, x, y); }
    @Override public void strokeText(String text, double x, double y) { gc.strokeText(text, x, y); }
}
