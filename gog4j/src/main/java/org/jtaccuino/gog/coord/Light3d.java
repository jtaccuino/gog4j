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
package org.jtaccuino.gog.coord;

import java.util.Arrays;

/**
 * A light source for the 3D geometry family, as configured by the
 * {@code light(...)} method of the 3-D coord configurator and the
 * {@link Light3d} factories below.
 * <p>
 * Each polygon is shaded by the cosine between its outward normal and the
 * light; the {@link Method} selects what the cosine produces — the full
 * {@link Method#DIFFUSE diffuse} range in {@code [-1, 1]}, the clipped
 * {@link Method#DIRECT direct} range in {@code [0, 1]}, or a per-normal
 * {@link Method#RGB rgb} colour that <em>replaces</em> the fill/stroke —
 * while {@link Method#NONE}, like a {@code null} light, renders flat. The
 * value is blended into the {@code fill} and/or {@code color} channels in
 * {@link Mode#HSV} or {@link Mode#HSL} space, scaled by {@code contrast}.
 * <p>
 * The light can be a {@code direction} (a parallel beam from that direction)
 * or a {@code position} in the scene (a point light whose rays spread from a
 * location, optionally attenuated by inverse-square {@link #distanceFalloff()}
 * distance falloff). The {@code direction} stays fixed in {@link Anchor#SCENE}
 * coordinates, or follows the camera with {@link Anchor#CAMERA}. Backfaces —
 * polygons winding away from the camera — shade as
 * {@code backface_scale * light + backface_offset} instead of their own light
 * value, so the default {@code backface_scale = -1} lights the far side of a
 * surface from behind.
 *
 * @param method         how the light value is derived from the surface normal
 * @param mode           the colour space the shading is blended in
 * @param fill           whether the fill channel is shaded
 * @param color          whether the stroke (colour) channel is shaded
 * @param contrast       the shading strength (a multiplier on the cosine)
 * @param direction      the unit direction the light travels toward the surface
 * @param position       the point-light position in data space, or {@code null}
 *                       for a directional light
 * @param distanceFalloff whether a positional light attenuates with inverse
 *                       square distance
 * @param anchor         whether the light direction is fixed in the scene or
 *                       follows the camera
 * @param backfaceScale  the multiplier applied to backface light values
 * @param backfaceOffset the offset added to backface light values
 */
@SuppressWarnings("ArrayRecordComponent") // defensive-copied immutable vectors
public record Light3d(Method method, Mode mode, boolean fill, boolean color,
        double contrast, double[] direction, double[] position,
        boolean distanceFalloff, Anchor anchor, double backfaceScale,
        double backfaceOffset) {

    /** How a surface's orientation becomes a light value. */
    public enum Method {
        /** The full cosine of the normal and light, in {@code [-1, 1]}. */
        DIFFUSE,
        /** The cosine clipped to {@code [0, 1]} (no shady back side). */
        DIRECT,
        /** Normals map directly to per-face colours that replace fill/stroke. */
        RGB,
        /** No shading; polygons render flat. */
        NONE
    }

    /** The colour space the shading is blended in. */
    public enum Mode {
        /** Shading scales the HSV value channel (brightness toward black). */
        HSV,
        /** Shading scales the HSL lightness channel (brightness toward white). */
        HSL
    }

    /** What the light direction is anchored to. */
    public enum Anchor {
        /** The direction is fixed in scene (data/cube) space. */
        SCENE,
        /** The direction follows the camera's view rotation. */
        CAMERA
    }

    /** The default light direction, the unit vector {@code (-0.5, 0, 1)}. */
    private static final double[] DEFAULT_DIRECTION = {-0.5, 0, 1};

    /**
     * The default light: diffuse shading in HSV space at full contrast
     * ({@code contrast = 1.0}) from above-front along the direction
     * {@code (-0.5, 0, 1)}, with backfaces showing negative light.
     *
     * @return a light with the {@code light(...)} defaults
     */
    public static Light3d defaultLight() {
        return builder().build();
    }

    /**
     * A light that renders polygons flat — equivalent to a {@code null} light.
     *
     * @return a light with no shading
     */
    public static Light3d none() {
        return builder().method(Method.NONE).build();
    }

    /**
     * Creates a light with full explicit control over every parameter.
     *
     * @param method         the {@link Method}
     * @param mode           the {@link Mode}
     * @param fill           whether the fill channel is shaded
     * @param color          whether the stroke (colour) channel is shaded
     * @param contrast       the shading strength
     * @param direction      the light direction (any length, normalised)
     * @param position       the point-light position, or {@code null}
     * @param distanceFalloff whether a positional light attenuates by distance
     * @param anchor         the {@link Anchor}
     * @param backfaceScale  the backface multiplier
     * @param backfaceOffset the backface offset
     * @return a new {@link Light3d}
     */
    public static Light3d of(Method method, Mode mode, boolean fill, boolean color,
            double contrast, double[] direction, double[] position,
            boolean distanceFalloff, Anchor anchor, double backfaceScale,
            double backfaceOffset) {
        return new Light3d(method, mode, fill, color, contrast, direction,
                position, distanceFalloff, anchor, backfaceScale, backfaceOffset);
    }

    /**
     * {@return a builder seeded with the {@link #defaultLight()}
     * defaults} — {@code null}/{@code false} values are kept verbatim so a null
     * {@code light} at a layer that inherits is easily reproduced.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent constructor for a {@link Light3d}, defaulted to the
     * {@link #defaultLight()} spec. */
    public static final class Builder {

        private Method method = Method.DIFFUSE;
        private Mode mode = Mode.HSV;
        private boolean fill = true;
        private boolean color = true;
        private double contrast = 1.0;
        private double[] direction = DEFAULT_DIRECTION.clone();
        private double[] position;
        private boolean distanceFalloff;
        private Anchor anchor = Anchor.SCENE;
        private double backfaceScale = -1;
        private double backfaceOffset;

        Builder() {
        }

        /** Sets the light {@link Method}.
         *
         * @param method the {@link Method} (default {@link Method#DIFFUSE})
         * @return this
         */
        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        /** Sets the blend {@link Mode}.
         *
         * @param mode the {@link Mode} (default {@link Mode#HSV})
         * @return this
         */
        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        /** Sets whether the fill channel is shaded.
         *
         * @param fill whether the fill channel is shaded (default {@code true})
         * @return this
         */
        public Builder fill(boolean fill) {
            this.fill = fill;
            return this;
        }

        /** Sets whether the stroke channel is shaded.
         *
         * @param color whether the stroke channel is shaded (default {@code true})
         * @return this
         */
        public Builder color(boolean color) {
            this.color = color;
            return this;
        }

        /** Sets the shading strength.
         *
         * @param contrast the shading strength (default {@code 1})
         * @return this
         */
        public Builder contrast(double contrast) {
            this.contrast = contrast;
            return this;
        }

        /** Sets the light direction as an {@code (x, y, z)} vector.
         *
         * @param x the x component
         * @param y the y component
         * @param z the z component
         * @return this
         */
        public Builder direction(double x, double y, double z) {
            this.direction = new double[] {x, y, z};
            return this;
        }

        /** Sets the point-light position in data space.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @param z the z coordinate
         * @return this
         */
        public Builder position(double x, double y, double z) {
            this.position = new double[] {x, y, z};
            return this;
        }

        /** Sets whether positional lights attenuate by distance.
         *
         * @param distanceFalloff whether position lights attenuate by distance
         * @return this
         */
        public Builder distanceFalloff(boolean distanceFalloff) {
            this.distanceFalloff = distanceFalloff;
            return this;
        }

        /** Sets the light {@link Anchor}.
         *
         * @param anchor the {@link Anchor} (default {@link Anchor#SCENE})
         * @return this
         */
        public Builder anchor(Anchor anchor) {
            this.anchor = anchor;
            return this;
        }

        /** Sets the backface multiplier.
         *
         * @param backfaceScale the backface multiplier (default {@code -1})
         * @return this
         */
        public Builder backfaceScale(double backfaceScale) {
            this.backfaceScale = backfaceScale;
            return this;
        }

        /** Sets the backface offset.
         *
         * @param backfaceOffset the backface offset (default {@code 0})
         * @return this
         */
        public Builder backfaceOffset(double backfaceOffset) {
            this.backfaceOffset = backfaceOffset;
            return this;
        }

        /** {@return the configured {@link Light3d}} */
        public Light3d build() {
            return new Light3d(method, mode, fill, color, contrast, direction,
                    position, distanceFalloff, anchor, backfaceScale, backfaceOffset);
        }
    }

    /** Normalises the direction vector, defaulting and padding as needed. */
    public Light3d {
        method = method == null ? Method.DIFFUSE : method;
        mode = mode == null ? Mode.HSV : mode;
        anchor = anchor == null ? Anchor.SCENE : anchor;
        contrast = Math.max(0.0, contrast);
        direction = normalize(Arrays.copyOf(direction, 3));
        position = position == null ? null : Arrays.copyOf(position, 3);
    }

    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len > 0) {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        } else {
            v = new double[] {-0.5, 0, 1};
            len = Math.sqrt(1.25);
            v[0] /= len;
            v[2] /= len;
        }
        return v;
    }

    /** {@return the unit light direction (defensive copy)} */
    @Override
    public double[] direction() {
        return direction.clone();
    }

    /** {@return the point-light position (defensive copy), or {@code null}} */
    @Override
    public double[] position() {
        return position == null ? null : position.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Light3d other
                && method == other.method
                && mode == other.mode
                && fill == other.fill
                && color == other.color
                && contrast == other.contrast
                && distanceFalloff == other.distanceFalloff
                && anchor == other.anchor
                && backfaceScale == other.backfaceScale
                && backfaceOffset == other.backfaceOffset
                && Arrays.equals(direction, other.direction)
                && Arrays.equals(position, other.position);
    }

    @Override
    public int hashCode() {
        int h = 31 * (method.hashCode() + 31 * mode.hashCode())
                + (fill ? 1 : 0) + (color ? 1 : 0);
        h = 31 * h + Double.hashCode(contrast);
        h = 31 * h + (distanceFalloff ? 1 : 0) + anchor.hashCode();
        h = 31 * h + Double.hashCode(backfaceScale);
        h = 31 * h + Double.hashCode(backfaceOffset);
        h = 31 * h + Arrays.hashCode(direction);
        return 31 * h + Arrays.hashCode(position);
    }
}
