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
 * The {@link org.jtaccuino.gog.Geoms#surface3d()} geometry layer: tessellates a
 * point grid into polygon tiles (rectangular/right-triangle/equilateral grid
 * tiles, or a Delaunay triangulation for irregular data) and hands them to the
 * shared {@link GeomPolygon3d} renderer. Runs
 * {@link org.jtaccuino.gog.stat.Stats#surface3d()} by default.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomSurface3d<DF> extends GeomPolygon3d<DF> {

    /**
     * Creates a surface geometry with the {@link org.jtaccuino.gog.Geoms#surface3d()}
     * defaults (a {@code grey60} fill, no stroke).
     */
    public GeomSurface3d() {
        super(new Polygon3dSpec().fill(Color.rgb(153, 153, 153)).colour(null));
    }

    /**
     * Creates a surface geometry with the given rendering specification.
     *
     * @param spec the {@link Polygon3dSpec} to use
     */
    protected GeomSurface3d(Polygon3dSpec spec) {
        super(spec);
    }

    @Override
    public Stat<DF> defaultStat() {
        return Stats.surface3d();
    }

    /**
     * Reports the domain a function surface wants when it is configured with
     * explicit {@code xlim}/{@code ylim} (a data-less
     * {@link org.jtaccuino.gog.Geoms#function3d}
     * plot): the coord then fits the cube to the evaluated domain instead of a
     * neutral 0..1 seed, which would push the tiles far outside the cube.
     */
    @Override
    public Layer.Bounds expandDomain(Layer.Bounds bounds, PlotContext<DF> ctx,
            boolean xDiscrete, boolean yDiscrete) {
        Object xl = attachedParams.stat().get("xlim", null);
        Object yl = attachedParams.stat().get("ylim", null);
        if (!(xl instanceof double[] xlim) || !(yl instanceof double[] ylim)
                || xlim.length < 2 || ylim.length < 2) {
            return bounds;
        }
        double xMin = Math.min(bounds.xMin(), xlim[0]);
        double xMax = Math.max(bounds.xMax(), xlim[1]);
        double yMin = Math.min(bounds.yMin(), ylim[0]);
        double yMax = Math.max(bounds.yMax(), ylim[1]);
        return new Layer.Bounds(xMin, xMax, yMin, yMax);
    }

    @Override
    protected List<Poly3d> polygonize(StatData statData, PanelContext<DF> ctx) {
        var xCol = statData.column("x");
        var yCol = statData.column("y");
        var zCol = statData.column("z");
        var gCol = statData.column("group");
        int n = statData.rowCount();
        if (n < 3) {
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
        int[] row = statData.column("row") != null ? toInts(statData.column("row")) : null;
        int[] col = statData.column("col") != null ? toInts(statData.column("col")) : null;
        var spec = getSpec();
        var tiles = MeshUtil.pointsToTiles(x, y, g, row, col,
                spec.getMethod(), spec.getGrid(), "surfaceTile");
        var out = new ArrayList<Poly3d>(tiles.size());
        for (var tile : tiles) {
            var idx = tile.vertexIdx();
            var xs = new double[idx.length];
            var ys = new double[idx.length];
            var zs = new double[idx.length];
            for (int k = 0; k < idx.length; k++) {
                xs[k] = x[idx[k]];
                ys[k] = y[idx[k]];
                zs[k] = z[idx[k]];
            }
            out.add(new Poly3d(xs, ys, zs, tile.group(), idx));
        }
        return out;
    }

    private static int[] toInts(List<Object> col) {
        var out = new int[col.size()];
        for (int i = 0; i < col.size(); i++) {
            out[i] = ((Number) col.get(i)).intValue();
        }
        return out;
    }
}
