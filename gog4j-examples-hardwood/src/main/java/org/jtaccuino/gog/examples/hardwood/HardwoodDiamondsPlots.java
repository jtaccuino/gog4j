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
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodDiamondsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The diamonds carat density, read from Parquet through the Hardwood backend:
 * 53,940 rows read columnar-ly.
 */
public class HardwoodDiamondsPlots {

    /** Utility class; not meant to be instantiated. */
    private HardwoodDiamondsPlots() {
    }

    /**
     * A diamonds density from Parquet: 53,940 rows read columnar-ly.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Diamond carat density over 53,940 Parquet rows through Hardwood.",
            title = "Hardwood: Diamonds density",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<HardwoodDataFrame> createDiamondsDensity() {
        var df = HardwoodDiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat"))
                .geoms(density().fill(Color.web("#4c72b0")))
                .labs(labs("Hardwood: diamonds from Parquet", "Carat", "Density"));
    }
}
