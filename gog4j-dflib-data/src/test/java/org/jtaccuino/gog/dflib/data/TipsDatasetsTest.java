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
 * Verifies the {@link TipsDatasets} loader: row and column shape.
 */
class TipsDatasetsTest {

    @Test
    void shapeIs244x7() {
        var df = TipsDatasets.loadTips();
        assertEquals(244, df.height(), "tips has 244 rows");
        assertEquals(7, df.getColumnsIndex().size(), "tips has 7 columns");
    }

    @Test
    void columnsMatchExpectedNames() {
        var df = TipsDatasets.loadTips();
        assertTrue(df.getColumnsIndex().contains("total_bill"));
        assertTrue(df.getColumnsIndex().contains("tip"));
        assertTrue(df.getColumnsIndex().contains("sex"));
        assertTrue(df.getColumnsIndex().contains("smoker"));
        assertTrue(df.getColumnsIndex().contains("day"));
        assertTrue(df.getColumnsIndex().contains("time"));
        assertTrue(df.getColumnsIndex().contains("size"));
    }

    @Test
    void numericColumnsAreDouble() {
        var df = TipsDatasets.loadTips();
        assertTrue(df.getColumn("total_bill").get(0) instanceof Double,
                "total_bill parses as a number");
        assertTrue(df.getColumn("tip").get(0) instanceof Double, "tip parses as a number");
    }

    @Test
    void categoricalValuesPresent() {
        var df = TipsDatasets.loadTips();
        assertEquals("Female", df.getColumn("sex").get(0));
        assertEquals("No", df.getColumn("smoker").get(0));
        assertEquals("Sun", df.getColumn("day").get(0));
        assertEquals("Dinner", df.getColumn("time").get(0));
        assertNotNull(df.getColumn("size").get(0));
    }
}
