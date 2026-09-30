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
import static org.jtaccuino.gog.Geoms.point3d;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Coords;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;

/**
 * 3D scatter plot examples using the diamonds dataset.
 * <p>
 * Demonstrates {@link org.jtaccuino.gog.coord.Coord3D} with continuous and
 * categorical z-axes, coloured by diamond cut and clarity.
 */
public class DiamondsPlots3d {

    /** Utility class; not meant to be instantiated. */
    private DiamondsPlots3d() {
    }

    /** {@return a 3D scatter plot of carat vs price vs depth} */
    @SamplePlot(description = "3D scatter of price against carat and depth, translucent points.",
            title = "3D: Price vs Carat vs Depth",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.THREE_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> create3dScatterPriceCaratDepth() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").z("depth"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d())
                .labs(labs("3D: Price vs Carat vs Depth", "carat", "price"));
    }

    /** {@return a 3D scatter plot of depth vs table vs price} */
    @SamplePlot(description = "3D scatter of depth, table, and price, translucent points.",
            title = "3D: Depth x Table x Price",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            features = {SampleFeature.THREE_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> create3dScatterDepthPriceTable() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("depth").y("table").z("price"))
                .geoms(point3d().size(4.0).opacity(0.6))
                .coord(Coords.coord3d())
                .labs(labs("3D: Depth x Table x Price", "depth", "table"));
    }
}
