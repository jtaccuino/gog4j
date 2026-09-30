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
 * Specialized builder configurator for column geometries ({@link GeomCol}).
 * <p>
 * {@code Geoms.col()} draws bars whose heights represent the values of the
 * {@code y} aesthetic directly (the identity statistic), stacked by default
 * when several bars share an x position. Provides fluid method chaining for
 * configuring the position adjustment ({@link #position(Position)}), the
 * column width factor ({@link #widthFactor(double)}), and the constant fill
 * ({@link #fill(Color)}), then registers the layer into an {@link Plot}. Pass
 * {@link #stat(Stat)} to run a statistic (e.g. {@code .stat(Stats.count())}).
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class ColConfigurator<DF> implements LayerConfigurator<DF> {
    private final GeomCol<DF> geomCol;
    private Aes localAes;
    private Stat<DF> stat = Stats.identity();

    /**
     * Constructs a new {@code ColConfigurator} wrapping a {@link GeomCol}
     * instance.
     *
     * @param geomCol the column geometry layer instance
     */
    public ColConfigurator(GeomCol<DF> geomCol) {
        this.geomCol = geomCol;
    }

    /**
     * Sets the position adjustment mode (identity, dodge, stack, or fill).
     *
     * @param position the {@link Position} layout mode
     * @return this {@code ColConfigurator} instance for fluid method chaining
     */
    public ColConfigurator<DF> position(Position position) {
        this.geomCol.position(position);
        return this;
    }

    /**
     * Sets a full parameterised position adjustment ({@code Positions.dodge(width)},
     * {@code Positions.stack(reverse)}, ...).
     *
     * @param adjust the {@link PositionAdjust} to apply
     * @return this {@code ColConfigurator} instance for fluid chaining
     */
    public ColConfigurator<DF> position(PositionAdjust adjust) {
        this.geomCol.position(adjust);
        return this;
    }

    /**
     * Applies {@link Positions#dodge(double) Positions.dodge(width)}.
     *
     * @param width the dodge width as a fraction of the group spacing, in {@code (0, 1]}
     * @return this {@code ColConfigurator} instance for fluid chaining
     */
    public ColConfigurator<DF> dodge(double width) {
        this.geomCol.position(Positions.dodge(width));
        return this;
    }

    /**
     * Applies {@code Positions.stack(reverse)} with the given ordering.
     *
     * @param reverse whether to reverse the stacking order
     * @return this {@code ColConfigurator} instance for fluid chaining
     */
    public ColConfigurator<DF> stack(boolean reverse) {
        this.geomCol.position(Positions.stack(reverse));
        return this;
    }

    /**
     * Applies {@code Positions.nudge(x, y)} shifting every mark by a fixed offset.
     *
     * @param x the horizontal data offset
     * @param y the vertical data offset
     * @return this {@code ColConfigurator} instance for fluid chaining
     */
    public ColConfigurator<DF> nudge(double x, double y) {
        this.geomCol.position(Positions.nudge(x, y));
        return this;
    }

    /**
     * Sets the column width factor relative to available tick spacing.
     *
     * @param widthFactor width factor between 0.0 and 1.0
     * @return this {@code ColConfigurator} instance for fluid method chaining
     */
    public ColConfigurator<DF> widthFactor(double widthFactor) {
        this.geomCol.widthFactor(widthFactor);
        return this;
    }

    /**
     * Sets the fill color of the columns for ungrouped rendering.
     *
     * @param color the JavaFX {@link Color}
     * @return this {@code ColConfigurator} instance for fluid method chaining
     */
    public ColConfigurator<DF> fill(Color color) {
        this.geomCol.fill(color);
        return this;
    }

    /**
     * Sets a layer-local aesthetic mapping, merged over the plot-global
     * {@code aes()} for this layer only.
     *
     * @param aes the local aesthetic mappings
     * @return this {@code ColConfigurator} instance for fluid method chaining
     */
    public ColConfigurator<DF> mapping(Aes aes) {
        this.localAes = aes;
        return this;
    }

    /**
     * Overrides the statistic this column layer runs. The default is identity
     * (draw the raw {@code y} values); pass {@link Stats#count()} to count
     * cases per {@code x} instead (the {@code Geoms.col(stat = "count")}),
     * or any other {@link Stat}.
     *
     * @param stat the statistic to run, or {@code null}/{@link Stats#identity()}
     *             for raw (identity) rendering
     * @return this {@code ColConfigurator} instance for fluid chaining
     */
    public ColConfigurator<DF> stat(Stat<DF> stat) {
        this.stat = stat;
        return this;
    }

    @Override
    public void configure(ConfigTarget<DF> plot) {
        if (stat == null) {
            // Identity: draw the raw columns directly.
            plot.registerInternalLayer(this.geomCol, localAes);
        } else {
            plot.layer(this.geomCol, stat, this.geomCol.positionAdjust(), localAes, LayerParams.empty());
        }
    }
}
