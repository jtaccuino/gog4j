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
package org.jtaccuino.gog.sampler.ui;

import java.util.Objects;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.jtaccuino.gog.RenderMode;

/**
 * A sliding switch for picking the {@link RenderMode}: a pill track with a
 * knob that carries the current mode's label. Clicking (or pressing Space with
 * the control focused) flips between FAST and FULL. The selection is mirrored
 * through two {@link ToggleButton}s whose own {@code selected} state drives
 * {@link #renderModeProperty()} and vice versa.
 */
public final class RenderModeSwitch extends StackPane {

    private static final double TRACK_WIDTH = 84;
    private static final double TRACK_HEIGHT = 26;
    private static final double TRACK_RADIUS = TRACK_HEIGHT / 2;
    private static final double KNOB_WIDTH = 42;
    private static final double KNOB_HEIGHT = TRACK_HEIGHT - 4;
    private static final double KNOB_RADIUS = KNOB_HEIGHT / 2;

    private final Region track = new Region();
    private final StackPane knob = new StackPane();
    private final Label knobLabel = new Label();
    private final ToggleButton fast = new ToggleButton("FAST");
    private final ToggleButton full = new ToggleButton("FULL");
    private final TranslateTransition slide;
    private final ObjectProperty<RenderMode> renderMode =
            new SimpleObjectProperty<>(this, "renderMode", RenderMode.FAST);

    /** Creates the switch, defaulting to {@link RenderMode#FAST}. */
    @SuppressWarnings("this-escape")
    public RenderModeSwitch() {
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setFocusTraversable(true);

        track.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        // A flat recessed-looking pill: a solid fill with a soft outer shadow.
        // Gradients and inner shadows band/alias against the sliding knob and
        // the focus ring, so the track stays a clean solid curve.
        track.setStyle("-fx-background-color: #d6d6d6; -fx-background-radius: "
                + TRACK_RADIUS + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.18), 3, 0.3, 0, 1);"
                + "-fx-cursor: hand;");

        knob.setPrefSize(KNOB_WIDTH, KNOB_HEIGHT);
        knob.setMaxSize(KNOB_WIDTH, KNOB_HEIGHT);
        knob.setMinSize(KNOB_WIDTH, KNOB_HEIGHT);
        knobLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #444444;");
        knob.getChildren().add(knobLabel);
        applyKnobStyle();

        // Hover and focus restyle the knob. The focus ring is an integer-width
        // solid border so the curve stays crisp instead of a fuzzy halo.
        knob.hoverProperty().addListener((obs, old, on) -> applyKnobStyle());
        focusedProperty().addListener((obs, old, on) -> applyKnobStyle());

        getChildren().addAll(track, knob);

        slide = new TranslateTransition(Duration.millis(160), knob);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        fast.setSelected(true);
        fast.selectedProperty().addListener((obs, old, on) -> {
            if (on) {
                renderMode.set(RenderMode.FAST);
            }
        });
        full.selectedProperty().addListener((obs, old, on) -> {
            if (on) {
                renderMode.set(RenderMode.FULL);
            }
        });
        renderMode.addListener((obs, old, mode) -> {
            var wantFull = Objects.equals(mode, RenderMode.FULL);
            fast.setSelected(!wantFull);
            full.setSelected(wantFull);
            step();
        });

        setOnMouseClicked(e -> flip());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                flip();
                e.consume();
            }
        });
        // Position the knob without animating: construction happens before the
        // control is on a scene, so a transition would play invisibly and could
        // leave the knob mid-slide when the first pulse arrives.
        knobLabel.setText("FAST");
        knob.setTranslateX(-swing());
    }

    /** The render mode selected by the switch. */
    public ObjectProperty<RenderMode> renderModeProperty() {
        return renderMode;
    }

    /** The FAST click model, exposed for tests. */
    public ToggleButton fast() {
        return fast;
    }

    /** The FULL click model, exposed for tests. */
    public ToggleButton full() {
        return full;
    }

    /**
     * The knob's horizontal travel from the centred position. The knob is
     * vertically inset by {@code (TRACK_HEIGHT - KNOB_HEIGHT) / 2}; to keep its
     * end caps concentric with the track's at the resting positions, the same
     * inset is applied horizontally, so the two curves share a centre.
     */
    private static double swing() {
        var inset = (TRACK_HEIGHT - KNOB_HEIGHT) / 2;
        return TRACK_WIDTH / 2 - KNOB_WIDTH / 2 - inset;
    }

    /**
     * Flips to the other render mode. The knob slides first and the shared
     * {@link #renderMode} property is only committed once the slide completes:
     * committing eagerly would start the plot reload storm (every visible card
     * re-renders under the new mode) while the knob is still animating, and
     * those FX-thread installs would starve the transition into stutter.
     */
    private void flip() {
        slide.stop();
        var target = Objects.equals(renderMode.get(), RenderMode.FULL)
                ? RenderMode.FAST
                : RenderMode.FULL;
        var wantFull = Objects.equals(target, RenderMode.FULL);
        knobLabel.setText(wantFull ? "FULL" : "FAST");
        slide.setFromX(knob.getTranslateX());
        slide.setToX(wantFull ? swing() : -swing());
        slide.setOnFinished(e -> renderMode.set(target));
        slide.playFromStart();
    }

/**
     * Renders the knob as a crisp raised pill: a solid white fill with a single
     * hairline border and a soft drop shadow. Hover darkens the fill slightly;
     * focus swaps the hairline for an integer-width accent ring so the curve
     * renders cleanly rather than as a fuzzy halo.
     */
    private void applyKnobStyle() {
        var fill = knob.isHover() ? "#f6f6f6" : "#ffffff";
        if (isFocused()) {
            knob.setStyle("-fx-background-color: " + fill + "; -fx-background-radius: "
                    + KNOB_RADIUS + ";"
                    + "-fx-border-color: #4a90d9; -fx-border-width: 2; -fx-border-radius: "
                    + KNOB_RADIUS + ";"
                    + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 4, 0.3, 0, 1);"
                    + "-fx-cursor: hand;");
        } else {
            knob.setStyle("-fx-background-color: " + fill + "; -fx-background-radius: "
                    + KNOB_RADIUS + ";"
                    + "-fx-border-color: #d8d8d8; -fx-border-width: 1; -fx-border-radius: "
                    + KNOB_RADIUS + ";"
                    + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.18), 4, 0.3, 0, 1);"
                    + "-fx-cursor: hand;");
        }
    }

    /**
     * Slides the knob and its label to wherever the selection currently is,
     * used for programmatic mode changes (e.g. restore-on-startup). If the knob
     * is already there, just snap it in place instead of replaying the slide.
     */
    private void step() {
        slide.stop();
        var wantFull = Objects.equals(renderMode.get(), RenderMode.FULL);
        knobLabel.setText(wantFull ? "FULL" : "FAST");
        var target = wantFull ? swing() : -swing();
        if (Math.abs(knob.getTranslateX() - target) < 0.5) {
            knob.setTranslateX(target);
            return;
        }
        slide.setToX(target);
        slide.setFromX(knob.getTranslateX());
        slide.playFromStart();
    }
}
