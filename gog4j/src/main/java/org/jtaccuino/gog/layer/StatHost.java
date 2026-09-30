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

import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.stat.Stat;

/**
 * Marks a geometry that can run an externally supplied {@link Stat} — the
 * geometry half of a {@code Plot#layer(...)}
 * call, which {@link Plot#layer} binds. A host keeps the stat, its position
 * adjustment, and the layer's params it was built with, runs the stat over
 * (per-panel) data in {@code render}, and draws the {@code StatData} output
 * instead of raw columns. Consequently its {@link Stat#outputColumns()} can be
 * referenced from an aesthetic mapping via {@link AesValue#afterStat(AesValue.ComputedVariable)}.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public interface StatHost<DF> {

    /**
     * Binds the given stat, position adjustment, and layer params to this
     * geometry, switching it into stat-consuming mode.
     *
     * @param stat     the statistic to run
     * @param position the position adjustment to apply
     * @param params   the layer's stat config and aesthetic defaults
     */
    void attach(Stat<DF> stat, PositionAdjust position, LayerParams params);
}
