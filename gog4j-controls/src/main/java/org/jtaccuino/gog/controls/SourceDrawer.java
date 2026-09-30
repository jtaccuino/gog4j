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

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import jfx.incubator.scene.control.richtext.CodeArea;

/**
 * A source code drawer docked along the bottom border of a plot card. The
 * handle is a compact tab — the {@code Code} label with a chevron in an opaque
 * rounded outline — mounted at the top edge of the code panel. While collapsed
 * only that small tab shows at the card's bottom border (the panel is hidden
 * behind the clip), so the handle is clearly smaller than the drawer. Opening
 * slides the whole unit up so the handle rides to the top of a code panel that
 * overlays the description and the lower part of the plot. The drawer is
 * clipped to its own bounds and never stretches to the card's height.
 * <p>
 * The chevron on the handle points <em>up</em> while collapsed (the next click
 * opens the drawer up into the card) and <em>down</em> while open (the next
 * click moves it away). The displayed text is the factory method's body — the
 * {@code public static ... () { } } wrapper is dropped and the statements are
 * dedented.
 */
public class SourceDrawer extends StackPane {

    /** Maximum code rows shown before the panel starts scrolling internally. */
    private static final int MAX_VISIBLE_ROWS = 15;

    private final Button nudge = new Button("Code");
    private final SVGPath chevron = chevronIcon();
    private final Button copyButton = new Button();
    private final Label methodLabel = new Label();
    private final VBox panel = new VBox();
    private final HBox handleRow = new HBox();
    private final VBox unit = new VBox();
    private final CodeArea codeArea = new CodeArea();
    private final TranslateTransition slide;
    private final Rectangle clip = new Rectangle();
    /** Strong end of the shared-theme subscription; dies with this drawer. */
    @SuppressWarnings("UnusedVariable") // the strong end of the weak subscription
    private final Runnable themeSubscription;

    private final EventHandler<MouseEvent> outsideClickClose =
            e -> {
                if (!containsEventTarget(e.getTarget())) {
                    close();
                }
            };

    /**
     * Height of one rendered code line in the Menlo font. Measured lazily on
     * the first construction (never from a static initializer): probing a
     * {@link Text} requires the JavaFX toolkit, and initializing it during
     * class-load can wedge the toolkit before any {@code Platform.startup}.
     */
    private static volatile double codeLineHeight;
    private final EventHandler<KeyEvent> escapeClose =
            e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    close();
                    e.consume();
                }
            };
    private boolean open;
    /** The offset that hides the panel below the bottom edge when collapsed. */
    private double closedSlide;

    /**
     * Creates a collapsed drawer; call {@link #setSource} to populate it and
     * {@link #toggle()} (or click the {@code Code} handle) to slide it open.
     */
    @SuppressWarnings("this-escape")
    public SourceDrawer() {
        getStylesheets().add(getClass().getResource("source-drawer.css").toExternalForm());

        chevron.getStyleClass().add("source-drawer-nudge-chevron");
        nudge.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        nudge.setGraphic(chevron);
        nudge.getStyleClass().add("source-drawer-nudge");
        nudge.setFocusTraversable(true);
        nudge.setTooltip(new Tooltip("Show code"));
        nudge.setOnAction(e -> toggle());

        // The handle strip is a compact tab on the drawer's top edge holding
        // just the Code label (with its chevron); a press anywhere on the tab
        // toggles the drawer (a press on the button itself is left to the
        // button).
        handleRow.getStyleClass().add("source-drawer-handle-bar");
        handleRow.setAlignment(Pos.CENTER);
        handleRow.setSpacing(8);
        handleRow.setPickOnBounds(true);
        // The handle is a compact tab on the drawer box's top edge, not a band
        // spanning the whole drawer: it keeps its content width and is centered
        // by the unit's alignment.
        handleRow.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        handleRow.getChildren().add(nudge);
        handleRow.setOnMouseClicked(e -> {
            for (var node = e.getTarget() instanceof Node n ? n : null;
                    node != null; node = node.getParent()) {
                if (node.equals(nudge)) {
                    return;
                }
            }
            toggle();
        });

        methodLabel.getStyleClass().add("source-drawer-method");
        copyButton.setGraphic(copyIcon());
        copyButton.getStyleClass().add("source-drawer-icon-button");
        copyButton.setTooltip(new Tooltip("Copy source"));
        copyButton.setOnAction(e -> {
            var content = new ClipboardContent();
            content.putString(codeArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        var header = new HBox(10, methodLabel, copyButton);
        header.setAlignment(Pos.CENTER_LEFT);
        methodLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(methodLabel, Priority.ALWAYS);

        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.setLineNumbersEnabled(true);
        codeArea.setTabSize(4);
        codeArea.setFont(Font.font("Menlo", 12));
        codeArea.setSyntaxDecorator(new JavaSyntaxDecorator());

        panel.getStyleClass().add("source-drawer-panel");
        panel.setPadding(new Insets(10));
        panel.setSpacing(6);
        panel.getChildren().addAll(header, codeArea);

        // All drawers share one scheme; switching it re-styles the code area
        // and flips the drawer's theme CSS class. The field keeps the weak
        // subscription effective for as long as this drawer lives and lets it
        // be collected together with a closed window instead of pinning it.
        themeSubscription = SourceTheme.subscribe(theme -> applyTheme());
        applyTheme();

        unit.getChildren().addAll(handleRow, panel);
        // The handle is a compact tab on the drawer's top edge, kept at its
        // content width and centered; the panel fills the drawer box width.
        unit.prefWidthProperty().bind(widthProperty().subtract(24));
        unit.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        // The drawer box is centered within the (full-width, clipped) drawer;
        // its alignment also centers the narrow handle tab on the box's top edge.
        unit.setAlignment(Pos.TOP_CENTER);

        // The whole box slides up from the bottom edge; the clip conceals the
        // part of the unit that is still below the drawer's own bounds.
        getChildren().add(unit);
        StackPane.setAlignment(unit, Pos.TOP_CENTER);
        setPickOnBounds(false);
        panel.setMouseTransparent(true);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        slide = new TranslateTransition(Duration.millis(220), unit);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        // Height grows with the payload but must never be stretched by the
        // card's StackPane parent; the width is left flexible so the drawer box
        // keeps its natural width.
        setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        // Re-home the "close on outside click / ESC" listeners when the drawer
        // moves between scenes (virtualized cells are re-attached constantly).
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_CLICKED, outsideClickClose);
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, escapeClose);
            }
            if (open && newScene != null) {
                attachSceneHandlers();
            }
        });
    }

    /**
     * Populates the drawer with the body of the given factory source, or clears
     * it when {@code source} is {@code null}, and slides the drawer shut.
     *
     * @param methodName the factory method name shown on the handle
     * @param source the factory source whose body to show, or {@code null}
     */
    public void setSource(String methodName, String source) {
        if (source == null || source.isBlank()) {
            methodLabel.setText("");
            codeArea.setText("");
            close();
            return;
        }
        methodLabel.setText(methodName + "()");
        codeArea.setText(SourceDisplay.methodBody(source));
        fitToContent();
        close();
    }

    /**
     * Shows arbitrary generated Java source (e.g. the code emitted by a visual
     * builder) in the drawer, labelled with the given factory method name.
     *
     * @param methodName the method name shown on the drawer's handle
     * @param code the code to display, or {@code null} to clear
     */
    public void setCode(String methodName, String code) {
        if (code == null || code.isBlank()) {
            methodLabel.setText("");
            codeArea.setText("");
            close();
            return;
        }
        methodLabel.setText(methodName + "()");
        codeArea.setText(code);
        fitToContent();
        close();
    }

    /**
     * Shows arbitrary generated Java source in the drawer, labelled
     * {@code build()}.
     *
     * @param code the code to display, or {@code null} to clear
     */
    public void setCode(String code) {
        setCode("build", code);
    }

    /**
     * Sizes the drawer to the code it will show and parks the unit so the
     * handle is the only visible part at the bottom edge.
     */
    private void fitToContent() {
        codeArea.setPrefHeight(visibleRows() * codeLineHeight());
        codeArea.setMinHeight(visibleRows() * codeLineHeight());
        refit();
    }

    /**
     * Measures the panel {@code prefHeight} and re-pins the drawer to it. Fonts
     * and control skins only resolve once the drawer is inside a live scene, so
     * the size settled here is authoritative; {@link #layoutChildren} re-runs
     * this once the drawer first lays out inside its parent.
     */
    private void refit() {
        panel.applyCss();
        double panelHeight = panel.prefHeight(Region.USE_COMPUTED_SIZE);
        if (Double.isNaN(panelHeight) || panelHeight < 20) {
            panelHeight = 120;
        }
        double height = panelHeight + handleRow.prefHeight(-1);
        if (Double.isNaN(height) || height < 30) {
            return;
        }
        if (Math.abs(closedSlide - panelHeight) > 0.5 || Math.abs(getPrefHeight() - height) > 0.5) {
            closedSlide = panelHeight;
            setPrefHeight(height);
            setMinHeight(height);
            setMaxHeight(height);
            slide.stop();
            unit.setTranslateY(open ? 0 : closedSlide);
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!codeArea.getText().isEmpty()) {
            refit();
        }
    }

    /**
     * Slides the drawer up so the handle rides to the top of the revealed code
     * panel and the chevron flips to point down, signalling the next click
     * moves the code away.
     */
    public void open() {
        slide.stop();
        slide.setToY(0);
        slide.setOnFinished(null);
        slide.play();
        open = true;
        panel.setMouseTransparent(false);
        chevron.setRotate(0);
        nudge.setTooltip(new Tooltip("Move code away"));
        attachSceneHandlers();
    }

    /**
     * Slides the drawer back down so only the handle peeks out; the chevron
     * points up, signalling the next click opens the drawer into the card.
     */
    public void close() {
        slide.stop();
        slide.setToY(closedSlide);
        slide.play();
        open = false;
        panel.setMouseTransparent(true);
        chevron.setRotate(180);
        nudge.setTooltip(new Tooltip("Show code"));
        detachSceneHandlers();
    }

    /**
     * While open, a click anywhere outside the drawer (or an ESC press) slides
     * it shut again.
     */
    private void attachSceneHandlers() {
        var scene = getScene();
        if (scene != null) {
            scene.addEventFilter(MouseEvent.MOUSE_CLICKED, outsideClickClose);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, escapeClose);
        }
    }

    private void detachSceneHandlers() {
        var scene = getScene();
        if (scene != null) {
            scene.removeEventFilter(MouseEvent.MOUSE_CLICKED, outsideClickClose);
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, escapeClose);
        }
    }

    private boolean containsEventTarget(Object target) {
        for (var node = target instanceof Node n ? n : null; node != null; node = node.getParent()) {
            if (node.equals(this)) {
                return true;
            }
        }
        return false;
    }

    /** Slides the drawer open if collapsed, shut if open. */
    public void toggle() {
        if (open) {
            close();
        } else {
            open();
        }
    }

    /**
     * Whether the drawer is currently open.
     *
     * @return {@code true} when the code panel is revealed
     */
    public boolean isOpen() {
        return open;
    }

    /** The offset that hides the panel when collapsed, exposed for tests. */
    double closedSlide() {
        return closedSlide;
    }

    /** The unit's current slide offset, exposed for tests. */
    double slideOffset() {
        return unit.getTranslateY();
    }

    /**
     * The minimum rows guaranteed to fit the code: the line count, clamped so
     * short bodies stay comfortably readable and long bodies scroll.
     *
     * @param text the code text
     * @return the number of rows to show
     */
    private static int rowsFor(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return Math.max(4, Math.min(lines, MAX_VISIBLE_ROWS));
    }

    /**
     * The number of code rows the drawer currently fits (the {@link CodeArea}'s
     * height), exposed for tests.
     *
     * @return the visible row count
     */
    int visibleRows() {
        return rowsFor(codeArea.getText());
    }

    /** Switches the drawer to the shared {@link SourceTheme} and re-styles the code. */
    private void applyTheme() {
        var theme = SourceTheme.CURRENT.get();
        getStyleClass().remove("source-drawer-theme-light");
        getStyleClass().remove("source-drawer-theme-dark");
        getStyleClass().add(theme.isDark()
                ? "source-drawer-theme-dark"
                : "source-drawer-theme-light");
        codeArea.setSyntaxDecorator(new JavaSyntaxDecorator());
    }

    private static double codeLineHeight() {
        double height = codeLineHeight;
        if (height == 0) {
            height = measureLineHeight();
            codeLineHeight = height;
        }
        return height;
    }

    /** Measures one rendered code line for the Menlo font used by the code area. */
    private static double measureLineHeight() {
        var probe = new Text("Ag");
        probe.setFont(Font.font("Menlo", 12));
        double height = probe.getLayoutBounds().getHeight();
        return height > 0 ? height + 4 : 16.0;
    }

    /** Copy glyph: two overlapping rounded rectangles. */
    private static SVGPath copyIcon() {
        return svg("M9 4h9a2 2 0 0 1 2 2v9M5 4H4a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-1");
    }

    /** Nudge affordance: a small chevron. */
    private static SVGPath chevronIcon() {
        return svg("M6 9l6 6 6-6");
    }

    private static SVGPath svg(String content) {
        var path = new SVGPath();
        path.setContent(content);
        path.getStyleClass().add("source-drawer-icon");
        return path;
    }

    /** The source code area, exposed for tests. */
    CodeArea sourceArea() {
        return codeArea;
    }

    /** The nudge handle, exposed for tests. */
    Button nudge() {
        return nudge;
    }
}
