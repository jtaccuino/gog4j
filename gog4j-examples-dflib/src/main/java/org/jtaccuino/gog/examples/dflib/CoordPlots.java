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
import static org.jtaccuino.gog.Coords.coordEqual;
import static org.jtaccuino.gog.Coords.coordTrans;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;

import org.dflib.DataFrame;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.scale.ScaleTransform;

/**
 * {@code Coords.coordEqual()} and {@code Coords.coordTrans()} examples, mirroring the
 * reference documentation for fixed-aspect and transformed coordinate systems.
 * <p>
 * {@link #createCoordEqual()} draws engine displacement against highway fuel
 * economy under a {@code coordEqual()} crop, so equal data spans map to equal
 * pixel spans on both continuous axes. {@link #createCoordTrans()} shows the
 * same variables on a {@code coordTrans(…, LOG10)} y-axis, where a decade
 * spans an equal distance in pixels no matter where it sits.
 *
 * @see org.jtaccuino.gog.Coords#coordEqual()
 * @see org.jtaccuino.gog.Coords#coordTrans(ScaleTransform, ScaleTransform)
 */
public class CoordPlots {

    /** Utility class; not meant to be instantiated. */
    private CoordPlots() {
    }

    /** {@return the fixed-aspect scatter of displacement against highway MPG} */
    @SamplePlot(description = "Scatter under a fixed-aspect coordinate system with equal pixels per unit.",
            title = "coordEqual: equal pixels per unit on both axes",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.EQUAL},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCoordEqual() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(point().size(2.0))
                .coord(coordEqual())
                .labs(labs("coordEqual: equal pixels per unit on both axes", "Displacement (L)", "Highway MPG"));
    }

    /** {@return the log-transformed scatter of displacement against highway MPG} */
    @SamplePlot(description = "Scatter on a log10-transformed y-axis where decades span equal pixels.",
            title = "coordTrans: log10 y-axis, decades at equal pixels",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            coords = {SampleCoord.TRANS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createCoordTrans() {
        var mpg = MpgDatasets.loadMpg();
        return ggplot(mpg, aes().x("displ").y("hwy"))
                .geoms(point().size(2.0))
                .coord(coordTrans(null, ScaleTransform.LOG10))
                .labs(labs("coordTrans: log10 y-axis, decades at equal pixels", "Displacement (L)", "Highway MPG (log10)"));
    }
}
