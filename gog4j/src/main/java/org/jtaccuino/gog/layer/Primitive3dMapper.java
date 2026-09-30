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

import java.util.List;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.scale.ColorResolver;
import org.jtaccuino.gog.theme.CubeStyle;

/**
 * Shared per-row mapping for the 3-D primitive geometry layers (points, text,
 * segments and paths): reads the mapped {@code x}/{@code y}/{@code z} data
 * columns plus the optional per-row {@code colour}/{@code alpha} aesthetics,
 * standardises and projects each row with the 3-D coord, and resolves its
 * colour and opacity from the geometry's defaults and the plot's scales — the
 * same column-reading, colour and opacity logic those four geometries used to
 * duplicate locally.
 */
public final class Primitive3dMapper {

    /**
     * One projected, coloured, opacity-bearing data row.
     *
     * @param x           the 3-D x coordinate
     * @param y           the 3-D y coordinate
     * @param z           the standardised z coordinate
     * @param sx          the projected screen x
     * @param sy          the projected screen y
     * @param depthScale  the camera nearness, larger means nearer
     * @param depthFactor the perspective scale cue, {@code 1.0} when the cue is off
     * @param color       the resolved per-row colour
     * @param alpha       the resolved per-row opacity
     */
    public record ResolvedPrimitive3d(double x, double y, double z, double sx, double sy,
            double depthScale, double depthFactor, Color color, double alpha) {}

    private final Coord3D coord;
    private final int n;
    private final List<?> rawX;
    private final List<?> rawY;
    private final List<?> rawZ;
    private final List<?> colorData;
    private final ColorResolver colorResolver;
    private final List<?> alphaData;
    private final MinMax alphaRange;
    private final Color defaultColor;
    private final double defaultAlpha;
    private final double depthStrength;

    private Primitive3dMapper(Coord3D coord, int n, List<?> rawX, List<?> rawY, List<?> rawZ,
            List<?> colorData, ColorResolver colorResolver, List<?> alphaData, MinMax alphaRange,
            Color defaultColor, double defaultAlpha, double depthStrength) {
        this.coord = coord;
        this.n = n;
        this.rawX = rawX;
        this.rawY = rawY;
        this.rawZ = rawZ;
        this.colorData = colorData;
        this.colorResolver = colorResolver;
        this.alphaData = alphaData;
        this.alphaRange = alphaRange;
        this.defaultColor = defaultColor;
        this.defaultAlpha = defaultAlpha;
        this.depthStrength = depthStrength;
    }

    /**
     * Builds a mapper over the panel's partition for the geometry's mapped
     * aesthetics and defaults.
     *
     * @param ctx           the panel context providing the data, coordinate and scales
     * @param defaultColor  the geometry's constant colour, used when no per-row colour is mapped
     * @param defaultAlpha  the geometry's constant opacity base
     * @param depthStrength the perspective depth-scale strength, {@code 0} disables the cue
     * @param <DF>          the DataFrame type
     * @return the mapper
     */
    public static <DF> Primitive3dMapper forPrimitives(PanelContext<DF> ctx,
            Color defaultColor, double defaultAlpha, double depthStrength) {
        var aes = ctx.plot().aes();
        var coord = (Coord3D) ctx.plot().coord();
        var df = ctx.partitionDf();
        var ext = ctx.plot().extractor();
        var rawX = aes.x() != null ? ext.getColumn(df, aes.x()) : null;
        var rawY = aes.y() != null ? ext.getColumn(df, aes.y()) : null;
        var rawZ = aes.z() != null ? ext.getColumn(df, aes.z()) : null;
        var n = ext.getRowCount(df);

        var colorCol = aes.color() != null ? aes.color() : aes.fill();
        var colorData = colorCol != null ? ext.getColumn(df, colorCol) : null;
        var colorResolver = colorCol != null ? ctx.plot().scales().colorResolverFor(colorCol) : null;
        var alphaCol = aes.alpha();
        var alphaData = alphaCol != null ? ext.getColumn(df, alphaCol) : null;
        var alphaRange = alphaData != null ? ctx.plot().scales().numericRange(alphaCol) : null;

        return new Primitive3dMapper(coord, n, rawX, rawY, rawZ, colorData, colorResolver,
                alphaData, alphaRange, defaultColor, defaultAlpha, depthStrength);
    }

    /** {@return the number of mapped data rows} */
    public int rowCount() {
        return n;
    }

    /**
     * {@return whether the row carries a full, non-null coordinate triple}
     *
     * @param i the row index
     */
    public boolean has(int i) {
        return rawX != null && rawY != null && rawZ != null
                && i < rawX.size() && i < rawY.size() && i < rawZ.size()
                && rawX.get(i) != null && rawY.get(i) != null && rawZ.get(i) != null;
    }

    /**
     * {@return the row's raw x value; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public Object rawX(int i) {
        return rawX.get(i);
    }

    /**
     * {@return the row's raw y value; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public Object rawY(int i) {
        return rawY.get(i);
    }

    /**
     * {@return the row's raw z value; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public Object rawZ(int i) {
        return rawZ.get(i);
    }

    /**
     * {@return the row's numeric x coordinate; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public double x(int i) {
        return Values.toDouble(rawX.get(i));
    }

    /**
     * {@return the row's numeric y coordinate; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public double y(int i) {
        return Values.toDouble(rawY.get(i));
    }

    /**
     * {@return the row's standardised z coordinate; callers should guard with {@link #has(int)}}
     *
     * @param i the row index
     */
    public double z(int i) {
        return coord.toZValue(rawZ.get(i));
    }

    /**
     * {@return the row's resolved colour, falling back to the geometry's
     * default when no colour maps or the mapped value resolves to nothing}
     *
     * @param i the row index
     */
    public Color color(int i) {
        if (colorData != null && i < colorData.size()) {
            var gv = colorData.get(i);
            if (gv != null && colorResolver != null) {
                var resolved = colorResolver.forValue(gv);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return defaultColor;
    }

    /**
     * {@return the row's resolved opacity, scaling the geometry's default base
     * by the mapped alpha value when one is present}
     *
     * @param i the row index
     */
    public double alpha(int i) {
        double alpha = defaultAlpha;
        if (alphaData != null && i < alphaData.size() && alphaRange != null) {
            var av = alphaData.get(i);
            if (av instanceof Number num) {
                double norm = (num.doubleValue() - alphaRange.min()) / (alphaRange.max() - alphaRange.min());
                alpha *= Math.max(0.0, Math.min(1.0, norm));
            }
        }
        return alpha;
    }

    /**
     * {@return the row's projection, or {@code null} for rows without coordinates}
     *
     * @param i the row index
     */
    public Coord3D.ProjResult project(int i) {
        if (!has(i)) {
            return null;
        }
        return coord.projectData(x(i), y(i), z(i));
    }

    /**
     * {@return the fully resolved row — projected, coloured and opacity-bearing —
     * or {@code null} for rows without coordinates}
     *
     * @param i the row index
     */
    public ResolvedPrimitive3d resolve(int i) {
        if (!has(i)) {
            return null;
        }
        var x = x(i);
        var y = y(i);
        var z = z(i);
        var proj = coord.projectData(x, y, z);
        return new ResolvedPrimitive3d(x, y, z, proj.sx(), proj.sy(), proj.depthScale(),
                CubeStyle.depthFactor(proj.depthScale(), depthStrength), color(i), alpha(i));
    }
}
