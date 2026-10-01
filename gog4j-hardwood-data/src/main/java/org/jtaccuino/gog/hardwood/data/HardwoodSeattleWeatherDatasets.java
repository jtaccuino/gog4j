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
package org.jtaccuino.gog.hardwood.data;

import java.util.stream.IntStream;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;

/**
 * The Seattle hourly weather normals dataset (8759 rows) as a Hardwood Parquet
 * table, the timestamp counterpart to the calendar-date datasets: its
 * {@code date} column is written as a Parquet {@code TIMESTAMP}, so it reaches
 * the engine as a timestamp column rather than as text.
 *
 * <p>The data is derived from the NOAA/NWS Seattle climate normals, published as
 * a U.S. Government work and therefore in the public domain.
 */
public final class HardwoodSeattleWeatherDatasets {

    private static final SoftCache<HardwoodDataFrame> HOURLY = new SoftCache<>();
    private static final SoftCache<HardwoodDataFrame> FIRST_DAY = new SoftCache<>();

    private HardwoodSeattleWeatherDatasets() {
    }

    /**
     * Loads the hourly observations for the whole year (8759 rows).
     *
     * @return the table
     */
    public static HardwoodDataFrame loadHourly() {
        return HOURLY.get(() -> HardwoodDataFrame.ofResource("/examples/seattle-weather/seattle-weather.parquet"));
    }

    /**
     * Loads the first 24 rows, a single day of hourly observations, for showing
     * how a timestamp axis narrows to hour breaks.
     *
     * @return the table
     */
    public static HardwoodDataFrame loadFirstDay() {
        return FIRST_DAY.get(() -> loadHourly().rows(IntStream.range(0, 24).toArray()));
    }
}
