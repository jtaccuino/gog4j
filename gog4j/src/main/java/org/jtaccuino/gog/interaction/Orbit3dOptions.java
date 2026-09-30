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
package org.jtaccuino.gog.interaction;

/**
 * Configuration for an {@link Orbit3d} rotation controller: which drag gesture
 * drives the cube, and how the live pitch/roll/yaw readout is shown.
 * <p>
 * Instances are created from the static shorthand presets and fine-tuned with
 * the fluent mutators, so {@code Orbit3dOptions.defaults().readout(OVERLAY)}
 * is the idiomatic way to build them.
 */
public final class Orbit3dOptions {

    /**
     * How a drag on the plot rotates the cube.
     */
    public enum Gesture {

        /**
         * Horizontal drag rotates the view axis (yaw), vertical drag tips the
         * elevation (pitch); holding Alt (or dragging with the right button)
         * rolls about the viewing direction instead. Predictable on a mouse
         * and a trackpad, and the conventional camera-orbit mapping of
         * horizontal motion to yaw and vertical motion to pitch.
         */
        MODIFIER,

        /**
         * A direct "grab and turn" gesture: the cursor carries a point on an
         * invisible sphere centred on the plot, so dragging spins the cube
         * about the swept axis — a combined motion that covers all three
         * angles in one continuous drag.
         */
        TRACKBALL
    }

    /**
     * How the live pitch/roll/yaw angles are surfaced while rotating.
     */
    public enum Readout {

        /** No angle readout is drawn. */
        NONE,

        /** A pill following the cursor while the plot is dragged. */
        POPUP,

        /** A fixed pill pinned to the bottom-left corner of the plot. */
        OVERLAY
    }

    /**
     * The default rotation sensitivity: degrees of rotation per pixel of drag
     * for the {@link Gesture#MODIFIER} gesture, and the scale of the swept
     * trackball angle (1.0 = the cursor's arc maps one-to-one to the cube's
     * rotation). Lower values turn the cube more slowly for a given drag.
     */
    public static final double DEFAULT_DEGREES_PER_PIXEL = 0.08;

    private Gesture gesture = Gesture.TRACKBALL;
    private Readout readout = Readout.OVERLAY;
    private double degreesPerPixel = DEFAULT_DEGREES_PER_PIXEL;

    private Orbit3dOptions() {
    }

    /**
     * {@return the default {@link Orbit3dOptions}}, a trackball drag with an
     * always-visible overlay readout}
     */
    public static Orbit3dOptions defaults() {
        return new Orbit3dOptions();
    }

    /**
     * {@return an {@link Orbit3dOptions} with the {@link Gesture#MODIFIER}
     * drag and the {@link Readout#OVERLAY} readout}
     */
    public static Orbit3dOptions controlled() {
        return new Orbit3dOptions().gesture(Gesture.MODIFIER).readout(Readout.OVERLAY);
    }

    /**
     * Sets the drag gesture.
     *
     * @param gesture the gesture style
     * @return this options instance for fluid chaining
     */
    public Orbit3dOptions gesture(Gesture gesture) {
        this.gesture = gesture;
        return this;
    }

    /**
     * The drag gesture.
     *
     * @return the gesture style
     */
    public Gesture gesture() {
        return gesture;
    }

    /**
     * Sets the angle readout style.
     *
     * @param readout the readout style
     * @return this options instance for fluid chaining
     */
    public Orbit3dOptions readout(Readout readout) {
        this.readout = readout;
        return this;
    }

    /**
     * The angle readout style.
     *
     * @return the readout style
     */
    public Readout readout() {
        return readout;
    }

    /**
     * Sets the rotation sensitivity: degrees of rotation per pixel of drag.
     * Lower values make the cube turn more slowly for a given drag distance.
     *
     * @param degreesPerPixel the sensitivity, in degrees per pixel
     * @return this options instance for fluid chaining
     */
    public Orbit3dOptions degreesPerPixel(double degreesPerPixel) {
        this.degreesPerPixel = degreesPerPixel;
        return this;
    }

    /**
     * The rotation sensitivity.
     *
     * @return the degrees of rotation per pixel of drag
     */
    public double degreesPerPixel() {
        return degreesPerPixel;
    }
}
