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
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * An editable, searchable {@link ComboBox}. Typing in the editor filters the
 * dropdown to entries whose display text contains the typed query; choosing an
 * entry from the list commits it. A committed {@code null} value means "All".
 * <p>
 * Because the box is editable, the {@code ComboBox} value property can transiently
 * hold raw text typed by the user; only real {@code T} items (selected from the
 * list) are ever committed to {@link #selectedValueProperty()}.
 *
 * @param <T> the item type shown in the box
 */
public class SearchableComboBox<T> extends ComboBox<T> {

    private final ObservableList<T> masterItems;
    private final ObservableList<T> filteredItems;
    private final Class<T> itemType;
    private final Function<T, String> display;
    private final ObjectProperty<T> selected;
    // Guards against the editable ComboBox reacting to a setAll() by resetting
    // the editor text, which would re-enter filter() and loop.
    private boolean filtering;
    // Set while the editor text is being changed programmatically (committing a
    // value, pushing the display label back after a skin reset). A commit must
    // not re-open the dropdown: the click selection is meant to close it, and
    // re-showing on our own text write is what made popups stay open.
    private boolean programmaticTextChange;

    /**
     * Creates a searchable combo over the given items.
     *
     * @param itemType the runtime class of the item type, used to distinguish
     *     committed items from the raw editor text
     * @param prompt the prompt text shown when nothing is selected
     * @param items the full item set (including the "All" entry, if any)
     * @param display converts an item to its display text
     */
    @SuppressWarnings("this-escape")
    public SearchableComboBox(Class<T> itemType, String prompt, List<T> items, Function<T, String> display) {
        super();
        this.masterItems = FXCollections.observableArrayList(items);
        this.filteredItems = FXCollections.observableArrayList(items);
        setItems(filteredItems);
        this.display = display;
        this.itemType = itemType;
        this.selected = new SimpleObjectProperty<>();
        setEditable(true);
        setPromptText(prompt);
        setValue(null);
        setButtonCell(new TextCell<>(display));
        setCellFactory(view -> new TextCell<>(display));
        getEditor().textProperty().addListener((obs, oldVal, newVal) -> filter(newVal));
        valueProperty().addListener((obs, oldVal, newVal) -> {
            // Editable boxes surface the raw editor text as the value; ignore
            // anything that is not a committed T item so it never leaks into
            // the selection. A null value resets the committed selection.
            if (itemType.isInstance(newVal)) {
                var item = itemType.cast(newVal);
                setEditorText(display.apply(item));
                selected.set(item);
                pushDisplayText();
            } else {
                selected.set(null);
            }
        });
        // Pressing Enter commits either the row highlighted in the dropdown (the
        // keyboard navigation path) or, failing that, resolves the typed text to
        // a real item when it uniquely identifies one. The ComboBox's own value
        // may hold the raw typed text, so this favours an explicit item.
        setOnAction(e -> {
            var highlighted = getSelectionModel().getSelectedItem();
            var picked = highlighted != null && itemType.isInstance(highlighted)
                    ? itemType.cast(highlighted)
                    : resolveText(getEditor().getText());
            if (picked != null) {
                setValue(picked);
            }
        });
    }

    /**
     * Creates a searchable combo whose item type is inferred from the items.
     *
     * @param prompt the prompt text shown when nothing is selected
     * @param items the full item set (including the "All" entry, if any)
     * @param display converts an item to its display text
     */
    @SuppressWarnings({"unchecked", "this-escape"})
    public SearchableComboBox(String prompt, List<T> items, Function<T, String> display) {
        this(inferType(items), prompt, items, display);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> inferType(List<T> items) {
        for (var item : items) {
            if (item != null) {
                return (Class<T>) item.getClass();
            }
        }
        return (Class<T>) Object.class;
    }

    /**
     * The committed selection, or {@code null} for "All".
     *
     * @return the selection property
     */
    public ObjectProperty<T> selectedValueProperty() {
        return selected;
    }

    private void filter(String query) {
        if (filtering) {
            return;
        }
        filtering = true;
        try {
            var needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
            var next = needle.isEmpty()
                    ? masterItems
                    : masterItems.stream()
                            .filter(item -> item != null
                                    && display.apply(item).toLowerCase(java.util.Locale.ROOT).contains(needle))
                            .toList();
            applyItems(next);
            // Only surface the dropdown while the user is actually typing, not
            // on programmatic text changes such as resetting the box or writing
            // back the display label after a commit.
            if (getEditor().isFocused() && !programmaticTextChange
                    && !filteredItems.isEmpty() && !isShowing()) {
                show();
            }
        } finally {
            filtering = false;
        }
    }

    /**
     * Maps typed text back to a catalogue item, or {@code null} if it does not
     * uniquely identify one. A case-insensitive exact match on the display text
     * wins; otherwise a single matching prefix is accepted so partial input like
     * {@code "Fe"} resolves to {@code FEATURE}.
     */
    private T resolveText(Object raw) {
        if (!(raw instanceof String text) || text.isBlank()) {
            return null;
        }
        var needle = text.trim().toLowerCase(java.util.Locale.ROOT);
        T prefixed = null;
        for (var item : masterItems) {
            if (item == null) {
                continue;
            }
            var label = display.apply(item).toLowerCase(java.util.Locale.ROOT);
            if (label.equals(needle)) {
                return item;
            }
            if (label.startsWith(needle)) {
                if (prefixed != null) {
                    prefixed = null; // ambiguous prefix
                } else {
                    prefixed = item;
                }
            }
        }
        return prefixed;
    }

    /**
     * Re-applies the display text to the editor on the next pulse. The stock
     * ComboBox skin rewrites an editable combo's editor to
     * {@code getValue().toString()} whenever the value changes, so without this
     * the button shows the raw identifier (e.g. {@code feature:facets}) instead
     * of the display name (e.g. {@code Facets}). This only touches the editor
     * text, never the items list, so it cannot trigger the dropdown OOB.
     */
    private void pushDisplayText() {
        javafx.application.Platform.runLater(() -> {
            if (getValue() != null) {
                setEditorText(display.apply(itemType.cast(getValue())));
            }
        });
    }

    /**
     * Sets the editor text without triggering an automatic dropdown open, used
     * for programmatic writes (committing a value, restoring display labels).
     *
     * @param text the display text
     */
    private void setEditorText(String text) {
        programmaticTextChange = true;
        try {
            getEditor().setText(text);
        } finally {
            programmaticTextChange = false;
        }
    }

    /**
     * Replaces the master item set (e.g. to narrow them to a selected tag key)
     * and re-filters to the current editor text.
     *
     * @param items the new full item set
     */
    public void setItems(List<T> items) {
        masterItems.setAll(items);
        filter(getEditor().getText());
    }

    /**
     * Applies a new item set to the dropdown. Items are never mutated while the
     * popup is open: {@code clearAndSelect} in the stock popup selection model
     * throws {@code IndexOutOfBoundsException} when the list changes underneath
     * a click, so the popup is closed first and reopened only if the editor is
     * still active. A no-match filter never empties the list either.
     */
    private void applyItems(List<T> next) {
        // Never empty the items: a (re)opened dropdown starts a selection at
        // row 0, which the stock model throws on for an empty list.
        var safe = next.isEmpty()
                ? (filteredItems.isEmpty() ? List.<T>of() : List.copyOf(filteredItems))
                : next;
        boolean wasShowing = isShowing();
        if (wasShowing) {
            hide();
        }
        filteredItems.setAll(safe);
        if (wasShowing && getEditor().isFocused() && !programmaticTextChange
                && !filteredItems.isEmpty()) {
            show();
        }
    }
}
