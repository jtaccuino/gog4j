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
package org.jtaccuino.gog.layer;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.stat.Stats;

/**
 * The {@link org.jtaccuino.gog.Geoms#contour3d()} geometry layer: converts a
 * point grid into contour band polygons (a "layer cake" of the regions where z
 * lies at or above each break level), then hands them to the shared
 * {@link GeomPolygon3d} renderer. Runs
 * {@link org.jtaccuino.gog.stat.Stats#surface3d()} by default. Bands at the
 * same level share a group id, so the even-odd rule renders holes — emulated
 * here by reversed-winding subpaths.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomContour3d<DF> extends GeomPolygon3d<DF> {

    /**
     * Creates a contour geometry with the {@link org.jtaccuino.gog.Geoms#contour3d()}
     * defaults (a {@code black} fill, a {@code white} stroke, pairwise
     * depth sorting).
     */
    public GeomContour3d() {
        super(new Polygon3dSpec()
                .fill(Color.web("black"))
                .colour(Color.web("white"))
                .sortMethod(Polygon3dSpec.SortMethod.PAIRWISE));
    }

    /**
     * Creates a contour geometry with the given rendering specification.
     *
     * @param spec the {@link Polygon3dSpec} to use
     */
    protected GeomContour3d(Polygon3dSpec spec) {
        super(spec);
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.surface3d();
    }

    @Override
    protected List<Poly3d> polygonize(StatData statData, PanelContext<DF> ctx) {
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        int n = statData.rowCount();
        if (n < 4) {
            return List.of();
        }
        var x = new double[n];
        var y = new double[n];
        var z = new double[n];
        var g = new String[n];
        for (int i = 0; i < n; i++) {
            x[i] = Values.toDouble(xCol.get(i));
            y[i] = Values.toDouble(yCol.get(i));
            z[i] = Values.toDouble(zCol.get(i));
            g[i] = String.valueOf(gCol.get(i));
        }
        var spec = getSpec();
        List<MeshUtil.Loop> loops;
        try {
            loops = MeshUtil.pointsToContours(x, y, z, g,
                    spec.getBreaks(), spec.getBins(), spec.getBinwidth());
        } catch (IllegalArgumentException ex) {
            // Not enough grid resolution to trace a single band.
            return List.of();
        }
        var out = new ArrayList<Poly3d>(loops.size());
        for (var loop : loops) {
            out.add(new Poly3d(loop.x(), loop.y(), loop.z(), loop.group(), null));
        }
        return out;
    }
}
