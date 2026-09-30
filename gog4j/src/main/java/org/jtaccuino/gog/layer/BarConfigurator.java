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

import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.Stats;

/**
 * Specialized builder configurator for bar chart geometries ({@link GeomBar}).
 * <p>
 * Provides fluid method chaining for configuring bar styling (e.g. {@link #fill(Color)})
 * and registering the layer into an {@link Plot}. Like the
 * {@code Geoms.bar()}, the bar geometry runs {@code Stats.count()} by default,
 * counting the observations per {@code x} position; pass {@link #stat(Stat)} to
 * override (e.g. {@code .stat(Stats.identity())} to draw raw values).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class BarConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomBar<DF> geomBar;
    private Aes localAes;
    private Stat<DF> stat = Stats.count();

    /**
     * Constructs a new {@code BarConfigurator} wrapping a {@link GeomBar} instance.
     *
     * @param geomBar the bar geometry layer instance
     */
    public BarConfigurator(GeomBar<DF> geomBar) {
        this.geomBar = geomBar;
    }

    /**
     * Sets the position adjustment mode (identity, dodge, or stack).
     *
     * @param position the {@link Position} layout mode
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> position(Position position) {
        this.geomBar.position(position);
        return this;
    }

    /**
     * Sets a full parameterised position adjustment ({@code Positions.dodge(width)},
     * {@code Positions.stack(reverse)}, ...).
     *
     * @param adjust the {@link PositionAdjust} to apply
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> position(PositionAdjust adjust) {
        this.geomBar.position(adjust);
        return this;
    }

    /**
     * Applies {@link Positions#dodge(double) Positions.dodge(width)}.
     *
     * @param width the dodge width as a fraction of the group spacing, in {@code (0, 1]}
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> dodge(double width) {
        this.geomBar.position(Positions.dodge(width));
        return this;
    }

    /**
     * Applies {@code Positions.stack(reverse)} with the given ordering.
     *
     * @param reverse whether to reverse the stacking order
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> stack(boolean reverse) {
        this.geomBar.position(Positions.stack(reverse));
        return this;
    }

    /**
     * Applies {@code Positions.nudge(x, y)} shifting every mark by a fixed offset.
     *
     * @param x the horizontal data offset
     * @param y the vertical data offset
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> nudge(double x, double y) {
        this.geomBar.position(Positions.nudge(x, y));
        return this;
    }

    /**
     * Sets the bar width factor relative to available tick spacing.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> widthFactor(double widthFactor) {
        this.geomBar.widthFactor(widthFactor);
        return this;
    }

    /**
     * Sets the fill color of the bars for ungrouped rendering.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> fill(Color color) {
        this.geomBar.fill(color);
        return this;
    }

    /**
     * Sets a layer-local aesthetic mapping, merged over the plot-global
     * {@code aes()} for this layer only.
     *
     * @param aes the local aesthetic mappings
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> mapping(Aes aes) {
        this.localAes = aes;
        return this;
    }

    /**
     * Overrides the statistic this bar layer runs. The default is
     * {@link Stats#count()}; pass {@link Stats#identity()} to draw the raw
     * {@code y} values instead (the {@code Geoms.bar(stat = "identity")}),
     * or any other {@link Stat}.
     *
     * @param stat the statistic to run, or {@code null}/{@link Stats#identity()}
     *             for raw (identity) rendering
     * @return this {@code BarConfigurator} instance for fluid chaining
     */
    public BarConfigurator<DF> stat(Stat<DF> stat) {
        this.stat = stat;
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        if (stat == null) {
            // Identity: draw the raw columns directly.
            plot.registerInternalLayer(this.geomBar, localAes);
        } else {
            plot.layer(this.geomBar, stat, this.geomBar.positionAdjust(), localAes, LayerParams.empty());
        }
    }
}
