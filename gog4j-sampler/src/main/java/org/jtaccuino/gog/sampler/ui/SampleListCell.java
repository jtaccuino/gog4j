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

import java.util.function.Consumer;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.PlotCard;
import org.jtaccuino.gog.interaction.Orbit3d;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;

/**
 * A virtualized list cell that reuses one {@link PlotCard} across recycled
 * rows. Only the cells for visible rows are created, so only a handful of plots
 * are ever built at once regardless of how many examples match the filters.
 * <p>
 * The sampler is a pure browser with no row selection, so primary-button press
 * events are consumed here before {@code CellBehaviorBase} can drive selection
 * against a changing items list, which would otherwise throw
 * {@code IndexOutOfBoundsException} when a click races a filter/virtualization.
 */
public class SampleListCell extends ListCell<SamplerExample> {

    private final PlotCard card = new PlotCard();
    // The example currently bound to the cell, used to resolve tag labels back
    // to the typed Tag when a chip is clicked and to colour chips by kind.
    private SamplerExample current;
    private Consumer<Tag> onTagSelected;

    // ─── Viewport-visibility gating ──────────────────────────────────────
    // The ListView materializes cells for a few rows beyond the viewport and
    // keeps a recycle pool, so "has a cell" is not "is on screen". Each cell
    // reports whether it is actually inside the viewport (in scene coordinates,
    // refreshed on layout, list resize and scroll-bar movement) so the card
    // loads its plot only while visible and releases it when scrolled away.
    private ListView<SamplerExample> attachedList;
    private ScrollBar verticalScrollBar;
    private final InvalidationListener boundsListener = obs -> updateVisibility();
    private final InvalidationListener scrollListener = obs -> updateVisibility();
    private final ChangeListener<Number> heightListener = (obs, o, n) -> updateVisibility();

    /**
     * Resolves a clicked chip label back to the typed {@link Tag} of the bound
     * example and forwards it to the {@code onTagSelected} callback.
     *
     * @param label the clicked chip label
     */
    private void resolveTag(String label) {
        if (onTagSelected != null && current != null) {
            current.tags().stream()
                    .filter(tag -> tag.label().equals(label))
                    .findFirst()
                    .ifPresent(onTagSelected);
        }
    }

    /**
     * Colours a chip by its tag kind (falling back to the card's auto-colour
     * when the label does not map to a known tag).
     *
     * @param label the chip label
     * @return a CSS colour
     */
    private String chipColor(String label) {
        if (current != null) {
            for (var tag : current.tags()) {
                if (tag.label().equals(label)) {
                    return TagColors.forKind(tag.kind());
                }
            }
        }
        return null;
    }

    /**
     * Creates a cell whose tag chips filter by a single tag through
     * {@code onTagSelected}; pass {@code null} to leave them inert. The cell
     * stays pinned to the {@code FAST} render mode.
     *
     * @param onTagSelected the handler receiving a clicked tag
     */
    public SampleListCell(Consumer<Tag> onTagSelected) {
        this(onTagSelected, null);
    }

    /**
     * Creates a cell that additionally tracks the shared A/B render mode:
     * whenever {@code renderMode} changes, the inner card re-renders its
     * current example under the new mode. Passing {@code null} keeps the card
     * on {@code FAST}.
     *
     * @param onTagSelected the handler receiving a clicked tag
     * @param renderMode the reactive render-mode source to follow, or
     *     {@code null} to stay on {@link RenderMode#FAST}
     */
    @SuppressWarnings("this-escape")
    public SampleListCell(Consumer<Tag> onTagSelected,
            ObservableValue<RenderMode> renderMode) {
        this.onTagSelected = onTagSelected;
        card.setOnTagSelected(this::resolveTag);
        card.setLoadProgress(LoadProgress.get());
        card.setTagColor(this::chipColor);
        if (renderMode != null) {
            var initialMode = renderMode.getValue();
            if (initialMode != null) {
                card.setRenderMode(initialMode);
            }
            renderMode.addListener((obs, oldMode, newMode) -> {
                if (newMode != null) {
                    card.setRenderMode(newMode);
                }
            });
        }
        // Consume primary presses so CellBehaviorBase cannot drive selection
        // against a changing items list, but never swallow presses that land on
        // an interactive control (a Button such as the source drawer's nudge):
        // a Button's action is fired from its press/release pairing, so
        // consuming the press here would leave the button permanently inert.
        // Nor swallow presses on an orbit-attached plot, whose drag-to-rotate
        // gesture reads the press position.
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY
                    && !isInteractive(e.getTarget())
                    && !Orbit3d.isAttached(e.getTarget() instanceof Node n ? n : null)) {
                e.consume();
            }
        });
        // Track the viewport so off-screen cards stop loading and release their
        // figure, and on-screen cards (re)load. Bounds changes cover recycling,
        // layout and viewport resizes; the scroll-bar value covers pixel scrolling.
        boundsInParentProperty().addListener(boundsListener);
        listViewProperty().addListener((obs, oldList, newList) -> attachList(newList));
        attachList(getListView());
    }

    /**
     * (Re)attaches the viewport listeners to the owning list. A cell is attached
     * once per list; the skin is observed so a re-skin (which rebuilds the
     * scroll bars) re-wires the scroll listener.
     *
     * @param list the list this cell belongs to, or {@code null}
     */
    @SuppressWarnings("ReferenceEquality") // JavaFX node identity, not value equality
    private void attachList(ListView<SamplerExample> list) {
        if (list == null || list == attachedList) {
            return;
        }
        attachedList = list;
        list.heightProperty().addListener(heightListener);
        list.skinProperty().addListener((obs, oldSkin, newSkin) -> attachScrollBar(list));
        attachScrollBar(list);
    }

    /**
     * Finds the vertical scroll bar of the current skin and observes its value
     * so small pixel scrolls (which translate the sheet without re-laying the
     * cell) still refresh the visibility verdict.
     *
     * @param list the owning list
     */
    @SuppressWarnings("ReferenceEquality") // JavaFX node identity, not value equality
    private void attachScrollBar(ListView<SamplerExample> list) {
        if (list != attachedList || list.getSkin() == null) {
            return;
        }
        for (var node : list.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar
                    && bar.getOrientation() == Orientation.VERTICAL) {
                if (bar != verticalScrollBar) {
                    if (verticalScrollBar != null) {
                        verticalScrollBar.valueProperty().removeListener(scrollListener);
                    }
                    verticalScrollBar = bar;
                    bar.valueProperty().addListener(scrollListener);
                }
                updateVisibility();
                return;
            }
        }
    }

    /**
     * Recomputes whether this cell intersects the owning list's viewport (in
     * scene coordinates) and reports it to the card. Cells that have not been
     * laid out yet keep the optimistic default so the debounced load may start;
     * the next layout pass corrects the verdict.
     */
    private void updateVisibility() {
        if (attachedList == null) {
            return;
        }
        var local = getBoundsInLocal();
        if (local.getWidth() <= 0 || local.getHeight() <= 0) {
            return;
        }
        var cellScene = localToScene(local);
        var listScene = attachedList.localToScene(attachedList.getBoundsInLocal());
        card.setViewportVisible(cellScene.intersects(listScene));
    }

    /**
     * Returns whether the press target is (or is inside) an interactive control
     * such as a {@link Button}, which must receive its press to fire.
     *
     * @param target the event target of the press
     * @return {@code true} if the target is inside a button or other control
     */
    private static boolean isInteractive(Object target) {
        for (var node = target instanceof Node n ? n : null; node != null; node = node.getParent()) {
            if (node instanceof Button) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateItem(SamplerExample item, boolean empty) {
        super.updateItem(item, empty);
        current = item;
        if (empty || item == null) {
            card.setSpec(null);
            setGraphic(null);
            return;
        }
        card.setSpec(item.toSpec());
        // The card stays installed for the life of the recycled cell; setting
        // it each time re-triggers a layout pass on the ListView, so only swap
        // it in when it isn't the graphic already.
        if (!card.equals(getGraphic())) {
            setGraphic(card);
        }
        updateVisibility();
    }
}
