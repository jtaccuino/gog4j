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

import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;

/**
 * conventional static factories for {@link PositionAdjust}. Each method
 * mirrors the corresponding {@code position adjustment} function and
 * returns an immutable adjustment record with the default parameters.
 * <p>
 * Callers never construct the {@code PositionAdjust} records directly — the
 * records' canonical constructors are an internal implementation detail — they
 * always go through these factories, matching the {@code Geoms} / {@code Scale}
 * style of the rest of the API.
 *
 * @see PositionAdjust
 */
public final class Positions {

    /** The default dodge width, a fraction of the group spacing. */
    public static final double DODGE_WIDTH = 0.9;

    private Positions() {
        // utility class — no public constructor
    }

    /**
     * {@return the default {@code PositionAdjust} that leaves marks at their data positions}
     * <p>
     * The default variants are the {@link Position} enum constants themselves.
     */
    public static PositionAdjust identity() {
        return Position.IDENTITY;
    }

    /**
     * {@return the default-width {@code PositionAdjust.Dodge} (the {@code Positions.dodge()})}
     * <p>
     * This is the {@link Position#DODGE} constant itself.
     */
    public static PositionAdjust dodge() {
        return Position.DODGE;
    }

    /**
     * {@return a {@code PositionAdjust.Dodge} that separates categories by the given width fraction}
     *
     * @param width the width as a fraction of the group spacing, in {@code (0, 1]}
     */
    public static PositionAdjust dodge(double width) {
        return new PositionAdjust.Dodge(width);
    }

    /**
     * {@return a {@code PositionAdjust.Dodge2} for a secondary axis, optionally reversed}
     *
     * @param width   the width as a fraction of the group spacing
     * @param reverse whether to reverse the category order
     */
    public static PositionAdjust dodge2(double width, boolean reverse) {
        return new PositionAdjust.Dodge2(width, reverse);
    }

    /**
     * {@return the forward-ordered {@code PositionAdjust.Stack} (the {@code Positions.stack()})}
     * <p>
     * This is the {@link Position#STACK} constant itself.
     */
    public static PositionAdjust stack() {
        return Position.STACK;
    }

    /**
     * {@return a {@code PositionAdjust.Stack} with the given ordering}
     *
     * @param reverse whether to reverse the stacking order
     */
    public static PositionAdjust stack(boolean reverse) {
        return new PositionAdjust.Stack(reverse);
    }

    /**
     * {@return the {@code PositionAdjust.Fill} that normalises stacked totals to fill the axis}
     * <p>
     * This is the {@link Position#FILL} constant itself.
     */
    public static PositionAdjust fill() {
        return Position.FILL;
    }

    /**
     * {@return a {@code PositionAdjust.Jitter} adding the given symmetric offsets}
     *
     * @param xj the horizontal jitter offset
     * @param yj the vertical jitter offset
     */
    public static PositionAdjust jitter(double xj, double yj) {
        return new PositionAdjust.Jitter(xj, yj);
    }

    /**
     * {@return a {@code PositionAdjust.JitterDodge} combining dodging and jitter}
     *
     * @param width   the dodge width fraction
     * @param jwidth  the horizontal jitter width
     * @param jheight the vertical jitter height
     */
    public static PositionAdjust jitterdodge(double width, double jwidth, double jheight) {
        return new PositionAdjust.JitterDodge(width, jwidth, jheight);
    }

    /**
     * {@return a {@code PositionAdjust.Nudge} shifting every mark by a fixed offset}
     *
     * @param x the horizontal data offset
     * @param y the vertical data offset
     */
    public static PositionAdjust nudge(double x, double y) {
        return new PositionAdjust.Nudge(x, y);
    }

    /**
     * {@return a face projection that flattens a native 3-D layer onto one cube face}
     * <p>
     * Every mark of a layer mapping a {@code z} aesthetic is projected
     * orthogonally onto the given face. Throws when the face name is not a
     * valid cube side.
     *
     * @param face the cube face (e.g. {@link CubeFace#ZMIN}) to project onto
     */
    public static PositionAdjust positionOnFace(CubeFace face) {
        return new PositionAdjust.PositionOnFace(face, null);
    }

    /**
     * {@return a face projection placing a 2-D layer (x/y only) onto a cube face}
     * <p>
     * The layer's {@code x} and {@code y} aesthetics are interpreted along the
     * two in-face 3-D dimensions ({@code axis1}, {@code axis2}), with the third
     * dimension fixed at the face plane.
     *
     * @param face  the cube face the layer is drawn onto
     * @param axis1 the 3-D dimension ({@code "x"}, {@code "y"} or {@code "z"})
     *              the layer's x aesthetic is mapped to
     * @param axis2 the 3-D dimension the layer's y aesthetic is mapped to
     */
    public static PositionAdjust positionOnFace(CubeFace face, String axis1, String axis2) {
        return new PositionAdjust.PositionOnFace(face, List.of(validateAxes(face, axis1, axis2)));
    }

    /**
     * {@return a face projection flattening a native 3-D layer onto one cube face}
     * <p>
     * Convenience overload taking the face name as a string — {@code "xmin"},
     * {@code "xmax"}, {@code "ymin"}, {@code "ymax"}, {@code "zmin"} or
     * {@code "zmax"}.
     *
     * @param face the cube face name to project onto (e.g. {@code "zmin"})
     * @throws IllegalArgumentException when the name is not a valid cube face
     */
    public static PositionAdjust positionOnFace(String face) {
        return new PositionAdjust.PositionOnFace(parseFace(face), null);
    }

    /**
     * {@return a face projection placing a 2-D layer (x/y only) onto a cube face}
     * <p>
     * Convenience overload taking the face name as a string, with the layer's
     * {@code x} and {@code y} aesthetics mapped along the two in-face 3-D
     * dimensions.
     *
     * @param face  the cube face name the layer is drawn onto (e.g. {@code "zmax"})
     * @param axis1 the 3-D dimension ({@code "x"}, {@code "y"} or {@code "z"})
     *              the layer's x aesthetic is mapped to
     * @param axis2 the 3-D dimension the layer's y aesthetic is mapped to
     * @throws IllegalArgumentException when the face name is not valid or the
     *                                  axes are not two distinct in-face dimensions
     */
    public static PositionAdjust positionOnFace(String face, String axis1, String axis2) {
        return new PositionAdjust.PositionOnFace(parseFace(face), List.of(validateAxes(parseFace(face), axis1, axis2)));
    }

    /**
     * Validates that the two layer axes are distinct in-face dimensions and do
     * not collide with the face's own axis.
     *
     * @param face  the cube face
     * @param axis1 the first in-face dimension
     * @param axis2 the second in-face dimension
     * @return the validated axes array
     */
    private static String[] validateAxes(CubeFace face, String axis1, String axis2) {
        int ax1 = axis(axis1);
        int ax2 = axis(axis2);
        if (ax1 == ax2) {
            throw new IllegalArgumentException("positionOnFace axes must name two distinct dimensions, got "
                    + axis1 + " and " + axis2);
        }
        int faceAxis = Coord3D.faceAxis(face);
        if (ax1 == faceAxis || ax2 == faceAxis) {
            throw new IllegalArgumentException("positionOnFace axes " + axis1 + "/" + axis2
                    + " must be the two in-face dimensions of the " + face.name() + " face, excluding the face axis");
        }
        return new String[]{axis1, axis2};
    }

    /** {@return the axis index (0=x, 1=y, 2=z) of an axis name}, validated. */
    private static int axis(String axis) {
        return switch (axis) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> throw new IllegalArgumentException("positionOnFace axis must be one of \"x\", \"y\", \"z\", got " + axis);
        };
    }

    /** Parses a cube face name ({@code "xmin"} ... {@code "zmax"}) into a {@link CubeFace}. */
    private static CubeFace parseFace(String face) {
        return switch (face) {
            case "xmin" -> CubeFace.XMIN;
            case "xmax" -> CubeFace.XMAX;
            case "ymin" -> CubeFace.YMIN;
            case "ymax" -> CubeFace.YMAX;
            case "zmin" -> CubeFace.ZMIN;
            case "zmax" -> CubeFace.ZMAX;
            default -> throw new IllegalArgumentException(
                    "positionOnFace face must be one of xmin, xmax, ymin, ymax, zmin, zmax, got " + face);
        };
    }
}
