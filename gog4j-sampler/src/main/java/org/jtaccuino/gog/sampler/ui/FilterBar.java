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

import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.SourceTheme;
import org.jtaccuino.gog.sampler.meta.TagKind;
import org.jtaccuino.gog.sampler.registry.Tag;

/**
 * Filter bar with a full-text search field and a two-level tag filter: first
 * pick a tag key (kind), then type to narrow that kind's tags by substring.
 * Pressing the {@code Add filter} button adds the chosen tag as an active,
 * removable filter chip; picking a kind or tag never changes the active filters
 * until the button is pressed.
 */
public class FilterBar extends VBox {

    private final TextField searchField = new TextField();
    private final SearchableComboBox<TagKind> kindBox;
    private final SearchableComboBox<Tag> tagBox;
    private final FlowPane chipsRow = new FlowPane(6, 4);
    private final Button addFilter = new Button("Add filter");
    private final Button clearAll = new Button("Clear all");
    private final ListProperty<Tag> activeTags = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final List<Tag> allTags;
    private final ProgressIndicator statusSpinner = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final HBox statusRow = new HBox(8, statusSpinner, statusLabel);
    private final Label countLabel = new Label();
    private final RenderModeSwitch renderSwitch = new RenderModeSwitch();
    private final ThemeSwitch themeSwitch = new ThemeSwitch();

    private static Label selectorLabel(String text) {
        var label = new Label(text);
        label.getStyleClass().add("sampler-filter-label");
        return label;
    }

    /**
     * Creates the filter bar over the given tags, used as the suggestion source,
     * bound to the shared {@link LoadProgress} singleton.
     *
     * @param allTags every tag in the catalogue
     */
    public FilterBar(List<Tag> allTags) {
        this(allTags, LoadProgress.get());
    }

    /**
     * Creates the filter bar over the given tags, reporting load status from the
     * given coordinator. The application passes the shared singleton; a test can
     * pass a private instance so its assertions are not perturbed by cards that
     * other suites keep loading on background threads.
     *
     * @param allTags  every tag in the catalogue
     * @param progress the load-progress source the status row follows
     */
    @SuppressWarnings("this-escape")
    FilterBar(List<Tag> allTags, LoadProgress progress) {
        this.allTags = allTags;
        getStylesheets().add(getClass().getResource("/org/jtaccuino/gog/controls/sampler.css").toExternalForm());
        getStyleClass().add("sampler-filter-bar");
        setSpacing(8);
        setPadding(new Insets(10, 10, 10, 10));

        searchField.setPromptText("Search examples\u2026");
        searchField.setPrefWidth(220);

        // Level 1: pick a tag key (kind), searchable on its display name. Picking
        // a kind hands the focus to the tag box, keeping a keyboard-driven filter
        // chain (search -> kind -> tag -> add) moving forward.
        kindBox = new SearchableComboBox<>(TagKind.class, "Coordinate, Feature, \u2026",
                List.of(TagKind.values()), TagKind::label);
        kindBox.setPrefWidth(160);

        // Level 2: narrow the chosen kind's tags by substring.
        tagBox = new SearchableComboBox<>(Tag.class, "Pick a tag key first\u2026",
                List.of(), Tag::label);
        tagBox.setPrefWidth(220);
        tagBox.setDisable(true);

        kindBox.selectedValueProperty().addListener((obs, old, kind) -> {
            onKindChanged(kind);
            if (kind != null) {
                tagBox.getEditor().requestFocus();
            }
        });

        addFilter.setDisable(true);
        addFilter.setStyle("-fx-cursor: hand;");
        // The button is only meaningful once a specific tag has been committed
        // in the tag box; it stays disabled while the box merely has candidates.
        tagBox.selectedValueProperty().addListener((obs, oldTag, tag) -> {
            addFilter.setDisable(tag == null);
            // Committing a tag hands the focus to the "Add filter" button so
            // the filter-building chain (search -> kind -> tag -> add) still
            // advances when working purely with the keyboard.
            if (tag != null) {
                addFilter.requestFocus();
            }
        });
        addFilter.setOnAction(e -> {
            var tag = tagBox.selectedValueProperty().get();
            if (tag != null) {
                if (!activeTags.contains(tag)) {
                    activeTags.add(tag);
                }
                resetSelectors();
                searchField.requestFocus();
            }
        });

        activeTags.addListener((ListChangeListener<Tag>) change -> rebuildChips());

        clearAll.getStyleClass().add("sampler-clear-all");
        clearAll.setOnAction(e -> activeTags.clear());

        var searchLabelled = labelled("Search", searchField);
        var kindLabelled = labelled("Tag key", kindBox);
        var tagLabelled = labelled("Tag", tagBox);
        var topRow = new HBox(10, searchLabelled, kindLabelled, tagLabelled, addFilter);
        topRow.setAlignment(Pos.BASELINE_LEFT);
        var top = new HBox(24, topRow, renderSwitch, themeSwitch);
        top.setAlignment(Pos.CENTER_LEFT);
        // The switches sit flush right so the row ends on a stable control and
        // the render-mode choice is not wedged between small labelled fields.
        HBox.setHgrow(topRow, Priority.ALWAYS);
        topRow.setMaxWidth(Double.MAX_VALUE);
        // The running count lives on the same line as the active filter chips,
        // flush right: it answers "how many examples match what I've filtered
        // for" right where the filters themselves are rendered.
        countLabel.getStyleClass().add("sampler-count-label");
        var chipsLine = new HBox(10, chipsRow, countLabel);
        chipsLine.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(chipsRow, Priority.ALWAYS);
        chipsRow.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(top, chipsLine);

        // Single, coalesced loading status for the whole sampler: cards no
        // longer animate a spinner each, so a filter burst shows one
        // indeterminate indicator plus a pending count here instead of
        // N pulsing spinners competing for FX pulses.
        var pending = progress.pendingProperty();
        statusSpinner.setPrefSize(14, 14);
        statusLabel.getStyleClass().add("sampler-status-label");
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.visibleProperty().bind(pending.greaterThan(0));
        statusRow.managedProperty().bind(statusRow.visibleProperty());
        statusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> pending.get() == 1
                        ? "Loading 1 example\u2026"
                        : "Loading " + pending.get() + " examples\u2026",
                pending));
        getChildren().add(statusRow);
    }

    private static HBox labelled(String caption, Node field) {
        var box = new HBox(4, selectorLabel(caption), field);
        box.setAlignment(Pos.BASELINE_LEFT);
        return box;
    }

    /**
     * The currently selected render mode: {@link RenderMode#FAST} by default,
     * or {@link RenderMode#FULL} when the sliding switch is flipped.
     *
     * @return the render mode
     */
    public ObjectProperty<RenderMode> renderModeProperty() {
        return renderSwitch.renderModeProperty();
    }

    /** The sliding switch itself, exposed so tests can see its knob. */
    RenderModeSwitch renderSwitch() {
        return renderSwitch;
    }

    /**
     * The light/dark theme selected by the theme switch, kept in sync with the
     * shared {@link SourceTheme#CURRENT} that drives every source drawer.
     *
     * @return the theme property
     */
    public ObjectProperty<SourceTheme> themeProperty() {
        return themeSwitch.themeProperty();
    }

    /** The theme switch itself, exposed for tests. */
    ThemeSwitch themeSwitch() {
        return themeSwitch;
    }

    private void onKindChanged(TagKind kind) {
        resetTagBox(kind == null ? List.of() : allTags.stream()
                .filter(tag -> tag.kind() == kind).toList());
    }

    private void resetSelectors() {
        kindBox.setValue(null);
        kindBox.getEditor().clear();
        resetTagBox(List.of());
    }

    private void resetTagBox(List<Tag> items) {
        tagBox.setValue(null);
        tagBox.getEditor().clear();
        tagBox.setItems(items);
        tagBox.setDisable(items.isEmpty());
    }

    private void rebuildChips() {
        chipsRow.getChildren().clear();
        for (var tag : activeTags) {
            chipsRow.getChildren().add(new ActiveFilterChip(tag, () -> activeTags.remove(tag)));
        }
        if (!activeTags.isEmpty()) {
            chipsRow.getChildren().add(clearAll);
        }
    }

    /**
     * The full-text search query; an empty string means no text filter.
     *
     * @return the search query property
     */
    public StringProperty queryProperty() {
        return searchField.textProperty();
    }

    /**
     * The tags currently active as filters. An example matches only if it
     * carries every active tag.
     *
     * @return the active tags property
     */
    public ListProperty<Tag> activeTagsProperty() {
        return activeTags;
    }

    /**
     * Binds the "N of M examples" summary to the shown and total sizes.
     *
     * @param shown the number of examples matching the current filters
     * @param total the total number of examples in the catalogue
     */
    public void bindResultCount(ObservableValue<Number> shown, ObservableValue<Number> total) {
        countLabel.textProperty().bind(Bindings.createStringBinding(
                () -> shown.getValue() + " of " + total.getValue() + " examples",
                shown, total));
    }

    /** The full-text search field, exposed for tests. */
    TextField searchField() {
        return searchField;
    }

    /** The tag-key (kind) selector, exposed for tests. */
    SearchableComboBox<TagKind> kindBox() {
        return kindBox;
    }

    /** The tag selector, exposed for tests. */
    SearchableComboBox<Tag> tagBox() {
        return tagBox;
    }

    /** The button that commits the chosen tag as a filter, exposed for tests. */
    Button addFilter() {
        return addFilter;
    }

    /** The coalesced loading-status row, exposed for tests. */
    HBox statusRow() {
        return statusRow;
    }

    /** The "N of M examples" summary label, exposed for tests. */
    public Label countLabel() {
        return countLabel;
    }

    /** The FAST side of the render switch, exposed for tests. */
    public ToggleButton renderFast() {
        return renderSwitch.fast();
    }

    /** The FULL side of the render switch, exposed for tests. */
    public ToggleButton renderFull() {
        return renderSwitch.full();
    }

    /** The LIGHT side of the theme switch, exposed for tests. */
    public ToggleButton themeLight() {
        return themeSwitch.light();
    }

    /** The DARK side of the theme switch, exposed for tests. */
    public ToggleButton themeDark() {
        return themeSwitch.dark();
    }
}
