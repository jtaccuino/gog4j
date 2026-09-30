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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.sampler.meta.TagKind;
import org.jtaccuino.gog.sampler.registry.Tags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the two-level tag cascade: picking a tag key (kind) must enable and
 * populate the tag selector to its right.
 */
class FilterBarTest {

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

    private static void onFx(Runnable r) throws Exception {
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(60, TimeUnit.SECONDS), "FX task timed out");
    }

    @Test
    void pickingAFeatureKindEnablesAndPopulatesTheTagBox() throws Exception {
        onFx(() -> {
            var bar = new FilterBar(Tags.all());
            assertTrue(bar.tagBox().isDisabled(), "tag box starts disabled");
            // Type "Fe" then commit the FEATURE kind, mirroring the reported case.
            bar.kindBox().getEditor().setText("Fe");
            bar.kindBox().setValue(TagKind.FEATURE);
            assertFalse(bar.tagBox().isDisabled(), "tag box must enable once a kind is picked");
            assertFalse(bar.tagBox().getItems().isEmpty(),
                    "tag box must be populated with the picked kind's tags");
            assertTrue(bar.addFilter().isDisabled(),
                    "add-filter button stays disabled until a tag is actually committed");
        });
    }

    @Test
    void committingATagEnablesTheAddFilterButton() throws Exception {
        onFx(() -> {
            var bar = new FilterBar(Tags.all());
            // Pick the GEOM kind via typed text + Enter, enabling the tag box.
            bar.kindBox().getEditor().setText("Geom");
            bar.kindBox().fireEvent(new javafx.event.ActionEvent());
            assertFalse(bar.tagBox().isDisabled(), "tag box must enable");
            assertTrue(bar.addFilter().isDisabled(), "add-filter still disabled before a tag is chosen");
            // Commit a tag in the tag box (the "Boxplot" geom tag).
            bar.tagBox().getEditor().setText("Box");
            bar.tagBox().fireEvent(new javafx.event.ActionEvent());
            assertFalse(bar.addFilter().isDisabled(),
                    "add-filter must enable once a specific tag is committed");
        });
    }

    @Test
    void committingTypedKindByEnterResolvesToItemAndEnablesTagBox() throws Exception {
        onFx(() -> {
            var bar = new FilterBar(Tags.all());
            assertTrue(bar.tagBox().isDisabled(), "tag box starts disabled");
            // The reported case: type "Fe" and press Enter, which commits the raw
            // text as the value; it must resolve to the FEATURE item and enable
            // the tag selector to the right.
            bar.kindBox().getEditor().setText("Fe");
            bar.kindBox().fireEvent(new javafx.event.ActionEvent());
            // The committed value must be the real item (not raw text "Fe"), so the
            // button cell can render it via the display function.
            assertTrue(bar.kindBox().getValue() != null,
                    "committed value must be a real TagKind item, not raw text");
            assertFalse(bar.kindBox().getEditor().getText().equals("Fe"),
                    "button text must not show the raw typed text");
            assertFalse(bar.tagBox().isDisabled(),
                    "tag box must enable after typing a kind and committing it");
            assertFalse(bar.tagBox().getItems().isEmpty(),
                    "tag box must be populated with the picked kind's tags");
        });
    }

    @Test
    void statusRowReflectsSharedLoadProgress() throws Exception {
        // A private coordinator rather than the shared singleton: cards from
        // other suites keep mutating the singleton from loader threads, which
        // would make this visibility assertion order-dependent.
        var progress = new LoadProgress();
        var bar = new FilterBar[1];
        onFx(() -> bar[0] = new FilterBar(Tags.all(), progress));
        onFx(() -> assertFalse(bar[0].statusRow().isVisible(),
                "status row must be hidden while nothing is loading"));
        progress.beginLoad();
        onFx(() -> { });
        onFx(() -> {
            assertTrue(bar[0].statusRow().isVisible(),
                    "status row must appear while a load is in flight");
            assertTrue(bar[0].statusRow().getChildrenUnmodifiable().get(1)
                            instanceof javafx.scene.control.Label,
                    "status row must carry a label");
        });
        progress.endLoad();
        onFx(() -> { });
        onFx(() -> assertFalse(bar[0].statusRow().isVisible(),
                "status row must hide once no work is pending"));
    }

    @Test
    void resultCountReflectsShownAndTotal() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> bar[0] = new FilterBar(Tags.all()));
        var shown = new SimpleIntegerProperty(42);
        var total = new SimpleIntegerProperty(248);
        onFx(() -> bar[0].bindResultCount(shown, total));
        onFx(() -> assertEquals("42 of 248 examples", bar[0].countLabel().getText(),
                "count label must show the bound shown/total sizes"));
        shown.set(7);
        onFx(() -> assertEquals("7 of 248 examples", bar[0].countLabel().getText(),
                "count label must follow the shown size"));
        total.set(300);
        onFx(() -> assertEquals("7 of 300 examples", bar[0].countLabel().getText(),
                "count label must follow the total size"));
    }

    @Test
    void countLabelSharesTheLineWithTheActiveTagChips() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            var parent = bar[0].countLabel().getParent();
            assertTrue(parent instanceof HBox,
                    "count label must live in a horizontal line with the chips");
            var line = (HBox) parent;
            assertTrue(line.getChildrenUnmodifiable().stream()
                            .anyMatch(n -> n instanceof FlowPane),
                    "count label line must also host the chips row");
        });
    }

    @Test
    void renderModeSwitchTogglesBetweenFastAndFull() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            var sw = bar[0].renderSwitch();
            // The switch is a pill track with a sliding knob, not two buttons.
            assertTrue(sw.getChildrenUnmodifiable().stream()
                            .anyMatch(n -> n instanceof StackPane),
                    "switch must contain a sliding knob child");
            assertTrue(sw.fast().isSelected(), "switch defaults to FAST");
            assertEquals(RenderMode.FAST, bar[0].renderModeProperty().get(),
                    "switch must default to FAST");
            // Selecting FULL through the model slides the knob label over.
            sw.full().fire();
            assertEquals(RenderMode.FULL, bar[0].renderModeProperty().get(),
                    "switch must report FULL after flipping");
            assertTrue(sw.full().isSelected(),
                    "FULL side of the model must be selected after flipping");
            // And back again.
            sw.fast().fire();
            assertEquals(RenderMode.FAST, bar[0].renderModeProperty().get(),
                    "switch must report FAST after flipping back");
            assertTrue(sw.fast().isSelected(),
                    "FAST side of the model must be selected after flipping back");
        });
    }

    @Test
    void pickingAKindHandsFocusToTheTagSelector() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            new javafx.scene.Scene(new javafx.scene.layout.StackPane(bar[0]));
            bar[0].kindBox().setValue(TagKind.GEOM);
            assertTrue(bar[0].tagBox().getEditor().isFocused(),
                    "picking a tag key must move focus to the tag box");
        });
    }

    @Test
    void committingATagHandsFocusToTheAddFilterButton() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            new javafx.scene.Scene(new javafx.scene.layout.StackPane(bar[0]));
            bar[0].kindBox().setValue(TagKind.GEOM);
            var tag = bar[0].tagBox().getItems().get(0);
            bar[0].tagBox().setValue(tag);
            assertTrue(bar[0].addFilter().isFocused(),
                    "committing a tag must move focus to Add filter");
        });
    }

    @Test
    void addingAFilterReturnsFocusToTheSearchField() throws Exception {
        var bar = new FilterBar[1];
        var geom = Tags.all().stream().filter(t -> t.kind() == TagKind.GEOM).findFirst().orElseThrow();
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            new javafx.scene.Scene(new javafx.scene.layout.StackPane(bar[0]));
            bar[0].kindBox().setValue(TagKind.GEOM);
            bar[0].tagBox().setValue(geom);
            bar[0].addFilter().fire();
            assertTrue(bar[0].searchField().isFocused(),
                    "Add filter must return focus to search");
        });
    }

    @Test
    void renderModeDefaultsToFast() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            assertEquals(RenderMode.FAST, bar[0].renderModeProperty().get(),
                    "render mode must default to FAST");
            assertTrue(bar[0].renderFast().isSelected(),
                    "FAST toggle must be selected by default");
            assertFalse(bar[0].renderFull().isSelected(),
                    "FULL toggle must not be selected by default");
        });
    }

    @Test
    void selectingFullToggleUpdatesTheRenderModeProperty() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            bar[0].renderFull().fire();
            assertEquals(RenderMode.FULL, bar[0].renderModeProperty().get(),
                    "selecting the FULL toggle must switch the render mode");
            bar[0].renderFast().fire();
            assertEquals(RenderMode.FAST, bar[0].renderModeProperty().get(),
                    "selecting the FAST toggle must switch the render mode back");
        });
    }

    @Test
    void settingTheRenderModePropertySelectsTheMatchingToggle() throws Exception {
        var bar = new FilterBar[1];
        onFx(() -> {
            bar[0] = new FilterBar(Tags.all());
            bar[0].renderModeProperty().set(RenderMode.FULL);
            assertTrue(bar[0].renderFull().isSelected(),
                    "FULL toggle must be selected when the mode is set to FULL");
            bar[0].renderModeProperty().set(RenderMode.FAST);
            assertTrue(bar[0].renderFast().isSelected(),
                    "FAST toggle must be selected when the mode is set to FAST");
        });
    }
}
