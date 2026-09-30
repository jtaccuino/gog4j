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
import org.jtaccuino.gog.controls.SourceTheme;

/**
 * A sliding switch for picking the {@link SourceTheme}, mirroring the
 * {@link RenderModeSwitch} look: a pill track with a knob that carries the
 * selected theme's label. Clicking (or pressing Space with the control focused)
 * flips between LIGHT and DARK. The selection mirrors through two
 * {@link ToggleButton}s and is kept in sync with the shared
 * {@link SourceTheme#CURRENT}, so every source drawer restyles automatically.
 */
public final class ThemeSwitch extends StackPane {

    private static final double TRACK_WIDTH = 84;
    private static final double TRACK_HEIGHT = 26;
    private static final double TRACK_RADIUS = TRACK_HEIGHT / 2;
    private static final double KNOB_WIDTH = 42;
    private static final double KNOB_HEIGHT = TRACK_HEIGHT - 4;
    private static final double KNOB_RADIUS = KNOB_HEIGHT / 2;

    private final Region track = new Region();
    private final StackPane knob = new StackPane();
    private final Label knobLabel = new Label();
    private final ToggleButton light = new ToggleButton("LIGHT");
    private final ToggleButton dark = new ToggleButton("DARK");
    private final TranslateTransition slide;
    private final ObjectProperty<SourceTheme> theme =
            new SimpleObjectProperty<>(this, "theme", SourceTheme.LIGHT);
    /** Strong end of the shared-theme subscription; dies with this switch. */
    @SuppressWarnings("UnusedVariable") // the strong end of the weak subscription
    private final Runnable themeSubscription;

    /** Creates the switch, defaulting to the light theme. */
    @SuppressWarnings("this-escape")
    public ThemeSwitch() {
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setFocusTraversable(true);

        track.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
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

        knob.hoverProperty().addListener((obs, old, on) -> applyKnobStyle());
        focusedProperty().addListener((obs, old, on) -> applyKnobStyle());

        getChildren().addAll(track, knob);

        slide = new TranslateTransition(Duration.millis(160), knob);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        light.setSelected(true);
        light.selectedProperty().addListener((obs, old, on) -> {
            if (on) {
                theme.set(SourceTheme.LIGHT);
            }
        });
        dark.selectedProperty().addListener((obs, old, on) -> {
            if (on) {
                theme.set(SourceTheme.DARK);
            }
        });
        theme.addListener((obs, old, selected) -> {
            var wantDark = selected == SourceTheme.DARK;
            light.setSelected(!wantDark);
            dark.setSelected(wantDark);
            step();
        });
        // The shared scheme follows any selection made here (strong listener
        // pair whose strongest end is this switch's own theme property), while
        // the reverse direction runs through a weak subscription so a closed
        // switch is never pinned by the static theme property.
        theme.addListener((obs, old, selected) -> SourceTheme.CURRENT.set(selected));
        themeSubscription = SourceTheme.subscribe(theme::set);
        theme.set(SourceTheme.CURRENT.get());

        setOnMouseClicked(e -> flip());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                flip();
                e.consume();
            }
        });
        knobLabel.setText("LIGHT");
        knob.setTranslateX(-swing());
    }

    /** The theme selected by the switch, synced with the shared scheme. */
    public ObjectProperty<SourceTheme> themeProperty() {
        return theme;
    }

    /** The LIGHT click model, exposed for tests. */
    public ToggleButton light() {
        return light;
    }

    /** The DARK click model, exposed for tests. */
    public ToggleButton dark() {
        return dark;
    }

    private static double swing() {
        var inset = (TRACK_HEIGHT - KNOB_HEIGHT) / 2;
        return TRACK_WIDTH / 2 - KNOB_WIDTH / 2 - inset;
    }

    /** Flips to the other theme, sliding the knob before committing. */
    private void flip() {
        slide.stop();
        var target = theme.get() == SourceTheme.DARK
                ? SourceTheme.LIGHT
                : SourceTheme.DARK;
        var wantDark = target == SourceTheme.DARK;
        knobLabel.setText(wantDark ? "DARK" : "LIGHT");
        slide.setFromX(knob.getTranslateX());
        slide.setToX(wantDark ? swing() : -swing());
        slide.setOnFinished(e -> theme.set(target));
        slide.playFromStart();
    }

    /**
     * Renders the knob as a crisp raised pill; hover darkens the fill and focus
     * swaps the hairline for an integer-width accent ring.
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

    /** Slides the knob and label to the selected theme, snapping when already there. */
    private void step() {
        slide.stop();
        var wantDark = theme.get() == SourceTheme.DARK;
        knobLabel.setText(wantDark ? "DARK" : "LIGHT");
        var target = wantDark ? swing() : -swing();
        if (Math.abs(knob.getTranslateX() - target) < 0.5) {
            knob.setTranslateX(target);
            return;
        }
        slide.setToX(target);
        slide.setFromX(knob.getTranslateX());
        slide.playFromStart();
    }
}
