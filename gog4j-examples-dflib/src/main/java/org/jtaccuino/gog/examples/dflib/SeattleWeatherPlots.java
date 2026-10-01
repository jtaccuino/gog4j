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
package org.jtaccuino.gog.examples.dflib;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Coords.coordFlip;
import static org.jtaccuino.gog.Geoms.area;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.SeattleWeatherDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Example plot definitions using the Seattle hourly weather normals dataset.
 * <p>
 * Its {@code date} column carries a time of day, so the x axis is a timestamp
 * axis: it breaks by months across the year and by hours across a single day,
 * which is the behaviour these plots exist to show.
 */
public class SeattleWeatherPlots {

    /** Utility class; not meant to be instantiated. */
    private SeattleWeatherPlots() {
    }

    /**
     * {@return a line plot of temperature against time over the whole year, so
     * the axis breaks the span by month}
     */
    @SamplePlot(description = "Hourly temperature over a year. The timestamp axis breaks the year by month.",
            title = "Seattle Hourly Temperature, 2010",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.LINE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createYearlyTemperature() {
        return ggplot(SeattleWeatherDatasets.loadHourly(), aes().x("date").y("temperature"))
                                .geoms(line())
                .labs(labs("Hourly Temperature", "Date (2010)", "Temperature (°C)"));
    }

    /**
     * {@return a scatter of pressure against time over a single day, so the
     * axis breaks the span by hour}
     */
    @SamplePlot(description = "Pressure over one day. The timestamp axis narrows to hourly breaks.",
            title = "Seattle Pressure, 1 January 2010",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createDailyPressure() {
        return ggplot(SeattleWeatherDatasets.loadFirstDay(), aes().x("date").y("pressure"))
                                .geoms(point())
                .labs(labs("Hourly Pressure", "1 January 2010", "Pressure (hPa)"));
    }

    /**
     * {@return temperature against time with the axes swapped, showing that a
     * timestamp axis breaks the same way when mapped to the vertical one}
     */
    @SamplePlot(description = "A timestamp column on the y axis. The temporal break ladder is unchanged by coordFlip.",
            title = "Temperature over Time (Flipped)",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.LINE, SampleGeom.POINT},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS},
            coords = {SampleCoord.FLIP})
    public static Plot<DataFrame> createFlippedTemperature() {
        return ggplot(SeattleWeatherDatasets.loadFirstDay(), aes().x("temperature").y("date"))
                .coord(coordFlip())
                                .geoms(line(), point())
                .labs(labs("Temperature", "Temperature (°C)", "1 January 2010"));
    }

    /**
     * {@return a filled area chart of temperature over the year, exercising the
     * timestamp label on the stat-geometry hover}
     */
    @SamplePlot(description = "A filled area of temperature over the year.",
            title = "Temperature Envelope",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.AREA},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createTemperatureArea() {
        return ggplot(SeattleWeatherDatasets.loadHourly(), aes().x("date").y("temperature"))
                                .geoms(area())
                .labs(labs("Temperature Range", "Date (2010)", "Temperature (°C)"));
    }
}
