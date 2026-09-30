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
package org.jtaccuino.gog.controls;

import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.paint.Color;

/**
 * The light/dark color scheme of the sampler's highlighted source view. The
 * light palette follows the classic NetBeans/JetBrains defaults; the dark
 * palette follows Darcula. All visible source drawers share one scheme through
 * {@link #CURRENT}.
 */
@SuppressWarnings("ImmutableEnumChecker") // javafx.scene.paint.Color is immutable
public enum SourceTheme {

    /** Light scheme (NetBeans/JetBrains defaults) on a white background. */
    LIGHT(false,
            Color.web("#1f1f1f"),
            Color.web("#0033b3"),
            Color.web("#067d17"),
            Color.web("#8c8c8c"),
            Color.web("#1750eb"),
            Color.web("#808000"),
            Color.web("#00627a")),

    /** Dark scheme (Darcula-like) on a dark background. */
    DARK(true,
            Color.web("#a9b7c6"),
            Color.web("#cc7832"),
            Color.web("#6a8759"),
            Color.web("#808080"),
            Color.web("#6897bb"),
            Color.web("#bbb529"),
            Color.web("#ffc66d"));

    /** The theme currently applied to every source drawer. */
    public static final ObjectProperty<SourceTheme> CURRENT =
            new SimpleObjectProperty<>(LIGHT);

    /**
     * Subscribes to every change of {@link #CURRENT} without holding the
     * consumer (and everything it captures, typically a UI component) alive.
     * The static property only references the listener weakly, so a window
     * that is closed and dropped simply becomes garbage together with its
     * subscription instead of being pinned for the lifetime of the JVM.
     * <p>
     * Callers must keep the returned handle strongly referenced (a field on
     * the subscribing component): it is the strong end that keeps the filter
     * effective for as long as the component lives.
     *
     * @param onChange effect to run on each change
     * @return a handle to remove the subscription immediately if desired
     */
    public static Runnable subscribe(Consumer<SourceTheme> onChange) {
        ChangeListener<SourceTheme> listener = (obs, oldTheme, theme) -> onChange.accept(theme);
        CURRENT.addListener(new WeakChangeListener<>(listener));
        return () -> CURRENT.removeListener(listener);
    }

    private final boolean dark;
    private final Color text;
    private final Color keyword;
    private final Color string;
    private final Color comment;
    private final Color number;
    private final Color annotation;
    private final Color method;

    SourceTheme(boolean dark, Color text, Color keyword, Color string, Color comment,
            Color number, Color annotation, Color method) {
        this.dark = dark;
        this.text = text;
        this.keyword = keyword;
        this.string = string;
        this.comment = comment;
        this.number = number;
        this.annotation = annotation;
        this.method = method;
    }

    public boolean isDark() {
        return dark;
    }

    /** Default text color (identifiers, operators, punctuation). */
    public Color text() {
        return text;
    }

    public Color keyword() {
        return keyword;
    }

    public Color string() {
        return string;
    }

    public Color comment() {
        return comment;
    }

    public Color number() {
        return number;
    }

    public Color annotation() {
        return annotation;
    }

    /** Color of identifiers that are invoked as methods. */
    public Color method() {
        return method;
    }
}
