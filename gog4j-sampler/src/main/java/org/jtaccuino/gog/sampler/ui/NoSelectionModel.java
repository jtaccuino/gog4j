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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 * A selection model that performs no selection at all. The sampler's {@code ListView}
 * is a pure virtualized browser of plot cards — there is no master/detail selection —
 * so clicks must never drive the selection machinery, which can otherwise throw
 * {@code IndexOutOfBoundsException} when the items list changes between focusing a
 * cell and finishing the click.
 *
 * @param <T> the item type
 */
public class NoSelectionModel<T> extends MultipleSelectionModel<T> {

    @SuppressWarnings("rawtypes")
    private static final ObservableList EMPTY = FXCollections.observableArrayList();

    /** Creates a selection model that never selects anything. */
    public NoSelectionModel() {
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void clearAndSelect(int index) {
    }

    @Override
    public void select(int index) {
    }

    @Override
    public void select(T obj) {
    }

    @Override
    public void clearSelection(int index) {
    }

    @Override
    public void clearSelection() {
    }

    @Override
    public boolean isSelected(int index) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void selectPrevious() {
    }

    @Override
    public void selectNext() {
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        @SuppressWarnings("unchecked")
        var cast = (ObservableList<Integer>) EMPTY;
        return cast;
    }

    @Override
    public ObservableList<T> getSelectedItems() {
        @SuppressWarnings("unchecked")
        var cast = (ObservableList<T>) EMPTY;
        return cast;
    }

    @Override
    public void selectIndices(int index, int... indices) {
    }

    @Override
    public void selectFirst() {
    }

    @Override
    public void selectLast() {
    }
}
