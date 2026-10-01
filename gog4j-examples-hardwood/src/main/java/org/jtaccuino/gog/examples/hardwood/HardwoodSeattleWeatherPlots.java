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
package org.jtaccuino.gog.examples.hardwood;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.line;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodSeattleWeatherDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The Seattle hourly weather plots read from Parquet through the Hardwood
 * backend.
 *
 * <p>These mirror the DFLib examples of the same dataset. The point of the pair
 * is that the Parquet schema carries the timestamp as a real {@code TIMESTAMP},
 * so Hardwood classifies the column as temporal from the schema alone, with no
 * value to sample — and the two backends produce the same axis.
 */
public class HardwoodSeattleWeatherPlots {

    /** Utility class; not meant to be instantiated. */
    private HardwoodSeattleWeatherPlots() {
    }

    /**
     * A timestamp line plot over the whole year, read from Parquet through
     * Hardwood.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Hourly temperature over a year, read from a Parquet TIMESTAMP column.",
            title = "Hardwood: Seattle Hourly Temperature, 2010",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.LINE},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<HardwoodDataFrame> createYearlyTemperature() {
        var df = HardwoodSeattleWeatherDatasets.loadHourly();
        return ggplot(df, aes().x("date").y("temperature"))
                .geoms(line())
                .labs(labs("Hourly temperature from Parquet", "Date (2010)", "Temperature (°C)"));
    }

    /**
     * A timestamp scatter over a single day, read from Parquet through Hardwood,
     * showing the axis narrow to hourly breaks.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Pressure over one day, read from a Parquet TIMESTAMP column.",
            title = "Hardwood: Seattle Pressure, 1 January 2010",
            dataset = SampleDataset.SEATTLE_WEATHER,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<HardwoodDataFrame> createDailyPressure() {
        var df = HardwoodSeattleWeatherDatasets.loadFirstDay();
        return ggplot(df, aes().x("date").y("pressure"))
                .geoms(point())
                .labs(labs("Hourly pressure from Parquet", "1 January 2010", "Pressure (hPa)"));
    }
}
