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

import org.jtaccuino.gog.coord.CubeFace;

/**
 * A position adjustment, following the {@code position adjustment} functions
 * ({@code Positions.dodge(width)}, {@code Positions.stack(reverse)},
 * {@code Positions.jitter()}, {@code Positions.nudge()} ...).
 * <p>
 * Each variant is an immutable record whose values drive how a geometry's
 * marks are repositioned before drawing, or one of the {@link Position} enum
 * constants operating as the shared default adjustment of its family. Code
 * that only needs a bare mode (no parameters) passes the enum constant
 * directly; anything wanting an explicit width, a reversed stack, or a nudging
 * offset uses one of the parameterised records, either via the
 * {@link Positions} factories or chained from the enum constant itself
 * ({@code Position.DODGE.dodge(0.7)}).
 *
 * @see Positions
 * @see Position
 */
public sealed interface PositionAdjust
        permits PositionAdjust.Identity, PositionAdjust.Dodge, PositionAdjust.Dodge2,
        PositionAdjust.Stack, PositionAdjust.Fill, PositionAdjust.Jitter,
        PositionAdjust.JitterDodge, PositionAdjust.Nudge, PositionAdjust.PositionOnFace,
        Position {

    /** No repositioning — marks keep their data positions. */
    record Identity() implements PositionAdjust {}

    /**
     * Separates categories that share an {@code x} position side-by-side.
     *
     * @param width the width as a fraction of the group spacing, in {@code (0, 1]}
     */
    record Dodge(double width) implements PositionAdjust {}

    /**
     * Positions categories side-by-side on a (possibly reversed) secondary axis.
     *
     * @param width   the width as a fraction of the group spacing
     * @param reverse whether to reverse the category order
     */
    record Dodge2(double width, boolean reverse) implements PositionAdjust {}

    /**
     * Stacks categories on top of one another, optionally reversing their order.
     *
     * @param reverse whether to reverse the stacking order
     */
    record Stack(boolean reverse) implements PositionAdjust {}

    /** Stacks categories and normalises the cumulative total to fill the axis. */
    record Fill() implements PositionAdjust {}

    /**
     * Adds a small random offset in both axes to reveal overplotting.
     *
     * @param xj the horizontal jitter offset
     * @param yj the vertical jitter offset
     */
    record Jitter(double xj, double yj) implements PositionAdjust {}

    /**
     * Dodges columns and jitters the points inside each dodge slot.
     *
     * @param width   the dodge width fraction
     * @param jwidth  the horizontal jitter width
     * @param jheight the vertical jitter height
     */
    record JitterDodge(double width, double jwidth, double jheight) implements PositionAdjust {}

    /**
     * Shifts every mark by a fixed data offset.
     *
     * @param x the horizontal data offset
     * @param y the vertical data offset
     */
    record Nudge(double x, double y) implements PositionAdjust {}

    /**
     * Places a layer on one of the cube faces of a 3-D scene.
     * <p>
     * A native 3-D layer (one mapping a {@code z} aesthetic) carries
     * {@code axes = null}: every mark is projected orthogonally onto the face
     * plane, flattening the layer onto the cube side like its shadow. A 2-D
     * layer (mapping only {@code x} and {@code y}) carries two in-face axes
     * (e.g. {@code ["x", "z"]} on the {@link CubeFace#YMAX} face): the layer's
     * x/y values are interpreted along those two 3-D dimensions, with the
     * third dimension fixed at the face plane.
     *
     * @param face the cube face the layer is drawn onto
     * @param axes the two in-face 3-D dimensions ({@code "x"}, {@code "y"},
     *             {@code "z"}) a 2-D layer's x and y are mapped to, or
     *             {@code null} to flatten a native 3-D layer
     */
    record PositionOnFace(CubeFace face, List<String> axes) implements PositionAdjust {}

    /**
     * The bare {@link Position} mode this adjustment belongs to, used to drive
     * the geometry's branch dispatch while the numeric parameters stay on the
     * record.
     *
     * @return the equivalent bare {@link Position} mode
     */
    default Position mode() {
        return switch (this) {
            case Identity _ -> Position.IDENTITY;
            case Dodge _, Dodge2 _ -> Position.DODGE;
            case Stack _, Fill _ -> Position.STACK;
            case Jitter _, JitterDodge _, Nudge _, PositionOnFace _ -> Position.IDENTITY;
            case Position p -> p;
        };
    }
}
