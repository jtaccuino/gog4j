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
package org.jtaccuino.gog;

import javafx.scene.paint.Color;
import org.jtaccuino.gog.stat.StatParams;

/**
 * The {@code params} bag of a {@link Plot#layer} call — the analogue of the
 * {@code layer(params = list(...))}. It holds
 * the stat's numeric configuration (bins, span, …) together with the geom's
 * aesthetic defaults (a constant {@code fill}/{@code color}), so a single
 * argument configures both halves of the layer exactly following convention.
 */
public final class LayerParams {

    private final StatParams stat;
    private final Color fill;
    private final Color color;

    private LayerParams(StatParams stat, Color fill, Color color) {
        this.stat = stat;
        this.fill = fill;
        this.color = color;
    }

    /**
     * Starts a new {@link LayerParams} builder.
     *
     * @return a fresh {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A {@link LayerParams} with no stat configuration and no aesthetic defaults.
     *
     * @return an empty parameter bag
     */
    public static LayerParams empty() {
        return new LayerParams(StatParams.empty(), null, null);
    }

    /**
     * Returns the stat's numeric configuration.
     *
     * @return the stat parameters bag
     */
    public StatParams stat() {
        return stat;
    }

    /**
     * Returns the constant fill colour default.
     *
     * @return the fill {@link Color}, or {@code null} for none
     */
    public Color fill() {
        return fill;
    }

    /**
     * Returns the constant stroke colour default.
     *
     * @return the stroke {@link Color}, or {@code null} for none
     */
    public Color color() {
        return color;
    }

    /**
     * Accumulates the fields of a {@link LayerParams}.
     */
    public static final class Builder {
        private StatParams stat = StatParams.empty();
        private Color fill;
        private Color color;

        private Builder() {
        }

        /**
         * Sets the stat's numeric configuration.
         *
         * @param stat the stat parameters
         * @return this builder
         */
        public Builder stat(StatParams stat) {
            this.stat = stat;
            return this;
        }

        /**
         * Sets the constant fill default for the geometry.
         *
         * @param fill the fill {@link Color}, or {@code null} for none
         * @return this builder
         */
        public Builder fill(Color fill) {
            this.fill = fill;
            return this;
        }

        /**
         * Sets the constant stroke default for the geometry.
         *
         * @param color the stroke {@link Color}, or {@code null} for none
         * @return this builder
         */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /**
         * Builds the finished {@link LayerParams}.
         *
         * @return an immutable parameter bag
         */
        public LayerParams build() {
            return new LayerParams(stat, fill, color);
        }
    }
}
