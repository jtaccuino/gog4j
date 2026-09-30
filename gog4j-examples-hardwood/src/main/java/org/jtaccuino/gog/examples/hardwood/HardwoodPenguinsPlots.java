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
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodPenguinsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * The Palmer penguins scatter, read from Parquet through the Hardwood backend.
 * <p>
 * It mirrors the DFLib example to show the same grammar runs on a different
 * frame type: the factory loads a {@link HardwoodDataFrame} from a Parquet
 * dataset the build derives from the shared CSV.
 */
public class HardwoodPenguinsPlots {

    /** Utility class; not meant to be instantiated. */
    private HardwoodPenguinsPlots() {
    }

    /**
     * A penguins scatter from Parquet, with the species mapped to colour and
     * shape — the mixed numeric/categorical case.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Palmer penguins read from Parquet through the Hardwood backend.",
            title = "Hardwood: Penguins scatter",
            dataset = SampleDataset.PENGUINS,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.CARTESIAN},
            features = {SampleFeature.TWO_D})
    public static Plot<HardwoodDataFrame> createPenguins() {
        var df = HardwoodPenguinsDatasets.loadPenguins();
        return ggplot(df, aes().x("bill_length_mm").y("body_mass_g").color("species"))
                .geoms(point().size(5.0).opacity(0.8))
                .labs(labs("Hardwood: penguins from Parquet",
                        "Bill length (mm)", "Body mass (g)"));
    }
}
