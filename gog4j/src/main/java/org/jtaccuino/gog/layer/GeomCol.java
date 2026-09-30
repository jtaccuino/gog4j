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

import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.Stats;

/**
 * Column geometry layer: the {@code Geoms.col()} counterpart of {@link GeomBar}.
 * <p>
 * Where {@code Geoms.bar()} maps the bar height to a count of cases per x
 * position, {@code Geoms.col()} makes the height represent a value that is
 * already in the data — the y aesthetic is taken as-is ({@code Stats.identity}
 * in grammar-of-graphics terms), with the bar rising from the zero baseline to {@code y}.
 * <p>
 * Rendering is identical to {@link GeomBar}; the two differ in intent and in
 * their default position adjustment, which is {@link Position#STACK} to match
 * the {@code Geoms.col()}, so bars sharing an x position pile up.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomCol<DF> extends GeomBar<DF> {

    /**
     * Creates a column geometry with the default {@link Position#STACK} layout.
     */
    public GeomCol() {
        super(Position.STACK);
    }

    @Override
    public Stat<DF> defaultStat() {
        // Geoms.col() inherits Stats.identity: heights are the y values already
        // in the data, so no statistic runs and the raw columns are drawn.
        return Stats.identity();
    }
}
