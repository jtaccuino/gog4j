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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.scale.Scale;

/**
 * Jittered scatter plot geometry layer implementation.
 * <p>
 * A convenient shortcut for {@code Geoms.point(position = "jitter")}: it adds a small
 * amount of random variation to the location of each point, which is useful for
 * handling overplotting caused by discreteness in smaller datasets (e.g. overlaying
 * raw observations on a {@link GeomBoxplot}).
 * <p>
 * Jitter is applied in data space, before scale mapping, and the offsets are
 * deterministic per data row (seeded), so the layer renders identically across
 * repaints and facet panels.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomJitter<DF> extends GeomPoint<DF> {

    private Double width = null;
    private Double height = null;
    private long seed = 12345L;
    private double xAmount = 0.0;
    private double yAmount = 0.0;

    /**
     * Creates a jittered scatter geometry with deterministic per-row jitter.
     */
    public GeomJitter() {
    }

    /**
     * Sets the amount of horizontal jitter in data units. Jitter is added in both
     * positive and negative directions, so the total spread is twice this value.
     *
     * @param width jitter width in data units
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    public GeomJitter<DF> width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the amount of vertical jitter in data units. Jitter is added in both
     * positive and negative directions, so the total spread is twice this value.
     *
     * @param height jitter height in data units
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    public GeomJitter<DF> height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets a constant point fill/stroke color for this layer.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    @Override
    public GeomJitter<DF> color(Color color) {
        super.color(color);
        return this;
    }

    /**
     * Sets a constant point diameter size in pixels for this layer.
     *
     * @param size the point diameter in pixels
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    @Override
    public GeomJitter<DF> size(double size) {
        super.size(size);
        return this;
    }

    /**
     * Sets a constant point symbol shape.
     *
     * @param shape the {@link PointShape} symbol
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    public GeomJitter<DF> shape(PointShape shape) {
        this.spec.shape(shape);
        return this;
    }

    /**
     * Sets the opacity for rendered points.
     *
     * @param opacity between 0.0 (fully transparent) and 1.0 (fully opaque)
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    public GeomJitter<DF> opacity(double opacity) {
        this.spec.opacity(opacity);
        return this;
    }

    /**
     * Sets the random seed used to generate deterministic per-row jitter offsets.
     *
     * @param seed the RNG seed
     * @return this {@code GeomJitter} instance for fluid chaining
     */
    public GeomJitter<DF> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    protected void prepareJitter(PanelContext<DF> ctx, List<?> rawX, List<?> rawY) {
        var coord = ctx.plot().coord();
        var sx = ctx.scaleX();
        var sy = ctx.scaleY();
        var xDataScale = coord.isFlipped() ? sy : sx;
        var yDataScale = coord.isFlipped() ? sx : sy;
        xAmount = rawX == null ? 0.0 : resolveAmount(width, xDataScale, rawX);
        yAmount = rawY == null ? 0.0 : resolveAmount(height, yDataScale, rawY);
    }

    @Override
    protected boolean usesJitter() {
        return true;
    }

    @Override
    protected double jitterX(double xData, int row) {
        return xData + jitter(row, xAmount);
    }

    @Override
    protected double jitterY(double yData, int row) {
        return yData + jitter(row, yAmount);
    }

    /**
     * Resolves the actual jitter amount: explicit value if provided, otherwise 20% of
     * the resolution of the data (occupying 40% of the implied bins), aligned to the
     * integer bin grid for categorical axes.
     */
    private double resolveAmount(Double amount, Scale dataScale, List<?> raw) {
        if (amount != null) {
            return amount;
        }
        if (dataScale.isDiscrete()) {
            return 0.2;
        }
        return resolution(raw) * 0.2;
    }

    /**
     * Computes the smallest positive difference between distinct data values (resolution).
     */
    private double resolution(List<?> raw) {
        var uniq = new ArrayList<Double>();
        for (var v : raw) {
            if (v == null) {
                continue;
            }
            double d = Values.toDouble(v);
            if (!uniq.contains(d)) {
                uniq.add(d);
            }
        }
        if (uniq.size() < 2) {
            return 1.0;
        }
        uniq.sort(Double::compareTo);
        double res = Double.MAX_VALUE;
        for (var i = 0; i < uniq.size() - 1; i++) {
            double diff = uniq.get(i + 1) - uniq.get(i);
            if (diff > 0 && diff < res) {
                res = diff;
            }
        }
        return res == Double.MAX_VALUE ? 1.0 : res;
    }

    /**
     * Deterministic per-row jitter offset in the range {@code [-amount, amount]}.
     */
    private double jitter(int rowIndex, double amount) {
        if (amount == 0) {
            return 0.0;
        }
        var rng = new Random(seed ^ (rowIndex * 0x9E3779B97F4A7C15L));
        return (rng.nextDouble() * 2.0 - 1.0) * amount;
    }
}
