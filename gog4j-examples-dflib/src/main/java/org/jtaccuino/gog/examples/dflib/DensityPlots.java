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
import static org.jtaccuino.gog.Geoms.density;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.sampler.meta.SampleScale;

/**
 * Density plot examples on the diamonds dataset, following the
 * {@code Geoms.density()} documentation of the reference: the basic estimate and its
 * flipped orientation, bandwidth adjustment, colour and fill mapping, boundary
 * correction, and the stacked / count / fill position adjustments.
 *
 * @see org.jtaccuino.gog.dflib.data.DiamondsDatasets
 */
public class DensityPlots {

    /** Utility class; not meant to be instantiated. */
    private DensityPlots() {
    }

    /** {@return a basic density plot of diamond carat} */
    @SamplePlot(description = "Smooth kernel density estimate of diamond carat.",
            title = "Diamond Carat Density",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBasicDensity() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat"))
                .geoms(density())
                .labs(labs("1. Diamond Carat Density", "carat", "density"));
    }

    /** {@return a flipped density plot mapping carat to the y aesthetic} */
    @SamplePlot(description = "Density estimate flipped to map carat onto the y-axis.",
            title = "Flipped Orientation (aes(y = carat))",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFlippedDensity() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().y("carat"))
                .geoms(density())
                .labs(labs("2. Flipped Orientation (aes(y = carat))", "density", "carat"));
    }

    /** {@return a density plot with a small bandwidth (adjust = 1/5)} */
    @SamplePlot(description = "Density with a narrow bandwidth for a closer fit to the data.",
            title = "Closer Fit (adjust = 1/5)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAdjustSmall() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat"))
                .geoms(density().adjust(0.2).fill(Color.web("#9ecae1")).color(Color.web("#3182bd")))
                .labs(labs("3. Closer Fit (adjust = 1/5)", "carat", "density"));
    }

    /** {@return a density plot with a wide bandwidth (adjust = 5)} */
    @SamplePlot(description = "Density with a wide bandwidth producing a smoother estimate.",
            title = "Smoother Estimate (adjust = 5)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAdjustLarge() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat"))
                .geoms(density().adjust(5.0).fill(Color.web("#d0c4ea")).color(Color.web("#756bb1")))
                .labs(labs("4. Smoother Estimate (adjust = 5)", "carat", "density"));
    }

    /** {@return a density plot colour-mapped by cut, zoomed to the depth range 55-70} */
    @SamplePlot(description = "One density curve per diamond cut, colour-mapped and zoomed to depth 55-70.",
            title = "One Density per Cut (xlim(55, 70))",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.X_LIMITS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColourMapped() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("depth").color("cut"))
                .coord(Coord2D.cartesian().xlim(55, 70))
                .geoms(density())
                .labs(labs("5. One Density per Cut (xlim(55, 70))", "depth", "density"));
    }

    /** {@return a density plot with fill and colour mapped to cut (alpha = 0.1)} */
    @SamplePlot(description = "Translucent filled densities per cut sharing a zoomed depth range.",
            title = "Filled Densities per Cut (alpha = 0.1)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.X_LIMITS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFillMapped() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("depth").fill("cut").color("cut"))
                .coord(Coord2D.cartesian().xlim(55, 70))
                .geoms(density().alpha(0.1))
                .labs(labs("6. Filled Densities per Cut (alpha = 0.1)", "depth", "density"));
    }

    /** {@return a density plot with boundary correction (bounds = c(1, Inf))} */
    @SamplePlot(description = "Overlaid density layers comparing an estimate with boundary correction to one without.",
            title = "Boundary Correction (bounds = c(1, Inf))",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createBounds() {
        var df = DiamondsDatasets.loadDiamonds();
        var big = df.rows(r -> r.getDouble("carat") >= 1.0).select();
        return ggplot(big, aes().x("carat"))
                .geoms(
                    density().color(Color.web("#e31a1c")).fill(Color.TRANSPARENT),
                    density().bounds(1.0, Double.POSITIVE_INFINITY).color(Color.web("#3182bd")).fill(Color.TRANSPARENT)
                )
                .labs(labs("7. Boundary Correction (bounds = c(1, Inf))", "carat", "density"));
    }

    /** {@return the stacked density plot of carat per cut} */
    @SamplePlot(description = "Stacked density areas per cut showing composition across carat.",
            title = "Stacked Densities (position = 'stack')",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStacked() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").fill("cut"))
                .geoms(density().position(Position.STACK).alpha(0.85))
                .labs(labs("8. Stacked Densities (position = 'stack')", "carat", "density"));
    }

    /** {@return the stacked count-density plot of carat per cut} */
    @SamplePlot(description = "Stacked density areas scaled to raw counts per cut.",
            title = "Stacked Count Densities (afterStat(count))",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            positions = {SamplePosition.STACK},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createStackedCount() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").fill("cut"))
                .geoms(density().stat("count").position(Position.STACK).alpha(0.85))
                .labs(labs("9. Stacked Count Densities (afterStat(count))", "carat", "count"));
    }

    /** {@return the conditional density plot of carat per cut (position = 'fill')} */
    @SamplePlot(description = "Conditional densities normalized to fill per cut.",
            title = "Conditional Density (position = 'fill')",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.DENSITY},
            positions = {SamplePosition.FILL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createFillPosition() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").fill("cut"))
                .geoms(density().stat("count").position(Position.FILL).alpha(0.85))
                .labs(labs("10. Conditional Density (position = 'fill')", "carat", "count"));
    }
}
