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
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.title;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.FaithfulDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * Example plot definitions using the Old Faithful dataset.
 * <p>
 * Demonstrates 2D heatmaps (tile geometry) via the {@link FaithfulDatasets} loader.
 */
public class FaithfulPlots {

    /** Utility class; not meant to be instantiated. */
    private FaithfulPlots() {
    }

    /** {@return a heatmap of the Old Faithful eruption density} */
    @SamplePlot(description = "Tile heatmap of eruption density by waiting time.",
            title = "Faithfully Waiting to Erupt",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.TILE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHeatmap() {
        var df = FaithfulDatasets.loadFaithfuld();
        return ggplot(df, aes().x("waiting").y("eruptions").fill("density"))
                .geoms(tile("plasma"))
                .labs(title("Faithfully Waiting to Erupt"));
    }
}
