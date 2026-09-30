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
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.scale.Scale;

/**
 * {@link org.jtaccuino.gog.Geoms#path3d()} geometry: connects observations in
 * 3D space in the order they appear — the path is split into consecutive
 * segments ({@code <group>__seg<k>}) so the shared painter's-algorithm sorting
 * of {@link GeomSegment3d} can depth-sort the individual pieces while the path
 * keeps its connected appearance.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class GeomPath3d<DF> extends GeomSegment3d<DF> {

    /**
     * Constructs a {@link org.jtaccuino.gog.Geoms#path3d()} geometry.
     */
    public GeomPath3d() {
    }

    @Override
    protected List<Seg> buildSegments(PanelContext<DF> ctx) {
        var aes = ctx.plot().aes();
        var xCol = aes.x();
        var yCol = aes.y();
        var zCol = aes.z();
        if (xCol == null || yCol == null || zCol == null) return List.of();

        var df = ctx.partitionDf();
        var ext = ctx.plot().extractor();
        var groupCol = aes.group() != null ? aes.group() : aes.color();
        var groupData = groupCol != null ? ext.getColumn(df, groupCol) : null;

        var mapper = Primitive3dMapper.forPrimitives(ctx, color(), alpha() != null ? alpha() : 1.0, 0.0);

        var points = new ArrayList<PathVertex3d>(mapper.rowCount());
        for (var i = 0; i < mapper.rowCount(); i++) {
            if (!mapper.has(i)) continue;
            var gv = groupData != null && i < groupData.size() ? groupData.get(i) : null;
            points.add(new PathVertex3d(mapper.x(i), mapper.y(i), mapper.z(i), gv,
                    mapper.color(i), mapper.alpha(i)));
        }

        var segs = new ArrayList<Seg>(Math.max(0, points.size() - 1));
        var prev = points.isEmpty() ? null : points.get(0);
        for (int k = 1; k < points.size(); k++) {
            var cur = points.get(k);
            if (prev != null && sameGroup(prev.group(), cur.group())) {
                segs.add(new Seg(prev.x(), prev.y(), prev.z(),
                                 cur.x(), cur.y(), cur.z(),
                                 prev.stroke(), prev.alpha()));
            }
            prev = cur;
        }
        return segs;
    }

    private record PathVertex3d(double x, double y, double z, Object group, Color stroke, double alpha) {}

    private static boolean sameGroup(Object a, Object b) {
        if (a == null && b == null) return true;
        return a != null && a.equals(b);
    }
}
