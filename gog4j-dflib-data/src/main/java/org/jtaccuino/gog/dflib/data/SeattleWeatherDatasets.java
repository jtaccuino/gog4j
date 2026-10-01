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
package org.jtaccuino.gog.dflib.data;

import org.dflib.DataFrame;
import org.dflib.ValueMapper;
import org.dflib.csv.Csv;

/**
 * Data loading for the Seattle hourly weather normals dataset, the timestamp
 * counterpart to the calendar-date datasets: its {@code date} column carries a
 * time of day, so a time axis breaks it down to the hour.
 *
 * <p>The data is derived from the NOAA/NWS Seattle climate normals, published as
 * a U.S. Government work and therefore in the public domain.
 *
 * <p>Provides a {@link #loadHourly()} method returning the full year of hourly
 * observations, and a {@link #loadFirstDay()} method returning a single day,
 * which is what exercises the intraday tick ladder.
 */
public class SeattleWeatherDatasets {

    /** The ISO-8601 local date-times the source file uses, e.g. {@code 2010-01-01T01:00:00}. */
    private static final String HOURLY = "/examples/seattle-weather/seattle-weather-hourly.csv";

    private static final SoftCache<DataFrame> HOURLY_CACHE = new SoftCache<>();
    private static final SoftCache<DataFrame> FIRST_DAY_CACHE = new SoftCache<>();

    /** Utility class; not meant to be instantiated. */
    private SeattleWeatherDatasets() {
    }

    /**
     * Loads the hourly observations for the whole year.
     *
     * @return a {@link DataFrame} with 8759 rows and the columns
     *         {@code date}, {@code pressure}, {@code temperature} and {@code wind}
     */
    public static DataFrame loadHourly() {
        return HOURLY_CACHE.get(SeattleWeatherDatasets::readHourly);
    }

    /**
     * Loads a single day of hourly observations, for showing how a timestamp
     * axis narrows to quarter-hour and hour breaks.
     *
     * @return a {@link DataFrame} with the 24 rows of 2010-01-01
     */
    public static DataFrame loadFirstDay() {
        return FIRST_DAY_CACHE.get(SeattleWeatherDatasets::readFirstDay);
    }

    private static DataFrame readHourly() {
        return Csv.loader()
                .col("pressure", ValueMapper.stringToDouble())
                .col("temperature", ValueMapper.stringToDouble())
                .col("wind", ValueMapper.stringToDouble())
                .dateTimeCol("date")
                .load(CsvResources.reader(HOURLY));
    }

    private static DataFrame readFirstDay() {
        return loadHourly().head(24);
    }
}
