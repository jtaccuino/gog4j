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

import java.util.function.Function;
import javafx.scene.control.ListCell;

/**
 * A {@link ListCell} that renders an item through a display function, used to
 * show {@link SearchableComboBox} entries as plain text.
 *
 * @param <T> the item type
 */
class TextCell<T> extends ListCell<T> {

    private final Function<T, String> display;

    TextCell(Function<T, String> display) {
        this.display = display;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : display.apply(item));
    }
}
