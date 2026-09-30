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
package org.jtaccuino.gog.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the built-in builder datasets carry a usable display name (the name
 * the dataset picker shows) alongside their loader metadata.
 */
class DflibDatasetsTest {

    @Test
    void everyDatasetHasADisplayNameAndLoader() {
        var datasets = DflibDatasets.all();
        assertFalse(datasets.isEmpty(), "the builder must offer datasets");
        for (var dataset : datasets) {
            assertFalse(dataset.displayName().isBlank(),
                    () -> dataset + " needs a display name");
            assertEquals(dataset.displayName(), dataset.toString(),
                    "the picker shows the display name via toString()");
            assertFalse(dataset.loaderExpr().isBlank(),
                    () -> dataset.displayName() + " needs a loader expression");
            assertFalse(dataset.loaderImport().isBlank(),
                    () -> dataset.displayName() + " needs a loader import");
            assertNotNull(dataset.data(), () -> dataset.displayName() + " must load data");
            assertFalse(dataset.columns().isEmpty(),
                    () -> dataset.displayName() + " must expose columns");
        }
    }

    @Test
    void includesThe3dDemoDatasets() {
        var names = DflibDatasets.all().stream().map(d -> d.displayName()).toList();
        assertEquals("MPG", names.get(0));
        assertTrue(names.contains("Diamonds"));
        assertTrue(names.contains("Mountain Surface"));
        assertTrue(names.contains("Sphere Points"));
    }
}
