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

import java.util.HashMap;
import org.jtaccuino.gog.coord.Coord;

/**
 * Shared base for the bar and area geometries, which both take part in baseline
 * stacking and share the same axis-domain rules.
 * <p>
 * Holds the {@link Position} adjustment mode and implements the common
 * {@link #expandDomain}
 * rules: a zero baseline and an axis that reaches the cumulative top of every
 * grouped column. All headroom padding comes from the per-scale
 * {@code Expansion} configured on the plot.
 *
 * @param <DF> the data-frame type
 */
public abstract class StackableGeom<DF> implements Layer<DF> {

    /** The position adjustment mode for the stacked geometry. */
    protected Position position;

    /** The parameterised adjustment driving the {@link #position} mode. */
    protected PositionAdjust adjust;

    /**
     * Creates a stackable geometry with the given default position.
     *
     * @param defaultPosition the initial {@link Position} adjustment mode
     */
    protected StackableGeom(Position defaultPosition) {
        this.position = defaultPosition;
        this.adjust = defaultPosition.adjust();
    }

    /**
     * Sets the position adjustment mode for this geometry.
     *
     * @param position the {@link Position} adjustment mode
     */
    protected final void setPosition(Position position) {
        this.position = position;
        this.adjust = position.adjust();
    }

    /**
     * Sets the full parameterised position adjustment, deriving the bare mode
     * from it for existing branch dispatch.
     *
     * @param adjust the {@link PositionAdjust} to apply
     */
    protected final void setAdjust(PositionAdjust adjust) {
        this.adjust = adjust;
        this.position = adjust.mode();
    }

    @Override
    public boolean stackable() {
        return true;
    }

    @Override
    public Position position() {
        return position;
    }

    /**
     * The parameterised position adjustment currently in effect — the
     * {@link PositionAdjust} driving the {@link #position()} mode. Lets a
     * builder/stat-fused path hand the effective adjustment to
     * {@code Plot.layer(...)} without overriding what the user configured.
     *
     * @return the current position adjustment, never {@code null}
     */
    @Override
    public PositionAdjust positionAdjust() {
        return adjust;
    }

    @Override
    public Bounds expandDomain(Bounds bounds, PlotContext<DF> ctx,
                               boolean xDiscrete, boolean yDiscrete) {
        var df = ctx.globalDf();
        var aes = ctx.aes();
        var coord = ctx.coord();
        var extractor = ctx.extractor();
        var dataMinX = bounds.xMin();
        var dataMaxX = bounds.xMax();
        var dataMinY = bounds.yMin();
        var dataMaxY = bounds.yMax();

        // A bar or area chart sits on a 0 baseline; a fully negative series
        // stays below zero untouched.
        if (dataMinY > 0) {
            dataMinY = 0;
        }

        // Stacked columns extend the Y axis to the cumulative top of each
        // group, so the tallest stack is never clipped. The radius axis of a
        // polar plot spans the stack exactly. All headroom beyond this comes
        // from the per-scale Expansion configured on the plot.
        if (position == Position.STACK && aes.y() != null && !coord.isFlipped()) {
            var groupCol = aes.color() != null ? aes.color() : aes.fill();
            if (groupCol != null && aes.x() != null) {
                var rawX = extractor.getColumn(df, aes.x());
                var rawY = extractor.getColumn(df, aes.y());
                var nRows = extractor.getRowCount(df);
                var sums = new HashMap<Object, Double>();
                for (var i = 0; i < nRows; i++) {
                    var xv = rawX.get(i);
                    var yv = rawY.get(i);
                    if (xv != null && yv instanceof Number num) {
                        sums.merge(xv, num.doubleValue(), Double::sum);
                    }
                }
                for (var s : sums.values()) {
                    if (s > dataMaxY) dataMaxY = s;
                }
            }
        }

        return new Bounds(dataMinX, dataMaxX, dataMinY, dataMaxY);
    }
}
