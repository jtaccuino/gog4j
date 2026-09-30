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
package org.jtaccuino.gog.stat;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.MeshUtil;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.function3d()} — evaluates a user-supplied {@code f(x, y) = z}
 * over a regular grid and emits the point rows with per-point gradients.
 * <p>
 * The function is provided via the {@code LayerParams} stat bag under the key
 * {@code "fun"} (a {@link DoubleBinaryOperator}). The plotting grid follows the
 * {@code make_point_grid()} contract: {@code grid} (rectangle/right1/right2/
 * equilateral), {@code n} (per-dimension resolution, default 40), and
 * {@code direction}.
 *
 * @param <DF> the DataFrame type
 */
public class StatFunction3d<DF> extends AbstractStat3d<DF> {

    /**
     * Constructs a Stats.function3d() transformation.
     */
    public StatFunction3d() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var fun = params.get("fun", null);
        if (!(fun instanceof DoubleBinaryOperator fn)) {
            return StatData.builder().build();
        }
        var grid = Stat3dSupport.gridParam(params, GridGeometry.RECTANGLE);
        var n = params.get("n", 40);
        var direction = Stat3dSupport.directionParam(params, GridDirection.X);
        var trim = params.getBoolean("trim", true);

        var xlim = Stat3dSupport.rangeParam(params, "xlim", dataRange(df, ext, aes.x()));
        var ylim = Stat3dSupport.rangeParam(params, "ylim", dataRange(df, ext, aes.y()));
        if (xlim == null || ylim == null) {
            return StatData.builder().build();
        }

        var pg = MeshUtil.makePointGrid(grid, Stat3dSupport.nParam(n), direction, xlim, ylim, trim);
        var x = pg.x();
        var y = pg.y();
        var z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            z[i] = fn.applyAsDouble(x[i], y[i]);
        }

        var grad = MeshUtil.gradients(x, y, z, null, null);
        var builder = StatData.builder();
        for (int i = 0; i < x.length; i++) {
            builder.add("x", x[i]);
            builder.add("y", y[i]);
            builder.add("z", z[i]);
            builder.add("group", "-1");
            addGradient(builder, grad, i);
        }
        return builder.build();
    }

    private double[] dataRange(DF df, DataExtractor<DF> ext, String column) {
        if (column == null) {
            return null;
        }
        var col = ext.getColumn(df, column);
        if (col == null || col.isEmpty()) {
            return null;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (var v : col) {
            if (v == null) {
                continue;
            }
            var d = Values.toDouble(v);
            if (Double.isFinite(d)) {
                min = Math.min(min, d);
                max = Math.max(max, d);
            }
        }
        if (!(min < max)) {
            return null;
        }
        return new double[] {min, max};
    }

    @Override
    public List<String> outputColumns() {
        return List.of("group", "dzdx", "dzdy", "slope", "aspect");
    }
}
