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
package org.jtaccuino.gog.dflib.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link PenguinsDatasets} loader: row and column shape.
 */
class PenguinsDatasetsTest {

    @Test
    void shapeIs344x8() {
        var df = PenguinsDatasets.loadPenguins();
        assertEquals(344, df.height(), "penguins has 344 rows");
        assertEquals(8, df.getColumnsIndex().size(), "penguins has 8 columns");
    }

    @Test
    void columnsMatchExpectedNames() {
        var df = PenguinsDatasets.loadPenguins();
        assertTrue(df.getColumnsIndex().contains("species"));
        assertTrue(df.getColumnsIndex().contains("island"));
        assertTrue(df.getColumnsIndex().contains("bill_length_mm"));
        assertTrue(df.getColumnsIndex().contains("bill_depth_mm"));
        assertTrue(df.getColumnsIndex().contains("flipper_length_mm"));
        assertTrue(df.getColumnsIndex().contains("body_mass_g"));
        assertTrue(df.getColumnsIndex().contains("sex"));
        assertTrue(df.getColumnsIndex().contains("year"));
    }

    @Test
    void numericColumnsAreDouble() {
        var df = PenguinsDatasets.loadPenguins();
        assertNotNull(df.getColumn("bill_length_mm").get(0));
        var first = df.getColumn("bill_length_mm").get(0);
        assertTrue(first instanceof Double, "bill_length_mm parses as a number");
    }

    @Test
    void speciesValuePresent() {
        var df = PenguinsDatasets.loadPenguins();
        assertEquals("Adelie", df.getColumn("species").get(0));
    }
}
