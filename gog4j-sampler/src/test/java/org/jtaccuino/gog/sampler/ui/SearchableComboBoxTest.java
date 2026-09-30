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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.jtaccuino.gog.sampler.meta.TagKind;
import org.jtaccuino.gog.sampler.registry.Tag;
import org.jtaccuino.gog.sampler.registry.Tags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the editable search combo against the re-entrancy StackOverflowError:
 * setting the items on an editable {@code ComboBox} can reset the editor text,
 * which re-triggers the filter and can loop forever. Typing must terminate.
 */
class SearchableComboBoxTest {

    @BeforeAll
    static void startToolkit() {
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assumeTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        } catch (IllegalStateException alreadyRunning) {
            // already up
        } catch (UnsupportedOperationException | InterruptedException noToolkit) {
            assumeTrue(false, "No JavaFX toolkit available: " + noToolkit.getMessage());
        }
    }

    private <T> T onFxThread(java.util.concurrent.Callable<T> work) throws Exception {
        var task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(60, TimeUnit.SECONDS);
    }

    @Test
    void typingAgainstEditableComboDoesNotStackOverflow() throws Exception {
        var geomTags = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).toList();

        onFxThread(() -> {
            var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", geomTags,
                    t -> t.name() + ": " + t.label());
            // Type a substring; setting the editor text must terminate (the
            // guard breaks the setAll -> text -> filter -> setAll loop).
            box.getEditor().setText("po");
            return null;
        });
    }

    @Test
    void replacingItemsReFiltersWithoutInfiniteLoop() throws Exception {
        var allTags = Tags.all();

        onFxThread(() -> {
            var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", List.of(),
                    t -> t.name() + ": " + t.label());
            box.getEditor().setText("den");
            box.setItems(allTags.stream().filter(t -> t.kind().equals(TagKind.FEATURE)).toList());
            return null;
        });
    }

    @Test
    void committedValueRendersItsDisplayLabelNotTheRawName() throws Exception {
        var geomTags = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).toList();
        var boxRef = onFxThread(() -> {
            var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", geomTags, Tag::label);
            var target = geomTags.stream().filter(t -> t.label().equals("Boxplot")).findFirst().orElseThrow();
            box.setValue(target);
            return new Object[] {box, target.label(), box.getValue().toString()};
        });
        var label = (String) boxRef[1];
        var rawName = (String) boxRef[2];
        var box = (SearchableComboBox<?>) boxRef[0];
        assertTrue(label.equals(box.getEditor().getText()),
                "editor must show the display label, got '" + box.getEditor().getText() + "'");
        assertFalse(label.equals(rawName),
                "editor showing the raw name would indicate the display broke");
    }

    @Test
    void kindComboSurfacesAllKinds() throws Exception {
        var kinds = onFxThread(() -> {
            var box = new SearchableComboBox<TagKind>(TagKind.class, "Tag key\u2026",
                    List.of(TagKind.values()), TagKind::name);
            return box.getItems();
        });
        assertTrue(kinds.size() >= 6, "expected at least the six tag kinds, got " + kinds);
    }

    @Test
    void commitThenResetClearsSelectionAndKeepsItems() throws Exception {
        var geomTags = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).toList();
        var boxRef = onFxThread(() -> {
            var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", geomTags,
                    t -> t.name() + ": " + t.label());
            // Simulate FilterBar: pick a tag, then reset the box the way
            // "Add filter" does. This must not throw the [0..1, size:0] OOB and
            // must leave the committed selection null and the items populated.
            box.setValue(geomTags.get(0));
            box.setValue(null);
            box.getEditor().clear();
            box.setItems(geomTags);
            return new Object[] {box, box.selectedValueProperty().get()};
        });
        var value = (Tag) boxRef[1];
        var box = (SearchableComboBox<?>) boxRef[0];
        assertTrue(value == null, "committed selection must reset to null");
        assertTrue(!box.getItems().isEmpty(), "items must remain populated after reset");
    }

    @Test
    void noMatchFilterNeverEmptiesTheItemsList() throws Exception {
        var geomTags = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).toList();
        var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", geomTags,
                t -> t.name() + ": " + t.label());
        // Apply a no-match filter on the FX thread.
        onFxThread(() -> {
            box.getEditor().setText("__no_such_tag__");
            return null;
        });
        // Drain the FX queue so the deferred popup update lands.
        drainFx();
        // The item list must never become empty, or a dropdown click throws
        // the [0..1, size:0] IndexOutOfBoundsException in the selection model.
        var stillPopulated = onFxThread(() -> !box.getItems().isEmpty());
        assertTrue(stillPopulated, "items must never empty, even on a no-match filter");
    }

    @Test
    void clickSelectionClosesThePopupAndKeepsItClosed() throws Exception {
        var geomTags = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).toList();

        var boxRef = new SearchableComboBox<?>[1];
        var opened = onFxThread(() -> {
            var box = new SearchableComboBox<Tag>(Tag.class, "Tag\u2026", geomTags, Tag::label);
            boxRef[0] = box;
            // The box must sit in a scene the editor can take focus from.
            new javafx.scene.Scene(new javafx.scene.layout.StackPane(box));
            box.getEditor().setText("Box");
            box.requestFocus();
            box.show();
            return box.isShowing();
        });
        assumeTrue(opened, "No JavaFX popup support in this environment");
        onFxThread(() -> {
            // The click path: the list cell selects the item and fires the
            // action; our onAction commits it, and the commit's editor rewrite
            // plus the deferred pushDisplayText must not re-open the dropdown.
            boxRef[0].getSelectionModel().select(0);
            boxRef[0].fireEvent(new javafx.event.ActionEvent());
            return null;
        });
        drainFx();
        assertFalse((boolean) onFxThread(() -> boxRef[0].isShowing()),
                "clicking a selection must close the dropdown and keep it closed");
    }

    /** Waits until everything currently queued on the FX thread has run. */
    private void drainFx() throws Exception {
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> Platform.runLater(latch::countDown));
        assumeTrue(latch.await(60, TimeUnit.SECONDS), "FX tasks did not drain");
    }
}
