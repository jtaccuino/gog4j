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

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.jtaccuino.gog.sampler.registry.Tag;

/**
 * A removable filter chip shown in the filter bar. Displays a tag's label and a
 * small {@code ×} button that removes the filter when clicked.
 */
public class ActiveFilterChip extends HBox {

    private final Runnable onRemove;

    /**
     * Creates a chip for the given tag.
     *
     * @param tag the tag this chip filters by
     * @param onRemove callback invoked when the chip is removed
     */
    @SuppressWarnings("this-escape")
    public ActiveFilterChip(Tag tag, Runnable onRemove) {
        this.onRemove = onRemove;
        var label = new Label(tag.label());
        var remove = new Button("\u00d7");
        remove.setStyle("-fx-background-color: transparent; -fx-padding: 0 2 0 2;"
                + "-fx-cursor: hand;");
        setStyle("-fx-background-color: " + TagColors.forKind(tag.kind())
                + "; -fx-background-radius: 10; -fx-padding: 2 6 2 8;");
        getChildren().addAll(label, remove);
        remove.setOnAction(e -> this.onRemove.run());
    }
}
