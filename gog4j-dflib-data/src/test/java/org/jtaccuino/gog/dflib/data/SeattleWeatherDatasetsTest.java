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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link SeattleWeatherDatasets} loader: the date column is
 * parsed into date-times rather than left as text, and the first-day slice is
 * derived from the full year.
 */
class SeattleWeatherDatasetsTest {

    private static final DflibDataExtractor EXTRACTOR = new DflibDataExtractor();

    @Test
    void shapeIs8759x4() {
        var df = SeattleWeatherDatasets.loadHourly();
        assertEquals(8759, df.height(), "a leap year of hourly readings");
        assertEquals(4, df.getColumnsIndex().size());
        assertTrue(df.getColumnsIndex().contains("date"));
    }

    @Test
    void dateColumnParsesAsDateTimes() {
        var df = SeattleWeatherDatasets.loadHourly();
        assertEquals(LocalDateTime.of(2010, 1, 1, 1, 0), df.getColumn("date").get(0));
        assertEquals(LocalDateTime.of(2010, 1, 1, 2, 0), df.getColumn("date").get(1));
    }

    @Test
    void theEngineSeesTheDateColumnAsTimestamps() {
        var df = SeattleWeatherDatasets.loadHourly();
        assertEquals(DflibDataExtractor.ColumnType.TIMESTAMP, EXTRACTOR.columnType(df, "date"));
        assertEquals(DflibDataExtractor.ColumnType.NUMBER, EXTRACTOR.columnType(df, "wind"));
    }

    @Test
    void theTimestampRangeIsFiniteAndInEpochMillis() {
        var df = SeattleWeatherDatasets.loadHourly();
        var range = EXTRACTOR.getMinMax(df, "date");
        assertTrue(Double.isFinite(range.min()) && Double.isFinite(range.max()),
                "a timestamp range must be finite, not the [0, 10] placeholder");
        assertTrue(range.min() > 0, "epoch millis, not days or raw text");
        assertEquals((double) LocalDateTime.of(2010, 1, 1, 1, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli(), range.min());
    }

    @Test
    void firstDayIsTheFirstTwentyFourHours() {
        var df = SeattleWeatherDatasets.loadFirstDay();
        assertEquals(24, df.height());
        assertEquals(LocalDateTime.of(2010, 1, 1, 1, 0), df.getColumn("date").get(0));
        assertEquals(LocalDateTime.of(2010, 1, 2, 0, 0), df.getColumn("date").get(23));
    }
}
