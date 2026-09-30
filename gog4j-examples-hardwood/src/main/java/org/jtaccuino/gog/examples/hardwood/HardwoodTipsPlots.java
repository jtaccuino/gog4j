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
import static org.jtaccuino.gog.Geoms.histogram;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodTipsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The restaurant tips histogram, read from Parquet through the Hardwood
 * backend, showing a categorical/continuous split with a fill mapping.
 */
public class HardwoodTipsPlots {

    /** Utility class; not meant to be instantiated. */
    private HardwoodTipsPlots() {
    }

    /**
     * A tips histogram from Parquet, showing a categorical/continuous split
     * with a fill mapping.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Restaurant tips histogram read from Parquet through Hardwood.",
            title = "Hardwood: Tips by day",
            dataset = SampleDataset.TIPS,
            geoms = {SampleGeom.HISTOGRAM},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<HardwoodDataFrame> createTips() {
        var df = HardwoodTipsDatasets.loadTips();
        return ggplot(df, aes().x("total_bill").fill("day"))
                .geoms(histogram().bins(20))
                .labs(labs("Hardwood: tips from Parquet", "Total bill", "Count"));
    }
}
